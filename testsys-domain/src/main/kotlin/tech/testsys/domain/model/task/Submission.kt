package tech.testsys.domain.model.task

import tech.testsys.domain.model.DomainEntity
import tech.testsys.domain.model.DomainId
import tech.testsys.domain.model.LazyEntity
import tech.testsys.domain.model.LazyEntityList
import tech.testsys.domain.model.user.User
import tech.testsys.domain.model.user.UserId
import java.time.Instant

@JvmInline
value class RecordingId(
    override val value: Long,
) : DomainId

class RecordingData(
    val file: FileData,
)

class Recording(
    id: RecordingId,
    createdAt: Instant,
    val data: RecordingData,
) : DomainEntity<RecordingId>(id, createdAt)

@JvmInline
value class LogsId(
    override val value: Long,
) : DomainId

class LogsData(
    val file: FileData,
)

class Logs(
    id: LogsId,
    createdAt: Instant,
    val data: LogsData,
) : DomainEntity<LogsId>(id, createdAt)

@JvmInline
value class VerdictId(
    override val value: Long,
) : DomainId

data class VerdictData(
    val score: Score,
    val task: LazyEntity<TaskId, Task>,
    val submission: LazyEntity<SubmissionId, Submission>,
    val logs: LazyEntity<LogsId, Logs>?,
    val recording: LazyEntity<RecordingId, Recording>?,
)

class Verdict(
    id: VerdictId,
    createdAt: Instant,
    val data: VerdictData,
) : DomainEntity<VerdictId>(id, createdAt)

@JvmInline
value class SubmissionId(
    override val value: Long,
) : DomainId

sealed interface GradingResult {
    data class Success(val verdict: LazyEntity<VerdictId, Verdict>) : GradingResult
    data class GradingError(val description: String) : GradingResult
    object Timeout : GradingResult
}

sealed interface SubmissionStatus {
    object Queued : SubmissionStatus
    object InProgress : SubmissionStatus
    data class Graded(val grade: GradingResult) : SubmissionStatus
}

sealed interface SubmissionKind {
    object DeveloperSolutionTest : SubmissionKind
    class Grading(val contest: LazyEntity<ContestId, Contest>) : SubmissionKind
}

data class SubmissionData(
    val author: LazyEntity<UserId, User<UserId>>,
    val solution: LazyEntity<SolutionId, Solution>,
    val task: LazyEntity<TaskId, Task>,
    val status: SubmissionStatus,
    val kind: SubmissionKind,
    val judgmentOrders: LazyEntityList<JudgmentOrderId, JudgmentOrder>,
)

class Submission(
    id: SubmissionId,
    createdAt: Instant,
    val data: SubmissionData,
) : DomainEntity<SubmissionId>(id, createdAt)
