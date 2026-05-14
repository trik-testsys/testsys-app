package tech.testsys.infra.database.internal.jpa.entity.user.multiple

import jakarta.persistence.Entity
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.SequenceJpaEntity

/**
 * JPA entity representing the judge role data attached to a user.
 *
 * @see tech.testsys.domain.model.user.JudgeData
 * @since %CURRENT_VERSION%
 */
@Entity
@InternalDatabaseApi
class JudgeDataJpaEntity(
    val userId: Long,
) : SequenceJpaEntity()
