package tech.testsys.infra.database.internal.jpa.entity.user.multiple

import jakarta.persistence.Entity
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.SnowflakeJpaEntity

/**
 * JPA entity of the administrator role data.
 *
 * @property userId id of the user holding the role.
 * @since 1.0.0-beta.1
 */
@Entity
@InternalDatabaseApi
class AdministratorDataJpaEntity(
    val userId: Long,
) : SnowflakeJpaEntity()
