package tech.testsys.web.app.security

import com.vaadin.flow.router.RouteConfiguration
import com.vaadin.flow.server.VaadinService
import com.vaadin.flow.server.VaadinServletRequest
import com.vaadin.flow.server.VaadinServletResponse
import com.vaadin.flow.server.auth.AccessAnnotationChecker
import com.vaadin.flow.server.auth.NavigationAccessControl
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.session.SessionRegistry
import org.springframework.security.web.context.HttpSessionSecurityContextRepository
import tech.testsys.infra.database.api.session.CabinetSignInUserId
import tech.testsys.infra.database.api.session.CabinetSignInUserKind
import tech.testsys.infra.database.api.session.StoredCabinetSignIn
import java.security.Principal
import kotlin.jvm.optionals.getOrNull

/**
 * Signs a user in from a Vaadin request handler, without a login form of Spring Security.
 *
 * @since 1.0.0-beta.1
 */
object CabinetSignIn {
    private const val ROLE_PREFIX = "ROLE_"

    private val logger: Logger = LoggerFactory.getLogger(CabinetSignIn::class.java)

    private val repository = HttpSessionSecurityContextRepository()

    /**
     * Returns the signed-in principal of the current request, or `null` for a guest.
     *
     * @since 1.0.0-beta.1
     */
    fun principal(): CabinetPrincipal? = SecurityContextHolder.getContext().authentication?.principal as? CabinetPrincipal

    /**
     * Changes the session id, registers the session of [principal] in the [SessionRegistry], stores [principal] with
     * its role in the session and in [RememberedSignIns], and returns the removed page the user opened before being
     * sent to sign in, or `null` if there is none or it is closed to the kind of [principal].
     *
     * @since 1.0.0-beta.1
     */
    fun signIn(principal: CabinetPrincipal): String? {
        val request = checkNotNull(VaadinServletRequest.getCurrent()) { "Sign-in needs a Vaadin servlet request" }.httpServletRequest
        val response = checkNotNull(VaadinServletResponse.getCurrent()) { "Sign-in needs a Vaadin servlet response" }.httpServletResponse
        val sessionId = request.changeSessionId()
        val instantiator = VaadinService.getCurrent().instantiator
        // The registry lets password changes end the other sessions of the user (CabinetSessions).
        instantiator.getOrCreate(SessionRegistry::class.java).registerNewSession(sessionId, principal)

        val strategy = SecurityContextHolder.getContextHolderStrategy()
        val authority = SimpleGrantedAuthority(authorityOf(principal.kind))
        val context = strategy.createEmptyContext().apply {
            authentication = UsernamePasswordAuthenticationToken.authenticated(principal, null, listOf(authority))
        }
        strategy.context = context
        repository.saveContext(context, request, response)
        instantiator.getOrCreate(RememberedSignIns::class.java).remember(request, response, signInOf(principal))

        val session = request.getSession(false)
        val returnAddress = session?.getAttribute(NavigationAccessControl.SESSION_STORED_REDIRECT) as? String
        session?.removeAttribute(NavigationAccessControl.SESSION_STORED_REDIRECT)
        session?.removeAttribute(NavigationAccessControl.SESSION_STORED_REDIRECT_ABSOLUTE)
        return returnAddress?.takeIf { isOpenTo(it, principal) }
    }

    /**
     * Signs the supervisor who signed in as the current user back in with [signIn] and returns the raw id of the user
     * they leave, or `null` without changes if the current user signed in themselves.
     *
     * @since 1.0.0-beta.1
     */
    fun returnToSupervisor(): Long? {
        val principal = principal() ?: return null
        val supervisorId = principal.impersonatedBy ?: return null

        signIn(CabinetPrincipal(userId = supervisorId, kind = UserKind.SUPERVISOR))
        logger.info("Supervisor {} returned from user {}", supervisorId, principal.userId)
        return principal.userId
    }

    /** Returns the only authority of a user of [kind]. */
    internal fun authorityOf(kind: UserKind): String = ROLE_PREFIX + kind.name

    /** Returns the sign-in of [principal] to remember. */
    private fun signInOf(principal: CabinetPrincipal): StoredCabinetSignIn.User {
        val kind = when (principal.kind) {
            UserKind.MULTIPLE_ROLE -> CabinetSignInUserKind.MULTIPLE_ROLE
            UserKind.PARTICIPANT -> CabinetSignInUserKind.PARTICIPANT
            UserKind.OBSERVER -> CabinetSignInUserKind.OBSERVER
            UserKind.SUPERVISOR -> CabinetSignInUserKind.SUPERVISOR
        }
        return StoredCabinetSignIn.User(
            userId = CabinetSignInUserId(principal.userId),
            kind = kind,
            impersonatedBy = principal.impersonatedBy?.let(::CabinetSignInUserId),
        )
    }

    /** Whether [address] leads to a page whose access annotations let [principal] in. */
    private fun isOpenTo(address: String, principal: CabinetPrincipal): Boolean {
        val page = RouteConfiguration.forApplicationScope().getRoute(address.substringBefore('?')).getOrNull() ?: return false
        return AccessAnnotationChecker().hasAccess(page, Principal { principal.userId.toString() }) { role -> role == principal.kind.name }
    }
}
