package tech.testsys.domain.builder.task

import tech.testsys.domain.builder.DomainEntityBuilderTests
import tech.testsys.domain.builder.api.contestData
import tech.testsys.domain.model.task.Contest
import tech.testsys.domain.model.task.ContestData
import java.time.Duration
import java.time.Instant

class ContestBuilderTests : DomainEntityBuilderTests<Contest, ContestData, ContestDataBuilder>(
    ContestBuilder(),
    ContestDataBuilder()
) {
    override fun buildDataWithAllFields() = listOf(
        contestData {
            owner(42)
            name = "Test Contest"
            description = "A test contest"
            contestDuration = Duration.ofHours(2)
            attemptDuration = Duration.ofMinutes(30)
            trikStudioVersion("3.0.0")
        },
        contestData {
            owner(42)
            name = "Full Contest"
            description = "A contest with all optional fields"
            contestDuration = Duration.ofHours(3)
            attemptDuration = Duration.ofHours(1)
            trikStudioVersion("4.0.0")
            tasks = mutableListOf()
            startsAt = Instant.ofEpochSecond(1000L)
            sharedTo(listOf(1L, 2L))
        },
    )
}
