package tech.testsys.domain.model.group

import tech.testsys.domain.model.DomainEntity
import tech.testsys.domain.model.DomainId
import tech.testsys.domain.model.EntityVersion
import tech.testsys.domain.model.LazyEntity
import tech.testsys.domain.model.LazyEntityList
import tech.testsys.domain.model.task.Contest
import tech.testsys.domain.model.task.ContestId
import tech.testsys.domain.model.user.MultipleRoleUser
import tech.testsys.domain.model.user.MultipleRoleUserId
import tech.testsys.domain.model.user.Participant
import tech.testsys.domain.model.user.SingleRoleUserId
import java.time.Instant

/**
 * Identifier of a [Competition].
 *
 * @since %CURRENT_VERSION%
 */
@JvmInline
value class CompetitionId(
    override val value: Long,
) : DomainId

/**
 * Data of a [Competition].
 *
 * @property owner the manager who owns the competition.
 * @property name the name of the competition.
 * @property description the description of the competition.
 * @property participants the participants registered in the competition.
 * @property contests the contests held within the competition.
 * @since %CURRENT_VERSION%
 */
data class CompetitionData(
    val owner: LazyEntity<MultipleRoleUserId, MultipleRoleUser>,
    val name: String,
    val description: String,
    val participants: LazyEntityList<SingleRoleUserId, Participant>,
    val contests: LazyEntityList<ContestId, Contest>,
)

/**
 * A set of participants competing in contests, owned by a manager. Each participant belongs to exactly one
 * competition.
 *
 * @property data the data of the competition.
 * @since %CURRENT_VERSION%
 */
class Competition(
    id: CompetitionId,
    createdAt: Instant,
    version: EntityVersion,
    val data: CompetitionData,
) : DomainEntity<CompetitionId>(id, createdAt, version)
