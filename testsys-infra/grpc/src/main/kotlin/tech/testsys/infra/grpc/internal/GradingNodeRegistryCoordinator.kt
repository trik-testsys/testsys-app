@file:OptIn(InternalGrpcApi::class)

package tech.testsys.infra.grpc.internal

import org.slf4j.LoggerFactory
import tech.testsys.domain.contract.GradingNodeAddress
import tech.testsys.domain.contract.GradingNodeStatus
import tech.testsys.infra.grpc.api.GradingNodeChangeResult
import tech.testsys.infra.grpc.api.GradingNodeEntry
import tech.testsys.infra.grpc.api.GradingNodeImportCandidate
import tech.testsys.infra.grpc.api.GradingNodeImportOutcome
import tech.testsys.infra.grpc.api.GradingNodeImportPreview
import tech.testsys.infra.grpc.api.GradingNodeImportReport
import tech.testsys.infra.grpc.api.GradingNodeRegistry
import tech.testsys.infra.grpc.api.GradingNodeRegistryOverview
import tech.testsys.infra.grpc.api.GradingNodeRejection
import tech.testsys.infra.monitoring.api.TechnicalFailures
import tech.testsys.infra.settings.api.registry.GradingNodeStore
import tech.testsys.infra.settings.api.registry.StoredGradingNode
import tech.testsys.infra.settings.api.service.SettingsPersistence
import java.util.concurrent.atomic.AtomicReference

/** Maximum length of a checking node address accepted by the registry. */
@InternalGrpcApi
internal const val MAX_GRADING_NODE_ADDRESS_LENGTH = 255

/**
 * Applies registry changes to the store and the running coordinator, starting observation or participation before a write
 * and stopping them only after a confirmed write. [failures] counts one technical failure per failed step, the read-back
 * that resolves a write exception included; an unreadable or unexpected read-back locks changes until restart.
 */
@InternalGrpcApi
internal class GradingNodeRegistryCoordinator(
    private val coordinator: GradingCoordinator,
    private val store: GradingNodeStore,
    configured: List<GradingNodeAddress>,
    private val failures: TechnicalFailures = TechnicalFailures.NONE,
) : GradingNodeRegistry {
    private val configured = configured.distinct()
    private val changes = Any()
    private val known = AtomicReference(KnownRegistry(startedNodes(coordinator), SettingsPersistence.Known))
    private val log = LoggerFactory.getLogger(javaClass)

    override fun overview(): GradingNodeRegistryOverview {
        val registry = known.get()
        val statuses = coordinator.observedStatuses()
        val entries = registry.nodes.entries.sortedBy { (address, _) -> address.target }.map { (address, isActive) ->
            GradingNodeEntry(StoredGradingNode(address, isActive), statuses[address] ?: GradingNodeStatus.Unknown)
        }
        return GradingNodeRegistryOverview(entries, registry.persistence)
    }

    override fun add(address: String, isActive: Boolean): GradingNodeChangeResult = synchronized(changes) {
        if (known.get().persistence == SettingsPersistence.Uncertain) return GradingNodeChangeResult.Uncertain
        val target = address.trim()
        when {
            target.isEmpty() -> return GradingNodeChangeResult.Rejected(GradingNodeRejection.Blank)
            target.length > MAX_GRADING_NODE_ADDRESS_LENGTH -> return GradingNodeChangeResult.Rejected(GradingNodeRejection.TooLong)
        }
        val node = StoredGradingNode(GradingNodeAddress(target), isActive)
        if (node.address in known.get().nodes) return GradingNodeChangeResult.Rejected(GradingNodeRejection.AlreadyRegistered)

        try {
            if (isActive) coordinator.addNode(node.address) else coordinator.observe(node.address)
        } catch (error: RuntimeException) {
            log.error("Cannot start checking node ${node.address.target}", error)
            failures.record(source = SOURCE, stage = "start", failure = error)
            coordinator.forget(node.address)
            return GradingNodeChangeResult.Failed
        }

        val isStored = try {
            store.add(node)
        } catch (error: RuntimeException) {
            log.error("Checking node write failed; resolving commit outcome", error)
            failures.record(source = SOURCE, stage = "write", failure = error)
            return resolve(node.address, expected = node, previous = null) { coordinator.forget(node.address) }
        }
        if (!isStored) {
            log.error("Checking node ${node.address.target} has a different stored record")
            failures.record(source = SOURCE, stage = "record_mismatch")
            coordinator.forget(node.address)
            return GradingNodeChangeResult.Failed
        }
        publish(node.address, isActive)
        GradingNodeChangeResult.Applied
    }

    override fun setActive(address: GradingNodeAddress, isActive: Boolean): GradingNodeChangeResult = synchronized(changes) {
        val registry = known.get()
        if (registry.persistence == SettingsPersistence.Uncertain) return GradingNodeChangeResult.Uncertain
        val wasActive = registry.nodes[address] ?: return GradingNodeChangeResult.Rejected(GradingNodeRejection.NotRegistered)
        when {
            wasActive == isActive -> GradingNodeChangeResult.Applied
            isActive -> enable(address)
            else -> disable(address)
        }
    }

    override fun remove(address: GradingNodeAddress): GradingNodeChangeResult = synchronized(changes) {
        val registry = known.get()
        if (registry.persistence == SettingsPersistence.Uncertain) return GradingNodeChangeResult.Uncertain
        val wasActive = registry.nodes[address] ?: return GradingNodeChangeResult.Rejected(GradingNodeRejection.NotRegistered)

        try {
            store.remove(address)
        } catch (error: RuntimeException) {
            log.error("Checking node removal failed; resolving commit outcome", error)
            failures.record(source = SOURCE, stage = "write", failure = error)
            return resolve(address, expected = null, previous = StoredGradingNode(address, wasActive)) {}
        }
        coordinator.forget(address)
        known.updateAndGet { state -> state.copy(nodes = state.nodes - address) }
        GradingNodeChangeResult.Applied
    }

    override fun importPreview(): GradingNodeImportPreview {
        val nodes = known.get().nodes
        return GradingNodeImportPreview(
            configured.map { address ->
                GradingNodeImportCandidate(address, nodes[address]?.let { isActive -> StoredGradingNode(address, isActive) })
            },
        )
    }

    override fun importNodes(selection: Map<GradingNodeAddress, Boolean>): GradingNodeImportReport = synchronized(changes) {
        val order = configured.filter { address -> address in selection } + selection.keys.filter { address -> address !in configured }
        val outcomes = mutableListOf<GradingNodeImportOutcome>()
        val notAttempted = mutableListOf<GradingNodeAddress>()
        order.forEach { address ->
            if (outcomes.lastOrNull()?.result == GradingNodeChangeResult.Uncertain) {
                notAttempted.add(address)
            } else {
                val result = if (address in configured) {
                    add(address.target, selection.getValue(address))
                } else {
                    GradingNodeChangeResult.Rejected(GradingNodeRejection.NotRegistered)
                }
                outcomes.add(GradingNodeImportOutcome(address, result))
            }
        }
        GradingNodeImportReport(outcomes, notAttempted)
    }

    private fun enable(address: GradingNodeAddress): GradingNodeChangeResult {
        try {
            coordinator.addNode(address)
        } catch (error: RuntimeException) {
            log.error("Cannot start participation of checking node ${address.target}", error)
            failures.record(source = SOURCE, stage = "start", failure = error)
            coordinator.removeNode(address)
            return GradingNodeChangeResult.Failed
        }

        val isStored = try {
            store.setActive(address, isActive = true)
        } catch (error: RuntimeException) {
            log.error("Checking node write failed; resolving commit outcome", error)
            failures.record(source = SOURCE, stage = "write", failure = error)
            return resolve(
                address,
                expected = StoredGradingNode(address, isActive = true),
                previous = StoredGradingNode(address, isActive = false),
            ) { coordinator.removeNode(address) }
        }
        if (!isStored) {
            log.error("Checking node ${address.target} has no stored record")
            failures.record(source = SOURCE, stage = "record_mismatch")
            coordinator.removeNode(address)
            return GradingNodeChangeResult.Failed
        }
        publish(address, isActive = true)
        return GradingNodeChangeResult.Applied
    }

    private fun disable(address: GradingNodeAddress): GradingNodeChangeResult {
        val isStored = try {
            store.setActive(address, isActive = false)
        } catch (error: RuntimeException) {
            log.error("Checking node write failed; resolving commit outcome", error)
            failures.record(source = SOURCE, stage = "write", failure = error)
            return resolve(
                address,
                expected = StoredGradingNode(address, isActive = false),
                previous = StoredGradingNode(address, isActive = true),
            ) {}
        }
        if (!isStored) {
            log.error("Checking node ${address.target} has no stored record")
            failures.record(source = SOURCE, stage = "record_mismatch")
            return GradingNodeChangeResult.Failed
        }
        coordinator.removeNode(address)
        publish(address, isActive = false)
        return GradingNodeChangeResult.Applied
    }

    /**
     * Reads back the record of [address] after a write exception. The expected record completes the change and the
     * previous one confirms a rollback; otherwise the registry is locked. [compensate] undoes a runtime step taken
     * before the write and runs unless the change is confirmed.
     */
    private fun resolve(
        address: GradingNodeAddress,
        expected: StoredGradingNode?,
        previous: StoredGradingNode?,
        compensate: () -> Unit,
    ): GradingNodeChangeResult {
        val observed = try {
            store.read().find { node -> node.address == address }
        } catch (error: RuntimeException) {
            log.error("Cannot determine durable checking node outcome", error)
            compensate()
            return uncertain()
        }

        return when (observed) {
            expected -> {
                complete(address, expected)
                GradingNodeChangeResult.Applied
            }
            previous -> {
                compensate()
                GradingNodeChangeResult.Failed
            }
            else -> {
                log.error("Unexpected durable record of checking node ${address.target}: $observed")
                compensate()
                uncertain()
            }
        }
    }

    /** Finishes a confirmed change whose runtime step follows the write. */
    private fun complete(address: GradingNodeAddress, expected: StoredGradingNode?) {
        when {
            expected == null -> {
                coordinator.forget(address)
                known.updateAndGet { state -> state.copy(nodes = state.nodes - address) }
            }
            known.get().nodes[address] == true && !expected.isActive -> {
                coordinator.removeNode(address)
                publish(address, isActive = false)
            }
            else -> publish(address, expected.isActive)
        }
    }

    private fun publish(address: GradingNodeAddress, isActive: Boolean) {
        known.updateAndGet { state -> state.copy(nodes = state.nodes + (address to isActive)) }
    }

    private fun uncertain(): GradingNodeChangeResult {
        known.updateAndGet { state -> state.copy(persistence = SettingsPersistence.Uncertain) }
        return GradingNodeChangeResult.Uncertain
    }

    private data class KnownRegistry(val nodes: Map<GradingNodeAddress, Boolean>, val persistence: SettingsPersistence)

    private companion object {
        const val SOURCE = "nodes"

        /** Restores the known registry from the coordinator started with it: observed nodes, registered ones active. */
        fun startedNodes(coordinator: GradingCoordinator): Map<GradingNodeAddress, Boolean> {
            val participants = coordinator.statuses().keys
            return coordinator.observedStatuses().keys.associateWith { address -> address in participants }
        }
    }
}
