package tech.testsys.domain.builder.task

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import tech.testsys.domain.builder.DomainEntityBuilderTests
import tech.testsys.domain.builder.api.contestData
import tech.testsys.domain.model.task.Contest
import tech.testsys.domain.model.task.ContestData
import java.time.Duration
import java.time.Instant

class ContestBuilderTests : DomainEntityBuilderTests<Contest, ContestData, ContestDataBuilder>(
    ContestBuilder(),
    ContestDataBuilder(),
) {
    override fun buildDataWithAllFields() = listOf(
        contestData {
            owner(42)
            name = "Test Contest"
            description = "A test contest"
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

    @ParameterizedTest
    @CsvSource("false,false", "false,true", "true,false", "true,true")
    fun `should compute an end only with both a start and a total duration`(hasStart: Boolean, hasDuration: Boolean) {
        val data = contestData {
            owner(42)
            name = "Contest"
            description = "Description"
            trikStudioVersion("3.0.0")
            startsAt = Instant.ofEpochSecond(1000).takeIf { hasStart }
            contestDuration = Duration.ofSeconds(60).takeIf { hasDuration }
        }

        Assertions.assertEquals(Instant.ofEpochSecond(1060).takeIf { hasStart && hasDuration }, data.endsAt)
        Assertions.assertNull(data.attemptDuration)
    }
}
