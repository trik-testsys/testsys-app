package tech.testsys.infra.database.internal.jpa.repository.user

import org.springframework.stereotype.Repository
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.user.EmailChangeRequestJpaEntity
import tech.testsys.infra.database.internal.jpa.repository.SnowflakeJpaEntityRepository

/**
 * Spring Data repository for [EmailChangeRequestJpaEntity].
 *
 * @since %CURRENT_VERSION%
 */
@Repository
@InternalDatabaseApi
interface EmailChangeRequestJpaEntityRepository : SnowflakeJpaEntityRepository<EmailChangeRequestJpaEntity> {

    /**
     * Finds the request row of the user [userId].
     *
     * @since %CURRENT_VERSION%
     */
    fun findByUserId(userId: Long): EmailChangeRequestJpaEntity?
}
