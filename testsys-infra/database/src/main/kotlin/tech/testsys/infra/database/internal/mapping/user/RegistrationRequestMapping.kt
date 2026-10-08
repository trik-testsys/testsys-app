package tech.testsys.infra.database.internal.mapping.user

import tech.testsys.domain.builder.api.registrationRequest
import tech.testsys.domain.builder.data
import tech.testsys.domain.model.user.RegistrationRequest
import tech.testsys.domain.model.user.RegistrationRequestData
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.user.RegistrationRequestJpaEntity
import tech.testsys.infra.database.internal.mapping.EntityMapping
import tech.testsys.infra.database.internal.utils.populateFields
import tech.testsys.infra.database.internal.utils.requireVersion

/**
 * Mapping between [RegistrationRequest] and [RegistrationRequestJpaEntity].
 *
 * @since %CURRENT_VERSION%
 */
@InternalDatabaseApi
object RegistrationRequestMapping : EntityMapping<RegistrationRequest, RegistrationRequestJpaEntity> {

    /**
     * Assembles a [RegistrationRequest] from [jpaEntity].
     *
     * @since %CURRENT_VERSION%
     */
    fun toDomain(jpaEntity: RegistrationRequestJpaEntity) = registrationRequest {
        populateFields(jpaEntity)
        data {
            email = jpaEntity.email
            confirmationCode = jpaEntity.confirmationCode
            expiresAt = jpaEntity.expiresAt
            attemptsLeft = jpaEntity.attemptsLeft
        }
    }

    /**
     * Creates a new [RegistrationRequestJpaEntity] row from [data].
     *
     * @since %CURRENT_VERSION%
     */
    fun toJpaEntity(data: RegistrationRequestData) = RegistrationRequestJpaEntity(
        email = data.email,
        confirmationCode = data.confirmationCode,
        expiresAt = data.expiresAt,
        attemptsLeft = data.attemptsLeft,
    )

    /**
     * Creates the [RegistrationRequestJpaEntity] row replacing [current] from [entity], keeping `email`, `createdAt`
     * and `version`.
     *
     * @since %CURRENT_VERSION%
     */
    fun toJpaEntity(entity: RegistrationRequest, current: RegistrationRequestJpaEntity) = RegistrationRequestJpaEntity(
        email = current.email,
        confirmationCode = entity.data.confirmationCode,
        expiresAt = entity.data.expiresAt,
        attemptsLeft = entity.data.attemptsLeft,
        id = entity.id.value,
    ).also {
        it.createdAt = current.createdAt
        it.version = entity.requireVersion()
    }
}
