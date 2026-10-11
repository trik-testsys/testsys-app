package tech.testsys.infra.grpc.api

import tech.testsys.domain.contract.GradingNodeAddress
import tech.testsys.domain.contract.GradingNodeStatus
import tech.testsys.infra.settings.api.SettingsPersistence
import tech.testsys.infra.settings.api.StoredGradingNode

/**
 * Service registry of checking nodes: durable participation records applied to the running grader.
 * Changes are serialized; an uncertain durable outcome locks further changes until restart.
 *
 * @since %CURRENT_VERSION%
 */
interface GradingNodeRegistry {
    /**
     * Returns the known records with their latest observed availability, without network calls.
     *
     * @since %CURRENT_VERSION%
     */
    fun overview(): GradingNodeRegistryOverview

    /**
     * Records a new node at the trimmed [address] and starts observing it; [isActive] also starts its participation.
     *
     * @since %CURRENT_VERSION%
     */
    fun add(address: String, isActive: Boolean): GradingNodeChangeResult

    /**
     * Saves participation of the registered node at [address] and starts or stops sending submissions to it.
     *
     * @since %CURRENT_VERSION%
     */
    fun setActive(address: GradingNodeAddress, isActive: Boolean): GradingNodeChangeResult

    /**
     * Deletes the record of [address], stops its participation and observation.
     *
     * @since %CURRENT_VERSION%
     */
    fun remove(address: GradingNodeAddress): GradingNodeChangeResult

    /**
     * Compares configured node addresses read at startup with the known records, without availability checks.
     *
     * @since %CURRENT_VERSION%
     */
    fun importPreview(): GradingNodeImportPreview

    /**
     * Adds the selected configured addresses in configuration order with the participation from [selection].
     * Addresses already registered are skipped unchanged; an uncertain outcome stops the import.
     *
     * @since %CURRENT_VERSION%
     */
    fun importNodes(selection: Map<GradingNodeAddress, Boolean>): GradingNodeImportReport
}

/**
 * Known registry state for the service page.
 *
 * @property nodes the records ordered by address with their latest observed availability.
 * @property persistence the certainty of the saved registry.
 * @since %CURRENT_VERSION%
 */
data class GradingNodeRegistryOverview(val nodes: List<GradingNodeEntry>, val persistence: SettingsPersistence)

/**
 * Registry record with the latest known availability of its node.
 *
 * @property node the known record.
 * @property status the latest observed availability, independent of participation.
 * @since %CURRENT_VERSION%
 */
data class GradingNodeEntry(val node: StoredGradingNode, val status: GradingNodeStatus)

/**
 * Outcome of one registry change.
 *
 * @since %CURRENT_VERSION%
 */
sealed interface GradingNodeChangeResult {
    /**
     * The record and the running grader reflect the change.
     *
     * @since %CURRENT_VERSION%
     */
    data object Applied : GradingNodeChangeResult

    /**
     * The request was refused before any change.
     *
     * @property reason why the request was refused.
     * @since %CURRENT_VERSION%
     */
    data class Rejected(val reason: GradingNodeRejection) : GradingNodeChangeResult

    /**
     * A confirmed failure; the previous record and grader state are kept.
     *
     * @since %CURRENT_VERSION%
     */
    data object Failed : GradingNodeChangeResult

    /**
     * The durable outcome could not be confirmed; further changes are locked until restart.
     *
     * @since %CURRENT_VERSION%
     */
    data object Uncertain : GradingNodeChangeResult
}

/**
 * Reason of a refused registry change.
 *
 * @since %CURRENT_VERSION%
 */
enum class GradingNodeRejection {
    /**
     * The address is empty after trimming.
     *
     * @since %CURRENT_VERSION%
     */
    Blank,

    /**
     * The address exceeds the stored length limit.
     *
     * @since %CURRENT_VERSION%
     */
    TooLong,

    /**
     * The registry already has the address.
     *
     * @since %CURRENT_VERSION%
     */
    AlreadyRegistered,

    /**
     * The address is not registered, or is absent from the configuration for an import.
     *
     * @since %CURRENT_VERSION%
     */
    NotRegistered,
}

/**
 * Configured addresses compared with the registry.
 *
 * @property candidates the configured addresses in configuration order.
 * @since %CURRENT_VERSION%
 */
data class GradingNodeImportPreview(val candidates: List<GradingNodeImportCandidate>)

/**
 * Configured address with its current registry record.
 *
 * @property address the configured gRPC target.
 * @property existing the known record, or `null` for a new address.
 * @since %CURRENT_VERSION%
 */
data class GradingNodeImportCandidate(val address: GradingNodeAddress, val existing: StoredGradingNode?)

/**
 * Result of an import; addresses added before a stop stay registered.
 *
 * @property outcomes the attempted addresses with their results.
 * @property notAttempted the selected addresses left after an uncertain outcome.
 * @since %CURRENT_VERSION%
 */
data class GradingNodeImportReport(val outcomes: List<GradingNodeImportOutcome>, val notAttempted: List<GradingNodeAddress>)

/**
 * Result of importing one address.
 *
 * @property address the configured gRPC target.
 * @property result the outcome of adding it.
 * @since %CURRENT_VERSION%
 */
data class GradingNodeImportOutcome(val address: GradingNodeAddress, val result: GradingNodeChangeResult)
