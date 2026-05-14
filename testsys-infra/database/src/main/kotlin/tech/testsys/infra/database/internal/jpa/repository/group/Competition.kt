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
 * Spring Data repository for [ContestToCompetitionJpaEntity] association entities.
 *
 * @since %CURRENT_VERSION%
 */
@Repository
@InternalDatabaseApi
interface ContestToCompetitionJpaEntityRepository :
    CompositeJpaEntityRepository<ContestToCompetitionJpaEntity, ContestToCompetitionId> {

    @Query("select e from ContestToCompetitionJpaEntity e where e.id.contestId = :contestId")
    fun findAllByContestId(@Param("contestId") contestId: Long): List<ContestToCompetitionJpaEntity>

    @Query("select e from ContestToCompetitionJpaEntity e where e.id.contestId = :contestId")
    fun findAllByContestId(@Param("contestId") contestId: Long, pageable: Pageable): Page<ContestToCompetitionJpaEntity>

    @Query("select e from ContestToCompetitionJpaEntity e where e.id.competitionId = :competitionId")
    fun findAllByCompetitionId(@Param("competitionId") competitionId: Long): List<ContestToCompetitionJpaEntity>

    @Query("select e from ContestToCompetitionJpaEntity e where e.id.competitionId = :competitionId")
    fun findAllByCompetitionId(@Param("competitionId") competitionId: Long, pageable: Pageable): Page<ContestToCompetitionJpaEntity>
}

/**
 * Spring Data repository for [ParticipantToCompetitionJpaEntity] association entities.
 *
 * @since %CURRENT_VERSION%
 */
@Repository
@InternalDatabaseApi
interface ParticipantToCompetitionJpaEntityRepository :
    CompositeJpaEntityRepository<ParticipantToCompetitionJpaEntity, ParticipantToCompetitionId> {

    @Query("select e from ParticipantToCompetitionJpaEntity e where e.id.participantId = :participantId")
    fun findAllByParticipantId(@Param("participantId") participantId: Long): List<ParticipantToCompetitionJpaEntity>

    @Query("select e from ParticipantToCompetitionJpaEntity e where e.id.participantId = :participantId")
    fun findAllByParticipantId(@Param("participantId") participantId: Long, pageable: Pageable): Page<ParticipantToCompetitionJpaEntity>

    @Query("select e from ParticipantToCompetitionJpaEntity e where e.id.competitionId = :competitionId")
    fun findAllByCompetitionId(@Param("competitionId") competitionId: Long): List<ParticipantToCompetitionJpaEntity>

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

    fun findAllByOwnerId(ownerId: Long): List<CompetitionJpaEntity>
}
