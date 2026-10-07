// Placeholder for the operations class listed in testsys-operation/README.md, kept until its first feature.
@file:Suppress("EmptyKotlinFile")

package tech.testsys.operation.user

import tech.testsys.domain.builder.api.participantContestEntryData
import tech.testsys.domain.contract.persistence.repository.CompetitionRepository
import tech.testsys.domain.contract.persistence.repository.ContestRepository
import tech.testsys.domain.contract.persistence.repository.ParticipantContestEntryRepository
import tech.testsys.domain.model.entry.ParticipantContestEntry
import tech.testsys.domain.model.task.Contest
import tech.testsys.domain.model.task.ContestId
import tech.testsys.domain.model.user.Participant
import tech.testsys.domain.model.user.SingleRoleUser
import tech.testsys.operation.annotation.Feature
import tech.testsys.operation.annotation.InternalOperationsApi
import tech.testsys.operation.error.CompetitionNotExistsError
import tech.testsys.operation.error.ContestAccessDeniedError
import tech.testsys.operation.error.ContestEndedError
import tech.testsys.operation.error.ContestNotExistsError
import tech.testsys.operation.error.ContestNotStartedError
import tech.testsys.operation.error.EnterParticipantContestError
import tech.testsys.operation.error.MissedParticipantRoleError
import tech.testsys.operation.error.OperationResult
import tech.testsys.operation.error.ViewParticipantContestsError
import tech.testsys.operation.error.asSuccess
import tech.testsys.operation.error.ensure
import tech.testsys.operation.error.operation
import java.time.Clock
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * Operations performed by participants in their competition.
 *
 * @since %CURRENT_VERSION%
 */
@OptIn(InternalOperationsApi::class)
class ParticipantOperations(
    private val competitionRepository: CompetitionRepository,
    private val contestRepository: ContestRepository,
    private val contestEntryRepository: ParticipantContestEntryRepository,
    private val clock: Clock,
) {

    /**
     * Returns pairs of first entry time and contest for [user], including future and completed contests.
     * The time is null until the user enters that contest in their competition; listing does not create entries.
     *
     * @since %CURRENT_VERSION%
     */
    @Feature("testsys.user.single.participant.viewContests")
    fun viewContests(user: SingleRoleUser): OperationResult<List<Pair<Instant?, Contest>>, ViewParticipantContestsError> =
        operation<List<Pair<Instant?, Contest>>, ViewParticipantContestsError> {
            ensure(user is Participant, MissedParticipantRoleError)
            val competitionId = user.data.competition.id
            val competition = competitionRepository.findById(competitionId)
            ensure(competition != null) { CompetitionNotExistsError(competitionId) }
            val contests = competition.data.contests.load(contestRepository)
            val entries = contestEntryRepository.findByContests(
                participantId = user.id,
                competitionId = competitionId,
                contestIds = contests.map { it.id },
            ).associateBy { it.data.contest.id }
            return contests.map { contest -> entries[contest.id]?.data?.enteredAt to contest }.asSuccess()
        }

    /**
     * Returns the first entry of [user] into [contestId] in their competition.
     * A new entry requires the current start-inclusive, end-exclusive interval; a repeat keeps its first time.
     *
     * @since %CURRENT_VERSION%
     */
    @Feature("testsys.user.single.participant.enterContest")
    fun enterContest(user: SingleRoleUser, contestId: ContestId): OperationResult<ParticipantContestEntry, EnterParticipantContestError> =
        operation<ParticipantContestEntry, EnterParticipantContestError> {
            ensure(user is Participant, MissedParticipantRoleError)
            val competitionId = user.data.competition.id
            val competition = competitionRepository.findById(competitionId)
            ensure(competition != null) { CompetitionNotExistsError(competitionId) }
            val contest = contestRepository.findById(contestId)
            ensure(contest != null) { ContestNotExistsError(contestId) }
            ensure(contestId in competition.data.contests.ids) { ContestAccessDeniedError(contestId) }
            val existing = contestEntryRepository.findByContext(
                participantId = user.id,
                competitionId = competitionId,
                contestId = contestId,
            )
            if (existing != null) return existing.asSuccess()
            val now = clock.instant()
            val startsAt = contest.data.startsAt
            val endsAt = contest.data.endsAt
            if ((startsAt != null && now.isBefore(startsAt)) || (endsAt != null && !now.isBefore(endsAt))) {
                val concurrentEntry = contestEntryRepository.findByContext(
                    participantId = user.id,
                    competitionId = competitionId,
                    contestId = contestId,
                )
                if (concurrentEntry != null) return concurrentEntry.asSuccess()
            }
            ensure(startsAt == null || !now.isBefore(startsAt)) { ContestNotStartedError(contestId, requireNotNull(startsAt)) }
            ensure(endsAt == null || now.isBefore(endsAt)) { ContestEndedError(contestId, requireNotNull(endsAt)) }
            val data = participantContestEntryData {
                participant = user.id
                this.competition = competitionId
                this.contest = contestId
                enteredAt = now.truncatedTo(ChronoUnit.MICROS)
            }
            return contestEntryRepository.findOrCreate(data).asSuccess()
        }
}
