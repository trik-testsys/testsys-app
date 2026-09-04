package tech.testsys.domain.contract

import tech.testsys.domain.model.task.Submission
import tech.testsys.domain.model.task.SubmissionId

/**
 * Port to the external grading system. Grading is asynchronous: [sendToGrade] returns immediately and completion
 * is announced through the callback registered with [subscribeOnGraded].
 *
 * @since %CURRENT_VERSION%
 */
interface Grader {
    /**
     * Hands a submission over to the grader without waiting for the result.
     *
     * @param submission the submission to grade.
     * @since %CURRENT_VERSION%
     */
    fun sendToGrade(submission: Submission)

    /**
     * Registers a callback invoked whenever grading of a submission has finished.
     *
     * @param onGraded the callback receiving the id of the graded submission.
     * @since %CURRENT_VERSION%
     */
    fun subscribeOnGraded(onGraded: (SubmissionId) -> Unit)
}
