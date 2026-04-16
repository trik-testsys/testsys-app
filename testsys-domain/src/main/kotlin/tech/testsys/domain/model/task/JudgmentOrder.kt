package tech.testsys.domain.model.task

import tech.testsys.domain.model.DomainEntity
import tech.testsys.domain.model.DomainId
import tech.testsys.domain.model.LazyEntity
import tech.testsys.domain.model.user.MultipleRoleUser
import tech.testsys.domain.model.user.MultipleRoleUserId
import tech.testsys.domain.model.user.UserId
import java.time.Instant

@JvmInline
value class JudgmentOrderId(
    override val value: Long
) : DomainId

data class JudgmentOrderData(
    val judge: LazyEntity<MultipleRoleUserId, MultipleRoleUser>,
    val verdict: LazyEntity<VerdictId, Verdict>
)

class JudgmentOrder(
    id: JudgmentOrderId,
    createdAt: Instant,
    val data: JudgmentOrderData,
) : DomainEntity<JudgmentOrderId>(id, createdAt)
