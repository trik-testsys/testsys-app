package tech.testsys.domain.builder.group

import tech.testsys.domain.builder.Builder
import tech.testsys.domain.builder.DomainEntityWithDataBuilder
import tech.testsys.domain.builder.util.applyVersion
import tech.testsys.domain.builder.util.lazify
import tech.testsys.domain.builder.util.requireField
import tech.testsys.domain.model.group.Community
import tech.testsys.domain.model.group.CommunityData
import tech.testsys.domain.model.group.CommunityId
import tech.testsys.domain.model.group.CommunityInviteId
import tech.testsys.domain.model.user.MultipleRoleUserId

/**
 * Builder of [CommunityData]. Required: [owner], [name], [description], [managerInvite], [developerInvite].
 *
 * @property owner the id of the owning administrator, or `null` if not set yet.
 * @property name the name of the community, or `null` if not set yet.
 * @property description the description of the community, or `null` if not set yet.
 * @property managerInvite the id of the manager-role invite code, or `null` if not set yet.
 * @property developerInvite the id of the developer-role invite code, or `null` if not set yet.
 * @since %CURRENT_VERSION%
 */
class CommunityDataBuilder : Builder<CommunityData> {

    var owner: MultipleRoleUserId? = null

    var name: String? = null

    var description: String? = null

    var managerInvite: CommunityInviteId? = null

    var developerInvite: CommunityInviteId? = null

    /**
     * Sets [managerInvite] from a raw id.
     *
     * @since %CURRENT_VERSION%
     */
    fun managerInvite(managerInvite: Long) {
        this.managerInvite = CommunityInviteId(managerInvite)
    }

    /**
     * Sets [developerInvite] from a raw id.
     *
     * @since %CURRENT_VERSION%
     */
    fun developerInvite(developerInvite: Long) {
        this.developerInvite = CommunityInviteId(developerInvite)
    }

    /**
     * Sets [owner] from a raw id.
     *
     * @since %CURRENT_VERSION%
     */
    fun owner(owner: Long) {
        this.owner = MultipleRoleUserId(owner)
    }

    override fun build(): CommunityData {
        val owner = requireField(owner) { ::owner }
        val name = requireField(name) { ::name }
        val description = requireField(description) { ::description }
        val managerInvite = requireField(managerInvite) { ::managerInvite }
        val developerInvite = requireField(developerInvite) { ::developerInvite }

        return CommunityData(
            owner = owner.lazify(),
            name = name,
            description = description,
            managerInvite = managerInvite.lazify(),
            developerInvite = developerInvite.lazify(),
        )
    }
}

/**
 * Builder of [Community] entities. Required: [id], [createdAt], [data].
 *
 * @since %CURRENT_VERSION%
 */
class CommunityBuilder : DomainEntityWithDataBuilder<Community, CommunityData, CommunityDataBuilder>() {

    override fun dataBuilder() = CommunityDataBuilder()

    override fun build(): Community {
        val id = requireField(id) { ::id }
        val createdAt = requireField(createdAt) { ::createdAt }
        val data = requireField(data) { ::data }

        return Community(
            id = CommunityId(id),
            createdAt = createdAt,
            data = data,
        ).applyVersion(version)
    }
}
