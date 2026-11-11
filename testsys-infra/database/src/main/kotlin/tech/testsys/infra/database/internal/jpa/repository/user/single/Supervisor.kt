package tech.testsys.infra.database.internal.jpa.repository.user.single

import org.springframework.stereotype.Repository
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.user.single.SupervisorDataJpaEntity
import tech.testsys.infra.database.internal.jpa.repository.SnowflakeJpaEntityRepository

/**
 * Spring Data repository for [SupervisorDataJpaEntity].
 *
 * @since 1.0.0-beta.1
 */
@Repository
@InternalDatabaseApi
interface SupervisorDataJpaEntityRepository : SnowflakeJpaEntityRepository<SupervisorDataJpaEntity> {

    /**
     * Finds the supervisor data row of the user [userId], or `null` if the user does not hold the role.
     *
     * @since 1.0.0-beta.1
     */
    fun findByUserId(userId: Long): SupervisorDataJpaEntity?

    /**
     * Finds the supervisor data rows of any of the users [userIds] in one query.
     *
     * @since 1.0.0-beta.1
     */
    fun findAllByUserIdIn(userIds: Collection<Long>): List<SupervisorDataJpaEntity>
}
