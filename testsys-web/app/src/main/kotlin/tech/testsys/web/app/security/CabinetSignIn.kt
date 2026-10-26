package tech.testsys.web.app.security

import com.vaadin.flow.router.RouteConfiguration
import com.vaadin.flow.server.VaadinServletRequest
import com.vaadin.flow.server.VaadinServletResponse
import com.vaadin.flow.server.auth.AccessAnnotationChecker
import com.vaadin.flow.server.auth.NavigationAccessControl
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.context.HttpSessionSecurityContextRepository
import java.security.Principal
import kotlin.jvm.optionals.getOrNull

/**
 * Signs a user in from a Vaadin request handler, without a login form of Spring Security.
 *
 * @since %CURRENT_VERSION%
 */
object CabinetSignIn {
    private const val ROLE_PREFIX = "ROLE_"

    private val logger: Logger = LoggerFactory.getLogger(CabinetSignIn::class.java)

    private val repository = HttpSessionSecurityContextRepository()

    /**
     * Returns the signed-in principal of the current request, or `null` for a guest.
     *
     * @since %CURRENT_VERSION%
     */
    fun principal(): CabinetPrincipal? = SecurityContextHolder.getContext().authentication?.principal as? CabinetPrincipal

    /**
     * Changes the session id against session fixation, stores [principal] with the role of its kind in the session
     * and returns the page the user opened before being sent to sign in, removing it, or `null` if there is none
     * or it is closed to the kind of [principal].
     *
     * @since %CURRENT_VERSION%
     */
    fun signIn(principal: CabinetPrincipal): String? {
        val request = checkNotNull(VaadinServletRequest.getCurrent()) { "Sign-in needs a Vaadin servlet request" }.httpServletRequest
        val response = checkNotNull(VaadinServletResponse.getCurrent()) { "Sign-in needs a Vaadin servlet response" }.httpServletResponse
        request.changeSessionId()

        val strategy = SecurityContextHolder.getContextHolderStrategy()
        val authority = SimpleGrantedAuthority(ROLE_PREFIX + principal.kind.name)
        val context = strategy.createEmptyContext().apply {
            authentication = UsernamePasswordAuthenticationToken.authenticated(principal, null, listOf(authority))
        }
        strategy.context = context
        repository.saveContext(context, request, response)

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
     * @since %CURRENT_VERSION%
     */
    fun returnToSupervisor(): Long? {
        val principal = principal() ?: return null
        val supervisorId = principal.impersonatedBy ?: return null

        signIn(CabinetPrincipal(userId = supervisorId, kind = UserKind.SUPERVISOR))
        logger.info("Supervisor {} returned from user {}", supervisorId, principal.userId)
        return principal.userId
    }

    /** Whether [address] leads to a page whose access annotations let [principal] in. */
    private fun isOpenTo(address: String, principal: CabinetPrincipal): Boolean {
        val page = RouteConfiguration.forApplicationScope().getRoute(address.substringBefore('?')).getOrNull() ?: return false
        return AccessAnnotationChecker().hasAccess(page, Principal { principal.userId.toString() }) { role -> role == principal.kind.name }
    }
}
