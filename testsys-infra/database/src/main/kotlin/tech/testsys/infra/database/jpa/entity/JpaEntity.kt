package tech.testsys.infra.database.jpa.entity

import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.MappedSuperclass
import org.hibernate.annotations.CreationTimestamp
import java.time.Instant

/**
 * Abstract class for every db entity, that has non-complex foreign key.
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