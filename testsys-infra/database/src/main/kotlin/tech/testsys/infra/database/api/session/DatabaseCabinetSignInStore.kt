package tech.testsys.infra.database.api.session

import org.springframework.stereotype.Component
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.support.TransactionTemplate
import tech.testsys.infra.database.api.transaction.TransactionRetry
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.session.CabinetSignInJpaEntity
import tech.testsys.infra.database.internal.jpa.repository.session.CabinetSignInJpaEntityRepository
import java.time.Instant

/** Kind of a sign-in to the service interface in [CabinetSignInJpaEntity.userKind]. */
private const val SERVICE_KIND = "SERVICE"

/**
 * Sign-in storage backed by [CabinetSignInJpaEntity] in independent transactions; every call repeats only database
 * conflicts.
 *
 * @since %CURRENT_VERSION%
 */
@Component
@OptIn(InternalDatabaseApi::class)
class DatabaseCabinetSignInStore(
    private val repository: CabinetSignInJpaEntityRepository,
    transactionManager: PlatformTransactionManager,
    private val retry: TransactionRetry,
) : CabinetSignInStore {
    private val transaction = TransactionTemplate(transactionManager).apply {
        propagationBehavior = TransactionDefinition.PROPAGATION_REQUIRES_NEW
    }

    override fun create(tokenHash: String, signIn: StoredCabinetSignIn, now: Instant) {
        val row = when (signIn) {
            StoredCabinetSignIn.Service -> CabinetSignInJpaEntity(
                tokenHash = tokenHash,
                userId = null,
                userKind = SERVICE_KIND,
                impersonatedBy = null,
                lastSeenAt = now,
            )
            is StoredCabinetSignIn.User -> CabinetSignInJpaEntity(
                tokenHash = tokenHash,
                userId = signIn.userId.value,
                userKind = signIn.kind.name,
                impersonatedBy = signIn.impersonatedBy?.value,
                lastSeenAt = now,
            )
        }

        inTransaction { repository.saveAndFlush(row) }
    }

    override fun find(tokenHash: String, idleSince: Instant): StoredCabinetSignIn? = inTransaction {
        repository.findByTokenHash(tokenHash)?.takeUnless { row -> row.lastSeenAt.isBefore(idleSince) }?.let(::signInOf)
    }

    override fun touch(tokenHash: String, now: Instant) {
        inTransaction { repository.touch(tokenHash, now) }
    }

    override fun delete(tokenHash: String): Boolean = inTransaction { repository.deleteByToken(tokenHash) > 0 }

    override fun deleteOwn(userId: CabinetSignInUserId, exceptTokenHash: String?) {
        inTransaction {
            if (exceptTokenHash == null) {
                repository.deleteOwn(userId.value)
            } else {
                repository.deleteOwnExcept(userId.value, exceptTokenHash)
            }
        }
    }

    override fun deleteAllOf(userId: CabinetSignInUserId): List<StoredCabinetSignIn> =
        deleteRead { repository.findAllByUserIdOrImpersonatedBy(userId = userId.value, impersonatedBy = userId.value) }

    override fun deleteIdle(before: Instant): List<StoredCabinetSignIn> = deleteRead { repository.findAllByLastSeenAtBefore(before) }

    /**
     * Deletes the rows of [read] in the same transaction and returns their sign-ins; a row changed or deleted
     * concurrently fails the deletion with a conflict, so the repeat reads it again.
     */
    private fun deleteRead(read: () -> List<CabinetSignInJpaEntity>): List<StoredCabinetSignIn> = inTransaction {
        val rows = read()
        repository.deleteAllByIdInBatch(rows.map { row -> checkNotNull(row.id) { "Stored sign-in ${row.tokenHash} has no id" } })
        rows.map(::signInOf)
    }

    private fun <T> inTransaction(action: () -> T): T = retry.run(RETRY_NAME) {
        transaction.execute { action() }
    }

    private fun signInOf(row: CabinetSignInJpaEntity): StoredCabinetSignIn = if (row.userKind == SERVICE_KIND) {
        StoredCabinetSignIn.Service
    } else {
        StoredCabinetSignIn.User(
            userId = CabinetSignInUserId(checkNotNull(row.userId) { "Sign-in ${row.id} of kind ${row.userKind} has no user" }),
            kind = CabinetSignInUserKind.valueOf(row.userKind),
            impersonatedBy = row.impersonatedBy?.let(::CabinetSignInUserId),
        )
    }

    private companion object {
        const val RETRY_NAME = "cabinet-sign-ins"
    }
}
