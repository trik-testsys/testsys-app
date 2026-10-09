package tech.testsys.domain.contract.persistence.repository

import tech.testsys.domain.contract.StoredBlobRef
import tech.testsys.domain.contract.persistence.ContestFilter
import tech.testsys.domain.contract.persistence.ContestTaskResult
import tech.testsys.domain.contract.persistence.ObserverContestFilter
import tech.testsys.domain.contract.persistence.Page
import tech.testsys.domain.contract.persistence.Pagination
import tech.testsys.domain.contract.persistence.SubmissionCount
import tech.testsys.domain.contract.persistence.TaskFilter
import tech.testsys.domain.contract.persistence.VerdictFilter
import tech.testsys.domain.model.group.CommunityId
import tech.testsys.domain.model.task.AuthorSubmissionFailure
import tech.testsys.domain.model.task.Contest
import tech.testsys.domain.model.task.ContestData
import tech.testsys.domain.model.task.ContestId
import tech.testsys.domain.model.task.DeveloperSolution
import tech.testsys.domain.model.task.DeveloperSolutionData
import tech.testsys.domain.model.task.DeveloperSolutionId
import tech.testsys.domain.model.task.Exercise
import tech.testsys.domain.model.task.ExerciseData
import tech.testsys.domain.model.task.ExerciseId
import tech.testsys.domain.model.task.JudgmentOrder
import tech.testsys.domain.model.task.JudgmentOrderData
import tech.testsys.domain.model.task.JudgmentOrderId
import tech.testsys.domain.model.task.Logs
import tech.testsys.domain.model.task.LogsData
import tech.testsys.domain.model.task.LogsId
import tech.testsys.domain.model.task.Recording
import tech.testsys.domain.model.task.RecordingData
import tech.testsys.domain.model.task.RecordingId
import tech.testsys.domain.model.task.Solution
import tech.testsys.domain.model.task.SolutionData
import tech.testsys.domain.model.task.SolutionId
import tech.testsys.domain.model.task.Statement
import tech.testsys.domain.model.task.StatementData
import tech.testsys.domain.model.task.StatementId
import tech.testsys.domain.model.task.Submission
import tech.testsys.domain.model.task.SubmissionData
import tech.testsys.domain.model.task.SubmissionId
import tech.testsys.domain.model.task.SubmissionKind
import tech.testsys.domain.model.task.Task
import tech.testsys.domain.model.task.TaskData
import tech.testsys.domain.model.task.TaskId
import tech.testsys.domain.model.task.TaskValidationRequest
import tech.testsys.domain.model.task.TaskValidationRequestData
import tech.testsys.domain.model.task.TaskValidationRequestId
import tech.testsys.domain.model.task.TaskValidationSnapshot
import tech.testsys.domain.model.task.TaskValidationTechnicalFailure
import tech.testsys.domain.model.task.Test
import tech.testsys.domain.model.task.TestData
import tech.testsys.domain.model.task.TestDiagnosticResult
import tech.testsys.domain.model.task.TestId
import tech.testsys.domain.model.task.Verdict
import tech.testsys.domain.model.task.VerdictData
import tech.testsys.domain.model.task.VerdictId
import tech.testsys.domain.model.task.VersionBucket
import tech.testsys.domain.model.user.MultipleRoleUserId
import tech.testsys.domain.model.user.UserId

/**
 * Persistence port for [Contest] entities.
 *
 * @since %CURRENT_VERSION%
 */
interface ContestRepository : EntityRepository<ContestData, ContestId, Contest> {

    /**
     * Synchronously finds the assigned [contestIds], without changing stored state.
     * Repeated calls reflect current relations and preserve all time limits; technical storage exceptions propagate.
     *
     * @param contestIds contests assigned to the observer; an empty set matches nothing.
     * @param pagination the requested page; id ascending is the default order and breaks ties unless explicitly sorted.
     * @param filter conditions combined with AND before paging and counting; missing or inaccessible ids match nothing.
     * @return unique contests, original pagination and exact filtered total; missing pages are empty but retain the total.
     * @since %CURRENT_VERSION%
     */
    fun findAvailableToObserver(
        contestIds: Set<ContestId>,
        pagination: Pagination,
        filter: ObserverContestFilter = ObserverContestFilter(),
    ): Page<Contest>

    /**
     * Synchronously finds all contests currently containing [taskId], ordered by identifier.
     * Repeated calls reflect current associations without changing state; technical storage exceptions propagate.
     *
     * @return the attached contests, or an empty list when no association exists.
     * @since %CURRENT_VERSION%
     */
    fun findByTaskId(taskId: TaskId): List<Contest>

    /**
     * Synchronously finds contests owned by [ownerId] or shared to any of [communityIds], without changing stored state.
     * Repeated calls reflect current data; storage exceptions propagate to the caller.
     *
     * @param ownerId the developer whose own contests are included.
     * @param communityIds the communities granting access; an empty set searches only by owner.
     * @param pagination the requested page; id ascending is the default order and breaks ties unless explicitly sorted.
     * @param filter conditions combined with AND before paging and counting, including shared community; unknown ids match nothing.
     * @return distinct authorized contests, the original pagination and exact filtered total; missing pages are empty.
     * @since %CURRENT_VERSION%
     */
    fun findAvailableToDeveloper(
        ownerId: MultipleRoleUserId,
        communityIds: Set<CommunityId>,
        pagination: Pagination,
        filter: ContestFilter = ContestFilter(),
    ): Page<Contest>
}

/**
 * Persistence port for [DeveloperSolution] entities. The solution and expected score are fixed on creation: `update`
 * with another value throws [UnsupportedOperationException], a new version is saved as a new developer solution in
 * the same version bucket.
 *
 * @since %CURRENT_VERSION%
 */
interface DeveloperSolutionRepository : EntityRepository<DeveloperSolutionData, DeveloperSolutionId, DeveloperSolution> {

    /**
     * Finds the latest version in [versionBucket], ordered by creation time and then by id, both descending.
     * Metadata updates do not change this order.
     *
     * @param versionBucket the resource version chain to search.
     * @return the latest version, or `null` if the chain has no versions.
     * @since %CURRENT_VERSION%
     */
    fun findLatestByVersionBucket(versionBucket: VersionBucket): DeveloperSolution?

    /**
     * Synchronously finds all existing versions in [versionBucket].
     * Repeated calls reflect current metadata and may load files; they do not change stored state.
     *
     * @param versionBucket the resource version chain to search.
     * @return existing versions, or an empty list for a missing chain.
     * @since %CURRENT_VERSION%
     */
    fun findVersionsByVersionBucket(versionBucket: VersionBucket): List<DeveloperSolution>

    /**
     * Synchronously checks whether [versionBucket] contains a version, without reading file contents.
     *
     * @param versionBucket the resource version chain to search.
     * @return whether any version exists in the chain.
     * @since %CURRENT_VERSION%
     */
    fun existsByVersionBucket(versionBucket: VersionBucket): Boolean

    /**
     * Synchronously returns the existing file reference of [id], without reading bytes or changing stored state.
     * Repeated calls return the same reference for an existing version.
     *
     * @param versionBucket the chain the requested version must belong to.
     * @param id the typed version identifier.
     * @return the persisted reference, or `null` if the version is missing or belongs to another chain.
     * @since %CURRENT_VERSION%
     */
    fun findFileRef(versionBucket: VersionBucket, id: DeveloperSolutionId): StoredBlobRef?
}

/**
 * Persistence port for [Exercise] entities. The file and language are fixed on creation: `update` with another value
 * throws [UnsupportedOperationException], a new version is saved as a new exercise in the same version bucket.
 *
 * @since %CURRENT_VERSION%
 */
interface ExerciseRepository : EntityRepository<ExerciseData, ExerciseId, Exercise> {

    /**
     * Finds the latest version in [versionBucket], ordered by creation time and then by id, both descending.
     * Metadata updates do not change this order.
     *
     * @param versionBucket the resource version chain to search.
     * @return the latest version, or `null` if the chain has no versions.
     * @since %CURRENT_VERSION%
     */
    fun findLatestByVersionBucket(versionBucket: VersionBucket): Exercise?

    /**
     * Synchronously finds all existing versions in [versionBucket].
     * Repeated calls reflect current metadata and may load files; they do not change stored state.
     *
     * @param versionBucket the resource version chain to search.
     * @return existing versions, or an empty list for a missing chain.
     * @since %CURRENT_VERSION%
     */
    fun findVersionsByVersionBucket(versionBucket: VersionBucket): List<Exercise>

    /**
     * Synchronously checks whether [versionBucket] contains a version, without reading file contents.
     *
     * @param versionBucket the resource version chain to search.
     * @return whether any version exists in the chain.
     * @since %CURRENT_VERSION%
     */
    fun existsByVersionBucket(versionBucket: VersionBucket): Boolean

    /**
     * Synchronously returns the existing file reference of [id], without reading bytes or changing stored state.
     * Repeated calls return the same reference for an existing version.
     *
     * @param versionBucket the chain the requested version must belong to.
     * @param id the typed version identifier.
     * @return the persisted reference, or `null` if the version is missing or belongs to another chain.
     * @since %CURRENT_VERSION%
     */
    fun findFileRef(versionBucket: VersionBucket, id: ExerciseId): StoredBlobRef?
}

/**
 * Persistence port for [JudgmentOrder] entities.
 *
 * @since %CURRENT_VERSION%
 */
interface JudgmentOrderRepository : EntityRepository<JudgmentOrderData, JudgmentOrderId, JudgmentOrder>

/**
 * Persistence port for [Logs] entities. Logs are fixed on creation, so `update` is not supported.
 *
 * @since %CURRENT_VERSION%
 */
interface LogsRepository : EntityRepository<LogsData, LogsId, Logs>

/**
 * Persistence port for [Recording] entities. A recording is fixed on creation, so `update` is not supported.
 *
 * @since %CURRENT_VERSION%
 */
interface RecordingRepository : EntityRepository<RecordingData, RecordingId, Recording>

/**
 * Persistence port for [Solution] entities. A solution is fixed on creation, so `update` is not supported.
 *
 * @since %CURRENT_VERSION%
 */
interface SolutionRepository : EntityRepository<SolutionData, SolutionId, Solution>

/**
 * Persistence port for [Statement] entities. The file is fixed on creation: `update` with another file
 * throws [UnsupportedOperationException], a new version is saved as a new statement in the same version bucket.
 *
 * @since %CURRENT_VERSION%
 */
interface StatementRepository : EntityRepository<StatementData, StatementId, Statement> {

    /**
     * Finds the latest version in [versionBucket], ordered by creation time and then by id, both descending.
     * Metadata updates do not change this order.
     *
     * @param versionBucket the resource version chain to search.
     * @return the latest version, or `null` if the chain has no versions.
     * @since %CURRENT_VERSION%
     */
    fun findLatestByVersionBucket(versionBucket: VersionBucket): Statement?

    /**
     * Synchronously finds all existing versions in [versionBucket].
     * Repeated calls reflect current metadata and may load files; they do not change stored state.
     *
     * @param versionBucket the resource version chain to search.
     * @return existing versions, or an empty list for a missing chain.
     * @since %CURRENT_VERSION%
     */
    fun findVersionsByVersionBucket(versionBucket: VersionBucket): List<Statement>

    /**
     * Synchronously checks whether [versionBucket] contains a version, without reading file contents.
     *
     * @param versionBucket the resource version chain to search.
     * @return whether any version exists in the chain.
     * @since %CURRENT_VERSION%
     */
    fun existsByVersionBucket(versionBucket: VersionBucket): Boolean

    /**
     * Synchronously returns the existing file reference of [id], without reading bytes or changing stored state.
     * Repeated calls return the same reference for an existing version.
     *
     * @param versionBucket the chain the requested version must belong to.
     * @param id the typed version identifier.
     * @return the persisted reference, or `null` if the version is missing or belongs to another chain.
     * @since %CURRENT_VERSION%
     */
    fun findFileRef(versionBucket: VersionBucket, id: StatementId): StoredBlobRef?
}

/**
 * Persistence port for [Submission] entities.
 *
 * @since %CURRENT_VERSION%
 */
interface SubmissionRepository : EntityRepository<SubmissionData, SubmissionId, Submission> {

    /**
     * Synchronously finds contest submissions of [taskId], excluding author solution tests, without changing state.
     * Repeated calls reflect current data; storage exceptions propagate to the caller.
     *
     * @param taskId the task the submissions were made to.
     * @return the submissions of kind [SubmissionKind.Grading] ordered by identifier, or an empty list if none exist.
     * @since %CURRENT_VERSION%
     */
    fun findGradingByTaskId(taskId: TaskId): List<Submission>

    /**
     * Synchronously finds grading submissions of [authorId] for [taskId] in [contestId], without changing stored state.
     * Storage exceptions propagate to the caller.
     *
     * @param authorId the user who submitted the solutions.
     * @param taskId the task the solutions were submitted to.
     * @param contestId the contest the solutions were submitted in.
     * @return matching submissions ordered by creation time and then by id, both ascending; an empty list if none match.
     * @since %CURRENT_VERSION%
     */
    fun findGradingByContext(authorId: UserId, taskId: TaskId, contestId: ContestId): List<Submission>

    /**
     * Synchronously summarizes grading submissions of [contestId] per author and task, without changing stored state
     * or loading file contents. Repeated calls reflect current data; technical exceptions propagate to the caller.
     *
     * @param contestId the contest the submissions were made in.
     * @param authorIds the authors to include; an empty set yields an empty list.
     * @param taskIds the tasks to include; an empty set yields an empty list.
     * @return results of author and task pairs having submissions, ordered by author id and then task id; a submission
     * result is the score of its latest judgment order, otherwise the total of its verdict.
     * @since %CURRENT_VERSION%
     */
    fun findContestResults(contestId: ContestId, authorIds: Set<UserId>, taskIds: Set<TaskId>): List<ContestTaskResult>

    /**
     * Synchronously counts grading submissions of [contestId] per task in any grading state, excluding author solution
     * tests, without changing stored state or loading file contents. Storage exceptions propagate to the caller.
     *
     * @param contestId the contest the submissions were made in.
     * @return the number of submissions of every task having submissions; tasks without submissions are absent.
     * @since %CURRENT_VERSION%
     */
    fun countGradingByTask(contestId: ContestId): Map<TaskId, Long>

    /**
     * Synchronously counts grading submissions made by any of [authorIds] in any of [contestIds] in any grading state,
     * and their distinct authors, without changing stored state or loading file contents. Storage exceptions propagate.
     *
     * @param authorIds the authors to include; an empty set counts nothing.
     * @param contestIds the contests to include; an empty set counts nothing.
     * @return the numbers of matching submissions and of their distinct authors, both zero if nothing matches.
     * @since %CURRENT_VERSION%
     */
    fun countGrading(authorIds: Set<UserId>, contestIds: Set<ContestId>): SubmissionCount
}

/**
 * Persistence port for [Verdict] entities. A verdict is fixed on creation, so `update` is not supported.
 *
 * @since %CURRENT_VERSION%
 */
interface VerdictRepository : EntityRepository<VerdictData, VerdictId, Verdict> {

    /**
     * Synchronously finds current successful grading verdicts whose authors currently hold a student or participant role.
     * Repeated calls reflect current data without changing it or loading file contents; technical exceptions propagate.
     *
     * @param pagination the requested page and ordering.
     * @param filter conjunctive author, submission and current group conditions applied before paging and counting.
     * @return verdicts with lazy references, requested pagination and exact total; unmatched filters or pages are empty.
     * @since %CURRENT_VERSION%
     */
    fun findAvailableToJudge(pagination: Pagination, filter: VerdictFilter = VerdictFilter()): Page<Verdict>
}

/**
 * Persistence port for [Task] entities.
 *
 * @since %CURRENT_VERSION%
 */
interface TaskRepository : EntityRepository<TaskData, TaskId, Task> {

    /**
     * Synchronously finds tasks owned by [ownerId] or shared to any of [communityIds], without changing stored state.
     * Repeated calls reflect current data; storage exceptions propagate to the caller.
     *
     * @param ownerId the developer whose own tasks are included.
     * @param communityIds the communities granting access; an empty set searches only by owner.
     * @param pagination the requested page; id ascending is the default order and breaks ties unless explicitly sorted.
     * @param filter conditions combined with AND before paging and counting, including shared community; unknown ids match nothing.
     * @return distinct authorized tasks, the original pagination and exact filtered total; missing pages are empty.
     * @since %CURRENT_VERSION%
     */
    fun findAvailableToDeveloper(
        ownerId: MultipleRoleUserId,
        communityIds: Set<CommunityId>,
        pagination: Pagination,
        filter: TaskFilter = TaskFilter(),
    ): Page<Task>
}

/**
 * Persistence port for [TaskValidationRequest] entities and their resumable diagnostic progress.
 *
 * @since %CURRENT_VERSION%
 */
interface TaskValidationRequestRepository :
    EntityRepository<TaskValidationRequestData, TaskValidationRequestId, TaskValidationRequest> {

    /**
     * Atomically saves a queued submission for every [TaskValidationSnapshot.authorRuns] entry of the [requestId] snapshot,
     * their ordered links and the created-submissions state, without network IO.
     * Repeated calls return the existing links without creating submissions.
     *
     * @param requestId the request awaiting submissions.
     * @return the request whose links follow the order of the snapshot author runs.
     * @throws IllegalArgumentException if the request does not exist.
     * @throws IllegalStateException if the request neither awaits nor already has submissions.
     * @since %CURRENT_VERSION%
     */
    fun createSubmissions(requestId: TaskValidationRequestId): TaskValidationRequest

    /**
     * Atomically completes author testing of [requestId] with [failures] and the current time.
     * Repeated calls on a completed request return it unchanged.
     *
     * @param requestId the request with created submissions.
     * @param failures every failed submission of the request in submission order, empty if testing succeeded.
     * @return the completed request.
     * @throws IllegalArgumentException if the request does not exist or a failure names a submission of another request.
     * @throws IllegalStateException if the request is in another state.
     * @since %CURRENT_VERSION%
     */
    fun completeTesting(requestId: TaskValidationRequestId, failures: List<AuthorSubmissionFailure>): TaskValidationRequest

    /**
     * Synchronously reads every active request, ordered by identifier, without changing state.
     *
     * @return the requests whose processing may continue.
     * @since %CURRENT_VERSION%
     */
    fun findActive(): List<TaskValidationRequest>

    /**
     * Synchronously finds the request linked to [submissionId], without changing state.
     * Repeated calls reflect current data; ambiguous associations and technical exceptions propagate.
     *
     * @return the linked request, or `null` for an unlinked submission.
     * @throws IllegalStateException if several requests reference the same submission.
     * @since %CURRENT_VERSION%
     */
    fun findBySubmissionId(submissionId: SubmissionId): TaskValidationRequest?

    /**
     * Atomically reads current task inputs and returns an active matching request or persists a new one.
     * Comparison ignores metadata, collection order and diagnostic configuration.
     *
     * @param taskId the task whose working revision inputs are pinned.
     * @param requestedBy the initiator; ownership must be checked by the caller.
     * @return the persisted matching or new request.
     * @throws IllegalArgumentException if the task does not exist.
     * @throws IllegalStateException if the task has no working revision at creation time.
     * @since %CURRENT_VERSION%
     */
    fun findOrCreateActive(taskId: TaskId, requestedBy: MultipleRoleUserId): TaskValidationRequest

    /**
     * Reads saved validation history without changing execution.
     *
     * @param taskId the task whose requests are read.
     * @return existing requests ordered by creation time and identifier.
     * @since %CURRENT_VERSION%
     */
    fun findHistory(taskId: TaskId): List<TaskValidationRequest>

    /**
     * Reads the caller-selected request if diagnostics are unfinished, without changing state or acquiring ownership.
     *
     * @param requestId the request selected exclusively by the application.
     * @return the unfinished request, or `null` if missing or no longer eligible for diagnostics.
     * @since %CURRENT_VERSION%
     */
    fun startDiagnostics(requestId: TaskValidationRequestId): TaskValidationRequest?

    /**
     * Reads completed polygon results, including empty message lists, while the public stage is incomplete.
     *
     * @param requestId the request whose progress is read.
     * @return the saved polygon results.
     * @since %CURRENT_VERSION%
     */
    fun findDiagnosticProgress(requestId: TaskValidationRequestId): List<TestDiagnosticResult>

    /**
     * Atomically saves a polygon result and its messages; the first saved result wins on repeated calls.
     *
     * @param requestId the request with unfinished diagnostics containing the polygon.
     * @param result the completed polygon result.
     * @return the persisted result, including an earlier result if already present.
     * @throws IllegalArgumentException if the request or polygon input does not exist.
     * @throws IllegalStateException if the request is ineligible for diagnostics.
     * @since %CURRENT_VERSION%
     */
    fun saveDiagnosticProgress(requestId: TaskValidationRequestId, result: TestDiagnosticResult): TestDiagnosticResult

    /**
     * Completes diagnostics after all snapshot polygons have saved results; Error terminates the request.
     * Repeated calls on a completed stage return its stored state.
     *
     * @param requestId the request to complete.
     * @return the request whose execution contains completed diagnostics, awaiting submissions or stopped by errors.
     * @throws IllegalStateException if results are missing or execution is ineligible.
     * @since %CURRENT_VERSION%
     */
    fun completeDiagnostics(requestId: TaskValidationRequestId): TaskValidationRequest

    /**
     * Records a terminal technical stop without discarding progress; terminal requests stay terminal.
     *
     * @param requestId the request known by the external exception boundary.
     * @param failure the technical failure details.
     * @return the stopped request retaining the payload of every completed stage in its execution state.
     * @since %CURRENT_VERSION%
     */
    fun recordTechnicalFailure(requestId: TaskValidationRequestId, failure: TaskValidationTechnicalFailure): TaskValidationRequest
}

/**
 * Persistence port for [Test] entities. The file is fixed on creation: `update` with another file
 * throws [UnsupportedOperationException], a new version is saved as a new test in the same version bucket.
 *
 * @since %CURRENT_VERSION%
 */
interface TestRepository : EntityRepository<TestData, TestId, Test> {

    /**
     * Finds the latest version in [versionBucket], ordered by creation time and then by id, both descending.
     * Metadata updates do not change this order.
     *
     * @param versionBucket the resource version chain to search.
     * @return the latest version, or `null` if the chain has no versions.
     * @since %CURRENT_VERSION%
     */
    fun findLatestByVersionBucket(versionBucket: VersionBucket): Test?

    /**
     * Synchronously finds all existing versions in [versionBucket].
     * Repeated calls reflect current metadata and may load files; they do not change stored state.
     *
     * @param versionBucket the resource version chain to search.
     * @return existing versions, or an empty list for a missing chain.
     * @since %CURRENT_VERSION%
     */
    fun findVersionsByVersionBucket(versionBucket: VersionBucket): List<Test>

    /**
     * Synchronously checks whether [versionBucket] contains a version, without reading file contents.
     *
     * @param versionBucket the resource version chain to search.
     * @return whether any version exists in the chain.
     * @since %CURRENT_VERSION%
     */
    fun existsByVersionBucket(versionBucket: VersionBucket): Boolean

    /**
     * Synchronously returns the existing file reference of [id], without reading bytes or changing stored state.
     * Repeated calls return the same reference for an existing version.
     *
     * @param versionBucket the chain the requested version must belong to.
     * @param id the typed version identifier.
     * @return the persisted reference, or `null` if the version is missing or belongs to another chain.
     * @since %CURRENT_VERSION%
     */
    fun findFileRef(versionBucket: VersionBucket, id: TestId): StoredBlobRef?
}
