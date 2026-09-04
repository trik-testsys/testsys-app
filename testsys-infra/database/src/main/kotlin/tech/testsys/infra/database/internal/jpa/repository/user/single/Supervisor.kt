package tech.testsys.infra.database.internal.jpa.repository.user.single

import org.springframework.stereotype.Repository
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.user.single.SupervisorDataJpaEntity
import tech.testsys.infra.database.internal.jpa.repository.SnowflakeJpaEntityRepository

/**
 * Spring Data repository for [SupervisorDataJpaEntity].
 *
 * @since %CURRENT_VERSION%
 */
@Repository
@InternalDatabaseApi
interface SupervisorDataJpaEntityRepository : SnowflakeJpaEntityRepository<SupervisorDataJpaEntity> {

    /**
     * Finds the supervisor data row of the user [userId], or `null` if the user does not hold the role.
     *
     * @since %CURRENT_VERSION%
     */
    fun findByUserId(userId: Long): SupervisorDataJpaEntity?
}
