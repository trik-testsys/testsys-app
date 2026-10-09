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
import tech.testsys.infra.database.internal.jpa.repository.CompositeJpaEntityRepository
import tech.testsys.infra.database.internal.jpa.repository.LinkedIdRow
import tech.testsys.infra.database.internal.jpa.repository.SnowflakeJpaEntityRepository

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
     * Finds the ids of the competitions [competitionIds] paired with the ids of their contests in one query.
     *
     * @since %CURRENT_VERSION%
     */
    @Query(
        "select new tech.testsys.infra.database.internal.jpa.repository.LinkedIdRow(e.id.competitionId, e.id.contestId) " +
            "from ContestToCompetitionJpaEntity e where e.id.competitionId in :competitionIds",
    )
    fun findLinkedIdsByCompetitionIdIn(@Param("competitionIds") competitionIds: Collection<Long>): List<LinkedIdRow>

    /**
     * Finds one [pageable] page of the association rows of the competition [competitionId].
     *
     * @since %CURRENT_VERSION%
     */
    @Query("select e from ContestToCompetitionJpaEntity e where e.id.competitionId = :competitionId")
    fun findAllByCompetitionId(@Param("competitionId") competitionId: Long, pageable: Pageable): Page<ContestToCompetitionJpaEntity>

    /**
     * Finds the ids of the competitions containing any of the contests [contestIds], without repetitions.
     *
     * @since %CURRENT_VERSION%
     */
    @Query("select distinct e.id.competitionId from ContestToCompetitionJpaEntity e where e.id.contestId in :contestIds")
    fun findCompetitionIdsByContestIds(@Param("contestIds") contestIds: Collection<Long>): List<Long>
}

/**
 * Spring Data repository for [CompetitionJpaEntity].
 *
 * @since %CURRENT_VERSION%
 */
@Repository
@InternalDatabaseApi
interface CompetitionJpaEntityRepository : SnowflakeJpaEntityRepository<CompetitionJpaEntity> {

    /**
     * Finds the ids of the users [ownerIds] paired with the ids of the competitions they own in one query.
     *
     * @since %CURRENT_VERSION%
     */
    @Query(
        "select new tech.testsys.infra.database.internal.jpa.repository.LinkedIdRow(e.ownerId, e.id) " +
            "from CompetitionJpaEntity e where e.ownerId in :ownerIds",
    )
    fun findLinkedIdsByOwnerIdIn(@Param("ownerIds") ownerIds: Collection<Long>): List<LinkedIdRow>
}
