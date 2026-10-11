package tech.testsys.web.app.service.observer

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tech.testsys.domain.contract.persistence.ObserverContestFilter
import tech.testsys.domain.contract.persistence.Page
import tech.testsys.domain.contract.persistence.Pagination
import tech.testsys.operation.error.getOrThrow
import tech.testsys.operation.user.ObserverOperations
import tech.testsys.operation.user.ObserverResult
import tech.testsys.web.app.service.CompetitionVo
import tech.testsys.web.app.service.ContestVo
import tech.testsys.web.app.service.CurrentUser
import tech.testsys.web.app.service.map
import tech.testsys.web.app.service.toVo

/**
 * Runs observer operations for the current user in read-only transactions.
 *
 * @since %CURRENT_VERSION%
 */
@Service
@Transactional(readOnly = true)
class ObserverService(private val operations: ObserverOperations, private val currentUser: CurrentUser) {

    /**
     * Returns current competition and assigned contest pairs for the observer.
     *
     * @since %CURRENT_VERSION%
     */
    fun viewContests(pagination: Pagination, filter: ObserverContestFilter = ObserverContestFilter()): Page<ObserverContestVo> =
        operations.viewContests(currentUser.singleRoleUser(), pagination, filter).getOrThrow().map { pair ->
            ObserverContestVo(pair.competition.toVo(), pair.contest.toVo())
        }

    /**
     * Generates results for all current competitions containing assigned contests.
     *
     * @since %CURRENT_VERSION%
     */
    fun downloadResult(): ObserverResult = operations.downloadResult(currentUser.singleRoleUser()).getOrThrow()
}

/**
 * One competition and contest association shown in the observer cabinet.
 *
 * @property competition the competition containing the contest.
 * @property contest the assigned contest.
 * @since %CURRENT_VERSION%
 */
data class ObserverContestVo(val competition: CompetitionVo, val contest: ContestVo)
