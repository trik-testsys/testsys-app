package tech.testsys.web.app.service.participant

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tech.testsys.domain.model.task.ContestId
import tech.testsys.operation.error.getOrThrow
import tech.testsys.operation.user.ParticipantOperations
import tech.testsys.web.app.service.ContestVo
import tech.testsys.web.app.service.CurrentUser
import tech.testsys.web.app.service.toVo
import java.time.Instant

/**
 * Runs [ParticipantOperations] for the current user in one transaction per call.
 * A failed operation throws `OperationException`, which rolls the transaction back.
 *
 * @since %CURRENT_VERSION%
 */
@Service
@Transactional
class ParticipantService(private val operations: ParticipantOperations, private val currentUser: CurrentUser) {
    /**
     * Runs [ParticipantOperations.viewContests].
     *
     * @since %CURRENT_VERSION%
     */
    @Transactional(readOnly = true)
    fun viewContests(): List<Pair<Instant?, ContestVo>> = operations.viewContests(currentUser.singleRoleUser())
        .getOrThrow().map { (enteredAt, contest) -> enteredAt to contest.toVo() }

    /**
     * Runs [ParticipantOperations.enterContest].
     *
     * @since %CURRENT_VERSION%
     */
    fun enterContest(contestId: ContestId): ParticipantContestEntryVo =
        operations.enterContest(currentUser.singleRoleUser(), contestId).getOrThrow().toVo()
}
