package tech.testsys.infra.database.jpa.entity

import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.MappedSuperclass
import jakarta.persistence.SequenceGenerator
import org.springframework.data.annotation.CreatedDate
import java.time.Instant

/**
 * Abstract class for every db entity, that has non-complex foreign key.
 *
 * @author Viktor Karasev
 * @author Vyacheslav Buchin
 * @author Roman Shishkin
 *
 * @since %CURRENT_VERSION%
 */
@MappedSuperclass
abstract class JpaEntity(

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "t_entity_seq")
    @SequenceGenerator(name = "t_entity_seq")
    val id: Long? = null,

    @CreatedDate
    val createdAt: Instant = Instant.now(),
)