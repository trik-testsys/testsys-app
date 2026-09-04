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
 * Builder of [CompetitionData]. Required: [owner], [name], [description].
 *
 * @property name the name of the competition, or `null` if not set yet.
 * @property description the description of the competition, or `null` if not set yet.
 * @property owner the id of the owning manager, or `null` if not set yet.
 * @property participants the ids of the registered participants.
 * @property contests the ids of the contests held within the competition.
 * @since %CURRENT_VERSION%
 */
class CompetitionDataBuilder : Builder<CompetitionData> {

    var name: String? = null

    var description: String? = null

    var owner: MultipleRoleUserId? = null

    var participants = mutableListOf<SingleRoleUserId>()

    var contests = mutableListOf<ContestId>()

    /**
     * Sets [owner] from a raw id.
     *
     * @since %CURRENT_VERSION%
     */
    fun owner(owner: Long) {
        this.owner = MultipleRoleUserId(owner)
    }

    /**
     * Sets [participants] from raw ids.
     *
     * @since %CURRENT_VERSION%
     */
    fun participants(participants: Iterable<Long>) {
        this.participants = participants.map { SingleRoleUserId(it) }.toMutableList()
    }

    /**
     * Sets [contests] from raw ids.
     *
     * @since %CURRENT_VERSION%
     */
    fun contests(contests: Iterable<Long>) {
        this.contests = contests.map { ContestId(it) }.toMutableList()
    }

    override fun build(): CompetitionData {
        val name = requireField(name) { ::name }
        val description = requireField(description) { ::description }
        val owner = requireField(owner) { ::owner }

        return CompetitionData(
            name = name,
            description = description,
            owner = owner.lazify(),
            participants = participants.lazify(),
            contests = contests.lazify(),
        )
    }
}

/**
 * Builder of [Competition] entities. Required: [id], [createdAt], [data].
 *
 * @since %CURRENT_VERSION%
 */
class CompetitionBuilder : DomainEntityWithDataBuilder<Competition, CompetitionData, CompetitionDataBuilder>() {

    override fun dataBuilder() = CompetitionDataBuilder()

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
