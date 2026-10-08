package tech.testsys.infra.database.api.persistence.adapter.group

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import tech.testsys.domain.contract.persistence.repository.CommunityRepository
import tech.testsys.domain.model.group.Community
import tech.testsys.domain.model.group.CommunityData
import tech.testsys.domain.model.group.CommunityId
import tech.testsys.domain.model.group.CommunityInvite
import tech.testsys.domain.model.group.CommunityInviteData
import tech.testsys.domain.model.group.CommunityInviteId
import tech.testsys.domain.model.user.MultipleRoleUserId
import tech.testsys.infra.database.api.persistence.adapter.AbstractPersistenceAdapter
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.group.CommunityJpaEntity
import tech.testsys.infra.database.internal.jpa.repository.group.CommunityInviteJpaEntityRepository
import tech.testsys.infra.database.internal.jpa.repository.group.CommunityJpaEntityRepository
import tech.testsys.infra.database.internal.mapping.group.CommunityInviteMapping
import tech.testsys.infra.database.internal.mapping.group.CommunityMapping
import tech.testsys.infra.database.internal.utils.requireId
import tech.testsys.infra.database.internal.utils.requireVersion

/**
 * Persistence adapter of [Community] entities backed by [CommunityJpaEntity].
 * The invite references are fixed on creation and both invites are removed together with the community.
 * Update and remove increment the community version; remove also increments the versions of the removed invites.
 *
 * @since %CURRENT_VERSION%
 */
@Component
@OptIn(InternalDatabaseApi::class)
class CommunityPersistenceAdapter(
    jpaEntityRepository: CommunityJpaEntityRepository,
    private val communityInviteJpaEntityRepository: CommunityInviteJpaEntityRepository,
) : AbstractPersistenceAdapter<CommunityData, CommunityId, Community, CommunityJpaEntity>(jpaEntityRepository),
    CommunityRepository {

    private val communities: CommunityJpaEntityRepository = jpaEntityRepository

    @Transactional
    override fun save(data: CommunityData): Community {
        val jpaEntity = CommunityMapping.toJpaEntity(data)
        val savedJpaEntity = jpaEntityRepository.save(jpaEntity)

        return assemble(savedJpaEntity)
    }

    // Spring AOP does not intercept the inner save call; the outer transaction makes all rows atomic.
    @Suppress("CallBeanMethodFromSameClass")
    @Transactional
    override fun saveWithInvites(
        managerInvite: CommunityInviteData,
        developerInvite: CommunityInviteData,
        data: (CommunityInviteId, CommunityInviteId) -> CommunityData,
    ): Community {
        val managerInviteId = saveInvite(managerInvite, CommunityInvite.Kind.Manager)
        val developerInviteId = saveInvite(developerInvite, CommunityInvite.Kind.Developer)
        return save(data(managerInviteId, developerInviteId))
    }

    @Transactional
    override fun update(entity: Community): Community {
        val savedJpaEntity = updateRoot(entity.id.value, entity.requireVersion()) { current ->
            CommunityMapping.toJpaEntity(entity, current)
        }

        return assemble(savedJpaEntity)
    }

    @Transactional(readOnly = true)
    override fun findByInvite(inviteId: CommunityInviteId): Community? =
        communities.findByManagerInviteIdOrDeveloperInviteId(inviteId.value, inviteId.value)?.let { assemble(it) }

    @Transactional(readOnly = true)
    override fun findByOwner(ownerId: MultipleRoleUserId): List<Community> =
        assembleAll(communities.findAllByOwnerIdOrderByIdAsc(ownerId.value))

    override fun removeRoot(id: CommunityId, expectedVersion: Long?) {
        jpaEntityRepository.findByIdOrNull(id.value) ?: return
        val jpaEntity = touchRoot(jpaEntityRepository, id.value, expectedVersion, changesRootData = true)
        jpaEntityRepository.delete(jpaEntity)
        jpaEntityRepository.flush()
        listOf(jpaEntity.managerInviteId, jpaEntity.developerInviteId).forEach { inviteId ->
            communityInviteJpaEntityRepository.delete(touchRoot(communityInviteJpaEntityRepository, inviteId, changesRootData = true))
        }
    }

    override fun assembleAll(rows: List<CommunityJpaEntity>) = rows.map { row -> CommunityMapping.toDomain(row) }

    private fun saveInvite(data: CommunityInviteData, kind: CommunityInvite.Kind): CommunityInviteId {
        val saved = communityInviteJpaEntityRepository.save(CommunityInviteMapping.toJpaEntity(data = data, kind = kind))
        return CommunityInviteId(saved.requireId())
    }
}
