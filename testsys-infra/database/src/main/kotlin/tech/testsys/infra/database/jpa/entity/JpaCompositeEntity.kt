package tech.testsys.infra.database.jpa.entity

import jakarta.persistence.Embeddable
import jakarta.persistence.EmbeddedId
import jakarta.persistence.MappedSuperclass
import org.hibernate.annotations.CreationTimestamp
import java.io.Serializable
import java.time.Instant

/**
 * Base abstract class for composite (multi-column) primary keys used with [JpaCompositeEntity].
 *
 * Subclasses must be annotated with [Embeddable] and are recommended to be Kotlin `data class`es,
 * which automatically provide the [equals] and [hashCode] implementations required by the JPA spec.
 *
 * @since %CURRENT_VERSION%
 */
abstract class JpaCompositeId : Serializable

/**
 * Base abstract class for all JPA entities with a composite (multi-column) primary key.
 *
 * Uses an [EmbeddedId] of type [T] instead of a simple auto-generated [Long] identifier.
 *
 * Subclasses should be annotated with `@Entity` and provide a concrete [JpaCompositeId] subclass
 * as the type parameter.
 *
 * @param T the composite key type, must extend [JpaCompositeId].
 * @property id the composite primary key.
 * @property createdAt moment of record creation (UTC), populated automatically on first persist.
 *
 * @since %CURRENT_VERSION%
 */
@MappedSuperclass
abstract class JpaCompositeEntity<T : JpaCompositeId>(

    @EmbeddedId
    val id: T,

    @CreationTimestamp
    val createdAt: Instant = Instant.now(),
)
