package tech.testsys.infra.database.jpa.entity

import jakarta.persistence.Column
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.MappedSuperclass
import jakarta.persistence.SequenceGenerator
import org.springframework.data.annotation.CreatedDate
import java.time.Instant

@MappedSuperclass
abstract class JpaEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "t_entity_seq")
    @SequenceGenerator(name = "t_entity_seq")
    val id: Long? = null,

    @Column(name = "created_at")
    @CreatedDate
    val createdAt: Instant = Instant.now(),
)