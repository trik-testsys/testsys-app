package tech.testsys.infra.database.internal.mapping.task

import tech.testsys.domain.builder.task.CommittedTaskContentBuilder
import tech.testsys.domain.builder.task.TaskContentBuilder
import tech.testsys.domain.builder.task.WipTaskContentBuilder
import tech.testsys.domain.model.task.CommittedTaskContent
import tech.testsys.domain.model.task.DeveloperSolutionId
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
 * Mapping between one content revision of a task, [WipTaskContent] or [CommittedTaskContent], and [TaskContentJpaEntity];
 * the sealed `TaskContent` is composed by [TaskMapping].
 *
 * @since %CURRENT_VERSION%
 */
@InternalDatabaseApi
object TaskContentMapping {

    /**
     * Fills [builder] with the work-in-progress [revision]; the exercise and statement references may be missing.
     *
     * @since %CURRENT_VERSION%
     */
    fun populateWip(builder: WipTaskContentBuilder, revision: TaskContentRevision) {
        revision.jpaEntity.requireId()
        builder.populateResources(revision)
        revision.jpaEntity.exerciseId?.let { exerciseId -> builder.exercise(exerciseId) }
        revision.jpaEntity.statementId?.let { statementId -> builder.statement(statementId) }
    }

    /**
     * Fills [builder] with the committed [revision]; fails when the exercise or statement reference is missing.
     *
     * @since %CURRENT_VERSION%
     */
    fun populateCommitted(builder: CommittedTaskContentBuilder, revision: TaskContentRevision) {
        val exerciseId = requireNotNull(revision.jpaEntity.exerciseId) {
            "TaskContent ${revision.jpaEntity.requireId()} marks committed payload but exerciseId is null"
        }
        val statementId = requireNotNull(revision.jpaEntity.statementId) {
            "TaskContent ${revision.jpaEntity.requireId()} marks committed payload but statementId is null"
        }
        builder.populateResources(revision)
        builder.exercise(exerciseId)
        builder.statement(statementId)
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
    fun toJpaEntity(content: CommittedTaskContent) = TaskContentJpaEntity(
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

    private fun TaskContentBuilder<*>.populateResources(revision: TaskContentRevision) {
        tests = revision.testIds.toMutableList()
        developerSolutions = revision.developerSolutionIds.toMutableList()
        supportedTrikStudioVersions = revision.supportedVersions.toMutableList()
    }
}

/**
 * One stored content revision of a task: its row together with the data read from join tables and dictionaries.
 *
 * @property jpaEntity the row of the revision.
 * @property testIds the ids of the tests linked to the revision.
 * @property developerSolutionIds the ids of the developer solutions linked to the revision.
 * @property supportedVersions the TRIK Studio versions linked to the revision.
 * @since %CURRENT_VERSION%
 */
@InternalDatabaseApi
data class TaskContentRevision(
    val jpaEntity: TaskContentJpaEntity,
    val testIds: List<TestId>,
    val developerSolutionIds: List<DeveloperSolutionId>,
    val supportedVersions: List<TrikStudioVersion>,
)
