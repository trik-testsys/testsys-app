package tech.testsys.infra.database.internal.jpa.entity

import jakarta.persistence.Embeddable
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Id
import jakarta.persistence.MappedSuperclass
import jakarta.persistence.PostLoad
import jakarta.persistence.PrePersist
import jakarta.persistence.Transient
import jakarta.persistence.Version
import org.hibernate.annotations.UpdateTimestamp
import org.springframework.data.domain.Persistable
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.id.SnowflakeId
import tech.testsys.infra.database.internal.jpa.id.SnowflakeIdGenerator
import java.io.Serializable
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * Base of all JPA entities: audit timestamps and an optimistic lock version. [updatedAt] and [version] are maintained
 * by Hibernate. [createdAt] is assigned on construction.
 *
 * @property createdAt moment the row was created (UTC).
 * @property updatedAt moment of the last update (UTC).
 * @property version optimistic lock counter.
 * @since %CURRENT_VERSION%
 */
@InternalDatabaseApi
@MappedSuperclass
abstract class JpaEntity(
    var createdAt: Instant = Instant.now().truncatedTo(ChronoUnit.MICROS),
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
 * Base of JPA entities keyed by an [EmbeddedId] composite key; equality is by [id]. An instance created by code is
 * a new row, so `save` inserts it through `persist` without reading the key first; a loaded or persisted one is not.
 *
 * @param T the composite key type.
 * @property id the composite primary key.
 * @since %CURRENT_VERSION%
 */
@InternalDatabaseApi
@MappedSuperclass
abstract class CompositeJpaEntity<T : CompositeId>(
    @EmbeddedId
    @get:JvmName("getCompositeId")
    final val id: T,
) : JpaEntity(),
    Persistable<T> {

    @Transient
    private var isNewRow: Boolean = true

    override fun getId(): T = id

    override fun isNew(): Boolean = isNewRow

    /** Marks the row as stored once it is passed to `persist` or loaded from the database. */
    @PrePersist
    @PostLoad
    protected fun markStored() {
        isNewRow = false
    }

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
