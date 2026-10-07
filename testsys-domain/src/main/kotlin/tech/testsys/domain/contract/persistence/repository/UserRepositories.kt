package tech.testsys.domain.contract.persistence.repository

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
