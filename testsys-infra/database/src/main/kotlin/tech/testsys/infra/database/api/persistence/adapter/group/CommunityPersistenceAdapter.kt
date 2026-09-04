package tech.testsys.infra.database.api.persistence.adapter.group

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import tech.testsys.domain.contract.persistence.repository.CommunityRepository
import tech.testsys.domain.model.group.Community
import tech.testsys.domain.model.group.CommunityData
import tech.testsys.domain.model.group.CommunityId
import tech.testsys.infra.database.api.persistence.adapter.AbstractPersistenceAdapter
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.group.CommunityJpaEntity
import tech.testsys.infra.database.internal.jpa.repository.group.CommunityJpaEntityRepository
import tech.testsys.infra.database.internal.mapping.group.CommunityMapping
import tech.testsys.infra.database.internal.utils.requireById

/**
 * Persistence adapter of [Community] entities backed by [CommunityJpaEntity].
 *
 * @since %CURRENT_VERSION%
 */
@Component
@OptIn(InternalDatabaseApi::class)
class CommunityPersistenceAdapter(
    jpaEntityRepository: CommunityJpaEntityRepository,
) : AbstractPersistenceAdapter<CommunityData, CommunityId, Community, CommunityJpaEntity>(jpaEntityRepository),
    CommunityRepository {

    @Transactional
    override fun save(data: CommunityData): Community {
        val jpaEntity = CommunityMapping.toJpaEntity(data)
        val savedJpaEntity = jpaEntityRepository.save(jpaEntity)

        return assemble(savedJpaEntity)
    }

    @Transactional
    override fun update(entity: Community): Community {
        val currentJpaEntity = jpaEntityRepository.findByIdOrNull(entity.id.value).requireById(entity.id.value)
        val updatedJpaEntity = CommunityMapping.toJpaEntity(entity, currentJpaEntity)
        val savedJpaEntity = jpaEntityRepository.saveAndFlush(updatedJpaEntity)

        return assemble(savedJpaEntity)
    }

    override fun assemble(jpaEntity: CommunityJpaEntity) = CommunityMapping.toDomain(jpaEntity)
}
