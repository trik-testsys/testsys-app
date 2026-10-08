package tech.testsys.infra.database.api.persistence.adapter.group

import org.springframework.data.domain.PageRequest
import org.springframework.data.jpa.domain.Specification
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import tech.testsys.domain.contract.persistence.CompetitionFilter
import tech.testsys.domain.contract.persistence.Page
import tech.testsys.domain.contract.persistence.Pagination
import tech.testsys.domain.contract.persistence.repository.CompetitionRepository
import tech.testsys.domain.model.group.Competition
import tech.testsys.domain.model.group.CompetitionData
import tech.testsys.domain.model.group.CompetitionId
import tech.testsys.domain.model.task.ContestId
import tech.testsys.domain.model.user.MultipleRoleUserId
import tech.testsys.domain.model.user.SingleRoleUserId
import tech.testsys.infra.database.api.persistence.adapter.AbstractPersistenceAdapter
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.group.CompetitionJpaEntity
import tech.testsys.infra.database.internal.jpa.repository.group.CompetitionJpaEntityRepository
import tech.testsys.infra.database.internal.jpa.repository.group.ContestToCompetitionJpaEntityRepository
import tech.testsys.infra.database.internal.jpa.repository.user.single.ParticipantDataJpaEntityRepository
import tech.testsys.infra.database.internal.mapping.group.CompetitionMapping
import tech.testsys.infra.database.internal.utils.findByIdOrError
import tech.testsys.infra.database.internal.utils.requireId
import tech.testsys.infra.database.internal.utils.syncJoinTable
import java.time.Instant
import org.springframework.data.domain.Sort as JpaSort

/**
 * Persistence adapter of [Competition] entities backed by [CompetitionJpaEntity]. Participants are a read-only
 * projection of the participant data rows, so `CompetitionData.participants` is ignored on write.
 *
 * @since %CURRENT_VERSION%
 */
@Component
@OptIn(InternalDatabaseApi::class)
class CompetitionPersistenceAdapter(
    jpaEntityRepository: CompetitionJpaEntityRepository,
    private val participantDataJpaEntityRepository: ParticipantDataJpaEntityRepository,
    private val contestToCompetitionJpaEntityRepository: ContestToCompetitionJpaEntityRepository,
) : AbstractPersistenceAdapter<CompetitionData, CompetitionId, Competition, CompetitionJpaEntity>(jpaEntityRepository),
    CompetitionRepository {

    private val competitionJpaEntityRepository: CompetitionJpaEntityRepository = jpaEntityRepository

    @Transactional(readOnly = true)
    override fun findAvailableToManager(
        ownerId: MultipleRoleUserId,
        pagination: Pagination,
        filter: CompetitionFilter,
    ): Page<Competition> {
        val specification = Specification<CompetitionJpaEntity> { entity, _, builder ->
            val predicates = mutableListOf(builder.equal(entity.get<Long>("ownerId"), ownerId.value))
            filter.name?.let { name ->
                predicates.add(builder.gt(builder.locate(builder.lower(entity.get("name")), name.lowercase()), 0))
            }
            filter.createdFrom?.let { lower ->
                predicates.add(builder.greaterThanOrEqualTo(entity.get<Instant>("createdAt"), lower))
            }
            filter.createdTo?.let { upper ->
                predicates.add(builder.lessThanOrEqualTo(entity.get<Instant>("createdAt"), upper))
            }
            builder.and(*predicates.toTypedArray())
        }
        val orders = pagination.sort.orders.map { order ->
            JpaSort.Order(JpaSort.Direction.valueOf(order.direction.name), order.field)
        }
        val stableOrders = if (orders.any { order -> order.property == "id" }) orders else orders + JpaSort.Order.asc("id")
        val pageable = PageRequest.of(pagination.page, pagination.size, JpaSort.by(stableOrders))
        val page = competitionJpaEntityRepository.findAll(specification, pageable)
        return Page(
            content = assembleAll(page.content),
            pagination = pagination,
            totalElements = page.totalElements,
        )
    }

    @Transactional
    override fun save(data: CompetitionData): Competition {
        val jpaEntity = CompetitionMapping.toJpaEntity(data)
        val savedJpaEntity = jpaEntityRepository.save(jpaEntity)
        val competitionId = savedJpaEntity.requireId()
        val contestsAssociations = CompetitionMapping.toContestAssociations(competitionId, data.contests.ids)
        contestToCompetitionJpaEntityRepository.saveAll(contestsAssociations)
        val domainEntity = CompetitionMapping.toDomain(savedJpaEntity, loadParticipantIds(competitionId), data.contests.ids)
        return domainEntity
    }

    @Transactional
    override fun update(entity: Competition): Competition {
        val currentJpaEntity = jpaEntityRepository.findByIdOrError(entity.id.value)
        val updatedJpaEntity = CompetitionMapping.toJpaEntity(entity, currentJpaEntity)
        val savedJpaEntity = jpaEntityRepository.saveAndFlush(updatedJpaEntity)
        val competitionId = savedJpaEntity.requireId()
        syncContests(competitionId, entity.data.contests.ids)
        val domainEntity = CompetitionMapping.toDomain(savedJpaEntity, loadParticipantIds(competitionId), entity.data.contests.ids)
        return domainEntity
    }

    @Transactional
    override fun removeById(id: CompetitionId) {
        val jpaEntity = jpaEntityRepository.findByIdOrNull(id.value) ?: return
        val competitionId = jpaEntity.requireId()
        contestToCompetitionJpaEntityRepository.deleteAll(contestToCompetitionJpaEntityRepository.findAllByCompetitionId(competitionId))
        jpaEntityRepository.delete(jpaEntity)
    }

    @Transactional
    override fun removeByIds(ids: List<CompetitionId>) = ids.forEach(::removeById)

    override fun assembleAll(rows: List<CompetitionJpaEntity>): List<Competition> = rows.map(::assemble)

    override fun assemble(jpaEntity: CompetitionJpaEntity): Competition {
        val competitionId = jpaEntity.requireId()
        val contestIds = contestToCompetitionJpaEntityRepository.findAllByCompetitionId(competitionId)
            .map { ContestId(it.id.contestId) }
        val domainEntity = CompetitionMapping.toDomain(jpaEntity, loadParticipantIds(competitionId), contestIds)
        return domainEntity
    }

    private fun loadParticipantIds(competitionId: Long): List<SingleRoleUserId> =
        participantDataJpaEntityRepository.findAllByCompetitionId(competitionId).map { SingleRoleUserId(it.userId) }

    private fun syncContests(competitionId: Long, target: List<ContestId>) = syncJoinTable(
        existing = contestToCompetitionJpaEntityRepository.findAllByCompetitionId(competitionId),
        targetKeys = target,
        keyOf = { ContestId(it.id.contestId) },
        buildAssociation = { CompetitionMapping.toContestAssociations(competitionId, listOf(it)).single() },
        deleteAll = { contestToCompetitionJpaEntityRepository.deleteAll(it) },
        saveAll = { contestToCompetitionJpaEntityRepository.saveAll(it) },
    )
}
