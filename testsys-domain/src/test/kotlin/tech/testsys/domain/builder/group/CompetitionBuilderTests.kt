package tech.testsys.domain.builder.group

import tech.testsys.domain.builder.DomainEntityBuilderTests
import tech.testsys.domain.builder.api.competitionData
import tech.testsys.domain.model.group.Competition
import tech.testsys.domain.model.group.CompetitionData

class CompetitionBuilderTests : DomainEntityBuilderTests<Competition, CompetitionData, CompetitionDataBuilder>(
    CompetitionBuilder(),
    CompetitionDataBuilder()
) {
    override fun buildDataWithAllFields() = listOf(
        competitionData {
            owner(42)
            name = "Competition"
            description = "Competition description"
        },
        competitionData {
            owner(42)
            name = "Full Competition"
            description = "Full competition description"
            participants(listOf(1L, 2L))
            contests(listOf(10L, 20L))
        },
    )
}
