package tech.testsys.operation.user

import tech.testsys.domain.builder.api.judgmentOrderData
import tech.testsys.domain.contract.persistence.Page
import tech.testsys.domain.contract.persistence.Pagination
import tech.testsys.domain.contract.persistence.VerdictFilter
import tech.testsys.domain.contract.persistence.repository.JudgmentOrderRepository
import tech.testsys.domain.contract.persistence.repository.MultipleRoleUserRepository
import tech.testsys.domain.contract.persistence.repository.ParticipantRepository
import tech.testsys.domain.contract.persistence.repository.SubmissionRepository
import tech.testsys.domain.contract.persistence.repository.VerdictRepository
import tech.testsys.domain.model.task.GradingResult
import tech.testsys.domain.model.task.JudgmentOrder
import tech.testsys.domain.model.task.Score
import tech.testsys.domain.model.task.SubmissionId
import tech.testsys.domain.model.task.SubmissionKind
import tech.testsys.domain.model.task.SubmissionStatus
import tech.testsys.domain.model.task.Verdict
import tech.testsys.domain.model.user.Judge
import tech.testsys.domain.model.user.MultipleRoleUser
import tech.testsys.domain.model.user.MultipleRoleUserId
import tech.testsys.domain.model.user.SingleRoleUserId
import tech.testsys.domain.model.user.Student
import tech.testsys.operation.annotation.Feature
import tech.testsys.operation.annotation.InternalOperationsApi
import tech.testsys.operation.error.BlankJudgmentReasonError
import tech.testsys.operation.error.ChangeVerdictError
import tech.testsys.operation.error.MissedJudgeRoleError
import tech.testsys.operation.error.NegativeJudgmentScoreError
import tech.testsys.operation.error.OperationResult
import tech.testsys.operation.error.SubmissionAccessDeniedError
import tech.testsys.operation.error.SubmissionIsDeveloperSolutionTestError
import tech.testsys.operation.error.SubmissionNotExistsError
import tech.testsys.operation.error.SubmissionNotSuccessfullyGradedError
import tech.testsys.operation.error.ViewResultsError
import tech.testsys.operation.error.asSuccess
import tech.testsys.operation.error.ensure
import tech.testsys.operation.error.operation
import tech.testsys.operation.util.hasRole

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
) {

    /**
     * Returns a page of current successful verdicts available to [user], optionally filtered by [filter], with lazy file references.
     *
     * @since %CURRENT_VERSION%
     */
    @Feature("testsys.user.multi.judge.viewResults")
    fun viewResults(
        user: MultipleRoleUser,
        pagination: Pagination,
        filter: VerdictFilter = VerdictFilter(),
    ): OperationResult<Page<Verdict>, ViewResultsError> = operation<Page<Verdict>, ViewResultsError> {
        ensure(user.hasRole<Judge>(), MissedJudgeRoleError)
        val page = verdictRepository.findAvailableToJudge(pagination = pagination, filter = filter)
        return page.asSuccess()
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
}
