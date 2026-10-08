package tech.testsys.web.app.service.judge

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tech.testsys.domain.contract.persistence.Page
import tech.testsys.domain.contract.persistence.Pagination
import tech.testsys.domain.contract.persistence.VerdictFilter
import tech.testsys.domain.model.task.Score
import tech.testsys.domain.model.task.SubmissionId
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
    fun viewResults(pagination: Pagination, filter: VerdictFilter = VerdictFilter()): Page<VerdictVo> =
        operations.viewResults(currentUser.multipleRoleUser(), pagination, filter)
            .getOrThrow().map { verdict -> verdict.toVo() }

    /**
     * Runs [JudgeOperations.changeVerdict].
     *
     * @since %CURRENT_VERSION%
     */
    fun changeVerdict(submissionId: SubmissionId, score: Score, reason: String): JudgmentOrderVo =
        operations.changeVerdict(currentUser.multipleRoleUser(), submissionId, score, reason).getOrThrow().toVo()
}
