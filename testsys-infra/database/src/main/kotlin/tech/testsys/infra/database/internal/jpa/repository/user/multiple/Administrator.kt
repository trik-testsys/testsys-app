package tech.testsys.infra.database.internal.jpa.repository.user.multiple

import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
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

    /**
     * Finds which of the users [userIds] hold the role, in one query.
     *
     * @since %CURRENT_VERSION%
     */
    @Query("select e.userId from AdministratorDataJpaEntity e where e.userId in :userIds")
    fun findUserIdsByUserIdIn(@Param("userIds") userIds: Collection<Long>): List<Long>
}
