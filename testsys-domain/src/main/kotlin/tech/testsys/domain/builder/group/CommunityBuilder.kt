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
 * Builder of [CommunityData]. Required: [owner], [name], [description].
 *
 * @property owner the id of the owning administrator, or `null` if not set yet.
 * @property name the name of the community, or `null` if not set yet.
 * @property description the description of the community, or `null` if not set yet.
 * @since %CURRENT_VERSION%
 */
class CommunityDataBuilder : Builder<CommunityData> {

    var owner: MultipleRoleUserId? = null

    var name: String? = null

    var description: String? = null

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

        return CommunityData(
            owner = owner.lazify(),
            name = name,
            description = description,
        )
    }
}

/**
 * Builder of [Community] entities. Required: [id], [createdAt], [version], [data].
 *
 * @since %CURRENT_VERSION%
 */
class CommunityBuilder : DomainEntityWithDataBuilder<Community, CommunityData, CommunityDataBuilder>() {

    override fun dataBuilder() = CommunityDataBuilder()

    override fun build(): Community {
        val id = requireField(id) { ::id }
        val createdAt = requireField(createdAt) { ::createdAt }
        val version = requireField(version) { ::version }
        val data = requireField(data) { ::data }

        return Community(
            id = CommunityId(id),
            createdAt = createdAt,
            version = version,
            data = data,
        )
    }
}
