package tech.testsys.domain.contract.persistence.repository

import tech.testsys.domain.model.DomainEntity
import tech.testsys.domain.model.DomainId
import tech.testsys.domain.model.LazyEntity
import tech.testsys.domain.model.LazyEntityList
import tech.testsys.domain.contract.DomainException.EntityNotFoundException

/**
 * Interface for finding domain entities.
 *
 * @param Id domain entity id type
 * @param Entity domain entity type
 *
 * @see DomainEntity
 * @see DomainId
 *
 * @since %CURRENT_VERSION%
 */
interface EntityFinder<Id : DomainId, Entity : DomainEntity<Id>> {

    /**
     * Finds entity by id
     *
     * @param id entity id
     *
     * @return entity if it's existing by id `null` otherwise
     */
    fun findById(id: Id): Entity?

    /**
     * Finds entities by id list
     *
     * @param ids id list
     *
     * @return list of found entities
     */
    fun findByIds(ids: List<Id>): List<Entity>
}

/**
 * Interface for loading domain entities by lazy reference.
 *
 * @param Id domain entity id type
 * @param Entity domain entity type
 *
 * @see DomainEntity
 * @see DomainId
 * @see LazyEntity
 * @see LazyEntityList
 *
 * @since %CURRENT_VERSION%
 */
interface EntityLoader<Id : DomainId, Entity : DomainEntity<Id>> {

    /**
     * Loads entity by lazy reference
     *
     * @param field lazy entity reference from other entity
     *
     * @return loaded entity by [LazyEntity.id]
     * @throws EntityNotFoundException if [LazyEntity.id] references to non-existence entity
     */
    @Throws(EntityNotFoundException::class) // TODO
    fun load(field: LazyEntity<Id, Entity>): Entity

    /**
     * Loads entity list by lazy reference
     *
     * @param list lazy entity list reference from other entity
     *
     * @return loaded entity list
     *
     * @throws EntityNotFoundException if any [LazyEntityList.ids] references to non-existence entity
     */
    @Throws(EntityNotFoundException::class) // TODO
    fun load(list: LazyEntityList<Id, Entity>): List<Entity>
}

/**
 * Interface for saving domain entities
 *
 * @param Data domain entity data type
 * @param Id domain entity id type
 * @param Entity domain entity type
 *
 * @see DomainEntity
 * @see DomainId
 *
 * @since %CURRENT_VERSION%
 */
interface EntitySaver<Data, Id : DomainId, Entity : DomainEntity<Id>> {

    /**
     * Saves new entity by its data
     *
     * @param data new entity data
     *
     * @return saved entity
     */
    fun save(data: Data): Entity

    /**
     * Saves new entities by their data
     *
     * @param dataList new entities data list
     *
     * @return saved entity list
     */
    fun save(dataList: List<Data>): List<Entity>

    /**
     * Updates existing entity
     *
     * @param entity updated existing entity
     *
     * @return saved updated entity
     */
    fun update(entity: Entity): Entity

    /**
     * Updates existing entities
     *
     * @param entityList updated existing entities
     *
     * @return saved updated entities
     */
    fun update(entityList: List<Entity>): List<Entity>
}

/**
 * Interface for removing domain entities
 *
 * @param Id domain entity id type
 * @param Entity domain entity type
 *
 * @see DomainEntity
 * @see DomainId
 *
 * @since %CURRENT_VERSION%
 */
interface EntityRemover<Id : DomainId, Entity : DomainEntity<Id>> {

    /**
     * Removes entity by its id
     *
     * @param id existing entity id
     */
    fun removeById(id: Id)

    /**
     * Removes entities by their ids
     *
     * @param ids entities ids
     */
    fun removeByIds(ids: List<Id>)

    /**
     * Removes entity
     *
     * @param entity entity to be removed
     */
    fun remove(entity: Entity)

    /**
     * Removes entities
     *
     * @param entityList entities to be removed
     */
    fun remove(entityList: List<Entity>)
}

/**
 * Interface for each implementation of entity repository
 *
 * @param Data domain entity data type
 * @param Id domain entity id type
 * @param Entity domain entity type
 *
 * @see DomainEntity
 * @see DomainId
 *
 * @see EntitySaver
 * @see EntityLoader
 * @see EntityFinder
 * @see EntityRemover
 *
 * @since %CURRENT_VERSION%
 */
interface EntityRepository<Data, Id : DomainId, Entity : DomainEntity<Id>> :
    EntitySaver<Data, Id, Entity>,
    EntityLoader<Id, Entity>,
    EntityFinder<Id, Entity>,
    EntityRemover<Id, Entity>
