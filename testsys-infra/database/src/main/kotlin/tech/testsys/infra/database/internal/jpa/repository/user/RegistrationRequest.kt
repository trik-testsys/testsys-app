package tech.testsys.infra.database.internal.jpa.repository.user

import org.springframework.stereotype.Repository
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.user.RegistrationRequestJpaEntity
import tech.testsys.infra.database.internal.jpa.repository.SnowflakeJpaEntityRepository

/**
 * Spring Data repository for [RegistrationRequestJpaEntity].
 *
 * @since 1.0.0-beta.1
 */
@Repository
@InternalDatabaseApi
interface RegistrationRequestJpaEntityRepository : SnowflakeJpaEntityRepository<RegistrationRequestJpaEntity> {

    /**
     * Finds the request row whose e-mail address equals [email].
     *
     * @since 1.0.0-beta.1
     */
    fun findByEmail(email: String): RegistrationRequestJpaEntity?
}
