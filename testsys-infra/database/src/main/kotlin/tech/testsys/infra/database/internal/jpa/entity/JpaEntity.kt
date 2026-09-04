package tech.testsys.infra.database.internal.jpa.entity

import jakarta.persistence.Embeddable
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Id
import jakarta.persistence.MappedSuperclass
import jakarta.persistence.Version
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.id.SnowflakeId
import tech.testsys.infra.database.internal.jpa.id.SnowflakeIdGenerator
import java.io.Serializable
import java.time.Instant

/**
 * Base of all JPA entities: audit timestamps and an optimistic lock version, all maintained by Hibernate.
 *
 * @property createdAt moment of the first persist (UTC).
 * @property updatedAt moment of the last update (UTC).
 * @property version optimistic lock counter.
 * @since %CURRENT_VERSION%
 */
@InternalDatabaseApi
@MappedSuperclass
abstract class JpaEntity(
    @CreationTimestamp
    var createdAt: Instant = Instant.now(),
    @UpdateTimestamp
    var updatedAt: Instant = Instant.now(),
    @Version
    var version: Long = 0,
)

/**
 * Marker of composite primary keys of [CompositeJpaEntity]; implementations are [Embeddable] data classes.
 *
 * @since %CURRENT_VERSION%
 */
@InternalDatabaseApi
interface CompositeId : Serializable

/**
 * Base of JPA entities keyed by an [EmbeddedId] composite key; equality is by [id].
 *
 * @param T the composite key type.
 * @property id the composite primary key.
 * @since %CURRENT_VERSION%
 */
@InternalDatabaseApi
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
 * Base of JPA entities keyed by a Snowflake-style [Long] issued by [SnowflakeIdGenerator]: seconds since its epoch,
 * node id and a per-second counter.
 *
 * @property id the primary key, `null` until persisted.
 * @since %CURRENT_VERSION%
 */
@InternalDatabaseApi
@MappedSuperclass
abstract class SnowflakeJpaEntity(
    @Id
    @SnowflakeId
    val id: Long? = null,
) : JpaEntity()
