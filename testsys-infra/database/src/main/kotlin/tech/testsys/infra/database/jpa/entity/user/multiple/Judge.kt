package tech.testsys.infra.database.jpa.entity.user.multiple

import jakarta.persistence.Entity
import tech.testsys.infra.database.jpa.entity.SequenceJpaEntity

/**
 * JPA entity representing the judge role data attached to a user.
 *
 * @see tech.testsys.domain.model.user.JudgeData
 * @since %CURRENT_VERSION%
 */
@Entity
class JudgeDataJpaEntity(
    val userId: Long,
) : SequenceJpaEntity()
