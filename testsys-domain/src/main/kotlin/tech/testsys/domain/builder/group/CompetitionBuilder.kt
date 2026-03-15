package tech.testsys.domain.builder.group

import tech.testsys.domain.builder.Builder
import tech.testsys.domain.builder.DomainEntityWithDataBuilder
import tech.testsys.domain.builder.util.lazify
import tech.testsys.domain.builder.user.MultipleRoleUserBuilder
import tech.testsys.domain.builder.util.requireField
import tech.testsys.domain.model.group.Competition
import tech.testsys.domain.model.group.CompetitionData
import tech.testsys.domain.model.group.CompetitionId
import tech.testsys.domain.model.task.ContestId
import tech.testsys.domain.model.user.MultipleRoleUser
import tech.testsys.domain.model.user.UserId

class CompetitionDataBuilder : Builder<CompetitionData> {

    var owner: MultipleRoleUser? = null
    var participants = mutableListOf<UserId>()
    var contests = mutableListOf<ContestId>()

    inline fun owner(builder: MultipleRoleUserBuilder.() -> Unit) {
        owner = MultipleRoleUserBuilder().apply(builder).build()
    }

    fun participants(participants: Iterable<Long>) {
        this.participants = participants.map { UserId(it) }.toMutableList()
    }

    fun contests(contests: Iterable<Long>) {
        this.contests = contests.map { ContestId(it) }.toMutableList()
    }

    override fun build(): CompetitionData {
        val owner = requireField(owner, "owner")

        return CompetitionData(
            owner = owner,
            participants = participants.lazify(),
            contests = contests.lazify(),
        )
    }
}

class CompetitionBuilder : DomainEntityWithDataBuilder<Competition, CompetitionData, CompetitionDataBuilder>() {

    override fun dataBuilder() = CompetitionDataBuilder()

    override fun build(): Competition {
        val id = requireField(id, "id")
        val createdAt = requireField(createdAt, "createdAt")
        val data = requireField(data, "data")

        return Competition(
            id = CompetitionId(id),
            createdAt = createdAt,
            data = data,
        )
    }

}

inline fun buildCompetitionData(builder: CompetitionDataBuilder.() -> Unit) =
    CompetitionDataBuilder().apply(builder).build()

inline fun buildCompetition(builder: CompetitionBuilder.() -> Unit) =
    CompetitionBuilder().apply(builder).build()
