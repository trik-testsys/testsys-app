package tech.testsys.infra.database.internal.mapping.task

import tech.testsys.domain.builder.util.lazify
import tech.testsys.domain.model.LazyEntity
import tech.testsys.domain.model.task.CommitedTaskContent
import tech.testsys.domain.model.task.DeveloperSolutionId
import tech.testsys.domain.model.task.ExerciseId
import tech.testsys.domain.model.task.StatementId
import tech.testsys.domain.model.task.TestId
import tech.testsys.domain.model.task.TrikStudioVersion
import tech.testsys.domain.model.task.WipTaskContent
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.task.DeveloperSolutionToTaskContentJpaEntity
import tech.testsys.infra.database.internal.jpa.entity.task.TaskContentJpaEntity
import tech.testsys.infra.database.internal.jpa.entity.task.TestToTaskContentJpaEntity
import tech.testsys.infra.database.internal.jpa.entity.task.TrikStudioVersionToTaskContentJpaEntity
import tech.testsys.infra.database.internal.utils.requireId

/**
 * Mapping between one content revision of a task, [WipTaskContent] or [CommitedTaskContent], and [TaskContentJpaEntity];
 * the sealed `TaskContent` is composed by [TaskMapping].
 *
 * @since %CURRENT_VERSION%
 */
@InternalDatabaseApi
object TaskContentMapping {

    /**
     * Assembles the [WipTaskContent] of the revision [jpaEntity] from [testIds], [developerSolutionIds] and [supportedVersions].
     *
     * @since %CURRENT_VERSION%
     */
    fun toWipDomain(
        jpaEntity: TaskContentJpaEntity,
        testIds: List<TestId>,
        developerSolutionIds: List<DeveloperSolutionId>,
        supportedVersions: List<TrikStudioVersion>,
    ): WipTaskContent {
        jpaEntity.requireId()
        return WipTaskContent(
            tests = testIds.lazify(),
            exercise = jpaEntity.exerciseId?.let { LazyEntity(ExerciseId(it)) },
            statement = jpaEntity.statementId?.let { LazyEntity(StatementId(it)) },
            developerSolutions = developerSolutionIds.lazify(),
            supportedTrikStudioVersions = supportedVersions,
        )
    }

    /**
     * Assembles the [CommitedTaskContent] of the revision [jpaEntity] from [testIds], [developerSolutionIds]
     * and [supportedVersions]; fails when the exercise or statement reference is missing.
     *
     * @since %CURRENT_VERSION%
     */
    fun toCommitedDomain(
        jpaEntity: TaskContentJpaEntity,
        testIds: List<TestId>,
        developerSolutionIds: List<DeveloperSolutionId>,
        supportedVersions: List<TrikStudioVersion>,
    ): CommitedTaskContent {
        val exerciseId = requireNotNull(jpaEntity.exerciseId) {
            "TaskContent ${jpaEntity.requireId()} marks committed payload but exerciseId is null"
        }
        val statementId = requireNotNull(jpaEntity.statementId) {
            "TaskContent ${jpaEntity.requireId()} marks committed payload but statementId is null"
        }
        return CommitedTaskContent(
            tests = testIds.lazify(),
            exercise = ExerciseId(exerciseId).lazify(),
            statement = StatementId(statementId).lazify(),
            developerSolutions = developerSolutionIds.lazify(),
            supportedTrikStudioVersions = supportedVersions,
        )
    }

    /**
     * Creates a new [TaskContentJpaEntity] row for the WIP [content].
     *
     * @since %CURRENT_VERSION%
     */
    fun toJpaEntity(content: WipTaskContent) = TaskContentJpaEntity(
        exerciseId = content.exercise?.id?.value,
        statementId = content.statement?.id?.value,
    )

    /**
     * Creates a new [TaskContentJpaEntity] row for the committed [content].
     *
     * @since %CURRENT_VERSION%
     */
    fun toJpaEntity(content: CommitedTaskContent) = TaskContentJpaEntity(
        exerciseId = content.exercise.id.value,
        statementId = content.statement.id.value,
    )

    /**
     * Creates the [TestToTaskContentJpaEntity] rows linking the revision [taskContentId] with [testIds].
     *
     * @since %CURRENT_VERSION%
     */
    fun toTestAssociations(taskContentId: Long, testIds: List<TestId>) = testIds.map {
        TestToTaskContentJpaEntity(testId = it.value, taskContentId = taskContentId)
    }

    /**
     * Creates the [DeveloperSolutionToTaskContentJpaEntity] rows linking the revision [taskContentId] with [developerSolutionIds].
     *
     * @since %CURRENT_VERSION%
     */
    fun toDeveloperSolutionAssociations(taskContentId: Long, developerSolutionIds: List<DeveloperSolutionId>) = developerSolutionIds.map {
        DeveloperSolutionToTaskContentJpaEntity(developerSolutionId = it.value, taskContentId = taskContentId)
    }

    /**
     * Creates the [TrikStudioVersionToTaskContentJpaEntity] rows linking the revision [taskContentId] with [trikStudioVersionIds].
     *
     * @since %CURRENT_VERSION%
     */
    fun toTrikStudioVersionAssociations(taskContentId: Long, trikStudioVersionIds: List<Long>) = trikStudioVersionIds.map {
        TrikStudioVersionToTaskContentJpaEntity(trikStudioVersionId = it, taskContentId = taskContentId)
    }
}
