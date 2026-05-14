package tech.testsys.infra.database.internal.mapping.task

import tech.testsys.domain.builder.api.judgmentOrder
import tech.testsys.domain.builder.data
import tech.testsys.domain.model.task.JudgmentOrder
import tech.testsys.domain.model.task.JudgmentOrderData
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.task.JudgmentOrderJpaEntity
import tech.testsys.infra.database.internal.mapping.EntityMapping
import tech.testsys.infra.database.internal.utils.populateFields

@InternalDatabaseApi
object JudgmentOrderMapping : EntityMapping<JudgmentOrder, JudgmentOrderJpaEntity> {

    fun toDomain(jpaEntity: JudgmentOrderJpaEntity) = judgmentOrder {
        populateFields(jpaEntity)
        data {
            judge(jpaEntity.judgeId)
            verdict(jpaEntity.verdictId)

            reason = jpaEntity.reason
        }
    }

    fun toJpaEntity(data: JudgmentOrderData) = JudgmentOrderJpaEntity(
        judgeId = data.judge.id.value,
        verdictId = data.verdict.id.value,
        reason = data.reason,
    )

    fun toJpaEntity(entity: JudgmentOrder, current: JudgmentOrderJpaEntity) = JudgmentOrderJpaEntity(
        judgeId = entity.data.judge.id.value,
        verdictId = entity.data.verdict.id.value,
        reason = entity.data.reason,
        id = entity.id.value,
    ).also {
        it.createdAt = current.createdAt
        it.version = current.version
    }
}
