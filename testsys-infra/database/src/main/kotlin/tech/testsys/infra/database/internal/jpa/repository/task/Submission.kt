package tech.testsys.infra.database.internal.jpa.repository.task

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.task.LogsJpaEntity
import tech.testsys.infra.database.internal.jpa.entity.task.RecordingJpaEntity
import tech.testsys.infra.database.internal.jpa.entity.task.SubmissionJpaEntity
import tech.testsys.infra.database.internal.jpa.entity.task.TestVerdictJpaEntity
import tech.testsys.infra.database.internal.jpa.entity.task.VerdictJpaEntity
import tech.testsys.infra.database.internal.jpa.repository.SnowflakeJpaEntityRepository

/**
 * Spring Data repository for [VerdictJpaEntity].
 *
 * @since %CURRENT_VERSION%
 */
@Repository
@InternalDatabaseApi
interface VerdictJpaEntityRepository : SnowflakeJpaEntityRepository<VerdictJpaEntity> {

    /**
     * Finds a page of current successful grading verdicts, filtering authors before paging and counting.
     * Role data rows identify current roles even when the author has no community memberships.
     *
     * @since %CURRENT_VERSION%
     */
    @Query(
        value = """
            select v from VerdictJpaEntity v, SubmissionJpaEntity s
            where s.id = v.submissionId
              and s.kind = tech.testsys.infra.database.internal.jpa.entity.task.SubmissionKindJpaEnum.GRADING
              and s.status = tech.testsys.infra.database.internal.jpa.entity.task.SubmissionStatusJpaEnum.GRADED
              and s.gradingResult = tech.testsys.infra.database.internal.jpa.entity.task.GradingResultJpaEnum.SUCCESS
              and s.gradingVerdictId = v.id
              and (:authorId is null or s.authorId = :authorId)
              and (s.authorId in (select student.userId from StudentDataJpaEntity student)
                or s.authorId in (select participant.userId from ParticipantDataJpaEntity participant))
        """,
        countQuery = """
            select count(v) from VerdictJpaEntity v, SubmissionJpaEntity s
            where s.id = v.submissionId
              and s.kind = tech.testsys.infra.database.internal.jpa.entity.task.SubmissionKindJpaEnum.GRADING
              and s.status = tech.testsys.infra.database.internal.jpa.entity.task.SubmissionStatusJpaEnum.GRADED
              and s.gradingResult = tech.testsys.infra.database.internal.jpa.entity.task.GradingResultJpaEnum.SUCCESS
              and s.gradingVerdictId = v.id
              and (:authorId is null or s.authorId = :authorId)
              and (s.authorId in (select student.userId from StudentDataJpaEntity student)
                or s.authorId in (select participant.userId from ParticipantDataJpaEntity participant))
        """,
    )
    fun findAvailableToJudge(@Param("authorId") authorId: Long?, pageable: Pageable): Page<VerdictJpaEntity>
}

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
     * Finds the submissions authored by the user [authorId].
     *
     * @since %CURRENT_VERSION%
     */
    fun findAllByAuthorId(authorId: Long): List<SubmissionJpaEntity>
}
