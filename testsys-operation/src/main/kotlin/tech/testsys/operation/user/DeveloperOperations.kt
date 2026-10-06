package tech.testsys.operation.user

import tech.testsys.domain.builder.api.contestData
import tech.testsys.domain.builder.api.developerSolutionData
import tech.testsys.domain.builder.api.exerciseData
import tech.testsys.domain.builder.api.solutionData
import tech.testsys.domain.builder.api.statementData
import tech.testsys.domain.builder.api.taskData
import tech.testsys.domain.builder.api.testData
import tech.testsys.domain.builder.api.withData
import tech.testsys.domain.contract.StoredBlobRef
import tech.testsys.domain.contract.persistence.repository.CommunityRepository
import tech.testsys.domain.contract.persistence.repository.ContestRepository
import tech.testsys.domain.contract.persistence.repository.DeveloperSolutionRepository
import tech.testsys.domain.contract.persistence.repository.ExerciseRepository
import tech.testsys.domain.contract.persistence.repository.SolutionRepository
import tech.testsys.domain.contract.persistence.repository.StatementRepository
import tech.testsys.domain.contract.persistence.repository.TaskRepository
import tech.testsys.domain.contract.persistence.repository.TaskValidationRequestRepository
import tech.testsys.domain.contract.persistence.repository.TestRepository
import tech.testsys.domain.model.DomainEntity
import tech.testsys.domain.model.DomainId
import tech.testsys.domain.model.group.CommunityId
import tech.testsys.domain.model.task.Contest
import tech.testsys.domain.model.task.ContestId
import tech.testsys.domain.model.task.DeveloperSolution
import tech.testsys.domain.model.task.DeveloperSolutionId
import tech.testsys.domain.model.task.Exercise
import tech.testsys.domain.model.task.ExerciseId
import tech.testsys.domain.model.task.FileData
import tech.testsys.domain.model.task.Score
import tech.testsys.domain.model.task.Statement
import tech.testsys.domain.model.task.StatementId
import tech.testsys.domain.model.task.Task
import tech.testsys.domain.model.task.TaskContent
import tech.testsys.domain.model.task.TaskId
import tech.testsys.domain.model.task.TaskValidationRequest
import tech.testsys.domain.model.task.Test
import tech.testsys.domain.model.task.TestId
import tech.testsys.domain.model.task.TrikStudioVersion
import tech.testsys.domain.model.task.TrikSupportedLanguage
import tech.testsys.domain.model.task.VersionBucket
import tech.testsys.domain.model.user.Developer
import tech.testsys.domain.model.user.MultipleRoleUser
import tech.testsys.operation.annotation.Feature
import tech.testsys.operation.annotation.InternalOperationsApi
import tech.testsys.operation.error.AddDeveloperSolutionError
import tech.testsys.operation.error.AddExerciseError
import tech.testsys.operation.error.AddStatementError
import tech.testsys.operation.error.AddTestError
import tech.testsys.operation.error.AttachDeveloperSolutionError
import tech.testsys.operation.error.AttachExerciseError
import tech.testsys.operation.error.AttachStatementError
import tech.testsys.operation.error.AttachTaskError
import tech.testsys.operation.error.AttachTestError
import tech.testsys.operation.error.AttemptDurationExceedsContestDurationError
import tech.testsys.operation.error.CommunityAccessDeniedError
import tech.testsys.operation.error.CommunityNotExistsError
import tech.testsys.operation.error.ContestAccessDeniedError
import tech.testsys.operation.error.ContestAlreadySharedError
import tech.testsys.operation.error.ContestEndNotAfterStartError
import tech.testsys.operation.error.ContestEndWithoutStartError
import tech.testsys.operation.error.ContestNotExistsError
import tech.testsys.operation.error.CreateContestError
import tech.testsys.operation.error.CreateTaskError
import tech.testsys.operation.error.DetachDeveloperSolutionError
import tech.testsys.operation.error.DetachExerciseError
import tech.testsys.operation.error.DetachStatementError
import tech.testsys.operation.error.DetachTestError
import tech.testsys.operation.error.DeveloperSolutionNotExistsError
import tech.testsys.operation.error.DeveloperSolutionNotUploadedToTaskError
import tech.testsys.operation.error.DeveloperSolutionVersionNotLatestError
import tech.testsys.operation.error.DownloadResourceVersionError
import tech.testsys.operation.error.EditContestError
import tech.testsys.operation.error.EditTaskInfoError
import tech.testsys.operation.error.ExerciseLanguageAlreadyAttachedError
import tech.testsys.operation.error.ExerciseNotExistsError
import tech.testsys.operation.error.ExerciseNotUploadedToTaskError
import tech.testsys.operation.error.ExerciseVersionNotLatestError
import tech.testsys.operation.error.MissedDeveloperRoleError
import tech.testsys.operation.error.NonPositiveAttemptDurationError
import tech.testsys.operation.error.OperationResult
import tech.testsys.operation.error.ResourceAlreadyAttachedError
import tech.testsys.operation.error.ResourceNotExistsError
import tech.testsys.operation.error.ResourceNotUploadedToTaskError
import tech.testsys.operation.error.ResourceVersionNotAttachedError
import tech.testsys.operation.error.ResourceVersionNotExistsError
import tech.testsys.operation.error.RevertTaskError
import tech.testsys.operation.error.RunDiagnosticsError
import tech.testsys.operation.error.ShareContestError
import tech.testsys.operation.error.ShareTaskError
import tech.testsys.operation.error.StatementNotExistsError
import tech.testsys.operation.error.StatementNotUploadedToTaskError
import tech.testsys.operation.error.StatementVersionNotLatestError
import tech.testsys.operation.error.TaskAccessDeniedError
import tech.testsys.operation.error.TaskAlreadyAttachedToContestError
import tech.testsys.operation.error.TaskAlreadyCommittedError
import tech.testsys.operation.error.TaskAlreadyHasStatementError
import tech.testsys.operation.error.TaskNotCommittedError
import tech.testsys.operation.error.TaskNotExistsError
import tech.testsys.operation.error.TaskTrikStudioVersionNotSupportedError
import tech.testsys.operation.error.TestNotExistsError
import tech.testsys.operation.error.TestNotUploadedToTaskError
import tech.testsys.operation.error.TestVersionNotLatestError
import tech.testsys.operation.error.UpdateDeveloperSolutionError
import tech.testsys.operation.error.UpdateExerciseError
import tech.testsys.operation.error.UpdateStatementError
import tech.testsys.operation.error.UpdateTestError
import tech.testsys.operation.error.ViewContestError
import tech.testsys.operation.error.ViewContestsError
import tech.testsys.operation.error.ViewResourceError
import tech.testsys.operation.error.ViewResourcesError
import tech.testsys.operation.error.ViewTaskError
import tech.testsys.operation.error.ViewTasksError
import tech.testsys.operation.error.asSuccess
import tech.testsys.operation.error.ensure
import tech.testsys.operation.error.operation
import tech.testsys.operation.util.changeEditableContent
import tech.testsys.operation.util.getEditableContent
import tech.testsys.operation.util.hasRole
import java.time.Duration
import java.time.Instant
import java.util.UUID

/**
 * Operations of a user with the [Developer] role.
 *
 * @since %CURRENT_VERSION%
 */
@OptIn(InternalOperationsApi::class)
class DeveloperOperations(
    private val taskRepository: TaskRepository,
    private val statementRepository: StatementRepository,
    private val communityRepository: CommunityRepository,
    private val exerciseRepository: ExerciseRepository,
    private val testRepository: TestRepository,
    private val developerSolutionRepository: DeveloperSolutionRepository,
    private val solutionRepository: SolutionRepository,
    private val contestRepository: ContestRepository,
    private val taskValidationRequestRepository: TaskValidationRequestRepository,
) {

    /**
     * Persists or returns an active validation request for the working revision of [taskId] owned by [user].
     * Technical storage exceptions propagate to the external caller.
     *
     * @since %CURRENT_VERSION%
     */
    @Feature("testsys.user.multi.developer.task.runDiagnostics")
    fun runDiagnostics(user: MultipleRoleUser, taskId: TaskId): OperationResult<TaskValidationRequest, RunDiagnosticsError> =
        operation<TaskValidationRequest, RunDiagnosticsError> {
            ensure(user.hasRole<Developer>(), MissedDeveloperRoleError)
            val task = taskRepository.findById(taskId)
            ensure(task != null) { TaskNotExistsError(taskId) }
            ensure(task.data.owner.id == user.id) { TaskAccessDeniedError(taskId) }
            ensure(task.data.content !is TaskContent.Committed) { TaskAlreadyCommittedError(taskId) }
            return taskValidationRequestRepository.findOrCreateActive(taskId = taskId, requestedBy = user.id).asSuccess()
        }

    /**
     * Returns contests owned by [user] or shared to communities of their [Developer] role, without changing contest state.
     * Missing developer role is an expected failure; storage exceptions propagate to the caller.
     *
     * @since %CURRENT_VERSION%
     */
    @Feature("testsys.user.multi.developer.contest.viewContests")
    fun viewContests(user: MultipleRoleUser): OperationResult<List<Contest>, ViewContestsError> =
        operation<List<Contest>, ViewContestsError> {
            ensure(user.hasRole<Developer>(), MissedDeveloperRoleError)
            val developer = user.data.roles.filterIsInstance<Developer>().single()
            val contests = contestRepository.findAvailableToDeveloper(ownerId = user.id, communityIds = developer.memberOf.ids.toSet())
            return contests.asSuccess()
        }

    /**
     * Returns [contestId] owned by [user] or shared to communities of their [Developer] role, preserving its data.
     * Missing role, contest and access are expected failures; storage exceptions propagate to the caller.
     *
     * @since %CURRENT_VERSION%
     */
    @Feature("testsys.user.multi.developer.contest.viewContest")
    fun viewContest(user: MultipleRoleUser, contestId: ContestId): OperationResult<Contest, ViewContestError> =
        operation<Contest, ViewContestError> {
            ensure(user.hasRole<Developer>(), MissedDeveloperRoleError)
            val contest = contestRepository.findById(contestId)
            ensure(contest != null) { ContestNotExistsError(contestId) }
            val developer = user.data.roles.filterIsInstance<Developer>().single()
            ensure(contest.data.owner.id == user.id || contest.data.sharedTo.ids.any { it in developer.memberOf.ids }) {
                ContestAccessDeniedError(contestId)
            }
            return contest.asSuccess()
        }

    /**
     * Creates a contest owned by [user] with [contestName], [contestDescription] and [trikStudioVersion].
     * Omitted [attemptDuration], [startsAt] and [endsAt] leave their limits unset; storage exceptions propagate.
     *
     * @throws IllegalArgumentException if a duration cannot be represented exactly in Long milliseconds.
     * @since %CURRENT_VERSION%
     */
    @Feature("testsys.user.multi.developer.contest.createContest")
    fun createContest(
        user: MultipleRoleUser,
        contestName: String,
        trikStudioVersion: TrikStudioVersion,
        attemptDuration: Duration? = null,
        startsAt: Instant? = null,
        endsAt: Instant? = null,
        contestDescription: String = "",
    ): OperationResult<Contest, CreateContestError> = operation<Contest, CreateContestError> {
        ensure(user.hasRole<Developer>(), MissedDeveloperRoleError)
        val contestDuration = endsAt?.let { end ->
            ensure(startsAt != null) { ContestEndWithoutStartError(end) }
            ensure(end > startsAt) { ContestEndNotAfterStartError(startsAt = startsAt, endsAt = end) }
            Duration.between(startsAt, end)
        }
        if (attemptDuration != null) {
            ensure(attemptDuration > Duration.ZERO) { NonPositiveAttemptDurationError(attemptDuration) }
            ensure(contestDuration == null || attemptDuration <= contestDuration) {
                AttemptDurationExceedsContestDurationError(
                    attemptDuration = attemptDuration,
                    contestDuration = requireNotNull(contestDuration),
                )
            }
        }
        contestDuration?.requireExactMillis("contestDuration")
        attemptDuration?.requireExactMillis("attemptDuration")
        val data = contestData {
            owner = user.id
            name = contestName
            description = contestDescription
            this.trikStudioVersion = trikStudioVersion
            this.attemptDuration = attemptDuration
            this.startsAt = startsAt
            this.contestDuration = contestDuration
        }
        return contestRepository.save(data).asSuccess()
    }

    /**
     * Replaces the name and dates of an unshared [contestId] owned by [user]; null dates clear the corresponding limits.
     * The resulting interval must fit exactly in Long milliseconds; unchanged values return the contest without saving.
     *
     * @throws IllegalArgumentException if the resulting interval cannot be represented exactly in Long milliseconds.
     * @since %CURRENT_VERSION%
     */
    @Feature("testsys.user.multi.developer.contest.editContest")
    fun editContest(
        user: MultipleRoleUser,
        contestId: ContestId,
        contestName: String,
        startsAt: Instant?,
        endsAt: Instant?,
    ): OperationResult<Contest, EditContestError> = operation<Contest, EditContestError> {
        ensure(user.hasRole<Developer>(), MissedDeveloperRoleError)
        val contest = contestRepository.findById(contestId)
        ensure(contest != null) { ContestNotExistsError(contestId) }
        ensure(contest.data.owner.id == user.id) { ContestAccessDeniedError(contestId) }
        ensure(contest.data.sharedTo.ids.isEmpty()) { ContestAlreadySharedError(contestId) }

        val contestDuration = endsAt?.let { end ->
            ensure(startsAt != null) { ContestEndWithoutStartError(end) }
            ensure(end > startsAt) { ContestEndNotAfterStartError(startsAt = startsAt, endsAt = end) }
            Duration.between(startsAt, end)
        }
        val attemptDuration = contest.data.attemptDuration
        ensure(attemptDuration == null || contestDuration == null || attemptDuration <= contestDuration) {
            AttemptDurationExceedsContestDurationError(
                attemptDuration = requireNotNull(attemptDuration),
                contestDuration = requireNotNull(contestDuration),
            )
        }
        contestDuration?.requireExactMillis("contestDuration")
        if (contestName == contest.data.name && startsAt == contest.data.startsAt && endsAt == contest.data.endsAt) {
            return contest.asSuccess()
        }

        val updatedContest = contest.withData {
            name = contestName
            this.startsAt = startsAt
            this.contestDuration = contestDuration
        }
        return contestRepository.update(updatedContest).asSuccess()
    }

    /**
     * Adds [communityIds] to the recipients of [contestId] owned by [user]; new recipients require Developer membership.
     * Requests without new recipients return the contest without saving; storage exceptions propagate to the caller.
     *
     * @since %CURRENT_VERSION%
     */
    @Feature("testsys.user.multi.developer.contest.shareContest")
    fun shareContest(
        user: MultipleRoleUser,
        contestId: ContestId,
        communityIds: Set<CommunityId>,
    ): OperationResult<Contest, ShareContestError> = operation<Contest, ShareContestError> {
        ensure(user.hasRole<Developer>(), MissedDeveloperRoleError)
        val contest = contestRepository.findById(contestId)
        ensure(contest != null) { ContestNotExistsError(contestId) }
        communityIds.forEach { communityId ->
            ensure(communityRepository.findById(communityId) != null) { CommunityNotExistsError(communityId) }
        }
        ensure(contest.data.owner.id == user.id) { ContestAccessDeniedError(contestId) }

        val sharedCommunityIds = contest.data.sharedTo.ids
        val newCommunityIds = communityIds.filterNot { communityId -> communityId in sharedCommunityIds }
        val developerCommunityIds = user.data.roles.filterIsInstance<Developer>().single().memberOf.ids
        newCommunityIds.forEach { communityId ->
            ensure(communityId in developerCommunityIds) { CommunityAccessDeniedError(communityId) }
        }
        if (newCommunityIds.isEmpty()) {
            return contest.asSuccess()
        }

        val sharedContest = contest.withData {
            sharedTo = (sharedCommunityIds + communityIds).distinct().toMutableList()
        }
        return contestRepository.update(sharedContest).asSuccess()
    }

    /**
     * Attaches [taskId] owned by [user] or shared to their Developer communities to an unshared [contestId] they own,
     * requiring a committed revision that supports the contest's TRIK Studio version and rejecting repeated attachment.
     * Only the contest's tasks change; the last committed revision is checked, and storage exceptions propagate.
     *
     * @since %CURRENT_VERSION%
     */
    @Feature("testsys.user.multi.developer.contest.attachTask")
    fun attachTask(user: MultipleRoleUser, contestId: ContestId, taskId: TaskId): OperationResult<Contest, AttachTaskError> =
        operation<Contest, AttachTaskError> {
            ensure(user.hasRole<Developer>(), MissedDeveloperRoleError)
            val contest = contestRepository.findById(contestId)
            ensure(contest != null) { ContestNotExistsError(contestId) }
            val task = taskRepository.findById(taskId)
            ensure(task != null) { TaskNotExistsError(taskId) }
            ensure(contest.data.owner.id == user.id) { ContestAccessDeniedError(contestId) }
            val developerCommunityIds = user.data.roles.filterIsInstance<Developer>().single().memberOf.ids
            ensure(task.data.owner.id == user.id || task.data.sharedTo.ids.any { communityId -> communityId in developerCommunityIds }) {
                TaskAccessDeniedError(taskId)
            }
            ensure(contest.data.sharedTo.ids.isEmpty()) { ContestAlreadySharedError(contestId) }

            val lastCommitted = when (val content = task.data.content) {
                is TaskContent.New -> null
                is TaskContent.Uncommitted -> content.lastCommitted
                is TaskContent.Committed -> content.lastCommitted
            }
            ensure(lastCommitted != null) { TaskNotCommittedError(taskId) }
            ensure(contest.data.trikStudioVersion in lastCommitted.supportedTrikStudioVersions) {
                TaskTrikStudioVersionNotSupportedError(taskId, contest.data.trikStudioVersion)
            }
            ensure(taskId !in contest.data.tasks.ids) { TaskAlreadyAttachedToContestError(contestId, taskId) }

            val updatedContest = contest.withData { tasks.add(taskId) }
            return contestRepository.update(updatedContest).asSuccess()
        }

    /**
     * Returns tasks owned by [user] or shared to communities of their [Developer] role, without changing task state.
     * Missing developer role is an expected failure; storage exceptions propagate to the caller.
     *
     * @since %CURRENT_VERSION%
     */
    @Feature("testsys.user.multi.developer.task.viewTasks")
    fun viewTasks(user: MultipleRoleUser): OperationResult<List<Task>, ViewTasksError> = operation<List<Task>, ViewTasksError> {
        ensure(user.hasRole<Developer>(), MissedDeveloperRoleError)
        val developer = user.data.roles.filterIsInstance<Developer>().single()
        val tasks = taskRepository.findAvailableToDeveloper(ownerId = user.id, communityIds = developer.memberOf.ids.toSet())
        return tasks.asSuccess()
    }

    /**
     * Returns the task with [taskId] owned by [user], preserving its state and both content revisions.
     * Missing role, task and ownership are expected failures; storage exceptions propagate to the caller.
     *
     * @since %CURRENT_VERSION%
     */
    @Feature("testsys.user.multi.developer.task.viewTask")
    fun viewTask(user: MultipleRoleUser, taskId: TaskId): OperationResult<Task, ViewTaskError> = operation<Task, ViewTaskError> {
        ensure(user.hasRole<Developer>(), MissedDeveloperRoleError)
        val task = taskRepository.findById(taskId)
        ensure(task != null) { TaskNotExistsError(taskId) }
        ensure(task.data.owner.id == user.id) { TaskAccessDeniedError(taskId) }
        return task.asSuccess()
    }

    /**
     * Edits [taskName], [taskDescription] and [supportedTrikStudioVersions] of [taskId] owned by [user], retaining omitted values.
     * Version set changes affect only the editable revision; unchanged values return the task without saving.
     *
     * @since %CURRENT_VERSION%
     */
    @Feature("testsys.user.multi.developer.task.editTaskInfo")
    fun editTaskInfo(
        user: MultipleRoleUser,
        taskId: TaskId,
        taskName: String? = null,
        taskDescription: String? = null,
        supportedTrikStudioVersions: List<TrikStudioVersion>? = null,
    ): OperationResult<Task, EditTaskInfoError> = operation<Task, EditTaskInfoError> {
        ensure(user.hasRole<Developer>(), MissedDeveloperRoleError)
        val task = taskRepository.findById(taskId)
        ensure(task != null) { TaskNotExistsError(taskId) }
        ensure(task.data.owner.id == user.id) { TaskAccessDeniedError(taskId) }

        val versions = supportedTrikStudioVersions?.distinct()
        val hasVersionChanges = versions != null && versions.toSet() != task.getEditableContent().supportedTrikStudioVersions.toSet()
        val name = taskName ?: task.data.name
        val description = taskDescription ?: task.data.description
        if (!hasVersionChanges && name == task.data.name && description == task.data.description) {
            return task.asSuccess()
        }

        val editedTask = if (hasVersionChanges) {
            task.changeEditableContent { this.supportedTrikStudioVersions = versions.toMutableList() }
        } else {
            task
        }
        val updatedTask = editedTask.withData {
            this.name = name
            this.description = description
        }
        return taskRepository.update(updatedTask).asSuccess()
    }

    /**
     * Returns the latest existing resource versions uploaded to tasks owned by [user], including unattached chains.
     * Each entity's creation time is the last-change time of its resource; viewing does not change task state.
     *
     * @since %CURRENT_VERSION%
     */
    @Feature("testsys.user.multi.developer.resource.viewResources")
    fun viewResources(user: MultipleRoleUser): OperationResult<List<DomainEntity<*>>, ViewResourcesError> =
        operation<List<DomainEntity<*>>, ViewResourcesError> {
            ensure(user.hasRole<Developer>(), MissedDeveloperRoleError)
            val developer = user.data.roles.filterIsInstance<Developer>().single()
            val buckets = taskRepository.findByIds(developer.data.tasks.ids)
                .filter { it.data.owner.id == user.id }
                .flatMap { it.data.uploadedResources }.toSet()
            val resources: List<DomainEntity<*>> = buckets.flatMap { bucket ->
                listOfNotNull(
                    statementRepository.findLatestByVersionBucket(bucket),
                    exerciseRepository.findLatestByVersionBucket(bucket),
                    testRepository.findLatestByVersionBucket(bucket),
                    developerSolutionRepository.findLatestByVersionBucket(bucket),
                )
            }
            return resources.asSuccess()
        }

    /**
     * Returns existing versions of [versionBucket] uploaded to [taskId] owned by [user].
     * Entities retain their current metadata and files; viewing does not change task state.
     *
     * @since %CURRENT_VERSION%
     */
    @Feature("testsys.user.multi.developer.resource.viewResource")
    fun viewResource(
        user: MultipleRoleUser,
        taskId: TaskId,
        versionBucket: VersionBucket,
    ): OperationResult<List<DomainEntity<*>>, ViewResourceError> = operation<List<DomainEntity<*>>, ViewResourceError> {
        ensure(user.hasRole<Developer>(), MissedDeveloperRoleError)
        val task = taskRepository.findById(taskId)
        ensure(task != null) { TaskNotExistsError(taskId) }
        ensure(resourceExists(versionBucket)) { ResourceNotExistsError(versionBucket) }
        ensure(task.data.owner.id == user.id) { TaskAccessDeniedError(taskId) }
        ensure(versionBucket in task.data.uploadedResources) { ResourceNotUploadedToTaskError(taskId, versionBucket) }
        val versions = statementRepository.findVersionsByVersionBucket(versionBucket) +
            exerciseRepository.findVersionsByVersionBucket(versionBucket) +
            testRepository.findVersionsByVersionBucket(versionBucket) +
            developerSolutionRepository.findVersionsByVersionBucket(versionBucket)
        ensure(versions.isNotEmpty()) { ResourceNotExistsError(versionBucket) }
        return versions.asSuccess()
    }

    /**
     * Returns the existing file reference of [versionId] in [versionBucket] uploaded to [taskId] owned by [user].
     * The operation accepts older versions and does not load file contents or change the task.
     *
     * @since %CURRENT_VERSION%
     */
    @Feature("testsys.user.multi.developer.resource.viewResource")
    fun downloadResourceVersion(
        user: MultipleRoleUser,
        taskId: TaskId,
        versionBucket: VersionBucket,
        versionId: DomainId,
    ): OperationResult<StoredBlobRef, DownloadResourceVersionError> = operation<StoredBlobRef, DownloadResourceVersionError> {
        ensure(user.hasRole<Developer>(), MissedDeveloperRoleError)
        val task = taskRepository.findById(taskId)
        ensure(task != null) { TaskNotExistsError(taskId) }
        ensure(resourceExists(versionBucket)) { ResourceNotExistsError(versionBucket) }
        ensure(task.data.owner.id == user.id) { TaskAccessDeniedError(taskId) }
        ensure(versionBucket in task.data.uploadedResources) { ResourceNotUploadedToTaskError(taskId, versionBucket) }
        val fileRef = when (versionId) {
            is StatementId -> statementRepository.findFileRef(versionBucket, versionId)
            is ExerciseId -> exerciseRepository.findFileRef(versionBucket, versionId)
            is TestId -> testRepository.findFileRef(versionBucket, versionId)
            is DeveloperSolutionId -> developerSolutionRepository.findFileRef(versionBucket, versionId)
            else -> null
        }
        ensure(fileRef != null) { ResourceVersionNotExistsError(versionBucket, versionId) }
        return fileRef.asSuccess()
    }

    /**
     * Creates a new task owned by [user] with [taskName] and [taskDescription]. The created task has
     * [TaskContent.New] content and no uploaded resources.
     *
     * @since %CURRENT_VERSION%
     */
    @Feature("testsys.user.multi.developer.task.createTask")
    fun createTask(user: MultipleRoleUser, taskName: String, taskDescription: String): OperationResult<Task, CreateTaskError> =
        operation<Task, CreateTaskError> {
            ensure(user.hasRole<Developer>(), MissedDeveloperRoleError)

            val taskData = taskData {
                owner = user.id
                name = taskName
                description = taskDescription
                content.new {}
            }

            val task = taskRepository.save(taskData)
            return task.asSuccess()
        }

    /**
     * Uploads [file] as a new statement named [resourceName] to [taskId] owned by [user].
     * Registers a new resource chain without attaching it or changing task revisions.
     *
     * @since %CURRENT_VERSION%
     */
    @Feature("testsys.user.multi.developer.resource.addResource")
    fun addStatement(
        user: MultipleRoleUser,
        taskId: TaskId,
        resourceName: String,
        file: FileData,
    ): OperationResult<Statement, AddStatementError> = operation<Statement, AddStatementError> {
        ensure(user.hasRole<Developer>(), MissedDeveloperRoleError)
        val task = taskRepository.findById(taskId)
        ensure(task != null) { TaskNotExistsError(taskId) }
        ensure(task.data.owner.id == user.id) { TaskAccessDeniedError(taskId) }

        val resource = statementRepository.save(
            statementData {
                name = resourceName
                description = ""
                versionBucket = VersionBucket(UUID.randomUUID())
                file(file.uploadedFilename, file.content)
            },
        )
        taskRepository.update(task.withData { uploadedResources.add(resource.data.versionBucket) })
        return resource.asSuccess()
    }

    /**
     * Uploads [file] as a new polygon named [resourceName] to [taskId] owned by [user].
     * Registers a new resource chain without attaching it or changing task revisions.
     *
     * @since %CURRENT_VERSION%
     */
    @Feature("testsys.user.multi.developer.resource.addResource")
    fun addTest(user: MultipleRoleUser, taskId: TaskId, resourceName: String, file: FileData): OperationResult<Test, AddTestError> =
        operation<Test, AddTestError> {
            ensure(user.hasRole<Developer>(), MissedDeveloperRoleError)
            val task = taskRepository.findById(taskId)
            ensure(task != null) { TaskNotExistsError(taskId) }
            ensure(task.data.owner.id == user.id) { TaskAccessDeniedError(taskId) }

            val resource = testRepository.save(
                testData {
                    name = resourceName
                    description = ""
                    versionBucket = VersionBucket(UUID.randomUUID())
                    file(file.uploadedFilename, file.content)
                },
            )
            taskRepository.update(task.withData { uploadedResources.add(resource.data.versionBucket) })
            return resource.asSuccess()
        }

    /**
     * Uploads [file] as a new exercise named [resourceName] to [taskId] owned by [user] using [language].
     * Registers a new resource chain without attaching it or changing task revisions.
     *
     * @since %CURRENT_VERSION%
     */
    @Feature("testsys.user.multi.developer.resource.addResource")
    fun addExercise(
        user: MultipleRoleUser,
        taskId: TaskId,
        resourceName: String,
        file: FileData,
        language: TrikSupportedLanguage,
    ): OperationResult<Exercise, AddExerciseError> = operation<Exercise, AddExerciseError> {
        ensure(user.hasRole<Developer>(), MissedDeveloperRoleError)
        val task = taskRepository.findById(taskId)
        ensure(task != null) { TaskNotExistsError(taskId) }
        ensure(task.data.owner.id == user.id) { TaskAccessDeniedError(taskId) }

        val resource = exerciseRepository.save(
            exerciseData {
                name = resourceName
                description = ""
                versionBucket = VersionBucket(UUID.randomUUID())
                file(file.uploadedFilename, file.content)
                when (language) {
                    TrikSupportedLanguage.Python -> this.language.python()
                    TrikSupportedLanguage.JavaScript -> this.language.javaScript()
                    TrikSupportedLanguage.VisualLanguage -> this.language.visualLanguage()
                }
            },
        )
        taskRepository.update(task.withData { uploadedResources.add(resource.data.versionBucket) })
        return resource.asSuccess()
    }

    /**
     * Uploads [file] as a new developer solution named [resourceName] to [taskId] owned by [user],
     * using [language] and [expectedScore]; registers a new chain without changing task revisions.
     * The uploaded resource is not attached to task content.
     *
     * @since %CURRENT_VERSION%
     */
    @Feature("testsys.user.multi.developer.resource.addResource")
    fun addDeveloperSolution(
        user: MultipleRoleUser,
        taskId: TaskId,
        resourceName: String,
        file: FileData,
        language: TrikSupportedLanguage,
        expectedScore: Score,
    ): OperationResult<DeveloperSolution, AddDeveloperSolutionError> = operation<DeveloperSolution, AddDeveloperSolutionError> {
        ensure(user.hasRole<Developer>(), MissedDeveloperRoleError)
        val task = taskRepository.findById(taskId)
        ensure(task != null) { TaskNotExistsError(taskId) }
        ensure(task.data.owner.id == user.id) { TaskAccessDeniedError(taskId) }

        val solution = solutionRepository.save(
            solutionData {
                file(file.uploadedFilename, file.content)
                when (language) {
                    TrikSupportedLanguage.Python -> this.language.python()
                    TrikSupportedLanguage.JavaScript -> this.language.javaScript()
                    TrikSupportedLanguage.VisualLanguage -> this.language.visualLanguage()
                }
            },
        )

        val resource = developerSolutionRepository.save(
            developerSolutionData {
                name = resourceName
                description = ""
                versionBucket = VersionBucket(UUID.randomUUID())
                this.solution = solution.id
                this.expectedScore = expectedScore
            },
        )
        taskRepository.update(task.withData { uploadedResources.add(resource.data.versionBucket) })
        return resource.asSuccess()
    }

    /**
     * Updates the latest [statementId] uploaded to [taskId] owned by [user], preserving omitted [resourceName].
     * A supplied [file] creates a new version without comparison and replaces only an existing editable chain link.
     *
     * @since %CURRENT_VERSION%
     */
    @Feature("testsys.user.multi.developer.resource.updateResource")
    fun updateStatement(
        user: MultipleRoleUser,
        taskId: TaskId,
        statementId: StatementId,
        resourceName: String? = null,
        file: FileData? = null,
    ): OperationResult<Statement, UpdateStatementError> = operation<Statement, UpdateStatementError> {
        ensure(user.hasRole<Developer>(), MissedDeveloperRoleError)
        val task = taskRepository.findById(taskId)
        ensure(task != null) { TaskNotExistsError(taskId) }
        val resource = statementRepository.findById(statementId)
        ensure(resource != null) { StatementNotExistsError(statementId) }
        ensure(task.data.owner.id == user.id) { TaskAccessDeniedError(taskId) }
        ensure(resource.data.versionBucket in task.data.uploadedResources) {
            StatementNotUploadedToTaskError(taskId, statementId)
        }
        ensure(statementRepository.findLatestByVersionBucket(resource.data.versionBucket)?.id == statementId) {
            StatementVersionNotLatestError(statementId)
        }

        if (file == null) {
            return statementRepository.update(resource.withData { name = resourceName ?: resource.data.name }).asSuccess()
        }

        val editable = task.getEditableContent()
        val attached = editable.statement?.let { reference -> statementRepository.load(reference) }
        val isChainAttached = attached?.data?.versionBucket == resource.data.versionBucket

        val updated = resource.withData {
            name = resourceName ?: resource.data.name
            file(file.uploadedFilename, file.content)
        }
        val saved = statementRepository.save(updated.data)
        if (isChainAttached) {
            taskRepository.update(
                task.changeEditableContent {
                    statement = saved.id
                },
            )
        }
        return saved.asSuccess()
    }

    /**
     * Updates the latest [exerciseId] uploaded to [taskId] owned by [user], preserving omitted [resourceName].
     * A supplied [file] creates a new version without comparison and replaces only an existing editable chain link.
     *
     * @since %CURRENT_VERSION%
     */
    @Feature("testsys.user.multi.developer.resource.updateResource")
    fun updateExercise(
        user: MultipleRoleUser,
        taskId: TaskId,
        exerciseId: ExerciseId,
        resourceName: String? = null,
        file: FileData? = null,
    ): OperationResult<Exercise, UpdateExerciseError> = operation<Exercise, UpdateExerciseError> {
        ensure(user.hasRole<Developer>(), MissedDeveloperRoleError)
        val task = taskRepository.findById(taskId)
        ensure(task != null) { TaskNotExistsError(taskId) }
        val resource = exerciseRepository.findById(exerciseId)
        ensure(resource != null) { ExerciseNotExistsError(exerciseId) }
        ensure(task.data.owner.id == user.id) { TaskAccessDeniedError(taskId) }
        ensure(resource.data.versionBucket in task.data.uploadedResources) {
            ExerciseNotUploadedToTaskError(taskId, exerciseId)
        }
        ensure(exerciseRepository.findLatestByVersionBucket(resource.data.versionBucket)?.id == exerciseId) {
            ExerciseVersionNotLatestError(exerciseId)
        }

        if (file == null) {
            return exerciseRepository.update(resource.withData { name = resourceName ?: resource.data.name }).asSuccess()
        }

        val editable = task.getEditableContent()
        val attached = exerciseRepository.load(editable.exercises)
        val replacedIds = attached.filter { it.data.versionBucket == resource.data.versionBucket }.map { it.id }.toSet()

        val updated = resource.withData {
            name = resourceName ?: resource.data.name
            file(file.uploadedFilename, file.content)
        }
        val saved = exerciseRepository.save(updated.data)
        if (replacedIds.isNotEmpty()) {
            taskRepository.update(
                task.changeEditableContent {
                    exercises = editable.exercises.ids.map { attachedId ->
                        if (attachedId in replacedIds) saved.id else attachedId
                    }.toMutableList()
                },
            )
        }
        return saved.asSuccess()
    }

    /**
     * Updates the latest [testId] uploaded to [taskId] owned by [user], preserving omitted [resourceName].
     * A supplied [file] creates a new version without comparison and replaces only an existing editable chain link.
     *
     * @since %CURRENT_VERSION%
     */
    @Feature("testsys.user.multi.developer.resource.updateResource")
    fun updateTest(
        user: MultipleRoleUser,
        taskId: TaskId,
        testId: TestId,
        resourceName: String? = null,
        file: FileData? = null,
    ): OperationResult<Test, UpdateTestError> = operation<Test, UpdateTestError> {
        ensure(user.hasRole<Developer>(), MissedDeveloperRoleError)
        val task = taskRepository.findById(taskId)
        ensure(task != null) { TaskNotExistsError(taskId) }
        val resource = testRepository.findById(testId)
        ensure(resource != null) { TestNotExistsError(testId) }
        ensure(task.data.owner.id == user.id) { TaskAccessDeniedError(taskId) }
        ensure(resource.data.versionBucket in task.data.uploadedResources) {
            TestNotUploadedToTaskError(taskId, testId)
        }
        ensure(testRepository.findLatestByVersionBucket(resource.data.versionBucket)?.id == testId) {
            TestVersionNotLatestError(testId)
        }

        if (file == null) {
            return testRepository.update(resource.withData { name = resourceName ?: resource.data.name }).asSuccess()
        }

        val editable = task.getEditableContent()
        val replacedIds = testRepository.load(editable.tests)
            .filter { attached -> attached.data.versionBucket == resource.data.versionBucket }
            .map { attached -> attached.id }
            .toSet()

        val updated = resource.withData {
            name = resourceName ?: resource.data.name
            file(file.uploadedFilename, file.content)
        }
        val saved = testRepository.save(updated.data)
        if (replacedIds.isNotEmpty()) {
            taskRepository.update(
                task.changeEditableContent {
                    tests = editable.tests.ids.map { attachedId ->
                        if (attachedId in replacedIds) saved.id else attachedId
                    }.toMutableList()
                },
            )
        }
        return saved.asSuccess()
    }

    /**
     * Updates the latest [developerSolutionId] uploaded to [taskId] owned by [user], preserving omitted [resourceName].
     * Supplied [file] or [expectedScore] creates a new version without comparison; replaces only an existing editable chain link.
     *
     * @since %CURRENT_VERSION%
     */
    @Feature("testsys.user.multi.developer.resource.updateResource")
    fun updateDeveloperSolution(
        user: MultipleRoleUser,
        taskId: TaskId,
        developerSolutionId: DeveloperSolutionId,
        resourceName: String? = null,
        file: FileData? = null,
        expectedScore: Score? = null,
    ): OperationResult<DeveloperSolution, UpdateDeveloperSolutionError> = operation<DeveloperSolution, UpdateDeveloperSolutionError> {
        ensure(user.hasRole<Developer>(), MissedDeveloperRoleError)
        val task = taskRepository.findById(taskId)
        ensure(task != null) { TaskNotExistsError(taskId) }
        val resource = developerSolutionRepository.findById(developerSolutionId)
        ensure(resource != null) { DeveloperSolutionNotExistsError(developerSolutionId) }
        ensure(task.data.owner.id == user.id) { TaskAccessDeniedError(taskId) }
        ensure(resource.data.versionBucket in task.data.uploadedResources) {
            DeveloperSolutionNotUploadedToTaskError(taskId, developerSolutionId)
        }
        ensure(developerSolutionRepository.findLatestByVersionBucket(resource.data.versionBucket)?.id == developerSolutionId) {
            DeveloperSolutionVersionNotLatestError(developerSolutionId)
        }

        if (file == null && expectedScore == null) {
            return developerSolutionRepository.update(resource.withData { name = resourceName ?: resource.data.name }).asSuccess()
        }

        val editable = task.getEditableContent()
        val replacedIds = developerSolutionRepository.load(editable.developerSolutions)
            .filter { attached -> attached.data.versionBucket == resource.data.versionBucket }
            .map { attached -> attached.id }
            .toSet()

        val solutionId = if (file != null) {
            val previousSolution = solutionRepository.load(resource.data.solution)
            solutionRepository.save(
                previousSolution.withData { file(file.uploadedFilename, file.content) }.data,
            ).id
        } else {
            resource.data.solution.id
        }
        val updated = resource.withData {
            name = resourceName ?: resource.data.name
            solution = solutionId
            this.expectedScore = expectedScore ?: resource.data.expectedScore
        }
        val saved = developerSolutionRepository.save(updated.data)
        if (replacedIds.isNotEmpty()) {
            taskRepository.update(
                task.changeEditableContent {
                    developerSolutions = editable.developerSolutions.ids.map { attachedId ->
                        if (attachedId in replacedIds) saved.id else attachedId
                    }.toMutableList()
                },
            )
        }
        return saved.asSuccess()
    }

    /**
     * Attaches the statement with [newStatementId] to the work-in-progress version of the task with [taskId] on
     * behalf of [user]. It must be the latest version of an uploaded chain; the editable revision allows one statement.
     *
     * @since %CURRENT_VERSION%
     */
    @Feature("testsys.user.multi.developer.task.attachResource")
    fun attachStatement(user: MultipleRoleUser, taskId: TaskId, newStatementId: StatementId): OperationResult<Task, AttachStatementError> =
        operation<Task, AttachStatementError> {
            ensure(user.hasRole<Developer>(), MissedDeveloperRoleError)

            val task = taskRepository.findById(taskId)
            ensure(task != null) { TaskNotExistsError(taskId) }

            val statement = statementRepository.findById(newStatementId)
            ensure(statement != null) { StatementNotExistsError(newStatementId) }

            ensure(task.data.owner.id == user.id) { TaskAccessDeniedError(taskId) }

            ensure(statement.data.versionBucket in task.data.uploadedResources) {
                StatementNotUploadedToTaskError(taskId = taskId, statementId = newStatementId)
            }

            ensure(statementRepository.findLatestByVersionBucket(statement.data.versionBucket)?.id == newStatementId) {
                StatementVersionNotLatestError(newStatementId)
            }

            val wipContent = task.getEditableContent()
            wipContent.statement?.let { attached ->
                ensure(statementRepository.load(attached).data.versionBucket != statement.data.versionBucket) {
                    ResourceAlreadyAttachedError(taskId, statement.data.versionBucket)
                }
            }
            ensure(wipContent.statement == null, TaskAlreadyHasStatementError)

            val updatedTask = task.changeEditableContent { this.statement = newStatementId }

            val savedTask = taskRepository.update(updatedTask)
            return savedTask.asSuccess()
        }

    /**
     * Attaches [newExerciseId] to the editable revision of [taskId] on behalf of [user]. It must be the latest version
     * of an uploaded chain; rejects an attached chain or another exercise using the same language.
     *
     * @since %CURRENT_VERSION%
     */
    @Feature("testsys.user.multi.developer.task.attachResource")
    fun attachExercise(user: MultipleRoleUser, taskId: TaskId, newExerciseId: ExerciseId): OperationResult<Task, AttachExerciseError> =
        operation<Task, AttachExerciseError> {
            ensure(user.hasRole<Developer>(), MissedDeveloperRoleError)
            val task = taskRepository.findById(taskId)
            ensure(task != null) { TaskNotExistsError(taskId) }
            val resource = exerciseRepository.findById(newExerciseId)
            ensure(resource != null) { ExerciseNotExistsError(newExerciseId) }
            ensure(task.data.owner.id == user.id) { TaskAccessDeniedError(taskId) }
            ensure(resource.data.versionBucket in task.data.uploadedResources) {
                ExerciseNotUploadedToTaskError(taskId, newExerciseId)
            }
            ensure(exerciseRepository.findLatestByVersionBucket(resource.data.versionBucket)?.id == newExerciseId) {
                ExerciseVersionNotLatestError(newExerciseId)
            }
            val wipContent = task.getEditableContent()
            val attached = exerciseRepository.load(wipContent.exercises)
            ensure(attached.none { it.data.versionBucket == resource.data.versionBucket }) {
                ResourceAlreadyAttachedError(taskId, resource.data.versionBucket)
            }
            ensure(attached.none { it.data.language == resource.data.language }) {
                ExerciseLanguageAlreadyAttachedError(taskId = taskId, language = resource.data.language)
            }
            val updatedTask = task.changeEditableContent { exercises.add(newExerciseId) }
            return taskRepository.update(updatedTask).asSuccess()
        }

    /**
     * Attaches [newTestId] to the editable revision of [taskId] on behalf of [user].
     * Requires the latest version of an uploaded chain and rejects an already attached chain.
     *
     * @since %CURRENT_VERSION%
     */
    @Feature("testsys.user.multi.developer.task.attachResource")
    fun attachTest(user: MultipleRoleUser, taskId: TaskId, newTestId: TestId): OperationResult<Task, AttachTestError> =
        operation<Task, AttachTestError> {
            ensure(user.hasRole<Developer>(), MissedDeveloperRoleError)
            val task = taskRepository.findById(taskId)
            ensure(task != null) { TaskNotExistsError(taskId) }
            val resource = testRepository.findById(newTestId)
            ensure(resource != null) { TestNotExistsError(newTestId) }
            ensure(task.data.owner.id == user.id) { TaskAccessDeniedError(taskId) }
            ensure(resource.data.versionBucket in task.data.uploadedResources) {
                TestNotUploadedToTaskError(taskId, newTestId)
            }
            ensure(testRepository.findLatestByVersionBucket(resource.data.versionBucket)?.id == newTestId) {
                TestVersionNotLatestError(newTestId)
            }
            val wipContent = task.getEditableContent()
            val attached = testRepository.load(wipContent.tests)
            ensure(attached.none { it.data.versionBucket == resource.data.versionBucket }) {
                ResourceAlreadyAttachedError(taskId, resource.data.versionBucket)
            }
            val updatedTask = task.changeEditableContent { tests.add(newTestId) }
            return taskRepository.update(updatedTask).asSuccess()
        }

    /**
     * Attaches [newDeveloperSolutionId] to the editable revision of [taskId] on behalf of [user].
     * Requires the latest version of an uploaded chain and rejects an already attached chain.
     *
     * @since %CURRENT_VERSION%
     */
    @Feature("testsys.user.multi.developer.task.attachResource")
    fun attachDeveloperSolution(
        user: MultipleRoleUser,
        taskId: TaskId,
        newDeveloperSolutionId: DeveloperSolutionId,
    ): OperationResult<Task, AttachDeveloperSolutionError> = operation<Task, AttachDeveloperSolutionError> {
        ensure(user.hasRole<Developer>(), MissedDeveloperRoleError)
        val task = taskRepository.findById(taskId)
        ensure(task != null) { TaskNotExistsError(taskId) }
        val resource = developerSolutionRepository.findById(newDeveloperSolutionId)
        ensure(resource != null) { DeveloperSolutionNotExistsError(newDeveloperSolutionId) }
        ensure(task.data.owner.id == user.id) { TaskAccessDeniedError(taskId) }
        ensure(resource.data.versionBucket in task.data.uploadedResources) {
            DeveloperSolutionNotUploadedToTaskError(taskId, newDeveloperSolutionId)
        }
        ensure(developerSolutionRepository.findLatestByVersionBucket(resource.data.versionBucket)?.id == newDeveloperSolutionId) {
            DeveloperSolutionVersionNotLatestError(newDeveloperSolutionId)
        }
        val wipContent = task.getEditableContent()
        val attached = developerSolutionRepository.load(wipContent.developerSolutions)
        ensure(attached.none { it.data.versionBucket == resource.data.versionBucket }) {
            ResourceAlreadyAttachedError(taskId, resource.data.versionBucket)
        }
        val updatedTask = task.changeEditableContent { developerSolutions.add(newDeveloperSolutionId) }
        return taskRepository.update(updatedTask).asSuccess()
    }

    /**
     * Detaches the exact [statementId] from the editable revision of [taskId] owned by [user].
     * Preserves the last committed revision, uploaded chains and resource versions.
     *
     * @since %CURRENT_VERSION%
     */
    @Feature("testsys.user.multi.developer.task.detachResource")
    fun detachStatement(user: MultipleRoleUser, taskId: TaskId, statementId: StatementId): OperationResult<Task, DetachStatementError> =
        operation<Task, DetachStatementError> {
            ensure(user.hasRole<Developer>(), MissedDeveloperRoleError)
            val task = taskRepository.findById(taskId)
            ensure(task != null) { TaskNotExistsError(taskId) }
            val resource = statementRepository.findById(statementId)
            ensure(resource != null) { StatementNotExistsError(statementId) }
            ensure(task.data.owner.id == user.id) { TaskAccessDeniedError(taskId) }
            ensure(resource.data.versionBucket in task.data.uploadedResources) {
                StatementNotUploadedToTaskError(taskId = taskId, statementId = statementId)
            }
            val editable = task.getEditableContent()
            ensure(editable.statement?.id == statementId) {
                ResourceVersionNotAttachedError(taskId = taskId, versionId = statementId)
            }
            val updatedTask = task.changeEditableContent { statement = null }
            return taskRepository.update(updatedTask).asSuccess()
        }

    /**
     * Detaches the exact [exerciseId] from the editable revision of [taskId] owned by [user].
     * Preserves the last committed revision, uploaded chains and resource versions.
     *
     * @since %CURRENT_VERSION%
     */
    @Feature("testsys.user.multi.developer.task.detachResource")
    fun detachExercise(user: MultipleRoleUser, taskId: TaskId, exerciseId: ExerciseId): OperationResult<Task, DetachExerciseError> =
        operation<Task, DetachExerciseError> {
            ensure(user.hasRole<Developer>(), MissedDeveloperRoleError)
            val task = taskRepository.findById(taskId)
            ensure(task != null) { TaskNotExistsError(taskId) }
            val resource = exerciseRepository.findById(exerciseId)
            ensure(resource != null) { ExerciseNotExistsError(exerciseId) }
            ensure(task.data.owner.id == user.id) { TaskAccessDeniedError(taskId) }
            ensure(resource.data.versionBucket in task.data.uploadedResources) {
                ExerciseNotUploadedToTaskError(taskId = taskId, exerciseId = exerciseId)
            }
            val editable = task.getEditableContent()
            ensure(exerciseId in editable.exercises.ids) {
                ResourceVersionNotAttachedError(taskId = taskId, versionId = exerciseId)
            }
            val updatedTask = task.changeEditableContent { exercises.remove(exerciseId) }
            return taskRepository.update(updatedTask).asSuccess()
        }

    /**
     * Detaches the exact [testId] from the editable revision of [taskId] owned by [user].
     * Preserves the last committed revision, uploaded chains and resource versions.
     *
     * @since %CURRENT_VERSION%
     */
    @Feature("testsys.user.multi.developer.task.detachResource")
    fun detachTest(user: MultipleRoleUser, taskId: TaskId, testId: TestId): OperationResult<Task, DetachTestError> =
        operation<Task, DetachTestError> {
            ensure(user.hasRole<Developer>(), MissedDeveloperRoleError)
            val task = taskRepository.findById(taskId)
            ensure(task != null) { TaskNotExistsError(taskId) }
            val resource = testRepository.findById(testId)
            ensure(resource != null) { TestNotExistsError(testId) }
            ensure(task.data.owner.id == user.id) { TaskAccessDeniedError(taskId) }
            ensure(resource.data.versionBucket in task.data.uploadedResources) {
                TestNotUploadedToTaskError(taskId = taskId, testId = testId)
            }
            val editable = task.getEditableContent()
            ensure(testId in editable.tests.ids) {
                ResourceVersionNotAttachedError(taskId = taskId, versionId = testId)
            }
            val updatedTask = task.changeEditableContent { tests.remove(testId) }
            return taskRepository.update(updatedTask).asSuccess()
        }

    /**
     * Detaches the exact [developerSolutionId] from the editable revision of [taskId] owned by [user].
     * Preserves the last committed revision, uploaded chains and resource versions.
     *
     * @since %CURRENT_VERSION%
     */
    @Feature("testsys.user.multi.developer.task.detachResource")
    fun detachDeveloperSolution(
        user: MultipleRoleUser,
        taskId: TaskId,
        developerSolutionId: DeveloperSolutionId,
    ): OperationResult<Task, DetachDeveloperSolutionError> = operation<Task, DetachDeveloperSolutionError> {
        ensure(user.hasRole<Developer>(), MissedDeveloperRoleError)
        val task = taskRepository.findById(taskId)
        ensure(task != null) { TaskNotExistsError(taskId) }
        val resource = developerSolutionRepository.findById(developerSolutionId)
        ensure(resource != null) { DeveloperSolutionNotExistsError(developerSolutionId) }
        ensure(task.data.owner.id == user.id) { TaskAccessDeniedError(taskId) }
        ensure(resource.data.versionBucket in task.data.uploadedResources) {
            DeveloperSolutionNotUploadedToTaskError(taskId = taskId, developerSolutionId = developerSolutionId)
        }
        val editable = task.getEditableContent()
        ensure(developerSolutionId in editable.developerSolutions.ids) {
            ResourceVersionNotAttachedError(taskId = taskId, versionId = developerSolutionId)
        }
        val updatedTask = task.changeEditableContent { developerSolutions.remove(developerSolutionId) }
        return taskRepository.update(updatedTask).asSuccess()
    }

    /**
     * Shares the task with [taskId] owned by [user] to the communities with [communityIds], adding them to the
     * communities the task is already shared to.
     *
     * @since %CURRENT_VERSION%
     */
    @Feature("testsys.user.multi.developer.task.shareTask")
    fun shareTask(user: MultipleRoleUser, taskId: TaskId, communityIds: Set<CommunityId>): OperationResult<Task, ShareTaskError> =
        operation<Task, ShareTaskError> {
            ensure(user.hasRole<Developer>(), MissedDeveloperRoleError)

            val task = taskRepository.findById(taskId)
            ensure(task != null) { TaskNotExistsError(taskId) }

            communityIds.forEach { communityId ->
                ensure(communityRepository.findById(communityId) != null) { CommunityNotExistsError(communityId) }
            }

            ensure(task.data.owner.id == user.id) { TaskAccessDeniedError(taskId) }

            val sharedCommunityIds = task.data.sharedTo.ids
            val developerCommunityIds = user.data.roles.filterIsInstance<Developer>().single().memberOf.ids
            communityIds.filterNot { communityId -> communityId in sharedCommunityIds }.forEach { communityId ->
                ensure(communityId in developerCommunityIds) { CommunityAccessDeniedError(communityId) }
            }

            ensure(task.data.content !is TaskContent.New) { TaskNotCommittedError(taskId) }

            val sharedTask = task.withData {
                sharedTo = (sharedCommunityIds + communityIds).distinct().toMutableList()
            }

            val savedTask = taskRepository.update(sharedTask)
            return savedTask.asSuccess()
        }

    /**
     * Restores the committed content of [taskId] owned by [user], creating latest resource versions when necessary.
     * Task and resource metadata are retained; storage exceptions propagate to the caller.
     *
     * @since %CURRENT_VERSION%
     */
    @Feature("testsys.user.multi.developer.task.revertTask")
    fun revertTask(user: MultipleRoleUser, taskId: TaskId): OperationResult<Task, RevertTaskError> = operation<Task, RevertTaskError> {
        ensure(user.hasRole<Developer>(), MissedDeveloperRoleError)
        val task = taskRepository.findById(taskId)
        ensure(task != null) { TaskNotExistsError(taskId) }
        ensure(task.data.owner.id == user.id) { TaskAccessDeniedError(taskId) }
        val previous = task.data.content
        ensure(previous !is TaskContent.New) { TaskNotCommittedError(taskId) }
        ensure(previous !is TaskContent.Committed) { TaskAlreadyCommittedError(taskId) }
        check(previous is TaskContent.Uncommitted) { "Task $taskId must have uncommitted content to revert" }
        val committed = previous.lastCommitted

        val statement = statementRepository.load(committed.statement)
        val exercises = exerciseRepository.load(committed.exercises).associateBy { it.id }
        val tests = testRepository.load(committed.tests).associateBy { it.id }
        val developerSolutions = developerSolutionRepository.load(committed.developerSolutions).associateBy { it.id }

        val statementId = restoreStatement(statement)
        val exerciseIds = committed.exercises.ids.map { id -> restoreExercise(exercises.getValue(id)) }
        val testIds = committed.tests.ids.map { id -> restoreTest(tests.getValue(id)) }
        val developerSolutionIds = committed.developerSolutions.ids.map { id -> restoreDeveloperSolution(developerSolutions.getValue(id)) }
        val restored = task.withData {
            content.committed {
                this.statement = statementId
                this.exercises = exerciseIds.toMutableList()
                this.tests = testIds.toMutableList()
                this.developerSolutions = developerSolutionIds.toMutableList()
                supportedTrikStudioVersions = committed.supportedTrikStudioVersions.toMutableList()
            }
        }
        return taskRepository.update(restored).asSuccess()
    }

    private fun restoreStatement(resource: Statement): StatementId {
        val latest = checkNotNull(statementRepository.findLatestByVersionBucket(resource.data.versionBucket)) {
            "Statement ${resource.id} has no latest version in bucket ${resource.data.versionBucket}"
        }
        if (latest.id == resource.id) return resource.id
        val restored = resource.withData {
            name = latest.data.name
            description = latest.data.description
        }
        return statementRepository.save(restored.data).id
    }

    private fun restoreExercise(resource: Exercise): ExerciseId {
        val latest = checkNotNull(exerciseRepository.findLatestByVersionBucket(resource.data.versionBucket)) {
            "Exercise ${resource.id} has no latest version in bucket ${resource.data.versionBucket}"
        }
        if (latest.id == resource.id) return resource.id
        val restored = resource.withData {
            name = latest.data.name
            description = latest.data.description
        }
        return exerciseRepository.save(restored.data).id
    }

    private fun restoreTest(resource: Test): TestId {
        val latest = checkNotNull(testRepository.findLatestByVersionBucket(resource.data.versionBucket)) {
            "Test ${resource.id} has no latest version in bucket ${resource.data.versionBucket}"
        }
        if (latest.id == resource.id) return resource.id
        val restored = resource.withData {
            name = latest.data.name
            description = latest.data.description
        }
        return testRepository.save(restored.data).id
    }

    private fun restoreDeveloperSolution(resource: DeveloperSolution): DeveloperSolutionId {
        val latest = checkNotNull(developerSolutionRepository.findLatestByVersionBucket(resource.data.versionBucket)) {
            "Developer solution ${resource.id} has no latest version in bucket ${resource.data.versionBucket}"
        }
        if (latest.id == resource.id) return resource.id
        val restored = resource.withData {
            name = latest.data.name
            description = latest.data.description
        }
        return developerSolutionRepository.save(restored.data).id
    }

    private fun resourceExists(versionBucket: VersionBucket): Boolean = statementRepository.existsByVersionBucket(versionBucket) ||
        exerciseRepository.existsByVersionBucket(versionBucket) ||
        testRepository.existsByVersionBucket(versionBucket) ||
        developerSolutionRepository.existsByVersionBucket(versionBucket)

    private fun Duration.requireExactMillis(field: String) {
        require(nano % NANOS_PER_MILLISECOND == 0) { "Contest $field=$this must be exactly representable in milliseconds" }
        try {
            toMillis()
        } catch (exception: ArithmeticException) {
            throw IllegalArgumentException("Contest $field=$this exceeds the Long millisecond range", exception)
        }
    }

    private companion object {
        const val NANOS_PER_MILLISECOND = 1_000_000
    }
}
