package tech.testsys.infra.settings.api.registry

import tech.testsys.domain.contract.GradingNodeAddress

/**
 * Durable registry record of a checking node; inactive nodes stay observed but receive no submissions.
 *
 * @property address the gRPC target of the node.
 * @property isActive whether the node participates in grading.
 * @since 1.0.0-beta.1
 */
data class StoredGradingNode(val address: GradingNodeAddress, val isActive: Boolean)

/**
 * Checking node registry storage; each operation is a separate completed transaction independent of a caller one.
 * A write exception may hide its commit outcome, so callers read back before reporting a rollback.
 *
 * @since 1.0.0-beta.1
 */
interface GradingNodeStore {
    /**
     * Reads all records ordered by address; failure aborts startup or makes an update outcome unknown.
     *
     * @since 1.0.0-beta.1
     */
    fun read(): List<StoredGradingNode>

    /**
     * Inserts [node] if its address is absent and returns whether the stored record now equals [node].
     *
     * @since 1.0.0-beta.1
     */
    fun add(node: StoredGradingNode): Boolean

    /**
     * Sets participation of the node at [address] to [isActive]; returns `false` if the address is absent.
     *
     * @since 1.0.0-beta.1
     */
    fun setActive(address: GradingNodeAddress, isActive: Boolean): Boolean

    /**
     * Deletes the record of [address]; deleting an absent address has no effect.
     *
     * @since 1.0.0-beta.1
     */
    fun remove(address: GradingNodeAddress)
}
