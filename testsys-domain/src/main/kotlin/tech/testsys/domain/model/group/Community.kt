package tech.testsys.domain.model.group

import tech.testsys.domain.model.DomainEntity
import tech.testsys.domain.model.DomainId
import tech.testsys.domain.model.LazyEntityList
import tech.testsys.domain.model.user.MultipleRoleUser
import tech.testsys.domain.model.user.User
import tech.testsys.domain.model.user.UserId

@JvmInline
value class CommunityId(
    override val value: Long
) : DomainId

data class CommunityData(
    val owner: MultipleRoleUser,
    val members: LazyEntityList<UserId, User>
)

class Community(
    id: CommunityId,
    val data: CommunityData
) : DomainEntity<CommunityId>(id)
