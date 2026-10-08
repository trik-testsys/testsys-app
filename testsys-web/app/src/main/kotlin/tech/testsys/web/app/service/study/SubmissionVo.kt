package tech.testsys.web.app.service.study

import tech.testsys.domain.model.task.ContestId
import tech.testsys.domain.model.task.GradingResult
import tech.testsys.domain.model.task.JudgmentOrderId
import tech.testsys.domain.model.task.SolutionId
import tech.testsys.domain.model.task.Submission
import tech.testsys.domain.model.task.SubmissionId
import tech.testsys.domain.model.task.SubmissionKind
import tech.testsys.domain.model.task.SubmissionStatus
import tech.testsys.domain.model.task.TaskId
import tech.testsys.domain.model.task.TrikStudioVersion
import tech.testsys.domain.model.task.VerdictId
import tech.testsys.domain.model.user.UserId
import java.time.Instant

/**
 * Submission data for pages, with links replaced by identifiers.
 *
 * @property id the identifier of the submission.
 * @property createdAt the moment the submission was created.
 * @property author the identifier of the author.
 * @property solution the identifier of the submitted solution.
 * @property task the identifier of the task.
 * @property status the grading status.
 * @property kind the purpose of the submission.
 * @property judgmentOrders the identifiers of the judgment orders of the submission.
 * @since %CURRENT_VERSION%
 */
data class SubmissionVo(
    val id: SubmissionId,
    val createdAt: Instant,
    val author: UserId,
    val solution: SolutionId,
    val task: TaskId,
    val status: SubmissionStatusVo,
    val kind: SubmissionKindVo,
    val judgmentOrders: List<JudgmentOrderId>,
)

/**
 * Grading status of a [SubmissionVo].
 *
 * @since %CURRENT_VERSION%
 */
sealed interface SubmissionStatusVo {
    /**
     * The submission waits for grading.
     *
     * @since %CURRENT_VERSION%
     */
    data object Queued : SubmissionStatusVo

    /**
     * The submission is being graded.
     *
     * @since %CURRENT_VERSION%
     */
    data object InProgress : SubmissionStatusVo

    /**
     * Grading has finished.
     *
     * @property grade the result of grading.
     * @since %CURRENT_VERSION%
     */
    data class Graded(val grade: GradingResultVo) : SubmissionStatusVo
}

/**
 * Result of grading a [SubmissionVo].
 *
 * @since %CURRENT_VERSION%
 */
sealed interface GradingResultVo {
    /**
     * Grading produced a verdict.
     *
     * @property verdict the identifier of the verdict.
     * @since %CURRENT_VERSION%
     */
    data class Success(val verdict: VerdictId) : GradingResultVo

    /**
     * Grading failed.
     *
     * @property description the description of the failure.
     * @since %CURRENT_VERSION%
     */
    data class GradingError(val description: String) : GradingResultVo

    /**
     * Grading did not finish in time.
     *
     * @since %CURRENT_VERSION%
     */
    data object Timeout : GradingResultVo
}

/**
 * Purpose of a [SubmissionVo].
 *
 * @since %CURRENT_VERSION%
 */
sealed interface SubmissionKindVo {
    /**
     * Run of a developer solution during task validation.
     *
     * @property trikStudioVersion the TRIK Studio version of the run.
     * @since %CURRENT_VERSION%
     */
    data class DeveloperSolutionTest(val trikStudioVersion: TrikStudioVersion) : SubmissionKindVo

    /**
     * Graded solution sent in a contest.
     *
     * @property contest the identifier of the contest.
     * @since %CURRENT_VERSION%
     */
    data class Grading(val contest: ContestId) : SubmissionKindVo
}

internal fun Submission.toVo(): SubmissionVo = SubmissionVo(
    id = id,
    createdAt = createdAt,
    author = data.author.id,
    solution = data.solution.id,
    task = data.task.id,
    status = data.status.toVo(),
    kind = data.kind.toVo(),
    judgmentOrders = data.judgmentOrders.ids,
)

private fun SubmissionStatus.toVo(): SubmissionStatusVo = when (this) {
    SubmissionStatus.Queued -> SubmissionStatusVo.Queued
    SubmissionStatus.InProgress -> SubmissionStatusVo.InProgress
    is SubmissionStatus.Graded -> SubmissionStatusVo.Graded(grade.toVo())
}

private fun GradingResult.toVo(): GradingResultVo = when (this) {
    is GradingResult.Success -> GradingResultVo.Success(verdict.id)
    is GradingResult.GradingError -> GradingResultVo.GradingError(description)
    GradingResult.Timeout -> GradingResultVo.Timeout
}

private fun SubmissionKind.toVo(): SubmissionKindVo = when (this) {
    is SubmissionKind.DeveloperSolutionTest -> SubmissionKindVo.DeveloperSolutionTest(trikStudioVersion)
    is SubmissionKind.Grading -> SubmissionKindVo.Grading(contest.id)
}
