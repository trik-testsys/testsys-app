package tech.testsys.infra.database.jpa.entity.user.single

import jakarta.persistence.Entity
import tech.testsys.infra.database.jpa.entity.SequenceJpaEntity

@Entity
class SupervisorDataJpaEntity(
    val userId: Long,
) : SequenceJpaEntity()