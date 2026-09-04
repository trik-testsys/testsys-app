package tech.testsys.domain.contract.persistence.repository

import tech.testsys.domain.contract.DomainException.EntityNotFoundException
import tech.testsys.domain.model.DomainEntity
import tech.testsys.domain.model.DomainId
import tech.testsys.domain.model.LazyEntity
import tech.testsys.domain.model.LazyEntityList

/**
 * Finds domain entities by identifier.
 *
 * @param Id the identifier type of the entity.
 * @param Entity the domain entity type.
 * @since %CURRENT_VERSION%
 */
interface EntityFinder<Id : DomainId, Entity : DomainEntity<Id>> {

    /**
     * Finds an entity by id.
     *
     * @param id the id of the entity.
     * @return the entity, or `null` if it does not exist.
     * @since %CURRENT_VERSION%
     */
    fun findById(id: Id): Entity?

    /**
     * Finds entities by ids.
     *
     * @param ids the ids of the entities.
     * @return the found entities; missing ids are skipped.
     * @since %CURRENT_VERSION%
     */
    fun findByIds(ids: List<Id>): List<Entity>
}

/**
 * Loads domain entities by lazy reference.
 *
 * @param Id the identifier type of the entity.
 * @param Entity the domain entity type.
 * @since %CURRENT_VERSION%
 */
interface EntityLoader<Id : DomainId, Entity : DomainEntity<Id>> {

    /**
     * Loads the entity behind a lazy reference.
     *
     * @param field the lazy reference held by another entity.
     * @return the referenced entity.
     * @throws EntityNotFoundException if the referenced entity does not exist.
     * @since %CURRENT_VERSION%
     */
    @Throws(EntityNotFoundException::class) // TODO
    fun load(field: LazyEntity<Id, Entity>): Entity

    /**
     * Loads the entities behind a lazy list reference.
     *
     * @param list the lazy list reference held by another entity.
     * @return the referenced entities.
     * @throws EntityNotFoundException if any referenced entity does not exist.
     * @since %CURRENT_VERSION%
     */
    @Throws(EntityNotFoundException::class) // TODO
    fun load(list: LazyEntityList<Id, Entity>): List<Entity>
}

/**
 * Saves new and updates existing domain entities.
 *
 * @param Data the data type a new entity is created from.
 * @param Id the identifier type of the entity.
 * @param Entity the domain entity type.
 * @since %CURRENT_VERSION%
 */
interface EntitySaver<Data, Id : DomainId, Entity : DomainEntity<Id>> {

    /**
     * Saves a new entity.
     *
     * @param data the data of the new entity.
     * @return the saved entity.
     * @since %CURRENT_VERSION%
     */
    fun save(data: Data): Entity

    /**
     * Saves new entities.
     *
     * @param dataList the data of the new entities.
     * @return the saved entities.
     * @since %CURRENT_VERSION%
     */
    fun save(dataList: List<Data>): List<Entity>

    /**
     * Updates an existing entity.
     *
     * @param entity the entity with updated data.
     * @return the saved entity.
     * @since %CURRENT_VERSION%
     */
    fun update(entity: Entity): Entity

    /**
     * Updates existing entities.
     *
     * @param entityList the entities with updated data.
     * @return the saved entities.
     * @since %CURRENT_VERSION%
     */
    fun update(entityList: List<Entity>): List<Entity>
}

/**
 * Removes domain entities.
 *
 * @param Id the identifier type of the entity.
 * @param Entity the domain entity type.
 * @since %CURRENT_VERSION%
 */
interface EntityRemover<Id : DomainId, Entity : DomainEntity<Id>> {

    /**
     * Removes an entity by id.
     *
     * @param id the id of the entity to remove.
     * @since %CURRENT_VERSION%
     */
    fun removeById(id: Id)

    /**
     * Removes entities by ids.
     *
     * @param ids the ids of the entities to remove.
     * @since %CURRENT_VERSION%
     */
    fun removeByIds(ids: List<Id>)

    /**
     * Removes an entity.
     *
     * @param entity the entity to remove.
     * @since %CURRENT_VERSION%
     */
    fun remove(entity: Entity)

    /**
     * Removes entities.
     *
     * @param entityList the entities to remove.
     * @since %CURRENT_VERSION%
     */
    fun remove(entityList: List<Entity>)
}

/**
 * Persistence port of a domain entity: finding, loading, saving and removing.
 *
 * @param Data the data type a new entity is created from.
 * @param Id the identifier type of the entity.
 * @param Entity the domain entity type.
 * @since %CURRENT_VERSION%
 */
interface EntityRepository<Data, Id : DomainId, Entity : DomainEntity<Id>> :
    EntitySaver<Data, Id, Entity>,
    EntityLoader<Id, Entity>,
    EntityFinder<Id, Entity>,
    EntityRemover<Id, Entity>
