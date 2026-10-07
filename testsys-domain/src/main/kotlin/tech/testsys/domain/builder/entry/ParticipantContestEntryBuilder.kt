package tech.testsys.domain.builder.entry

import tech.testsys.domain.builder.Builder
import tech.testsys.domain.builder.DomainEntityWithDataBuilder
import tech.testsys.domain.builder.util.applyVersion
import tech.testsys.domain.builder.util.lazify
import tech.testsys.domain.builder.util.requireField
import tech.testsys.domain.model.entry.ParticipantContestEntry
import tech.testsys.domain.model.entry.ParticipantContestEntryData
import tech.testsys.domain.model.entry.ParticipantContestEntryId
import tech.testsys.domain.model.group.CompetitionId
import tech.testsys.domain.model.task.ContestId
import tech.testsys.domain.model.user.SingleRoleUserId
import java.time.Instant

/**
 * Builder of [ParticipantContestEntryData]. Required: [participant], [competition], [contest], [enteredAt].
 *
 * @property participant the context identifier, or null if not set yet.
 * @property competition the context identifier, or null if not set yet.
 * @property contest the context identifier, or null if not set yet.
 * @property enteredAt the first entry moment, or null if not set yet.
 * @since %CURRENT_VERSION%
 */
class ParticipantContestEntryDataBuilder : Builder<ParticipantContestEntryData> {

    var participant: SingleRoleUserId? = null

    var competition: CompetitionId? = null

    var contest: ContestId? = null

    var enteredAt: Instant? = null

    /**
     * Sets [participant] from a raw id.
     *
     * @since %CURRENT_VERSION%
     */
    fun participant(participant: Long) {
        this.participant = SingleRoleUserId(participant)
    }

    /**
     * Sets [competition] from a raw id.
     *
     * @since %CURRENT_VERSION%
     */
    fun competition(competition: Long) {
        this.competition = CompetitionId(competition)
    }

    /**
     * Sets [contest] from a raw id.
     *
     * @since %CURRENT_VERSION%
     */
    fun contest(contest: Long) {
        this.contest = ContestId(contest)
    }

    override fun build(): ParticipantContestEntryData = ParticipantContestEntryData(
        participant = requireField(participant) { ::participant }.lazify(),
        competition = requireField(competition) { ::competition }.lazify(),
        contest = requireField(contest) { ::contest }.lazify(),
        enteredAt = requireField(enteredAt) { ::enteredAt },
    )
}

/**
 * Builder of [ParticipantContestEntry] entities. Required: [id], [createdAt], [data].
 *
 * @since %CURRENT_VERSION%
 */
class ParticipantContestEntryBuilder :
    DomainEntityWithDataBuilder<ParticipantContestEntry, ParticipantContestEntryData, ParticipantContestEntryDataBuilder>() {

    override fun dataBuilder() = ParticipantContestEntryDataBuilder()

    override fun build(): ParticipantContestEntry = ParticipantContestEntry(
        id = ParticipantContestEntryId(requireField(id) { ::id }),
        createdAt = requireField(createdAt) { ::createdAt },
        data = requireField(data) { ::data },
    ).applyVersion(version)
}
