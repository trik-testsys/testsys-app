package tech.testsys.domain.builder.task

import tech.testsys.domain.builder.DomainEntityBuilderTests
import tech.testsys.domain.builder.api.solutionData
import tech.testsys.domain.model.task.Solution
import tech.testsys.domain.model.task.SolutionData
import java.util.UUID

class SolutionBuilderTests : DomainEntityBuilderTests<Solution, SolutionData, SolutionDataBuilder>(
    SolutionBuilder(),
    SolutionDataBuilder()
) {
    override fun buildDataWithAllFields() = listOf(
        solutionData {
            name = "Solution Python"
            description = "Python solution"
            file("solution.py", byteArrayOf(1, 2, 3))
            language.python()
            versionBucket = UUID.randomUUID()
        },
        solutionData {
            name = "Solution JS"
            description = "JS solution"
            file("solution.js", byteArrayOf(4, 5, 6))
            language.javaScript()
            versionBucket = UUID.randomUUID()
        },
        solutionData {
            name = "Solution Visual"
            description = "Visual solution"
            file("solution.xml", byteArrayOf(7, 8, 9))
            language.visualLanguage()
            versionBucket = UUID.randomUUID()
        },
    )
}
