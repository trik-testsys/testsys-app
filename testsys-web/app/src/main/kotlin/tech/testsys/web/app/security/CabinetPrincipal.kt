package tech.testsys.web.app.security

import tech.testsys.domain.model.user.MultipleRoleUser
import tech.testsys.domain.model.user.Observer
import tech.testsys.domain.model.user.Participant
import tech.testsys.domain.model.user.Supervisor
import tech.testsys.domain.model.user.User
import java.io.Serializable

/**
 * Kind of a signed-in user; its name is the only role checked by `@RolesAllowed` on the pages.
 *
 * @since %CURRENT_VERSION%
 */
enum class UserKind {

    MULTIPLE_ROLE,
    PARTICIPANT,
    OBSERVER,
    SUPERVISOR,
}

/**
 * Signed-in user kept in the session: only the id and kind, without the access code and user data.
 *
 * @property userId the raw id of the user.
 * @property kind the kind of the user.
 * @since %CURRENT_VERSION%
 */
data class CabinetPrincipal(val userId: Long, val kind: UserKind) : Serializable {
    companion object {
        private const val serialVersionUID: Long = 1

        /**
         * Returns the principal of [user].
         *
         * @since %CURRENT_VERSION%
         */
        fun of(user: User<*>): CabinetPrincipal {
            val kind = when (user) {
                is MultipleRoleUser -> UserKind.MULTIPLE_ROLE
                is Participant -> UserKind.PARTICIPANT
                is Observer -> UserKind.OBSERVER
                is Supervisor -> UserKind.SUPERVISOR
            }
            return CabinetPrincipal(userId = user.id.value, kind = kind)
        }
    }
}
