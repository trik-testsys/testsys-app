package tech.testsys.infra.database.jpa.beans.repository.user.multiple

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import tech.testsys.infra.database.jpa.beans.repository.CompositeJpaEntityRepository
import tech.testsys.infra.database.jpa.entity.user.multiple.MultipleRoleToUserId
import tech.testsys.infra.database.jpa.entity.user.multiple.MultipleRoleToUserJpaEntity
import tech.testsys.infra.database.jpa.entity.user.multiple.UserMultipleRoleJpaEnum

/**
 * Spring Data repository for [MultipleRoleToUserJpaEntity] association entities.
 *
 * @since %CURRENT_VERSION%
 */
@Repository
interface MultipleRoleToUserJpaEntityRepository : CompositeJpaEntityRepository<MultipleRoleToUserJpaEntity, MultipleRoleToUserId> {

    @Query("select e from MultipleRoleToUserJpaEntity e where e.id.multipleRole = :multipleRole")
    fun findAllByMultipleRole(@Param("multipleRole") multipleRole: UserMultipleRoleJpaEnum): List<MultipleRoleToUserJpaEntity>

    @Query("select e from MultipleRoleToUserJpaEntity e where e.id.multipleRole = :multipleRole")
    fun findAllByMultipleRole(
        @Param("multipleRole") multipleRole: UserMultipleRoleJpaEnum,
        pageable: Pageable,
    ): Page<MultipleRoleToUserJpaEntity>

    @Query("select e from MultipleRoleToUserJpaEntity e where e.id.userId = :userId")
    fun findAllByUserId(@Param("userId") userId: Long): List<MultipleRoleToUserJpaEntity>

    @Query("select e from MultipleRoleToUserJpaEntity e where e.id.userId = :userId")
    fun findAllByUserId(@Param("userId") userId: Long, pageable: Pageable): Page<MultipleRoleToUserJpaEntity>

    @Query("select e from MultipleRoleToUserJpaEntity e where e.id.communityId = :communityId")
    fun findAllByCommunityId(@Param("communityId") communityId: Long): List<MultipleRoleToUserJpaEntity>

    @Query("select e from MultipleRoleToUserJpaEntity e where e.id.communityId = :communityId")
    fun findAllByCommunityId(@Param("communityId") communityId: Long, pageable: Pageable): Page<MultipleRoleToUserJpaEntity>
}
