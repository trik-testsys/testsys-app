package tech.testsys.domain.model.user

import tech.testsys.domain.model.LazyEntity
import tech.testsys.domain.model.LazyEntityList
import tech.testsys.domain.model.group.Community
import tech.testsys.domain.model.group.CommunityId
import tech.testsys.domain.model.group.Competition
import tech.testsys.domain.model.group.CompetitionId
import java.time.Instant

/**
 * Identifier of a [SingleRoleUser]; shared by all fixed-role user kinds.
 *
 * @since %CURRENT_VERSION%
 */
@JvmInline
value class SingleRoleUserId(
    override val value: Long,
) : UserId

/**
 * Sealed base of users holding exactly one fixed role, which excludes any other role.
 *
 * @since %CURRENT_VERSION%
 */
sealed class SingleRoleUser(
    id: SingleRoleUserId,
    createdAt: Instant,
    data: UserData,
) : User<SingleRoleUserId>(id, createdAt, data)

/**
 * Data of a [Participant].
 *
 * @property accessToken the access code the participant logs in with.
 * @property name the name of the participant.
 * @property competition the competition the participant belongs to.
 * @since %CURRENT_VERSION%
 */
data class ParticipantData(
    override val accessToken: String,
    override val name: String,
    val competition: LazyEntity<CompetitionId, Competition>,
) : UserData

/**
 * A fixed-role user who belongs to exactly one [Competition] and takes part in its contests.
 *
 * @property data the data of the participant.
 * @since %CURRENT_VERSION%
 */
class Participant(
    id: SingleRoleUserId,
    createdAt: Instant,
    val data: ParticipantData,
) : SingleRoleUser(id, createdAt, data)

/**
 * Data of an [Observer].
 *
 * @property accessToken the access code the observer logs in with.
 * @property name the name of the observer.
 * @property community the community the observer is a member of.
 * @property competitions the competitions whose results the observer may view.
 * @since %CURRENT_VERSION%
 */
data class ObserverData(
    override val accessToken: String,
    override val name: String,
    val community: LazyEntity<CommunityId, Community>,
    val competitions: LazyEntityList<CompetitionId, Competition>,
) : UserData

/**
 * A fixed-role user who may view the results of the competitions assigned to them.
 *
 * @property data the data of the observer.
 * @since %CURRENT_VERSION%
 */
class Observer(
    id: SingleRoleUserId,
    createdAt: Instant,
    val data: ObserverData,
) : SingleRoleUser(id, createdAt, data)

/**
 * Data of a [Supervisor].
 *
 * @property accessToken the access code the supervisor logs in with.
 * @property name the name of the supervisor.
 * @since %CURRENT_VERSION%
 */
data class SupervisorData(
    override val accessToken: String,
    override val name: String,
) : UserData

/**
 * A fixed-role user who creates and manages other users.
 *
 * @property data the data of the supervisor.
 * @since %CURRENT_VERSION%
 */
class Supervisor(
    id: SingleRoleUserId,
    createdAt: Instant,
    val data: SupervisorData,
) : SingleRoleUser(id, createdAt, data)
