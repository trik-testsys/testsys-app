package tech.testsys.web.app.service.judge

import tech.testsys.domain.model.task.LogsId
import tech.testsys.domain.model.task.RecordingId
import tech.testsys.domain.model.task.Score
import tech.testsys.domain.model.task.SubmissionId
import tech.testsys.domain.model.task.TaskId
import tech.testsys.domain.model.task.TestId
import tech.testsys.domain.model.task.TestVerdict
import tech.testsys.domain.model.task.Verdict
import tech.testsys.domain.model.task.VerdictId
import java.time.Instant

/**
 * Verdict data for pages, with links replaced by identifiers.
 *
 * @property id the identifier of the verdict.
 * @property createdAt the moment the verdict was created.
 * @property task the identifier of the graded task.
 * @property submission the identifier of the graded submission.
 * @property testVerdicts the outcomes of the tests.
 * @since %CURRENT_VERSION%
 */
data class VerdictVo(
    val id: VerdictId,
    val createdAt: Instant,
    val task: TaskId,
    val submission: SubmissionId,
    val testVerdicts: List<TestVerdictVo>,
)

/**
 * Outcome of one test of a [VerdictVo].
 *
 * @property score the score for the test.
 * @property test the identifier of the test.
 * @property logs the identifier of the grading logs.
 * @property recording the identifier of the video recording, or `null` if there is none.
 * @since %CURRENT_VERSION%
 */
data class TestVerdictVo(
    val score: Score,
    val test: TestId,
    val logs: LogsId,
    val recording: RecordingId?,
)

internal fun Verdict.toVo(): VerdictVo = VerdictVo(
    id = id,
    createdAt = createdAt,
    task = data.task.id,
    submission = data.submission.id,
    testVerdicts = data.testVerdicts.map { testVerdict -> testVerdict.toVo() },
)

private fun TestVerdict.toVo() = TestVerdictVo(score = score, test = test.id, logs = logs.id, recording = recording?.id)
