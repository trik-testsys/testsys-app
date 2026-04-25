package tech.testsys.infra.database.jpa.entity.user.multiple

import jakarta.persistence.Entity
import tech.testsys.infra.database.jpa.entity.SequenceJpaEntity

/**
 * JPA entity representing the developer role data attached to a user.
 *
 * @see tech.testsys.domain.model.user.DeveloperData
 * @since %CURRENT_VERSION%
 */
@Entity
class DeveloperDataJpaEntity(
    val userId: Long,
) : SequenceJpaEntity()
