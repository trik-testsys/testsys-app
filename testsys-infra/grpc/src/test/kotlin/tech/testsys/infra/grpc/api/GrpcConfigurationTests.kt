@file:OptIn(InternalGrpcApi::class)

package tech.testsys.infra.grpc.api

import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.boot.context.properties.bind.BindException
import org.springframework.core.NestedExceptionUtils
import org.springframework.core.env.MapPropertySource
import org.springframework.core.env.StandardEnvironment
import org.springframework.core.env.SystemEnvironmentPropertySource
import org.springframework.core.io.support.ResourcePropertySource
import tech.testsys.domain.contract.GradingNodeAddress
import tech.testsys.domain.contract.GradingNodeStatus
import tech.testsys.domain.model.group.CommunityId
import tech.testsys.infra.grpc.internal.FakeNode
import tech.testsys.infra.grpc.internal.GradingProperties
import tech.testsys.infra.grpc.internal.InternalGrpcApi
import tech.testsys.infra.grpc.internal.NodeClientFactory
import tech.testsys.infra.grpc.internal.nodeSettings
import tech.testsys.infra.monitoring.api.TestSysMetrics
import tech.testsys.infra.settings.api.RuntimeSettings
import tech.testsys.infra.settings.api.SettingsSnapshot
import tech.testsys.infra.settings.api.catalog.SettingKey
import tech.testsys.infra.settings.api.catalog.SettingsValues
import tech.testsys.infra.settings.api.registry.GradingNodeStore
import tech.testsys.infra.settings.api.registry.StoredGradingNode
import java.time.Duration

internal class GrpcConfigurationTests {
    private val configuration = GrpcConfiguration()
    private val environment = StandardEnvironment().apply {
        propertySources.addFirst(ResourcePropertySource("classpath:grading-defaults.properties"))
    }

    @Nested
    inner class GradingSettingsTests {
        @Test
        fun `should enable video recording from the module defaults`() {
            val properties = configuration.gradingSettings(environment).current()

            assertTrue(properties.shouldRecordVideo)
        }

        @Test
        fun `should disable video recording when the environment overrides its default`() {
            environment.propertySources.addFirst(MapPropertySource("override", mapOf("testsys.grading.should-record-video" to "false")))

            val properties = configuration.gradingSettings(environment).current()

            assertFalse(properties.shouldRecordVideo)
        }

        @Test
        fun `should configure no nodes from the module defaults`() {
            val properties = configuration.gradingSettings(environment).current()

            assertEquals(emptyList<GradingNodeAddress>(), properties.addresses)
        }

        @ParameterizedTest
        @ValueSource(strings = ["", "  ", "\t"])
        fun `should configure no nodes when the setting is blank`(value: String) {
            overrideNodes(value)

            val properties = configuration.gradingSettings(environment).current()

            assertEquals(emptyList<GradingNodeAddress>(), properties.addresses)
        }

        @Test
        fun `should configure a single node from an environment override`() {
            overrideNodes("grader:50051")

            val properties = configuration.gradingSettings(environment).current()

            assertEquals(listOf(GradingNodeAddress("grader:50051")), properties.addresses)
        }

        @Test
        fun `should trim multiple addresses without changing grpc targets`() {
            overrideNodes(" grader-1:50051, dns:///grader-2:50051 , [::1]:50051 ")

            val properties = configuration.gradingSettings(environment).current()

            assertEquals(
                listOf(
                    GradingNodeAddress("grader-1:50051"),
                    GradingNodeAddress("dns:///grader-2:50051"),
                    GradingNodeAddress("[::1]:50051"),
                ),
                properties.addresses,
            )
        }

        @Test
        fun `should configure nodes from the environment variable`() {
            environment.propertySources.addFirst(
                SystemEnvironmentPropertySource("testEnvironment", mapOf("TESTSYS_GRADING_NODES" to "grader-1:50051,grader-2:50051")),
            )

            val properties = configuration.gradingSettings(environment).current()

            assertEquals(listOf(GradingNodeAddress("grader-1:50051"), GradingNodeAddress("grader-2:50051")), properties.addresses)
        }

        @ParameterizedTest
        @ValueSource(strings = [",grader:50051", "grader:50051,", "grader-1:50051,,grader-2:50051", "grader:50051,  ", ","])
        fun `should reject an empty address in a nonempty node list`(value: String) {
            overrideNodes(value)

            val error = assertThrows<BindException> { configuration.gradingSettings(environment) }

            assertTrue(NestedExceptionUtils.getRootCause(error)?.message.orEmpty().contains("testsys.grading.nodes"))
        }

        @Test
        fun `should not import nodes by default`() {
            val properties = configuration.gradingSettings(environment).current()

            assertFalse(properties.shouldImportNodes)
        }

        @Test
        fun `should enable node import from the environment variable`() {
            environment.propertySources.addFirst(
                SystemEnvironmentPropertySource("testEnvironment", mapOf("TESTSYS_GRADING_IMPORT_NODES" to "true")),
            )

            val properties = configuration.gradingSettings(environment).current()

            assertTrue(properties.shouldImportNodes)
        }

        @Test
        fun `should reject an invalid node import flag`() {
            environment.propertySources.addFirst(MapPropertySource("override", mapOf("testsys.grading.import-nodes" to "sometimes")))

            assertThrows<BindException> { configuration.gradingSettings(environment) }
        }

        @Test
        fun `should read saved values over the environment and the active message limit with the shared holder`() {
            overrideNodes("grader:50051")
            val values = SettingsValues(
                mapOf(
                    SettingKey.GradingMaxAttempts to "1",
                    SettingKey.TotalTimeout to "PT1M",
                    SettingKey.RpcTimeout to "PT2S",
                    SettingKey.StatusTimeout to "PT3S",
                    SettingKey.PollInterval to "PT4S",
                    SettingKey.MaxMessageBytes to "100",
                    SettingKey.ShouldRecordVideo to "false",
                ),
            )
            val snapshot = SettingsSnapshot(revision = 0, values = values, activeMaxMessageBytes = 200)
            val runtime = RuntimeSettings(snapshot, CommunityId(42))

            val properties = configuration.gradingSettings(environment, runtime).current()

            assertEquals(
                GradingProperties(
                    nodes = "grader:50051",
                    shouldImportNodes = false,
                    maxAttempts = 1,
                    totalTimeout = Duration.ofMinutes(1),
                    rpcTimeout = Duration.ofSeconds(2),
                    statusTimeout = Duration.ofSeconds(3),
                    pollInterval = Duration.ofSeconds(4),
                    maxMessageBytes = 200,
                    shouldRecordVideo = false,
                ),
                properties,
            )
            assertEquals(listOf(GradingNodeAddress("grader:50051")), properties.addresses)
        }
    }

    @Nested
    inner class GradingCoordinatorTests {
        @Test
        fun `should register configured nodes before starting background work`() {
            val firstAddress = GradingNodeAddress("grader-1:50051")
            val secondAddress = GradingNodeAddress("grader-2:50051")
            val firstNode = FakeNode()
            val secondNode = FakeNode()
            val factory = mockk<NodeClientFactory>()
            every { factory.create(firstAddress) } returns firstNode
            every { factory.create(secondAddress) } returns secondNode

            configuration.gradingCoordinator(
                persistence = mockk(),
                environment = environment,
                settings = nodeSettings(listOf(firstAddress, secondAddress)),
                parser = mockk(),
                nodeClientFactory = factory,
            ).use { coordinator ->
                assertEquals(
                    mapOf(firstAddress to GradingNodeStatus.Unknown, secondAddress to GradingNodeStatus.Unknown),
                    coordinator.statuses(),
                )
                assertEquals(0, firstNode.polls.get())
                assertEquals(0, secondNode.polls.get())
            }
        }

        @Test
        fun `should create one status and one grading client for duplicate configured addresses`() {
            overrideNodes("grader:50051, grader:50051 ")
            val created = mutableListOf<GradingNodeAddress>()
            val factory = NodeClientFactory { address ->
                created.add(address)
                FakeNode()
            }

            configuration.gradingCoordinator(
                persistence = mockk(),
                environment = environment,
                settings = configuration.gradingSettings(environment),
                parser = mockk(),
                nodeClientFactory = factory,
            ).use { coordinator ->
                assertEquals(listOf(GradingNodeAddress("grader:50051"), GradingNodeAddress("grader:50051")), created)
                assertEquals(mapOf(GradingNodeAddress("grader:50051") to GradingNodeStatus.Unknown), coordinator.statuses())
            }
        }

        @Test
        fun `should observe all stored nodes and register only active ones instead of configured nodes`() {
            val store = mockk<GradingNodeStore> {
                every { read() } returns listOf(
                    StoredGradingNode(GradingNodeAddress("grader-1:50051"), isActive = true),
                    StoredGradingNode(GradingNodeAddress("grader-2:50051"), isActive = false),
                )
            }

            configuration.gradingCoordinator(
                persistence = mockk(),
                environment = environment,
                settings = nodeSettings(listOf(GradingNodeAddress("configured:50051"))),
                parser = mockk(),
                nodeClientFactory = NodeClientFactory { FakeNode() },
                nodeStore = store,
            ).use { coordinator ->
                assertEquals(mapOf(GradingNodeAddress("grader-1:50051") to GradingNodeStatus.Unknown), coordinator.statuses())
                assertEquals(
                    setOf(GradingNodeAddress("grader-1:50051"), GradingNodeAddress("grader-2:50051")),
                    coordinator.observedStatuses().keys,
                )
            }
        }

        @Test
        fun `should start with an empty stored registry despite configured nodes`() {
            val store = mockk<GradingNodeStore> { every { read() } returns emptyList() }

            configuration.gradingCoordinator(
                persistence = mockk(),
                environment = environment,
                settings = nodeSettings(listOf(GradingNodeAddress("configured:50051"))),
                parser = mockk(),
                nodeClientFactory = NodeClientFactory { FakeNode() },
                nodeStore = store,
            ).use { coordinator ->
                assertEquals(emptyMap<GradingNodeAddress, GradingNodeStatus>(), coordinator.observedStatuses())
            }
        }

        @Test
        fun `should abort creation when the stored registry cannot be read`() {
            val failure = IllegalStateException("Database unavailable")
            val store = mockk<GradingNodeStore> { every { read() } throws failure }
            val factory = mockk<NodeClientFactory>()

            val error = assertThrows<IllegalStateException> {
                configuration.gradingCoordinator(
                    persistence = mockk(),
                    environment = environment,
                    settings = nodeSettings(listOf(GradingNodeAddress("configured:50051"))),
                    parser = mockk(),
                    nodeClientFactory = factory,
                    nodeStore = store,
                )
            }

            assertSame(failure, error)
            verify(exactly = 0) { factory.create(any()) }
        }

        @Test
        fun `should close configured clients when the coordinator closes`() {
            val firstNode = FakeNode()
            val secondNode = FakeNode()
            val factory = mockk<NodeClientFactory>()
            every { factory.create(GradingNodeAddress("grader-1:50051")) } returns firstNode
            every { factory.create(GradingNodeAddress("grader-2:50051")) } returns secondNode
            val coordinator = configuration.gradingCoordinator(
                persistence = mockk(),
                environment = environment,
                settings = nodeSettings(listOf(GradingNodeAddress("grader-1:50051"), GradingNodeAddress("grader-2:50051"))),
                parser = mockk(),
                nodeClientFactory = factory,
            )

            coordinator.close()

            assertTrue(firstNode.isClosed.get())
            assertTrue(secondNode.isClosed.get())
            assertEquals(emptyMap<GradingNodeAddress, GradingNodeStatus>(), coordinator.statuses())
        }

        @Test
        fun `should close registered clients when a later client cannot be created`() {
            val firstNode = FakeNode()
            val failure = IllegalArgumentException("Invalid target")
            val factory = mockk<NodeClientFactory>()
            every { factory.create(GradingNodeAddress("grader-1:50051")) } returns firstNode
            every { factory.create(GradingNodeAddress("invalid")) } throws failure

            val error = assertThrows<IllegalArgumentException> {
                configuration.gradingCoordinator(
                    persistence = mockk(),
                    environment = environment,
                    settings = nodeSettings(listOf(GradingNodeAddress("grader-1:50051"), GradingNodeAddress("invalid"))),
                    parser = mockk(),
                    nodeClientFactory = factory,
                )
            }

            assertSame(failure, error)
            assertTrue(firstNode.isClosed.get())
        }

        @Test
        fun `should add absent configured nodes as active when importing at startup`() {
            val stored = GradingNodeAddress("stored:50051")
            val first = GradingNodeAddress("configured-1:50051")
            val second = GradingNodeAddress("configured-2:50051")
            val store = mockk<GradingNodeStore> {
                every { read() } returns listOf(StoredGradingNode(stored, isActive = true))
                every { add(any()) } returns true
            }

            configuration.gradingCoordinator(
                persistence = mockk(),
                environment = environment,
                settings = nodeSettings(listOf(first, second), shouldImportNodes = true),
                parser = mockk(),
                nodeClientFactory = NodeClientFactory { FakeNode() },
                nodeStore = store,
            ).use { coordinator ->
                assertEquals(setOf(stored, first, second), coordinator.statuses().keys)
                verifyOrder {
                    store.add(StoredGradingNode(first, isActive = true))
                    store.add(StoredGradingNode(second, isActive = true))
                }
            }
        }

        @Test
        fun `should enable inactive configured nodes when importing at startup`() {
            val address = GradingNodeAddress("configured:50051")
            val store = mockk<GradingNodeStore> {
                every { read() } returns listOf(StoredGradingNode(address, isActive = false))
                every { setActive(address, isActive = true) } returns true
            }

            configuration.gradingCoordinator(
                persistence = mockk(),
                environment = environment,
                settings = nodeSettings(listOf(address), shouldImportNodes = true),
                parser = mockk(),
                nodeClientFactory = NodeClientFactory { FakeNode() },
                nodeStore = store,
            ).use { coordinator ->
                assertEquals(setOf(address), coordinator.statuses().keys)
                verify(exactly = 1) { store.setActive(address, isActive = true) }
                verify(exactly = 0) { store.add(any()) }
            }
        }

        @Test
        fun `should leave active configured nodes and nodes outside the configuration unchanged when importing`() {
            val configured = GradingNodeAddress("configured:50051")
            val active = GradingNodeAddress("manual-active:50051")
            val inactive = GradingNodeAddress("manual-inactive:50051")
            val store = mockk<GradingNodeStore> {
                every { read() } returns listOf(
                    StoredGradingNode(configured, isActive = true),
                    StoredGradingNode(active, isActive = true),
                    StoredGradingNode(inactive, isActive = false),
                )
            }

            configuration.gradingCoordinator(
                persistence = mockk(),
                environment = environment,
                settings = nodeSettings(listOf(configured), shouldImportNodes = true),
                parser = mockk(),
                nodeClientFactory = NodeClientFactory { FakeNode() },
                nodeStore = store,
            ).use { coordinator ->
                assertEquals(setOf(configured, active), coordinator.statuses().keys)
                assertEquals(setOf(configured, active, inactive), coordinator.observedStatuses().keys)
                verify(exactly = 0) { store.add(any()) }
                verify(exactly = 0) { store.setActive(any(), any()) }
            }
        }

        @Test
        fun `should not write configured nodes without the import flag`() {
            val inactive = GradingNodeAddress("configured-inactive:50051")
            val absent = GradingNodeAddress("configured-absent:50051")
            val store = mockk<GradingNodeStore> {
                every { read() } returns listOf(StoredGradingNode(inactive, isActive = false))
            }

            configuration.gradingCoordinator(
                persistence = mockk(),
                environment = environment,
                settings = nodeSettings(listOf(inactive, absent)),
                parser = mockk(),
                nodeClientFactory = NodeClientFactory { FakeNode() },
                nodeStore = store,
            ).use { coordinator ->
                assertEquals(emptyMap<GradingNodeAddress, GradingNodeStatus>(), coordinator.statuses())
                assertEquals(setOf(inactive), coordinator.observedStatuses().keys)
                verify(exactly = 0) { store.add(any()) }
                verify(exactly = 0) { store.setActive(any(), any()) }
            }
        }

        @Test
        fun `should ignore the import flag without a store`() {
            val address = GradingNodeAddress("configured:50051")

            configuration.gradingCoordinator(
                persistence = mockk(),
                environment = environment,
                settings = nodeSettings(listOf(address), shouldImportNodes = true),
                parser = mockk(),
                nodeClientFactory = NodeClientFactory { FakeNode() },
            ).use { coordinator ->
                assertEquals(mapOf(address to GradingNodeStatus.Unknown), coordinator.statuses())
            }
        }

        @Test
        fun `should abort startup and close clients when an import write fails`() {
            val stored = GradingNodeAddress("stored:50051")
            val configured = GradingNodeAddress("configured:50051")
            val storedNode = FakeNode()
            val configuredNode = FakeNode()
            val failure = IllegalStateException("Database unavailable")
            val store = mockk<GradingNodeStore> {
                every { read() } returns listOf(StoredGradingNode(stored, isActive = true))
                every { add(any()) } throws failure
            }
            val factory = mockk<NodeClientFactory> {
                every { create(stored) } returns storedNode
                every { create(configured) } returns configuredNode
            }

            val error = assertThrows<IllegalStateException> {
                configuration.gradingCoordinator(
                    persistence = mockk(),
                    environment = environment,
                    settings = nodeSettings(listOf(configured), shouldImportNodes = true),
                    parser = mockk(),
                    nodeClientFactory = factory,
                    nodeStore = store,
                )
            }

            assertSame(failure, error)
            assertTrue(storedNode.isClosed.get())
            assertTrue(configuredNode.isClosed.get())
        }

        @Test
        fun `should abort startup when the stored record differs from the imported node`() {
            val address = GradingNodeAddress("configured:50051")
            val node = FakeNode()
            val store = mockk<GradingNodeStore> {
                every { read() } returns emptyList()
                every { add(StoredGradingNode(address, isActive = true)) } returns false
            }

            val error = assertThrows<IllegalStateException> {
                configuration.gradingCoordinator(
                    persistence = mockk(),
                    environment = environment,
                    settings = nodeSettings(listOf(address), shouldImportNodes = true),
                    parser = mockk(),
                    nodeClientFactory = NodeClientFactory { node },
                    nodeStore = store,
                )
            }

            assertTrue(error.message.orEmpty().contains("configured:50051"))
            assertTrue(node.isClosed.get())
        }

        @Test
        fun `should abort startup when an inactive configured node disappears before enabling`() {
            val address = GradingNodeAddress("configured:50051")
            val node = FakeNode()
            val store = mockk<GradingNodeStore> {
                every { read() } returns listOf(StoredGradingNode(address, isActive = false))
                every { setActive(address, isActive = true) } returns false
            }

            val error = assertThrows<IllegalStateException> {
                configuration.gradingCoordinator(
                    persistence = mockk(),
                    environment = environment,
                    settings = nodeSettings(listOf(address), shouldImportNodes = true),
                    parser = mockk(),
                    nodeClientFactory = NodeClientFactory { node },
                    nodeStore = store,
                )
            }

            assertTrue(error.message.orEmpty().contains("configured:50051"))
            assertTrue(node.isClosed.get())
        }

        @Test
        fun `should abort startup without writing when a configured client cannot be created`() {
            val address = GradingNodeAddress("configured:50051")
            val failure = IllegalArgumentException("Invalid target")
            val store = mockk<GradingNodeStore> { every { read() } returns emptyList() }
            val factory = mockk<NodeClientFactory> { every { create(address) } throws failure }

            val error = assertThrows<IllegalArgumentException> {
                configuration.gradingCoordinator(
                    persistence = mockk(),
                    environment = environment,
                    settings = nodeSettings(listOf(address), shouldImportNodes = true),
                    parser = mockk(),
                    nodeClientFactory = factory,
                    nodeStore = store,
                )
            }

            assertSame(failure, error)
            verify(exactly = 0) { store.add(any()) }
        }

        @Test
        fun `should reject an overlong configured address before creating clients when importing`() {
            val store = mockk<GradingNodeStore> { every { read() } returns emptyList() }
            val factory = mockk<NodeClientFactory>()

            val error = assertThrows<IllegalArgumentException> {
                configuration.gradingCoordinator(
                    persistence = mockk(),
                    environment = environment,
                    settings = nodeSettings(listOf(GradingNodeAddress("a".repeat(256))), shouldImportNodes = true),
                    parser = mockk(),
                    nodeClientFactory = factory,
                    nodeStore = store,
                )
            }

            assertTrue(error.message.orEmpty().contains("testsys.grading.nodes"))
            verify(exactly = 0) { factory.create(any()) }
            verify(exactly = 0) { store.add(any()) }
        }
    }

    @Nested
    inner class GradingNodeRegistryTests {
        @Test
        fun `should show configured nodes as registered participants without a store`() {
            val address = GradingNodeAddress("grader:50051")
            val configured = nodeSettings(listOf(address))

            configuration.gradingCoordinator(
                persistence = mockk(),
                environment = environment,
                settings = configured,
                parser = mockk(),
                nodeClientFactory = NodeClientFactory { FakeNode() },
            ).use { coordinator ->
                val registry = configuration.gradingNodeRegistry(coordinator, configured)

                assertEquals(
                    listOf(GradingNodeEntry(StoredGradingNode(address, isActive = true), GradingNodeStatus.Unknown)),
                    registry.overview().nodes,
                )
                assertEquals(
                    GradingNodeImportPreview(listOf(GradingNodeImportCandidate(address, StoredGradingNode(address, isActive = true)))),
                    registry.importPreview(),
                )
            }
        }
    }

    @Nested
    inner class GradingMeterBinderTests {
        @Test
        fun `should expose the queue and the configured nodes of the grader as gauges`() {
            val address = GradingNodeAddress("grader:50051")
            val configured = nodeSettings(listOf(address))
            val meters = SimpleMeterRegistry()

            configuration.gradingCoordinator(
                persistence = mockk(),
                environment = environment,
                settings = configured,
                parser = mockk(),
                nodeClientFactory = NodeClientFactory { FakeNode() },
            ).use { coordinator ->
                val registry = configuration.gradingNodeRegistry(coordinator, configured)

                configuration.gradingMeterBinder(configuration.gradingQueueMonitor(coordinator), registry).bindTo(meters)

                assertEquals(0.0, meters.get(TestSysMetrics.GRADING_QUEUE).tag(TestSysMetrics.STATE, "waiting").gauge().value())
                assertEquals(
                    1.0,
                    meters.get(TestSysMetrics.GRADING_NODES)
                        .tags(TestSysMetrics.PARTICIPATION, "active", TestSysMetrics.STATUS, "unknown").gauge().value(),
                )
            }
        }
    }

    private fun overrideNodes(value: String) {
        environment.propertySources.addFirst(MapPropertySource("override", mapOf("testsys.grading.nodes" to value)))
    }
}
