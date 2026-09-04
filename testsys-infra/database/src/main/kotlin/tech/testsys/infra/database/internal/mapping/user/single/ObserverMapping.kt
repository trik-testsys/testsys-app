package tech.testsys.infra.database.internal.mapping.user.single

import tech.testsys.domain.builder.api.observer
import tech.testsys.domain.builder.data
import tech.testsys.domain.model.group.CompetitionId
import tech.testsys.domain.model.user.Observer
import tech.testsys.domain.model.user.ObserverData
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.user.UserJpaEntity
import tech.testsys.infra.database.internal.jpa.entity.user.UserTypeJpaEnum
import tech.testsys.infra.database.internal.jpa.entity.user.single.CompetitionToObserverJpaEntity
import tech.testsys.infra.database.internal.jpa.entity.user.single.ObserverDataJpaEntity
import tech.testsys.infra.database.internal.utils.populateFields

/**
 * Mapping between [Observer] and its [UserJpaEntity] and [ObserverDataJpaEntity] rows.
 *
 * @since %CURRENT_VERSION%
 */
@InternalDatabaseApi
object ObserverMapping {

    /**
     * Assembles an [Observer] from [userJpaEntity], [dataJpaEntity] and [competitionIds]; fails when the rows are not bound.
     *
     * @since %CURRENT_VERSION%
     */
    fun toDomain(userJpaEntity: UserJpaEntity, dataJpaEntity: ObserverDataJpaEntity, competitionIds: List<CompetitionId>) = observer {
        populateFields(userJpaEntity)
        check(dataJpaEntity.userId == userJpaEntity.id) {
            "ObserverData ${dataJpaEntity.id} bound to user ${dataJpaEntity.userId} != ${userJpaEntity.id}"
        }
        data {
            accessToken = userJpaEntity.accessToken
            name = userJpaEntity.name
            community(dataJpaEntity.communityId)
            competitions(competitionIds.map { it.value })
        }
    }

    /**
     * Creates a new single-role [UserJpaEntity] row from [data].
     *
     * @since %CURRENT_VERSION%
     */
    fun toUserJpaEntity(data: ObserverData) = UserJpaEntity(
        name = data.name,
        accessToken = data.accessToken,
        email = null,
        type = UserTypeJpaEnum.SINGLE_ROLE,
    )

    /**
     * Creates the [UserJpaEntity] row replacing [current] from [entity], keeping its e-mail, `createdAt` and `version`.
     *
     * @since %CURRENT_VERSION%
     */
    fun toUserJpaEntity(entity: Observer, current: UserJpaEntity) = UserJpaEntity(
        name = entity.data.name,
        accessToken = entity.data.accessToken,
        email = current.email,
        type = UserTypeJpaEnum.SINGLE_ROLE,
        id = entity.id.value,
    ).also {
        it.createdAt = current.createdAt
        it.version = current.version
    }

    /**
     * Creates a new [ObserverDataJpaEntity] row of the user [userId] from [data].
     *
     * @since %CURRENT_VERSION%
     */
    fun toDataJpaEntity(userId: Long, data: ObserverData) = ObserverDataJpaEntity(
        userId = userId,
        communityId = data.community.id.value,
    )

    /**
     * Creates the [ObserverDataJpaEntity] row of the user [userId] replacing [current] from [entity],
     * keeping its id, `createdAt` and `version`.
     *
     * @since %CURRENT_VERSION%
     */
    fun toDataJpaEntity(userId: Long, entity: Observer, current: ObserverDataJpaEntity) = ObserverDataJpaEntity(
        userId = userId,
        communityId = entity.data.community.id.value,
        id = current.id,
    ).also {
        it.createdAt = current.createdAt
        it.version = current.version
    }

    /**
     * Creates the [CompetitionToObserverJpaEntity] rows linking the observer [observerId] with [competitionIds].
     *
     * @since %CURRENT_VERSION%
     */
    fun toCompetitionAssociations(observerId: Long, competitionIds: List<Long>) = competitionIds.map {
        CompetitionToObserverJpaEntity(competitionId = it, observerId = observerId)
    }
}
