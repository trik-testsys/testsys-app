package tech.testsys.infra.database.internal.mapping.group

import tech.testsys.domain.builder.api.community
import tech.testsys.domain.builder.data
import tech.testsys.domain.model.group.Community
import tech.testsys.domain.model.group.CommunityData
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.group.CommunityJpaEntity
import tech.testsys.infra.database.internal.mapping.EntityMapping
import tech.testsys.infra.database.internal.utils.populateFields
import tech.testsys.infra.database.internal.utils.requireVersion

/**
 * Mapping between [Community] and [CommunityJpaEntity].
 *
 * @since 1.0.0-beta.1
 */
@InternalDatabaseApi
object CommunityMapping : EntityMapping<Community, CommunityJpaEntity> {

    /**
     * Assembles a [Community] from [jpaEntity].
     *
     * @since 1.0.0-beta.1
     */
    fun toDomain(jpaEntity: CommunityJpaEntity) = community {
        populateFields(jpaEntity)
        data {
            owner(jpaEntity.ownerId)
            name = jpaEntity.name
            description = jpaEntity.description
            managerInvite(jpaEntity.managerInviteId)
            developerInvite(jpaEntity.developerInviteId)
        }
    }

    /**
     * Creates a new [CommunityJpaEntity] row from [data], optionally using the supplied [id].
     *
     * @since 1.0.0-beta.1
     */
    fun toJpaEntity(data: CommunityData, id: Long? = null) = CommunityJpaEntity(
        name = data.name,
        description = data.description,
        ownerId = data.owner.id.value,
        managerInviteId = data.managerInvite.id.value,
        developerInviteId = data.developerInvite.id.value,
        id = id,
    )

    /**
     * Creates the [CommunityJpaEntity] row replacing [current] from [entity], keeping `ownerId`, the invite ids,
     * `createdAt` and `version`.
     *
     * @since 1.0.0-beta.1
     */
    fun toJpaEntity(entity: Community, current: CommunityJpaEntity) = CommunityJpaEntity(
        name = entity.data.name,
        description = entity.data.description,
        ownerId = current.ownerId,
        managerInviteId = current.managerInviteId,
        developerInviteId = current.developerInviteId,
        id = entity.id.value,
    ).also {
        it.createdAt = current.createdAt
        it.version = entity.requireVersion()
    }
}
