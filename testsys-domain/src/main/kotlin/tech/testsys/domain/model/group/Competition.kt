package tech.testsys.domain.model.group

import tech.testsys.domain.model.DomainEntity
import tech.testsys.domain.model.DomainId
import tech.testsys.domain.model.LazyEntity
import tech.testsys.domain.model.LazyEntityList
import tech.testsys.domain.model.task.Contest
import tech.testsys.domain.model.task.ContestId
import tech.testsys.domain.model.user.MultipleRoleUser
import tech.testsys.domain.model.user.MultipleRoleUserId
import tech.testsys.domain.model.user.Participant
import tech.testsys.domain.model.user.SingleRoleUserId
import java.time.Instant

@JvmInline
value class CompetitionId(
    override val value: Long
) : DomainId

data class CompetitionData(
    val owner: LazyEntity<MultipleRoleUserId, MultipleRoleUser>,
    val participants: LazyEntityList<SingleRoleUserId, Participant>,
    val contests: LazyEntityList<ContestId, Contest>
)

class Competition(
    id: CompetitionId,
    createdAt: Instant,
    val data: CompetitionData,
) : DomainEntity<CompetitionId>(id, createdAt)
