package tech.testsys.infra.settings.api

import tech.testsys.domain.model.group.CommunityId
import tech.testsys.infra.settings.internal.InternalSettingsApi
import java.util.concurrent.atomic.AtomicReference

/**
 * Shared settings holder; readers capture one snapshot at their documented operation boundary.
 * Resource registration and updates share a lock so shutdown cannot race a prepared change.
 *
 * @property publicCommunityId the deployment-only community identifier.
 * @since %CURRENT_VERSION%
 */
class RuntimeSettings(initial: SettingsSnapshot, val publicCommunityId: CommunityId) : AutoCloseable {
    private val reference = AtomicReference(initial)
    private val preparations = mutableListOf<SettingsChangePreparation>()
    private val resources = mutableListOf<AutoCloseable>()
    private var isClosed = false
    internal val monitor = Any()

    /**
     * Captures one immutable publication.
     *
     * @since %CURRENT_VERSION%
     */
    fun snapshot(): SettingsSnapshot = reference.get()

    /**
     * Registers a consumer and prepares its initial resources before background work starts.
     * Closing this registration prevents future preparations and releases its current resource.
     *
     * @since %CURRENT_VERSION%
     */
    fun register(preparation: SettingsChangePreparation): AutoCloseable = synchronized(monitor) {
        check(!isClosed) { "Settings are closed" }
        val resource = preparation.prepare(snapshot())
        preparations.add(preparation)
        resources.add(resource)
        AutoCloseable {
            synchronized(monitor) {
                val index = preparations.indexOf(preparation)
                if (index >= 0) {
                    preparations.removeAt(index)
                    resources.removeAt(index).close()
                }
            }
        }
    }

    @InternalSettingsApi
    internal fun prepare(candidate: SettingsSnapshot): List<AutoCloseable> {
        check(!isClosed) { "Settings are closed" }
        val prepared = mutableListOf<AutoCloseable>()
        try {
            preparations.forEach { preparation -> prepared.add(preparation.prepare(candidate)) }
        } catch (error: RuntimeException) {
            prepared.asReversed().forEach { resource ->
                try {
                    resource.close()
                } catch (closeError: Exception) {
                    error.addSuppressed(closeError)
                }
            }
            throw error
        }
        return prepared
    }

    @InternalSettingsApi
    internal fun publish(candidate: SettingsSnapshot, prepared: List<AutoCloseable>): List<AutoCloseable> {
        val previous = resources.toList()
        resources.clear()
        resources.addAll(prepared)
        reference.set(candidate)
        return previous
    }

    override fun close() = synchronized(monitor) {
        isClosed = true
        resources.asReversed().forEach(AutoCloseable::close)
        resources.clear()
        preparations.clear()
    }
}
