@file:OptIn(InternalGrpcApi::class)

package tech.testsys.infra.grpc.internal

import io.grpc.Status
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import tech.testsys.domain.contract.GradingNodeAddress
import tech.testsys.domain.contract.GradingNodeStatus

class GradingNodeManagerTests {
    private val address = GradingNodeAddress("node")

    @Nested
    inner class PollTests {
        @Test
        @Tag("regression")
        fun `should reject a status response started before a transport failure and accept a fresh poll`() {
            val client = mockk<NodeClient>(relaxed = true)
            val callbacks = mutableListOf<(GradingNodeStatus) -> Unit>()
            every { client.poll(capture(callbacks)) } returns Unit
            val manager = GradingNodeManager(NodeClientFactory { client })
            manager.observe(address)
            manager.register(address)
            manager.poll()
            val registration = checkNotNull(manager.registration(address))
            manager.markUnavailable(address = address, registration = registration, reason = "Transport failed")

            callbacks[0](GradingNodeStatus.Available(queued = 0, capacity = 1))

            assertEquals(GradingNodeStatus.Unreachable("Transport failed"), manager.participantStatuses()[address])
            manager.poll()
            assertEquals(2, callbacks.size)
            callbacks[1](GradingNodeStatus.Available(queued = 0, capacity = 1))
            assertEquals(GradingNodeStatus.Available(queued = 0, capacity = 1), manager.participantStatuses()[address])
            manager.close()
        }

        @Test
        @Tag("regression")
        fun `should keep a node unavailable after a fresh status poll fails`() {
            val client = mockk<NodeClient>(relaxed = true)
            val callbacks = mutableListOf<(GradingNodeStatus) -> Unit>()
            every { client.poll(capture(callbacks)) } returns Unit
            val manager = GradingNodeManager(NodeClientFactory { client })
            manager.observe(address)
            manager.register(address)
            val registration = checkNotNull(manager.registration(address))
            manager.markUnavailable(address = address, registration = registration, reason = "Transport failed")

            manager.poll()
            callbacks.single()(GradingNodeStatus.Unreachable("Poll failed"))

            assertEquals(GradingNodeStatus.Unreachable("Poll failed"), manager.participantStatuses()[address])
            manager.close()
        }

        @Test
        fun `should poll an observed node without participation`() {
            val node = FakeNode()
            val manager = GradingNodeManager(NodeClientFactory { node })
            manager.observe(address)

            manager.poll()

            assertEquals(1, node.polls.get())
            assertEquals(mapOf(address to GradingNodeStatus.Available(queued = 0, capacity = 1)), manager.observedStatuses())
            assertEquals(emptyMap<GradingNodeAddress, GradingNodeStatus>(), manager.participantStatuses())
            manager.close()
        }

        @Test
        fun `should reject a status response started before participation begins`() {
            val client = mockk<NodeClient>(relaxed = true)
            val callbacks = mutableListOf<(GradingNodeStatus) -> Unit>()
            every { client.poll(capture(callbacks)) } returns Unit
            val manager = GradingNodeManager(NodeClientFactory { client })
            manager.observe(address)
            manager.poll()

            manager.register(address)
            callbacks.single()(GradingNodeStatus.Available(queued = 0, capacity = 1))

            assertEquals(GradingNodeStatus.Unknown, manager.participantStatuses()[address])
            manager.close()
        }

        @Test
        fun `should describe a status poll that throws by its gRPC status code`() {
            val client = mockk<NodeClient>(relaxed = true)
            every { client.poll(any<(GradingNodeStatus) -> Unit>()) } throws
                Status.UNAVAILABLE.withDescription("Channel shutdown invoked").asRuntimeException()
            val manager = GradingNodeManager(NodeClientFactory { client })
            manager.observe(address)

            manager.poll()

            assertEquals(GradingNodeStatus.Unreachable("UNAVAILABLE"), manager.observedStatuses()[address])
            manager.close()
        }

        @Test
        fun `should describe a status poll that throws without a gRPC status as UNKNOWN`() {
            val client = mockk<NodeClient>(relaxed = true)
            every { client.poll(any<(GradingNodeStatus) -> Unit>()) } throws IllegalStateException("Channel is closed")
            val manager = GradingNodeManager(NodeClientFactory { client })
            manager.observe(address)

            manager.poll()

            assertEquals(GradingNodeStatus.Unreachable("UNKNOWN"), manager.observedStatuses()[address])
            manager.close()
        }
    }

    @Nested
    inner class RegisterTests {
        @Test
        fun `should use separate status and grading channels`() {
            val created = mutableListOf<FakeNode>()
            val manager = GradingNodeManager(NodeClientFactory { FakeNode().also(created::add) })
            manager.observe(address)

            manager.register(address)

            assertEquals(2, created.size)
            assertNotSame(created[0], checkNotNull(manager.registration(address)).client)
            manager.close()
        }

        @Test
        fun `should reset an observed status to unknown when participation begins`() {
            val manager = GradingNodeManager(NodeClientFactory { FakeNode() })
            manager.observe(address)
            manager.poll()

            val isRegistered = manager.register(address)

            assertTrue(isRegistered)
            assertEquals(mapOf(address to GradingNodeStatus.Unknown), manager.participantStatuses())
            manager.close()
        }

        @Test
        fun `should keep the registration and status when the same address is registered again`() {
            val manager = GradingNodeManager(NodeClientFactory { FakeNode() })
            manager.observe(address)
            manager.register(address)
            val registration = manager.registration(address)
            manager.poll()

            val isRegistered = manager.register(address)

            assertFalse(isRegistered)
            assertSame(registration, manager.registration(address))
            assertInstanceOf(GradingNodeStatus.Available::class.java, manager.participantStatuses()[address])
            manager.close()
        }
    }

    @Nested
    inner class UnregisterTests {
        @Test
        fun `should close only the grading channel and keep observing the node`() {
            val created = mutableListOf<FakeNode>()
            val manager = GradingNodeManager(NodeClientFactory { FakeNode().also(created::add) })
            manager.observe(address)
            manager.register(address)

            manager.unregister(address)
            manager.poll()

            assertFalse(created[0].isClosed.get())
            assertTrue(created[1].isClosed.get())
            assertEquals(1, created[0].polls.get())
            assertEquals(setOf(address), manager.observedStatuses().keys)
            assertEquals(emptyMap<GradingNodeAddress, GradingNodeStatus>(), manager.participantStatuses())
            manager.close()
        }
    }

    @Nested
    inner class ForgetTests {
        @Test
        fun `should close the status channel and stop polling a forgotten node`() {
            val node = FakeNode()
            val manager = GradingNodeManager(NodeClientFactory { node })
            manager.observe(address)

            manager.forget(address)
            manager.poll()

            assertTrue(node.isClosed.get())
            assertEquals(0, node.polls.get())
            assertEquals(emptyMap<GradingNodeAddress, GradingNodeStatus>(), manager.observedStatuses())
        }
    }

    @Nested
    inner class MarkUnavailableTests {
        @Test
        @Tag("regression")
        fun `should preserve a replacement registration when the previous node becomes unavailable`() {
            val manager = GradingNodeManager(NodeClientFactory { FakeNode() })
            manager.observe(address)
            manager.register(address)
            val previous = checkNotNull(manager.registration(address))
            manager.unregister(address)
            manager.register(address)
            manager.poll()

            manager.markUnavailable(address = address, registration = previous, reason = "Old transport failed")

            assertInstanceOf(GradingNodeStatus.Available::class.java, manager.participantStatuses()[address])
            manager.close()
        }

        @Test
        fun `should keep the observed status of a node without participation`() {
            val manager = GradingNodeManager(NodeClientFactory { FakeNode() })
            manager.observe(address)
            manager.register(address)
            val previous = checkNotNull(manager.registration(address))
            manager.unregister(address)
            manager.poll()

            manager.markUnavailable(address = address, registration = previous, reason = "Old transport failed")

            assertInstanceOf(GradingNodeStatus.Available::class.java, manager.observedStatuses()[address])
            manager.close()
        }
    }

    @Nested
    inner class CloseTests {
        @Test
        fun `should close status and grading channels`() {
            val created = mutableListOf<FakeNode>()
            val manager = GradingNodeManager(NodeClientFactory { FakeNode().also(created::add) })
            manager.observe(address)
            manager.register(address)

            manager.close()

            assertTrue(created.all { node -> node.isClosed.get() })
            assertEquals(emptyMap<GradingNodeAddress, GradingNodeStatus>(), manager.observedStatuses())
        }
    }
}
