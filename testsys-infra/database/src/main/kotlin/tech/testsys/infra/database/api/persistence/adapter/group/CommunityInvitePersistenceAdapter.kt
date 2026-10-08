package tech.testsys.infra.database.api.persistence.adapter.group

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import tech.testsys.domain.contract.persistence.repository.CommunityInviteRepository
import tech.testsys.domain.contract.persistence.repository.DeveloperCommunityInviteRepository
import tech.testsys.domain.contract.persistence.repository.ManagerCommunityInviteRepository
import tech.testsys.domain.model.group.CommunityInvite
import tech.testsys.domain.model.group.CommunityInviteData
import tech.testsys.domain.model.group.CommunityInviteId
import tech.testsys.domain.model.group.InviteCodeHash
import tech.testsys.infra.database.api.persistence.adapter.AbstractPersistenceAdapter
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.group.CommunityInviteJpaEntity
import tech.testsys.infra.database.internal.jpa.repository.group.CommunityInviteJpaEntityRepository
import tech.testsys.infra.database.internal.mapping.group.CommunityInviteMapping
import tech.testsys.infra.database.internal.utils.requireById
import tech.testsys.infra.database.internal.utils.requireId
import tech.testsys.infra.database.internal.utils.toJpaEnum
import java.time.Instant

/**
 * Base of persistence adapters of one [CommunityInvite] variant stored in the shared [CommunityInviteJpaEntity] table:
 * rows of the other variant are reported as absent, so finding, loading, updating and removing stay within [kind].
 *
 * @param Invite the stored variant.
 * @property kind the stored variant.
 * @since %CURRENT_VERSION%
 */
@InternalDatabaseApi
abstract class AbstractCommunityInvitePersistenceAdapter<Invite : CommunityInvite>(
    jpaEntityRepository: CommunityInviteJpaEntityRepository,
    protected val kind: CommunityInvite.Kind,
) : AbstractPersistenceAdapter<CommunityInviteData, CommunityInviteId, Invite, CommunityInviteJpaEntity>(jpaEntityRepository),
    CommunityInviteRepository<Invite> {

    private val invites: CommunityInviteJpaEntityRepository = jpaEntityRepository

    @Transactional(readOnly = true)
    override fun findById(id: CommunityInviteId) =
        jpaEntityRepository.findByIdOrNull(id.value)?.takeIf { supports(it) }?.let { assemble(it) }

    @Transactional(readOnly = true)
    override fun findByIds(ids: List<CommunityInviteId>) =
        assembleAll(jpaEntityRepository.findAllById(ids.map { it.value }).filter { supports(it) })

    @Transactional
    override fun save(data: CommunityInviteData): Invite =
        assemble(jpaEntityRepository.save(CommunityInviteMapping.toJpaEntity(data = data, kind = kind)))

    @Transactional
    override fun update(entity: Invite): Invite {
        val currentJpaEntity = jpaEntityRepository.findByIdOrNull(entity.id.value)?.takeIf { supports(it) }
            .requireById(entity.id.value)
        val updatedJpaEntity = CommunityInviteMapping.toJpaEntity(entity, currentJpaEntity)
        return assemble(jpaEntityRepository.saveAndFlush(updatedJpaEntity))
    }

    @Transactional
    override fun removeById(id: CommunityInviteId) {
        jpaEntityRepository.findByIdOrNull(id.value)?.takeIf { supports(it) }?.let { jpaEntityRepository.delete(it) }
    }

    @Transactional
    override fun removeByIds(ids: List<CommunityInviteId>) {
        val supportedIds = jpaEntityRepository.findAllById(ids.map { it.value }).filter { supports(it) }.map { it.requireId() }
        jpaEntityRepository.deleteAllByIdInBatch(supportedIds)
    }

    @Transactional(readOnly = true)
    override fun findByCode(codeHash: InviteCodeHash): Invite? = invites.findByRoleAndCodeAndCodeHashAlgorithm(
        role = kind.toJpaEnum(),
        code = codeHash.value,
        codeHashAlgorithm = codeHash.algorithm.toJpaEnum(),
    )?.let { assemble(it) }

    @Transactional(readOnly = true)
    override fun findExpired(now: Instant): List<CommunityInviteId> =
        invites.findAllByRoleAndExpiresAtLessThanEqualOrderByIdAsc(role = kind.toJpaEnum(), now = now)
            .map { CommunityInviteId(it.requireId()) }

    private fun supports(jpaEntity: CommunityInviteJpaEntity) = jpaEntity.role == kind.toJpaEnum()
}

/**
 * Persistence adapter of [CommunityInvite.Manager] entities backed by manager-role [CommunityInviteJpaEntity] rows.
 *
 * @since %CURRENT_VERSION%
 */
@Component
@OptIn(InternalDatabaseApi::class)
class ManagerCommunityInvitePersistenceAdapter(
    jpaEntityRepository: CommunityInviteJpaEntityRepository,
) : AbstractCommunityInvitePersistenceAdapter<CommunityInvite.Manager>(jpaEntityRepository, CommunityInvite.Kind.Manager),
    ManagerCommunityInviteRepository {

    override fun assembleAll(rows: List<CommunityInviteJpaEntity>) = rows.map { row -> CommunityInviteMapping.toManager(row) }
}

/**
 * Persistence adapter of [CommunityInvite.Developer] entities backed by developer-role [CommunityInviteJpaEntity] rows.
 *
 * @since %CURRENT_VERSION%
 */
@Component
@OptIn(InternalDatabaseApi::class)
class DeveloperCommunityInvitePersistenceAdapter(
    jpaEntityRepository: CommunityInviteJpaEntityRepository,
) : AbstractCommunityInvitePersistenceAdapter<CommunityInvite.Developer>(jpaEntityRepository, CommunityInvite.Kind.Developer),
    DeveloperCommunityInviteRepository {

    override fun assembleAll(rows: List<CommunityInviteJpaEntity>) = rows.map { row -> CommunityInviteMapping.toDeveloper(row) }
}
