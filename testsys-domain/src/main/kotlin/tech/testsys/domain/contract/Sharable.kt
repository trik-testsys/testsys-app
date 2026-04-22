package tech.testsys.domain.contract

import tech.testsys.domain.model.Sharable
import tech.testsys.domain.model.group.CommunityId

interface ShareService<E : Sharable> {

    fun shareWith(entity: E, community: CommunityId): E
    fun unshareFrom(entity: E, community: CommunityId): E
    fun isSharedTo(entity: E, community: CommunityId): Boolean = entity.sharedTo.ids.contains(community)
}
