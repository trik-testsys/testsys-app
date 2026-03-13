package tech.testsys.domain.model

import tech.testsys.domain.contract.EntityLoader
import java.time.Instant

interface DomainId {
    val value: Long
}

abstract class DomainEntity<Id: DomainId>(
    val id: Id,
    val createdAt: Instant
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DomainEntity<*>

        return id.value == other.id.value
    }

    override fun hashCode(): Int {
        return id.value.hashCode()
    }
}

class LazyEntity<Id: DomainId, Entity: DomainEntity<Id>>(val id: Id) {

    private var value: Entity? = null

    fun load(persistence: EntityLoader<Id, Entity>): Entity {
        value?.let {
            return it
        }

        val result = persistence.load(this)
        value = result

        return result
    }
}

class LazyNullableEntity<Id: DomainId, Event: DomainEntity<Id>>(val id: Id) {

    private var value: Event? = null
    private var wasLoaded = false

    fun load(persistence: EntityLoader<Id, Event>): Event? {
        if (wasLoaded) return value

        val result = persistence.load(this)
        value = result
        wasLoaded = true

        return result
    }
}

class LazyEntityList<Id: DomainId, Entity: DomainEntity<Id>>(val ids: List<Id>) {
    private var value: List<Entity>? = null

    fun load(persistence: EntityLoader<Id, Entity>, pageSize: Int = Int.MAX_VALUE, page: Int = 0): List<Entity> {
        value?.let {
            return it
        }

        val result = persistence.load(this, pageSize, page)
        value = result

        return result
    }
}
