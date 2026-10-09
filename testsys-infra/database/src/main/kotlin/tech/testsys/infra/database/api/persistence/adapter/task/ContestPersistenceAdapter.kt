package tech.testsys.infra.database.api.persistence.adapter.task

import org.springframework.data.domain.PageRequest
import org.springframework.data.jpa.domain.Specification
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import tech.testsys.domain.contract.persistence.ContestFilter
import tech.testsys.domain.contract.persistence.ObserverContestFilter
import tech.testsys.domain.contract.persistence.Page
import tech.testsys.domain.contract.persistence.Pagination
import tech.testsys.domain.contract.persistence.repository.ContestRepository
import tech.testsys.domain.model.group.CommunityId
import tech.testsys.domain.model.task.Contest
import tech.testsys.domain.model.task.ContestData
import tech.testsys.domain.model.task.ContestId
import tech.testsys.domain.model.task.TaskId
import tech.testsys.domain.model.task.TrikStudioVersion
import tech.testsys.domain.model.user.MultipleRoleUserId
import tech.testsys.infra.database.api.persistence.adapter.AbstractPersistenceAdapter
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.task.CommunityToContestJpaEntity
import tech.testsys.infra.database.internal.jpa.entity.task.ContestJpaEntity
import tech.testsys.infra.database.internal.jpa.repository.LinkedIdRow
import tech.testsys.infra.database.internal.jpa.repository.task.CommunityToContestJpaEntityRepository
import tech.testsys.infra.database.internal.jpa.repository.task.ContestJpaEntityRepository
import tech.testsys.infra.database.internal.jpa.repository.task.TaskToContestJpaEntityRepository
import tech.testsys.infra.database.internal.jpa.repository.task.TrikStudioVersionJpaEntityRepository
import tech.testsys.infra.database.internal.mapping.task.ContestMapping
import tech.testsys.infra.database.internal.utils.findAllByIdOrError
import tech.testsys.infra.database.internal.utils.findIdByTagOrError
import tech.testsys.infra.database.internal.utils.findLinkedIds
import tech.testsys.infra.database.internal.utils.requireId
import tech.testsys.infra.database.internal.utils.requireVersion
import tech.testsys.infra.database.internal.utils.syncJoinTable
import org.springframework.data.domain.Sort as JpaSort

/**
 * Persistence adapter of [Contest] entities backed by [ContestJpaEntity].
 * Task and shared-community membership is synced through the join tables on save and update and dropped on remove;
 * update and remove increment the contest version first. The TRIK Studio version must already be registered by tag.
 *
 * @since %CURRENT_VERSION%
 */
@Component
@OptIn(InternalDatabaseApi::class)
class ContestPersistenceAdapter(
    jpaEntityRepository: ContestJpaEntityRepository,
    private val taskToContestJpaEntityRepository: TaskToContestJpaEntityRepository,
    private val communityToContestJpaEntityRepository: CommunityToContestJpaEntityRepository,
    private val trikStudioVersionJpaEntityRepository: TrikStudioVersionJpaEntityRepository,
) : AbstractPersistenceAdapter<ContestData, ContestId, Contest, ContestJpaEntity>(jpaEntityRepository),
    ContestRepository {

    private val contestJpaEntityRepository: ContestJpaEntityRepository = jpaEntityRepository

    @Transactional(readOnly = true)
    override fun findAvailableToObserver(
        contestIds: Set<ContestId>,
        pagination: Pagination,
        filter: ObserverContestFilter,
    ): Page<Contest> {
        if (contestIds.isEmpty()) return Page(content = emptyList(), pagination = pagination, totalElements = 0)
        val specification = Specification<ContestJpaEntity> { entity, _, builder ->
            val predicates = mutableListOf(
                entity.get<Long>("id").`in`(contestIds.map { contest -> contest.value }),
            )
            filter.name?.let { name ->
                predicates.add(builder.gt(builder.locate(builder.lower(entity.get("name")), name.lowercase()), 0))
            }
            filter.contestId?.let { contest ->
                predicates.add(builder.equal(entity.get<Long>("id"), contest.value))
            }
            builder.and(*predicates.toTypedArray())
        }
        val orders = pagination.sort.orders.map { order ->
            JpaSort.Order(JpaSort.Direction.valueOf(order.direction.name), order.field)
        }
        val stableOrders = if (orders.any { order -> order.property == "id" }) orders else orders + JpaSort.Order.asc("id")
        val pageable = PageRequest.of(pagination.page, pagination.size, JpaSort.by(stableOrders))
        val page = contestJpaEntityRepository.findAll(specification, pageable)
        return Page(
            content = assembleAll(page.content),
            pagination = pagination,
            totalElements = page.totalElements,
        )
    }

    @Transactional(readOnly = true)
    override fun findByTaskId(taskId: TaskId): List<Contest> {
        val contestIds = taskToContestJpaEntityRepository.findAllByTaskId(taskId.value)
            .map { association -> association.id.contestId }
            .distinct()
            .sorted()
        val rows = contestJpaEntityRepository.findAllByIdOrError(contestIds)
        return assembleAll(contestIds.map { contestId -> rows.getValue(contestId) })
    }

    @Transactional(readOnly = true)
    override fun findAvailableToDeveloper(
        ownerId: MultipleRoleUserId,
        communityIds: Set<CommunityId>,
        pagination: Pagination,
        filter: ContestFilter,
    ): Page<Contest> {
        val specification = Specification<ContestJpaEntity> { entity, query, builder ->
            val owned = builder.equal(entity.get<Long>("ownerId"), ownerId.value)
            val access = if (communityIds.isEmpty()) {
                owned
            } else {
                val shared = query.subquery(Long::class.java)
                val association = shared.from(CommunityToContestJpaEntity::class.java)
                shared.select(association.get<Any>("id").get<Long>("contestId")).where(
                    builder.equal(association.get<Any>("id").get<Long>("contestId"), entity.get<Long>("id")),
                    association.get<Any>("id").get<Long>("communityId").`in`(communityIds.map { community -> community.value }),
                )
                builder.or(owned, builder.exists(shared))
            }
            val predicates = mutableListOf(access)
            filter.name?.let { name ->
                predicates.add(builder.gt(builder.locate(builder.lower(entity.get("name")), name.lowercase()), 0))
            }
            filter.ownerId?.let { owner ->
                predicates.add(builder.equal(entity.get<Long>("ownerId"), owner.value))
            }

            filter.communityId?.let { community ->
                val shared = query.subquery(Long::class.java)
                val association = shared.from(CommunityToContestJpaEntity::class.java)
                shared.select(association.get<Any>("id").get<Long>("contestId")).where(
                    builder.equal(association.get<Any>("id").get<Long>("contestId"), entity.get<Long>("id")),
                    builder.equal(association.get<Any>("id").get<Long>("communityId"), community.value),
                )
                predicates.add(builder.exists(shared))
            }
            builder.and(*predicates.toTypedArray())
        }
        val orders = pagination.sort.orders.map { order ->
            JpaSort.Order(JpaSort.Direction.valueOf(order.direction.name), order.field)
        }
        val stableOrders = if (orders.any { order -> order.property == "id" }) orders else orders + JpaSort.Order.asc("id")
        val pageable = PageRequest.of(pagination.page, pagination.size, JpaSort.by(stableOrders))
        val page = contestJpaEntityRepository.findAll(specification, pageable)
        return Page(
            content = assembleAll(page.content),
            pagination = pagination,
            totalElements = page.totalElements,
        )
    }

    @Transactional(readOnly = true)
    override fun findTrikStudioVersions(): List<TrikStudioVersion> = trikStudioVersionJpaEntityRepository.findAll()
        .map { entity -> TrikStudioVersion(entity.tag) }
        .sortedBy { version -> version.version }

    @Transactional
    override fun save(data: ContestData): Contest {
        val trikStudioVersionId = trikStudioVersionJpaEntityRepository.findIdByTagOrError(data.trikStudioVersion.version)
        val jpaEntity = ContestMapping.toJpaEntity(data, trikStudioVersionId)
        val savedJpaEntity = jpaEntityRepository.save(jpaEntity)
        val contestId = savedJpaEntity.requireId()

        val taskAssociations = ContestMapping.toTaskAssociations(contestId, data.tasks.ids)
        val communityAssociations = ContestMapping.toCommunityAssociations(contestId, data.sharedTo.ids)

        taskToContestJpaEntityRepository.saveAll(taskAssociations)
        communityToContestJpaEntityRepository.saveAll(communityAssociations)

        val domainEntity = ContestMapping.toDomain(savedJpaEntity, data.trikStudioVersion, data.tasks.ids, data.sharedTo.ids)
        return domainEntity
    }

    @Transactional
    override fun update(entity: Contest): Contest {
        val trikStudioVersionId = trikStudioVersionJpaEntityRepository.findIdByTagOrError(entity.data.trikStudioVersion.version)
        val savedJpaEntity = updateRoot(entity.id.value, entity.requireVersion()) { current ->
            ContestMapping.toJpaEntity(entity, current, trikStudioVersionId)
        }

        val contestId = savedJpaEntity.requireId()

        syncTasks(contestId, entity.data.tasks.ids)
        syncSharedTo(contestId, entity.data.sharedTo.ids)

        val domainEntity = ContestMapping.toDomain(
            savedJpaEntity,
            entity.data.trikStudioVersion,
            entity.data.tasks.ids,
            entity.data.sharedTo.ids,
        )
        return domainEntity
    }

    override fun removeRoot(id: ContestId, expectedVersion: Long?) {
        jpaEntityRepository.findByIdOrNull(id.value) ?: return
        val jpaEntity = touchRoot(jpaEntityRepository, id.value, expectedVersion, changesRootData = true)
        val contestId = jpaEntity.requireId()
        taskToContestJpaEntityRepository.deleteAll(taskToContestJpaEntityRepository.findAllByContestId(contestId))
        communityToContestJpaEntityRepository.deleteAll(communityToContestJpaEntityRepository.findAllByContestId(contestId))
        jpaEntityRepository.delete(jpaEntity)
    }

    override fun assembleAll(rows: List<ContestJpaEntity>): List<Contest> {
        val contestIds = rows.map { row -> row.requireId() }
        val versions = trikStudioVersionJpaEntityRepository.findAllByIdOrError(rows.map { row -> row.trikStudioVersionId })
        val taskIds = findLinkedIds(
            ownerIds = contestIds,
            find = taskToContestJpaEntityRepository::findLinkedIdsByContestIdIn,
            ownerIdOf = LinkedIdRow::ownerId,
            linkedIdOf = { link -> TaskId(link.linkedId) },
        )
        val sharedToIds = findLinkedIds(
            ownerIds = contestIds,
            find = communityToContestJpaEntityRepository::findLinkedIdsByContestIdIn,
            ownerIdOf = LinkedIdRow::ownerId,
            linkedIdOf = { link -> CommunityId(link.linkedId) },
        )

        return rows.map { row ->
            val contestId = row.requireId()
            ContestMapping.toDomain(
                jpaEntity = row,
                trikStudioVersion = TrikStudioVersion(versions.getValue(row.trikStudioVersionId).tag),
                taskIds = taskIds.getValue(contestId),
                sharedToIds = sharedToIds.getValue(contestId),
            )
        }
    }

    private fun syncTasks(contestId: Long, target: List<TaskId>) = syncJoinTable(
        existing = taskToContestJpaEntityRepository.findAllByContestId(contestId),
        targetKeys = target,
        keyOf = { TaskId(it.id.taskId) },
        buildAssociation = { ContestMapping.toTaskAssociations(contestId, listOf(it)).single() },
        deleteAll = { taskToContestJpaEntityRepository.deleteAll(it) },
        saveAll = { taskToContestJpaEntityRepository.saveAll(it) },
    )

    private fun syncSharedTo(contestId: Long, target: List<CommunityId>) = syncJoinTable(
        existing = communityToContestJpaEntityRepository.findAllByContestId(contestId),
        targetKeys = target,
        keyOf = { CommunityId(it.id.communityId) },
        buildAssociation = { ContestMapping.toCommunityAssociations(contestId, listOf(it)).single() },
        deleteAll = { communityToContestJpaEntityRepository.deleteAll(it) },
        saveAll = { communityToContestJpaEntityRepository.saveAll(it) },
    )
}
