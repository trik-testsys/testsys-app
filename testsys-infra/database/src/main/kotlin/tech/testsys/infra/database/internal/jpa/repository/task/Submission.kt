package tech.testsys.infra.database.internal.jpa.repository.task

import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.task.LogsJpaEntity
import tech.testsys.infra.database.internal.jpa.entity.task.RecordingJpaEntity
import tech.testsys.infra.database.internal.jpa.entity.task.SubmissionJpaEntity
import tech.testsys.infra.database.internal.jpa.entity.task.SubmissionKindJpaEnum
import tech.testsys.infra.database.internal.jpa.entity.task.TestVerdictJpaEntity
import tech.testsys.infra.database.internal.jpa.entity.task.VerdictJpaEntity
import tech.testsys.infra.database.internal.jpa.repository.LinkedIdRow
import tech.testsys.infra.database.internal.jpa.repository.SnowflakeJpaEntityRepository

/**
 * Spring Data repository for [VerdictJpaEntity].
 *
 * @since %CURRENT_VERSION%
 */
@Repository
@InternalDatabaseApi
interface VerdictJpaEntityRepository : SnowflakeJpaEntityRepository<VerdictJpaEntity>

/**
 * Spring Data repository for [TestVerdictJpaEntity].
 *
 * @since %CURRENT_VERSION%
 */
@Repository
@InternalDatabaseApi
interface TestVerdictJpaEntityRepository : SnowflakeJpaEntityRepository<TestVerdictJpaEntity> {

    /**
     * Finds the test outcomes of the verdict [verdictId], ordered by the id of the test.
     *
     * @since %CURRENT_VERSION%
     */
    fun findAllByVerdictIdOrderByTestIdAsc(verdictId: Long): List<TestVerdictJpaEntity>

    /**
     * Finds test outcomes of [verdictIds] in one query, ordered by verdict id and then test id.
     *
     * @since %CURRENT_VERSION%
     */
    fun findAllByVerdictIdInOrderByVerdictIdAscTestIdAsc(verdictIds: List<Long>): List<TestVerdictJpaEntity>
}

/**
 * Spring Data repository for [RecordingJpaEntity].
 *
 * @since %CURRENT_VERSION%
 */
@Repository
@InternalDatabaseApi
interface RecordingJpaEntityRepository : SnowflakeJpaEntityRepository<RecordingJpaEntity>

/**
 * Spring Data repository for [LogsJpaEntity].
 *
 * @since %CURRENT_VERSION%
 */
@Repository
@InternalDatabaseApi
interface LogsJpaEntityRepository : SnowflakeJpaEntityRepository<LogsJpaEntity>

/**
 * Spring Data repository for [SubmissionJpaEntity].
 *
 * @since %CURRENT_VERSION%
 */
@Repository
@InternalDatabaseApi
interface SubmissionJpaEntityRepository : SnowflakeJpaEntityRepository<SubmissionJpaEntity> {

    /**
     * Finds the ids of the users [authorIds] paired with the ids of the submissions they authored in one query.
     *
     * @since %CURRENT_VERSION%
     */
    @Query(
        "select new tech.testsys.infra.database.internal.jpa.repository.LinkedIdRow(e.authorId, e.id) " +
            "from SubmissionJpaEntity e where e.authorId in :authorIds",
    )
    fun findLinkedIdsByAuthorIdIn(@Param("authorIds") authorIds: Collection<Long>): List<LinkedIdRow>

    /**
     * Finds the submissions of [kind] made to the task [taskId], ordered by id ascending.
     *
     * @since %CURRENT_VERSION%
     */
    fun findAllByTaskIdAndKindOrderByIdAsc(taskId: Long, kind: SubmissionKindJpaEnum): List<SubmissionJpaEntity>

    /**
     * Finds submissions of [kind] by [authorId] for [taskId] in [gradingContestId], ordered by creation time and then id.
     *
     * @since %CURRENT_VERSION%
     */
    fun findAllByAuthorIdAndTaskIdAndKindAndGradingContestIdOrderByCreatedAtAscIdAsc(
        authorId: Long,
        taskId: Long,
        kind: SubmissionKindJpaEnum,
        gradingContestId: Long,
    ): List<SubmissionJpaEntity>

    /**
     * Finds the submissions of [kind] made in the contest [gradingContestId] by any of [authorIds] for any of [taskIds].
     *
     * @since %CURRENT_VERSION%
     */
    fun findAllByKindAndGradingContestIdAndAuthorIdInAndTaskIdIn(
        kind: SubmissionKindJpaEnum,
        gradingContestId: Long,
        authorIds: Collection<Long>,
        taskIds: Collection<Long>,
    ): List<SubmissionJpaEntity>

    /**
     * Counts the grading submissions of the contest [contestId] per task, omitting tasks without submissions.
     *
     * @since %CURRENT_VERSION%
     */
    @Query(
        """
            select s.taskId as taskId, count(s) as submissions from SubmissionJpaEntity s
            where s.kind = tech.testsys.infra.database.internal.jpa.entity.task.SubmissionKindJpaEnum.GRADING
              and s.gradingContestId = :contestId
            group by s.taskId
        """,
    )
    fun countGradingByTask(@Param("contestId") contestId: Long): List<TaskSubmissionCountJpaProjection>

    /**
     * Counts the grading submissions of any of [authorIds] in any of [contestIds] and their distinct authors.
     *
     * @since %CURRENT_VERSION%
     */
    @Query(
        """
            select count(s) as submissions, count(distinct s.authorId) as authors from SubmissionJpaEntity s
            where s.kind = tech.testsys.infra.database.internal.jpa.entity.task.SubmissionKindJpaEnum.GRADING
              and s.authorId in :authorIds
              and s.gradingContestId in :contestIds
        """,
    )
    fun countGrading(
        @Param("authorIds") authorIds: Collection<Long>,
        @Param("contestIds") contestIds: Collection<Long>,
    ): SubmissionCountJpaProjection
}

/**
 * Number of grading submissions of one task, read by [SubmissionJpaEntityRepository.countGradingByTask].
 *
 * @property taskId id of the task.
 * @property submissions the number of submissions of the task.
 * @since %CURRENT_VERSION%
 */
@InternalDatabaseApi
interface TaskSubmissionCountJpaProjection {
    val taskId: Long
    val submissions: Long
}

/**
 * Numbers of grading submissions and their distinct authors, read by [SubmissionJpaEntityRepository.countGrading].
 *
 * @property submissions the number of submissions.
 * @property authors the number of distinct authors.
 * @since %CURRENT_VERSION%
 */
@InternalDatabaseApi
interface SubmissionCountJpaProjection {
    val submissions: Long
    val authors: Long
}
