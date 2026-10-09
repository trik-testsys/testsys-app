package tech.testsys.operation.user

import tech.testsys.domain.model.task.Contest
import tech.testsys.domain.model.task.JudgmentOrder
import tech.testsys.domain.model.task.Solution
import tech.testsys.domain.model.task.Submission
import tech.testsys.domain.model.task.Task
import tech.testsys.domain.model.task.Test
import tech.testsys.domain.model.task.Verdict
import tech.testsys.domain.model.user.MultipleRoleUser
import tech.testsys.domain.model.user.User

/**
 * A submission viewed by a judge (testsys.user.multi.judge.viewSolution) together with the entities it refers to.
 *
 * @property submission the viewed submission.
 * @property author the author of the submission.
 * @property task the task of the submission.
 * @property contest the contest the submission was made in.
 * @property solution the submitted solution with its file.
 * @property verdict the current successful verdict, or `null` if the submission has none.
 * @property tests the tests of [verdict] in the order of its test outcomes; empty without a verdict.
 * @property finalScore the score of the last judgment order, or the total score of [verdict] without orders; `null`
 *   without a verdict.
 * @property judgmentOrders the judgment orders of the submission in the order they were issued, each with its judge.
 * @since %CURRENT_VERSION%
 */
data class SubmissionDetails(
    val submission: Submission,
    val author: User<*>,
    val task: Task,
    val contest: Contest,
    val solution: Solution,
    val verdict: Verdict?,
    val tests: List<Test>,
    val finalScore: Long?,
    val judgmentOrders: List<Pair<JudgmentOrder, MultipleRoleUser>>,
)
