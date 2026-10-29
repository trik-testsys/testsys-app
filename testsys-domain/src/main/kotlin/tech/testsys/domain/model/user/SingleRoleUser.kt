package tech.testsys.domain.model.user

import tech.testsys.domain.model.LazyEntity
import tech.testsys.domain.model.LazyEntityList
import tech.testsys.domain.model.group.Community
import tech.testsys.domain.model.group.CommunityId
import tech.testsys.domain.model.group.Competition
import tech.testsys.domain.model.group.CompetitionId
import tech.testsys.domain.model.task.Contest
import tech.testsys.domain.model.task.ContestId
import java.time.Instant

/**
 * Identifier of a [SingleRoleUser]; shared by all fixed-role user kinds.
 *
 * @since 1.0.0-beta.1
 */
@JvmInline
value class SingleRoleUserId(
    override val value: Long,
) : UserId

/**
 * Data common to every kind of [SingleRoleUser]: fixed-role users sign in with an access code.
 *
 * @property accessTokenHash the stored access-code representation together with its hashing algorithm.
 * @since 1.0.0-beta.1
 */
interface SingleRoleUserData : UserData {
    val accessTokenHash: AccessTokenHash
}

/**
 * Sealed base of users holding exactly one fixed role, which excludes any other role.
 *
 * @since 1.0.0-beta.1
 */
sealed class SingleRoleUser(
    id: SingleRoleUserId,
    createdAt: Instant,
    data: SingleRoleUserData,
) : User<SingleRoleUserId>(id, createdAt, data)

/**
 * Data of a [Participant].
 *
 * @property competition the competition the participant belongs to; fixed on creation and ignored on update.
 * @since 1.0.0-beta.1
 */
data class ParticipantData(
    override val accessTokenHash: AccessTokenHash,
    override val name: String,
    val competition: LazyEntity<CompetitionId, Competition>,
) : SingleRoleUserData

/**
 * A fixed-role user who belongs to exactly one [Competition] and takes part in its contests.
 *
 * @property data the data of the participant.
 * @since 1.0.0-beta.1
 */
class Participant(
    id: SingleRoleUserId,
    createdAt: Instant,
    val data: ParticipantData,
) : SingleRoleUser(id, createdAt, data)

/**
 * Data of an [Observer].
 *
 * @property community the community the observer is a member of.
 * @property contests the contests assigned to the observer.
 * @since 1.0.0-beta.1
 */
data class ObserverData(
    override val accessTokenHash: AccessTokenHash,
    override val name: String,
    val community: LazyEntity<CommunityId, Community>,
    val contests: LazyEntityList<ContestId, Contest>,
) : SingleRoleUserData

/**
 * A fixed-role user who may view competitions containing any contest assigned to them, limited to those contests.
 *
 * @property data the data of the observer.
 * @since 1.0.0-beta.1
 */
class Observer(
    id: SingleRoleUserId,
    createdAt: Instant,
    val data: ObserverData,
) : SingleRoleUser(id, createdAt, data)

/**
 * Data of a [Supervisor].
 *
 * @since 1.0.0-beta.1
 */
data class SupervisorData(
    override val accessTokenHash: AccessTokenHash,
    override val name: String,
) : SingleRoleUserData

/**
 * A fixed-role user who creates and manages other users.
 *
 * @property data the data of the supervisor.
 * @since 1.0.0-beta.1
 */
class Supervisor(
    id: SingleRoleUserId,
    createdAt: Instant,
    val data: SupervisorData,
) : SingleRoleUser(id, createdAt, data)
