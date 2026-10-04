package tech.testsys.infra.database.internal.mapping.task

import tech.testsys.domain.builder.api.verdict
import tech.testsys.domain.builder.data
import tech.testsys.domain.model.task.TestVerdict
import tech.testsys.domain.model.task.Verdict
import tech.testsys.domain.model.task.VerdictData
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.task.TestVerdictJpaEntity
import tech.testsys.infra.database.internal.jpa.entity.task.VerdictJpaEntity
import tech.testsys.infra.database.internal.mapping.EntityMapping
import tech.testsys.infra.database.internal.utils.populateFields

/**
 * Mapping between [Verdict] and [VerdictJpaEntity]; the outcome of every test run is kept in [TestVerdictJpaEntity] rows.
 *
 * @since %CURRENT_VERSION%
 */
@InternalDatabaseApi
object VerdictMapping : EntityMapping<Verdict, VerdictJpaEntity> {

    /**
     * Assembles a [Verdict] from [jpaEntity] and the rows [testVerdictJpaEntities] holding the outcome of its test runs.
     *
     * @since %CURRENT_VERSION%
     */
    fun toDomain(jpaEntity: VerdictJpaEntity, testVerdictJpaEntities: List<TestVerdictJpaEntity>) = verdict {
        populateFields(jpaEntity)

        data {
            task(jpaEntity.taskId)
            submission(jpaEntity.submissionId)

            testVerdictJpaEntities.forEach { row ->
                testVerdict {
                    score = row.score

                    test(row.testId)
                    logs(row.logsId)
                    row.recordingId?.let { recording(it) }
                }
            }
        }
    }

    /**
     * Creates a new [VerdictJpaEntity] row from [data].
     *
     * @since %CURRENT_VERSION%
     */
    fun toJpaEntity(data: VerdictData) = VerdictJpaEntity(
        taskId = data.task.id.value,
        submissionId = data.submission.id.value,
    )

    /**
     * Creates the [TestVerdictJpaEntity] rows holding [testVerdicts] of the verdict [verdictId].
     *
     * @since %CURRENT_VERSION%
     */
    fun toTestVerdictAssociations(verdictId: Long, testVerdicts: List<TestVerdict>) = testVerdicts.map {
        TestVerdictJpaEntity(
            verdictId = verdictId,
            testId = it.test.id.value,
            score = it.score.value,
            logsId = it.logs.id.value,
            recordingId = it.recording?.id?.value,
        )
    }
}
