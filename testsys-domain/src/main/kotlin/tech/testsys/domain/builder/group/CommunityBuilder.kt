package tech.testsys.domain.builder.group

import tech.testsys.domain.builder.Builder
import tech.testsys.domain.builder.DomainEntityWithDataBuilder
import tech.testsys.domain.builder.util.lazify
import tech.testsys.domain.builder.user.MultipleRoleUserBuilder
import tech.testsys.domain.builder.util.requireField
import tech.testsys.domain.model.group.Community
import tech.testsys.domain.model.group.CommunityData
import tech.testsys.domain.model.group.CommunityId
import tech.testsys.domain.model.user.MultipleRoleUser
import tech.testsys.domain.model.user.UserId

class CommunityDataBuilder : Builder<CommunityData> {

    var owner: MultipleRoleUser? = null
    var members = mutableListOf<UserId>()

    inline fun owner(builder: MultipleRoleUserBuilder.() -> Unit) {
        owner = MultipleRoleUserBuilder().apply(builder).build()
    }

    fun members(members: Iterable<Long>) {
        this.members = members.map { UserId(it) }.toMutableList()
    }

    override fun build(): CommunityData {
        val owner = requireField(owner, "owner")

        return CommunityData(
            owner = owner,
            members = members.lazify(),
        )
    }
}

class CommunityBuilder : DomainEntityWithDataBuilder<Community, CommunityData, CommunityDataBuilder>() {

    override fun dataBuilder() = CommunityDataBuilder()

    override fun build(): Community {
        val id = requireField(id, "id")
        val createdAt = requireField(createdAt, "createdAt")
        val data = requireField(data, "data")

        return Community(
            id = CommunityId(id),
            createdAt = createdAt,
            data = data,
        )
    }
}

inline fun buildCommunityData(builder: CommunityDataBuilder.() -> Unit) = CommunityDataBuilder().apply(builder).build()

inline fun buildCommunity(builder: CommunityBuilder.() -> Unit) = CommunityBuilder().apply(builder).build()
