package tech.testsys.domain.builder.task

import tech.testsys.domain.builder.DomainEntityBuilderTests
import tech.testsys.domain.builder.api.developerSolutionData
import tech.testsys.domain.model.task.DeveloperSolution
import tech.testsys.domain.model.task.DeveloperSolutionData
import tech.testsys.domain.model.task.VersionBucket
import java.util.UUID

class DeveloperSolutionBuilderTests : DomainEntityBuilderTests<DeveloperSolution, DeveloperSolutionData, DeveloperSolutionDataBuilder>(
    DeveloperSolutionBuilder(),
    DeveloperSolutionDataBuilder(),
) {
    override fun buildDataWithAllFields() = listOf(
        developerSolutionData {
            name = "Developer Solution"
            description = "Developer solution description"
            solution(42)
            expectedScore(100)
            versionBucket = VersionBucket(UUID.randomUUID())
        },
    )
}
