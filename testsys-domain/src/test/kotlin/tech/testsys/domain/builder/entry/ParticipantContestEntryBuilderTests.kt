package tech.testsys.domain.builder.entry

import tech.testsys.domain.builder.DomainEntityBuilderTests
import tech.testsys.domain.builder.api.participantContestEntryData
import tech.testsys.domain.model.entry.ParticipantContestEntry
import tech.testsys.domain.model.entry.ParticipantContestEntryData
import java.time.Instant

class ParticipantContestEntryBuilderTests :
    DomainEntityBuilderTests<ParticipantContestEntry, ParticipantContestEntryData, ParticipantContestEntryDataBuilder>(
        ParticipantContestEntryBuilder(),
        ParticipantContestEntryDataBuilder(),
    ) {
    override fun buildDataWithAllFields() = listOf(
        participantContestEntryData {
            participant(1)
            competition(2)
            contest(3)
            enteredAt = Instant.EPOCH
        },
    )
}
