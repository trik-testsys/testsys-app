package tech.testsys.infra.database.internal.jpa.entity.user.single

import jakarta.persistence.Entity
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.SnowflakeJpaEntity

/**
 * JPA entity of [tech.testsys.domain.model.user.SupervisorData].
 *
 * @property userId id of the user holding the role.
 * @since 1.0.0-beta.1
 */
@Entity
@InternalDatabaseApi
class SupervisorDataJpaEntity(
    val userId: Long,
    id: Long? = null,
) : SnowflakeJpaEntity(id)
