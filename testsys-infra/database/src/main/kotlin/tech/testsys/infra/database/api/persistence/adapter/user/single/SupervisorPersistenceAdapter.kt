package tech.testsys.infra.database.api.persistence.adapter.user.single

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import tech.testsys.domain.contract.persistence.repository.SupervisorRepository
import tech.testsys.domain.model.user.SingleRoleUserId
import tech.testsys.domain.model.user.Supervisor
import tech.testsys.domain.model.user.SupervisorData
import tech.testsys.infra.database.api.persistence.adapter.AbstractPersistenceAdapter
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.user.UserJpaEntity
import tech.testsys.infra.database.internal.jpa.entity.user.single.SingleRoleToUserJpaEntity
import tech.testsys.infra.database.internal.jpa.entity.user.single.UserSingleRoleJpaEnum
import tech.testsys.infra.database.internal.jpa.repository.user.UserJpaEntityRepository
import tech.testsys.infra.database.internal.jpa.repository.user.single.SingleRoleToUserJpaEntityRepository
import tech.testsys.infra.database.internal.jpa.repository.user.single.SupervisorDataJpaEntityRepository
import tech.testsys.infra.database.internal.mapping.user.single.SupervisorMapping
import tech.testsys.infra.database.internal.utils.findByIdOrError
import tech.testsys.infra.database.internal.utils.requireById
import tech.testsys.infra.database.internal.utils.requireId

@Component
@OptIn(InternalDatabaseApi::class)
class SupervisorPersistenceAdapter(
    jpaEntityRepository: UserJpaEntityRepository,
    private val supervisorDataJpaEntityRepository: SupervisorDataJpaEntityRepository,
    private val singleRoleToUserJpaEntityRepository: SingleRoleToUserJpaEntityRepository,
) : AbstractPersistenceAdapter<SupervisorData, SingleRoleUserId, Supervisor, UserJpaEntity>(jpaEntityRepository),
    SupervisorRepository {

    @Transactional
    override fun save(data: SupervisorData): Supervisor {
        val savedUserJpaEntity = jpaEntityRepository.save(SupervisorMapping.toUserJpaEntity(data))
        val userId = savedUserJpaEntity.requireId()

        singleRoleToUserJpaEntityRepository.save(
            SingleRoleToUserJpaEntity(singleRole = UserSingleRoleJpaEnum.SUPERVISOR, userId = userId),
        )
        val savedDataJpaEntity = supervisorDataJpaEntityRepository.save(SupervisorMapping.toDataJpaEntity(userId))

        val domainEntity = SupervisorMapping.toDomain(savedUserJpaEntity, savedDataJpaEntity)
        return domainEntity
    }

    @Transactional
    override fun update(entity: Supervisor): Supervisor {
        val currentUserJpaEntity = jpaEntityRepository.findByIdOrError(entity.id.value)
        val currentDataJpaEntity = supervisorDataJpaEntityRepository.findByUserId(entity.id.value).requireById(entity.id.value)

        val updatedUserJpaEntity = jpaEntityRepository.save(
            SupervisorMapping.toUserJpaEntity(entity, currentUserJpaEntity),
        )

        val domainEntity = SupervisorMapping.toDomain(updatedUserJpaEntity, currentDataJpaEntity)
        return domainEntity
    }

    @Transactional
    override fun removeById(id: SingleRoleUserId) {
        val dataJpaEntity = supervisorDataJpaEntityRepository.findByUserId(id.value) ?: return
        supervisorDataJpaEntityRepository.delete(dataJpaEntity)
        singleRoleToUserJpaEntityRepository.deleteAll(singleRoleToUserJpaEntityRepository.findAllByUserId(id.value))
        jpaEntityRepository.deleteById(id.value)
    }

    @Transactional
    override fun removeByIds(ids: List<SingleRoleUserId>) = ids.forEach(::removeById)

    override fun supports(jpaEntity: UserJpaEntity) =
        supervisorDataJpaEntityRepository.findByUserId(jpaEntity.requireId()) != null

    override fun assemble(jpaEntity: UserJpaEntity): Supervisor {
        val userId = jpaEntity.requireId()
        val dataJpaEntity = supervisorDataJpaEntityRepository.findByUserId(userId).requireById(userId)
        val domainEntity = SupervisorMapping.toDomain(jpaEntity, dataJpaEntity)
        return domainEntity
    }
}
