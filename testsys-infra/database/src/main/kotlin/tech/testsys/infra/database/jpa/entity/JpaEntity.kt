package tech.testsys.infra.database.jpa.entity

import jakarta.persistence.MappedSuperclass
import jakarta.persistence.Version
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
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
