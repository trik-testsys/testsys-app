package tech.testsys.infra.database.jpa.entity.user.multiple

import jakarta.persistence.Entity
import tech.testsys.infra.database.jpa.entity.SequenceJpaEntity

/**
 * JPA entity representing the administrator role data attached to a user.
 *
 * @see tech.testsys.domain.model.user.AdministratorData
 * @since %CURRENT_VERSION%
 */
@Entity
class AdministratorDataJpaEntity(
    val userId: Long,
) : SequenceJpaEntity()
