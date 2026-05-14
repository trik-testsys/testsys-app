package tech.testsys.infra.database.internal.jpa.repository.user.multiple

import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.user.multiple.MultipleRoleToUserId
import tech.testsys.infra.database.internal.jpa.entity.user.multiple.MultipleRoleToUserJpaEntity
import tech.testsys.infra.database.internal.jpa.repository.CompositeJpaEntityRepository

/**
 * Spring Data repository for [MultipleRoleToUserJpaEntity] association entities.
 *
 * @since %CURRENT_VERSION%
 */
@Repository
@InternalDatabaseApi
interface MultipleRoleToUserJpaEntityRepository :
    CompositeJpaEntityRepository<MultipleRoleToUserJpaEntity, MultipleRoleToUserId> {

    @Query("select e from MultipleRoleToUserJpaEntity e where e.id.userId = :userId")
    fun findAllByUserId(@Param("userId") userId: Long): List<MultipleRoleToUserJpaEntity>
}
