package tech.testsys.infra.database.internal.jpa.repository.user.single

import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.user.single.SingleRoleToUserId
import tech.testsys.infra.database.internal.jpa.entity.user.single.SingleRoleToUserJpaEntity
import tech.testsys.infra.database.internal.jpa.repository.CompositeJpaEntityRepository

/**
 * Spring Data repository for [SingleRoleToUserJpaEntity] association entities.
 *
 * @since %CURRENT_VERSION%
 */
@Repository
@InternalDatabaseApi
interface SingleRoleToUserJpaEntityRepository :
    CompositeJpaEntityRepository<SingleRoleToUserJpaEntity, SingleRoleToUserId> {

    @Query("select e from SingleRoleToUserJpaEntity e where e.id.userId = :userId")
    fun findAllByUserId(@Param("userId") userId: Long): List<SingleRoleToUserJpaEntity>
}
