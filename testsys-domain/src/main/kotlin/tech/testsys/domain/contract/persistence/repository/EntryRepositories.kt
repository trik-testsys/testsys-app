package tech.testsys.domain.contract.persistence.repository

import tech.testsys.domain.model.entry.ParticipantContestEntry
import tech.testsys.domain.model.entry.ParticipantContestEntryData
import tech.testsys.domain.model.entry.ParticipantContestEntryId
import tech.testsys.domain.model.entry.StudentContestEntry
import tech.testsys.domain.model.entry.StudentContestEntryData
import tech.testsys.domain.model.entry.StudentContestEntryId
import tech.testsys.domain.model.group.ClassId
import tech.testsys.domain.model.group.CompetitionId
import tech.testsys.domain.model.task.ContestId
import tech.testsys.domain.model.user.MultipleRoleUserId
import tech.testsys.domain.model.user.SingleRoleUserId

/**
 * Persistence port for [ParticipantContestEntry]. Entries are immutable; update throws [UnsupportedOperationException].
 *
 * @since %CURRENT_VERSION%
 */
interface ParticipantContestEntryRepository :
    EntityRepository<ParticipantContestEntryData, ParticipantContestEntryId, ParticipantContestEntry> {

    /**
     * Reads the full context entry without changing state; storage exceptions propagate.
     *
     * @param participantId the selected participant context identifier.
     * @param competitionId the selected competition context identifier.
     * @param contestId the selected contest context identifier.
     * @return the existing entry, or null when its context has not been entered.
     * @since %CURRENT_VERSION%
     */
    fun findByContext(participantId: SingleRoleUserId, competitionId: CompetitionId, contestId: ContestId): ParticipantContestEntry?

    /**
     * Reads entries for [contestIds] in the selected context; empty input returns an empty list.
     *
     * @param participantId the selected participant context identifier.
     * @param competitionId the selected competition context identifier.
     * @param contestIds the requested contests; unrelated entries are excluded.
     * @return persisted entries in this context; unentered contests have no matching entry.
     * @since %CURRENT_VERSION%
     */
    fun findByContests(
        participantId: SingleRoleUserId,
        competitionId: CompetitionId,
        contestIds: List<ContestId>,
    ): List<ParticipantContestEntry>

    /**
     * Atomically returns the existing context entry or saves [data], preserving the first entry moment.
     * The caller checks access and time constraints; storage exceptions propagate.
     *
     * @param data the required context and candidate first moment; existing entry fields are never replaced.
     * @return the persisted entry, shared by concurrent calls for the same context.
     * @since %CURRENT_VERSION%
     */
    fun findOrCreate(data: ParticipantContestEntryData): ParticipantContestEntry
}

/**
 * Persistence port for [StudentContestEntry]. Entries are immutable; update throws [UnsupportedOperationException].
 *
 * @since %CURRENT_VERSION%
 */
interface StudentContestEntryRepository :
    EntityRepository<StudentContestEntryData, StudentContestEntryId, StudentContestEntry> {

    /**
     * Reads the full context entry without changing state; storage exceptions propagate.
     *
     * @param userId the selected user context identifier.
     * @param studyClassId the selected studyClass context identifier.
     * @param contestId the selected contest context identifier.
     * @return the existing entry, or null when its context has not been entered.
     * @since %CURRENT_VERSION%
     */
    fun findByContext(userId: MultipleRoleUserId, studyClassId: ClassId, contestId: ContestId): StudentContestEntry?

    /**
     * Reads entries for [contestIds] in the selected context; empty input returns an empty list.
     *
     * @param userId the selected user context identifier.
     * @param studyClassId the selected studyClass context identifier.
     * @param contestIds the requested contests; unrelated entries are excluded.
     * @return persisted entries in this context; unentered contests have no matching entry.
     * @since %CURRENT_VERSION%
     */
    fun findByContests(userId: MultipleRoleUserId, studyClassId: ClassId, contestIds: List<ContestId>): List<StudentContestEntry>

    /**
     * Atomically returns the existing context entry or saves [data], preserving the first entry moment.
     * The caller checks access and time constraints; storage exceptions propagate.
     *
     * @param data the required context and candidate first moment; existing entry fields are never replaced.
     * @return the persisted entry, shared by concurrent calls for the same context.
     * @since %CURRENT_VERSION%
     */
    fun findOrCreate(data: StudentContestEntryData): StudentContestEntry
}
