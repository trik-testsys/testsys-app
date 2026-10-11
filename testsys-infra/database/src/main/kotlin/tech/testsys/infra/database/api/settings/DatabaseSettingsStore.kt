package tech.testsys.infra.database.api.settings

import org.springframework.stereotype.Component
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.support.TransactionTemplate
import tech.testsys.infra.database.api.transaction.TransactionRetry
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.settings.RuntimeSettingsJpaEntity
import tech.testsys.infra.database.internal.jpa.repository.settings.RuntimeSettingsJpaEntityRepository
import tech.testsys.infra.database.internal.settings.SettingsPayloadCodec
import tech.testsys.infra.settings.api.SettingsStore
import tech.testsys.infra.settings.api.StoredSettings
import java.util.UUID

/**
 * Atomic settings storage in independent transactions; database-only conflicts may be retried.
 * A commit exception is propagated so the coordinator can identify the write and compensate safely.
 *
 * @since %CURRENT_VERSION%
 */
@Component
@OptIn(InternalDatabaseApi::class)
class DatabaseSettingsStore(
    private val repository: RuntimeSettingsJpaEntityRepository,
    transactionManager: PlatformTransactionManager,
    private val retry: TransactionRetry,
) : SettingsStore {
    private val transaction = TransactionTemplate(transactionManager).apply {
        propagationBehavior = TransactionDefinition.PROPAGATION_REQUIRES_NEW
    }

    override fun read(): StoredSettings = transaction.execute {
        repository.findBySetKey(SET_KEY)?.stored() ?: StoredSettings(0, emptyMap(), null)
    }

    override fun compareAndSet(
        expectedRevision: Long,
        overrides: Map<String, String>,
        changeId: UUID,
        expectedChangeId: UUID?,
    ): StoredSettings? = retry.run("runtime-settings") {
        transaction.execute {
            val current = repository.findBySetKey(SET_KEY)
            if (current?.changeId == changeId) return@execute current.stored()
            if ((current?.revision ?: 0) != expectedRevision) return@execute null
            if (expectedChangeId != null && current?.changeId != expectedChangeId) return@execute null
            val payload = SettingsPayloadCodec.encode(overrides)
            val row = current?.apply {
                this.payload = payload
                revision = Math.addExact(expectedRevision, 1)
                this.changeId = changeId
            } ?: RuntimeSettingsJpaEntity(
                setKey = SET_KEY,
                payload = payload,
                revision = 1,
                changeId = changeId,
            )
            repository.saveAndFlush(row).stored()
        }
    }

    private fun RuntimeSettingsJpaEntity.stored(): StoredSettings = StoredSettings(revision, SettingsPayloadCodec.decode(payload), changeId)

    private companion object {
        const val SET_KEY = "runtime"
    }
}
