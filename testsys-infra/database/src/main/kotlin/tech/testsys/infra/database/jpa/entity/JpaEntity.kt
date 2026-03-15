package tech.testsys.infra.database.jpa.entity

import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.MappedSuperclass
import org.hibernate.annotations.CreationTimestamp
import java.time.Instant

/**
 * Base abstract class for all JPA entities with a simple (non-composite) primary key.
 *
 * Provides common fields:
 * - [id] — auto-generated primary key using a database sequence;
 * - [createdAt] — creation timestamp, automatically set by Hibernate on first persist.
 *
 * Subclasses should be annotated with `@Entity` and define their own fields and relationships.
 *
 * @property id unique entity identifier, `null` until persisted to the database.
 * @property createdAt moment of record creation (UTC), populated automatically on first persist.
 *
 * @since %CURRENT_VERSION%
 */
@MappedSuperclass
abstract class JpaEntity(

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    val id: Long? = null,

    @CreationTimestamp
    val createdAt: Instant = Instant.now(),
)