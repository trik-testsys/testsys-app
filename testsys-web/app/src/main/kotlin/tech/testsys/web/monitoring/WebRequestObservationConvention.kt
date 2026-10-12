package tech.testsys.web.monitoring

import com.vaadin.flow.router.RouteData
import com.vaadin.flow.server.VaadinServletContext
import com.vaadin.flow.server.startup.ApplicationRouteRegistry
import io.micrometer.common.KeyValue
import io.micrometer.common.KeyValues
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.http.server.observation.DefaultServerRequestObservationConvention
import org.springframework.http.server.observation.ServerRequestObservationContext
import org.springframework.web.util.UrlPathHelper

/**
 * Labels HTTP requests of one web interface with [webInterface] and a bounded `uri`: the template of a Vaadin route,
 * the kind of a framework request, or `NOT_FOUND`, `REDIRECTION` or `OTHER` by status. Raw paths and query strings
 * never become label values.
 *
 * @property webInterface the name of the observed interface, `main` or `service`.
 * @since %CURRENT_VERSION%
 */
class WebRequestObservationConvention(val webInterface: String) : DefaultServerRequestObservationConvention() {
    // Templates are rebuilt only when the registered routes change, for example after a development reload.
    @Volatile
    private var routes: Pair<List<RouteData>, VaadinRouteTemplates>? = null

    override fun getLowCardinalityKeyValues(context: ServerRequestObservationContext): KeyValues =
        super.getLowCardinalityKeyValues(context).and(KeyValue.of(INTERFACE, webInterface))

    override fun uri(context: ServerRequestObservationContext): KeyValue = KeyValue.of(URI, endpoint(context))

    private fun endpoint(context: ServerRequestObservationContext): String {
        val request = context.carrier ?: return OTHER
        val path = UrlPathHelper.defaultInstance.getPathWithinApplication(request).ifEmpty { "/" }
        val requestType = requestType(request)

        return when {
            path == PUSH_PATH || path.startsWith("$PUSH_PATH/") -> PUSH_PATH
            requestType != null -> "/?$REQUEST_TYPE=" + (requestType.takeIf { type -> type in REQUEST_TYPES } ?: "other")
            path.startsWith(DYNAMIC_PREFIX) -> "$DYNAMIC_PREFIX**"
            path.startsWith(VAADIN_PREFIX) -> "$VAADIN_PREFIX**"
            path.startsWith(COMPONENTS_PREFIX) -> "$COMPONENTS_PREFIX**"
            else -> templates(request).match(path) ?: byStatus(context)
        }
    }

    private fun templates(request: HttpServletRequest): VaadinRouteTemplates {
        val registered = ApplicationRouteRegistry.getInstance(VaadinServletContext(request.servletContext)).registeredRoutes
        val cached = routes
        if (cached != null && cached.first == registered) return cached.second

        return VaadinRouteTemplates.of(registered).also { templates -> routes = registered to templates }
    }

    private fun byStatus(context: ServerRequestObservationContext): String {
        val status = context.response?.let { response -> HttpStatus.resolve(response.status) }
        return when {
            status == HttpStatus.NOT_FOUND -> "NOT_FOUND"
            status?.is3xxRedirection == true -> "REDIRECTION"
            else -> OTHER
        }
    }

    private companion object {
        const val INTERFACE = "interface"
        const val URI = "uri"
        const val OTHER = "OTHER"
        const val REQUEST_TYPE = "v-r"
        const val PUSH_PATH = "/VAADIN/push"
        const val DYNAMIC_PREFIX = "/VAADIN/dynamic/"
        const val VAADIN_PREFIX = "/VAADIN/"
        const val COMPONENTS_PREFIX = "/testsys-ui/"
        val REQUEST_TYPES = setOf("uidl", "heartbeat", "init", "push")

        /** Reads the Vaadin request type from the query string only, so a form body is never parsed for a label. */
        fun requestType(request: HttpServletRequest): String? = request.queryString?.split('&')
            ?.firstOrNull { parameter -> parameter.substringBefore('=') == REQUEST_TYPE }
            ?.substringAfter('=', "")
    }
}
