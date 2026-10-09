package tech.testsys.web.app.service.judge

import tech.testsys.domain.model.task.Solution
import tech.testsys.domain.model.task.SolutionId
import tech.testsys.domain.model.task.TrikSupportedLanguage
import tech.testsys.domain.model.user.MultipleRoleUser
import tech.testsys.domain.model.user.Observer
import tech.testsys.domain.model.user.Participant
import tech.testsys.domain.model.user.Supervisor
import tech.testsys.domain.model.user.User
import tech.testsys.domain.model.user.UserId
import tech.testsys.operation.user.SubmissionDetails
import tech.testsys.web.app.service.ContestVo
import tech.testsys.web.app.service.TaskVo
import tech.testsys.web.app.service.developer.TestVo
import tech.testsys.web.app.service.developer.toVo
import tech.testsys.web.app.service.study.SubmissionVo
import tech.testsys.web.app.service.study.toVo
import tech.testsys.web.app.service.toVo
import java.time.Instant

/**
 * User shown to a judge by identifier and nickname.
 *
 * @property id the identifier of the user.
 * @property name the nickname of the user.
 * @since %CURRENT_VERSION%
 */
data class NamedUserVo(val id: UserId, val name: String)

/**
 * Solution data for pages; the file is represented by its name without content.
 *
 * @property id the identifier of the solution.
 * @property createdAt the moment the solution was uploaded.
 * @property fileName the uploaded name of the solution file.
 * @property language the programming language of the solution.
 * @since %CURRENT_VERSION%
 */
data class SolutionVo(val id: SolutionId, val createdAt: Instant, val fileName: String, val language: TrikSupportedLanguage)

/**
 * Submission viewed by a judge (testsys.user.multi.judge.viewSolution), with links replaced by the data pages show.
 *
 * @property submission the submission.
 * @property author the author of the submission.
 * @property task the task of the submission.
 * @property contest the contest the submission was made in.
 * @property solution the submitted solution.
 * @property verdict the current successful verdict, or `null` if the submission has none.
 * @property tests the tests of [verdict] in the order of its test outcomes.
 * @property finalScore the final score, or `null` without a successful verdict.
 * @property judgmentOrders the judgment orders in the order they were issued, each with its judge.
 * @since %CURRENT_VERSION%
 */
data class SubmissionDetailsVo(
    val submission: SubmissionVo,
    val author: NamedUserVo,
    val task: TaskVo,
    val contest: ContestVo,
    val solution: SolutionVo,
    val verdict: VerdictVo?,
    val tests: List<TestVo>,
    val finalScore: Long?,
    val judgmentOrders: List<Pair<JudgmentOrderVo, NamedUserVo>>,
)

internal fun User<*>.toNamedVo(): NamedUserVo {
    val name = when (this) {
        is MultipleRoleUser -> data.name
        is Participant -> data.name
        is Observer -> data.name
        is Supervisor -> data.name
    }
    return NamedUserVo(id = id, name = name)
}

internal fun SubmissionDetails.toVo(): SubmissionDetailsVo = SubmissionDetailsVo(
    submission = submission.toVo(),
    author = author.toNamedVo(),
    task = task.toVo(),
    contest = contest.toVo(),
    solution = solution.toVo(),
    verdict = verdict?.toVo(),
    tests = tests.map { test -> test.toVo() },
    finalScore = finalScore,
    judgmentOrders = judgmentOrders.map { (order, judge) -> order.toVo() to judge.toNamedVo() },
)

private fun Solution.toVo() = SolutionVo(id = id, createdAt = createdAt, fileName = data.file.uploadedFilename, language = data.language)
