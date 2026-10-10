package tech.testsys.infra.database.internal.mapping.task

import tech.testsys.domain.builder.task.CommittedTaskContentBuilder
import tech.testsys.domain.builder.task.TaskContentBuilder
import tech.testsys.domain.builder.task.WipTaskContentBuilder
import tech.testsys.domain.model.task.CommittedTaskContent
import tech.testsys.domain.model.task.DeveloperSolutionId
import tech.testsys.domain.model.task.ExerciseId
import tech.testsys.domain.model.task.TestId
import tech.testsys.domain.model.task.TrikStudioVersion
import tech.testsys.domain.model.task.WipTaskContent
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.task.DeveloperSolutionToTaskContentJpaEntity
import tech.testsys.infra.database.internal.jpa.entity.task.ExerciseToTaskContentJpaEntity
import tech.testsys.infra.database.internal.jpa.entity.task.TaskContentJpaEntity
import tech.testsys.infra.database.internal.jpa.entity.task.TestToTaskContentJpaEntity
import tech.testsys.infra.database.internal.jpa.entity.task.TrikStudioVersionToTaskContentJpaEntity
import tech.testsys.infra.database.internal.utils.requireId

/**
 * Mapping between one content revision of a task, [WipTaskContent] or [CommittedTaskContent], and [TaskContentJpaEntity];
 * the sealed `TaskContent` is composed by [TaskMapping].
 *
 * @since %CURRENT_VERSION%
 */
@InternalDatabaseApi
object TaskContentMapping {

    /**
     * Fills [builder] with the work-in-progress [revision]; exercises may be empty and the statement reference may be missing.
     *
     * @since %CURRENT_VERSION%
     */
    fun populateWip(builder: WipTaskContentBuilder, revision: TaskContentRevision) {
        revision.jpaEntity.requireId()
        builder.populateResources(revision)
        revision.jpaEntity.statementId?.let { statementId -> builder.statement(statementId) }
    }

    /**
     * Fills [builder] with the committed [revision]; fails when exercises are empty or the statement reference is missing.
     *
     * @since %CURRENT_VERSION%
     */
    fun populateCommitted(builder: CommittedTaskContentBuilder, revision: TaskContentRevision) {
        require(revision.exerciseIds.isNotEmpty()) {
            "TaskContent ${revision.jpaEntity.requireId()} marks committed payload but exerciseIds is empty"
        }
        val statementId = requireNotNull(revision.jpaEntity.statementId) {
            "TaskContent ${revision.jpaEntity.requireId()} marks committed payload but statementId is null"
        }
        builder.populateResources(revision)
        builder.statement(statementId)
    }

    /**
     * Creates a new [TaskContentJpaEntity] row for the WIP [content].
     *
     * @since %CURRENT_VERSION%
     */
    fun toJpaEntity(content: WipTaskContent) = TaskContentJpaEntity(
        statementId = content.statement?.id?.value,
    )

    /**
     * Creates a new [TaskContentJpaEntity] row for the committed [content].
     *
     * @since %CURRENT_VERSION%
     */
    fun toJpaEntity(content: CommittedTaskContent) = TaskContentJpaEntity(
        statementId = content.statement.id.value,
    )

    /**
     * Creates the [ExerciseToTaskContentJpaEntity] rows linking the revision [taskContentId] with [exerciseIds], one row per distinct id.
     *
     * @since %CURRENT_VERSION%
     */
    fun toExerciseAssociations(taskContentId: Long, exerciseIds: List<ExerciseId>) = exerciseIds.distinct().map {
        ExerciseToTaskContentJpaEntity(exerciseId = it.value, taskContentId = taskContentId)
    }

    /**
     * Creates the [TestToTaskContentJpaEntity] rows linking the revision [taskContentId] with [testIds], one row per distinct id.
     *
     * @since %CURRENT_VERSION%
     */
    fun toTestAssociations(taskContentId: Long, testIds: List<TestId>) = testIds.distinct().map {
        TestToTaskContentJpaEntity(testId = it.value, taskContentId = taskContentId)
    }

    /**
     * Creates the [DeveloperSolutionToTaskContentJpaEntity] rows linking the revision [taskContentId] with
     * [developerSolutionIds], one row per distinct id.
     *
     * @since %CURRENT_VERSION%
     */
    fun toDeveloperSolutionAssociations(taskContentId: Long, developerSolutionIds: List<DeveloperSolutionId>) =
        developerSolutionIds.distinct().map {
            DeveloperSolutionToTaskContentJpaEntity(developerSolutionId = it.value, taskContentId = taskContentId)
        }

    /**
     * Creates the [TrikStudioVersionToTaskContentJpaEntity] rows linking the revision [taskContentId] with
     * [trikStudioVersionIds], one row per distinct id.
     *
     * @since %CURRENT_VERSION%
     */
    fun toTrikStudioVersionAssociations(taskContentId: Long, trikStudioVersionIds: List<Long>) = trikStudioVersionIds.distinct().map {
        TrikStudioVersionToTaskContentJpaEntity(trikStudioVersionId = it, taskContentId = taskContentId)
    }

    private fun TaskContentBuilder<*>.populateResources(revision: TaskContentRevision) {
        exercises = revision.exerciseIds.toMutableList()
        tests = revision.testIds.toMutableList()
        developerSolutions = revision.developerSolutionIds.toMutableList()
        supportedTrikStudioVersions = revision.supportedVersions.toMutableList()
    }
}

/**
 * One stored content revision of a task: its row together with the data read from join tables and dictionaries.
 *
 * @property jpaEntity the row of the revision.
 * @property exerciseIds the ids of the exercises linked to the revision.
 * @property testIds the ids of the tests linked to the revision.
 * @property developerSolutionIds the ids of the developer solutions linked to the revision.
 * @property supportedVersions the TRIK Studio versions linked to the revision.
 * @since %CURRENT_VERSION%
 */
@InternalDatabaseApi
data class TaskContentRevision(
    val jpaEntity: TaskContentJpaEntity,
    val exerciseIds: List<ExerciseId>,
    val testIds: List<TestId>,
    val developerSolutionIds: List<DeveloperSolutionId>,
    val supportedVersions: List<TrikStudioVersion>,
)
