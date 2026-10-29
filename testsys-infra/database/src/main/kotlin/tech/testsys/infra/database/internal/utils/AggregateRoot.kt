package tech.testsys.infra.database.internal.utils

import jakarta.persistence.EntityManager
import jakarta.persistence.LockModeType
import org.springframework.orm.ObjectOptimisticLockingFailureException
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.AggregateVersionTracker
import tech.testsys.infra.database.internal.jpa.entity.SnowflakeJpaEntity
import tech.testsys.infra.database.internal.jpa.repository.SnowflakeJpaEntityRepository

/** Protects an aggregate root and records its writes in the current transaction. */
@InternalDatabaseApi
internal fun <Root : SnowflakeJpaEntity> EntityManager.touchAggregateRoot(
    rootRepository: SnowflakeJpaEntityRepository<Root>,
    id: Long,
    expectedVersion: Long? = null,
    changesRootData: Boolean = false,
    writeRoot: (Root) -> Unit = {},
): Root {
    val root = rootRepository.findByIdOrError(id)
    val state = AggregateVersionTracker.state(root)
    val isTokenAccepted = expectedVersion == null || expectedVersion == root.version ||
        (expectedVersion == state.loadedVersion && !state.hasDataChanges)
    if (!isTokenAccepted) throw ObjectOptimisticLockingFailureException(root.javaClass, id)

    writeRoot(root)
    if (state.loadedVersion != null && root.version == state.loadedVersion) {
        lock(root, LockModeType.PESSIMISTIC_FORCE_INCREMENT)
    }
    if (changesRootData) state.hasDataChanges = true
    return root
}
