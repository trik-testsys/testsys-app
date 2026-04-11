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
import java.time.Instant
import java.time.Duration

/**
 * Builder for constructing [ContestData].
 *
 * @since %CURRENT_VERSION%
 */
class ContestDataBuilder : Builder<ContestData> {

    /**
     * The owner of the contest.
     *
     * @since %CURRENT_VERSION%
     */
    var owner: MultipleRoleUserId? = null

    /**
     * The name of the contest.
     *
     * @since %CURRENT_VERSION%
     */
    var name: String? = null

    /**
     * The description of the contest.
     *
     * @since %CURRENT_VERSION%
     */
    var description: String? = null

    /**
     * The list of task IDs included in this contest.
     *
     * @since %CURRENT_VERSION%
     */
    var tasks = mutableListOf<TaskId>()

    /**
     * The start time of the contest, or `null` if not scheduled.
     *
     * @since %CURRENT_VERSION%
     */
    var startsAt: Instant? = null

    /**
     * The total duration of the contest.
     *
     * @since %CURRENT_VERSION%
     */
    var contestDuration: Duration? = null

    /**
     * The duration of each attempt within the contest.
     *
     * @since %CURRENT_VERSION%
     */
    var attemptDuration: Duration? = null

    /**
     * The TRIK Studio version required for this contest.
     *
     * @since %CURRENT_VERSION%
     */
    var trikStudioVersion: TrikStudioVersion? = null

    var sharedTo = mutableListOf<CommunityId>()

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
     * Sets the [trikStudioVersion] from image and tag strings.
     *
     * @since %CURRENT_VERSION%
     */
    fun trikStudioVersion(version: String) {
        trikStudioVersion = TrikStudioVersion(
            version = version,
        )
    }

    fun sharedTo(sharedTo: Iterable<Long>) {
        this.sharedTo = sharedTo.map { CommunityId(it) }.toMutableList()
    }

    /**
     * Builds the [ContestData] instance.
     *
     * @return the constructed [ContestData].
     * @throws IllegalArgumentException if any required field is not set.
     * @since %CURRENT_VERSION%
     */
    override fun build(): ContestData {
        val owner = requireField(owner) { ::owner }
        val name = requireField(name) { ::name }
        val description = requireField(description) { ::description }
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
 * Builder for constructing [Contest] domain entities.
 *
 * @since %CURRENT_VERSION%
 */
class ContestBuilder : DomainEntityWithDataBuilder<Contest, ContestData, ContestDataBuilder>() {

    override fun dataBuilder() = ContestDataBuilder()

    /**
     * Builds the [Contest] instance.
     *
     * @return the constructed [Contest].
     * @throws IllegalArgumentException if any required field is not set.
     * @since %CURRENT_VERSION%
     */
    override fun build(): Contest {
        val id = requireField(id) { ::id }
        val createdAt = requireField(createdAt) { ::createdAt }
        val data = requireField(data) { ::data }

        return Contest(
            id = ContestId(id),
            createdAt = createdAt,
            data = data,
        )
    }

}

/**
 * DSL entry point for building [ContestData].
 *
 * @param builder the configuration block applied to [ContestDataBuilder].
 * @return the constructed [ContestData].
 * @since %CURRENT_VERSION%
 */
inline fun buildContestData(builder: ContestDataBuilder.() -> Unit) = ContestDataBuilder().apply(builder).build()

/**
 * DSL entry point for building a [Contest].
 *
 * @param builder the configuration block applied to [ContestBuilder].
 * @return the constructed [Contest].
 * @since %CURRENT_VERSION%
 */
inline fun buildContest(builder: ContestBuilder.() -> Unit) = ContestBuilder().apply(builder).build()
