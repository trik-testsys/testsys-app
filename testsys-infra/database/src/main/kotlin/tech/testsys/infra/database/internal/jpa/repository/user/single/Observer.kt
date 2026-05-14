package tech.testsys.infra.database.internal.jpa.repository.user.single

import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.user.single.CompetitionToObserverId
import tech.testsys.infra.database.internal.jpa.entity.user.single.CompetitionToObserverJpaEntity
import tech.testsys.infra.database.internal.jpa.entity.user.single.ObserverDataJpaEntity
import tech.testsys.infra.database.internal.jpa.repository.CompositeJpaEntityRepository
import tech.testsys.infra.database.internal.jpa.repository.SequenceJpaEntityRepository

/**
 * Spring Data repository for [CompetitionToObserverJpaEntity] association entities.
 *
 * @since %CURRENT_VERSION%
 */
@Repository
@InternalDatabaseApi
interface CompetitionToObserverJpaEntityRepository :
    CompositeJpaEntityRepository<CompetitionToObserverJpaEntity, CompetitionToObserverId> {

    @Query("select e from CompetitionToObserverJpaEntity e where e.id.observerId = :observerId")
    fun findAllByObserverId(@Param("observerId") observerId: Long): List<CompetitionToObserverJpaEntity>
}

/**
 * Spring Data repository for [ObserverDataJpaEntity].
 *
 * @since %CURRENT_VERSION%
 */
@Repository
@InternalDatabaseApi
interface ObserverDataJpaEntityRepository : SequenceJpaEntityRepository<ObserverDataJpaEntity> {

    fun findByUserId(userId: Long): ObserverDataJpaEntity?
}
