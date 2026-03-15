package tech.testsys.domain.model.task

import tech.testsys.domain.model.DomainEntity
import tech.testsys.domain.model.DomainId
import tech.testsys.domain.model.LazyEntity
import tech.testsys.domain.model.LazyEntityList
import tech.testsys.domain.model.user.MultipleRoleUser
import java.time.Instant

@JvmInline
value class VerdictId(
    override val value: Long
) : DomainId

data class VerdictData(
    val score: Int,
    val task: LazyEntity<TaskId, Task>,
    val submission: LazyEntity<SubmissionId, Submission>,
)

class Verdict(
    id: VerdictId,
    createdAt: Instant,
    val data: VerdictData,
) : DomainEntity<VerdictId>(id, createdAt)

@JvmInline
value class SubmissionId(
    override val value: Long
) : DomainId

sealed interface GradingResult {
    data class Success(val verdict: LazyEntity<VerdictId, Verdict>) : GradingResult
    data class GradingError(val description: String) : GradingResult
    object Timeout : GradingResult

    companion object
}

sealed interface SubmissionStatus {
    object Queued : SubmissionStatus
    object InProgress : SubmissionStatus
    data class Graded(val grade: GradingResult) : SubmissionStatus

    companion object
}

sealed interface SubmissionKind {
    object DeveloperSolutionTest : SubmissionKind
    class Grading(val contest: LazyEntity<ContestId, Contest>) : SubmissionKind

    companion object
}

data class SubmissionData(
    val author: MultipleRoleUser,
    val solution: LazyEntity<SolutionId, Solution>,
    val task: LazyEntity<TaskId, Task>,
    val status: SubmissionStatus,
    val kind: SubmissionKind,
    val judgmentOrders: LazyEntityList<JudgmentOrderId, JudgmentOrder>
)

class Submission(
    id: SubmissionId,
    createdAt: Instant,
    val data: SubmissionData,
) : DomainEntity<SubmissionId>(id, createdAt)
