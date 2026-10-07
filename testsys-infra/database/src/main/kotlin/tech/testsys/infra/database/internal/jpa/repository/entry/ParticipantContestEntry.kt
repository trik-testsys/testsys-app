package tech.testsys.infra.database.internal.jpa.repository.entry

import org.springframework.stereotype.Repository
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.entry.ParticipantContestEntryJpaEntity
import tech.testsys.infra.database.internal.jpa.repository.SnowflakeJpaEntityRepository

/**
 * Spring Data repository for [ParticipantContestEntryJpaEntity].
 *
 * @since %CURRENT_VERSION%
 */
@Repository
@InternalDatabaseApi
interface ParticipantContestEntryJpaEntityRepository : SnowflakeJpaEntityRepository<ParticipantContestEntryJpaEntity> {

    /**
     * Finds an entry by its complete context.
     *
     * @since %CURRENT_VERSION%
     */
    fun findByParticipantIdAndCompetitionIdAndContestId(
        participantId: Long,
        competitionId: Long,
        contestId: Long,
    ): ParticipantContestEntryJpaEntity?

    /**
     * Finds entries for the selected contests and context.
     *
     * @since %CURRENT_VERSION%
     */
    fun findAllByParticipantIdAndCompetitionIdAndContestIdIn(
        participantId: Long,
        competitionId: Long,
        contestIds: List<Long>,
    ): List<ParticipantContestEntryJpaEntity>
}
