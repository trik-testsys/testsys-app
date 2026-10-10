package tech.testsys.infra.database.internal.jpa.repository.user.single

import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.user.single.ParticipantDataJpaEntity
import tech.testsys.infra.database.internal.jpa.repository.LinkedIdRow
import tech.testsys.infra.database.internal.jpa.repository.SnowflakeJpaEntityRepository

/**
 * Spring Data repository for [ParticipantDataJpaEntity].
 *
 * @since %CURRENT_VERSION%
 */
@Repository
@InternalDatabaseApi
interface ParticipantDataJpaEntityRepository : SnowflakeJpaEntityRepository<ParticipantDataJpaEntity> {

    /**
     * Finds the participant data row of the user [userId], or `null` if the user does not hold the role.
     *
     * @since %CURRENT_VERSION%
     */
    fun findByUserId(userId: Long): ParticipantDataJpaEntity?

    /**
     * Finds the participant data rows of any of the users [userIds] in one query.
     *
     * @since %CURRENT_VERSION%
     */
    fun findAllByUserIdIn(userIds: Collection<Long>): List<ParticipantDataJpaEntity>

    /**
     * Finds the participant data rows of the competition [competitionId].
     *
     * @since %CURRENT_VERSION%
     */
    fun findAllByCompetitionId(competitionId: Long): List<ParticipantDataJpaEntity>

    /**
     * Finds the ids of the competitions [competitionIds] paired with the ids of their participants in one query.
     *
     * @since %CURRENT_VERSION%
     */
    @Query(
        "select new tech.testsys.infra.database.internal.jpa.repository.LinkedIdRow(e.competitionId, e.userId) " +
            "from ParticipantDataJpaEntity e where e.competitionId in :competitionIds",
    )
    fun findLinkedIdsByCompetitionIdIn(@Param("competitionIds") competitionIds: Collection<Long>): List<LinkedIdRow>
}
