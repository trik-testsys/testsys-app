package tech.testsys.operation.util

import tech.testsys.domain.builder.util.lazify
import tech.testsys.domain.contract.persistence.repository.EntityFinder
import tech.testsys.domain.contract.persistence.repository.EntityLoader
import tech.testsys.domain.model.DomainEntity
import tech.testsys.domain.model.DomainId
import tech.testsys.operation.annotation.InternalOperationsApi

/**
 * Finds the entities with [ids] in one repository call and maps each found id to its entity; missing ids are absent
 * from the result, and empty [ids] skip the call.
 *
 * @param Id the identifier type of the entities.
 * @param Entity the type of the entities.
 * @since %CURRENT_VERSION%
 */
@InternalOperationsApi
fun <Id : DomainId, Entity : DomainEntity<Id>> EntityFinder<Id, Entity>.findByIdsAsMap(ids: Collection<Id>): Map<Id, Entity> {
    if (ids.isEmpty()) return emptyMap()
    return findByIds(ids.distinct()).associateBy { entity -> entity.id }
}

/**
 * Loads the entities with [ids] in one repository call and maps each id to its entity; empty [ids] skip the call.
 *
 * @param Id the identifier type of the entities.
 * @param Entity the type of the entities.
 * @throws IllegalArgumentException if an entity with one of [ids] does not exist.
 * @since %CURRENT_VERSION%
 */
@InternalOperationsApi
fun <Id : DomainId, Entity : DomainEntity<Id>> EntityLoader<Id, Entity>.loadByIdsAsMap(ids: Collection<Id>): Map<Id, Entity> {
    if (ids.isEmpty()) return emptyMap()
    return load(ids.distinct().lazify()).associateBy { entity -> entity.id }
}
