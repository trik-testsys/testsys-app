@file:OptIn(InternalGrpcApi::class)

package tech.testsys.infra.grpc.internal

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
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
            manager.add(address)
            manager.poll()
            val node = checkNotNull(manager.find(address))
            manager.markUnavailable(address = address, node = node, reason = "Transport failed")

            callbacks[0](GradingNodeStatus.Available(queued = 0, capacity = 1))

            assertEquals(GradingNodeStatus.Unreachable("Transport failed"), manager.statuses()[address])
            manager.poll()
            assertEquals(2, callbacks.size)
            callbacks[1](GradingNodeStatus.Available(queued = 0, capacity = 1))
            assertEquals(GradingNodeStatus.Available(queued = 0, capacity = 1), manager.statuses()[address])
            manager.close()
        }

        @Test
        @Tag("regression")
        fun `should keep a node unavailable after a fresh status poll fails`() {
            val client = mockk<NodeClient>(relaxed = true)
            val callbacks = mutableListOf<(GradingNodeStatus) -> Unit>()
            every { client.poll(capture(callbacks)) } returns Unit
            val manager = GradingNodeManager(NodeClientFactory { client })
            manager.add(address)
            manager.markUnavailable(address = address, node = checkNotNull(manager.find(address)), reason = "Transport failed")

            manager.poll()
            callbacks.single()(GradingNodeStatus.Unreachable("Poll failed"))

            assertEquals(GradingNodeStatus.Unreachable("Poll failed"), manager.statuses()[address])
            manager.close()
        }
    }

    @Nested
    inner class MarkUnavailableTests {
        @Test
        @Tag("regression")
        fun `should preserve a replacement registration when the previous node becomes unavailable`() {
            val manager = GradingNodeManager(NodeClientFactory { FakeNode() })
            manager.add(address)
            val previous = checkNotNull(manager.find(address))
            manager.remove(address)
            manager.add(address)
            manager.poll()

            manager.markUnavailable(address = address, node = previous, reason = "Old transport failed")

            assertInstanceOf(GradingNodeStatus.Available::class.java, manager.statuses()[address])
            manager.close()
        }
    }
}
