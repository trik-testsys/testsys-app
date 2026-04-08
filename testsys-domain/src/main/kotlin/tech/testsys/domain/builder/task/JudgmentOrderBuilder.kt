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

        return JudgmentOrderData(
            judge = judge.lazify(),
            verdict = verdict.lazify(),
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

/**
 * DSL entry point for building [JudgmentOrderData].
 *
 * @param builder the configuration block applied to [JudgmentOrderDataBuilder].
 * @return the constructed [JudgmentOrderData].
 * @since %CURRENT_VERSION%
 */
inline fun buildJudgmentOrderData(builder: JudgmentOrderDataBuilder.() -> Unit) =
    JudgmentOrderDataBuilder().apply(builder).build()

/**
 * DSL entry point for building a [JudgmentOrder].
 *
 * @param builder the configuration block applied to [JudgmentOrderBuilder].
 * @return the constructed [JudgmentOrder].
 * @since %CURRENT_VERSION%
 */
inline fun buildJudgmentOrder(builder: JudgmentOrderBuilder.() -> Unit) =
    JudgmentOrderBuilder().apply(builder).build()
