package tech.testsys.infra.database.internal.jpa.entity.task

import jakarta.persistence.Entity
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.SequenceJpaEntity

// TODO: video recordings, logs

/**
 * JPA entity representing a judgment order domain entity.
 *
 * @see tech.testsys.domain.model.task.JudgmentOrder
 * @see tech.testsys.domain.model.task.JudgmentOrderData
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
) : SequenceJpaEntity(id)
