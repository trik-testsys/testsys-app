package tech.testsys.infra.database.internal.jpa.entity.user.multiple

import jakarta.persistence.Entity
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.SequenceJpaEntity

/**
 * JPA entity of [tech.testsys.domain.model.user.ManagerData].
 *
 * @property userId id of the user holding the role.
 * @since %CURRENT_VERSION%
 */
@Entity
@InternalDatabaseApi
class ManagerDataJpaEntity(
    val userId: Long,
) : SequenceJpaEntity()
