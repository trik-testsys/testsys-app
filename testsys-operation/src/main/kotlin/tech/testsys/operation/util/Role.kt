package tech.testsys.operation.util

import tech.testsys.domain.contract.persistence.repository.MultipleRoleUserRepository
import tech.testsys.domain.model.group.CommunityId
import tech.testsys.domain.model.user.Administrator
import tech.testsys.domain.model.user.CommunityRole
import tech.testsys.domain.model.user.CompatibleUserRole
import tech.testsys.domain.model.user.Developer
import tech.testsys.domain.model.user.Judge
import tech.testsys.domain.model.user.Manager
import tech.testsys.domain.model.user.MultipleRoleUser
import tech.testsys.domain.model.user.Student
import tech.testsys.operation.annotation.InternalOperationsApi

/**
 * Checks whether this user holds the role [R].
 *
 * @param R the role to look for.
 * @since %CURRENT_VERSION%
 */
@InternalOperationsApi
inline fun <reified R : CompatibleUserRole> MultipleRoleUser.hasRole(): Boolean {
    return this.data.roles.filterIsInstance<R>().size == 1
}

/**
 * Returns the held role of this user matching [role], or `null` if the user does not hold it.
 *
 * @since %CURRENT_VERSION%
 */
@InternalOperationsApi
fun MultipleRoleUser.heldRole(role: CommunityRole): CompatibleUserRole? = when (role) {
    CommunityRole.Administrator -> data.roles.filterIsInstance<Administrator>()
    CommunityRole.Manager -> data.roles.filterIsInstance<Manager>()
    CommunityRole.Developer -> data.roles.filterIsInstance<Developer>()
    CommunityRole.Student -> data.roles.filterIsInstance<Student>()
    CommunityRole.Judge -> data.roles.filterIsInstance<Judge>()
}.singleOrNull()

/**
 * Makes [user] a member of [communityId] in [role] and returns the stored user; if [user] does not hold [role] yet and it
 * is not the administrator role, first makes it a member of [publicCommunityId] in that role, as testsys.entity.multi.role
 * requires.
 *
 * @since %CURRENT_VERSION%
 */
@InternalOperationsApi
fun MultipleRoleUserRepository.joinCommunity(
    user: MultipleRoleUser,
    communityId: CommunityId,
    role: CommunityRole,
    publicCommunityId: CommunityId,
): MultipleRoleUser {
    if (role != CommunityRole.Administrator && user.heldRole(role) == null) {
        addCommunityMembership(userId = user.id, communityId = publicCommunityId, role = role)
    }
    return addCommunityMembership(userId = user.id, communityId = communityId, role = role)
}
