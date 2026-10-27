package tech.testsys.web.app.security

import org.springframework.security.core.session.SessionRegistry
import org.springframework.stereotype.Component

/**
 * Ends the cabinet sessions of a user through the [SessionRegistry] filled by [CabinetSignIn]; an expired session is
 * signed out on its next request. Sessions live in the memory of one application instance.
 *
 * @since %CURRENT_VERSION%
 */
@Component
class CabinetSessions(private val registry: SessionRegistry) {
    /**
     * Expires every session in which the user with non-fixed roles [userId] signed in themselves, except the session
     * with id [except]; sessions of a supervisor signed in as the user stay.
     *
     * @since %CURRENT_VERSION%
     */
    fun expire(userId: Long, except: String? = null) {
        registry.getAllSessions(CabinetPrincipal(userId = userId, kind = UserKind.MULTIPLE_ROLE), false)
            .filter { session -> session.sessionId != except }
            .forEach { session -> session.expireNow() }
    }
}
