package tech.testsys.domain.builder.task

import tech.testsys.domain.builder.Builder
import tech.testsys.domain.builder.DomainEntityWithDataBuilder
import tech.testsys.domain.builder.util.lazify
import tech.testsys.domain.builder.util.requireField
import tech.testsys.domain.model.group.CommunityId
import tech.testsys.domain.model.task.Contest
import tech.testsys.domain.model.task.ContestData
import tech.testsys.domain.model.task.ContestId
import tech.testsys.domain.model.task.TaskId
import tech.testsys.domain.model.task.TrikStudioVersion
import tech.testsys.domain.model.user.MultipleRoleUserId
import java.time.Duration
import java.time.Instant

/**
 * Builder of [ContestData]. Required: [owner], [name], [description], [contestDuration], [attemptDuration],
 * [trikStudioVersion].
 *
 * @property owner the id of the owning developer, or `null` if not set yet.
 * @property name the name of the contest, or `null` if not set yet.
 * @property description the description of the contest, or `null` if not set yet.
 * @property tasks the ids of the included tasks.
 * @property startsAt the start moment of the contest, or `null` if not scheduled.
 * @property contestDuration the total duration of the contest, or `null` if not set yet.
 * @property attemptDuration the time limit of a single attempt, or `null` if not set yet.
 * @property trikStudioVersion the TRIK Studio version of the contest, or `null` if not set yet.
 * @property sharedTo the ids of the communities the contest is shared to.
 * @since %CURRENT_VERSION%
 */
class ContestDataBuilder : Builder<ContestData> {

    var owner: MultipleRoleUserId? = null

    var name: String? = null

    var description: String? = null

    var tasks = mutableListOf<TaskId>()

    var startsAt: Instant? = null

    var contestDuration: Duration? = null

    var attemptDuration: Duration? = null

    var trikStudioVersion: TrikStudioVersion? = null

    var sharedTo = mutableListOf<CommunityId>()

    /**
     * Sets [owner] from a raw id.
     *
     * @since %CURRENT_VERSION%
     */
    fun owner(owner: Long) {
        this.owner = MultipleRoleUserId(owner)
    }

    /**
     * Sets [trikStudioVersion] from a raw version tag, e.g. `"3.0.0"`.
     *
     * @since %CURRENT_VERSION%
     */
    fun trikStudioVersion(version: String) {
        trikStudioVersion = TrikStudioVersion(
            version = version,
        )
    }

    /**
     * Sets [sharedTo] from raw ids.
     *
     * @since %CURRENT_VERSION%
     */
    fun sharedTo(sharedTo: Iterable<Long>) {
        this.sharedTo = sharedTo.map { CommunityId(it) }.toMutableList()
    }

    override fun build(): ContestData {
        val name = requireField(name) { ::name }
        val description = requireField(description) { ::description }
        val owner = requireField(owner) { ::owner }
        val contestDuration = requireField(contestDuration) { ::contestDuration }
        val attemptDuration = requireField(attemptDuration) { ::attemptDuration }
        val trikStudioVersion = requireField(trikStudioVersion) { ::trikStudioVersion }

        return ContestData(
            owner = owner.lazify(),
            name = name,
            description = description,
            tasks = tasks.lazify(),
            startsAt = startsAt,
            contestDuration = contestDuration,
            attemptDuration = attemptDuration,
            trikStudioVersion = trikStudioVersion,
            sharedTo = sharedTo.lazify(),
        )
    }
}

/**
 * Builder of [Contest] entities. Required: [id], [createdAt], [version], [data].
 *
 * @since %CURRENT_VERSION%
 */
class ContestBuilder : DomainEntityWithDataBuilder<Contest, ContestData, ContestDataBuilder>() {

    override fun dataBuilder() = ContestDataBuilder()

    override fun build(): Contest {
        val id = requireField(id) { ::id }
        val createdAt = requireField(createdAt) { ::createdAt }
        val version = requireField(version) { ::version }
        val data = requireField(data) { ::data }

        return Contest(
            id = ContestId(id),
            createdAt = createdAt,
            version = version,
            data = data,
        )
    }
}
