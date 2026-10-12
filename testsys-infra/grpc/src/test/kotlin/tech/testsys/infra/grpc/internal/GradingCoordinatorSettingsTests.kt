@file:OptIn(InternalGrpcApi::class)

package tech.testsys.infra.grpc.internal

import io.grpc.Status
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.core.env.MapPropertySource
import org.springframework.core.env.StandardEnvironment
import org.springframework.core.io.support.ResourcePropertySource
import tech.testsys.domain.contract.GradingAdmission
import tech.testsys.domain.contract.GradingNodeAddress
import tech.testsys.domain.contract.GradingNodeStatus
import tech.testsys.domain.model.task.GradingResult
import tech.testsys.domain.model.task.SubmissionStatus
import tech.testsys.infra.settings.api.SettingsConfiguration
import tech.testsys.infra.settings.api.catalog.SettingKey
import tech.testsys.infra.settings.api.service.SettingsChangeResult
import tech.testsys.infra.settings.api.settingsOf
import tech.testsys.infra.settings.api.store.SettingsStore
import tech.testsys.infra.settings.api.store.StoredSettings
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

internal class GradingCoordinatorSettingsTests {
    private val repository = RepositoryFixture()
    private val node = FakeNode()
    private val queue = ManualExecutor()
    private val polling = ManualExecutor()
    private val sender = Executors.newSingleThreadExecutor()
    private val durable = AtomicReference(StoredSettings(0, emptyMap(), null))
    private val store = mockk<SettingsStore> {
        every { read() } answers { durable.get() }
        every { compareAndSet(any(), any(), any(), any()) } answers {
            StoredSettings(firstArg<Long>() + 1, secondArg(), thirdArg<UUID>()).also(durable::set)
        }
    }
    private val environment = StandardEnvironment().apply {
        propertySources.addFirst(ResourcePropertySource("classpath:grading-defaults.properties"))
        propertySources.addFirst(
            MapPropertySource(
                "settings-test",
                mapOf(
                    "testsys.operation.competition.max-participants" to "100",
                    "testsys.operation.email-confirmation.confirmation-code-lifetime" to "PT5M",
                    "testsys.operation.email-confirmation.max-confirmation-attempts" to "5",
                    "testsys.operation.community-invite.ttl" to "PT24H",
                    "testsys.operation.class-invite.ttl" to "PT24H",
                    "testsys.operation.community.public-community-id" to "42",
                ),
            ),
        )
    }
    private val configuration = SettingsConfiguration()
    private val runtime = configuration.runtimeSettings(store, environment)
    private val service = configuration.settingsService(store, runtime, environment)
    private val now = AtomicReference(Instant.EPOCH)
    private val clock = mockk<Clock> {
        every { instant() } answers { now.get() }
    }
    private val coordinator = GradingCoordinator(
        persistence = repository.persistence,
        nodes = GradingNodeManager(NodeClientFactory { node }),
        settings = settingsOf(GradingProperties::class.java, environment, runtime),
        executor = queue.service,
        senderExecutor = sender,
        pollingExecutor = polling.service,
        clock = clock,
        parser = JsonLogParser(),
        runtime = runtime,
        environment = environment,
    )

    @AfterEach
    fun stopWorker() {
        coordinator.close()
        assertTrue(sender.awaitTermination(5, TimeUnit.SECONDS))
        runtime.close()
    }

    @Test
    fun `should preserve accepted settings through preparation duplicate request and transport retry`() {
        coordinator.submit(repository.initial)
        service.change(
            0,
            mapOf(
                SettingKey.ShouldRecordVideo to "false",
                SettingKey.RpcTimeout to "PT1S",
                SettingKey.GradingMaxAttempts to "1",
                SettingKey.TotalTimeout to "PT1S",
            ),
        )
        assertEquals(GradingAdmission.AlreadyPending, coordinator.submit(repository.initial))
        coordinator.start()
        coordinator.addNode(GradingNodeAddress("node"))
        polling.tick()
        queue.await { node.messages.isNotEmpty() }
        node.errors.first()(Status.UNAVAILABLE.asRuntimeException())
        queue.drain()
        polling.tick()
        queue.await { node.messages.size == 2 }

        assertTrue(node.messages.first().options.recordVideo)
        assertEquals(listOf(Duration.ofMinutes(10), Duration.ofMinutes(10)), node.timeouts)
    }

    @Test
    fun `should apply new settings to a regrade after the previous run finished`() {
        coordinator.start()
        coordinator.addNode(GradingNodeAddress("node"))
        polling.tick()
        coordinator.submit(repository.initial)
        queue.await { node.messages.isNotEmpty() }
        node.results.first()(result())
        queue.drain()
        service.change(0, mapOf(SettingKey.ShouldRecordVideo to "false", SettingKey.RpcTimeout to "PT1S"))

        coordinator.submit(repository.initial)
        queue.await { node.messages.size == 2 }

        assertEquals(false, node.messages.last().options.recordVideo)
        assertEquals(Duration.ofSeconds(1), node.timeouts.last())
    }

    @Test
    fun `should finish a run accepted after an attempt limit change after that many transport failures`() {
        service.change(0, mapOf(SettingKey.GradingMaxAttempts to "2"))
        coordinator.start()
        coordinator.addNode(GradingNodeAddress("node"))
        polling.tick()
        coordinator.submit(repository.initial)
        queue.await { node.messages.isNotEmpty() }
        node.errors[0](Status.UNAVAILABLE.asRuntimeException())
        queue.drain()
        polling.tick()
        queue.await { node.messages.size == 2 }

        node.errors[1](Status.UNAVAILABLE.asRuntimeException())
        queue.drain()

        val status = assertInstanceOf(SubmissionStatus.Graded::class.java, repository.current.get().data.status)
        assertInstanceOf(GradingResult.GradingError::class.java, status.grade)
        assertEquals(2, node.messages.size)
    }

    @Test
    fun `should expire a run accepted after a total timeout change while it waits for a free node`() {
        service.change(0, mapOf(SettingKey.TotalTimeout to "PT1M"))
        coordinator.start()
        node.status.set(GradingNodeStatus.Available(queued = 1, capacity = 1))
        coordinator.addNode(GradingNodeAddress("node"))
        polling.tick()
        coordinator.submit(repository.initial)
        queue.runNext()
        now.set(Instant.EPOCH.plusSeconds(60))
        node.status.set(GradingNodeStatus.Available(queued = 0, capacity = 1))

        polling.tick()
        queue.await { repository.current.get().data.status is SubmissionStatus.Graded }

        val status = assertInstanceOf(SubmissionStatus.Graded::class.java, repository.current.get().data.status)
        assertEquals(GradingResult.Timeout, status.grade)
        assertEquals(emptyList<trik.testsys.grading.GradingNodeOuterClass.Submission>(), node.messages)
    }

    @Test
    fun `should retain old polling and in flight RPC when preparing a new schedule fails`() {
        coordinator.start()
        coordinator.addNode(GradingNodeAddress("node"))
        polling.tick()
        coordinator.submit(repository.initial)
        queue.await { node.messages.isNotEmpty() }
        every { polling.service.scheduleWithFixedDelay(any(), any(), any(), any()) } throws RejectedExecutionException("closed")

        val changed = service.change(0, mapOf(SettingKey.PollInterval to "PT1S"))
        polling.tick()

        assertEquals(SettingsChangeResult.Failed, changed)
        assertEquals(2, node.polls.get())
        assertEquals(0, node.cancellations.get())
        assertEquals(0, durable.get().revision)
    }

    @Test
    fun `should gate old polling schedule after atomic settings publication without replacing the channel`() {
        coordinator.start()
        coordinator.addNode(GradingNodeAddress("node"))
        polling.tick()
        assertEquals(1, node.polls.get())

        val changed = service.change(0, mapOf(SettingKey.PollInterval to "PT1S", SettingKey.StatusTimeout to "PT2S"))
        polling.tick()

        assertTrue(changed is SettingsChangeResult.Applied)
        assertEquals(2, node.polls.get())
        assertEquals(listOf(Duration.ofSeconds(5), Duration.ofSeconds(2)), node.statusTimeouts)
        assertEquals(false, node.isClosed.get())
        verify(exactly = 1) { polling.service.scheduleWithFixedDelay(any(), any(), 5_000_000_000L, TimeUnit.NANOSECONDS) }
        verify(exactly = 1) { polling.service.scheduleWithFixedDelay(any(), any(), 1_000_000_000L, TimeUnit.NANOSECONDS) }
    }
}
