package tech.testsys.infra.database.internal.utils

import org.springframework.data.repository.CrudRepository
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.JpaEntity

/**
 * Reconciles join rows with [targetKeys]: rows whose [keyOf] is absent from the target go to [deleteAll], keys without
 * a row (deduplicated) are built by [buildAssociation] and go to [saveAll].
 */
@InternalDatabaseApi
internal fun <Association, K> syncJoinTable(
    existing: List<Association>,
    targetKeys: List<K>,
    keyOf: (Association) -> K,
    buildAssociation: (K) -> Association,
    deleteAll: (List<Association>) -> Unit,
    saveAll: (List<Association>) -> Unit,
) {
    val targetSet = targetKeys.toSet()
    val existingSet = existing.map(keyOf).toSet()

    val toRemove = existing.filter { keyOf(it) !in targetSet }
    val toAdd = (targetSet - existingSet).map(buildAssociation)

    if (toRemove.isNotEmpty()) deleteAll(toRemove)
    if (toAdd.isNotEmpty()) saveAll(toAdd)
}

/**
 * Finds the entity with [id] or throws [IllegalArgumentException] if there is no such row.
 *
 * @param T the JPA entity type.
 * @param ID the primary key type.
 * @since %CURRENT_VERSION%
 */
@InternalDatabaseApi
fun <T : JpaEntity, ID : Any> CrudRepository<T, ID>.findByIdOrError(id: ID) = findById(id).orElse(null).requireById(id)
