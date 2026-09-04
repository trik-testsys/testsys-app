package tech.testsys.infra.database.internal.jpa.entity.task

import jakarta.persistence.Entity
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.SnowflakeJpaEntity

/**
 * JPA entity of [tech.testsys.domain.model.task.JudgmentOrder].
 *
 * @property judgeId id of the judge issuing the order.
 * @property verdictId id of the [VerdictJpaEntity] the order applies to.
 * @property reason the reason of the order.
 * @since %CURRENT_VERSION%
 */
@Entity
@InternalDatabaseApi
class JudgmentOrderJpaEntity(
    val judgeId: Long,
    val verdictId: Long,
    @field:JdbcTypeCode(SqlTypes.LONG32VARCHAR)
    val reason: String,
    id: Long? = null,
) : SnowflakeJpaEntity(id)
