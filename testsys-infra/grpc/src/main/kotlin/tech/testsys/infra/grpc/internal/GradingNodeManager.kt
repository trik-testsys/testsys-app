@file:OptIn(InternalGrpcApi::class)

package tech.testsys.infra.grpc.internal

import tech.testsys.domain.contract.GradingNodeAddress
import tech.testsys.domain.contract.GradingNodeStatus
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

@InternalGrpcApi
internal class RegisteredNode(val client: NodeClient) {
    val status = AtomicReference<GradingNodeStatus>(GradingNodeStatus.Unknown)
    val isPolling = AtomicBoolean(false)
}

@InternalGrpcApi
internal class GradingNodeManager(private val factory: NodeClientFactory) : AutoCloseable {
    private val nodes = ConcurrentHashMap<GradingNodeAddress, RegisteredNode>()

    fun add(address: GradingNodeAddress) {
        require(address.target.isNotBlank()) { "Checking node address must not be blank" }
        nodes.computeIfAbsent(address) { RegisteredNode(factory.create(address)) }
    }

    fun remove(address: GradingNodeAddress) {
        nodes.remove(address)?.client?.close()
    }

    fun statuses(): Map<GradingNodeAddress, GradingNodeStatus> = nodes.mapValues { (_, node) -> node.status.get() }

    fun find(address: GradingNodeAddress): RegisteredNode? = nodes[address]

    fun markUnavailable(address: GradingNodeAddress, node: RegisteredNode, reason: String) {
        nodes.computeIfPresent(address) { _, current ->
            if (current === node) {
                current.status.set(GradingNodeStatus.Unreachable(reason))
            }
            current
        }
    }

    fun poll() {
        nodes.values.forEach { node ->
            if (node.isPolling.compareAndSet(false, true)) {
                val previous = node.status.get()
                try {
                    node.client.poll { status ->
                        node.status.compareAndSet(previous, status)
                        node.isPolling.set(false)
                    }
                } catch (error: RuntimeException) {
                    node.status.compareAndSet(previous, GradingNodeStatus.Unreachable(error.message ?: "Status RPC failed"))
                    node.isPolling.set(false)
                }
            }
        }
    }

    override fun close() {
        nodes.keys.toList().forEach(::remove)
    }
}
