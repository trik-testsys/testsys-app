package tech.testsys.web.app.service

import tech.testsys.domain.model.group.CommunityId
import tech.testsys.domain.model.task.CommittedTaskContent
import tech.testsys.domain.model.task.DeveloperSolutionId
import tech.testsys.domain.model.task.ExerciseId
import tech.testsys.domain.model.task.StatementId
import tech.testsys.domain.model.task.Task
import tech.testsys.domain.model.task.TaskContent
import tech.testsys.domain.model.task.TaskId
import tech.testsys.domain.model.task.TestId
import tech.testsys.domain.model.task.TrikStudioVersion
import tech.testsys.domain.model.task.VersionBucket
import tech.testsys.domain.model.task.WipTaskContent
import tech.testsys.domain.model.user.MultipleRoleUserId
import java.time.Instant

/**
 * Task data for pages, with links replaced by identifiers.
 *
 * @property id the identifier of the task.
 * @property createdAt the moment the task was created.
 * @property owner the identifier of the owner.
 * @property name the name of the task.
 * @property description the description of the task.
 * @property sharedTo the identifiers of the communities the task is shared to.
 * @property wip the working revision, or `null` if the task has no uncommitted changes.
 * @property lastCommitted the last committed revision, or `null` if the task was never committed.
 * @property uploadedResources the version buckets of the resources uploaded to the task.
 * @since %CURRENT_VERSION%
 */
data class TaskVo(
    val id: TaskId,
    val createdAt: Instant,
    val owner: MultipleRoleUserId,
    val name: String,
    val description: String,
    val sharedTo: List<CommunityId>,
    val wip: TaskRevisionVo?,
    val lastCommitted: TaskRevisionVo?,
    val uploadedResources: Set<VersionBucket>,
)

/**
 * Resources of one revision of a [TaskVo].
 *
 * @property tests the identifiers of the tests.
 * @property exercises the identifiers of the exercises.
 * @property statement the identifier of the statement, or `null` if a working revision has none yet.
 * @property developerSolutions the identifiers of the developer solutions.
 * @property supportedTrikStudioVersions the supported TRIK Studio versions.
 * @since %CURRENT_VERSION%
 */
data class TaskRevisionVo(
    val tests: List<TestId>,
    val exercises: List<ExerciseId>,
    val statement: StatementId?,
    val developerSolutions: List<DeveloperSolutionId>,
    val supportedTrikStudioVersions: List<TrikStudioVersion>,
)

internal fun Task.toVo(): TaskVo {
    val (wip, lastCommitted) = when (val content = data.content) {
        is TaskContent.New -> content.wip.toVo() to null
        is TaskContent.Uncommitted -> content.wip.toVo() to content.lastCommitted.toVo()
        is TaskContent.Committed -> null to content.lastCommitted.toVo()
    }

    return TaskVo(
        id = id,
        createdAt = createdAt,
        owner = data.owner.id,
        name = data.name,
        description = data.description,
        sharedTo = data.sharedTo.ids,
        wip = wip,
        lastCommitted = lastCommitted,
        uploadedResources = data.uploadedResources,
    )
}

private fun WipTaskContent.toVo() = TaskRevisionVo(
    tests = tests.ids,
    exercises = exercises.ids,
    statement = statement?.id,
    developerSolutions = developerSolutions.ids,
    supportedTrikStudioVersions = supportedTrikStudioVersions,
)

private fun CommittedTaskContent.toVo() = TaskRevisionVo(
    tests = tests.ids,
    exercises = exercises.ids,
    statement = statement.id,
    developerSolutions = developerSolutions.ids,
    supportedTrikStudioVersions = supportedTrikStudioVersions,
)
