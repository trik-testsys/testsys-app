package tech.testsys.infra.database.jpa.entity

import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.MappedSuperclass

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
abstract class JpaSequenceEntity(

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    val id: Long? = null,
) : JpaEntity()