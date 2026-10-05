package tech.testsys.domain.model.task

import tech.testsys.domain.model.DomainEntity
import tech.testsys.domain.model.DomainId
import tech.testsys.domain.model.LazyEntity
import tech.testsys.domain.model.LazyEntityList
import tech.testsys.domain.model.group.Community
import tech.testsys.domain.model.group.CommunityId
import tech.testsys.domain.model.user.MultipleRoleUser
import tech.testsys.domain.model.user.MultipleRoleUserId
import java.time.Duration
import java.time.Instant

/**
 * Identifier of a [Contest].
 *
 * @since %CURRENT_VERSION%
 */
@JvmInline
value class ContestId(
    override val value: Long,
) : DomainId

/**
 * Data of a [Contest].
 *
 * @property owner the developer who owns the contest; fixed on creation and ignored on update.
 * @property name the name of the contest.
 * @property description the description of the contest.
 * @property tasks the tasks included in the contest.
 * @property startsAt the moment the contest starts, or `null` if not scheduled yet.
 * @property contestDuration the total duration counted from [startsAt], or `null` if there is no end limit.
 * @property attemptDuration the time limit counted from opening the contest, or `null` if there is no individual limit.
 * @property trikStudioVersion the TRIK Studio version used to run and grade solutions.
 * @property sharedTo the communities the contest is shared to.
 * @property endsAt [startsAt] plus [contestDuration], or `null` if either value is absent.
 * @since %CURRENT_VERSION%
 */
data class ContestData(
    val owner: LazyEntity<MultipleRoleUserId, MultipleRoleUser>,
    val name: String,
    val description: String,
    val tasks: LazyEntityList<TaskId, Task>,
    val startsAt: Instant?,
    val contestDuration: Duration?,
    val attemptDuration: Duration?,
    val trikStudioVersion: TrikStudioVersion,
    val sharedTo: LazyEntityList<CommunityId, Community>,
) {

    val endsAt: Instant? = if (startsAt != null && contestDuration != null) startsAt + contestDuration else null
}

/**
 * A set of tasks with time limits, authored by a developer and solved by students and participants.
 *
 * @property data the data of the contest.
 * @since %CURRENT_VERSION%
 */
class Contest(
    id: ContestId,
    createdAt: Instant,
    val data: ContestData,
) : DomainEntity<ContestId>(id, createdAt)
