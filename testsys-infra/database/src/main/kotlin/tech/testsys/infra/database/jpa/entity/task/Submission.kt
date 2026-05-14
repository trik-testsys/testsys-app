package tech.testsys.infra.database.jpa.entity.task

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import tech.testsys.infra.database.jpa.entity.SequenceJpaEntity

/**
 * JPA entity representing a verdict domain entity.
 *
 * @see tech.testsys.domain.model.task.Verdict
 * @see tech.testsys.domain.model.task.VerdictData
 * @since %CURRENT_VERSION%
 */
@Entity
class VerdictJpaEntity(
    val score: Int,
    val taskId: Long,
    val submissionId: Long,
    id: Long? = null,
) : SequenceJpaEntity(id)

/**
 * Possible lifecycle states of a [SubmissionJpaEntity].
 *
 * @see tech.testsys.domain.model.task.SubmissionStatus
 * @since %CURRENT_VERSION%
 */
enum class SubmissionStatusJpaEnum {
    QUEUED,
    IN_PROGRESS,
    GRADED,
}

/**
 * Possible outcomes of a grading attempt.
 *
 * Populated only when the parent submission is in [SubmissionStatusJpaEnum.GRADED] state.
 *
 * @see tech.testsys.domain.model.task.GradingResult
 * @since %CURRENT_VERSION%
 */
enum class GradingResultJpaEnum {
    SUCCESS,
    GRADING_ERROR,
    TIMEOUT,
}

/**
 * Classifies the purpose of a submission.
 *
 * @see tech.testsys.domain.model.task.SubmissionKind
 * @since %CURRENT_VERSION%
 */
enum class SubmissionKindJpaEnum {
    DEVELOPER_SOLUTION_TEST,
    GRADING,
}

/**
 * JPA entity representing a submission domain entity.
 *
 * Status, grading result and kind payloads are flattened onto this entity because
 * they do not exist independently of a submission. Nullability of the grading and
 * kind fields encodes the active sealed-interface variant:
 *
 *  - [status] == `GRADED` ⇔ [gradingResult] is non-null;
 *  - [gradingResult] == `SUCCESS` ⇔ [gradingVerdictId] is non-null;
 *  - [gradingResult] == `GRADING_ERROR` ⇔ [gradingErrorDescription] is non-null;
 *  - [kind] == `GRADING` ⇔ [gradingContestId] is non-null.
 *
 * @see tech.testsys.domain.model.task.Submission
 * @see tech.testsys.domain.model.task.SubmissionData
 * @since %CURRENT_VERSION%
 */
@Entity
class SubmissionJpaEntity(
    val authorId: Long,
    val solutionId: Long,
    val taskId: Long,
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
) : SequenceJpaEntity(id)
