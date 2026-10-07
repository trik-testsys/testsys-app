package tech.testsys.infra.database.api.persistence.adapter.entry

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import tech.testsys.domain.contract.persistence.repository.ParticipantContestEntryRepository
import tech.testsys.domain.model.entry.ParticipantContestEntry
import tech.testsys.domain.model.entry.ParticipantContestEntryData
import tech.testsys.domain.model.entry.ParticipantContestEntryId
import tech.testsys.domain.model.group.CompetitionId
import tech.testsys.domain.model.task.ContestId
import tech.testsys.domain.model.user.SingleRoleUserId
import tech.testsys.infra.database.api.persistence.adapter.AbstractPersistenceAdapter
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.entry.ParticipantContestEntryJpaEntity
import tech.testsys.infra.database.internal.jpa.repository.entry.ParticipantContestEntryJpaEntityRepository
import tech.testsys.infra.database.internal.jpa.repository.user.UserJpaEntityRepository
import tech.testsys.infra.database.internal.mapping.entry.ParticipantContestEntryMapping

/**
 * Persistence adapter of [ParticipantContestEntry] entities backed by [ParticipantContestEntryJpaEntity].
 *
 * @since %CURRENT_VERSION%
 */
@Component
@OptIn(InternalDatabaseApi::class)
class ParticipantContestEntryPersistenceAdapter(
    jpaEntityRepository: ParticipantContestEntryJpaEntityRepository,
    private val users: UserJpaEntityRepository,
) : AbstractPersistenceAdapter<
    ParticipantContestEntryData,
    ParticipantContestEntryId,
    ParticipantContestEntry,
    ParticipantContestEntryJpaEntity,
    >(jpaEntityRepository),
    ParticipantContestEntryRepository {

    private val entries = jpaEntityRepository

    @Transactional
    override fun save(data: ParticipantContestEntryData): ParticipantContestEntry =
        assemble(entries.save(ParticipantContestEntryMapping.toJpaEntity(data)))

    override fun update(entity: ParticipantContestEntry): ParticipantContestEntry = throw UnsupportedOperationException(
        "participantContestEntry ${entity.id.value} cannot be updated: every entry field is fixed on creation",
    )

    @Transactional(readOnly = true)
    override fun findByContext(
        participantId: SingleRoleUserId,
        competitionId: CompetitionId,
        contestId: ContestId,
    ): ParticipantContestEntry? = entries.findByParticipantIdAndCompetitionIdAndContestId(
        participantId = participantId.value,
        competitionId = competitionId.value,
        contestId = contestId.value,
    )?.let { assemble(it) }

    @Transactional(readOnly = true)
    override fun findByContests(
        participantId: SingleRoleUserId,
        competitionId: CompetitionId,
        contestIds: List<ContestId>,
    ): List<ParticipantContestEntry> {
        if (contestIds.isEmpty()) return emptyList()
        return entries.findAllByParticipantIdAndCompetitionIdAndContestIdIn(
            participantId = participantId.value,
            competitionId = competitionId.value,
            contestIds = contestIds.map { it.value },
        ).map { assemble(it) }
    }

    @Transactional
    override fun findOrCreate(data: ParticipantContestEntryData): ParticipantContestEntry {
        requireNotNull(users.lockById(data.participant.id.value)) {
            "user ${data.participant.id.value} does not exist for participantContestEntry"
        }
        val existing = findByContext(
            participantId = data.participant.id,
            competitionId = data.competition.id,
            contestId = data.contest.id,
        )
        return existing ?: save(data)
    }

    override fun assemble(jpaEntity: ParticipantContestEntryJpaEntity): ParticipantContestEntry =
        ParticipantContestEntryMapping.toDomain(jpaEntity)
}
