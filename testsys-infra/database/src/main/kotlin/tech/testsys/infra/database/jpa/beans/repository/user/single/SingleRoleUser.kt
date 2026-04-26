package tech.testsys.infra.database.jpa.beans.repository.user.single

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import tech.testsys.infra.database.jpa.beans.repository.CompositeJpaEntityRepository
import tech.testsys.infra.database.jpa.entity.user.single.SingleRoleToUserId
import tech.testsys.infra.database.jpa.entity.user.single.SingleRoleToUserJpaEntity
import tech.testsys.infra.database.jpa.entity.user.single.UserSingleRoleJpaEnum

/**
 * Spring Data repository for [SingleRoleToUserJpaEntity] association entities.
 *
 * @since %CURRENT_VERSION%
 */
@Repository
interface SingleRoleToUserJpaEntityRepository : CompositeJpaEntityRepository<SingleRoleToUserJpaEntity, SingleRoleToUserId> {

    @Query("select e from SingleRoleToUserJpaEntity e where e.id.singleRole = :singleRole")
    fun findAllBySingleRole(@Param("singleRole") singleRole: UserSingleRoleJpaEnum): List<SingleRoleToUserJpaEntity>

    @Query("select e from SingleRoleToUserJpaEntity e where e.id.singleRole = :singleRole")
    fun findAllBySingleRole(@Param("singleRole") singleRole: UserSingleRoleJpaEnum, pageable: Pageable): Page<SingleRoleToUserJpaEntity>

    @Query("select e from SingleRoleToUserJpaEntity e where e.id.userId = :userId")
    fun findAllByUserId(@Param("userId") userId: Long): List<SingleRoleToUserJpaEntity>

    @Query("select e from SingleRoleToUserJpaEntity e where e.id.userId = :userId")
    fun findAllByUserId(@Param("userId") userId: Long, pageable: Pageable): Page<SingleRoleToUserJpaEntity>
}
