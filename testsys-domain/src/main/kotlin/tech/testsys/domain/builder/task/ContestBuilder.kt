package tech.testsys.domain.builder.task

import tech.testsys.domain.builder.Builder
import tech.testsys.domain.builder.DomainEntityWithDataBuilder
import tech.testsys.domain.builder.util.lazify
import tech.testsys.domain.builder.user.MultipleRoleUserBuilder
import tech.testsys.domain.builder.util.requireField
import tech.testsys.domain.model.task.Contest
import tech.testsys.domain.model.task.ContestData
import tech.testsys.domain.model.task.ContestId
import tech.testsys.domain.model.task.TaskId
import tech.testsys.domain.model.task.TrikStudioVersion
import tech.testsys.domain.model.user.MultipleRoleUser
import java.time.Instant
import java.time.Duration

class ContestDataBuilder : Builder<ContestData> {

    var owner: MultipleRoleUser? = null
    var name: String? = null
    var description: String? = null
    var tasks = mutableListOf<TaskId>()
    var startsAt: Instant? = null
    var contestDuration: Duration? = null
    var attemptDuration: Duration? = null
    var trikStudioVersion: TrikStudioVersion? = null

    inline fun owner(builder: MultipleRoleUserBuilder.() -> Unit) {
        owner = MultipleRoleUserBuilder().apply(builder).build()
    }

    fun trikStudioVersion(image: String, tag: String) {
        trikStudioVersion = TrikStudioVersion(
            image = image,
            tag = tag,
        )
    }

    override fun build(): ContestData {
        val owner = requireField(owner, "owner")
        val name = requireField(name, "name")
        val description = requireField(description, "description")
        val contestDuration = requireField(contestDuration, "contestDuration")
        val attemptDuration = requireField(attemptDuration, "attemptDuration")
        val trikStudioVersion = requireField(trikStudioVersion, "trikStudioVersion")

        return ContestData(
            owner = owner,
            name = name,
            description = description,
            tasks = tasks.lazify(),
            startsAt = startsAt,
            contestDuration = contestDuration,
            attemptDuration = attemptDuration,
            trikStudioVersion = trikStudioVersion,
        )
    }
}

class ContestBuilder : DomainEntityWithDataBuilder<Contest, ContestData, ContestDataBuilder>() {

    override fun dataBuilder() = ContestDataBuilder()

    override fun build(): Contest {
        val id = requireField(id, "id")
        val createdAt = requireField(createdAt, "createdAt")
        val data = requireField(data, "data")

        return Contest(
            id = ContestId(id),
            createdAt = createdAt,
            data = data,
        )
    }

}

inline fun buildContestData(builder: ContestDataBuilder.() -> Unit) = ContestDataBuilder().apply(builder).build()

inline fun buildContest(builder: ContestBuilder.() -> Unit) = ContestBuilder().apply(builder).build()
