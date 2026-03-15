package tech.testsys.domain.builder.task

import tech.testsys.domain.builder.Builder
import tech.testsys.domain.builder.DomainEntityWithDataBuilder
import tech.testsys.domain.builder.util.lazify
import tech.testsys.domain.model.task.JudgmentOrder
import tech.testsys.domain.model.task.JudgmentOrderData
import tech.testsys.domain.model.task.JudgmentOrderId
import tech.testsys.domain.model.task.VerdictId
import tech.testsys.domain.model.user.UserId

class JudgmentOrderDataBuilder : Builder<JudgmentOrderData> {

    var judge: UserId? = null
    var verdict: VerdictId? = null

    fun judge(id: Long) {
        this.judge = UserId(id)
    }

    fun verdict(id: Long) {
        this.verdict = VerdictId(id)
    }

    override fun build(): JudgmentOrderData {
        val judge = requireNotNull(judge)
        val verdict = requireNotNull(verdict)

        return JudgmentOrderData(
            judge = judge.lazify(),
            verdict = verdict.lazify(),
        )
    }

}

class JudgmentOrderBuilder : DomainEntityWithDataBuilder<JudgmentOrder, JudgmentOrderData, JudgmentOrderDataBuilder>() {

    override fun dataBuilder() = JudgmentOrderDataBuilder()

    override fun build(): JudgmentOrder {
        val id = requireNotNull(id)
        val createdAt = requireNotNull(createdAt)
        val data = requireNotNull(data)

        return JudgmentOrder(
            id = JudgmentOrderId(id),
            createdAt = createdAt,
            data = data,
        )
    }

}

inline fun buildJudgmentOrderData(builder: JudgmentOrderDataBuilder.() -> Unit) =
    JudgmentOrderDataBuilder().apply(builder).build()

inline fun buildJudgmentOrder(builder: JudgmentOrderBuilder.() -> Unit) =
    JudgmentOrderBuilder().apply(builder).build()
