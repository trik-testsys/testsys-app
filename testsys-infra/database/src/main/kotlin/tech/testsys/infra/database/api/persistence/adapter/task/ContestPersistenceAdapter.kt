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
import tech.testsys.domain.model.group.CompetitionId
import tech.testsys.domain.model.task.Contest
import tech.testsys.domain.model.task.ContestData
import tech.testsys.domain.model.task.ContestId
import tech.testsys.domain.model.task.TaskId
import tech.testsys.domain.model.task.TrikStudioVersion
import tech.testsys.domain.model.user.MultipleRoleUserId
import tech.testsys.infra.database.api.persistence.adapter.AbstractPersistenceAdapter
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.group.ContestToCompetitionJpaEntity
import tech.testsys.infra.database.internal.jpa.entity.task.CommunityToContestJpaEntity
import tech.testsys.infra.database.internal.jpa.entity.task.ContestJpaEntity
import tech.testsys.infra.database.internal.jpa.repository.task.CommunityToContestJpaEntityRepository
import tech.testsys.infra.database.internal.jpa.repository.task.ContestJpaEntityRepository
import tech.testsys.infra.database.internal.jpa.repository.task.TaskToContestJpaEntityRepository
import tech.testsys.infra.database.internal.jpa.repository.task.TrikStudioVersionJpaEntityRepository
import tech.testsys.infra.database.internal.mapping.task.ContestMapping
import tech.testsys.infra.database.internal.utils.findByIdOrError
import tech.testsys.infra.database.internal.utils.findIdByTagOrError
import tech.testsys.infra.database.internal.utils.requireId
import tech.testsys.infra.database.internal.utils.syncJoinTable
import org.springframework.data.domain.Sort as JpaSort

/**
 * Persistence adapter of [Contest] entities backed by [ContestJpaEntity].
 * Task and shared-community membership is synced through the join tables on save and update and dropped on remove;
 * the TRIK Studio version must already be registered by tag.
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
        competitionIds: Set<CompetitionId>,
        pagination: Pagination,
        filter: ObserverContestFilter,
    ): Page<Contest> {
        if (competitionIds.isEmpty()) return Page(content = emptyList(), pagination = pagination, totalElements = 0)
        val specification = Specification<ContestJpaEntity> { entity, query, builder ->
            val assigned = requireNotNull(query).subquery(Long::class.java)
            val association = assigned.from(ContestToCompetitionJpaEntity::class.java)
            assigned.select(association.get<Any>("id").get<Long>("contestId")).where(
                builder.equal(association.get<Any>("id").get<Long>("contestId"), entity.get<Long>("id")),
                association.get<Any>("id").get<Long>("competitionId").`in`(competitionIds.map { competition -> competition.value }),
            )
            val predicates = mutableListOf(builder.exists(assigned))
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
            content = page.content.map { entity -> assemble(entity) },
            pagination = pagination,
            totalElements = page.totalElements,
        )
    }

    @Transactional(readOnly = true)
    override fun findByTaskId(taskId: TaskId): List<Contest> =
        taskToContestJpaEntityRepository.findAllByTaskId(taskId.value).map { it.id.contestId }
            .distinct().sorted().map { assemble(contestJpaEntityRepository.findByIdOrError(it)) }

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
            content = page.content.map { entity -> assemble(entity) },
            pagination = pagination,
            totalElements = page.totalElements,
        )
    }

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
        val currentJpaEntity = jpaEntityRepository.findByIdOrError(entity.id.value)
        val trikStudioVersionId = trikStudioVersionJpaEntityRepository.findIdByTagOrError(entity.data.trikStudioVersion.version)
        val updatedJpaEntity = ContestMapping.toJpaEntity(entity, currentJpaEntity, trikStudioVersionId)
        val savedJpaEntity = jpaEntityRepository.saveAndFlush(updatedJpaEntity)

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

    @Transactional
    override fun removeById(id: ContestId) {
        val jpaEntity = jpaEntityRepository.findByIdOrNull(id.value) ?: return
        val contestId = jpaEntity.requireId()
        taskToContestJpaEntityRepository.deleteAll(taskToContestJpaEntityRepository.findAllByContestId(contestId))
        communityToContestJpaEntityRepository.deleteAll(communityToContestJpaEntityRepository.findAllByContestId(contestId))
        jpaEntityRepository.delete(jpaEntity)
    }

    @Transactional
    override fun removeByIds(ids: List<ContestId>) = ids.forEach(::removeById)

    override fun assemble(jpaEntity: ContestJpaEntity): Contest {
        val contestId = jpaEntity.requireId()
        val tag = trikStudioVersionJpaEntityRepository.findByIdOrError(jpaEntity.trikStudioVersionId).tag

        val taskIds = taskToContestJpaEntityRepository.findAllByContestId(contestId).map { TaskId(it.id.taskId) }
        val sharedToIds = communityToContestJpaEntityRepository.findAllByContestId(contestId).map { CommunityId(it.id.communityId) }

        val domainEntity = ContestMapping.toDomain(jpaEntity, TrikStudioVersion(tag), taskIds, sharedToIds)
        return domainEntity
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
