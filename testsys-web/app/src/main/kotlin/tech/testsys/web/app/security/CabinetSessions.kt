package tech.testsys.web.app.security

import com.vaadin.flow.server.VaadinServletRequest
import org.springframework.security.core.session.SessionRegistry
import org.springframework.stereotype.Component
import tech.testsys.infra.database.api.session.CabinetSignInStore
import tech.testsys.infra.database.api.session.CabinetSignInUserId

/**
 * Ends the cabinet sessions of a user through the [SessionRegistry] filled by [CabinetSignIn] and deletes their
 * remembered sign-ins from [store]; an expired session is signed out on its next request. Sessions live in the memory
 * of one application instance, remembered sign-ins are deleted for every instance.
 *
 * @since 1.0.0-beta.1
 */
@Component
class CabinetSessions(
    private val registry: SessionRegistry,
    private val store: CabinetSignInStore,
    private val signIns: RememberedSignIns,
) {
    /**
     * Expires every session in which the user with non-fixed roles [userId] signed in themselves and deletes their
     * remembered sign-ins, except the session with id [except] of the current request and its sign-in; sessions of a
     * supervisor signed in as the user stay.
     *
     * @since 1.0.0-beta.1
     */
    fun expire(userId: Long, except: String? = null) {
        registry.getAllSessions(CabinetPrincipal(userId = userId, kind = UserKind.MULTIPLE_ROLE), false)
            .filter { session -> session.sessionId != except }
            .forEach { session -> session.expireNow() }

        val session = VaadinServletRequest.getCurrent()?.httpServletRequest?.getSession(false)
        val current = session?.takeIf { except != null && session.id == except }
        store.deleteOwn(CabinetSignInUserId(userId), exceptTokenHash = current?.let(signIns::tokenHashOf))
    }
}
