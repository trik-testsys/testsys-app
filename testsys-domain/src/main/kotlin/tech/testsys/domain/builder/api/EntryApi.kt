@file:Suppress("FunctionNaming")

package tech.testsys.domain.builder.api

import tech.testsys.domain.builder.entry.ParticipantContestEntryBuilder
import tech.testsys.domain.builder.entry.ParticipantContestEntryDataBuilder
import tech.testsys.domain.builder.entry.StudentContestEntryBuilder
import tech.testsys.domain.builder.entry.StudentContestEntryDataBuilder
import tech.testsys.domain.builder.util.applyVersion
import tech.testsys.domain.model.entry.ParticipantContestEntry
import tech.testsys.domain.model.entry.ParticipantContestEntryData
import tech.testsys.domain.model.entry.StudentContestEntry
import tech.testsys.domain.model.entry.StudentContestEntryData

/**
 * Builds [ParticipantContestEntryData] with a [ParticipantContestEntryDataBuilder] block.
 *
 * @since %CURRENT_VERSION%
 */
inline fun participantContestEntryData(builder: ParticipantContestEntryDataBuilder.() -> Unit) =
    ParticipantContestEntryDataBuilder().apply(builder).build()

/**
 * Builds a [ParticipantContestEntry] with a [ParticipantContestEntryBuilder] block.
 *
 * @since %CURRENT_VERSION%
 */
inline fun participantContestEntry(builder: ParticipantContestEntryBuilder.() -> Unit) =
    ParticipantContestEntryBuilder().apply(builder).build()

private fun ParticipantContestEntryData.toBuilder(): ParticipantContestEntryDataBuilder {
    val thisData = this
    return ParticipantContestEntryDataBuilder().apply {
        participant = thisData.participant.id
        competition = thisData.competition.id
        contest = thisData.contest.id
        enteredAt = thisData.enteredAt
    }
}

/**
 * Returns a copy of this entry with its data modified by [builder].
 *
 * @since %CURRENT_VERSION%
 */
fun ParticipantContestEntry.withData(builder: ParticipantContestEntryDataBuilder.() -> Unit): ParticipantContestEntry {
    return ParticipantContestEntry(
        id = id,
        createdAt = createdAt,
        data = data.toBuilder().apply(builder).build(),
    ).applyVersion(version)
}

/**
 * Builds [StudentContestEntryData] with a [StudentContestEntryDataBuilder] block.
 *
 * @since %CURRENT_VERSION%
 */
inline fun studentContestEntryData(builder: StudentContestEntryDataBuilder.() -> Unit) =
    StudentContestEntryDataBuilder().apply(builder).build()

/**
 * Builds a [StudentContestEntry] with a [StudentContestEntryBuilder] block.
 *
 * @since %CURRENT_VERSION%
 */
inline fun studentContestEntry(builder: StudentContestEntryBuilder.() -> Unit) = StudentContestEntryBuilder().apply(builder).build()

private fun StudentContestEntryData.toBuilder(): StudentContestEntryDataBuilder {
    val thisData = this
    return StudentContestEntryDataBuilder().apply {
        user = thisData.user.id
        studyClass = thisData.studyClass.id
        contest = thisData.contest.id
        enteredAt = thisData.enteredAt
    }
}

/**
 * Returns a copy of this entry with its data modified by [builder].
 *
 * @since %CURRENT_VERSION%
 */
fun StudentContestEntry.withData(builder: StudentContestEntryDataBuilder.() -> Unit): StudentContestEntry {
    return StudentContestEntry(
        id = id,
        createdAt = createdAt,
        data = data.toBuilder().apply(builder).build(),
    ).applyVersion(version)
}
