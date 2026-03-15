package tech.testsys.infra.database.jpa.entity.task

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import tech.testsys.infra.database.jpa.entity.JpaEntity

/**
 * @since %CURRENT_VERSION%
 */
@Entity
class VerdictJpaEntity(
    val score: Long,
    val taskId: Long,
    val submissionId: Long
) : JpaEntity()

/**
 * @since %CURRENT_VERSION%
 */
enum class SubmissionStatusJpaEnum {
    QUEUED,
    IN_PROGRESS,
    GRADED;
}

/**
 * @since %CURRENT_VERSION%
 */
@Entity
class SubmissionStatusJpaEntity(
    @Enumerated(EnumType.STRING)
    val status: SubmissionStatusJpaEnum,
    val gradingResultId: Long
) : JpaEntity()

/**
 * @since %CURRENT_VERSION%
 */
enum class GradingResultJpaEnum {
    SUCCESS,
    GRADING_ERROR,
    TIMEOUT
}

/**
 * @since %CURRENT_VERSION%
 */
@Entity
class GradingResultJpaEntity(
    @Enumerated(EnumType.STRING)
    val gradingResult: GradingResultJpaEnum,
    val description: String?,
    val verdictId: Long?
) : JpaEntity()

/**
 * @since %CURRENT_VERSION%
 */
enum class SubmissionKindJpaEnum {
    GRADING,
    DEVELOPER_SOLUTION
}

/**
 * @since %CURRENT_VERSION%
 */
@Entity
class SubmissionKindJpaEntity(
    @Enumerated(EnumType.STRING)
    val submissionKind: SubmissionKindJpaEnum,
    val contestId: Long?
) : JpaEntity()

/**
 * @since %CURRENT_VERSION%
 */
@Entity
class SubmissionJpaEntity(
    val authorId: Long,
    val solutionId: Long,
    val taskId: Long,
    val submissionStatusId: Long,
    val submissionKindId: Long
) : JpaEntity()