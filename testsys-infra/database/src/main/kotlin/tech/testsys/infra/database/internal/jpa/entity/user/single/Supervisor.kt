package tech.testsys.infra.database.internal.jpa.entity.user.single

import jakarta.persistence.Entity
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.SequenceJpaEntity

/**
 * JPA entity representing the supervisor role data attached to a user.
 *
 * @see tech.testsys.domain.model.user.SupervisorData
 * @since %CURRENT_VERSION%
 */
@Entity
@InternalDatabaseApi
class SupervisorDataJpaEntity(
    val userId: Long,
    id: Long? = null,
) : SequenceJpaEntity(id)
