package tech.testsys.web.app.service.developer

import tech.testsys.domain.model.task.Regrade
import tech.testsys.domain.model.task.RegradeId
import tech.testsys.domain.model.task.SubmissionId
import tech.testsys.domain.model.task.TaskId
import tech.testsys.domain.model.task.TestId
import java.time.Instant

/**
 * Regrade data for pages, with links replaced by identifiers.
 *
 * @property id the identifier of the regrade.
 * @property createdAt the moment the submissions were sent to grading again.
 * @property task the identifier of the regraded task.
 * @property tests the identifiers of the tests the submissions are graded on.
 * @property submissions the identifiers of the submissions sent to grading again.
 * @since %CURRENT_VERSION%
 */
data class RegradeVo(
    val id: RegradeId,
    val createdAt: Instant,
    val task: TaskId,
    val tests: List<TestId>,
    val submissions: List<SubmissionId>,
)

internal fun Regrade.toVo(): RegradeVo = RegradeVo(
    id = id,
    createdAt = createdAt,
    task = data.task.id,
    tests = data.tests.ids,
    submissions = data.submissions.ids,
)
