package tech.testsys.domain.builder.group

import tech.testsys.domain.builder.Builder
import tech.testsys.domain.builder.DomainEntityWithDataBuilder
import tech.testsys.domain.builder.util.lazify
import tech.testsys.domain.builder.util.requireField
import tech.testsys.domain.model.group.Community
import tech.testsys.domain.model.group.CommunityData
import tech.testsys.domain.model.group.CommunityId
import tech.testsys.domain.model.user.MultipleRoleUserId

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
    var owner: MultipleRoleUserId? = null

    /**
     * Sets the [owner] from a raw ID value.
     *
     * @param owner the raw owner ID.
     * @since %CURRENT_VERSION%
     */
    fun owner(owner: Long) {
        this.owner = MultipleRoleUserId(owner)
    }

    /**
     * Builds the [CommunityData] instance.
     *
     * @return the constructed [CommunityData].
     * @throws IllegalArgumentException if [owner] is not set.
     * @since %CURRENT_VERSION%
     */
    override fun build(): CommunityData {
        val owner = requireField(owner) { ::owner }

        return CommunityData(
            owner = owner.lazify(),
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
        val id = requireField(id) { ::id }
        val createdAt = requireField(createdAt) { ::createdAt }
        val data = requireField(data) { ::data }

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
