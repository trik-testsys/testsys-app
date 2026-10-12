package tech.testsys.web.app.config

import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.core.env.MapPropertySource
import org.springframework.core.env.StandardEnvironment
import tech.testsys.domain.builder.api.taskValidationRequest
import tech.testsys.domain.builder.api.taskValidationRequestData
import tech.testsys.domain.builder.api.taskValidationSnapshot
import tech.testsys.domain.contract.persistence.repository.TaskValidationRequestRepository
import tech.testsys.domain.model.EntityVersion
import tech.testsys.infra.grpc.api.BalancingGrader
import tech.testsys.infra.monitoring.api.TechnicalFailures
import tech.testsys.infra.monitoring.api.TestSysMetrics
import tech.testsys.infra.settings.api.RuntimeSettings
import tech.testsys.infra.settings.api.SettingsSnapshot
import tech.testsys.infra.settings.api.catalog.SettingKey
import tech.testsys.infra.settings.api.catalog.SettingsValues
import tech.testsys.operation.TaskValidationOperations
import java.time.Duration
import java.time.Instant
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

class OperationsConfigurationTests {
    private val configuration = OperationsConfiguration()
    private val requests = mockk<TaskValidationRequestRepository>()
    private val operations = mockk<TaskValidationOperations>()
    private val registry = SimpleMeterRegistry()
    private val executor = configuration.taskValidationExecutor()
    private val dispatcher = configuration.taskValidationDispatcher(
        requests = requests,
        operations = operations,
        grader = mockk<BalancingGrader>(),
        executor = executor,
        failures = TechnicalFailures(registry),
    )
    private val request = taskValidationRequest {
        id = 11
        createdAt = Instant.EPOCH
        version = EntityVersion(0)
        data = taskValidationRequestData {
            task(0)
            requestedBy(0)
            snapshot = taskValidationSnapshot {}
            execution.pendingDiagnostics()
        }
    }

    @Test
    fun `should count a failed processing of a validation request as a validation technical failure`() {
        every { operations.proceed(request.id) } throws IllegalStateException("Grader unavailable")
        every { requests.recordTechnicalFailure(request.id, any()) } returns request

        dispatcher.schedule(request.id)
        executor.shutdown()
        executor.awaitTermination(5, TimeUnit.SECONDS)

        val failures = registry.find(TestSysMetrics.TECHNICAL_FAILURES)
            .tags(
                TestSysMetrics.SOURCE,
                "validation",
                TestSysMetrics.STAGE,
                "processing",
                TestSysMetrics.EXCEPTION,
                "IllegalStateException",
            )
            .counter()
        assertEquals(1.0, failures?.count())
    }

    @Test
    fun `should read operation settings from the current snapshot`() {
        val current = AtomicReference(operationSnapshot(revision = 0, number = 1, duration = "PT1M"))
        val runtime = mockk<RuntimeSettings> {
            every { snapshot() } answers { current.get() }
            every { register(any()) } returns AutoCloseable {}
        }
        val refreshPeriods = mapOf<String, Any>(
            "testsys.operation.community-invite.refresh-period" to "PT1H",
            "testsys.operation.class-invite.refresh-period" to "PT1H",
        )
        val environment = StandardEnvironment().apply { propertySources.addFirst(MapPropertySource("test", refreshPeriods)) }
        val competition = configuration.competitionConfig(configuration.competitionSettings(environment, runtime))
        val emailConfirmation = configuration.emailConfirmationConfig(configuration.emailConfirmationSettings(environment, runtime))
        val communityInvite = configuration.communityInviteConfig(environment, configuration.communityInviteSettings(environment, runtime))
        val classInvite = configuration.classInviteConfig(environment, configuration.classInviteSettings(environment, runtime))
        val read = {
            listOf(
                competition.maxParticipants,
                emailConfirmation.maxConfirmationAttempts,
                emailConfirmation.confirmationCodeLifetime,
                communityInvite.ttl,
                classInvite.ttl,
            )
        }
        val before = read()

        current.set(operationSnapshot(revision = 1, number = 2, duration = "PT2M"))

        assertEquals(listOf(1, 1, Duration.ofMinutes(1), Duration.ofMinutes(1), Duration.ofMinutes(1)), before)
        assertEquals(listOf(2, 2, Duration.ofMinutes(2), Duration.ofMinutes(2), Duration.ofMinutes(2)), read())
    }

    private fun operationSnapshot(revision: Long, number: Int, duration: String) = SettingsSnapshot(
        revision = revision,
        values = SettingsValues(
            mapOf(
                SettingKey.MaxParticipants to number.toString(),
                SettingKey.MaxConfirmationAttempts to number.toString(),
                SettingKey.ConfirmationCodeLifetime to duration,
                SettingKey.CommunityInviteTtl to duration,
                SettingKey.ClassInviteTtl to duration,
            ),
        ),
        activeMaxMessageBytes = 100,
    )
}
