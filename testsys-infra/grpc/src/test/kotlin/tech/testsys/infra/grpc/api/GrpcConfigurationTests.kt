@file:OptIn(InternalGrpcApi::class)

package tech.testsys.infra.grpc.api

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.core.env.MapPropertySource
import org.springframework.core.env.StandardEnvironment
import org.springframework.core.env.SystemEnvironmentPropertySource
import org.springframework.core.io.support.ResourcePropertySource
import tech.testsys.domain.contract.GradingNodeAddress
import tech.testsys.domain.contract.GradingNodeStatus
import tech.testsys.infra.grpc.internal.FakeNode
import tech.testsys.infra.grpc.internal.InternalGrpcApi
import tech.testsys.infra.grpc.internal.NodeClientFactory
import tech.testsys.infra.grpc.internal.settings

internal class GrpcConfigurationTests {
    private val configuration = GrpcConfiguration()
    private val environment = StandardEnvironment().apply {
        propertySources.addFirst(ResourcePropertySource("classpath:grading-defaults.properties"))
    }

    @Nested
    inner class GradingSettingsTests {
        @Test
        fun `should enable video recording from the module defaults`() {
            val settings = configuration.gradingSettings(environment)

            assertTrue(settings.shouldRecordVideo)
        }

        @Test
        fun `should disable video recording when the environment overrides its default`() {
            environment.propertySources.addFirst(MapPropertySource("override", mapOf("testsys.grading.should-record-video" to "false")))

            val settings = configuration.gradingSettings(environment)

            assertFalse(settings.shouldRecordVideo)
        }

        @Test
        fun `should configure no nodes from the module defaults`() {
            val settings = configuration.gradingSettings(environment)

            assertEquals(emptyList<GradingNodeAddress>(), settings.nodes)
        }

        @ParameterizedTest
        @ValueSource(strings = ["", "  ", "\t"])
        fun `should configure no nodes when the setting is blank`(value: String) {
            overrideNodes(value)

            val settings = configuration.gradingSettings(environment)

            assertEquals(emptyList<GradingNodeAddress>(), settings.nodes)
        }

        @Test
        fun `should configure a single node from an environment override`() {
            overrideNodes("grader:50051")

            val settings = configuration.gradingSettings(environment)

            assertEquals(listOf(GradingNodeAddress("grader:50051")), settings.nodes)
        }

        @Test
        fun `should trim multiple addresses without changing grpc targets`() {
            overrideNodes(" grader-1:50051, dns:///grader-2:50051 , [::1]:50051 ")

            val settings = configuration.gradingSettings(environment)

            assertEquals(
                listOf(
                    GradingNodeAddress("grader-1:50051"),
                    GradingNodeAddress("dns:///grader-2:50051"),
                    GradingNodeAddress("[::1]:50051"),
                ),
                settings.nodes,
            )
        }

        @Test
        fun `should configure nodes from the environment variable`() {
            environment.propertySources.addFirst(
                SystemEnvironmentPropertySource("testEnvironment", mapOf("TESTSYS_GRADING_NODES" to "grader-1:50051,grader-2:50051")),
            )

            val settings = configuration.gradingSettings(environment)

            assertEquals(listOf(GradingNodeAddress("grader-1:50051"), GradingNodeAddress("grader-2:50051")), settings.nodes)
        }

        @ParameterizedTest
        @ValueSource(strings = [",grader:50051", "grader:50051,", "grader-1:50051,,grader-2:50051", "grader:50051,  ", ","])
        fun `should reject an empty address in a nonempty node list`(value: String) {
            overrideNodes(value)

            val error = assertThrows<IllegalArgumentException> { configuration.gradingSettings(environment) }

            assertTrue(error.message.orEmpty().contains("testsys.grading.nodes"))
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
                settings = settings.copy(nodes = listOf(firstAddress, secondAddress)),
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
        fun `should create only one client for duplicate configured addresses`() {
            overrideNodes("grader:50051, grader:50051 ")
            val created = mutableListOf<GradingNodeAddress>()
            val factory = NodeClientFactory { address ->
                created.add(address)
                FakeNode()
            }

            configuration.gradingCoordinator(
                persistence = mockk(),
                settings = configuration.gradingSettings(environment),
                parser = mockk(),
                nodeClientFactory = factory,
            ).use { coordinator ->
                assertEquals(listOf(GradingNodeAddress("grader:50051")), created)
                assertEquals(mapOf(GradingNodeAddress("grader:50051") to GradingNodeStatus.Unknown), coordinator.statuses())
            }
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
                settings = settings.copy(nodes = listOf(GradingNodeAddress("grader-1:50051"), GradingNodeAddress("grader-2:50051"))),
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
                    settings = settings.copy(nodes = listOf(GradingNodeAddress("grader-1:50051"), GradingNodeAddress("invalid"))),
                    parser = mockk(),
                    nodeClientFactory = factory,
                )
            }

            assertSame(failure, error)
            assertTrue(firstNode.isClosed.get())
        }
    }

    private fun overrideNodes(value: String) {
        environment.propertySources.addFirst(MapPropertySource("override", mapOf("testsys.grading.nodes" to value)))
    }
}
