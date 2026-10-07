package tech.testsys.domain.builder.api

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import tech.testsys.domain.model.EntityVersion
import java.time.Instant

class EntryApiTests {

    @Nested
    inner class ParticipantContestEntryTests {

        private val original = participantContestEntry {
            id = 71
            createdAt = Instant.EPOCH
            version = EntityVersion(9)
            data = participantContestEntryData {
                participant(1)
                competition(2)
                contest(3)
                enteredAt = Instant.ofEpochSecond(30)
            }
        }

        @Test
        fun `should retain every entry field and version when data is unchanged`() {
            val copied = original.withData {}

            Assertions.assertEquals(original.id, copied.id)
            Assertions.assertEquals(original.createdAt, copied.createdAt)
            Assertions.assertEquals(original.version, copied.version)
            Assertions.assertEquals(original.data.participant.id, copied.data.participant.id)
            Assertions.assertEquals(original.data.competition.id, copied.data.competition.id)
            Assertions.assertEquals(original.data.contest.id, copied.data.contest.id)
            Assertions.assertEquals(original.data.enteredAt, copied.data.enteredAt)
        }

        @Test
        fun `should copy changed entry fields without mutating the original`() {
            val copied = original.withData {
                participant(11)
                competition(12)
                contest(13)
                enteredAt = Instant.ofEpochSecond(40)
            }

            Assertions.assertEquals(11L, copied.data.participant.id.value)
            Assertions.assertEquals(1L, original.data.participant.id.value)
            Assertions.assertEquals(12L, copied.data.competition.id.value)
            Assertions.assertEquals(2L, original.data.competition.id.value)
            Assertions.assertEquals(13L, copied.data.contest.id.value)
            Assertions.assertEquals(3L, original.data.contest.id.value)
            Assertions.assertEquals(Instant.ofEpochSecond(40), copied.data.enteredAt)
            Assertions.assertEquals(Instant.ofEpochSecond(30), original.data.enteredAt)
            Assertions.assertEquals(original.version, copied.version)
        }
    }

    @Nested
    inner class StudentContestEntryTests {

        private val original = studentContestEntry {
            id = 71
            createdAt = Instant.EPOCH
            version = EntityVersion(9)
            data = studentContestEntryData {
                user(1)
                studyClass(2)
                contest(3)
                enteredAt = Instant.ofEpochSecond(30)
            }
        }

        @Test
        fun `should retain every entry field and version when data is unchanged`() {
            val copied = original.withData {}

            Assertions.assertEquals(original.id, copied.id)
            Assertions.assertEquals(original.createdAt, copied.createdAt)
            Assertions.assertEquals(original.version, copied.version)
            Assertions.assertEquals(original.data.user.id, copied.data.user.id)
            Assertions.assertEquals(original.data.studyClass.id, copied.data.studyClass.id)
            Assertions.assertEquals(original.data.contest.id, copied.data.contest.id)
            Assertions.assertEquals(original.data.enteredAt, copied.data.enteredAt)
        }

        @Test
        fun `should copy changed entry fields without mutating the original`() {
            val copied = original.withData {
                user(11)
                studyClass(12)
                contest(13)
                enteredAt = Instant.ofEpochSecond(40)
            }

            Assertions.assertEquals(11L, copied.data.user.id.value)
            Assertions.assertEquals(1L, original.data.user.id.value)
            Assertions.assertEquals(12L, copied.data.studyClass.id.value)
            Assertions.assertEquals(2L, original.data.studyClass.id.value)
            Assertions.assertEquals(13L, copied.data.contest.id.value)
            Assertions.assertEquals(3L, original.data.contest.id.value)
            Assertions.assertEquals(Instant.ofEpochSecond(40), copied.data.enteredAt)
            Assertions.assertEquals(Instant.ofEpochSecond(30), original.data.enteredAt)
            Assertions.assertEquals(original.version, copied.version)
        }
    }
}
