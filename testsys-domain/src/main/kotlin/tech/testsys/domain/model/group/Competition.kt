package tech.testsys.domain.model.group

import tech.testsys.domain.model.DomainEntity
import tech.testsys.domain.model.DomainId
import tech.testsys.domain.model.LazyEntityList
import tech.testsys.domain.model.task.Contest
import tech.testsys.domain.model.task.ContestId
import tech.testsys.domain.model.user.MultipleRoleUser
import tech.testsys.domain.model.user.Participant
import tech.testsys.domain.model.user.UserId

@JvmInline
value class CompetitionId(
    override val value: Long
) : DomainId

data class CompetitionData(
    // TODO: Expiration?
    val owner: MultipleRoleUser,
    val participants: LazyEntityList<UserId, Participant>,
    val contests: LazyEntityList<ContestId, Contest>
)

class Competition(
    id: CompetitionId,
    val data: CompetitionData
) : DomainEntity<CompetitionId>(id)
