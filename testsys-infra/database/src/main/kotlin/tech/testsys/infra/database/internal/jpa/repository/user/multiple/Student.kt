package tech.testsys.infra.database.internal.jpa.repository.user.multiple

import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.user.multiple.StudentDataJpaEntity
import tech.testsys.infra.database.internal.jpa.repository.SnowflakeJpaEntityRepository

/**
 * Spring Data repository for [StudentDataJpaEntity].
 *
 * @since %CURRENT_VERSION%
 */
@Repository
@InternalDatabaseApi
interface StudentDataJpaEntityRepository : SnowflakeJpaEntityRepository<StudentDataJpaEntity> {

    /**
     * Finds the student data row of the user [userId], or `null` if the user does not hold the role.
     *
     * @since %CURRENT_VERSION%
     */
    fun findByUserId(userId: Long): StudentDataJpaEntity?

    /**
     * Finds which of the users [userIds] hold the role, in one query.
     *
     * @since %CURRENT_VERSION%
     */
    @Query("select e.userId from StudentDataJpaEntity e where e.userId in :userIds")
    fun findUserIdsByUserIdIn(@Param("userIds") userIds: Collection<Long>): List<Long>
}
