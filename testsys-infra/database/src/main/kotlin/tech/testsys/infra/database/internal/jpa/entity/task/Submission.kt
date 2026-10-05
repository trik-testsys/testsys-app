package tech.testsys.infra.database.internal.jpa.entity.task

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.SnowflakeJpaEntity

/**
 * JPA entity of [tech.testsys.domain.model.task.Verdict].
 *
 * @property taskId id of the graded task.
 * @property submissionId id of the graded [SubmissionJpaEntity].
 * @since %CURRENT_VERSION%
 */
@Entity
@InternalDatabaseApi
class VerdictJpaEntity(
    val taskId: Long,
    val submissionId: Long,
    id: Long? = null,
) : SnowflakeJpaEntity(id)

/**
 * JPA entity of [tech.testsys.domain.model.task.TestVerdict], the outcome of a single test run of a [VerdictJpaEntity].
 *
 * @property verdictId id of the [VerdictJpaEntity] the outcome belongs to.
 * @property testId id of the test the solution was run on.
 * @property score the score awarded for the test.
 * @property logsId id of the [LogsJpaEntity] captured while running the test.
 * @property recordingId id of the [RecordingJpaEntity] captured while running the test, or `null` if none.
 * @since %CURRENT_VERSION%
 */
@Entity
@InternalDatabaseApi
class TestVerdictJpaEntity(
    val verdictId: Long,
    val testId: Long,
    val score: Int,
    val logsId: Long,
    val recordingId: Long?,
    id: Long? = null,
) : SnowflakeJpaEntity(id)

/**
 * Column form of [tech.testsys.domain.model.task.SubmissionStatus], the lifecycle state of a [SubmissionJpaEntity].
 *
 * @since %CURRENT_VERSION%
 */
@InternalDatabaseApi
enum class SubmissionStatusJpaEnum {
    QUEUED,
    IN_PROGRESS,
    GRADED,
}

/**
 * Column form of [tech.testsys.domain.model.task.GradingResult]; set only on [SubmissionStatusJpaEnum.GRADED] submissions.
 *
 * @since %CURRENT_VERSION%
 */
@InternalDatabaseApi
enum class GradingResultJpaEnum {
    SUCCESS,
    GRADING_ERROR,
    TIMEOUT,
}

/**
 * Column form of [tech.testsys.domain.model.task.SubmissionKind], the purpose of a [SubmissionJpaEntity].
 *
 * @since %CURRENT_VERSION%
 */
@InternalDatabaseApi
enum class SubmissionKindJpaEnum {
    DEVELOPER_SOLUTION_TEST,
    GRADING,
}

/**
 * JPA entity of [tech.testsys.domain.model.task.Recording], the runtime trace captured while grading.
 *
 * @property fileDataId id of the [FileDataJpaEntity] holding the recording.
 * @since %CURRENT_VERSION%
 */
@Entity
@InternalDatabaseApi
class RecordingJpaEntity(
    val fileDataId: Long,
    id: Long? = null,
) : SnowflakeJpaEntity(id)

/**
 * JPA entity of [tech.testsys.domain.model.task.Logs], the textual output captured while grading.
 *
 * @property fileDataId id of the [FileDataJpaEntity] holding the logs.
 * @since %CURRENT_VERSION%
 */
@Entity
@InternalDatabaseApi
class LogsJpaEntity(
    val fileDataId: Long,
    id: Long? = null,
) : SnowflakeJpaEntity(id)

/**
 * JPA entity of [tech.testsys.domain.model.task.Submission] with status, grading result and kind flattened onto it.
 * Nullability encodes the active variant: [gradingResult] is set iff [status] is `GRADED`, [gradingVerdictId] iff the
 * result is `SUCCESS`, [gradingErrorDescription] iff it is `GRADING_ERROR`, [gradingContestId] iff [kind] is `GRADING`.
 *
 * @property authorId id of the user who submitted the solution.
 * @property solutionId id of the submitted solution.
 * @property taskId id of the task the solution is submitted to.
 * @property trikStudioVersionId id of the [TrikStudioVersionJpaEntity] used for grading.
 * @property status lifecycle state of the submission.
 * @property gradingResult outcome of grading, or `null` while not graded.
 * @property gradingVerdictId id of the verdict of a successful grading, or `null` otherwise.
 * @property gradingErrorDescription description of a grading error, or `null` otherwise.
 * @property kind purpose of the submission.
 * @property gradingContestId id of the contest a grading submission belongs to, or `null` otherwise.
 * @since %CURRENT_VERSION%
 */
@Entity
@InternalDatabaseApi
class SubmissionJpaEntity(
    val authorId: Long,
    val solutionId: Long,
    val taskId: Long,
    val trikStudioVersionId: Long,
    @Enumerated(EnumType.STRING)
    val status: SubmissionStatusJpaEnum,
    @Enumerated(EnumType.STRING)
    val gradingResult: GradingResultJpaEnum?,
    val gradingVerdictId: Long?,
    @field:JdbcTypeCode(SqlTypes.LONG32VARCHAR)
    val gradingErrorDescription: String?,
    @Enumerated(EnumType.STRING)
    val kind: SubmissionKindJpaEnum,
    val gradingContestId: Long?,
    id: Long? = null,
) : SnowflakeJpaEntity(id)
