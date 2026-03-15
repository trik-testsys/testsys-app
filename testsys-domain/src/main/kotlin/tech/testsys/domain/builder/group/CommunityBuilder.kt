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

/**
 * Builder for constructing [CommunityData].
 *
 * @since %CURRENT_VERSION%
 */
class CommunityDataBuilder : Builder<CommunityData> {

    /**
     * The owner of the community.
     *
     * @since %CURRENT_VERSION%
     */
    var owner: MultipleRoleUser? = null

    /**
     * The list of community member IDs.
     *
     * @since %CURRENT_VERSION%
     */
    var members = mutableListOf<UserId>()

    /**
     * Configures the [owner] using a DSL block on [MultipleRoleUserBuilder].
     *
     * @param builder the configuration block for the owner.
     * @since %CURRENT_VERSION%
     */
    inline fun owner(builder: MultipleRoleUserBuilder.() -> Unit) {
        owner = MultipleRoleUserBuilder().apply(builder).build()
    }

    /**
     * Sets the [members] list from raw ID values.
     *
     * @param members the raw user IDs.
     * @since %CURRENT_VERSION%
     */
    fun members(members: Iterable<Long>) {
        this.members = members.map { UserId(it) }.toMutableList()
    }

    /**
     * Builds the [CommunityData] instance.
     *
     * @return the constructed [CommunityData].
     * @throws IllegalArgumentException if [owner] is not set.
     * @since %CURRENT_VERSION%
     */
    override fun build(): CommunityData {
        val owner = requireField(owner, "owner")

        return CommunityData(
            owner = owner,
            members = members.lazify(),
        )
    }
}

/**
 * Builder for constructing [Community] domain entities.
 *
 * @since %CURRENT_VERSION%
 */
class CommunityBuilder : DomainEntityWithDataBuilder<Community, CommunityData, CommunityDataBuilder>() {

    override fun dataBuilder() = CommunityDataBuilder()

    /**
     * Builds the [Community] instance.
     *
     * @return the constructed [Community].
     * @throws IllegalArgumentException if any required field is not set.
     * @since %CURRENT_VERSION%
     */
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

/**
 * DSL entry point for building [CommunityData].
 *
 * @param builder the configuration block applied to [CommunityDataBuilder].
 * @return the constructed [CommunityData].
 * @since %CURRENT_VERSION%
 */
inline fun buildCommunityData(builder: CommunityDataBuilder.() -> Unit) = CommunityDataBuilder().apply(builder).build()

/**
 * DSL entry point for building a [Community].
 *
 * @param builder the configuration block applied to [CommunityBuilder].
 * @return the constructed [Community].
 * @since %CURRENT_VERSION%
 */
inline fun buildCommunity(builder: CommunityBuilder.() -> Unit) = CommunityBuilder().apply(builder).build()
