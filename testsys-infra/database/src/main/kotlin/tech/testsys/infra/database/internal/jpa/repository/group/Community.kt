package tech.testsys.infra.database.internal.jpa.repository.group

import org.springframework.stereotype.Repository
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.group.CommunityJpaEntity
import tech.testsys.infra.database.internal.jpa.repository.SnowflakeJpaEntityRepository

/**
 * Spring Data repository for [CommunityJpaEntity].
 *
 * @since 1.0.0-beta.1
 */
@Repository
@InternalDatabaseApi
interface CommunityJpaEntityRepository : SnowflakeJpaEntityRepository<CommunityJpaEntity> {

    /**
     * Finds the community referencing [managerInviteId] as its manager invite or [developerInviteId] as its developer invite.
     *
     * @since 1.0.0-beta.1
     */
    fun findByManagerInviteIdOrDeveloperInviteId(managerInviteId: Long, developerInviteId: Long): CommunityJpaEntity?

    /**
     * Finds the communities owned by [ownerId] in ascending id order.
     *
     * @since 1.0.0-beta.1
     */
    fun findAllByOwnerIdOrderByIdAsc(ownerId: Long): List<CommunityJpaEntity>
}
