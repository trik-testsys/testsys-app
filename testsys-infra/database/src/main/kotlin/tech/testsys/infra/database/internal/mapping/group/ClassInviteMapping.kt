package tech.testsys.infra.database.internal.mapping.group

import tech.testsys.domain.builder.api.classInvite
import tech.testsys.domain.builder.data
import tech.testsys.domain.model.group.ClassInvite
import tech.testsys.domain.model.group.ClassInviteData
import tech.testsys.domain.model.group.InviteCodeHash
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.group.ClassInviteJpaEntity
import tech.testsys.infra.database.internal.mapping.EntityMapping
import tech.testsys.infra.database.internal.utils.populateFields
import tech.testsys.infra.database.internal.utils.requireVersion
import tech.testsys.infra.database.internal.utils.toDomain
import tech.testsys.infra.database.internal.utils.toJpaEnum

/**
 * Mapping between [ClassInvite] and [ClassInviteJpaEntity].
 *
 * @since %CURRENT_VERSION%
 */
@InternalDatabaseApi
object ClassInviteMapping : EntityMapping<ClassInvite, ClassInviteJpaEntity> {

    /**
     * Assembles a [ClassInvite] from [jpaEntity] without hashing its stored code.
     *
     * @since %CURRENT_VERSION%
     */
    fun toDomain(jpaEntity: ClassInviteJpaEntity) = classInvite {
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

    /**
     * Creates a new [ClassInviteJpaEntity] row from [data].
     *
     * @since %CURRENT_VERSION%
     */
    fun toJpaEntity(data: ClassInviteData) = ClassInviteJpaEntity(
        code = data.codeHash.value,
        codeHashAlgorithm = data.codeHash.algorithm.toJpaEnum(),
        expiresAt = data.expiresAt,
    )

    /**
     * Creates the [ClassInviteJpaEntity] row replacing [current] from [entity], keeping `createdAt` and `version`.
     *
     * @since %CURRENT_VERSION%
     */
    fun toJpaEntity(entity: ClassInvite, current: ClassInviteJpaEntity) = ClassInviteJpaEntity(
        code = entity.data.codeHash.value,
        codeHashAlgorithm = entity.data.codeHash.algorithm.toJpaEnum(),
        expiresAt = entity.data.expiresAt,
        id = entity.id.value,
    ).also {
        it.createdAt = current.createdAt
        it.version = entity.requireVersion()
    }
}
