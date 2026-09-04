package tech.testsys.infra.database.internal.jpa.repository.user.multiple

import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.user.multiple.MultipleRoleToUserId
import tech.testsys.infra.database.internal.jpa.entity.user.multiple.MultipleRoleToUserJpaEntity
import tech.testsys.infra.database.internal.jpa.repository.CompositeJpaEntityRepository

/**
 * Spring Data repository for [MultipleRoleToUserJpaEntity].
 *
 * @since %CURRENT_VERSION%
 */
@Repository
@InternalDatabaseApi
interface MultipleRoleToUserJpaEntityRepository :
    CompositeJpaEntityRepository<MultipleRoleToUserJpaEntity, MultipleRoleToUserId> {

    /**
     * Finds the (role, community) membership rows of the user [userId].
     *
     * @since %CURRENT_VERSION%
     */
    @Query("select e from MultipleRoleToUserJpaEntity e where e.id.userId = :userId")
    fun findAllByUserId(@Param("userId") userId: Long): List<MultipleRoleToUserJpaEntity>
}
