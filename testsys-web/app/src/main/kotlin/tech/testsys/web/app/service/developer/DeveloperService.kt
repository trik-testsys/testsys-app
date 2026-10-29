package tech.testsys.web.app.service.developer

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tech.testsys.domain.contract.persistence.ContestFilter
import tech.testsys.domain.contract.persistence.Page
import tech.testsys.domain.contract.persistence.Pagination
import tech.testsys.domain.contract.persistence.TaskFilter
import tech.testsys.domain.model.DomainId
import tech.testsys.domain.model.group.CommunityId
import tech.testsys.domain.model.task.ContestId
import tech.testsys.domain.model.task.DeveloperSolutionId
import tech.testsys.domain.model.task.ExerciseId
import tech.testsys.domain.model.task.FileData
import tech.testsys.domain.model.task.Score
import tech.testsys.domain.model.task.StatementId
import tech.testsys.domain.model.task.TaskId
import tech.testsys.domain.model.task.TestId
import tech.testsys.domain.model.task.TrikStudioVersion
import tech.testsys.domain.model.task.TrikSupportedLanguage
import tech.testsys.domain.model.task.VersionBucket
import tech.testsys.operation.error.getOrThrow
import tech.testsys.operation.user.DeveloperOperations
import tech.testsys.web.app.service.CommunityVo
import tech.testsys.web.app.service.ContestVo
import tech.testsys.web.app.service.CurrentUser
import tech.testsys.web.app.service.TaskVo
import tech.testsys.web.app.service.map
import tech.testsys.web.app.service.toVo
import java.time.Duration
import java.time.Instant

/**
 * Runs [DeveloperOperations] for the current user in one transaction per call.
 * A failed operation throws `OperationException`, which rolls the transaction back.
 *
 * @since 1.0.0-beta.1
 */
@Service
@Transactional
class DeveloperService(private val operations: DeveloperOperations, private val currentUser: CurrentUser) {
    /**
     * Runs [DeveloperOperations.testTask].
     *
     * @since 1.0.0-beta.1
     */
    fun testTask(taskId: TaskId): TaskValidationRequestVo = operations.testTask(currentUser.multipleRoleUser(), taskId).getOrThrow().toVo()

    /**
     * Runs [DeveloperOperations.viewTaskValidationRequests].
     *
     * @since 1.0.0-beta.1
     */
    @Transactional(readOnly = true)
    fun viewTaskValidationRequests(taskId: TaskId): List<TaskValidationRequestVo> =
        operations.viewTaskValidationRequests(currentUser.multipleRoleUser(), taskId)
            .getOrThrow().map { request -> request.toVo() }

    /**
     * Runs [DeveloperOperations.commitTask].
     *
     * @since 1.0.0-beta.1
     */
    fun commitTask(taskId: TaskId, regradeSubmissions: Boolean): TaskVo =
        operations.commitTask(currentUser.multipleRoleUser(), taskId, regradeSubmissions).getOrThrow().toVo()

    /**
     * Runs [DeveloperOperations.countRegradeSubmissions].
     *
     * @since 1.0.0-beta.1
     */
    @Transactional(readOnly = true)
    fun countRegradeSubmissions(taskId: TaskId): Int? =
        operations.countRegradeSubmissions(currentUser.multipleRoleUser(), taskId).getOrThrow()

    /**
     * Runs [DeveloperOperations.viewRegrades]; each regrade comes with the number of its graded submissions.
     *
     * @since 1.0.0-beta.1
     */
    @Transactional(readOnly = true)
    fun viewRegrades(taskId: TaskId): List<Pair<RegradeVo, Int>> =
        operations.viewRegrades(currentUser.multipleRoleUser(), taskId).getOrThrow().map { (regrade, graded) -> regrade.toVo() to graded }

    /**
     * Runs [DeveloperOperations.viewContests].
     *
     * @since 1.0.0-beta.1
     */
    @Transactional(readOnly = true)
    fun viewContests(pagination: Pagination, filter: ContestFilter = ContestFilter()): Page<Pair<ContestVo, List<CommunityVo>>> =
        operations.viewContests(currentUser.multipleRoleUser(), pagination, filter)
            .getOrThrow().map { (contest, communities) -> contest.toVo() to communities.map { community -> community.toVo() } }

    /**
     * Runs [DeveloperOperations.viewContest].
     *
     * @since 1.0.0-beta.1
     */
    @Transactional(readOnly = true)
    fun viewContest(contestId: ContestId): Triple<ContestVo, List<TaskVo>, List<CommunityVo>> {
        val (contest, tasks, communities) = operations.viewContest(currentUser.multipleRoleUser(), contestId).getOrThrow()
        return Triple(contest.toVo(), tasks.map { task -> task.toVo() }, communities.map { community -> community.toVo() })
    }

    /**
     * Runs [DeveloperOperations.viewTrikStudioVersions].
     *
     * @since 1.0.0-beta.1
     */
    @Transactional(readOnly = true)
    fun viewTrikStudioVersions(): List<TrikStudioVersion> = operations.viewTrikStudioVersions(currentUser.multipleRoleUser()).getOrThrow()

    /**
     * Runs [DeveloperOperations.createContest].
     *
     * @since 1.0.0-beta.1
     */
    fun createContest(
        contestName: String,
        trikStudioVersion: TrikStudioVersion,
        attemptDuration: Duration? = null,
        startsAt: Instant? = null,
        endsAt: Instant? = null,
        contestDescription: String = "",
    ): ContestVo = operations.createContest(
        user = currentUser.multipleRoleUser(),
        contestName = contestName,
        trikStudioVersion = trikStudioVersion,
        attemptDuration = attemptDuration,
        startsAt = startsAt,
        endsAt = endsAt,
        contestDescription = contestDescription,
    ).getOrThrow().toVo()

    /**
     * Runs [DeveloperOperations.editContest].
     *
     * @since 1.0.0-beta.1
     */
    fun editContest(contestId: ContestId, contestName: String, startsAt: Instant?, endsAt: Instant?): ContestVo = operations.editContest(
        user = currentUser.multipleRoleUser(),
        contestId = contestId,
        contestName = contestName,
        startsAt = startsAt,
        endsAt = endsAt,
    ).getOrThrow().toVo()

    /**
     * Runs [DeveloperOperations.shareContest].
     *
     * @since 1.0.0-beta.1
     */
    fun shareContest(contestId: ContestId, communityIds: Set<CommunityId>): ContestVo =
        operations.shareContest(currentUser.multipleRoleUser(), contestId, communityIds).getOrThrow().toVo()

    /**
     * Runs [DeveloperOperations.attachTask].
     *
     * @since 1.0.0-beta.1
     */
    fun attachTask(contestId: ContestId, taskId: TaskId): ContestVo =
        operations.attachTask(currentUser.multipleRoleUser(), contestId, taskId).getOrThrow().toVo()

    /**
     * Runs [DeveloperOperations.detachTask].
     *
     * @since 1.0.0-beta.1
     */
    fun detachTask(contestId: ContestId, taskId: TaskId): ContestVo =
        operations.detachTask(currentUser.multipleRoleUser(), contestId, taskId).getOrThrow().toVo()

    /**
     * Runs [DeveloperOperations.deleteContest].
     *
     * @since 1.0.0-beta.1
     */
    fun deleteContest(contestId: ContestId): ContestVo =
        operations.deleteContest(currentUser.multipleRoleUser(), contestId).getOrThrow().toVo()

    /**
     * Runs [DeveloperOperations.viewTasks].
     *
     * @since 1.0.0-beta.1
     */
    @Transactional(readOnly = true)
    fun viewTasks(pagination: Pagination, filter: TaskFilter = TaskFilter()): Page<TaskVo> =
        operations.viewTasks(currentUser.multipleRoleUser(), pagination, filter).getOrThrow().map { task -> task.toVo() }

    /**
     * Runs [DeveloperOperations.viewTask].
     *
     * @since 1.0.0-beta.1
     */
    @Transactional(readOnly = true)
    fun viewTask(taskId: TaskId): Pair<TaskVo, List<CommunityVo>> {
        val (task, communities) = operations.viewTask(currentUser.multipleRoleUser(), taskId).getOrThrow()
        return task.toVo() to communities.map { community -> community.toVo() }
    }

    /**
     * Runs [DeveloperOperations.editTaskInfo].
     *
     * @since 1.0.0-beta.1
     */
    fun editTaskInfo(
        taskId: TaskId,
        taskName: String? = null,
        taskDescription: String? = null,
        supportedTrikStudioVersions: List<TrikStudioVersion>? = null,
    ): TaskVo = operations.editTaskInfo(
        user = currentUser.multipleRoleUser(),
        taskId = taskId,
        taskName = taskName,
        taskDescription = taskDescription,
        supportedTrikStudioVersions = supportedTrikStudioVersions,
    ).getOrThrow().toVo()

    /**
     * Runs [DeveloperOperations.viewResources].
     *
     * @since 1.0.0-beta.1
     */
    @Transactional(readOnly = true)
    fun viewResources(): List<ResourceVo> = operations.viewResources(currentUser.multipleRoleUser())
        .getOrThrow().map { resource -> resource.toResourceVo() }

    /**
     * Runs [DeveloperOperations.viewResource].
     *
     * @since 1.0.0-beta.1
     */
    @Transactional(readOnly = true)
    fun viewResource(taskId: TaskId, versionBucket: VersionBucket): List<Pair<ResourceVo, SolutionVo?>> =
        operations.viewResource(currentUser.multipleRoleUser(), taskId, versionBucket)
            .getOrThrow().map { (version, solution) -> version.toResourceVo() to solution?.toVo() }

    /**
     * Runs [DeveloperOperations.downloadResourceVersion].
     *
     * @since 1.0.0-beta.1
     */
    @Transactional(readOnly = true)
    fun downloadResourceVersion(taskId: TaskId, versionBucket: VersionBucket, versionId: DomainId): FileData =
        operations.downloadResourceVersion(currentUser.multipleRoleUser(), taskId, versionBucket, versionId).getOrThrow()

    /**
     * Runs [DeveloperOperations.createTask].
     *
     * @since 1.0.0-beta.1
     */
    fun createTask(taskName: String, taskDescription: String): TaskVo = operations.createTask(
        user = currentUser.multipleRoleUser(),
        taskName = taskName,
        taskDescription = taskDescription,
    ).getOrThrow().toVo()

    /**
     * Runs [DeveloperOperations.addStatement].
     *
     * @since 1.0.0-beta.1
     */
    fun addStatement(taskId: TaskId, resourceName: String, file: FileData): StatementVo =
        operations.addStatement(currentUser.multipleRoleUser(), taskId, resourceName, file).getOrThrow().toVo()

    /**
     * Runs [DeveloperOperations.addTest].
     *
     * @since 1.0.0-beta.1
     */
    fun addTest(taskId: TaskId, resourceName: String, file: FileData): TestVo =
        operations.addTest(currentUser.multipleRoleUser(), taskId, resourceName, file).getOrThrow().toVo()

    /**
     * Runs [DeveloperOperations.addExercise].
     *
     * @since 1.0.0-beta.1
     */
    fun addExercise(taskId: TaskId, resourceName: String, file: FileData, language: TrikSupportedLanguage): ExerciseVo =
        operations.addExercise(currentUser.multipleRoleUser(), taskId, resourceName, file, language).getOrThrow().toVo()

    /**
     * Runs [DeveloperOperations.addDeveloperSolution].
     *
     * @since 1.0.0-beta.1
     */
    fun addDeveloperSolution(
        taskId: TaskId,
        resourceName: String,
        file: FileData,
        language: TrikSupportedLanguage,
        expectedScore: Score,
    ): DeveloperSolutionVo = operations.addDeveloperSolution(
        user = currentUser.multipleRoleUser(),
        taskId = taskId,
        resourceName = resourceName,
        file = file,
        language = language,
        expectedScore = expectedScore,
    ).getOrThrow().toVo()

    /**
     * Runs [DeveloperOperations.updateStatement].
     *
     * @since 1.0.0-beta.1
     */
    fun updateStatement(taskId: TaskId, statementId: StatementId, resourceName: String? = null, file: FileData? = null): StatementVo =
        operations.updateStatement(currentUser.multipleRoleUser(), taskId, statementId, resourceName, file)
            .getOrThrow().toVo()

    /**
     * Runs [DeveloperOperations.updateExercise].
     *
     * @since 1.0.0-beta.1
     */
    fun updateExercise(taskId: TaskId, exerciseId: ExerciseId, resourceName: String? = null, file: FileData? = null): ExerciseVo =
        operations.updateExercise(currentUser.multipleRoleUser(), taskId, exerciseId, resourceName, file)
            .getOrThrow().toVo()

    /**
     * Runs [DeveloperOperations.updateTest].
     *
     * @since 1.0.0-beta.1
     */
    fun updateTest(taskId: TaskId, testId: TestId, resourceName: String? = null, file: FileData? = null): TestVo =
        operations.updateTest(currentUser.multipleRoleUser(), taskId, testId, resourceName, file).getOrThrow().toVo()

    /**
     * Runs [DeveloperOperations.updateDeveloperSolution].
     *
     * @since 1.0.0-beta.1
     */
    fun updateDeveloperSolution(
        taskId: TaskId,
        developerSolutionId: DeveloperSolutionId,
        resourceName: String? = null,
        file: FileData? = null,
        expectedScore: Score? = null,
    ): DeveloperSolutionVo = operations.updateDeveloperSolution(
        user = currentUser.multipleRoleUser(),
        taskId = taskId,
        developerSolutionId = developerSolutionId,
        resourceName = resourceName,
        file = file,
        expectedScore = expectedScore,
    ).getOrThrow().toVo()

    /**
     * Runs [DeveloperOperations.attachStatement].
     *
     * @since 1.0.0-beta.1
     */
    fun attachStatement(taskId: TaskId, newStatementId: StatementId): TaskVo =
        operations.attachStatement(currentUser.multipleRoleUser(), taskId, newStatementId).getOrThrow().toVo()

    /**
     * Runs [DeveloperOperations.attachExercise].
     *
     * @since 1.0.0-beta.1
     */
    fun attachExercise(taskId: TaskId, newExerciseId: ExerciseId): TaskVo =
        operations.attachExercise(currentUser.multipleRoleUser(), taskId, newExerciseId).getOrThrow().toVo()

    /**
     * Runs [DeveloperOperations.attachTest].
     *
     * @since 1.0.0-beta.1
     */
    fun attachTest(taskId: TaskId, newTestId: TestId): TaskVo =
        operations.attachTest(currentUser.multipleRoleUser(), taskId, newTestId).getOrThrow().toVo()

    /**
     * Runs [DeveloperOperations.attachDeveloperSolution].
     *
     * @since 1.0.0-beta.1
     */
    fun attachDeveloperSolution(taskId: TaskId, newDeveloperSolutionId: DeveloperSolutionId): TaskVo =
        operations.attachDeveloperSolution(currentUser.multipleRoleUser(), taskId, newDeveloperSolutionId).getOrThrow().toVo()

    /**
     * Runs [DeveloperOperations.detachStatement].
     *
     * @since 1.0.0-beta.1
     */
    fun detachStatement(taskId: TaskId, statementId: StatementId): TaskVo =
        operations.detachStatement(currentUser.multipleRoleUser(), taskId, statementId).getOrThrow().toVo()

    /**
     * Runs [DeveloperOperations.detachExercise].
     *
     * @since 1.0.0-beta.1
     */
    fun detachExercise(taskId: TaskId, exerciseId: ExerciseId): TaskVo =
        operations.detachExercise(currentUser.multipleRoleUser(), taskId, exerciseId).getOrThrow().toVo()

    /**
     * Runs [DeveloperOperations.detachTest].
     *
     * @since 1.0.0-beta.1
     */
    fun detachTest(taskId: TaskId, testId: TestId): TaskVo =
        operations.detachTest(currentUser.multipleRoleUser(), taskId, testId).getOrThrow().toVo()

    /**
     * Runs [DeveloperOperations.detachDeveloperSolution].
     *
     * @since 1.0.0-beta.1
     */
    fun detachDeveloperSolution(taskId: TaskId, developerSolutionId: DeveloperSolutionId): TaskVo =
        operations.detachDeveloperSolution(currentUser.multipleRoleUser(), taskId, developerSolutionId).getOrThrow().toVo()

    /**
     * Runs [DeveloperOperations.shareTask].
     *
     * @since 1.0.0-beta.1
     */
    fun shareTask(taskId: TaskId, communityIds: Set<CommunityId>): TaskVo =
        operations.shareTask(currentUser.multipleRoleUser(), taskId, communityIds).getOrThrow().toVo()

    /**
     * Runs [DeveloperOperations.revertTask].
     *
     * @since 1.0.0-beta.1
     */
    fun revertTask(taskId: TaskId): TaskVo = operations.revertTask(currentUser.multipleRoleUser(), taskId).getOrThrow().toVo()
}
