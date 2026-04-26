package tech.testsys.infra.database.jpa.entity.task

import jakarta.persistence.Entity
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import tech.testsys.infra.database.jpa.entity.SequenceJpaEntity

// TODO: video recordings, logs

/**
 * JPA entity representing a judgment order domain entity.
 *
 * @see tech.testsys.domain.model.task.JudgmentOrder
 * @see tech.testsys.domain.model.task.JudgmentOrderData
 * @since %CURRENT_VERSION%
 */
@Entity
class JudgmentOrderJpaEntity(
    val judgeId: Long,
    val verdictId: Long,
    @field:JdbcTypeCode(SqlTypes.LONG32VARCHAR)
    val reason: String,
) : SequenceJpaEntity()
