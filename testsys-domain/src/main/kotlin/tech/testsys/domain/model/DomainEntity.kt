package tech.testsys.domain.model

import tech.testsys.domain.contract.persistence.repository.EntityLoader
import java.time.Instant

/**
 * Typed identifier of a [DomainEntity]. Each entity kind wraps a `Long` into its own value class,
 * so identifiers of different kinds cannot be mixed up at compile time.
 *
 * @property value the raw numeric value of the identifier.
 * @since %CURRENT_VERSION%
 */
interface DomainId {
    val value: Long
}

/**
 * Base class of every persisted domain entity. Two instances are equal when they have the same runtime class
 * and equal [id] values; entity data never affects identity.
 *
 * @param Id the identifier type of the entity.
 * @property id the identifier of the entity.
 * @property createdAt the moment the entity was created.
 * @since %CURRENT_VERSION%
 */
abstract class DomainEntity<Id : DomainId>(
    val id: Id,
    val createdAt: Instant,
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

/**
 * Lazy reference to a single domain entity, holding only its [id]. The entity is resolved on demand
 * through an [EntityLoader] and memoized after the first successful [load].
 *
 * @param Id the identifier type of the referenced entity.
 * @param Entity the type of the referenced entity.
 * @property id the identifier of the referenced entity.
 * @since %CURRENT_VERSION%
 */
class LazyEntity<Id : DomainId, Entity : DomainEntity<Id>>(val id: Id) {

    private var value: Entity? = null

    /**
     * Resolves the referenced entity; later calls return the memoized instance.
     *
     * @param persistence the loader used to fetch the entity on the first call.
     * @return the referenced entity.
     * @throws tech.testsys.domain.contract.DomainException.EntityNotFoundException if no entity with [id] exists.
     * @since %CURRENT_VERSION%
     */
    fun load(persistence: EntityLoader<Id, Entity>): Entity {
        value?.let {
            return it
        }

        val result = persistence.load(this)
        value = result

        return result
    }
}

/**
 * Lazy reference to a list of domain entities, holding only their [ids]. The list is resolved on demand
 * through an [EntityLoader] and memoized after the first successful [load].
 *
 * @param Id the identifier type of the referenced entities.
 * @param Entity the type of the referenced entities.
 * @property ids the identifiers of the referenced entities.
 * @since %CURRENT_VERSION%
 */
class LazyEntityList<Id : DomainId, Entity : DomainEntity<Id>>(val ids: List<Id>) {
    private var value: List<Entity>? = null

    /**
     * Resolves all referenced entities; later calls return the memoized list.
     *
     * @param persistence the loader used to fetch the entities on the first call.
     * @param pageSize reserved for future pagination; currently ignored.
     * @param page reserved for future pagination; currently ignored.
     * @return the referenced entities.
     * @throws tech.testsys.domain.contract.DomainException.EntityNotFoundException if any of [ids] does not exist.
     * @since %CURRENT_VERSION%
     */
    fun load(persistence: EntityLoader<Id, Entity>, pageSize: Int = Int.MAX_VALUE, page: Int = 0): List<Entity> {
        value?.let {
            return it
        }

        val result = persistence.load(this)
        value = result

        return result
    }
}
