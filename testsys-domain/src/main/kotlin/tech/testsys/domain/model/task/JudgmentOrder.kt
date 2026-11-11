package tech.testsys.domain.model.task

import tech.testsys.domain.model.DomainEntity
import tech.testsys.domain.model.DomainId
import tech.testsys.domain.model.LazyEntity
import tech.testsys.domain.model.user.MultipleRoleUser
import tech.testsys.domain.model.user.MultipleRoleUserId
import java.time.Instant

/**
 * Identifier of a [JudgmentOrder].
 *
 * @since 1.0.0-beta.1
 */
@JvmInline
value class JudgmentOrderId(
    override val value: Long,
) : DomainId

/**
 * Data of a [JudgmentOrder].
 *
 * @property judge the user holding the judge role who issued the order; fixed on creation and ignored on update.
 * @property submission the submission the order applies to; fixed on creation and ignored on update.
 * @property score the score the judge awards to the submission.
 * @property reason the judge's justification of the ruling.
 * @since 1.0.0-beta.1
 */
data class JudgmentOrderData(
    val judge: LazyEntity<MultipleRoleUserId, MultipleRoleUser>,
    val submission: LazyEntity<SubmissionId, Submission>,
    val score: Score,
    val reason: String,
)

/**
 * A judge's manual ruling on a [Submission]: a new score replacing the graded one.
 *
 * @property data the data of the judgment order.
 * @since 1.0.0-beta.1
 */
class JudgmentOrder(
    id: JudgmentOrderId,
    createdAt: Instant,
    val data: JudgmentOrderData,
) : DomainEntity<JudgmentOrderId>(id, createdAt)
