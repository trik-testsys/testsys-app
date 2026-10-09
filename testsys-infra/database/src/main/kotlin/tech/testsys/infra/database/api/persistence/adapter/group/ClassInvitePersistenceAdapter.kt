package tech.testsys.infra.database.api.persistence.adapter.group

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import tech.testsys.domain.contract.persistence.repository.ClassInviteRepository
import tech.testsys.domain.model.group.ClassInvite
import tech.testsys.domain.model.group.ClassInviteData
import tech.testsys.domain.model.group.ClassInviteId
import tech.testsys.domain.model.group.InviteCodeHash
import tech.testsys.infra.database.api.persistence.adapter.AbstractPersistenceAdapter
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.group.ClassInviteJpaEntity
import tech.testsys.infra.database.internal.jpa.repository.group.ClassInviteJpaEntityRepository
import tech.testsys.infra.database.internal.mapping.group.ClassInviteMapping
import tech.testsys.infra.database.internal.utils.requireId
import tech.testsys.infra.database.internal.utils.requireVersion
import tech.testsys.infra.database.internal.utils.toJpaEnum
import java.time.Instant

/**
 * Persistence adapter of [ClassInvite] entities backed by [ClassInviteJpaEntity].
 *
 * @since %CURRENT_VERSION%
 */
@Component
@OptIn(InternalDatabaseApi::class)
class ClassInvitePersistenceAdapter(
    jpaEntityRepository: ClassInviteJpaEntityRepository,
) : AbstractPersistenceAdapter<ClassInviteData, ClassInviteId, ClassInvite, ClassInviteJpaEntity>(jpaEntityRepository),
    ClassInviteRepository {

    private val invites: ClassInviteJpaEntityRepository = jpaEntityRepository

    @Transactional
    override fun save(data: ClassInviteData): ClassInvite {
        val savedJpaEntity = jpaEntityRepository.save(ClassInviteMapping.toJpaEntity(data))
        return assemble(savedJpaEntity)
    }

    @Transactional
    override fun update(entity: ClassInvite): ClassInvite {
        val savedJpaEntity = updateRoot(entity.id.value, entity.requireVersion()) { current ->
            ClassInviteMapping.toJpaEntity(entity, current)
        }
        return assemble(savedJpaEntity)
    }

    @Transactional(readOnly = true)
    override fun findByCode(codeHash: InviteCodeHash): ClassInvite? = invites.findByCodeAndCodeHashAlgorithm(
        code = codeHash.value,
        codeHashAlgorithm = codeHash.algorithm.toJpaEnum(),
    )?.let { assemble(it) }

    @Transactional(readOnly = true)
    override fun findExpired(now: Instant): List<ClassInviteId> =
        invites.findAllByExpiresAtLessThanEqualOrderByIdAsc(now).map { ClassInviteId(it.requireId()) }

    override fun assembleAll(rows: List<ClassInviteJpaEntity>) = rows.map { row -> ClassInviteMapping.toDomain(row) }
}
