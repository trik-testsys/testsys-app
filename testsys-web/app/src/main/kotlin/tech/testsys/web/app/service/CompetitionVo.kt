package tech.testsys.web.app.service

import tech.testsys.domain.model.group.Competition
import tech.testsys.domain.model.group.CompetitionId
import tech.testsys.domain.model.task.ContestId
import tech.testsys.domain.model.user.MultipleRoleUserId
import tech.testsys.domain.model.user.SingleRoleUserId
import java.time.Instant

/**
 * Competition data for pages, with links replaced by identifiers.
 *
 * @property id the identifier of the competition.
 * @property createdAt the moment the competition was created.
 * @property owner the identifier of the owner.
 * @property name the name of the competition.
 * @property description the description of the competition.
 * @property participants the identifiers of the participants.
 * @property contests the identifiers of the contests of the competition.
 * @since %CURRENT_VERSION%
 */
data class CompetitionVo(
    val id: CompetitionId,
    val createdAt: Instant,
    val owner: MultipleRoleUserId,
    val name: String,
    val description: String,
    val participants: List<SingleRoleUserId>,
    val contests: List<ContestId>,
)

internal fun Competition.toVo(): CompetitionVo = CompetitionVo(
    id = id,
    createdAt = createdAt,
    owner = data.owner.id,
    name = data.name,
    description = data.description,
    participants = data.participants.ids,
    contests = data.contests.ids,
)
