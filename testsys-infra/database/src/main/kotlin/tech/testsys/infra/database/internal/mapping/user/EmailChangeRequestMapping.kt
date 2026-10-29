package tech.testsys.infra.database.internal.mapping.user

import tech.testsys.domain.builder.api.emailChangeRequest
import tech.testsys.domain.builder.data
import tech.testsys.domain.model.user.EmailChangeRequest
import tech.testsys.domain.model.user.EmailChangeRequestData
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.user.EmailChangeRequestJpaEntity
import tech.testsys.infra.database.internal.mapping.EntityMapping
import tech.testsys.infra.database.internal.utils.populateFields
import tech.testsys.infra.database.internal.utils.requireVersion

/**
 * Mapping between [EmailChangeRequest] and [EmailChangeRequestJpaEntity].
 *
 * @since 1.0.0-beta.1
 */
@InternalDatabaseApi
object EmailChangeRequestMapping : EntityMapping<EmailChangeRequest, EmailChangeRequestJpaEntity> {

    /**
     * Assembles an [EmailChangeRequest] from [jpaEntity].
     *
     * @since 1.0.0-beta.1
     */
    fun toDomain(jpaEntity: EmailChangeRequestJpaEntity) = emailChangeRequest {
        populateFields(jpaEntity)
        data {
            user(jpaEntity.userId)
            email = jpaEntity.email
            confirmationCode = jpaEntity.confirmationCode
            expiresAt = jpaEntity.expiresAt
            attemptsLeft = jpaEntity.attemptsLeft
        }
    }

    /**
     * Creates a new [EmailChangeRequestJpaEntity] row from [data].
     *
     * @since 1.0.0-beta.1
     */
    fun toJpaEntity(data: EmailChangeRequestData) = EmailChangeRequestJpaEntity(
        userId = data.user.id.value,
        email = data.email,
        confirmationCode = data.confirmationCode,
        expiresAt = data.expiresAt,
        attemptsLeft = data.attemptsLeft,
    )

    /**
     * Creates the [EmailChangeRequestJpaEntity] row replacing [current] from [entity], keeping `userId`, `createdAt`
     * and `version`.
     *
     * @since 1.0.0-beta.1
     */
    fun toJpaEntity(entity: EmailChangeRequest, current: EmailChangeRequestJpaEntity) = EmailChangeRequestJpaEntity(
        userId = current.userId,
        email = entity.data.email,
        confirmationCode = entity.data.confirmationCode,
        expiresAt = entity.data.expiresAt,
        attemptsLeft = entity.data.attemptsLeft,
        id = entity.id.value,
    ).also {
        it.createdAt = current.createdAt
        it.version = entity.requireVersion()
    }
}
