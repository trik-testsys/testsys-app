package tech.testsys.infra.database.internal.mapping.task

import tech.testsys.domain.builder.api.judgmentOrder
import tech.testsys.domain.builder.data
import tech.testsys.domain.model.task.JudgmentOrder
import tech.testsys.domain.model.task.JudgmentOrderData
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.task.JudgmentOrderJpaEntity
import tech.testsys.infra.database.internal.mapping.EntityMapping
import tech.testsys.infra.database.internal.utils.populateFields
import tech.testsys.infra.database.internal.utils.requireVersion

/**
 * Mapping between [JudgmentOrder] and [JudgmentOrderJpaEntity].
 *
 * @since 1.0.0-beta.1
 */
@InternalDatabaseApi
object JudgmentOrderMapping : EntityMapping<JudgmentOrder, JudgmentOrderJpaEntity> {

    /**
     * Assembles a [JudgmentOrder] from [jpaEntity].
     *
     * @since 1.0.0-beta.1
     */
    fun toDomain(jpaEntity: JudgmentOrderJpaEntity) = judgmentOrder {
        populateFields(jpaEntity)
        data {
            judge(jpaEntity.judgeId)
            submission(jpaEntity.submissionId)

            score = jpaEntity.score
            reason = jpaEntity.reason
        }
    }

    /**
     * Creates a new [JudgmentOrderJpaEntity] row from [data].
     *
     * @since 1.0.0-beta.1
     */
    fun toJpaEntity(data: JudgmentOrderData) = JudgmentOrderJpaEntity(
        judgeId = data.judge.id.value,
        submissionId = data.submission.id.value,
        score = data.score.value,
        reason = data.reason,
    )

    /**
     * Creates the [JudgmentOrderJpaEntity] row replacing [current] from [entity],
     * keeping `judgeId`, `submissionId`, `createdAt` and `version`.
     *
     * @since 1.0.0-beta.1
     */
    fun toJpaEntity(entity: JudgmentOrder, current: JudgmentOrderJpaEntity) = JudgmentOrderJpaEntity(
        judgeId = current.judgeId,
        submissionId = current.submissionId,
        score = entity.data.score.value,
        reason = entity.data.reason,
        id = entity.id.value,
    ).also {
        it.createdAt = current.createdAt
        it.version = entity.requireVersion()
    }
}
