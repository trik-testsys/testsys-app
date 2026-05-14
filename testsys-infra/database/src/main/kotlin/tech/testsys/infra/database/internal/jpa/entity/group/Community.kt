package tech.testsys.infra.database.internal.jpa.entity.group

import jakarta.persistence.Entity
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.SequenceJpaEntity

/**
 * JPA entity representing a community domain entity.
 *
 * @see Community
 * @see CommunityData
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
) : SequenceJpaEntity(id)
