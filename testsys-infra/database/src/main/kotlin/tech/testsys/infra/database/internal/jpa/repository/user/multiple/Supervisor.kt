package tech.testsys.infra.database.internal.jpa.repository.user.multiple

import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.user.multiple.SupervisorDataJpaEntity
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
     * Finds which of the users [userIds] hold the role, in one query.
     *
     * @since %CURRENT_VERSION%
     */
    @Query("select e.userId from SupervisorDataJpaEntity e where e.userId in :userIds")
    fun findUserIdsByUserIdIn(@Param("userIds") userIds: Collection<Long>): List<Long>
}
