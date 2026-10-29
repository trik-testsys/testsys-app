package tech.testsys.infra.database.internal.utils

import org.springframework.data.repository.CrudRepository
import tech.testsys.domain.model.task.TrikStudioVersion
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.JpaEntity
import tech.testsys.infra.database.internal.jpa.entity.task.TrikStudioVersionJpaEntity
import tech.testsys.infra.database.internal.jpa.repository.task.TrikStudioVersionJpaEntityRepository

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

/**
 * Returns the id of the [TrikStudioVersionJpaEntity] with the tag and the version number of [version] or throws
 * [IllegalArgumentException] if no such version is registered; versions are registered outside the domain and never
 * created on the fly.
 */
@InternalDatabaseApi
internal fun TrikStudioVersionJpaEntityRepository.findIdByTagOrError(version: TrikStudioVersion): Long =
    findByTag(version.tag).requireRegistered(version).requireId()

/**
 * Maps the tag of each of [versions] to the id of its [TrikStudioVersionJpaEntity], reading them through
 * [findAllInChunks]; throws [IllegalArgumentException] for the first version whose tag and version number are not
 * registered together.
 */
@InternalDatabaseApi
internal fun TrikStudioVersionJpaEntityRepository.findIdsByTagOrError(versions: Collection<TrikStudioVersion>): Map<String, Long> {
    val rowsByTag = findAllInChunks(ids = versions.map { version -> version.tag }, find = ::findAllByTagIn)
        .associateBy { row -> row.tag }
    return versions.associate { version ->
        version.tag to rowsByTag[version.tag].requireRegistered(version).requireId()
    }
}

@InternalDatabaseApi
private fun TrikStudioVersionJpaEntity?.requireRegistered(version: TrikStudioVersion): TrikStudioVersionJpaEntity {
    requireNotNull(this) { "TRIK Studio version with tag=${version.tag} is not registered" }
    require(studioVersion == version.version) {
        "TRIK Studio version with tag=${version.tag} is registered with version=$studioVersion, not ${version.version}"
    }
    return this
}

/**
 * Converts this row to the domain [TrikStudioVersion].
 */
@InternalDatabaseApi
internal fun TrikStudioVersionJpaEntity.toDomain(): TrikStudioVersion = TrikStudioVersion(tag = tag, version = studioVersion)
