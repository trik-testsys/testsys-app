package tech.testsys.infra.database.internal.jpa.repository.user.multiple

import org.springframework.stereotype.Repository
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.user.multiple.AdministratorDataJpaEntity
import tech.testsys.infra.database.internal.jpa.repository.SnowflakeJpaEntityRepository

/**
 * Spring Data repository for [AdministratorDataJpaEntity].
 *
 * @since %CURRENT_VERSION%
 */
@Repository
@InternalDatabaseApi
interface AdministratorDataJpaEntityRepository : SnowflakeJpaEntityRepository<AdministratorDataJpaEntity> {

    /**
     * Finds the administrator data row of the user [userId], or `null` if the user does not hold the role.
     *
     * @since %CURRENT_VERSION%
     */
    fun findByUserId(userId: Long): AdministratorDataJpaEntity?
}
