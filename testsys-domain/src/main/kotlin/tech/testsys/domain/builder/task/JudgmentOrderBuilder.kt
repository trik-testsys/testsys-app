package tech.testsys.domain.builder.task

import tech.testsys.domain.builder.Builder
import tech.testsys.domain.builder.DomainEntityWithDataBuilder
import tech.testsys.domain.builder.util.lazify
import tech.testsys.domain.builder.util.requireField
import tech.testsys.domain.model.task.JudgmentOrder
import tech.testsys.domain.model.task.JudgmentOrderData
import tech.testsys.domain.model.task.JudgmentOrderId
import tech.testsys.domain.model.task.VerdictId
import tech.testsys.domain.model.user.MultipleRoleUserId

/**
 * Builder of [JudgmentOrderData]. Required: [judge], [verdict], [reason].
 *
 * @property judge the id of the issuing judge, or `null` if not set yet.
 * @property verdict the id of the verdict the order applies to, or `null` if not set yet.
 * @property reason the justification of the ruling, or `null` if not set yet.
 * @since %CURRENT_VERSION%
 */
class JudgmentOrderDataBuilder : Builder<JudgmentOrderData> {

    var judge: MultipleRoleUserId? = null

    var verdict: VerdictId? = null

    var reason: String? = null

    /**
     * Sets [judge] from a raw id.
     *
     * @since %CURRENT_VERSION%
     */
    fun judge(id: Long) {
        this.judge = MultipleRoleUserId(id)
    }

    /**
     * Sets [verdict] from a raw id.
     *
     * @since %CURRENT_VERSION%
     */
    fun verdict(id: Long) {
        this.verdict = VerdictId(id)
    }

    override fun build(): JudgmentOrderData {
        val judge = requireField(judge) { ::judge }
        val verdict = requireField(verdict) { ::verdict }
        val reason = requireField(reason) { ::reason }

        return JudgmentOrderData(
            judge = judge.lazify(),
            verdict = verdict.lazify(),
            reason = reason,
        )
    }
}

/**
 * Builder of [JudgmentOrder] entities. Required: [id], [createdAt], [data].
 *
 * @since %CURRENT_VERSION%
 */
class JudgmentOrderBuilder : DomainEntityWithDataBuilder<JudgmentOrder, JudgmentOrderData, JudgmentOrderDataBuilder>() {

    override fun dataBuilder() = JudgmentOrderDataBuilder()

    override fun build(): JudgmentOrder {
        val id = requireField(id) { ::id }
        val createdAt = requireField(createdAt) { ::createdAt }
        val data = requireField(data) { ::data }

        return JudgmentOrder(
            id = JudgmentOrderId(id),
            createdAt = createdAt,
            data = data,
        )
    }
}
