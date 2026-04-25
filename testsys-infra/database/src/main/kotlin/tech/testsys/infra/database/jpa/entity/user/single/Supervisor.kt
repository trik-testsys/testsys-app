package tech.testsys.infra.database.jpa.entity.user.single

import jakarta.persistence.Entity
import tech.testsys.infra.database.jpa.entity.SequenceJpaEntity

/**
 * JPA entity representing the supervisor role data attached to a user.
 *
 * @see tech.testsys.domain.model.user.SupervisorData
 * @since %CURRENT_VERSION%
 */
@Entity
class SupervisorDataJpaEntity(
    val userId: Long,
) : SequenceJpaEntity()
