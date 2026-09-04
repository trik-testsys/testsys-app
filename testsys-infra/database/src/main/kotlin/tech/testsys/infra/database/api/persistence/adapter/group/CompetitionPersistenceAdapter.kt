package tech.testsys.infra.database.api.persistence.adapter.group

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import tech.testsys.domain.contract.persistence.repository.CompetitionRepository
import tech.testsys.domain.model.group.Competition
import tech.testsys.domain.model.group.CompetitionData
import tech.testsys.domain.model.group.CompetitionId
import tech.testsys.domain.model.task.ContestId
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

/**
 * Persistence adapter of [Competition] entities backed by [CompetitionJpaEntity].
 * Contest membership is synced through the join table on save and update; participants are a read-only projection
 * of the participant data rows pointing at the competition, so `CompetitionData.participants` is ignored on write.
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
