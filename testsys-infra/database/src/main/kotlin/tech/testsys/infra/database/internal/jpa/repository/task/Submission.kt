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
}
