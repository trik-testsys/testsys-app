package tech.testsys.web.app.service.judge

import tech.testsys.domain.model.task.JudgmentOrder
import tech.testsys.domain.model.task.JudgmentOrderId
import tech.testsys.domain.model.task.Score
import tech.testsys.domain.model.task.SubmissionId
import tech.testsys.domain.model.user.MultipleRoleUserId
import java.time.Instant

/**
 * Judgment order data for pages, with links replaced by identifiers.
 *
 * @property id the identifier of the order.
 * @property createdAt the moment the order was created.
 * @property judge the identifier of the judge.
 * @property submission the identifier of the judged submission.
 * @property score the score set by the judge.
 * @property reason the reason for the change.
 * @since %CURRENT_VERSION%
 */
data class JudgmentOrderVo(
    val id: JudgmentOrderId,
    val createdAt: Instant,
    val judge: MultipleRoleUserId,
    val submission: SubmissionId,
    val score: Score,
    val reason: String,
)

internal fun JudgmentOrder.toVo(): JudgmentOrderVo = JudgmentOrderVo(
    id = id,
    createdAt = createdAt,
    judge = data.judge.id,
    submission = data.submission.id,
    score = data.score,
    reason = data.reason,
)
