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
 * Builder for constructing [JudgmentOrderData].
 *
 * @since %CURRENT_VERSION%
 */
class JudgmentOrderDataBuilder : Builder<JudgmentOrderData> {

    /**
     * The ID of the judge assigned to this order.
     *
     * @since %CURRENT_VERSION%
     */
    var judge: MultipleRoleUserId? = null

    /**
     * The ID of the verdict associated with this order.
     *
     * @since %CURRENT_VERSION%
     */
    var verdict: VerdictId? = null

    /**
     * Free-form reasoning describing why this judgment was issued.
     *
     * @since %CURRENT_VERSION%
     */
    var reason: String? = null

    /**
     * Sets the [judge] from a raw ID value.
     *
     * @param id the raw user ID of the judge.
     * @since %CURRENT_VERSION%
     */
    fun judge(id: Long) {
        this.judge = MultipleRoleUserId(id)
    }

    /**
     * Sets the [verdict] from a raw ID value.
     *
     * @param id the raw verdict ID.
     * @since %CURRENT_VERSION%
     */
    fun verdict(id: Long) {
        this.verdict = VerdictId(id)
    }

    /**
     * Builds the [JudgmentOrderData] instance.
     *
     * @return the constructed [JudgmentOrderData].
     * @throws IllegalArgumentException if any required field is not set.
     * @since %CURRENT_VERSION%
     */
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
 * Builder for constructing [JudgmentOrder] domain entities.
 *
 * @since %CURRENT_VERSION%
 */
class JudgmentOrderBuilder : DomainEntityWithDataBuilder<JudgmentOrder, JudgmentOrderData, JudgmentOrderDataBuilder>() {

    override fun dataBuilder() = JudgmentOrderDataBuilder()

    /**
     * Builds the [JudgmentOrder] instance.
     *
     * @return the constructed [JudgmentOrder].
     * @throws IllegalArgumentException if any required field is not set.
     * @since %CURRENT_VERSION%
     */
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
