package tech.testsys.domain.contract.persistence

import tech.testsys.domain.model.task.Score
import tech.testsys.domain.model.task.TaskId
import tech.testsys.domain.model.user.UserId

/**
 * Summary of the grading submissions of one author for one task within a contest.
 *
 * @property authorId the author of the submissions.
 * @property taskId the task the submissions were made for.
 * @property bestScore the highest result among submissions with a successful verdict, or `null` if there are none.
 * @property submissionCount the number of submissions in any grading state.
 * @throws IllegalArgumentException if [submissionCount] is not positive.
 * @since %CURRENT_VERSION%
 */
data class ContestTaskResult(
    val authorId: UserId,
    val taskId: TaskId,
    val bestScore: Score?,
    val submissionCount: Int,
) {

    init {
        require(submissionCount > 0) {
            "Contest result of author ${authorId.value} for task ${taskId.value} must count at least one submission, " +
                "got $submissionCount"
        }
    }
}
