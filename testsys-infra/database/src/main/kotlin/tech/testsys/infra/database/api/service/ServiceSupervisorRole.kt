package tech.testsys.infra.database.api.service

import tech.testsys.domain.model.group.CommunityId
import tech.testsys.domain.model.user.MultipleRoleUser

/**
 * Service grant and revocation of the supervisor role for the service cabinet; it bypasses the access rules of the
 * cabinets of users.
 *
 * @since 1.0.0-beta.2
 */
interface ServiceSupervisorRole {

    /**
     * Grants the supervisor role to the user with non-fixed roles [id] and makes them a member of [publicCommunityId]
     * in it; a user already holding the role stays unchanged.
     *
     * @param id the id of the user.
     * @param publicCommunityId the id of the public community.
     * @return the stored user, [SupervisorRoleChange.NotFound] or [SupervisorRoleChange.NotAllowed] for a fixed-role
     * user and the owner of the public community; nothing is stored on refusal.
     * @since 1.0.0-beta.2
     */
    fun grant(id: ServiceUserId, publicCommunityId: CommunityId): SupervisorRoleChange

    /**
     * Revokes the supervisor role of the user with non-fixed roles [id] together with its memberships; other roles stay,
     * and a user without the role stays unchanged.
     *
     * @param id the id of the user.
     * @param publicCommunityId the id of the public community.
     * @return the stored user, [SupervisorRoleChange.NotFound] or [SupervisorRoleChange.NotAllowed] for a fixed-role
     * user and the owner of the public community; nothing is stored on refusal.
     * @since 1.0.0-beta.2
     */
    fun revoke(id: ServiceUserId, publicCommunityId: CommunityId): SupervisorRoleChange
}

/**
 * Result of [ServiceSupervisorRole.grant] and [ServiceSupervisorRole.revoke].
 *
 * @since 1.0.0-beta.2
 */
sealed interface SupervisorRoleChange {

    /**
     * The role is held or not held as requested.
     *
     * @property user the stored user.
     * @since 1.0.0-beta.2
     */
    data class Changed(val user: MultipleRoleUser) : SupervisorRoleChange

    /**
     * No user has the id.
     *
     * @since 1.0.0-beta.2
     */
    data object NotFound : SupervisorRoleChange

    /**
     * The user has a fixed role or owns the public community.
     *
     * @since 1.0.0-beta.2
     */
    data object NotAllowed : SupervisorRoleChange
}
