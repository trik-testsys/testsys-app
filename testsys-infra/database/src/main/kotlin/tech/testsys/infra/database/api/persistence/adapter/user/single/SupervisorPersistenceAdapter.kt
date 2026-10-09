package tech.testsys.infra.database.api.persistence.adapter.user.single

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import tech.testsys.domain.contract.persistence.repository.SupervisorRepository
import tech.testsys.domain.model.user.SingleRoleUserId
import tech.testsys.domain.model.user.Supervisor
import tech.testsys.domain.model.user.SupervisorData
import tech.testsys.infra.database.api.persistence.adapter.user.AbstractUserPersistenceAdapter
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.user.UserJpaEntity
import tech.testsys.infra.database.internal.jpa.entity.user.single.SingleRoleToUserJpaEntity
import tech.testsys.infra.database.internal.jpa.entity.user.single.UserSingleRoleJpaEnum
import tech.testsys.infra.database.internal.jpa.repository.user.UserJpaEntityRepository
import tech.testsys.infra.database.internal.jpa.repository.user.single.SingleRoleToUserJpaEntityRepository
import tech.testsys.infra.database.internal.jpa.repository.user.single.SupervisorDataJpaEntityRepository
import tech.testsys.infra.database.internal.mapping.user.single.SupervisorMapping
import tech.testsys.infra.database.internal.utils.findAllInChunks
import tech.testsys.infra.database.internal.utils.requireById
import tech.testsys.infra.database.internal.utils.requireId
import tech.testsys.infra.database.internal.utils.requireVersion

/**
 * Persistence adapter of [Supervisor] entities backed by [UserJpaEntity] rows having a supervisor data row.
 * Update and remove increment the user version first.
 *
 * @since %CURRENT_VERSION%
 */
@Component
@OptIn(InternalDatabaseApi::class)
class SupervisorPersistenceAdapter(
    jpaEntityRepository: UserJpaEntityRepository,
    private val supervisorDataJpaEntityRepository: SupervisorDataJpaEntityRepository,
    private val singleRoleToUserJpaEntityRepository: SingleRoleToUserJpaEntityRepository,
) : AbstractUserPersistenceAdapter<SupervisorData, SingleRoleUserId, Supervisor>(jpaEntityRepository),
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
        val currentDataJpaEntity = supervisorDataJpaEntityRepository.findByUserId(entity.id.value).requireById(entity.id.value)

        val updatedUserJpaEntity = updateRoot(entity.id.value, entity.requireVersion()) { current ->
            SupervisorMapping.toUserJpaEntity(entity, current)
        }

        val domainEntity = SupervisorMapping.toDomain(updatedUserJpaEntity, currentDataJpaEntity)
        return domainEntity
    }

    override fun removeRoot(id: SingleRoleUserId, expectedVersion: Long?) {
        val dataJpaEntity = supervisorDataJpaEntityRepository.findByUserId(id.value) ?: return
        touchRoot(jpaEntityRepository, id.value, expectedVersion, changesRootData = true)
        supervisorDataJpaEntityRepository.delete(dataJpaEntity)
        singleRoleToUserJpaEntityRepository.deleteAll(singleRoleToUserJpaEntityRepository.findAllByUserId(id.value))
        jpaEntityRepository.deleteById(id.value)
    }

    override fun assembleSupported(rows: List<UserJpaEntity>): List<Supervisor> {
        val dataByUserId = findAllInChunks(
            ids = rows.map { row -> row.requireId() },
            find = supervisorDataJpaEntityRepository::findAllByUserIdIn,
        ).associateBy { data -> data.userId }
        return rows.mapNotNull { row -> dataByUserId[row.requireId()]?.let { data -> SupervisorMapping.toDomain(row, data) } }
    }
}
