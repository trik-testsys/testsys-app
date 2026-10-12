package tech.testsys.bootstrap

import org.springframework.core.env.MapPropertySource
import org.springframework.web.context.support.StandardServletEnvironment

/**
 * Creates separate mutable environments for web and metrics children, leaving configuration-file and command-line
 * loading to Boot.
 *
 * @since %CURRENT_VERSION%
 */
object WebContextEnvironment {
    /**
     * Returns a new servlet environment whose property source collection is not shared with another context.
     *
     * @since %CURRENT_VERSION%
     */
    fun create(): StandardServletEnvironment = StandardServletEnvironment()

    /**
     * Applies service-only listener and cookie settings after Boot has loaded deployment configuration.
     * Native forwarded-header handling is installed explicitly with the service proxy allowlist.
     *
     * @since %CURRENT_VERSION%
     */
    fun configureService(environment: StandardServletEnvironment, settings: ServiceWebSettings) {
        environment.propertySources.addFirst(
            MapPropertySource(
                "testsysServiceWeb",
                mapOf(
                    "server.address" to settings.address,
                    "server.port" to settings.port,
                    "server.servlet.context-path" to "",
                    "server.ssl.enabled" to false,
                    "server.forward-headers-strategy" to "none",
                    "server.tomcat.remoteip.protocol-header" to "",
                    "server.tomcat.remoteip.remote-ip-header" to "",
                    "server.servlet.session.cookie.name" to "TESTSYS_SERVICE_SESSION",
                    "server.servlet.session.cookie.http-only" to true,
                    "server.servlet.session.cookie.secure" to true,
                    "server.servlet.session.cookie.same-site" to "lax",
                    "server.servlet.session.cookie.domain" to "",
                    "server.servlet.session.cookie.path" to "/",
                    "server.servlet.session.cookie.max-age" to "-1s",
                    "server.servlet.session.tracking-modes" to "cookie",
                    "vaadin.allowed-packages" to "tech.testsys.web.service,tech.testsys.web.shell,tech.testsys.web.components,com.vaadin",
                ),
            ),
        )
    }

    /**
     * Applies the metrics listener and exposes only the Prometheus endpoint at `/actuator/prometheus`,
     * without static resources, the endpoint index or a separate management server.
     *
     * @since %CURRENT_VERSION%
     */
    fun configureMetrics(environment: StandardServletEnvironment, settings: MetricsWebSettings) {
        environment.propertySources.addFirst(
            MapPropertySource(
                "testsysMetricsWeb",
                mapOf(
                    "server.address" to settings.address,
                    "server.port" to settings.port,
                    "server.servlet.context-path" to "",
                    "server.ssl.enabled" to false,
                    "server.forward-headers-strategy" to "none",
                    "spring.web.resources.add-mappings" to false,
                    // An empty port reads as unset, so the endpoints stay on this server.
                    "management.server.port" to "",
                    "management.endpoints.web.base-path" to "/actuator",
                    "management.endpoints.web.exposure.include" to "prometheus",
                    "management.endpoints.web.exposure.exclude" to "",
                    "management.endpoints.web.discovery.enabled" to false,
                ),
            ),
        )
    }
}
