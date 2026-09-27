package tech.testsys.domain.builder.task

import tech.testsys.domain.builder.DomainEntityBuilderTests
import tech.testsys.domain.builder.api.solutionData
import tech.testsys.domain.model.task.Solution
import tech.testsys.domain.model.task.SolutionData

class SolutionBuilderTests : DomainEntityBuilderTests<Solution, SolutionData, SolutionDataBuilder>(
    SolutionBuilder(),
    SolutionDataBuilder()
) {
    override fun buildDataWithAllFields() = listOf(
        solutionData {
            file("solution.py", byteArrayOf(1, 2, 3))
            language.python()
        },
        solutionData {
            file("solution.js", byteArrayOf(4, 5, 6))
            language.javaScript()
        },
        solutionData {
            file("solution.xml", byteArrayOf(7, 8, 9))
            language.visualLanguage()
        },
    )
}
