package tech.testsys.infra.database.internal.jpa.entity.group

import jakarta.persistence.Entity
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.SnowflakeJpaEntity

/**
 * JPA entity of [tech.testsys.domain.model.group.Community].
 *
 * @property name the name of the community.
 * @property description the description of the community.
 * @property ownerId id of the user owning the community.
 * @since %CURRENT_VERSION%
 */
@Entity
@InternalDatabaseApi
class CommunityJpaEntity(
    val name: String,
    @field:JdbcTypeCode(SqlTypes.LONG32VARCHAR)
    val description: String,
    val ownerId: Long,
    id: Long? = null,
) : SnowflakeJpaEntity(id)
