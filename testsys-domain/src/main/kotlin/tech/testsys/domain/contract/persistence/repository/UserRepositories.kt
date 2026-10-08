package tech.testsys.domain.contract.persistence.repository

import tech.testsys.domain.contract.persistence.Page
import tech.testsys.domain.contract.persistence.Pagination
import tech.testsys.domain.contract.persistence.UserFilter
import tech.testsys.domain.model.group.CommunityId
import tech.testsys.domain.model.group.CommunityInvite
import tech.testsys.domain.model.group.CompetitionId
import tech.testsys.domain.model.user.AccessTokenHash
import tech.testsys.domain.model.user.MultipleRoleUser
import tech.testsys.domain.model.user.MultipleRoleUserData
import tech.testsys.domain.model.user.MultipleRoleUserId
import tech.testsys.domain.model.user.Observer
import tech.testsys.domain.model.user.ObserverData
import tech.testsys.domain.model.user.Participant
import tech.testsys.domain.model.user.ParticipantData
import tech.testsys.domain.model.user.SingleRoleUserId
import tech.testsys.domain.model.user.Supervisor
import tech.testsys.domain.model.user.SupervisorData
import tech.testsys.domain.model.user.User
import tech.testsys.domain.model.user.UserId

/**
 * Search port for users of every kind available to an administrator.
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
     * Synchronously checks whether a user of any kind, including participants and supervisors, has [userId],
     * without changing stored state. Storage exceptions propagate to the caller.
     *
     * @param userId the id of the user to check.
     * @return `true` if a user of the kind matching [userId] exists, `false` otherwise.
     * @since %CURRENT_VERSION%
     */
    fun existsById(userId: UserId): Boolean
}

/**
 * Persistence port for [MultipleRoleUser] entities.
 *
 * @since %CURRENT_VERSION%
 */
interface MultipleRoleUserRepository : EntityRepository<MultipleRoleUserData, MultipleRoleUserId, MultipleRoleUser> {

    /**
     * Synchronously and atomically grants [kind] to [userId] if it is not held yet and makes the user a member of
     * [communityId] in that role unless already a member. Other roles and memberships are unchanged;
     * storage exceptions propagate to the caller.
     *
     * @param userId the user joining the community.
     * @param communityId the community the user joins.
     * @param kind the role held in [communityId]; a newly granted role has empty role data.
     * @return the stored user after joining.
     * @throws IllegalArgumentException if the user does not exist.
     * @since %CURRENT_VERSION%
     */
    fun addCommunityMembership(userId: MultipleRoleUserId, communityId: CommunityId, kind: CommunityInvite.Kind): MultipleRoleUser
}

/**
 * Persistence port for [Observer] entities.
 *
 * @since %CURRENT_VERSION%
 */
interface ObserverRepository : EntityRepository<ObserverData, SingleRoleUserId, Observer>

/**
 * Persistence port for [Participant] entities.
 *
 * @since %CURRENT_VERSION%
 */
interface ParticipantRepository : EntityRepository<ParticipantData, SingleRoleUserId, Participant> {

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
interface SupervisorRepository : EntityRepository<SupervisorData, SingleRoleUserId, Supervisor>
