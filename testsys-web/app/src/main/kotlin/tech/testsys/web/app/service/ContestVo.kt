package tech.testsys.web.app.service

import tech.testsys.domain.model.group.CommunityId
import tech.testsys.domain.model.task.Contest
import tech.testsys.domain.model.task.ContestId
import tech.testsys.domain.model.task.TaskId
import tech.testsys.domain.model.task.TrikStudioVersion
import tech.testsys.domain.model.user.MultipleRoleUserId
import java.time.Duration
import java.time.Instant

/**
 * Contest data for pages, with links replaced by identifiers.
 *
 * @property id the identifier of the contest.
 * @property createdAt the moment the contest was created.
 * @property owner the identifier of the owner.
 * @property name the name of the contest.
 * @property description the description of the contest.
 * @property tasks the identifiers of the tasks of the contest.
 * @property startsAt the start of the contest, or `null` if not set.
 * @property contestDuration the duration of the contest, or `null` if not set.
 * @property attemptDuration the duration of one attempt, or `null` if not set.
 * @property trikStudioVersion the TRIK Studio version of the contest.
 * @property sharedTo the identifiers of the communities the contest is shared to.
 * @property endsAt the end of the contest, or `null` if the start or the duration is not set.
 * @since %CURRENT_VERSION%
 */
data class ContestVo(
    val id: ContestId,
    val createdAt: Instant,
    val owner: MultipleRoleUserId,
    val name: String,
    val description: String,
    val tasks: List<TaskId>,
    val startsAt: Instant?,
    val contestDuration: Duration?,
    val attemptDuration: Duration?,
    val trikStudioVersion: TrikStudioVersion,
    val sharedTo: List<CommunityId>,
    val endsAt: Instant?,
)

internal fun Contest.toVo(): ContestVo = ContestVo(
    id = id,
    createdAt = createdAt,
    owner = data.owner.id,
    name = data.name,
    description = data.description,
    tasks = data.tasks.ids,
    startsAt = data.startsAt,
    contestDuration = data.contestDuration,
    attemptDuration = data.attemptDuration,
    trikStudioVersion = data.trikStudioVersion,
    sharedTo = data.sharedTo.ids,
    endsAt = data.endsAt,
)
