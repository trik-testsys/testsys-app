package tech.testsys.infra.grpc.internal

import tech.testsys.domain.contract.GradingNodeAddress
import tech.testsys.infra.settings.api.registry.GradingNodeStore
import tech.testsys.infra.settings.api.registry.StoredGradingNode
import java.util.TreeMap

/** In-memory registry of a grader without durable storage: configured nodes participate, changes last until restart. */
@InternalGrpcApi
internal class ConfiguredGradingNodeStore(configured: List<GradingNodeAddress>) : GradingNodeStore {
    private val nodes = TreeMap<String, Boolean>().apply { configured.forEach { address -> put(address.target, true) } }

    @Synchronized
    override fun read(): List<StoredGradingNode> =
        nodes.map { (target, isActive) -> StoredGradingNode(GradingNodeAddress(target), isActive) }

    @Synchronized
    override fun add(node: StoredGradingNode): Boolean = nodes.getOrPut(node.address.target) { node.isActive } == node.isActive

    @Synchronized
    override fun setActive(address: GradingNodeAddress, isActive: Boolean): Boolean =
        nodes.computeIfPresent(address.target) { _, _ -> isActive } != null

    @Synchronized
    override fun remove(address: GradingNodeAddress) {
        nodes.remove(address.target)
    }
}
