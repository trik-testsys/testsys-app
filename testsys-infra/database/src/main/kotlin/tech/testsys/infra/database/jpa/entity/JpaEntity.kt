package tech.testsys.infra.database.jpa.entity

import jakarta.persistence.Embeddable
import jakarta.persistence.EmbeddedId
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.MappedSuperclass
import jakarta.persistence.Version
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.io.Serializable
import java.time.Instant

/**
 * Common base class for all JPA entities, providing audit and versioning fields.
 *
 * @property createdAt moment of record creation (UTC), populated automatically on first persist.
 * @property updatedAt moment of last update (UTC), populated automatically on each merge/flush.
 * @property version optimistic lock counter, incremented automatically on each update.
 *
 * @since %CURRENT_VERSION%
 */
@MappedSuperclass
abstract class JpaEntity(
    @CreationTimestamp
    val createdAt: Instant = Instant.now(),
    @UpdateTimestamp
    var updatedAt: Instant = Instant.now(),
    @Version
    val version: Long = 0,
)

/**
 * Base abstract class for composite (multi-column) primary keys used with [CompositeJpaEntity].
 *
 * Subclasses must be annotated with [Embeddable] and are recommended to be Kotlin `data class`es,
 * which automatically provide the [equals] and [hashCode] implementations required by the JPA spec.
 *
 * @since %CURRENT_VERSION%
 */
interface CompositeId : Serializable

/**
 * Base abstract class for all JPA entities with a composite (multi-column) primary key.
 *
 * Uses an [EmbeddedId] of type [T] instead of a simple auto-generated [Long] identifier.
 * Inherits audit and versioning fields from [JpaEntity].
 *
 * Subclasses should be annotated with `@Entity` and provide a concrete [CompositeId] subclass
 * as the type parameter.
 *
 * @param T the composite key type, must extend [CompositeId].
 * @property id the composite primary key.
 *
 * @since %CURRENT_VERSION%
 */
@MappedSuperclass
abstract class CompositeJpaEntity<T : CompositeId>(
    @EmbeddedId
    val id: T,
) : JpaEntity() {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CompositeJpaEntity<*>) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}

/**
 * Base abstract class for all JPA entities with a simple (non-composite) primary key.
 *
 * Provides an auto-generated primary key using a database sequence.
 * Inherits audit and versioning fields from [JpaEntity].
 *
 * Subclasses should be annotated with `@Entity` and define their own fields and relationships.
 *
 * @property id unique entity identifier, `null` until persisted to the database.
 *
 * @since %CURRENT_VERSION%
 */
@MappedSuperclass
abstract class SequenceJpaEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    val id: Long? = null,
) : JpaEntity()
