package tech.testsys.domain.contract.persistence.repository

import tech.testsys.domain.contract.persistence.Page
import tech.testsys.domain.contract.persistence.Pagination
import tech.testsys.domain.contract.persistence.UserFilter
import tech.testsys.domain.model.group.CommunityId
import tech.testsys.domain.model.group.CompetitionId
import tech.testsys.domain.model.user.AccessTokenHash
import tech.testsys.domain.model.user.CommunityRole
import tech.testsys.domain.model.user.EmailChangeRequest
import tech.testsys.domain.model.user.EmailChangeRequestData
import tech.testsys.domain.model.user.EmailChangeRequestId
import tech.testsys.domain.model.user.MultipleRoleUser
import tech.testsys.domain.model.user.MultipleRoleUserData
import tech.testsys.domain.model.user.MultipleRoleUserId
import tech.testsys.domain.model.user.Observer
import tech.testsys.domain.model.user.ObserverData
import tech.testsys.domain.model.user.Participant
import tech.testsys.domain.model.user.ParticipantData
import tech.testsys.domain.model.user.RegistrationRequest
import tech.testsys.domain.model.user.RegistrationRequestData
import tech.testsys.domain.model.user.RegistrationRequestId
import tech.testsys.domain.model.user.SingleRoleUserId
import tech.testsys.domain.model.user.Supervisor
import tech.testsys.domain.model.user.SupervisorData
import tech.testsys.domain.model.user.User
import tech.testsys.domain.model.user.UserId
import java.time.Instant

/**
 * Port for users of every kind: search of those available to an administrator and records of their logins.
 *
 * @since %CURRENT_VERSION%
 */
interface UserRepository {

    /**
     * Synchronously finds distinct users of communities created by [administratorId], without changing stored state.
     * Repeated calls reflect current data; storage exceptions propagate to the caller.
     *
     * @param administratorId the creator of the communities whose multiple-role members, observers and creator are included.
     * @param pagination the requested page; id ascending is the default order and breaks ties unless explicitly sorted.
     * @param filter conditions combined with AND before paging and counting; foreign or unknown communities match nothing.
     * @return available users of their concrete kinds, the original pagination and exact filtered total; missing pages are empty.
     * @since %CURRENT_VERSION%
     */
    fun findAvailableToAdministrator(
        administratorId: MultipleRoleUserId,
        pagination: Pagination,
        filter: UserFilter = UserFilter(),
    ): Page<User<*>>

    /**
     * Synchronously finds [userId] if it is available to [administratorId], without changing stored state.
     * Storage exceptions propagate to the caller.
     *
     * @param administratorId the creator of the communities that grant access as in [findAvailableToAdministrator] without filters.
     * @param userId the id of the user of any kind to find.
     * @return the available user of its concrete kind with all its data, or `null` if it is missing or not available.
     * @since %CURRENT_VERSION%
     */
    fun findAvailableToAdministratorById(administratorId: MultipleRoleUserId, userId: UserId): User<*>?

    /**
     * Synchronously counts distinct users of communities created by [administratorId] as [findAvailableToAdministrator]
     * selects them, without changing stored state. Storage exceptions propagate to the caller.
     *
     * @param administratorId the creator of the communities whose multiple-role members, observers and creator are counted.
     * @param filter conditions combined with AND; foreign or unknown communities match nothing.
     * @return the exact number of matching available users.
     * @since %CURRENT_VERSION%
     */
    fun countAvailableToAdministrator(administratorId: MultipleRoleUserId, filter: UserFilter = UserFilter()): Long

    /**
     * Synchronously reads the last recorded logins of [userIds], without changing stored state.
     * Storage exceptions propagate to the caller.
     *
     * @param userIds the ids of users of any kind; an id of another kind than the stored user matches nothing.
     * @return the last login of every found user that has logged in; users without a login or missing users are absent.
     * @since %CURRENT_VERSION%
     */
    fun findLastLogins(userIds: List<UserId>): Map<UserId, Instant>

    /**
     * Synchronously checks whether a user of any kind, including participants and supervisors, has [userId],
     * without changing stored state. Storage exceptions propagate to the caller.
     *
     * @param userId the id of the user to check.
     * @return `true` if a user of the kind matching [userId] exists, `false` otherwise.
     * @since %CURRENT_VERSION%
     */
    fun existsById(userId: UserId): Boolean

    /**
     * Synchronously records [loggedInAt] as the last login of [userId] protecting the user aggregate and increasing
     * its version. Does nothing if no user of the kind matching [userId] exists; storage exceptions propagate to the caller.
     *
     * @param userId the id of the user who has logged in.
     * @param loggedInAt the moment of the login.
     * @since %CURRENT_VERSION%
     */
    fun recordLogin(userId: UserId, loggedInAt: Instant)
}

/**
 * Finds users of one kind by their access code.
 *
 * @param Id the identifier type of the user kind.
 * @param Entity the user kind.
 * @since %CURRENT_VERSION%
 */
interface UserAccessTokenFinder<Id : UserId, Entity : User<Id>> {

    /**
     * Finds the user of this kind whose current access code equals [rawAccessToken] exactly, without normalization.
     * Technical exceptions of the adapter are propagated.
     *
     * @param rawAccessToken the access code entered by the user.
     * @return the user, or `null` if no user of this kind has this access code.
     * @since %CURRENT_VERSION%
     */
    fun findByAccessToken(rawAccessToken: String): Entity?
}

/**
 * Persistence port for [MultipleRoleUser] entities.
 *
 * @since %CURRENT_VERSION%
 */
interface MultipleRoleUserRepository :
    EntityRepository<MultipleRoleUserData, MultipleRoleUserId, MultipleRoleUser>,
    UserAccessTokenFinder<MultipleRoleUserId, MultipleRoleUser> {

    /**
     * Synchronously and atomically grants [role] to [userId] if it is not held yet and makes the user a member of
     * [communityId] in that role unless already a member. Other roles and memberships are unchanged;
     * storage exceptions propagate to the caller.
     *
     * @param userId the user joining the community.
     * @param communityId the community the user joins.
     * @param role the role held in [communityId]; a newly granted role has empty role data.
     * @return the stored user after joining.
     * @throws IllegalArgumentException if the user does not exist.
     * @since %CURRENT_VERSION%
     */
    fun addCommunityMembership(userId: MultipleRoleUserId, communityId: CommunityId, role: CommunityRole): MultipleRoleUser

    /**
     * Finds the user whose e-mail address equals [email] exactly, without normalization.
     * Technical exceptions of the adapter are propagated.
     *
     * @param email the e-mail address to look for.
     * @return the user, or `null` if no user has this e-mail address.
     * @since %CURRENT_VERSION%
     */
    fun findByEmail(email: String): MultipleRoleUser?
}

/**
 * Persistence port for [Observer] entities.
 *
 * @since %CURRENT_VERSION%
 */
interface ObserverRepository :
    EntityRepository<ObserverData, SingleRoleUserId, Observer>,
    UserAccessTokenFinder<SingleRoleUserId, Observer>

/**
 * Persistence port for [Participant] entities.
 *
 * @since %CURRENT_VERSION%
 */
interface ParticipantRepository :
    EntityRepository<ParticipantData, SingleRoleUserId, Participant>,
    UserAccessTokenFinder<SingleRoleUserId, Participant> {

    /**
     * Synchronously saves one new participant of [competitionId] per access-code hash, all of them or none.
     * Storage exceptions, including an access code already held by another user, propagate and leave no participant saved.
     *
     * @param competitionId the competition the new participants belong to.
     * @param accessTokenHashes the stored access codes of the new participants; an empty list saves nothing.
     * @param nameOf the name of a new participant computed from its assigned id.
     * @return the saved participants in the order of [accessTokenHashes].
     * @since %CURRENT_VERSION%
     */
    fun saveToCompetition(
        competitionId: CompetitionId,
        accessTokenHashes: List<AccessTokenHash>,
        nameOf: (SingleRoleUserId) -> String,
    ): List<Participant>
}

/**
 * Persistence port for [Supervisor] entities.
 *
 * @since %CURRENT_VERSION%
 */
interface SupervisorRepository :
    EntityRepository<SupervisorData, SingleRoleUserId, Supervisor>,
    UserAccessTokenFinder<SingleRoleUserId, Supervisor>

/**
 * Persistence port for [RegistrationRequest] entities; at most one request is stored per e-mail address.
 *
 * @since %CURRENT_VERSION%
 */
interface RegistrationRequestRepository : EntityRepository<RegistrationRequestData, RegistrationRequestId, RegistrationRequest> {

    /**
     * Finds the request whose e-mail address equals [email] exactly, without normalization.
     * Technical exceptions of the adapter are propagated.
     *
     * @param email the e-mail address to look for.
     * @return the request, or `null` if there is no request for this e-mail address.
     * @since %CURRENT_VERSION%
     */
    fun findByEmail(email: String): RegistrationRequest?
}

/**
 * Persistence port for [EmailChangeRequest] entities; at most one request is stored per user.
 *
 * @since %CURRENT_VERSION%
 */
interface EmailChangeRequestRepository : EntityRepository<EmailChangeRequestData, EmailChangeRequestId, EmailChangeRequest> {

    /**
     * Finds the e-mail change request of the user [user].
     * Technical exceptions of the adapter are propagated.
     *
     * @param user the user changing the e-mail address.
     * @return the request, or `null` if the user has no e-mail change request.
     * @since %CURRENT_VERSION%
     */
    fun findByUser(user: MultipleRoleUserId): EmailChangeRequest?
}
