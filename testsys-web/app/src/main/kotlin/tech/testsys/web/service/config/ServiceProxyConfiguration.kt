package tech.testsys.web.service.config

import org.apache.catalina.valves.RemoteIpValve
import org.springframework.boot.tomcat.servlet.TomcatServletWebServerFactory
import org.springframework.boot.web.server.WebServerFactoryCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import tech.testsys.bootstrap.ServiceWebSettings

/**
 * Trusts forwarded addresses and HTTPS only from explicitly configured proxy peers.
 *
 * @since %CURRENT_VERSION%
 */
@Configuration(proxyBeanMethods = false)
class ServiceProxyConfiguration {
    /**
     * Installs container-native forwarding without Tomcat's default trust of private networks.
     *
     * @since %CURRENT_VERSION%
     */
    @Bean
    fun serviceProxy(settings: ServiceWebSettings): WebServerFactoryCustomizer<TomcatServletWebServerFactory> =
        WebServerFactoryCustomizer { factory ->
            factory.addEngineValves(
                RemoteIpValve().apply {
                    internalProxies = settings.trustedProxies
                    trustedProxies = "(?!)"
                    remoteIpHeader = "X-Forwarded-For"
                    protocolHeader = "X-Forwarded-Proto"
                    portHeader = "X-Forwarded-Port"
                },
            )
        }
}
