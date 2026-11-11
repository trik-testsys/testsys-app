package tech.testsys.infra.database.internal.mapping.user

import tech.testsys.domain.builder.api.accessRestorationRequest
import tech.testsys.domain.builder.data
import tech.testsys.domain.model.user.AccessRestorationRequest
import tech.testsys.domain.model.user.AccessRestorationRequestData
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.user.AccessRestorationRequestJpaEntity
import tech.testsys.infra.database.internal.mapping.EntityMapping
import tech.testsys.infra.database.internal.utils.populateFields
import tech.testsys.infra.database.internal.utils.requireVersion

/**
 * Mapping between [AccessRestorationRequest] and [AccessRestorationRequestJpaEntity].
 *
 * @since 1.0.0-beta.1
 */
@InternalDatabaseApi
object AccessRestorationRequestMapping : EntityMapping<AccessRestorationRequest, AccessRestorationRequestJpaEntity> {

    /**
     * Assembles an [AccessRestorationRequest] from [jpaEntity].
     *
     * @since 1.0.0-beta.1
     */
    fun toDomain(jpaEntity: AccessRestorationRequestJpaEntity) = accessRestorationRequest {
        populateFields(jpaEntity)
        data {
            user(jpaEntity.userId)
            confirmationCode = jpaEntity.confirmationCode
            expiresAt = jpaEntity.expiresAt
            attemptsLeft = jpaEntity.attemptsLeft
        }
    }

    /**
     * Creates a new [AccessRestorationRequestJpaEntity] row from [data].
     *
     * @since 1.0.0-beta.1
     */
    fun toJpaEntity(data: AccessRestorationRequestData) = AccessRestorationRequestJpaEntity(
        userId = data.user.id.value,
        confirmationCode = data.confirmationCode,
        expiresAt = data.expiresAt,
        attemptsLeft = data.attemptsLeft,
    )

    /**
     * Creates the [AccessRestorationRequestJpaEntity] row replacing [current] from [entity], keeping `userId`,
     * `createdAt` and `version`.
     *
     * @since 1.0.0-beta.1
     */
    fun toJpaEntity(entity: AccessRestorationRequest, current: AccessRestorationRequestJpaEntity) = AccessRestorationRequestJpaEntity(
        userId = current.userId,
        confirmationCode = entity.data.confirmationCode,
        expiresAt = entity.data.expiresAt,
        attemptsLeft = entity.data.attemptsLeft,
        id = entity.id.value,
    ).also {
        it.createdAt = current.createdAt
        it.version = entity.requireVersion()
    }
}
