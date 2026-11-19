package tech.testsys.infra.database.internal.jpa.repository.task

import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.task.RegradeJpaEntity
import tech.testsys.infra.database.internal.jpa.entity.task.SubmissionToRegradeId
import tech.testsys.infra.database.internal.jpa.entity.task.SubmissionToRegradeJpaEntity
import tech.testsys.infra.database.internal.jpa.entity.task.TestToRegradeId
import tech.testsys.infra.database.internal.jpa.entity.task.TestToRegradeJpaEntity
import tech.testsys.infra.database.internal.jpa.repository.CompositeJpaEntityRepository
import tech.testsys.infra.database.internal.jpa.repository.LinkedIdRow
import tech.testsys.infra.database.internal.jpa.repository.SnowflakeJpaEntityRepository
import java.time.Instant

/**
 * Spring Data repository for [TestToRegradeJpaEntity].
 *
 * @since 1.0.0-beta.1
 */
@Repository
@InternalDatabaseApi
interface TestToRegradeJpaEntityRepository : CompositeJpaEntityRepository<TestToRegradeJpaEntity, TestToRegradeId> {

    /**
     * Finds the association rows of the regrade [regradeId].
     *
     * @since 1.0.0-beta.1
     */
    @Query("select e from TestToRegradeJpaEntity e where e.id.regradeId = :regradeId")
    fun findAllByRegradeId(@Param("regradeId") regradeId: Long): List<TestToRegradeJpaEntity>

    /**
     * Finds the ids of the regrades [regradeIds] paired with the ids of their tests in one query.
     *
     * @since 1.0.0-beta.1
     */
    @Query(
        "select new tech.testsys.infra.database.internal.jpa.repository.LinkedIdRow(e.id.regradeId, e.id.testId) " +
            "from TestToRegradeJpaEntity e where e.id.regradeId in :regradeIds",
    )
    fun findLinkedIdsByRegradeIdIn(@Param("regradeIds") regradeIds: Collection<Long>): List<LinkedIdRow>
}

/**
 * Spring Data repository for [SubmissionToRegradeJpaEntity].
 *
 * @since 1.0.0-beta.1
 */
@Repository
@InternalDatabaseApi
interface SubmissionToRegradeJpaEntityRepository :
    CompositeJpaEntityRepository<SubmissionToRegradeJpaEntity, SubmissionToRegradeId> {

    /**
     * Finds the association rows of the regrade [regradeId].
     *
     * @since 1.0.0-beta.1
     */
    @Query("select e from SubmissionToRegradeJpaEntity e where e.id.regradeId = :regradeId")
    fun findAllByRegradeId(@Param("regradeId") regradeId: Long): List<SubmissionToRegradeJpaEntity>

    /**
     * Finds the ids of the regrades [regradeIds] paired with the ids of their submissions in one query.
     *
     * @since 1.0.0-beta.1
     */
    @Query(
        "select new tech.testsys.infra.database.internal.jpa.repository.LinkedIdRow(e.id.regradeId, e.id.submissionId) " +
            "from SubmissionToRegradeJpaEntity e where e.id.regradeId in :regradeIds",
    )
    fun findLinkedIdsByRegradeIdIn(@Param("regradeIds") regradeIds: Collection<Long>): List<LinkedIdRow>
}

/**
 * Spring Data repository for [RegradeJpaEntity].
 *
 * @since 1.0.0-beta.1
 */
@Repository
@InternalDatabaseApi
interface RegradeJpaEntityRepository : SnowflakeJpaEntityRepository<RegradeJpaEntity> {

    /**
     * Finds the regrades of the task [taskId] ordered by creation time and then by id, both ascending.
     *
     * @since 1.0.0-beta.1
     */
    fun findAllByTaskIdOrderByCreatedAtAscIdAsc(taskId: Long): List<RegradeJpaEntity>

    /**
     * Counts, for each of the existing regrades [regradeIds], its distinct submissions with a verdict created not
     * earlier than the regrade and earlier than the next regrade of the same task, ordered by creation time and then
     * by id, whose test verdicts ran exactly on the regrade's tests: on each of them and on no other.
     *
     * @since 1.0.0-beta.1
     */
    @Query(
        """
            select r.id as regradeId, count(distinct v.submissionId) as graded from RegradeJpaEntity r
            left join SubmissionToRegradeJpaEntity s on s.id.regradeId = r.id
            left join VerdictJpaEntity v on v.submissionId = s.id.submissionId and v.createdAt >= r.createdAt
                and not exists (
                    select n.id from RegradeJpaEntity n where n.taskId = r.taskId and n.createdAt <= v.createdAt
                        and (n.createdAt > r.createdAt or (n.createdAt = r.createdAt and n.id > r.id))
                )
                and not exists (
                    select tv.id from TestVerdictJpaEntity tv where tv.verdictId = v.id and not exists (
                        select t.id from TestToRegradeJpaEntity t where t.id.regradeId = r.id and t.id.testId = tv.testId
                    )
                )
                and not exists (
                    select t.id from TestToRegradeJpaEntity t where t.id.regradeId = r.id and not exists (
                        select tv.id from TestVerdictJpaEntity tv where tv.verdictId = v.id and tv.testId = t.id.testId
                    )
                )
            where r.id in :regradeIds
            group by r.id
        """,
    )
    fun countGraded(@Param("regradeIds") regradeIds: Collection<Long>): List<RegradeGradedCountJpaProjection>

    /**
     * Finds the id of the regrade of the task [taskId] immediately preceding the moment [createdAt] and the id [id],
     * ordered by creation time and then by id, if it is not stopped and one of its submissions is queued, in progress or
     * not updated since that regrade was created.
     *
     * @since 1.0.0-beta.5
     */
    @Query(
        """
            select p.id from RegradeJpaEntity p
            where p.taskId = :taskId and p.stoppedAt is null
                and (p.createdAt < :createdAt or (p.createdAt = :createdAt and p.id < :id))
                and not exists (
                    select n.id from RegradeJpaEntity n where n.taskId = p.taskId
                        and (n.createdAt < :createdAt or (n.createdAt = :createdAt and n.id < :id))
                        and (n.createdAt > p.createdAt or (n.createdAt = p.createdAt and n.id > p.id))
                )
                and exists (
                    select s.id from SubmissionToRegradeJpaEntity l, SubmissionJpaEntity s
                    where l.id.regradeId = p.id and s.id = l.id.submissionId and (
                        s.status in (
                            tech.testsys.infra.database.internal.jpa.entity.task.SubmissionStatusJpaEnum.QUEUED,
                            tech.testsys.infra.database.internal.jpa.entity.task.SubmissionStatusJpaEnum.IN_PROGRESS
                        )
                        or s.updatedAt < p.createdAt
                    )
                )
        """,
    )
    fun findPreviousUnfinished(@Param("taskId") taskId: Long, @Param("createdAt") createdAt: Instant, @Param("id") id: Long): Long?
}

/**
 * Number of graded submissions of one regrade, read by [RegradeJpaEntityRepository.countGraded].
 *
 * @property regradeId id of the regrade.
 * @property graded the number of its submissions with a verdict created not earlier than the regrade and earlier than
 *   the next regrade of the same task whose test verdicts ran exactly on the regrade's tests.
 * @since 1.0.0-beta.1
 */
@InternalDatabaseApi
interface RegradeGradedCountJpaProjection {
    val regradeId: Long
    val graded: Long
}
