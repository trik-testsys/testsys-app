package tech.testsys.operation.util

import tech.testsys.domain.model.LazyEntity
import tech.testsys.domain.model.task.GradingResult
import tech.testsys.domain.model.task.Submission
import tech.testsys.domain.model.task.SubmissionStatus
import tech.testsys.domain.model.task.Verdict
import tech.testsys.domain.model.task.VerdictId
import tech.testsys.operation.annotation.InternalOperationsApi

/**
 * Returns the reference to the current successful verdict of this submission, or `null` if grading has not succeeded.
 *
 * @since %CURRENT_VERSION%
 */
@InternalOperationsApi
fun Submission.successfulVerdict(): LazyEntity<VerdictId, Verdict>? {
    val status = data.status as? SubmissionStatus.Graded
    return (status?.grade as? GradingResult.Success)?.verdict
}
