package tech.testsys.infra.database.api.persistence.adapter.user.single

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import tech.testsys.domain.contract.persistence.repository.ObserverRepository
import tech.testsys.domain.model.task.ContestId
import tech.testsys.domain.model.user.Observer
import tech.testsys.domain.model.user.ObserverData
import tech.testsys.domain.model.user.SingleRoleUserId
import tech.testsys.infra.database.api.persistence.adapter.user.AbstractUserPersistenceAdapter
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.user.UserJpaEntity
import tech.testsys.infra.database.internal.jpa.entity.user.single.SingleRoleToUserJpaEntity
import tech.testsys.infra.database.internal.jpa.entity.user.single.UserSingleRoleJpaEnum
import tech.testsys.infra.database.internal.jpa.repository.LinkedIdRow
import tech.testsys.infra.database.internal.jpa.repository.user.UserJpaEntityRepository
import tech.testsys.infra.database.internal.jpa.repository.user.single.ContestToObserverJpaEntityRepository
import tech.testsys.infra.database.internal.jpa.repository.user.single.ObserverDataJpaEntityRepository
import tech.testsys.infra.database.internal.jpa.repository.user.single.SingleRoleToUserJpaEntityRepository
import tech.testsys.infra.database.internal.mapping.user.single.ObserverMapping
import tech.testsys.infra.database.internal.utils.findAllInChunks
import tech.testsys.infra.database.internal.utils.findLinkedIds
import tech.testsys.infra.database.internal.utils.requireById
import tech.testsys.infra.database.internal.utils.requireId
import tech.testsys.infra.database.internal.utils.requireVersion
import tech.testsys.infra.database.internal.utils.syncJoinTable

/**
 * Persistence adapter of [Observer] entities backed by [UserJpaEntity] rows having an observer data row.
 * Watched contests are synced through the join table; update and remove increment the user version first.
 *
 * @since %CURRENT_VERSION%
 */
@Component
@OptIn(InternalDatabaseApi::class)
class ObserverPersistenceAdapter(
    jpaEntityRepository: UserJpaEntityRepository,
    private val observerDataJpaEntityRepository: ObserverDataJpaEntityRepository,
    private val contestToObserverJpaEntityRepository: ContestToObserverJpaEntityRepository,
    private val singleRoleToUserJpaEntityRepository: SingleRoleToUserJpaEntityRepository,
) : AbstractUserPersistenceAdapter<ObserverData, SingleRoleUserId, Observer>(jpaEntityRepository),
    ObserverRepository {

    @Transactional
    override fun save(data: ObserverData): Observer {
        val savedUserJpaEntity = jpaEntityRepository.save(ObserverMapping.toUserJpaEntity(data))
        val userId = savedUserJpaEntity.requireId()

        singleRoleToUserJpaEntityRepository.save(
            SingleRoleToUserJpaEntity(singleRole = UserSingleRoleJpaEnum.OBSERVER, userId = userId),
        )
        val savedDataJpaEntity = observerDataJpaEntityRepository.save(ObserverMapping.toDataJpaEntity(userId, data))
        val contestIds = data.contests.ids.distinct()
        contestToObserverJpaEntityRepository.saveAll(
            ObserverMapping.toContestAssociations(userId, contestIds.map { it.value }),
        )

        val domainEntity = ObserverMapping.toDomain(savedUserJpaEntity, savedDataJpaEntity, contestIds)
        return domainEntity
    }

    @Transactional
    override fun update(entity: Observer): Observer {
        val currentDataJpaEntity = observerDataJpaEntityRepository.findByUserId(entity.id.value).requireById(entity.id.value)

        val updatedUserJpaEntity = updateRoot(entity.id.value, entity.requireVersion()) { current ->
            ObserverMapping.toUserJpaEntity(entity, current)
        }
        val updatedDataJpaEntity = observerDataJpaEntityRepository.save(
            ObserverMapping.toDataJpaEntity(updatedUserJpaEntity.requireId(), entity, currentDataJpaEntity),
        )
        val contestIds = entity.data.contests.ids.distinct()
        syncContests(updatedUserJpaEntity.requireId(), contestIds)

        val domainEntity = ObserverMapping.toDomain(updatedUserJpaEntity, updatedDataJpaEntity, contestIds)
        return domainEntity
    }

    override fun removeRoot(id: SingleRoleUserId, expectedVersion: Long?) {
        val dataJpaEntity = observerDataJpaEntityRepository.findByUserId(id.value) ?: return
        touchRoot(jpaEntityRepository, id.value, expectedVersion, changesRootData = true)
        contestToObserverJpaEntityRepository.deleteAll(
            contestToObserverJpaEntityRepository.findAllByObserverId(id.value),
        )
        observerDataJpaEntityRepository.delete(dataJpaEntity)
        singleRoleToUserJpaEntityRepository.deleteAll(singleRoleToUserJpaEntityRepository.findAllByUserId(id.value))
        jpaEntityRepository.deleteById(id.value)
    }

    override fun assembleSupported(rows: List<UserJpaEntity>): List<Observer> {
        val dataByUserId = findAllInChunks(
            ids = rows.map { row -> row.requireId() },
            find = observerDataJpaEntityRepository::findAllByUserIdIn,
        ).associateBy { data -> data.userId }
        val contestIds = findLinkedIds(
            ownerIds = dataByUserId.keys,
            find = contestToObserverJpaEntityRepository::findLinkedIdsByObserverIdIn,
            ownerIdOf = LinkedIdRow::ownerId,
            linkedIdOf = { link -> ContestId(link.linkedId) },
        )

        return rows.mapNotNull { row ->
            val userId = row.requireId()
            dataByUserId[userId]?.let { data -> ObserverMapping.toDomain(row, data, contestIds.getValue(userId)) }
        }
    }

    private fun syncContests(observerId: Long, target: List<ContestId>) = syncJoinTable(
        existing = contestToObserverJpaEntityRepository.findAllByObserverId(observerId),
        targetKeys = target,
        keyOf = { ContestId(it.id.contestId) },
        buildAssociation = {
            ObserverMapping.toContestAssociations(observerId, listOf(it.value)).single()
        },
        deleteAll = { contestToObserverJpaEntityRepository.deleteAll(it) },
        saveAll = { contestToObserverJpaEntityRepository.saveAll(it) },
    )
}
