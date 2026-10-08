package tech.testsys.infra.database.internal.jpa.repository.user.single

import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.user.single.ContestToObserverId
import tech.testsys.infra.database.internal.jpa.entity.user.single.ContestToObserverJpaEntity
import tech.testsys.infra.database.internal.jpa.entity.user.single.ObserverDataJpaEntity
import tech.testsys.infra.database.internal.jpa.repository.CompositeJpaEntityRepository
import tech.testsys.infra.database.internal.jpa.repository.LinkedIdRow
import tech.testsys.infra.database.internal.jpa.repository.SnowflakeJpaEntityRepository

/**
 * Spring Data repository for [ContestToObserverJpaEntity].
 *
 * @since %CURRENT_VERSION%
 */
@Repository
@InternalDatabaseApi
interface ContestToObserverJpaEntityRepository :
    CompositeJpaEntityRepository<ContestToObserverJpaEntity, ContestToObserverId> {

    /**
     * Finds the association rows of the observer [observerId].
     *
     * @since %CURRENT_VERSION%
     */
    @Query("select e from ContestToObserverJpaEntity e where e.id.observerId = :observerId")
    fun findAllByObserverId(@Param("observerId") observerId: Long): List<ContestToObserverJpaEntity>

    /**
     * Finds the ids of the observers [observerIds] paired with the ids of the contests they watch in one query.
     *
     * @since %CURRENT_VERSION%
     */
    @Query(
        "select new tech.testsys.infra.database.internal.jpa.repository.LinkedIdRow(e.id.observerId, e.id.contestId) " +
            "from ContestToObserverJpaEntity e where e.id.observerId in :observerIds",
    )
    fun findLinkedIdsByObserverIdIn(@Param("observerIds") observerIds: Collection<Long>): List<LinkedIdRow>

    /**
     * Finds the association rows of the contest [contestId].
     *
     * @since %CURRENT_VERSION%
     */
    @Query("select e from ContestToObserverJpaEntity e where e.id.contestId = :contestId")
    fun findAllByContestId(@Param("contestId") contestId: Long): List<ContestToObserverJpaEntity>
}

/**
 * Spring Data repository for [ObserverDataJpaEntity].
 *
 * @since %CURRENT_VERSION%
 */
@Repository
@InternalDatabaseApi
interface ObserverDataJpaEntityRepository : SnowflakeJpaEntityRepository<ObserverDataJpaEntity> {

    /**
     * Finds the observer data row of the user [userId], or `null` if the user does not hold the role.
     *
     * @since %CURRENT_VERSION%
     */
    fun findByUserId(userId: Long): ObserverDataJpaEntity?

    /**
     * Finds the observer data rows of any of the users [userIds] in one query.
     *
     * @since %CURRENT_VERSION%
     */
    fun findAllByUserIdIn(userIds: Collection<Long>): List<ObserverDataJpaEntity>
}
