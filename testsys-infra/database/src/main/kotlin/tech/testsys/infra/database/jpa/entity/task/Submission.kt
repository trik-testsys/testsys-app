package tech.testsys.infra.database.jpa.entity.task

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import tech.testsys.infra.database.jpa.entity.JpaSequenceEntity

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
) : JpaSequenceEntity()

/**
 * Possible lifecycle states of a [SubmissionJpaEntity].
 *
 * @see tech.testsys.domain.model.task.SubmissionStatus
 * @since %CURRENT_VERSION%
 */
enum class SubmissionStatusJpaEnum {
    QUEUED,
    IN_PROGRESS,
    GRADED;
}

/**
 * JPA entity representing a submission status domain entity.
 *
 * @see tech.testsys.domain.model.task.SubmissionStatus
 * @since %CURRENT_VERSION%
 */
@Entity
class SubmissionStatusJpaEntity(
    @Enumerated(EnumType.STRING)
    val status: SubmissionStatusJpaEnum,
    val gradingResultId: Long?,
) : JpaSequenceEntity()

/**
 * Possible outcomes of a grading attempt.
 *
 * @see tech.testsys.domain.model.task.GradingResult
 * @since %CURRENT_VERSION%
 */
enum class GradingResultJpaEnum {
    SUCCESS,
    GRADING_ERROR,
    TIMEOUT;
}

/**
 * JPA entity representing a grading result domain entity.
 *
 * @see tech.testsys.domain.model.task.GradingResult
 * @since %CURRENT_VERSION%
 */
@Entity
class GradingResultJpaEntity(
    @Enumerated(EnumType.STRING)
    val gradingResult: GradingResultJpaEnum,
    val verdictId: Long?,
    val description: String?,
) : JpaSequenceEntity()

/**
 * Classifies the purpose of a submission.
 *
 * @see tech.testsys.domain.model.task.SubmissionKind
 * @since %CURRENT_VERSION%
 */
enum class SubmissionKindJpaEnum {
    DEVELOPER_SOLUTION,
    GRADING;
}

/**
 * JPA entity representing a submission kind domain entity.
 *
 * @see tech.testsys.domain.model.task.SubmissionKind
 * @since %CURRENT_VERSION%
 */
@Entity
class SubmissionKindJpaEntity(
    @Enumerated(EnumType.STRING)
    val submissionKind: SubmissionKindJpaEnum,
    val contestId: Long?,
) : JpaSequenceEntity()

/**
 * JPA entity representing a submission domain entity.
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
    val submissionStatusId: Long,
    val submissionKindId: Long,
) : JpaSequenceEntity()