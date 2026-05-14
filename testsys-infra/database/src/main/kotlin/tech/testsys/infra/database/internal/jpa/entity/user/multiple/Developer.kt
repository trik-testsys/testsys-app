package tech.testsys.infra.database.internal.jpa.entity.user.multiple

import jakarta.persistence.Entity
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.SequenceJpaEntity

/**
 * JPA entity representing the developer role data attached to a user.
 *
 * @see tech.testsys.domain.model.user.DeveloperData
 * @since %CURRENT_VERSION%
 */
@Entity
@InternalDatabaseApi
class DeveloperDataJpaEntity(
    val userId: Long,
) : SequenceJpaEntity()
