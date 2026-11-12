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
     * Expires every session in which the user with non-fixed roles [userId] signed in themselves or, as a supervisor, as another
     * user, except the session [except] of the current request, and deletes their remembered sign-ins, writing at INFO the end
     * of the supervisor ones (testsys.user.multi.supervisor.impersonate); sessions of a supervisor as the user stay.
     *
     * @since 1.0.0-beta.1
     */
    fun expire(userId: Long, except: String? = null) {
        val impersonations = registry.allPrincipals.filter { principal -> (principal as? CabinetPrincipal)?.impersonatedBy == userId }
        (impersonations + CabinetPrincipal(userId = userId, kind = UserKind.MULTIPLE_ROLE))
            .flatMap { principal -> registry.getAllSessions(principal, false) }
            .filter { session -> session.sessionId != except }
            .forEach { session -> session.expireNow() }

        val session = VaadinServletRequest.getCurrent()?.httpServletRequest?.getSession(false)
        val current = session?.takeIf { except != null && session.id == except }
        store.deleteOwn(CabinetSignInUserId(userId), exceptTokenHash = current?.let(signIns::tokenHashOf))
        // The sessions of these sign-ins find them deleted and no longer record their end.
        logEndedSupervisorSessions(store.deleteImpersonatedBy(CabinetSignInUserId(userId)))
    }
}
