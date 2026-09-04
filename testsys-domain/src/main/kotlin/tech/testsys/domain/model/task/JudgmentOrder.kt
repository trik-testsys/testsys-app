package tech.testsys.domain.model.task

import tech.testsys.domain.model.DomainEntity
import tech.testsys.domain.model.DomainId
import tech.testsys.domain.model.EntityVersion
import tech.testsys.domain.model.LazyEntity
import tech.testsys.domain.model.user.MultipleRoleUser
import tech.testsys.domain.model.user.MultipleRoleUserId
import java.time.Instant

/**
 * Identifier of a [JudgmentOrder].
 *
 * @since %CURRENT_VERSION%
 */
@JvmInline
value class JudgmentOrderId(
    override val value: Long,
) : DomainId

/**
 * Data of a [JudgmentOrder].
 *
 * @property judge the user holding the judge role who issued the order.
 * @property verdict the verdict the order applies to.
 * @property reason the judge's justification of the ruling.
 * @since %CURRENT_VERSION%
 */
data class JudgmentOrderData(
    val judge: LazyEntity<MultipleRoleUserId, MultipleRoleUser>,
    val verdict: LazyEntity<VerdictId, Verdict>,
    val reason: String,
)

/**
 * A judge's manual ruling on a [Verdict].
 *
 * @property data the data of the judgment order.
 * @since %CURRENT_VERSION%
 */
class JudgmentOrder(
    id: JudgmentOrderId,
    createdAt: Instant,
    version: EntityVersion,
    val data: JudgmentOrderData,
) : DomainEntity<JudgmentOrderId>(id, createdAt, version)
