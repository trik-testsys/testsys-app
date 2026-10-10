package tech.testsys.web.app.service.judge

import tech.testsys.operation.user.JudgeOperations
import tech.testsys.web.app.service.study.SubmissionVo
import tech.testsys.web.app.service.study.toVo

/**
 * Successful submission and its current results for the judge's table.
 *
 * @property verdict the automatic verdict.
 * @property author the submission author.
 * @property submission the submission data.
 * @property lastJudgment the latest judgment, or `null` without judgments.
 * @property finalScore the latest judgment score or the automatic total.
 * @since %CURRENT_VERSION%
 */
data class JudgeResultVo(
    val verdict: VerdictVo,
    val author: NamedUserVo,
    val submission: SubmissionVo,
    val lastJudgment: JudgmentOrderVo?,
    val finalScore: Long,
)

internal fun JudgeOperations.JudgeResult.toVo(): JudgeResultVo = JudgeResultVo(
    verdict = verdict.toVo(),
    author = author.toNamedVo(),
    submission = submission.toVo(),
    lastJudgment = lastJudgment?.toVo(),
    finalScore = finalScore,
)
