package tech.testsys.infra.database.internal.jpa.repository.user.multiple

import org.springframework.stereotype.Repository
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.user.multiple.DeveloperDataJpaEntity
import tech.testsys.infra.database.internal.jpa.repository.SnowflakeJpaEntityRepository

/**
 * Spring Data repository for [DeveloperDataJpaEntity].
 *
 * @since %CURRENT_VERSION%
 */
@Repository
@InternalDatabaseApi
interface DeveloperDataJpaEntityRepository : SnowflakeJpaEntityRepository<DeveloperDataJpaEntity> {

    /**
     * Finds the developer data row of the user [userId], or `null` if the user does not hold the role.
     *
     * @since %CURRENT_VERSION%
     */
    fun findByUserId(userId: Long): DeveloperDataJpaEntity?
}
