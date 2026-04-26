package tech.testsys.infra.database.jpa.beans.repository.user.single

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import tech.testsys.infra.database.jpa.beans.repository.CompositeJpaEntityRepository
import tech.testsys.infra.database.jpa.beans.repository.SequenceJpaEntityRepository
import tech.testsys.infra.database.jpa.entity.user.single.CompetitionToObserverId
import tech.testsys.infra.database.jpa.entity.user.single.CompetitionToObserverJpaEntity
import tech.testsys.infra.database.jpa.entity.user.single.ObserverDataJpaEntity

/**
 * Spring Data repository for [CompetitionToObserverJpaEntity] association entities.
 *
 * @since %CURRENT_VERSION%
 */
@Repository
interface CompetitionToObserverJpaEntityRepository : CompositeJpaEntityRepository<CompetitionToObserverJpaEntity, CompetitionToObserverId> {

    @Query("select e from CompetitionToObserverJpaEntity e where e.id.competitionId = :competitionId")
    fun findAllByCompetitionId(@Param("competitionId") competitionId: Long): List<CompetitionToObserverJpaEntity>

    @Query("select e from CompetitionToObserverJpaEntity e where e.id.competitionId = :competitionId")
    fun findAllByCompetitionId(@Param("competitionId") competitionId: Long, pageable: Pageable): Page<CompetitionToObserverJpaEntity>

    @Query("select e from CompetitionToObserverJpaEntity e where e.id.observerId = :observerId")
    fun findAllByObserverId(@Param("observerId") observerId: Long): List<CompetitionToObserverJpaEntity>

    @Query("select e from CompetitionToObserverJpaEntity e where e.id.observerId = :observerId")
    fun findAllByObserverId(@Param("observerId") observerId: Long, pageable: Pageable): Page<CompetitionToObserverJpaEntity>
}

/**
 * Spring Data repository for [ObserverDataJpaEntity].
 *
 * @since %CURRENT_VERSION%
 */
@Repository
interface ObserverDataJpaEntityRepository : SequenceJpaEntityRepository<ObserverDataJpaEntity>
