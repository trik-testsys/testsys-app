package tech.testsys.domain.builder.task

import tech.testsys.domain.builder.DomainEntityBuilderTests
import tech.testsys.domain.builder.api.judgmentOrderData
import tech.testsys.domain.model.task.JudgmentOrder
import tech.testsys.domain.model.task.JudgmentOrderData

class JudgmentOrderBuilderTests : DomainEntityBuilderTests<JudgmentOrder, JudgmentOrderData, JudgmentOrderDataBuilder>(
    JudgmentOrderBuilder(),
    JudgmentOrderDataBuilder()
) {
    override fun buildDataWithAllFields() = listOf(judgmentOrderData {
        judge(42)
        verdict(1)
        reason = "manual override"
    })
}
