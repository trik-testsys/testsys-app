package tech.testsys.infra.database.internal.mapping.user.single

import tech.testsys.domain.builder.api.supervisor
import tech.testsys.domain.builder.data
import tech.testsys.domain.model.user.Supervisor
import tech.testsys.domain.model.user.SupervisorData
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.user.UserJpaEntity
import tech.testsys.infra.database.internal.jpa.entity.user.UserTypeJpaEnum
import tech.testsys.infra.database.internal.jpa.entity.user.single.SupervisorDataJpaEntity
import tech.testsys.infra.database.internal.utils.populateFields
import tech.testsys.infra.database.internal.utils.requireAccessTokenHash
import tech.testsys.infra.database.internal.utils.requireVersion
import tech.testsys.infra.database.internal.utils.toJpaEnum

/**
 * Mapping between [Supervisor] and its [UserJpaEntity] and [SupervisorDataJpaEntity] rows.
 *
 * @since 1.0.0-beta.1
 */
@InternalDatabaseApi
object SupervisorMapping {

    /**
     * Assembles a [Supervisor] from [userJpaEntity] and [dataJpaEntity]; fails when the rows are not bound.
     *
     * @since 1.0.0-beta.1
     */
    fun toDomain(userJpaEntity: UserJpaEntity, dataJpaEntity: SupervisorDataJpaEntity) = supervisor {
        populateFields(userJpaEntity)
        // dataJpaEntity is consumed only for presence; it has no extra fields.
        check(dataJpaEntity.userId == userJpaEntity.id) {
            "SupervisorData ${dataJpaEntity.id} bound to user ${dataJpaEntity.userId} != ${userJpaEntity.id}"
        }
        data {
            storedAccessToken(userJpaEntity.requireAccessTokenHash())
            name = userJpaEntity.name
        }
    }

    /**
     * Creates a new single-role [UserJpaEntity] row from [data].
     *
     * @since 1.0.0-beta.1
     */
    fun toUserJpaEntity(data: SupervisorData) = UserJpaEntity(
        name = data.name,
        accessToken = data.accessTokenHash.value,
        accessTokenHashAlgorithm = data.accessTokenHash.algorithm.toJpaEnum(),
        passwordHash = null,
        email = null,
        type = UserTypeJpaEnum.SINGLE_ROLE,
    )

    /**
     * Creates the [UserJpaEntity] row replacing [current] from [entity], keeping its e-mail, `createdAt` and `version`.
     *
     * @since 1.0.0-beta.1
     */
    fun toUserJpaEntity(entity: Supervisor, current: UserJpaEntity) = UserJpaEntity(
        name = entity.data.name,
        accessToken = entity.data.accessTokenHash.value,
        accessTokenHashAlgorithm = entity.data.accessTokenHash.algorithm.toJpaEnum(),
        passwordHash = null,
        email = current.email,
        type = UserTypeJpaEnum.SINGLE_ROLE,
        id = entity.id.value,
    ).also {
        it.createdAt = current.createdAt
        it.version = entity.requireVersion()
    }

    /**
     * Creates a new [SupervisorDataJpaEntity] row of the user [userId].
     *
     * @since 1.0.0-beta.1
     */
    fun toDataJpaEntity(userId: Long) = SupervisorDataJpaEntity(userId = userId)
}
