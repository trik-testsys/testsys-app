package tech.testsys.web.app.service.participant

import tech.testsys.domain.model.entry.ParticipantContestEntry
import tech.testsys.domain.model.entry.ParticipantContestEntryId
import tech.testsys.domain.model.group.CompetitionId
import tech.testsys.domain.model.task.ContestId
import tech.testsys.domain.model.user.SingleRoleUserId
import java.time.Instant

/**
 * First entry of a participant into a contest for pages, with links replaced by identifiers.
 *
 * @property id the identifier of the entry.
 * @property createdAt the moment the entry was created.
 * @property participant the identifier of the participant.
 * @property competition the identifier of the competition.
 * @property contest the identifier of the contest.
 * @property enteredAt the moment of the first entry.
 * @since %CURRENT_VERSION%
 */
data class ParticipantContestEntryVo(
    val id: ParticipantContestEntryId,
    val createdAt: Instant,
    val participant: SingleRoleUserId,
    val competition: CompetitionId,
    val contest: ContestId,
    val enteredAt: Instant,
)

internal fun ParticipantContestEntry.toVo(): ParticipantContestEntryVo = ParticipantContestEntryVo(
    id = id,
    createdAt = createdAt,
    participant = data.participant.id,
    competition = data.competition.id,
    contest = data.contest.id,
    enteredAt = data.enteredAt,
)
