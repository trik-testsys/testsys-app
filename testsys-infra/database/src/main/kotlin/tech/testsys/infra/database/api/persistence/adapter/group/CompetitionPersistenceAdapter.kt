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
import tech.testsys.infra.database.internal.jpa.repository.group.ParticipantToCompetitionJpaEntityRepository
import tech.testsys.infra.database.internal.mapping.group.CompetitionMapping
import tech.testsys.infra.database.internal.utils.findByIdOrError
import tech.testsys.infra.database.internal.utils.requireId
import tech.testsys.infra.database.internal.utils.syncJoinTable

/**
 * Persistence adapter of [Competition] entities backed by [CompetitionJpaEntity].
 * Participant and contest membership is synced through the join tables on save and update.
 *
 * @since %CURRENT_VERSION%
 */
@Component
@OptIn(InternalDatabaseApi::class)
class CompetitionPersistenceAdapter(
    jpaEntityRepository: CompetitionJpaEntityRepository,
    private val participantToCompetitionJpaEntityRepository: ParticipantToCompetitionJpaEntityRepository,
    private val contestToCompetitionJpaEntityRepository: ContestToCompetitionJpaEntityRepository,
) : AbstractPersistenceAdapter<CompetitionData, CompetitionId, Competition, CompetitionJpaEntity>(jpaEntityRepository),
    CompetitionRepository {

    @Transactional
    override fun save(data: CompetitionData): Competition {
        val jpaEntity = CompetitionMapping.toJpaEntity(data)
        val savedJpaEntity = jpaEntityRepository.save(jpaEntity)
        val competitionId = savedJpaEntity.requireId()

        val participantAssociations = CompetitionMapping.toParticipantAssociations(competitionId, data.participants.ids)
        val contestsAssociations = CompetitionMapping.toContestAssociations(competitionId, data.contests.ids)

        participantToCompetitionJpaEntityRepository.saveAll(participantAssociations)
        contestToCompetitionJpaEntityRepository.saveAll(contestsAssociations)

        val domainEntity = CompetitionMapping.toDomain(savedJpaEntity, data.participants.ids, data.contests.ids)
        return domainEntity
    }

    @Transactional
    override fun update(entity: Competition): Competition {
        val currentJpaEntity = jpaEntityRepository.findByIdOrError(entity.id.value)
        val updatedJpaEntity = CompetitionMapping.toJpaEntity(entity, currentJpaEntity)
        val savedJpaEntity = jpaEntityRepository.saveAndFlush(updatedJpaEntity)

        val competitionId = savedJpaEntity.requireId()

        syncParticipants(competitionId, entity.data.participants.ids)
        syncContests(competitionId, entity.data.contests.ids)

        val domainEntity =
            CompetitionMapping.toDomain(savedJpaEntity, entity.data.participants.ids, entity.data.contests.ids)
        return domainEntity
    }

    override fun assemble(jpaEntity: CompetitionJpaEntity): Competition {
        val competitionId = jpaEntity.requireId()

        val participantIds = participantToCompetitionJpaEntityRepository.findAllByCompetitionId(competitionId)
            .map { SingleRoleUserId(it.id.participantId) }
        val contestIds = contestToCompetitionJpaEntityRepository.findAllByCompetitionId(competitionId)
            .map { ContestId(it.id.contestId) }

        val domainEntity = CompetitionMapping.toDomain(jpaEntity, participantIds, contestIds)
        return domainEntity
    }

    private fun syncParticipants(competitionId: Long, target: List<SingleRoleUserId>) = syncJoinTable(
        existing = participantToCompetitionJpaEntityRepository.findAllByCompetitionId(competitionId),
        targetKeys = target,
        keyOf = { SingleRoleUserId(it.id.participantId) },
        buildAssociation = { CompetitionMapping.toParticipantAssociations(competitionId, listOf(it)).single() },
        deleteAll = { participantToCompetitionJpaEntityRepository.deleteAll(it) },
        saveAll = { participantToCompetitionJpaEntityRepository.saveAll(it) },
    )

    private fun syncContests(competitionId: Long, target: List<ContestId>) = syncJoinTable(
        existing = contestToCompetitionJpaEntityRepository.findAllByCompetitionId(competitionId),
        targetKeys = target,
        keyOf = { ContestId(it.id.contestId) },
        buildAssociation = { CompetitionMapping.toContestAssociations(competitionId, listOf(it)).single() },
        deleteAll = { contestToCompetitionJpaEntityRepository.deleteAll(it) },
        saveAll = { contestToCompetitionJpaEntityRepository.saveAll(it) },
    )
}
