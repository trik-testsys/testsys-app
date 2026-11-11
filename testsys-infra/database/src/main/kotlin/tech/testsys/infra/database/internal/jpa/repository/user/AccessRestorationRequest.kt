package tech.testsys.infra.database.internal.jpa.repository.user

import org.springframework.stereotype.Repository
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.user.AccessRestorationRequestJpaEntity
import tech.testsys.infra.database.internal.jpa.repository.SnowflakeJpaEntityRepository

/**
 * Spring Data repository for [AccessRestorationRequestJpaEntity].
 *
 * @since 1.0.0-beta.1
 */
@Repository
@InternalDatabaseApi
interface AccessRestorationRequestJpaEntityRepository : SnowflakeJpaEntityRepository<AccessRestorationRequestJpaEntity> {

    /**
     * Finds the request row of the user [userId].
     *
     * @since 1.0.0-beta.1
     */
    fun findByUserId(userId: Long): AccessRestorationRequestJpaEntity?
}
