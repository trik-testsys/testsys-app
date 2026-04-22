package tech.testsys.infra.database.jpa.entity.user.multiple

import jakarta.persistence.Entity
import tech.testsys.infra.database.jpa.entity.SequenceJpaEntity

@Entity
class DeveloperDataJpaEntity(
    val userId: Long,
) : SequenceJpaEntity()
