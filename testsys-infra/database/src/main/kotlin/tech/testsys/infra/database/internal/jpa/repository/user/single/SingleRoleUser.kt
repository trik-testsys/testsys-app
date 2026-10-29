package tech.testsys.infra.database.internal.jpa.repository.user.single

import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.user.single.SingleRoleToUserId
import tech.testsys.infra.database.internal.jpa.entity.user.single.SingleRoleToUserJpaEntity
import tech.testsys.infra.database.internal.jpa.repository.CompositeJpaEntityRepository

/**
 * Spring Data repository for [SingleRoleToUserJpaEntity].
 *
 * @since 1.0.0-beta.1
 */
@Repository
@InternalDatabaseApi
interface SingleRoleToUserJpaEntityRepository :
    CompositeJpaEntityRepository<SingleRoleToUserJpaEntity, SingleRoleToUserId> {

    /**
     * Finds the role rows of the user [userId].
     *
     * @since 1.0.0-beta.1
     */
    @Query("select e from SingleRoleToUserJpaEntity e where e.id.userId = :userId")
    fun findAllByUserId(@Param("userId") userId: Long): List<SingleRoleToUserJpaEntity>
}
