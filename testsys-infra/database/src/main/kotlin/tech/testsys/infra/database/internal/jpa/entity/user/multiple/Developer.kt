package tech.testsys.infra.database.internal.jpa.entity.user.multiple

import jakarta.persistence.Entity
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.SnowflakeJpaEntity

/**
 * JPA entity of [tech.testsys.domain.model.user.DeveloperData].
 *
 * @property userId id of the user holding the role.
 * @since %CURRENT_VERSION%
 */
@Entity
@InternalDatabaseApi
class DeveloperDataJpaEntity(
    val userId: Long,
) : SnowflakeJpaEntity()
