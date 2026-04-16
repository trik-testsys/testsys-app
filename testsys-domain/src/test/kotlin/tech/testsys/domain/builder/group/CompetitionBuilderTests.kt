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
        competitionData { owner(42) },
        competitionData {
            owner(42)
            participants(listOf(1L, 2L))
            contests(listOf(10L, 20L))
        },
    )
}
