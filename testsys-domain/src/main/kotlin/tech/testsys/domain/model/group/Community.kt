package tech.testsys.domain.model.group

import tech.testsys.domain.model.DomainEntity
import tech.testsys.domain.model.DomainId
import tech.testsys.domain.model.LazyEntity
import tech.testsys.domain.model.user.MultipleRoleUser
import tech.testsys.domain.model.user.MultipleRoleUserId
import java.time.Instant

@JvmInline
value class CommunityId(
    override val value: Long
) : DomainId

data class CommunityData(
    val owner: LazyEntity<MultipleRoleUserId, MultipleRoleUser>,
)

class Community(
    id: CommunityId,
    createdAt: Instant,
    val data: CommunityData,
) : DomainEntity<CommunityId>(id, createdAt)
