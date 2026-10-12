@file:OptIn(InternalGrpcApi::class)

package tech.testsys.infra.grpc.internal

import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.core.env.StandardEnvironment
import tech.testsys.domain.contract.GradingNodeAddress
import tech.testsys.domain.contract.GradingNodeStatus
import tech.testsys.infra.grpc.api.GradingNodeChangeResult
import tech.testsys.infra.grpc.api.GradingNodeEntry
import tech.testsys.infra.grpc.api.GradingNodeImportCandidate
import tech.testsys.infra.grpc.api.GradingNodeImportOutcome
import tech.testsys.infra.grpc.api.GradingNodeImportPreview
import tech.testsys.infra.grpc.api.GradingNodeImportReport
import tech.testsys.infra.grpc.api.GradingNodeRejection
import tech.testsys.infra.monitoring.api.TechnicalFailures
import tech.testsys.infra.monitoring.api.TestSysMetrics
import tech.testsys.infra.settings.api.registry.GradingNodeStore
import tech.testsys.infra.settings.api.registry.StoredGradingNode
import tech.testsys.infra.settings.api.service.SettingsPersistence
import java.time.Clock
import java.util.concurrent.Executors

internal class GradingNodeRegistryCoordinatorTests {
    private val queue = ManualExecutor()
    private val polling = ManualExecutor()
    private val factory = mockk<NodeClientFactory> {
        every { create(any()) } answers { FakeNode() }
    }
    private val coordinator = GradingCoordinator(
        persistence = RepositoryFixture().persistence,
        nodes = GradingNodeManager(factory),
        settings = settings,
        executor = queue.service,
        senderExecutor = Executors.newSingleThreadExecutor(),
        pollingExecutor = polling.service,
        clock = Clock.systemUTC(),
        parser = JsonLogParser(),
        environment = StandardEnvironment(),
    )
    private val store = mockk<GradingNodeStore>()
    private val meters = SimpleMeterRegistry()
    private val active = GradingNodeAddress("grader-1:50051")
    private val inactive = GradingNodeAddress("grader-2:50051")
    private val added = GradingNodeAddress("grader-3:50051")

    @AfterEach
    fun closeCoordinator() {
        coordinator.close()
    }

    @Nested
    inner class OverviewTests {
        @Test
        fun `should restore known records from the started coordinator ordered by address`() {
            val registry = registry()
            coordinator.start()
            polling.tick()

            val overview = registry.overview()

            assertEquals(
                listOf(
                    GradingNodeEntry(StoredGradingNode(active, isActive = true), GradingNodeStatus.Available(queued = 0, capacity = 1)),
                    GradingNodeEntry(StoredGradingNode(inactive, isActive = false), GradingNodeStatus.Available(queued = 0, capacity = 1)),
                ),
                overview.nodes,
            )
            assertEquals(SettingsPersistence.Known, overview.persistence)
        }
    }

    @Nested
    inner class AddTests {
        @Test
        fun `should start participation before writing an active node`() {
            val registry = registry()
            val participants = mutableListOf<Set<GradingNodeAddress>>()
            every { store.add(StoredGradingNode(added, isActive = true)) } answers {
                participants.add(coordinator.statuses().keys)
                true
            }

            val result = registry.add(" grader-3:50051 ", isActive = true)

            assertEquals(GradingNodeChangeResult.Applied, result)
            assertEquals(listOf(setOf(active, added)), participants)
            assertEquals(setOf(active, added), coordinator.statuses().keys)
            assertEquals(
                GradingNodeEntry(StoredGradingNode(added, isActive = true), GradingNodeStatus.Unknown),
                registry.overview().nodes[2],
            )
        }

        @Test
        fun `should observe an inactive node without participation`() {
            val registry = registry()
            every { store.add(StoredGradingNode(added, isActive = false)) } returns true

            val result = registry.add("grader-3:50051", isActive = false)

            assertEquals(GradingNodeChangeResult.Applied, result)
            assertEquals(setOf(active), coordinator.statuses().keys)
            assertEquals(setOf(active, inactive, added), coordinator.observedStatuses().keys)
        }

        @Test
        fun `should reject a blank address without changes`() {
            val registry = registry()

            val result = registry.add("   ", isActive = true)

            assertEquals(GradingNodeChangeResult.Rejected(GradingNodeRejection.Blank), result)
            verify(exactly = 0) { store.add(any()) }
        }

        @Test
        fun `should reject an address longer than the stored limit`() {
            val registry = registry()

            val result = registry.add("a".repeat(256), isActive = true)

            assertEquals(GradingNodeChangeResult.Rejected(GradingNodeRejection.TooLong), result)
            verify(exactly = 0) { store.add(any()) }
        }

        @Test
        fun `should accept an address of exactly the stored limit`() {
            val registry = registry()
            val address = "a".repeat(255)
            every { store.add(StoredGradingNode(GradingNodeAddress(address), isActive = false)) } returns true

            val result = registry.add(address, isActive = false)

            assertEquals(GradingNodeChangeResult.Applied, result)
        }

        @Test
        fun `should reject an already registered address without changing it`() {
            val registry = registry()

            val result = registry.add(inactive.target, isActive = true)

            assertEquals(GradingNodeChangeResult.Rejected(GradingNodeRejection.AlreadyRegistered), result)
            assertEquals(setOf(active), coordinator.statuses().keys)
            verify(exactly = 0) { store.add(any()) }
        }

        @Test
        fun `should report failure without a write when the node channel cannot be created`() {
            val registry = registry()
            val failing = GradingNodeAddress("invalid")
            every { factory.create(failing) } throws IllegalArgumentException("Invalid target")

            val result = registry.add(failing.target, isActive = true)

            assertEquals(GradingNodeChangeResult.Failed, result)
            assertEquals(setOf(active, inactive), coordinator.observedStatuses().keys)
            verify(exactly = 0) { store.add(any()) }
        }

        @Test
        fun `should apply a write whose exception hides a confirmed commit`() {
            val registry = registry()
            every { store.add(any()) } throws IllegalStateException("Commit failed")
            every { store.read() } returns listOf(StoredGradingNode(added, isActive = true))

            val result = registry.add(added.target, isActive = true)

            assertEquals(GradingNodeChangeResult.Applied, result)
            assertEquals(setOf(active, added), coordinator.statuses().keys)
        }

        @Test
        fun `should compensate the runtime after a confirmed rollback`() {
            val registry = registry()
            every { store.add(any()) } throws IllegalStateException("Commit failed")
            every { store.read() } returns emptyList()

            val result = registry.add(added.target, isActive = true)

            assertEquals(GradingNodeChangeResult.Failed, result)
            assertEquals(setOf(active, inactive), coordinator.observedStatuses().keys)
            assertEquals(SettingsPersistence.Known, registry.overview().persistence)
        }

        @Test
        fun `should compensate the runtime when the address has a different stored record`() {
            val registry = registry()
            every { store.add(any()) } returns false

            val result = registry.add(added.target, isActive = true)

            assertEquals(GradingNodeChangeResult.Failed, result)
            assertEquals(setOf(active, inactive), coordinator.observedStatuses().keys)
        }

        @Test
        fun `should compensate the runtime and lock changes when the outcome cannot be read`() {
            val registry = registry()
            every { store.add(any()) } throws IllegalStateException("Commit failed")
            every { store.read() } throws IllegalStateException("Read failed")

            val result = registry.add(added.target, isActive = true)

            assertEquals(GradingNodeChangeResult.Uncertain, result)
            assertEquals(setOf(active, inactive), coordinator.observedStatuses().keys)
            assertEquals(SettingsPersistence.Uncertain, registry.overview().persistence)
            assertEquals(GradingNodeChangeResult.Uncertain, registry.remove(inactive))
        }

        @Test
        fun `should lock changes when the read back record is unexpected`() {
            val registry = registry()
            every { store.add(any()) } throws IllegalStateException("Commit failed")
            every { store.read() } returns listOf(StoredGradingNode(added, isActive = false))

            val result = registry.add(added.target, isActive = true)

            assertEquals(GradingNodeChangeResult.Uncertain, result)
            assertEquals(setOf(active, inactive), coordinator.observedStatuses().keys)
            assertEquals(GradingNodeChangeResult.Uncertain, registry.setActive(inactive, isActive = true))
        }

        @Test
        fun `should return uncertain without changes after the registry is locked`() {
            val registry = registry()
            lockChanges(registry)

            val result = registry.add(added.target, isActive = true)

            assertEquals(GradingNodeChangeResult.Uncertain, result)
            assertEquals(setOf(active), coordinator.statuses().keys)
            assertEquals(setOf(active, inactive), coordinator.observedStatuses().keys)
            verify(exactly = 0) { store.add(any()) }
        }
    }

    @Nested
    inner class SetActiveTests {
        @Test
        fun `should write disabling before stopping participation and keep observing the node`() {
            val registry = registry()
            val participants = mutableListOf<Set<GradingNodeAddress>>()
            every { store.setActive(active, isActive = false) } answers {
                participants.add(coordinator.statuses().keys)
                true
            }

            val result = registry.setActive(active, isActive = false)

            assertEquals(GradingNodeChangeResult.Applied, result)
            assertEquals(listOf(setOf(active)), participants)
            assertEquals(emptySet<GradingNodeAddress>(), coordinator.statuses().keys)
            assertEquals(setOf(active, inactive), coordinator.observedStatuses().keys)
            assertEquals(StoredGradingNode(active, isActive = false), registry.overview().nodes[0].node)
        }

        @Test
        fun `should start participation before writing enabling`() {
            val registry = registry()
            val participants = mutableListOf<Set<GradingNodeAddress>>()
            every { store.setActive(inactive, isActive = true) } answers {
                participants.add(coordinator.statuses().keys)
                true
            }

            val result = registry.setActive(inactive, isActive = true)

            assertEquals(GradingNodeChangeResult.Applied, result)
            assertEquals(listOf(setOf(active, inactive)), participants)
            assertEquals(setOf(active, inactive), coordinator.statuses().keys)
        }

        @Test
        fun `should show unknown availability of an enabled node until the next poll`() {
            val registry = registry()
            coordinator.start()
            polling.tick()
            every { store.setActive(inactive, isActive = true) } returns true

            registry.setActive(inactive, isActive = true)
            val beforePoll = registry.overview().nodes[1]
            polling.tick()

            assertEquals(GradingNodeEntry(StoredGradingNode(inactive, isActive = true), GradingNodeStatus.Unknown), beforePoll)
            assertEquals(GradingNodeStatus.Available(queued = 0, capacity = 1), registry.overview().nodes[1].status)
        }

        @Test
        fun `should report failure without a write when participation cannot start`() {
            val registry = registry()
            every { factory.create(inactive) } throws IllegalStateException("Channel failed")

            val result = registry.setActive(inactive, isActive = true)

            assertEquals(GradingNodeChangeResult.Failed, result)
            assertEquals(setOf(active), coordinator.statuses().keys)
            assertEquals(setOf(active, inactive), coordinator.observedStatuses().keys)
            assertEquals(StoredGradingNode(inactive, isActive = false), registry.overview().nodes[1].node)
            verify(exactly = 0) { store.setActive(any(), any()) }
        }

        @Test
        fun `should keep participation after an enabling write with a confirmed commit`() {
            val registry = registry()
            every { store.setActive(inactive, isActive = true) } throws IllegalStateException("Commit failed")
            every { store.read() } returns listOf(StoredGradingNode(active, isActive = true), StoredGradingNode(inactive, isActive = true))

            val result = registry.setActive(inactive, isActive = true)

            assertEquals(GradingNodeChangeResult.Applied, result)
            assertEquals(setOf(active, inactive), coordinator.statuses().keys)
            assertEquals(StoredGradingNode(inactive, isActive = true), registry.overview().nodes[1].node)
        }

        @Test
        fun `should stop the new participation and lock changes when an enabling outcome cannot be read`() {
            val registry = registry()
            every { store.setActive(inactive, isActive = true) } throws IllegalStateException("Commit failed")
            every { store.read() } throws IllegalStateException("Read failed")

            val result = registry.setActive(inactive, isActive = true)

            assertEquals(GradingNodeChangeResult.Uncertain, result)
            assertEquals(setOf(active), coordinator.statuses().keys)
            assertEquals(setOf(active, inactive), coordinator.observedStatuses().keys)
            assertEquals(StoredGradingNode(inactive, isActive = false), registry.overview().nodes[1].node)
            assertEquals(SettingsPersistence.Uncertain, registry.overview().persistence)
        }

        @Test
        fun `should stop the new participation when the enabled record has disappeared`() {
            val registry = registry()
            every { store.setActive(inactive, isActive = true) } returns false

            val result = registry.setActive(inactive, isActive = true)

            assertEquals(GradingNodeChangeResult.Failed, result)
            assertEquals(setOf(active), coordinator.statuses().keys)
            assertEquals(setOf(active, inactive), coordinator.observedStatuses().keys)
        }

        @Test
        fun `should apply the same participation without a write`() {
            val registry = registry()

            val result = registry.setActive(active, isActive = true)

            assertEquals(GradingNodeChangeResult.Applied, result)
            verify(exactly = 0) { store.setActive(any(), any()) }
        }

        @Test
        fun `should reject an unregistered address`() {
            val registry = registry()

            val result = registry.setActive(added, isActive = true)

            assertEquals(GradingNodeChangeResult.Rejected(GradingNodeRejection.NotRegistered), result)
        }

        @Test
        fun `should stop participation after a disabling write with a confirmed commit`() {
            val registry = registry()
            every { store.setActive(active, isActive = false) } throws IllegalStateException("Commit failed")
            every { store.read() } returns listOf(StoredGradingNode(active, isActive = false))

            val result = registry.setActive(active, isActive = false)

            assertEquals(GradingNodeChangeResult.Applied, result)
            assertEquals(emptySet<GradingNodeAddress>(), coordinator.statuses().keys)
        }

        @Test
        fun `should keep participation after a disabling write with a confirmed rollback`() {
            val registry = registry()
            every { store.setActive(active, isActive = false) } throws IllegalStateException("Commit failed")
            every { store.read() } returns listOf(StoredGradingNode(active, isActive = true))

            val result = registry.setActive(active, isActive = false)

            assertEquals(GradingNodeChangeResult.Failed, result)
            assertEquals(setOf(active), coordinator.statuses().keys)
        }

        @Test
        fun `should keep participation and lock changes when a disabling outcome cannot be read`() {
            val registry = registry()
            every { store.setActive(active, isActive = false) } throws IllegalStateException("Commit failed")
            every { store.read() } throws IllegalStateException("Read failed")

            val result = registry.setActive(active, isActive = false)

            assertEquals(GradingNodeChangeResult.Uncertain, result)
            assertEquals(setOf(active), coordinator.statuses().keys)
            assertEquals(StoredGradingNode(active, isActive = true), registry.overview().nodes[0].node)
            assertEquals(SettingsPersistence.Uncertain, registry.overview().persistence)
        }

        @Test
        fun `should stop the new participation after an enabling write with a confirmed rollback`() {
            val registry = registry()
            every { store.setActive(inactive, isActive = true) } throws IllegalStateException("Commit failed")
            every { store.read() } returns listOf(StoredGradingNode(inactive, isActive = false))

            val result = registry.setActive(inactive, isActive = true)

            assertEquals(GradingNodeChangeResult.Failed, result)
            assertEquals(setOf(active), coordinator.statuses().keys)
            assertEquals(setOf(active, inactive), coordinator.observedStatuses().keys)
        }

        @Test
        fun `should report failure when the stored record has disappeared`() {
            val registry = registry()
            every { store.setActive(active, isActive = false) } returns false

            val result = registry.setActive(active, isActive = false)

            assertEquals(GradingNodeChangeResult.Failed, result)
            assertEquals(setOf(active), coordinator.statuses().keys)
        }
    }

    @Nested
    inner class RemoveTests {
        @Test
        fun `should delete the record before stopping participation and observation`() {
            val registry = registry()
            val observed = mutableListOf<Set<GradingNodeAddress>>()
            every { store.remove(active) } answers { observed.add(coordinator.observedStatuses().keys) }

            val result = registry.remove(active)

            assertEquals(GradingNodeChangeResult.Applied, result)
            assertEquals(listOf(setOf(active, inactive)), observed)
            assertEquals(setOf(inactive), coordinator.observedStatuses().keys)
            assertEquals(emptySet<GradingNodeAddress>(), coordinator.statuses().keys)
            assertEquals(listOf(inactive), registry.overview().nodes.map { entry -> entry.node.address })
        }

        @Test
        fun `should finish a removal whose exception hides a confirmed commit`() {
            val registry = registry()
            every { store.remove(active) } throws IllegalStateException("Commit failed")
            every { store.read() } returns listOf(StoredGradingNode(inactive, isActive = false))

            val result = registry.remove(active)

            assertEquals(GradingNodeChangeResult.Applied, result)
            assertEquals(setOf(inactive), coordinator.observedStatuses().keys)
        }

        @Test
        fun `should keep the node after a removal with a confirmed rollback`() {
            val registry = registry()
            every { store.remove(active) } throws IllegalStateException("Commit failed")
            every { store.read() } returns listOf(StoredGradingNode(active, isActive = true), StoredGradingNode(inactive, isActive = false))

            val result = registry.remove(active)

            assertEquals(GradingNodeChangeResult.Failed, result)
            assertEquals(setOf(active), coordinator.statuses().keys)
        }

        @Test
        fun `should keep the node and lock changes when a removal outcome cannot be read`() {
            val registry = registry()
            every { store.remove(active) } throws IllegalStateException("Commit failed")
            every { store.read() } throws IllegalStateException("Read failed")

            val result = registry.remove(active)

            assertEquals(GradingNodeChangeResult.Uncertain, result)
            assertEquals(setOf(active), coordinator.statuses().keys)
            assertEquals(setOf(active, inactive), coordinator.observedStatuses().keys)
            assertEquals(listOf(active, inactive), registry.overview().nodes.map { entry -> entry.node.address })
            assertEquals(SettingsPersistence.Uncertain, registry.overview().persistence)
        }

        @Test
        fun `should keep the node and lock changes when the record read after removal is unexpected`() {
            val registry = registry()
            every { store.remove(active) } throws IllegalStateException("Commit failed")
            every { store.read() } returns listOf(StoredGradingNode(active, isActive = false))

            val result = registry.remove(active)

            assertEquals(GradingNodeChangeResult.Uncertain, result)
            assertEquals(setOf(active), coordinator.statuses().keys)
            assertEquals(setOf(active, inactive), coordinator.observedStatuses().keys)
            assertEquals(SettingsPersistence.Uncertain, registry.overview().persistence)
        }

        @Test
        fun `should reject an unregistered address`() {
            val registry = registry()

            val result = registry.remove(added)

            assertEquals(GradingNodeChangeResult.Rejected(GradingNodeRejection.NotRegistered), result)
        }
    }

    @Nested
    inner class ImportPreviewTests {
        @Test
        fun `should list configured addresses in configuration order with their records`() {
            val registry = registry(configured = listOf(added, inactive, added))

            val preview = registry.importPreview()

            assertEquals(
                GradingNodeImportPreview(
                    listOf(
                        GradingNodeImportCandidate(added, existing = null),
                        GradingNodeImportCandidate(inactive, StoredGradingNode(inactive, isActive = false)),
                    ),
                ),
                preview,
            )
        }
    }

    @Nested
    inner class ImportNodesTests {
        @Test
        fun `should add selected new addresses and skip registered and unconfigured ones`() {
            val other = GradingNodeAddress("grader-4:50051")
            val unknown = GradingNodeAddress("grader-5:50051")
            val registry = registry(configured = listOf(other, active, added))
            every { store.add(any()) } returns true

            val report = registry.importNodes(mapOf(unknown to true, added to true, active to false, other to false))

            assertEquals(
                GradingNodeImportReport(
                    outcomes = listOf(
                        GradingNodeImportOutcome(other, GradingNodeChangeResult.Applied),
                        GradingNodeImportOutcome(active, GradingNodeChangeResult.Rejected(GradingNodeRejection.AlreadyRegistered)),
                        GradingNodeImportOutcome(added, GradingNodeChangeResult.Applied),
                        GradingNodeImportOutcome(unknown, GradingNodeChangeResult.Rejected(GradingNodeRejection.NotRegistered)),
                    ),
                    notAttempted = emptyList(),
                ),
                report,
            )
            assertEquals(setOf(active, added), coordinator.statuses().keys)
            assertEquals(setOf(active, inactive, added, other), coordinator.observedStatuses().keys)
        }

        @Test
        fun `should stop after an uncertain outcome and keep earlier additions`() {
            val other = GradingNodeAddress("grader-4:50051")
            val last = GradingNodeAddress("grader-5:50051")
            val registry = registry(configured = listOf(added, other, last))
            every { store.add(StoredGradingNode(added, isActive = true)) } returns true
            every { store.add(StoredGradingNode(other, isActive = true)) } throws IllegalStateException("Commit failed")
            every { store.read() } throws IllegalStateException("Read failed")

            val report = registry.importNodes(mapOf(added to true, other to true, last to true))

            assertEquals(
                GradingNodeImportReport(
                    outcomes = listOf(
                        GradingNodeImportOutcome(added, GradingNodeChangeResult.Applied),
                        GradingNodeImportOutcome(other, GradingNodeChangeResult.Uncertain),
                    ),
                    notAttempted = listOf(last),
                ),
                report,
            )
            assertEquals(setOf(active, added), coordinator.statuses().keys)
            verify(exactly = 0) { store.add(StoredGradingNode(last, isActive = true)) }
        }

        @Test
        fun `should attempt nothing after the registry is locked`() {
            val other = GradingNodeAddress("grader-4:50051")
            val registry = registry(configured = listOf(added, other))
            lockChanges(registry)

            val report = registry.importNodes(mapOf(added to true, other to true))

            assertEquals(
                GradingNodeImportReport(
                    outcomes = listOf(GradingNodeImportOutcome(added, GradingNodeChangeResult.Uncertain)),
                    notAttempted = listOf(other),
                ),
                report,
            )
            assertEquals(setOf(active, inactive), coordinator.observedStatuses().keys)
            verify(exactly = 0) { store.add(any()) }
        }
    }

    @Nested
    inner class ConfiguredStoreTests {
        @Test
        fun `should keep configured nodes participating and apply changes in memory`() {
            val configured = listOf(active, added)
            configured.forEach(coordinator::addNode)
            val registry = GradingNodeRegistryCoordinator(
                coordinator = coordinator,
                store = ConfiguredGradingNodeStore(configured),
                configured = configured,
            )

            val result = registry.setActive(added, isActive = false)

            assertEquals(GradingNodeChangeResult.Applied, result)
            assertEquals(setOf(active), coordinator.statuses().keys)
            assertEquals(
                listOf(
                    GradingNodeImportCandidate(active, StoredGradingNode(active, isActive = true)),
                    GradingNodeImportCandidate(added, StoredGradingNode(added, isActive = false)),
                ),
                registry.importPreview().candidates,
            )
        }
    }

    @Nested
    inner class TechnicalFailureTests {
        @Test
        fun `should count a node that cannot start`() {
            val registry = registry()
            val failing = GradingNodeAddress("invalid")
            every { factory.create(failing) } throws IllegalArgumentException("Invalid target")

            registry.add(failing.target, isActive = true)

            assertEquals(1.0, failures("start", "IllegalArgumentException"))
        }

        @Test
        fun `should count a write failure once when its outcome cannot be read`() {
            val registry = registry()
            every { store.add(any()) } throws IllegalStateException("Commit failed")
            every { store.read() } throws IllegalStateException("Read failed")

            registry.add(added.target, isActive = true)

            assertEquals(1.0, failures("write", "IllegalStateException"))
            assertEquals(1.0, meters.find(TestSysMetrics.TECHNICAL_FAILURES).counters().sumOf { counter -> counter.count() })
        }

        @Test
        fun `should count a stored record that does not match the change`() {
            val registry = registry()
            every { store.setActive(inactive, isActive = true) } returns false

            registry.setActive(inactive, isActive = true)

            assertEquals(1.0, failures("record_mismatch", "none"))
        }

        @Test
        fun `should not count changes refused after the registry is locked`() {
            val registry = registry()
            lockChanges(registry)

            registry.add(added.target, isActive = true)
            registry.setActive(inactive, isActive = true)

            assertEquals(1.0, meters.find(TestSysMetrics.TECHNICAL_FAILURES).counters().sumOf { counter -> counter.count() })
        }

        private fun failures(stage: String, exception: String): Double = meters.find(TestSysMetrics.TECHNICAL_FAILURES)
            .tags(TestSysMetrics.SOURCE, "nodes", TestSysMetrics.STAGE, stage, TestSysMetrics.EXCEPTION, exception)
            .counter()?.count() ?: 0.0
    }

    private fun registry(configured: List<GradingNodeAddress> = emptyList()): GradingNodeRegistryCoordinator {
        coordinator.addNode(active)
        coordinator.observe(inactive)
        return GradingNodeRegistryCoordinator(coordinator, store, configured, TechnicalFailures(meters))
    }

    /** Locks [registry] through a removal of the inactive node whose outcome cannot be read. */
    private fun lockChanges(registry: GradingNodeRegistryCoordinator) {
        every { store.remove(inactive) } throws IllegalStateException("Commit failed")
        every { store.read() } throws IllegalStateException("Read failed")
        registry.remove(inactive)
    }
}
