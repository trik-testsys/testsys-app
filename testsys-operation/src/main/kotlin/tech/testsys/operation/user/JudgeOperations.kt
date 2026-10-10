package tech.testsys.operation.user

import tech.testsys.domain.builder.api.judgmentOrderData
import tech.testsys.domain.contract.persistence.Page
import tech.testsys.domain.contract.persistence.Pagination
import tech.testsys.domain.contract.persistence.VerdictFilter
import tech.testsys.domain.contract.persistence.repository.ContestRepository
import tech.testsys.domain.contract.persistence.repository.JudgmentOrderRepository
import tech.testsys.domain.contract.persistence.repository.LogsRepository
import tech.testsys.domain.contract.persistence.repository.MultipleRoleUserRepository
import tech.testsys.domain.contract.persistence.repository.ParticipantRepository
import tech.testsys.domain.contract.persistence.repository.RecordingRepository
import tech.testsys.domain.contract.persistence.repository.SolutionRepository
import tech.testsys.domain.contract.persistence.repository.SubmissionRepository
import tech.testsys.domain.contract.persistence.repository.TaskRepository
import tech.testsys.domain.contract.persistence.repository.TestRepository
import tech.testsys.domain.contract.persistence.repository.VerdictRepository
import tech.testsys.domain.model.LazyEntity
import tech.testsys.domain.model.task.FileData
import tech.testsys.domain.model.task.GradingResult
import tech.testsys.domain.model.task.JudgmentOrder
import tech.testsys.domain.model.task.Score
import tech.testsys.domain.model.task.Submission
import tech.testsys.domain.model.task.SubmissionId
import tech.testsys.domain.model.task.SubmissionKind
import tech.testsys.domain.model.task.SubmissionStatus
import tech.testsys.domain.model.task.TestId
import tech.testsys.domain.model.task.Verdict
import tech.testsys.domain.model.task.VerdictId
import tech.testsys.domain.model.user.Judge
import tech.testsys.domain.model.user.MultipleRoleUser
import tech.testsys.domain.model.user.MultipleRoleUserId
import tech.testsys.domain.model.user.SingleRoleUserId
import tech.testsys.domain.model.user.Student
import tech.testsys.domain.model.user.User
import tech.testsys.operation.annotation.Feature
import tech.testsys.operation.annotation.InternalOperationsApi
import tech.testsys.operation.error.BlankJudgmentReasonError
import tech.testsys.operation.error.ChangeVerdictError
import tech.testsys.operation.error.DownloadLogsError
import tech.testsys.operation.error.DownloadRecordingError
import tech.testsys.operation.error.DownloadSolutionError
import tech.testsys.operation.error.MissedJudgeRoleError
import tech.testsys.operation.error.NegativeJudgmentScoreError
import tech.testsys.operation.error.OperationResult
import tech.testsys.operation.error.RecordingNotExistsError
import tech.testsys.operation.error.SubmissionAccessDeniedError
import tech.testsys.operation.error.SubmissionIsDeveloperSolutionTestError
import tech.testsys.operation.error.SubmissionNotExistsError
import tech.testsys.operation.error.SubmissionNotSuccessfullyGradedError
import tech.testsys.operation.error.TestNotInVerdictError
import tech.testsys.operation.error.ViewResultsError
import tech.testsys.operation.error.ViewSolutionError
import tech.testsys.operation.error.asSuccess
import tech.testsys.operation.error.ensure
import tech.testsys.operation.error.operation
import tech.testsys.operation.util.findByIdsAsMap
import tech.testsys.operation.util.hasRole
import tech.testsys.operation.util.loadByIdsAsMap

/**
 * Operations performed by users holding the judge role.
 *
 * @since %CURRENT_VERSION%
 */
@OptIn(InternalOperationsApi::class)
class JudgeOperations(
    private val verdictRepository: VerdictRepository,
    private val submissionRepository: SubmissionRepository,
    private val judgmentOrderRepository: JudgmentOrderRepository,
    private val multipleRoleUserRepository: MultipleRoleUserRepository,
    private val participantRepository: ParticipantRepository,
    private val taskRepository: TaskRepository,
    private val contestRepository: ContestRepository,
    private val solutionRepository: SolutionRepository,
    private val testRepository: TestRepository,
    private val logsRepository: LogsRepository,
    private val recordingRepository: RecordingRepository,
) {

    /**
     * Returns a page of current successful verdicts available to [user], optionally filtered by [filter], each with the
     * author of its submission; test outcomes keep lazy file references.
     *
     * @since %CURRENT_VERSION%
     */
    @Feature("testsys.user.multi.judge.viewResults")
    fun viewResults(
        user: MultipleRoleUser,
        pagination: Pagination,
        filter: VerdictFilter = VerdictFilter(),
    ): OperationResult<Page<JudgeResult>, ViewResultsError> = operation<Page<JudgeResult>, ViewResultsError> {
        ensure(user.hasRole<Judge>(), MissedJudgeRoleError)
        val page = verdictRepository.findAvailableToJudge(pagination = pagination, filter = filter)

        val submissions = submissionRepository.loadByIdsAsMap(page.content.map { verdict -> verdict.data.submission.id })
        val authorIds = submissions.values.map { submission -> submission.data.author.id }
        val students = multipleRoleUserRepository.findByIdsAsMap(authorIds.filterIsInstance<MultipleRoleUserId>())
        val participants = participantRepository.findByIdsAsMap(authorIds.filterIsInstance<SingleRoleUserId>())
        val orders = judgmentOrderRepository.loadByIdsAsMap(submissions.values.flatMap { submission -> submission.data.judgmentOrders.ids })
        val content = page.content.map { verdict ->
            val submission = submissions.getValue(verdict.data.submission.id)
            val author = when (val id = submission.data.author.id) {
                is MultipleRoleUserId -> students[id]?.takeIf { student -> student.hasRole<Student>() }
                is SingleRoleUserId -> participants[id]
                else -> null
            }.takeIf { submission.data.kind is SubmissionKind.Grading }
            val lastOrder = submission.data.judgmentOrders.ids.map(orders::getValue)
                .maxWithOrNull(compareBy<JudgmentOrder> { order -> order.createdAt }.thenBy { order -> order.id.value })
            JudgeResult(
                verdict = verdict,
                author = checkNotNull(author) {
                    "Author of submission ${submission.id.value} with verdict ${verdict.id.value} is not available to judges"
                },
                submission = submission,
                lastJudgment = lastOrder,
                finalScore = lastOrder?.data?.score?.value?.toLong()
                    ?: verdict.data.testVerdicts.sumOf { outcome -> outcome.score.value.toLong() },
            )
        }
        return Page(content = content, pagination = page.pagination, totalElements = page.totalElements).asSuccess()
    }

    /**
     * Creates a judgment order for [submissionId] on behalf of [user], awarding [score] with [reason].
     * Requires a current student or participant author, a grading submission rather than a developer solution test,
     * successful automatic grading, a nonnegative score and a nonblank reason.
     *
     * @since %CURRENT_VERSION%
     */
    @Feature("testsys.user.multi.judge.changeVerdict")
    fun changeVerdict(
        user: MultipleRoleUser,
        submissionId: SubmissionId,
        score: Score,
        reason: String,
    ): OperationResult<JudgmentOrder, ChangeVerdictError> = operation<JudgmentOrder, ChangeVerdictError> {
        ensure(user.hasRole<Judge>(), MissedJudgeRoleError)
        val submission = submissionRepository.findById(submissionId)
        ensure(submission != null) { SubmissionNotExistsError(submissionId) }
        val hasAccessibleAuthor = when (val authorId = submission.data.author.id) {
            is MultipleRoleUserId -> multipleRoleUserRepository.findById(authorId)?.hasRole<Student>() == true
            is SingleRoleUserId -> participantRepository.findById(authorId) != null
            else -> false
        }
        ensure(hasAccessibleAuthor) { SubmissionAccessDeniedError(submissionId) }
        ensure(submission.data.kind is SubmissionKind.Grading) { SubmissionIsDeveloperSolutionTestError(submissionId) }
        val status = submission.data.status
        ensure(status is SubmissionStatus.Graded && status.grade is GradingResult.Success) {
            SubmissionNotSuccessfullyGradedError(submissionId)
        }
        ensure(score.value >= 0) { NegativeJudgmentScoreError(score) }
        ensure(reason.isNotBlank(), BlankJudgmentReasonError)

        val data = judgmentOrderData {
            judge = user.id
            this.submission = submissionId
            this.score = score.value
            this.reason = reason
        }
        val judgmentOrder = judgmentOrderRepository.save(data)
        return judgmentOrder.asSuccess()
    }

    /**
     * Returns the submission [submissionId] available to [user] with its author, task, contest, solution, current
     * successful verdict with its tests, final score and judgment orders with their judges.
     *
     * @since %CURRENT_VERSION%
     */
    @Feature("testsys.user.multi.judge.viewSolution")
    fun viewSolution(user: MultipleRoleUser, submissionId: SubmissionId): OperationResult<SubmissionDetails, ViewSolutionError> =
        operation<SubmissionDetails, ViewSolutionError> {
            ensure(user.hasRole<Judge>(), MissedJudgeRoleError)
            val submission = submissionRepository.findById(submissionId)
            ensure(submission != null) { SubmissionNotExistsError(submissionId) }
            val author = availableAuthorOf(submission)
            ensure(author != null) { SubmissionAccessDeniedError(submissionId) }

            val contest = when (val kind = submission.data.kind) {
                is SubmissionKind.Grading -> contestRepository.load(kind.contest)
                is SubmissionKind.DeveloperSolutionTest -> error("Submission ${submissionId.value} available to judges is not graded")
            }
            val verdict = successfulVerdictOf(submission)?.let { reference -> verdictRepository.load(reference) }
            val testIds = verdict?.data?.testVerdicts?.map { outcome -> outcome.test.id }.orEmpty()
            val testsById = testRepository.loadByIdsAsMap(testIds)
            val tests = testIds.map(testsById::getValue)
            val orders = judgmentOrderRepository.findByIds(submission.data.judgmentOrders.ids)
                .sortedWith(compareBy<JudgmentOrder> { order -> order.createdAt }.thenBy { order -> order.id.value })
            val judges = multipleRoleUserRepository.findByIds(orders.map { order -> order.data.judge.id }.distinct())
                .associateBy { judge -> judge.id }
            val finalScore = verdict?.let { graded ->
                val lastOrder = orders.lastOrNull()
                lastOrder?.data?.score?.value?.toLong() ?: graded.data.testVerdicts.sumOf { outcome -> outcome.score.value.toLong() }
            }

            return SubmissionDetails(
                submission = submission,
                author = author,
                task = taskRepository.load(submission.data.task),
                contest = contest,
                solution = solutionRepository.load(submission.data.solution),
                verdict = verdict,
                tests = tests,
                finalScore = finalScore,
                judgmentOrders = orders.map { order ->
                    order to checkNotNull(judges[order.data.judge.id]) {
                        "Judge ${order.data.judge.id.value} of judgment order ${order.id.value} does not exist"
                    }
                },
            ).asSuccess()
        }

    /**
     * Returns the solution file of the submission [submissionId] available to [user].
     *
     * @since %CURRENT_VERSION%
     */
    @Feature("testsys.user.multi.judge.viewSolution")
    fun downloadSolution(user: MultipleRoleUser, submissionId: SubmissionId): OperationResult<FileData, DownloadSolutionError> =
        operation<FileData, DownloadSolutionError> {
            ensure(user.hasRole<Judge>(), MissedJudgeRoleError)
            val submission = submissionRepository.findById(submissionId)
            ensure(submission != null) { SubmissionNotExistsError(submissionId) }
            ensure(availableAuthorOf(submission) != null) { SubmissionAccessDeniedError(submissionId) }

            return solutionRepository.load(submission.data.solution).data.file.asSuccess()
        }

    /**
     * Returns the grading logs of the run on [testId] from the successful verdict of the submission [submissionId]
     * available to [user].
     *
     * @since %CURRENT_VERSION%
     */
    @Feature("testsys.user.multi.judge.viewSolution")
    fun downloadLogs(user: MultipleRoleUser, submissionId: SubmissionId, testId: TestId): OperationResult<FileData, DownloadLogsError> =
        operation<FileData, DownloadLogsError> {
            ensure(user.hasRole<Judge>(), MissedJudgeRoleError)
            val submission = submissionRepository.findById(submissionId)
            ensure(submission != null) { SubmissionNotExistsError(submissionId) }
            ensure(availableAuthorOf(submission) != null) { SubmissionAccessDeniedError(submissionId) }
            val verdict = successfulVerdictOf(submission)
            ensure(verdict != null) { SubmissionNotSuccessfullyGradedError(submissionId) }
            val outcome = verdictRepository.load(verdict).data.testVerdicts.firstOrNull { candidate -> candidate.test.id == testId }
            ensure(outcome != null) { TestNotInVerdictError(submissionId, testId) }

            return logsRepository.load(outcome.logs).data.file.asSuccess()
        }

    /**
     * Returns the recording of the run on [testId] from the successful verdict of the submission [submissionId]
     * available to [user].
     *
     * @since %CURRENT_VERSION%
     */
    @Feature("testsys.user.multi.judge.viewSolution")
    fun downloadRecording(
        user: MultipleRoleUser,
        submissionId: SubmissionId,
        testId: TestId,
    ): OperationResult<FileData, DownloadRecordingError> = operation<FileData, DownloadRecordingError> {
        ensure(user.hasRole<Judge>(), MissedJudgeRoleError)
        val submission = submissionRepository.findById(submissionId)
        ensure(submission != null) { SubmissionNotExistsError(submissionId) }
        ensure(availableAuthorOf(submission) != null) { SubmissionAccessDeniedError(submissionId) }
        val verdict = successfulVerdictOf(submission)
        ensure(verdict != null) { SubmissionNotSuccessfullyGradedError(submissionId) }
        val outcome = verdictRepository.load(verdict).data.testVerdicts.firstOrNull { candidate -> candidate.test.id == testId }
        ensure(outcome != null) { TestNotInVerdictError(submissionId, testId) }
        val recording = outcome.recording
        ensure(recording != null) { RecordingNotExistsError(submissionId, testId) }

        return recordingRepository.load(recording).data.file.asSuccess()
    }

    /**
     * Returns the author of [submission] if the submission is available to judges (testsys.user.multi.judge.authorization):
     * a contest submission of a current student or participant; `null` otherwise.
     */
    private fun availableAuthorOf(submission: Submission): User<*>? {
        if (submission.data.kind !is SubmissionKind.Grading) return null

        return when (val authorId = submission.data.author.id) {
            is MultipleRoleUserId -> multipleRoleUserRepository.findById(authorId)?.takeIf { author -> author.hasRole<Student>() }
            is SingleRoleUserId -> participantRepository.findById(authorId)
            else -> null
        }
    }

    /** Returns the reference to the current successful verdict of [submission], or `null` if grading has not succeeded. */
    private fun successfulVerdictOf(submission: Submission): LazyEntity<VerdictId, Verdict>? {
        val status = submission.data.status as? SubmissionStatus.Graded
        return (status?.grade as? GradingResult.Success)?.verdict
    }

    /**
     * Current successful verdict with its submission, author and final result.
     *
     * @property verdict the automatic verdict.
     * @property author the author available to the judge.
     * @property submission the graded submission.
     * @property lastJudgment the latest judgment by creation time and identifier, or `null` if absent.
     * @property finalScore the latest judgment score or the automatic total.
     * @since %CURRENT_VERSION%
     */
    data class JudgeResult(
        val verdict: Verdict,
        val author: User<*>,
        val submission: Submission,
        val lastJudgment: JudgmentOrder?,
        val finalScore: Long,
    )
}
