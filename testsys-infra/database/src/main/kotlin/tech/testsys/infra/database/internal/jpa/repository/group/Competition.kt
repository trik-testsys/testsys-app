package tech.testsys.infra.database.internal.jpa.repository.group

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.group.CompetitionJpaEntity
import tech.testsys.infra.database.internal.jpa.entity.group.ContestToCompetitionId
import tech.testsys.infra.database.internal.jpa.entity.group.ContestToCompetitionJpaEntity
import tech.testsys.infra.database.internal.jpa.entity.group.ParticipantToCompetitionId
import tech.testsys.infra.database.internal.jpa.entity.group.ParticipantToCompetitionJpaEntity
import tech.testsys.infra.database.internal.jpa.repository.CompositeJpaEntityRepository
import tech.testsys.infra.database.internal.jpa.repository.SequenceJpaEntityRepository

/**
 * Spring Data repository for [ContestToCompetitionJpaEntity].
 *
 * @since %CURRENT_VERSION%
 */
@Repository
@InternalDatabaseApi
interface ContestToCompetitionJpaEntityRepository :
    CompositeJpaEntityRepository<ContestToCompetitionJpaEntity, ContestToCompetitionId> {

    /**
     * Finds the association rows of the contest [contestId].
     *
     * @since %CURRENT_VERSION%
     */
    @Query("select e from ContestToCompetitionJpaEntity e where e.id.contestId = :contestId")
    fun findAllByContestId(@Param("contestId") contestId: Long): List<ContestToCompetitionJpaEntity>

    /**
     * Finds one [pageable] page of the association rows of the contest [contestId].
     *
     * @since %CURRENT_VERSION%
     */
    @Query("select e from ContestToCompetitionJpaEntity e where e.id.contestId = :contestId")
    fun findAllByContestId(@Param("contestId") contestId: Long, pageable: Pageable): Page<ContestToCompetitionJpaEntity>

    /**
     * Finds the association rows of the competition [competitionId].
     *
     * @since %CURRENT_VERSION%
     */
    @Query("select e from ContestToCompetitionJpaEntity e where e.id.competitionId = :competitionId")
    fun findAllByCompetitionId(@Param("competitionId") competitionId: Long): List<ContestToCompetitionJpaEntity>

    /**
     * Finds one [pageable] page of the association rows of the competition [competitionId].
     *
     * @since %CURRENT_VERSION%
     */
    @Query("select e from ContestToCompetitionJpaEntity e where e.id.competitionId = :competitionId")
    fun findAllByCompetitionId(@Param("competitionId") competitionId: Long, pageable: Pageable): Page<ContestToCompetitionJpaEntity>
}

/**
 * Spring Data repository for [ParticipantToCompetitionJpaEntity].
 *
 * @since %CURRENT_VERSION%
 */
@Repository
@InternalDatabaseApi
interface ParticipantToCompetitionJpaEntityRepository :
    CompositeJpaEntityRepository<ParticipantToCompetitionJpaEntity, ParticipantToCompetitionId> {

    /**
     * Finds the association rows of the participant [participantId].
     *
     * @since %CURRENT_VERSION%
     */
    @Query("select e from ParticipantToCompetitionJpaEntity e where e.id.participantId = :participantId")
    fun findAllByParticipantId(@Param("participantId") participantId: Long): List<ParticipantToCompetitionJpaEntity>

    /**
     * Finds one [pageable] page of the association rows of the participant [participantId].
     *
     * @since %CURRENT_VERSION%
     */
    @Query("select e from ParticipantToCompetitionJpaEntity e where e.id.participantId = :participantId")
    fun findAllByParticipantId(@Param("participantId") participantId: Long, pageable: Pageable): Page<ParticipantToCompetitionJpaEntity>

    /**
     * Finds the association rows of the competition [competitionId].
     *
     * @since %CURRENT_VERSION%
     */
    @Query("select e from ParticipantToCompetitionJpaEntity e where e.id.competitionId = :competitionId")
    fun findAllByCompetitionId(@Param("competitionId") competitionId: Long): List<ParticipantToCompetitionJpaEntity>

    /**
     * Finds one [pageable] page of the association rows of the competition [competitionId].
     *
     * @since %CURRENT_VERSION%
     */
    @Query("select e from ParticipantToCompetitionJpaEntity e where e.id.competitionId = :competitionId")
    fun findAllByCompetitionId(@Param("competitionId") competitionId: Long, pageable: Pageable): Page<ParticipantToCompetitionJpaEntity>
}

/**
 * Spring Data repository for [CompetitionJpaEntity].
 *
 * @since %CURRENT_VERSION%
 */
@Repository
@InternalDatabaseApi
interface CompetitionJpaEntityRepository : SequenceJpaEntityRepository<CompetitionJpaEntity> {

    /**
     * Finds the competitions owned by the user [ownerId].
     *
     * @since %CURRENT_VERSION%
     */
    fun findAllByOwnerId(ownerId: Long): List<CompetitionJpaEntity>
}
