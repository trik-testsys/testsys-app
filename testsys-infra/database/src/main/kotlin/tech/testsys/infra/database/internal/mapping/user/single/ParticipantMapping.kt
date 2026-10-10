package tech.testsys.infra.database.internal.mapping.user.single

import tech.testsys.domain.builder.api.participant
import tech.testsys.domain.builder.data
import tech.testsys.domain.model.user.AccessTokenHash
import tech.testsys.domain.model.user.Participant
import tech.testsys.domain.model.user.ParticipantData
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.user.UserJpaEntity
import tech.testsys.infra.database.internal.jpa.entity.user.UserTypeJpaEnum
import tech.testsys.infra.database.internal.jpa.entity.user.single.ParticipantDataJpaEntity
import tech.testsys.infra.database.internal.utils.populateFields
import tech.testsys.infra.database.internal.utils.requireVersion
import tech.testsys.infra.database.internal.utils.toDomain
import tech.testsys.infra.database.internal.utils.toJpaEnum

/**
 * Mapping between [Participant] and its [UserJpaEntity] and [ParticipantDataJpaEntity] rows.
 *
 * @since %CURRENT_VERSION%
 */
@InternalDatabaseApi
object ParticipantMapping {

    /**
     * Assembles a [Participant] from [userJpaEntity] and [dataJpaEntity]; fails when the rows are not bound.
     *
     * @since %CURRENT_VERSION%
     */
    fun toDomain(userJpaEntity: UserJpaEntity, dataJpaEntity: ParticipantDataJpaEntity) = participant {
        populateFields(userJpaEntity)
        check(dataJpaEntity.userId == userJpaEntity.id) {
            "ParticipantData ${dataJpaEntity.id} bound to user ${dataJpaEntity.userId} != ${userJpaEntity.id}"
        }
        data {
            storedAccessToken(
                AccessTokenHash(
                    value = userJpaEntity.accessToken,
                    algorithm = userJpaEntity.accessTokenHashAlgorithm.toDomain(),
                ),
            )
            name = userJpaEntity.name
            competition(dataJpaEntity.competitionId)
        }
    }

    /**
     * Creates a new single-role [UserJpaEntity] row from [data].
     *
     * @since %CURRENT_VERSION%
     */
    fun toUserJpaEntity(data: ParticipantData) = UserJpaEntity(
        name = data.name,
        accessToken = data.accessTokenHash.value,
        accessTokenHashAlgorithm = data.accessTokenHash.algorithm.toJpaEnum(),
        email = null,
        type = UserTypeJpaEnum.SINGLE_ROLE,
    )

    /**
     * Creates the [UserJpaEntity] row replacing [current] from [entity], keeping its e-mail, `createdAt` and `version`.
     *
     * @since %CURRENT_VERSION%
     */
    fun toUserJpaEntity(entity: Participant, current: UserJpaEntity) = UserJpaEntity(
        name = entity.data.name,
        accessToken = entity.data.accessTokenHash.value,
        accessTokenHashAlgorithm = entity.data.accessTokenHash.algorithm.toJpaEnum(),
        email = current.email,
        type = UserTypeJpaEnum.SINGLE_ROLE,
        id = entity.id.value,
    ).also {
        it.createdAt = current.createdAt
        it.version = entity.requireVersion()
    }

    /**
     * Creates a new [ParticipantDataJpaEntity] row of the user [userId] from [data].
     *
     * @since %CURRENT_VERSION%
     */
    fun toDataJpaEntity(userId: Long, data: ParticipantData) = ParticipantDataJpaEntity(
        userId = userId,
        competitionId = data.competition.id.value,
    )
}
