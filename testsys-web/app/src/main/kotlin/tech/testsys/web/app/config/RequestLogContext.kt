package tech.testsys.web.app.config

import com.vaadin.flow.router.AfterNavigationEvent
import com.vaadin.flow.router.BeforeEnterEvent
import com.vaadin.flow.router.Location
import com.vaadin.flow.server.ServiceInitEvent
import com.vaadin.flow.server.VaadinRequest
import com.vaadin.flow.server.VaadinRequestInterceptor
import com.vaadin.flow.server.VaadinResponse
import com.vaadin.flow.server.VaadinServiceInitListener
import com.vaadin.flow.server.VaadinSession
import com.vaadin.flow.server.communication.StreamRequestHandler
import com.vaadin.flow.shared.ApplicationConstants
import com.vaadin.flow.spring.annotation.SpringComponent
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import tech.testsys.web.app.security.CabinetPrincipal
import tech.testsys.web.app.security.CabinetSignIn

/**
 * Puts the id of the signed-in user, the id of the supervisor signed in as them, and the path of the open page into
 * the log context of every Vaadin request of the main interface and writes each opened page at DEBUG.
 *
 * @since %CURRENT_VERSION%
 */
@SpringComponent
class RequestLogContext :
    VaadinServiceInitListener,
    VaadinRequestInterceptor {
    override fun serviceInit(event: ServiceInitEvent) {
        event.addVaadinRequestInterceptor(this)
        event.source.addUIInitListener { uiEvent ->
            val ui = uiEvent.ui
            ui.addBeforeEnterListener { entered -> entering(entered) }
            ui.addAfterNavigationListener { navigation -> opened(navigation, ui.uiId, ui.session) }
            ui.addDetachListener { forget(ui.session, ui.uiId) }
        }
    }

    // Push over a websocket starts and ends its requests without a response.
    override fun requestStart(request: VaadinRequest, response: VaadinResponse?) {
        putUser()

        // The Vaadin session is not looked up yet, so the page path of the UI is read from the HTTP session.
        val uiId = uiIdOf(request) ?: return
        val route = request.getWrappedSession(false)?.getAttribute(ROUTE_ATTRIBUTE + uiId) as? String
        route?.let { path -> MDC.put(ROUTE, path) }
    }

    override fun handleException(request: VaadinRequest, response: VaadinResponse?, session: VaadinSession?, exception: Exception) = Unit

    override fun requestEnd(request: VaadinRequest, response: VaadinResponse?, session: VaadinSession?) {
        MDC.remove(USER)
        MDC.remove(IMPERSONATED_BY)
        MDC.remove(ROUTE)
    }

    /**
     * Puts the user signed in by now and the path of the page entered by [event] into the log context, so the calls
     * made while the page is built and its background work carry them even when sign-in and navigation share a request.
     */
    internal fun entering(event: BeforeEnterEvent) {
        putUser()
        MDC.put(ROUTE, pathOf(event.location))
    }

    /** Writes the page opened by [event] in the UI [uiId] and remembers its path for the next requests of that UI. */
    internal fun opened(event: AfterNavigationEvent, uiId: Int, session: VaadinSession?) {
        val path = pathOf(event.location)
        MDC.put(ROUTE, path)
        session?.session?.setAttribute(ROUTE_ATTRIBUTE + uiId, path)
        logger.debug("Opened {} ({})", path, event.activeChain.firstOrNull()?.javaClass?.simpleName)
    }

    private fun putUser() = putUser(CabinetSignIn.principal())

    // A file transfer request carries the UI id in its path instead of the parameter.
    private fun uiIdOf(request: VaadinRequest): String? = request.getParameter(ApplicationConstants.UI_ID_PARAMETER)
        ?: request.pathInfo
            ?.takeIf { path -> path.startsWith(RESOURCE_PATH) }
            ?.removePrefix(RESOURCE_PATH)
            ?.substringBefore('/')

    private fun pathOf(location: Location): String = "/" + location.path

    private fun forget(session: VaadinSession?, uiId: Int) {
        // A UI is also detached when its HTTP session is already invalidated, and then nothing is left to remove.
        try {
            session?.session?.removeAttribute(ROUTE_ATTRIBUTE + uiId)
        } catch (_: IllegalStateException) {
            return
        }
    }

    companion object {
        private const val USER = "user"
        private const val IMPERSONATED_BY = "impersonatedBy"
        private const val ROUTE = "route"
        private const val ROUTE_ATTRIBUTE = "testsys.route."
        private const val RESOURCE_PATH = "/" + StreamRequestHandler.DYN_RES_PREFIX
        private val logger: Logger = LoggerFactory.getLogger(RequestLogContext::class.java)

        /**
         * Runs [action] with the ids of [principal] in the log context, also outside a request, and then restores
         * the ids that were there before.
         */
        internal fun withUser(principal: CabinetPrincipal, action: () -> Unit) {
            val previous = listOf(USER, IMPERSONATED_BY).associateWith { key -> MDC.get(key) }
            putUser(principal)
            try {
                action()
            } finally {
                previous.forEach { (key, value) -> if (value == null) MDC.remove(key) else MDC.put(key, value) }
            }
        }

        private fun putUser(principal: CabinetPrincipal?) {
            if (principal == null) {
                MDC.remove(USER)
            } else {
                MDC.put(USER, principal.userId.toString())
            }

            val supervisorId = principal?.impersonatedBy
            if (supervisorId == null) {
                MDC.remove(IMPERSONATED_BY)
            } else {
                MDC.put(IMPERSONATED_BY, supervisorId.toString())
            }
        }
    }
}
