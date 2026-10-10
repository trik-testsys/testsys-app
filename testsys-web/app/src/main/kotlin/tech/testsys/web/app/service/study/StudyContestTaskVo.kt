package tech.testsys.web.app.service.study

import tech.testsys.operation.user.StudyOperations
import tech.testsys.web.app.service.TaskVo
import tech.testsys.web.app.service.toVo

/**
 * Task and results shown in an entered contest.
 *
 * @property task the task data without lazy references.
 * @property bestScore the highest final score, or `null` without successful grading.
 * @property lastSubmission the latest submission, or `null` without submissions.
 * @since %CURRENT_VERSION%
 */
data class StudyContestTaskVo(val task: TaskVo, val bestScore: Int?, val lastSubmission: SubmissionVo?)

internal fun StudyOperations.StudyContestTask.toVo(): StudyContestTaskVo =
    StudyContestTaskVo(task = task.toVo(), bestScore = bestScore, lastSubmission = lastSubmission?.toVo())
