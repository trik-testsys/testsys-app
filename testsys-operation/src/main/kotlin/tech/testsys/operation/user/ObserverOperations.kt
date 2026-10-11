package tech.testsys.operation.user

import tech.testsys.domain.contract.persistence.ObserverContestFilter
import tech.testsys.domain.contract.persistence.Page
import tech.testsys.domain.contract.persistence.Pagination
import tech.testsys.domain.contract.persistence.repository.CompetitionRepository
import tech.testsys.domain.contract.persistence.repository.ContestRepository
import tech.testsys.domain.contract.persistence.repository.ObserverContest
import tech.testsys.domain.contract.persistence.repository.ParticipantRepository
import tech.testsys.domain.contract.persistence.repository.SubmissionRepository
import tech.testsys.domain.contract.persistence.repository.TaskRepository
import tech.testsys.domain.model.group.Competition
import tech.testsys.domain.model.task.ContestId
import tech.testsys.domain.model.task.TaskId
import tech.testsys.domain.model.user.Observer
import tech.testsys.domain.model.user.SingleRoleUser
import tech.testsys.domain.model.user.UserId
import tech.testsys.operation.annotation.Feature
import tech.testsys.operation.annotation.InternalOperationsApi
import tech.testsys.operation.error.DownloadObserverResultError
import tech.testsys.operation.error.MissedObserverRoleError
import tech.testsys.operation.error.OperationResult
import tech.testsys.operation.error.ViewObserverContestsError
import tech.testsys.operation.error.asSuccess
import tech.testsys.operation.error.ensure
import tech.testsys.operation.error.operation

/**
 * Operations performed by observers for their assigned contests and competitions containing them.
 *
 * @since %CURRENT_VERSION%
 */
@OptIn(InternalOperationsApi::class)
class ObserverOperations(
    private val contestRepository: ContestRepository,
    private val competitionRepository: CompetitionRepository,
    private val participantRepository: ParticipantRepository,
    private val taskRepository: TaskRepository,
    private val submissionRepository: SubmissionRepository,
) {

    /**
     * Returns current competition and assigned contest pairs for [user], applying [filter] and [pagination] without recording entry.
     * Future and completed contests retain their time limits; technical storage exceptions propagate.
     *
     * @since %CURRENT_VERSION%
     */
    @Feature("testsys.user.single.observer.viewContests")
    fun viewContests(
        user: SingleRoleUser,
        pagination: Pagination,
        filter: ObserverContestFilter = ObserverContestFilter(),
    ): OperationResult<Page<ObserverContest>, ViewObserverContestsError> = operation<Page<ObserverContest>, ViewObserverContestsError> {
        ensure(user is Observer, MissedObserverRoleError)
        return competitionRepository.findAvailableToObserver(
            contestIds = user.data.contests.ids.toSet(),
            pagination = pagination,
            filter = filter,
        ).asSuccess()
    }

    /**
     * Returns results for all competitions containing contests assigned to [user], without changing stored data.
     * Rows contain each current participant once and one best-score column per task of each observed contest.
     * Technical storage exceptions propagate.
     *
     * @since %CURRENT_VERSION%
     */
    @Feature("testsys.user.single.observer.downloadResult")
    fun downloadResult(user: SingleRoleUser): OperationResult<ObserverResult, DownloadObserverResultError> =
        operation<ObserverResult, DownloadObserverResultError> {
            ensure(user is Observer, MissedObserverRoleError)
            val assigned = user.data.contests.ids.toSet()
            val competitions = competitionRepository.findByContestIds(assigned).sortedBy { it.id.value }
            return collectResult(competitions, assigned).asSuccess()
        }

    /** Collects results of [competitions] restricted to [assigned] contests. */
    private fun collectResult(competitions: List<Competition>, assigned: Set<ContestId>): ObserverResult {
        val observedContests = competitions.flatMap { competition -> competition.data.contests.ids }
            .filter { contestId -> contestId in assigned }
            .distinct()
            .sortedBy { it.value }
        val contests = contestRepository.findByIds(observedContests).associateBy { it.id }
        val participantIds = competitions.flatMap { it.data.participants.ids }.distinct()
        val participants = participantRepository.findByIds(participantIds).associateBy { it.id }
        val taskIds = observedContests.flatMap { id -> contests[id]?.data?.tasks?.ids.orEmpty() }.distinct()
        val tasks = taskRepository.findByIds(taskIds).associateBy { it.id }
        val columns = observedContests.flatMap { contestId ->
            val contest = contests[contestId] ?: return@flatMap emptyList()
            contest.data.tasks.ids.distinct().sortedBy { it.value }.mapNotNull { taskId ->
                tasks[taskId]?.let { task -> contest to task }
            }
        }
        val authorIdsByContest = observedContests.associateWith { contestId ->
            competitions.filter { contestId in it.data.contests.ids }
                .flatMap { it.data.participants.ids }
                .toSet()
                .intersect(participants.keys)
        }
        val results = observedContests.associateWith { contestId ->
            val ids = contests[contestId]?.data?.tasks?.ids?.toSet().orEmpty()
            submissionRepository.findContestResults(contestId, authorIdsByContest.getValue(contestId), ids)
                .associateBy { result -> result.authorId to result.taskId }
        }
        val resultColumns = columns.map { (contest, task) ->
            ObserverResult.Column(
                contestName = contest.data.name,
                contestId = contest.id,
                taskName = task.data.name,
                taskId = task.id,
            )
        }
        val rows = participants.keys.sortedBy { it.value }.map { id ->
            ObserverResult.Row(
                participantId = id,
                scores = columns.map { (contest, task) ->
                    if (id in authorIdsByContest.getValue(contest.id)) {
                        results[contest.id]?.get(id to task.id)?.bestScore?.value
                    } else {
                        null
                    }
                },
            )
        }
        return ObserverResult(columns = resultColumns, rows = rows)
    }
}

/** Results of an observer's accessible contests across all current competitions.
 *
 * @property columns the task columns in contest and task identifier order.
 * @property rows the unique participants and their scores in participant identifier order.
 * @since %CURRENT_VERSION%
 */
data class ObserverResult(
    val columns: List<Column>,
    val rows: List<Row>,
) {
    /** One task column of a contest.
     *
     * @property contestName the contest name.
     * @property contestId the contest identifier.
     * @property taskName the task name.
     * @property taskId the task identifier.
     * @since %CURRENT_VERSION%
     */
    data class Column(val contestName: String, val contestId: ContestId, val taskName: String, val taskId: TaskId)

    /** One participant and its scores in [ObserverResult.columns] order.
     *
     * @property participantId the participant identifier.
     * @property scores the best scores, or `null` without a successful verdict or eligible contest membership.
     * @since %CURRENT_VERSION%
     */
    data class Row(
        val participantId: UserId,
        val scores: List<Int?>,
    )
}
