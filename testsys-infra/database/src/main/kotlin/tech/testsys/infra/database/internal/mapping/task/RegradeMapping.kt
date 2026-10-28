package tech.testsys.infra.database.internal.mapping.task

import tech.testsys.domain.builder.api.regrade
import tech.testsys.domain.builder.data
import tech.testsys.domain.model.task.Regrade
import tech.testsys.domain.model.task.RegradeData
import tech.testsys.domain.model.task.SubmissionId
import tech.testsys.domain.model.task.TestId
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.task.RegradeJpaEntity
import tech.testsys.infra.database.internal.jpa.entity.task.SubmissionToRegradeJpaEntity
import tech.testsys.infra.database.internal.jpa.entity.task.TestToRegradeJpaEntity
import tech.testsys.infra.database.internal.mapping.EntityMapping
import tech.testsys.infra.database.internal.utils.populateFields

/**
 * Mapping between [Regrade] and [RegradeJpaEntity].
 *
 * @since %CURRENT_VERSION%
 */
@InternalDatabaseApi
object RegradeMapping : EntityMapping<Regrade, RegradeJpaEntity> {

    /**
     * Assembles a [Regrade] from [jpaEntity] and the ids of its tests and submissions.
     *
     * @since %CURRENT_VERSION%
     */
    fun toDomain(jpaEntity: RegradeJpaEntity, testIds: List<TestId>, submissionIds: List<SubmissionId>) = regrade {
        populateFields(jpaEntity)
        data {
            task(jpaEntity.taskId)

            tests = testIds.toMutableList()
            submissions = submissionIds.toMutableList()
        }
    }

    /**
     * Creates a new [RegradeJpaEntity] row from [data].
     *
     * @since %CURRENT_VERSION%
     */
    fun toJpaEntity(data: RegradeData) = RegradeJpaEntity(
        taskId = data.task.id.value,
    )

    /**
     * Creates the [TestToRegradeJpaEntity] rows linking the regrade [regradeId] with [testIds], one row per distinct id.
     *
     * @since %CURRENT_VERSION%
     */
    fun toTestAssociations(regradeId: Long, testIds: List<TestId>) = testIds.distinct().map { testId ->
        TestToRegradeJpaEntity(
            regradeId = regradeId,
            testId = testId.value,
        )
    }

    /**
     * Creates the [SubmissionToRegradeJpaEntity] rows linking the regrade [regradeId] with [submissionIds], one row per
     * distinct id.
     *
     * @since %CURRENT_VERSION%
     */
    fun toSubmissionAssociations(regradeId: Long, submissionIds: List<SubmissionId>) = submissionIds.distinct().map { submissionId ->
        SubmissionToRegradeJpaEntity(
            regradeId = regradeId,
            submissionId = submissionId.value,
        )
    }
}
