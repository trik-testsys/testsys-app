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
 * @since 1.0.0-beta.1
 */
@Service
@Transactional(readOnly = true)
class ObserverService(private val operations: ObserverOperations, private val currentUser: CurrentUser) {

    /**
     * Returns current competition and assigned contest pairs for the observer.
     *
     * @since 1.0.0-beta.1
     */
    fun viewContests(pagination: Pagination, filter: ObserverContestFilter = ObserverContestFilter()): Page<ObserverContestVo> =
        operations.viewContests(currentUser.singleRoleUser(), pagination, filter).getOrThrow().map { pair ->
            ObserverContestVo(pair.competition.toVo(), pair.contest.toVo())
        }

    /**
     * Generates results for all current competitions containing assigned contests.
     *
     * @since 1.0.0-beta.1
     */
    fun downloadResult(): ObserverResult = operations.downloadResult(currentUser.singleRoleUser()).getOrThrow()
}

/**
 * One competition and contest association shown in the observer cabinet.
 *
 * @property competition the competition containing the contest.
 * @property contest the assigned contest.
 * @since 1.0.0-beta.1
 */
data class ObserverContestVo(val competition: CompetitionVo, val contest: ContestVo)
