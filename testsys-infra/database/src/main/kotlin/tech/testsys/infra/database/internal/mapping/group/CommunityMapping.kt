package tech.testsys.infra.database.internal.mapping.group

import tech.testsys.domain.builder.api.community
import tech.testsys.domain.builder.data
import tech.testsys.domain.model.group.Community
import tech.testsys.domain.model.group.CommunityData
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.group.CommunityJpaEntity
import tech.testsys.infra.database.internal.mapping.EntityMapping
import tech.testsys.infra.database.internal.utils.populateFields

/**
 * Mapping between [Community] and [CommunityJpaEntity].
 *
 * @since %CURRENT_VERSION%
 */
@InternalDatabaseApi
object CommunityMapping : EntityMapping<Community, CommunityJpaEntity> {

    /**
     * Assembles a [Community] from [jpaEntity].
     *
     * @since %CURRENT_VERSION%
     */
    fun toDomain(jpaEntity: CommunityJpaEntity) = community {
        populateFields(jpaEntity)
        data {
            owner(jpaEntity.ownerId)
            name = jpaEntity.name
            description = jpaEntity.description
        }
    }

    /**
     * Creates a new [CommunityJpaEntity] row from [data].
     *
     * @since %CURRENT_VERSION%
     */
    fun toJpaEntity(data: CommunityData) = CommunityJpaEntity(
        name = data.name,
        description = data.description,
        ownerId = data.owner.id.value,
    )

    /**
     * Creates the [CommunityJpaEntity] row replacing [current] from [entity], keeping `createdAt` and `version`.
     *
     * @since %CURRENT_VERSION%
     */
    fun toJpaEntity(entity: Community, current: CommunityJpaEntity) = CommunityJpaEntity(
        name = entity.data.name,
        description = entity.data.description,
        ownerId = entity.data.owner.id.value,
        id = entity.id.value,
    ).also {
        it.createdAt = current.createdAt
        it.version = current.version
    }
}
