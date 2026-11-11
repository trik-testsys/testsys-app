package tech.testsys.infra.database.internal.mapping.entry

import tech.testsys.domain.builder.api.participantContestEntry
import tech.testsys.domain.builder.data
import tech.testsys.domain.model.entry.ParticipantContestEntry
import tech.testsys.domain.model.entry.ParticipantContestEntryData
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.entry.ParticipantContestEntryJpaEntity
import tech.testsys.infra.database.internal.mapping.EntityMapping
import tech.testsys.infra.database.internal.utils.populateFields

/**
 * Mapping between [ParticipantContestEntry] and [ParticipantContestEntryJpaEntity].
 *
 * @since 1.0.0-beta.1
 */
@InternalDatabaseApi
object ParticipantContestEntryMapping : EntityMapping<ParticipantContestEntry, ParticipantContestEntryJpaEntity> {

    /**
     * Assembles an entry from [jpaEntity].
     *
     * @since 1.0.0-beta.1
     */
    fun toDomain(jpaEntity: ParticipantContestEntryJpaEntity): ParticipantContestEntry = participantContestEntry {
        populateFields(jpaEntity)
        data {
            participant(jpaEntity.participantId)
            competition(jpaEntity.competitionId)
            contest(jpaEntity.contestId)
            enteredAt = jpaEntity.enteredAt
        }
    }

    /**
     * Creates a new row from [data].
     *
     * @since 1.0.0-beta.1
     */
    fun toJpaEntity(data: ParticipantContestEntryData): ParticipantContestEntryJpaEntity = ParticipantContestEntryJpaEntity(
        participantId = data.participant.id.value,
        competitionId = data.competition.id.value,
        contestId = data.contest.id.value,
        enteredAt = data.enteredAt,
    )
}
