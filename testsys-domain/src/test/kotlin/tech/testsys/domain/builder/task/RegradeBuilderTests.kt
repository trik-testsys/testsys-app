package tech.testsys.domain.builder.task

import tech.testsys.domain.builder.DomainEntityBuilderTests
import tech.testsys.domain.builder.api.regradeData
import tech.testsys.domain.model.task.Regrade
import tech.testsys.domain.model.task.RegradeData

class RegradeBuilderTests : DomainEntityBuilderTests<Regrade, RegradeData, RegradeDataBuilder>(
    RegradeBuilder(),
    RegradeDataBuilder(),
) {
    override fun buildDataWithAllFields() = listOf(
        regradeData {
            task(42)
        },
        regradeData {
            task(42)
            tests(listOf(1L, 2L))
            submissions(listOf(3L, 4L))
        },
    )
}
