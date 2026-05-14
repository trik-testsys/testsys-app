package tech.testsys.domain.contract

import tech.testsys.domain.model.DomainEntity
import tech.testsys.domain.model.DomainId
import tech.testsys.domain.model.LazyEntity
import tech.testsys.domain.model.LazyEntityList
import tech.testsys.domain.model.LazyNullableEntity

interface EntityFinder<Id : DomainId, Entity : DomainEntity<Id>> {

    fun findById(id: Id): Entity?
    fun findByIds(ids: List<Id>): List<Entity>
//    fun findFiltered(filterParams: FilterParams): List<Entity>
}

//interface FilterParams

interface EntityLoader<Id : DomainId, Entity : DomainEntity<Id>> {

    fun load(field: LazyEntity<Id, Entity>): Entity
    fun load(field: LazyNullableEntity<Id, Entity>): Entity?
    fun load(list: LazyEntityList<Id, Entity>, pageSize: Int, page: Int): List<Entity>
}

interface EntitySaver<Data, Id : DomainId, Entity : DomainEntity<Id>> {

    fun save(data: Data): Entity
    fun save(dataList: List<Data>): List<Entity>

    fun update(entity: Entity): Entity
    fun update(entityList: List<Entity>): List<Entity>
}

interface EntityRepository<Data, Id : DomainId, Entity : DomainEntity<Id>> :
    EntitySaver<Data, Id, Entity>,
    EntityLoader<Id, Entity>,
    EntityFinder<Id, Entity>
