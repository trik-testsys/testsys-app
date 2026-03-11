package tech.testsys.infra.database.jpa.entity.task

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table
import tech.testsys.infra.database.jpa.entity.JpaEntity

@Entity
@Table(name = "t_verdict")
class VerdictJpaEntity(
    @Column(name = "score")
    val score: Long,

    @Column(name = "task_id")
    val taskId: Long,

    @Column(name = "submission_id")
    val submissionId: Long
) : JpaEntity()

@Entity
@Table(name = "t_submission")
class SubmissionJpaEntity(
    @Column(name = "author_id")
    val authorId: Long,

    @Column(name = "solution_id")
    val solutionId: Long,

    @Column(name = "task_id")
    val taskId: Long,

    @Column(name = "submission_status_id")
    val submissionStatusId: Long,

    @Column(name = "submission_kind_id")
    val submissionKindId: Long
) : JpaEntity()

enum class SubmissionStatusJpaEnum {
    QUEUED,
    IN_PROGRESS,
    GRADED;
}

@Entity
@Table(name = "t_submission_status")
class SubmissionStatusJpaEntity(
    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    val status: SubmissionStatusJpaEnum,

    @Column(name = "grading_result_id")
    val gradingResultId: Long
) : JpaEntity()

enum class GradingResultJpaEnum {
    SUCCESS,
    GRADING_ERROR,
    TIMEOUT
}

@Entity
@Table(name = "t_grading_result")
class GradingResultJpaEntity(
    @Column(name = "grading_result")
    @Enumerated(EnumType.STRING)
    val gradingResult: GradingResultJpaEnum,

    @Column(name = "description")
    val description: String?,

    @Column(name = "verdict_id")
    val verdictId: Long?
) : JpaEntity()

enum class SubmissionKindJpaEnum {
    GRADING,
    DEVELOPER_SOLUTION
}

@Entity
@Table(name = "t_submission_kind")
class SubmissionKindJpaEntity(
    @Column(name = "submission_kind")
    @Enumerated(EnumType.STRING)
    val submissionKind: SubmissionKindJpaEnum,

    @Column(name = "contest_id")
    val contestId: Long?
) : JpaEntity()