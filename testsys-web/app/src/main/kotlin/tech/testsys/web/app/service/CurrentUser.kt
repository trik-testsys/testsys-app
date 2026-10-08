package tech.testsys.web.app.service

import org.springframework.stereotype.Component
import tech.testsys.domain.contract.persistence.repository.MultipleRoleUserRepository
import tech.testsys.domain.contract.persistence.repository.ObserverRepository
import tech.testsys.domain.contract.persistence.repository.ParticipantRepository
import tech.testsys.domain.contract.persistence.repository.SupervisorRepository
import tech.testsys.domain.model.user.MultipleRoleUser
import tech.testsys.domain.model.user.MultipleRoleUserId
import tech.testsys.domain.model.user.SingleRoleUser
import tech.testsys.domain.model.user.SingleRoleUserId
import tech.testsys.domain.model.user.User
import tech.testsys.web.app.security.CabinetPrincipal
import tech.testsys.web.app.security.CabinetSignIn
import tech.testsys.web.app.security.UserKind

/**
 * User of the current request on whose behalf the services run operations.
 *
 * @since %CURRENT_VERSION%
 */
interface CurrentUser {
    /**
     * Returns the current user of any kind.
     *
     * @since %CURRENT_VERSION%
     */
    fun user(): User<*>

    /**
     * Returns the current user, who must be a [MultipleRoleUser].
     *
     * @since %CURRENT_VERSION%
     */
    fun multipleRoleUser(): MultipleRoleUser

    /**
     * Returns the current user, who must be a [SingleRoleUser].
     *
     * @since %CURRENT_VERSION%
     */
    fun singleRoleUser(): SingleRoleUser
}

/**
 * [CurrentUser] of the signed-in [CabinetPrincipal], loaded on every call; without a signed-in user, with a missing user
 * or with a user of another kind every call throws [IllegalStateException].
 *
 * @since %CURRENT_VERSION%
 */
@Component
class SecurityCurrentUser(
    private val multipleRoleUsers: MultipleRoleUserRepository,
    private val participants: ParticipantRepository,
    private val observers: ObserverRepository,
    private val supervisors: SupervisorRepository,
) : CurrentUser {
    override fun user(): User<*> {
        val principal = checkNotNull(CabinetSignIn.principal()) { "No signed-in user" }
        val user = when (principal.kind) {
            UserKind.MULTIPLE_ROLE -> multipleRoleUsers.findById(MultipleRoleUserId(principal.userId))
            UserKind.PARTICIPANT -> participants.findById(SingleRoleUserId(principal.userId))
            UserKind.OBSERVER -> observers.findById(SingleRoleUserId(principal.userId))
            UserKind.SUPERVISOR -> supervisors.findById(SingleRoleUserId(principal.userId))
        }
        return checkNotNull(user) { "Signed-in user id=${principal.userId} of kind ${principal.kind} does not exist" }
    }

    override fun multipleRoleUser(): MultipleRoleUser {
        val user = user()
        check(user is MultipleRoleUser) { "Signed-in user id=${user.id.value} is not a multiple-role user" }
        return user
    }

    override fun singleRoleUser(): SingleRoleUser {
        val user = user()
        check(user is SingleRoleUser) { "Signed-in user id=${user.id.value} is not a single-role user" }
        return user
    }
}
