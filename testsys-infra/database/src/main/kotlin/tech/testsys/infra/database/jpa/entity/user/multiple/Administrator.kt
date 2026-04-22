package tech.testsys.infra.database.jpa.entity.user.multiple

import jakarta.persistence.Entity
import tech.testsys.infra.database.jpa.entity.SequenceJpaEntity

@Entity
class AdministratorDataJpaEntity(
    val userId: Long,
) : SequenceJpaEntity()