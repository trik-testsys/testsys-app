package tech.testsys.domain.builder.entry

import tech.testsys.domain.builder.DomainEntityBuilderTests
import tech.testsys.domain.builder.api.studentContestEntryData
import tech.testsys.domain.model.entry.StudentContestEntry
import tech.testsys.domain.model.entry.StudentContestEntryData
import java.time.Instant

class StudentContestEntryBuilderTests :
    DomainEntityBuilderTests<StudentContestEntry, StudentContestEntryData, StudentContestEntryDataBuilder>(
        StudentContestEntryBuilder(),
        StudentContestEntryDataBuilder(),
    ) {
    override fun buildDataWithAllFields() = listOf(
        studentContestEntryData {
            user(1)
            studyClass(2)
            contest(3)
            enteredAt = Instant.EPOCH
        },
    )
}
