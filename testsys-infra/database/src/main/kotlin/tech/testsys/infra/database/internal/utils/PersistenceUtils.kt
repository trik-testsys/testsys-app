package tech.testsys.infra.database.internal.utils

import org.springframework.data.repository.CrudRepository
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.JpaEntity

/**
 * Reconcile a join table to a desired set of foreign keys.
 *
 * Compares [existing] association rows against [targetKeys] using [keyOf],
 * inserts the rows produced by [buildAssociation] for keys not yet present,
 * and deletes rows whose key is no longer in [targetKeys]. Duplicate keys in
 * [targetKeys] are collapsed so at most one association row exists per key.
 *
 * @since %CURRENT_VERSION%
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

@InternalDatabaseApi
fun <T : JpaEntity, ID: Any> CrudRepository<T, ID>.findByIdOrError(id: ID) = findById(id).orElse(null).requireById(id)