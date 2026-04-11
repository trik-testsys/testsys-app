package tech.testsys.domain.builder.group

import tech.testsys.domain.builder.Builder
import tech.testsys.domain.builder.DomainEntityWithDataBuilder
import tech.testsys.domain.builder.util.lazify
import tech.testsys.domain.builder.util.requireField
import tech.testsys.domain.model.group.Competition
import tech.testsys.domain.model.group.CompetitionData
import tech.testsys.domain.model.group.CompetitionId
import tech.testsys.domain.model.task.ContestId
import tech.testsys.domain.model.user.MultipleRoleUserId
import tech.testsys.domain.model.user.SingleRoleUserId

/**
 * Builder for constructing [CompetitionData].
 *
 * @since %CURRENT_VERSION%
 */
class CompetitionDataBuilder : Builder<CompetitionData> {

    /**
     * The owner of the competition.
     *
     * @since %CURRENT_VERSION%
     */
    var owner: MultipleRoleUserId? = null

    /**
     * The list of participant user IDs.
     *
     * @since %CURRENT_VERSION%
     */
    var participants = mutableListOf<SingleRoleUserId>()

    /**
     * The list of contest IDs included in this competition.
     *
     * @since %CURRENT_VERSION%
     */
    var contests = mutableListOf<ContestId>()

    /**
     * Sets the [owner] from a raw ID value.
     *
     * @param owner the raw owner ID.
     * @since %CURRENT_VERSION%
     */
    fun owner(owner: Long) {
        this.owner = MultipleRoleUserId(owner)
    }

    /**
     * Sets the [participants] list from raw ID values.
     *
     * @param participants the raw user IDs.
     * @since %CURRENT_VERSION%
     */
    fun participants(participants: Iterable<Long>) {
        this.participants = participants.map { SingleRoleUserId(it) }.toMutableList()
    }

    /**
     * Sets the [contests] list from raw ID values.
     *
     * @param contests the raw contest IDs.
     * @since %CURRENT_VERSION%
     */
    fun contests(contests: Iterable<Long>) {
        this.contests = contests.map { ContestId(it) }.toMutableList()
    }

    /**
     * Builds the [CompetitionData] instance.
     *
     * @return the constructed [CompetitionData].
     * @throws IllegalArgumentException if [owner] is not set.
     * @since %CURRENT_VERSION%
     */
    override fun build(): CompetitionData {
        val owner = requireField(owner) { ::owner }

        return CompetitionData(
            owner = owner.lazify(),
            participants = participants.lazify(),
            contests = contests.lazify(),
        )
    }
}

/**
 * Builder for constructing [Competition] domain entities.
 *
 * @since %CURRENT_VERSION%
 */
class CompetitionBuilder : DomainEntityWithDataBuilder<Competition, CompetitionData, CompetitionDataBuilder>() {

    override fun dataBuilder() = CompetitionDataBuilder()

    /**
     * Builds the [Competition] instance.
     *
     * @return the constructed [Competition].
     * @throws IllegalArgumentException if any required field is not set.
     * @since %CURRENT_VERSION%
     */
    override fun build(): Competition {
        val id = requireField(id) { ::id }
        val createdAt = requireField(createdAt) { ::createdAt }
        val data = requireField(data) { ::data }

        return Competition(
            id = CompetitionId(id),
            createdAt = createdAt,
            data = data,
        )
    }

}

/**
 * DSL entry point for building [CompetitionData].
 *
 * @param builder the configuration block applied to [CompetitionDataBuilder].
 * @return the constructed [CompetitionData].
 * @since %CURRENT_VERSION%
 */
inline fun buildCompetitionData(builder: CompetitionDataBuilder.() -> Unit) =
    CompetitionDataBuilder().apply(builder).build()

/**
 * DSL entry point for building a [Competition].
 *
 * @param builder the configuration block applied to [CompetitionBuilder].
 * @return the constructed [Competition].
 * @since %CURRENT_VERSION%
 */
inline fun buildCompetition(builder: CompetitionBuilder.() -> Unit) =
    CompetitionBuilder().apply(builder).build()
