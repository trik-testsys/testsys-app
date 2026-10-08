package tech.testsys.infra.database.internal.mapping.group

import tech.testsys.domain.builder.api.developerCommunityInvite
import tech.testsys.domain.builder.api.managerCommunityInvite
import tech.testsys.domain.builder.data
import tech.testsys.domain.builder.group.CommunityInviteBuilder
import tech.testsys.domain.model.group.CommunityInvite
import tech.testsys.domain.model.group.CommunityInviteData
import tech.testsys.domain.model.group.InviteCodeHash
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.group.CommunityInviteJpaEntity
import tech.testsys.infra.database.internal.jpa.entity.group.CommunityInviteRoleJpaEnum
import tech.testsys.infra.database.internal.mapping.EntityMapping
import tech.testsys.infra.database.internal.utils.populateFields
import tech.testsys.infra.database.internal.utils.requireVersion
import tech.testsys.infra.database.internal.utils.toDomain
import tech.testsys.infra.database.internal.utils.toJpaEnum

/**
 * Mapping between [CommunityInvite] and [CommunityInviteJpaEntity]; the `role` column selects the variant.
 *
 * @since %CURRENT_VERSION%
 */
@InternalDatabaseApi
object CommunityInviteMapping : EntityMapping<CommunityInvite, CommunityInviteJpaEntity> {

    /**
     * Assembles the [CommunityInvite] variant selected by the role of [jpaEntity] without hashing its stored code.
     *
     * @since %CURRENT_VERSION%
     */
    fun toDomain(jpaEntity: CommunityInviteJpaEntity): CommunityInvite = when (jpaEntity.role) {
        CommunityInviteRoleJpaEnum.MANAGER -> toManager(jpaEntity)
        CommunityInviteRoleJpaEnum.DEVELOPER -> toDeveloper(jpaEntity)
    }

    /**
     * Assembles a [CommunityInvite.Manager] from [jpaEntity] of the manager role.
     *
     * @since %CURRENT_VERSION%
     */
    fun toManager(jpaEntity: CommunityInviteJpaEntity): CommunityInvite.Manager {
        require(jpaEntity.role == CommunityInviteRoleJpaEnum.MANAGER) {
            "Community invite id=${jpaEntity.id} has role ${jpaEntity.role}, expected MANAGER"
        }
        return managerCommunityInvite { populate(jpaEntity) }
    }

    /**
     * Assembles a [CommunityInvite.Developer] from [jpaEntity] of the developer role.
     *
     * @since %CURRENT_VERSION%
     */
    fun toDeveloper(jpaEntity: CommunityInviteJpaEntity): CommunityInvite.Developer {
        require(jpaEntity.role == CommunityInviteRoleJpaEnum.DEVELOPER) {
            "Community invite id=${jpaEntity.id} has role ${jpaEntity.role}, expected DEVELOPER"
        }
        return developerCommunityInvite { populate(jpaEntity) }
    }

    /**
     * Creates a new [CommunityInviteJpaEntity] row of [kind] from [data].
     *
     * @since %CURRENT_VERSION%
     */
    fun toJpaEntity(data: CommunityInviteData, kind: CommunityInvite.Kind) = CommunityInviteJpaEntity(
        role = kind.toJpaEnum(),
        code = data.codeHash.value,
        codeHashAlgorithm = data.codeHash.algorithm.toJpaEnum(),
        expiresAt = data.expiresAt,
    )

    /**
     * Creates the [CommunityInviteJpaEntity] row replacing [current] from [entity], keeping `role`, `createdAt` and `version`.
     *
     * @since %CURRENT_VERSION%
     */
    fun toJpaEntity(entity: CommunityInvite, current: CommunityInviteJpaEntity) = CommunityInviteJpaEntity(
        role = current.role,
        code = entity.data.codeHash.value,
        codeHashAlgorithm = entity.data.codeHash.algorithm.toJpaEnum(),
        expiresAt = entity.data.expiresAt,
        id = entity.id.value,
    ).also {
        it.createdAt = current.createdAt
        it.version = entity.requireVersion()
    }

    private fun CommunityInviteBuilder<*>.populate(jpaEntity: CommunityInviteJpaEntity) {
        populateFields(jpaEntity)
        data {
            storedCode(
                InviteCodeHash(
                    value = jpaEntity.code,
                    algorithm = jpaEntity.codeHashAlgorithm.toDomain(),
                ),
            )
            expiresAt = jpaEntity.expiresAt
        }
    }
}
