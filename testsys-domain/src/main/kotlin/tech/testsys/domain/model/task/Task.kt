package tech.testsys.domain.model.task

import tech.testsys.domain.model.DomainEntity
import tech.testsys.domain.model.DomainId
import tech.testsys.domain.model.LazyEntity
import tech.testsys.domain.model.LazyEntityList
import tech.testsys.domain.model.group.Community
import tech.testsys.domain.model.group.CommunityId
import tech.testsys.domain.model.user.MultipleRoleUser
import tech.testsys.domain.model.user.MultipleRoleUserId
import java.time.Instant

/**
 * Identifier of a [Task].
 *
 * @since %CURRENT_VERSION%
 */
@JvmInline
value class TaskId(
    override val value: Long,
) : DomainId

/**
 * Versioned content of a [Task]. A task keeps at most two revisions: the last committed one, which is complete
 * and usable in contests, and the work-in-progress one being edited by the developer.
 *
 * @since %CURRENT_VERSION%
 */
sealed interface TaskContent {

    /**
     * Content that has never been committed: only a work-in-progress revision exists.
     *
     * @property wip the work-in-progress revision.
     * @since %CURRENT_VERSION%
     */
    data class New(
        val wip: WipTaskContent,
    ) : TaskContent

    /**
     * Content with a committed revision and pending work-in-progress changes on top of it.
     *
     * @property wip the work-in-progress revision.
     * @property lastCommited the last committed revision.
     * @since %CURRENT_VERSION%
     */
    data class Uncommited(
        val wip: WipTaskContent,
        val lastCommited: CommitedTaskContent,
    ) : TaskContent

    /**
     * Content whose latest changes are committed: no work-in-progress revision exists.
     *
     * @property lastCommited the last committed revision.
     * @since %CURRENT_VERSION%
     */
    data class Committed(
        val lastCommited: CommitedTaskContent,
    ) : TaskContent
}

/**
 * A committed revision of [Task] content; both the exercise and the statement are present.
 *
 * @property tests the tests (TRIK Studio world models) solutions are graded against.
 * @property exercise the exercise (TRIK Studio program with a locked world model) given to solvers.
 * @property statement the statement (PDF/TXT document) describing the task.
 * @property developerSolutions the developer's reference solutions.
 * @property supportedTrikStudioVersions the TRIK Studio versions the task can be run with.
 * @since %CURRENT_VERSION%
 */
data class CommitedTaskContent(
    val tests: LazyEntityList<TestId, Test>,
    val exercise: LazyEntity<ExerciseId, Exercise>,
    val statement: LazyEntity<StatementId, Statement>,
    val developerSolutions: LazyEntityList<DeveloperSolutionId, DeveloperSolution>,
    val supportedTrikStudioVersions: List<TrikStudioVersion>,
)

/**
 * A work-in-progress revision of [Task] content; the exercise and the statement may not be uploaded yet.
 *
 * @property tests the tests (TRIK Studio world models) solutions are graded against.
 * @property exercise the exercise given to solvers, or `null` if not uploaded yet.
 * @property statement the statement describing the task, or `null` if not uploaded yet.
 * @property developerSolutions the developer's reference solutions.
 * @property supportedTrikStudioVersions the TRIK Studio versions the task can be run with.
 * @since %CURRENT_VERSION%
 */
data class WipTaskContent(
    val tests: LazyEntityList<TestId, Test>,
    val exercise: LazyEntity<ExerciseId, Exercise>?,
    val statement: LazyEntity<StatementId, Statement>?,
    val developerSolutions: LazyEntityList<DeveloperSolutionId, DeveloperSolution>,
    val supportedTrikStudioVersions: List<TrikStudioVersion>,
)

/**
 * Data of a [Task].
 *
 * @property owner the developer who owns the task.
 * @property name the name of the task.
 * @property description the description of the task.
 * @property sharedTo the communities the task is shared to.
 * @property content the versioned content of the task.
 * @since %CURRENT_VERSION%
 */
data class TaskData(
    val owner: LazyEntity<MultipleRoleUserId, MultipleRoleUser>,
    val name: String,
    val description: String,
    val sharedTo: LazyEntityList<CommunityId, Community>,
    val content: TaskContent,
)

/**
 * A problem to be solved in TRIK Studio, authored by a developer and graded against its tests.
 *
 * @property data the data of the task.
 * @since %CURRENT_VERSION%
 */
class Task(
    id: TaskId,
    createdAt: Instant,
    val data: TaskData,
) : DomainEntity<TaskId>(id, createdAt)
