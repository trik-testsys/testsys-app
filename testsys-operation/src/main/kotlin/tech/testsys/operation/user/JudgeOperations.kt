package tech.testsys.operation.user

import tech.testsys.domain.contract.persistence.Page
import tech.testsys.domain.contract.persistence.Pagination
import tech.testsys.domain.contract.persistence.repository.VerdictRepository
import tech.testsys.domain.model.task.Verdict
import tech.testsys.domain.model.user.Judge
import tech.testsys.domain.model.user.MultipleRoleUser
import tech.testsys.domain.model.user.UserId
import tech.testsys.operation.annotation.Feature
import tech.testsys.operation.annotation.InternalOperationsApi
import tech.testsys.operation.error.MissedJudgeRoleError
import tech.testsys.operation.error.OperationResult
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
class JudgeOperations(private val verdictRepository: VerdictRepository) {

    /**
     * Returns a page of current successful verdicts available to [user], optionally filtered by [authorId], with lazy file references.
     *
     * @since %CURRENT_VERSION%
     */
    @Feature("testsys.user.multi.judge.viewResults")
    fun viewResults(
        user: MultipleRoleUser,
        pagination: Pagination,
        authorId: UserId? = null,
    ): OperationResult<Page<Verdict>, ViewResultsError> = operation<Page<Verdict>, ViewResultsError> {
        ensure(user.hasRole<Judge>(), MissedJudgeRoleError)
        val page = verdictRepository.findAvailableToJudge(pagination = pagination, authorId = authorId)
        return page.asSuccess()
    }
}
