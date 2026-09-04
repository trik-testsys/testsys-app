package tech.testsys.infra.database.api.persistence.adapter.user.single

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import tech.testsys.domain.contract.persistence.repository.ParticipantRepository
import tech.testsys.domain.model.user.Participant
import tech.testsys.domain.model.user.ParticipantData
import tech.testsys.domain.model.user.SingleRoleUserId
import tech.testsys.infra.database.api.persistence.adapter.user.AbstractUserPersistenceAdapter
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.user.UserJpaEntity
import tech.testsys.infra.database.internal.jpa.entity.user.single.SingleRoleToUserJpaEntity
import tech.testsys.infra.database.internal.jpa.entity.user.single.UserSingleRoleJpaEnum
import tech.testsys.infra.database.internal.jpa.repository.user.UserJpaEntityRepository
import tech.testsys.infra.database.internal.jpa.repository.user.single.ParticipantDataJpaEntityRepository
import tech.testsys.infra.database.internal.jpa.repository.user.single.SingleRoleToUserJpaEntityRepository
import tech.testsys.infra.database.internal.mapping.user.single.ParticipantMapping
import tech.testsys.infra.database.internal.utils.findByIdOrError
import tech.testsys.infra.database.internal.utils.requireById
import tech.testsys.infra.database.internal.utils.requireId

/**
 * Persistence adapter of [Participant] entities backed by [UserJpaEntity] rows having a participant data row.
 *
 * @since %CURRENT_VERSION%
 */
@Component
@OptIn(InternalDatabaseApi::class)
class ParticipantPersistenceAdapter(
    jpaEntityRepository: UserJpaEntityRepository,
    private val participantDataJpaEntityRepository: ParticipantDataJpaEntityRepository,
    private val singleRoleToUserJpaEntityRepository: SingleRoleToUserJpaEntityRepository,
) : AbstractUserPersistenceAdapter<ParticipantData, SingleRoleUserId, Participant>(jpaEntityRepository),
    ParticipantRepository {

    @Transactional
    override fun save(data: ParticipantData): Participant {
        val userJpaEntity = ParticipantMapping.toUserJpaEntity(data)
        val savedUserJpaEntity = jpaEntityRepository.save(userJpaEntity)
        val userId = savedUserJpaEntity.requireId()

        singleRoleToUserJpaEntityRepository.save(
            SingleRoleToUserJpaEntity(singleRole = UserSingleRoleJpaEnum.PARTICIPANT, userId = userId),
        )
        val savedDataJpaEntity = participantDataJpaEntityRepository.save(ParticipantMapping.toDataJpaEntity(userId, data))

        val domainEntity = ParticipantMapping.toDomain(savedUserJpaEntity, savedDataJpaEntity)
        return domainEntity
    }

    @Transactional
    override fun update(entity: Participant): Participant {
        val currentUserJpaEntity = jpaEntityRepository.findByIdOrError(entity.id.value)
        val currentDataJpaEntity = participantDataJpaEntityRepository.findByUserId(entity.id.value).requireById(entity.id.value)

        val updatedUserJpaEntity = jpaEntityRepository.saveAndFlush(
            ParticipantMapping.toUserJpaEntity(entity, currentUserJpaEntity),
        )
        val updatedDataJpaEntity = participantDataJpaEntityRepository.save(
            ParticipantMapping.toDataJpaEntity(updatedUserJpaEntity.requireId(), entity, currentDataJpaEntity),
        )

        val domainEntity = ParticipantMapping.toDomain(updatedUserJpaEntity, updatedDataJpaEntity)
        return domainEntity
    }

    @Transactional
    override fun removeById(id: SingleRoleUserId) {
        val dataJpaEntity = participantDataJpaEntityRepository.findByUserId(id.value) ?: return
        participantDataJpaEntityRepository.delete(dataJpaEntity)
        singleRoleToUserJpaEntityRepository.deleteAll(singleRoleToUserJpaEntityRepository.findAllByUserId(id.value))
        jpaEntityRepository.deleteById(id.value)
    }

    @Transactional
    override fun removeByIds(ids: List<SingleRoleUserId>) = ids.forEach(::removeById)

    override fun supports(jpaEntity: UserJpaEntity) = participantDataJpaEntityRepository.findByUserId(jpaEntity.requireId()) != null

    override fun assemble(jpaEntity: UserJpaEntity): Participant {
        val userId = jpaEntity.requireId()
        val dataJpaEntity = participantDataJpaEntityRepository.findByUserId(userId).requireById(userId)
        val domainEntity = ParticipantMapping.toDomain(jpaEntity, dataJpaEntity)
        return domainEntity
    }
}
