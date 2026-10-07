package tech.testsys.domain.model.entry

import tech.testsys.domain.model.DomainEntity
import tech.testsys.domain.model.DomainId
import tech.testsys.domain.model.LazyEntity
import tech.testsys.domain.model.group.Competition
import tech.testsys.domain.model.group.CompetitionId
import tech.testsys.domain.model.task.Contest
import tech.testsys.domain.model.task.ContestId
import tech.testsys.domain.model.user.SingleRoleUser
import tech.testsys.domain.model.user.SingleRoleUserId
import java.time.Instant

/**
 * Identifier of a [ParticipantContestEntry].
 *
 * @since %CURRENT_VERSION%
 */
@JvmInline
value class ParticipantContestEntryId(
    override val value: Long,
) : DomainId

/**
 * Data of a [ParticipantContestEntry]. Every field is fixed on creation; update is unsupported.
 *
 * @property participant the entry context; fixed on creation.
 * @property competition the entry context; fixed on creation.
 * @property contest the entry context; fixed on creation.
 * @property enteredAt the first entry moment; fixed on creation.
 * @since %CURRENT_VERSION%
 */
data class ParticipantContestEntryData(
    val participant: LazyEntity<SingleRoleUserId, SingleRoleUser>,
    val competition: LazyEntity<CompetitionId, Competition>,
    val contest: LazyEntity<ContestId, Contest>,
    val enteredAt: Instant,
)

/**
 * The first persisted entry into its context.
 *
 * @property data the immutable entry data.
 * @since %CURRENT_VERSION%
 */
class ParticipantContestEntry(
    id: ParticipantContestEntryId,
    createdAt: Instant,
    val data: ParticipantContestEntryData,
) : DomainEntity<ParticipantContestEntryId>(id, createdAt)
