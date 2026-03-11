package tech.testsys.domain.contract

import tech.testsys.domain.model.task.Submission
import tech.testsys.domain.model.task.SubmissionId

interface Grader {
    fun sendToGrade(submission: Submission)
    fun subscribeOnGraded(onGraded: (SubmissionId) -> Unit)
}
