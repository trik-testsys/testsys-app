package tech.testsys.infra.database.jpa.entity.user.multiple

import jakarta.persistence.Entity
import tech.testsys.infra.database.jpa.entity.SequenceJpaEntity

/**
 * JPA entity representing the student role data attached to a user.
 *
 * @see tech.testsys.domain.model.user.StudentData
 * @since %CURRENT_VERSION%
 */
@Entity
class StudentDataJpaEntity(
    val userId: Long,
) : SequenceJpaEntity()
