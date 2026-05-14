package tech.testsys.infra.database.internal.mapping.task

import tech.testsys.domain.builder.api.verdict
import tech.testsys.domain.builder.data
import tech.testsys.domain.model.task.Verdict
import tech.testsys.domain.model.task.VerdictData
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.task.VerdictJpaEntity
import tech.testsys.infra.database.internal.mapping.EntityMapping
import tech.testsys.infra.database.internal.utils.populateFields

@InternalDatabaseApi
object VerdictMapping : EntityMapping<Verdict, VerdictJpaEntity> {

    fun toDomain(jpaEntity: VerdictJpaEntity) = verdict {
        populateFields(jpaEntity)

        data {
            score = jpaEntity.score

            task(jpaEntity.taskId)
            submission(jpaEntity.submissionId)
            jpaEntity.logsId?.let { logs(it) }
            jpaEntity.recordingId?.let { recording(it) }
        }
    }

    fun toJpaEntity(data: VerdictData) = VerdictJpaEntity(
        score = data.score.value,
        taskId = data.task.id.value,
        submissionId = data.submission.id.value,
        logsId = data.logs?.id?.value,
        recordingId = data.recording?.id?.value,
    )

    fun toJpaEntity(entity: Verdict, current: VerdictJpaEntity) = VerdictJpaEntity(
        score = entity.data.score.value,
        taskId = entity.data.task.id.value,
        submissionId = entity.data.submission.id.value,
        logsId = entity.data.logs?.id?.value,
        recordingId = entity.data.recording?.id?.value,
        id = entity.id.value,
    ).also {
        it.createdAt = current.createdAt
        it.version = current.version
    }
}
