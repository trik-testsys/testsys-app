package tech.testsys.web.app.service.judge

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tech.testsys.domain.contract.persistence.Page
import tech.testsys.domain.contract.persistence.Pagination
import tech.testsys.domain.contract.persistence.VerdictFilter
import tech.testsys.domain.model.task.FileData
import tech.testsys.domain.model.task.Score
import tech.testsys.domain.model.task.SubmissionId
import tech.testsys.domain.model.task.TestId
import tech.testsys.operation.error.getOrThrow
import tech.testsys.operation.user.JudgeOperations
import tech.testsys.web.app.service.CurrentUser
import tech.testsys.web.app.service.map

/**
 * Runs [JudgeOperations] for the current user in one transaction per call.
 * A failed operation throws `OperationException`, which rolls the transaction back.
 *
 * @since %CURRENT_VERSION%
 */
@Service
@Transactional
class JudgeService(private val operations: JudgeOperations, private val currentUser: CurrentUser) {
    /**
     * Runs [JudgeOperations.viewResults].
     *
     * @since %CURRENT_VERSION%
     */
    @Transactional(readOnly = true)
    fun viewResults(pagination: Pagination, filter: VerdictFilter = VerdictFilter()): Page<JudgeResultVo> =
        operations.viewResults(currentUser.multipleRoleUser(), pagination, filter)
            .getOrThrow().map { result -> result.toVo() }

    /**
     * Runs [JudgeOperations.changeVerdict].
     *
     * @since %CURRENT_VERSION%
     */
    fun changeVerdict(submissionId: SubmissionId, score: Score, reason: String): JudgmentOrderVo =
        operations.changeVerdict(currentUser.multipleRoleUser(), submissionId, score, reason).getOrThrow().toVo()

    /**
     * Runs [JudgeOperations.viewSolution].
     *
     * @since %CURRENT_VERSION%
     */
    @Transactional(readOnly = true)
    fun viewSolution(submissionId: SubmissionId): SubmissionDetailsVo =
        operations.viewSolution(currentUser.multipleRoleUser(), submissionId).getOrThrow().toVo()

    /**
     * Runs [JudgeOperations.downloadSolution].
     *
     * @since %CURRENT_VERSION%
     */
    @Transactional(readOnly = true)
    fun downloadSolution(submissionId: SubmissionId): FileData =
        operations.downloadSolution(currentUser.multipleRoleUser(), submissionId).getOrThrow()

    /**
     * Runs [JudgeOperations.downloadLogs].
     *
     * @since %CURRENT_VERSION%
     */
    @Transactional(readOnly = true)
    fun downloadLogs(submissionId: SubmissionId, testId: TestId): FileData =
        operations.downloadLogs(currentUser.multipleRoleUser(), submissionId, testId).getOrThrow()

    /**
     * Runs [JudgeOperations.downloadRecording].
     *
     * @since %CURRENT_VERSION%
     */
    @Transactional(readOnly = true)
    fun downloadRecording(submissionId: SubmissionId, testId: TestId): FileData =
        operations.downloadRecording(currentUser.multipleRoleUser(), submissionId, testId).getOrThrow()
}
