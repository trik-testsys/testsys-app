package tech.testsys.infra.database.jpa.entity.user.multiple

import jakarta.persistence.Entity
import tech.testsys.infra.database.jpa.entity.SequenceJpaEntity

/**
 * JPA entity representing the manager role data attached to a user.
 *
 * @see tech.testsys.domain.model.user.ManagerData
 * @since %CURRENT_VERSION%
 */
@Entity
class ManagerDataJpaEntity(
    val userId: Long,
) : SequenceJpaEntity()
