package tech.testsys.infra.settings.internal

import org.slf4j.LoggerFactory
import tech.testsys.infra.monitoring.api.TechnicalFailures
import tech.testsys.infra.settings.api.RuntimeSettings
import tech.testsys.infra.settings.api.SettingsSnapshot
import tech.testsys.infra.settings.api.catalog.SettingKey
import tech.testsys.infra.settings.api.service.SettingsChangeResult
import tech.testsys.infra.settings.api.service.SettingsOverview
import tech.testsys.infra.settings.api.service.SettingsPersistence
import tech.testsys.infra.settings.api.service.SettingsService
import tech.testsys.infra.settings.api.store.SettingsStore
import tech.testsys.infra.settings.api.store.StoredSettings
import java.time.DateTimeException
import java.util.UUID

@InternalSettingsApi
internal class SettingsCoordinator(
    private val store: SettingsStore,
    private val runtime: RuntimeSettings,
    private val resolver: SettingsResolver,
    private var stored: StoredSettings,
    private val failures: TechnicalFailures = TechnicalFailures.NONE,
) : SettingsService {
    private var persistence = SettingsPersistence.Known
    private val log = LoggerFactory.getLogger(javaClass)

    override fun overview(): SettingsOverview = synchronized(runtime.monitor) {
        SettingsOverview(stored.revision, runtime.snapshot(), persistence)
    }

    override fun change(expectedRevision: Long, changes: Map<SettingKey, String>): SettingsChangeResult = synchronized(runtime.monitor) {
        if (persistence == SettingsPersistence.Uncertain) return SettingsChangeResult.Uncertain
        if (expectedRevision != stored.revision) return SettingsChangeResult.Stale
        val errors = mutableMapOf<SettingKey, String>()
        val canonical = changes.mapNotNull { (key, raw) ->
            try {
                key.property to SettingsValidator.canonical(key, raw)
            } catch (error: IllegalArgumentException) {
                errors[key] = error.message ?: "Invalid value"
                null
            } catch (error: DateTimeException) {
                errors[key] = error.message ?: "Invalid duration"
                null
            } catch (error: ArithmeticException) {
                errors[key] = error.message ?: "Duration is too large"
                null
            }
        }.toMap()
        if (errors.isNotEmpty()) return SettingsChangeResult.Invalid(errors)
        if (changes.isEmpty()) return SettingsChangeResult.Applied(overview())
        val overrides = stored.overrides + canonical
        val previous = runtime.snapshot()
        val candidate = SettingsSnapshot(
            revision = stored.revision + 1,
            values = resolver.resolve(overrides),
            activeMaxMessageBytes = previous.activeMaxMessageBytes,
        )
        val prepared = try {
            runtime.prepare(candidate)
        } catch (error: RuntimeException) {
            log.error("Settings preparation failed", error)
            failures.record(source = SOURCE, stage = "prepare", failure = error)
            return SettingsChangeResult.Failed
        }
        val changeId = UUID.randomUUID()
        val committed = try {
            store.compareAndSet(stored.revision, overrides, changeId)
        } catch (error: RuntimeException) {
            release(prepared)
            log.error("Settings write failed; resolving commit outcome", error)
            // Resolving the outcome belongs to this incident, so its own failures are not counted separately.
            failures.record(source = SOURCE, stage = "write", failure = error)
            return recover(changeId)
        }
        if (committed == null) {
            release(prepared)
            return SettingsChangeResult.Stale
        }
        check(committed.revision == candidate.revision) { "Unexpected settings revision ${committed.revision}" }
        stored = committed
        val obsolete = runtime.publish(candidate, prepared)
        release(obsolete)
        SettingsChangeResult.Applied(overview())
    }

    private fun recover(changeId: UUID): SettingsChangeResult {
        val previous = stored
        try {
            val observed = store.read()
            if (observed == previous) return SettingsChangeResult.Failed
            if (observed.changeId != changeId) return uncertain()
            val compensationId = UUID.randomUUID()
            val compensated = try {
                store.compareAndSet(observed.revision, previous.overrides, compensationId, changeId)
            } catch (error: RuntimeException) {
                log.error("Settings compensation failed; resolving commit outcome", error)
                store.read().takeIf { result -> result.changeId == compensationId && result.overrides == previous.overrides }
            } ?: return uncertain()
            stored = compensated
            return SettingsChangeResult.Failed
        } catch (error: RuntimeException) {
            log.error("Cannot determine durable settings outcome", error)
            return uncertain()
        }
    }

    private fun uncertain(): SettingsChangeResult {
        persistence = SettingsPersistence.Uncertain
        return SettingsChangeResult.Uncertain
    }

    private fun release(resources: List<AutoCloseable>) {
        resources.asReversed().forEach { resource ->
            try {
                resource.close()
            } catch (error: Exception) {
                log.warn("Cannot release obsolete settings resource", error)
            }
        }
    }

    private companion object {
        const val SOURCE = "settings"
    }
}
