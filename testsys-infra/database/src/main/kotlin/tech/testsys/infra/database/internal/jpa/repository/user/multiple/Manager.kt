package tech.testsys.infra.database.internal.jpa.repository.user.multiple

import org.springframework.stereotype.Repository
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.user.multiple.ManagerDataJpaEntity
import tech.testsys.infra.database.internal.jpa.repository.SnowflakeJpaEntityRepository

/**
 * Spring Data repository for [ManagerDataJpaEntity].
 *
 * @since %CURRENT_VERSION%
 */
@Repository
@InternalDatabaseApi
interface ManagerDataJpaEntityRepository : SnowflakeJpaEntityRepository<ManagerDataJpaEntity> {

    /**
     * Finds the manager data row of the user [userId], or `null` if the user does not hold the role.
     *
     * @since %CURRENT_VERSION%
     */
    fun findByUserId(userId: Long): ManagerDataJpaEntity?
}
