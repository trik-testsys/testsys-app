package tech.testsys.bootstrap

import com.vaadin.flow.spring.SpringBootAutoConfiguration
import com.vaadin.flow.spring.SpringSecurityAutoConfiguration
import org.springframework.boot.autoconfigure.ImportAutoConfiguration
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration
import org.springframework.boot.autoconfigure.ssl.SslAutoConfiguration
import org.springframework.boot.autoconfigure.task.TaskExecutionAutoConfiguration
import org.springframework.boot.http.converter.autoconfigure.HttpMessageConvertersAutoConfiguration
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration
import org.springframework.boot.security.autoconfigure.web.servlet.SecurityFilterAutoConfiguration
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration
import org.springframework.boot.servlet.autoconfigure.HttpEncodingAutoConfiguration
import org.springframework.boot.servlet.autoconfigure.MultipartAutoConfiguration
import org.springframework.boot.tomcat.autoconfigure.servlet.TomcatServletWebServerAutoConfiguration
import org.springframework.boot.webmvc.autoconfigure.DispatcherServletAutoConfiguration
import org.springframework.boot.webmvc.autoconfigure.WebMvcAutoConfiguration
import org.springframework.boot.webmvc.autoconfigure.WebMvcObservationAutoConfiguration
import org.springframework.boot.webmvc.autoconfigure.error.ErrorMvcAutoConfiguration
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.PropertySource

/** Defaults of the `testsys.service.*`, `testsys.metrics.*` and `testsys.web.*` parameters of this module. */
internal const val WEB_DEFAULTS = "classpath:web-defaults.properties"

/**
 * Imports only servlet, UI, security and request observation auto-configurations; persistence and the metrics
 * registry belong to the shared parent. Each importing child owns its web server and application executor
 * and gets the defaults of the module parameters from `web-defaults.properties`.
 *
 * @since 1.0.0-beta.1
 */
@Configuration(proxyBeanMethods = false)
@PropertySource(WEB_DEFAULTS)
@ImportAutoConfiguration(
    ConfigurationPropertiesAutoConfiguration::class,
    PropertyPlaceholderAutoConfiguration::class,
    SslAutoConfiguration::class,
    TaskExecutionAutoConfiguration::class,
    JacksonAutoConfiguration::class,
    HttpMessageConvertersAutoConfiguration::class,
    HttpEncodingAutoConfiguration::class,
    MultipartAutoConfiguration::class,
    TomcatServletWebServerAutoConfiguration::class,
    DispatcherServletAutoConfiguration::class,
    WebMvcAutoConfiguration::class,
    WebMvcObservationAutoConfiguration::class,
    ErrorMvcAutoConfiguration::class,
    SecurityAutoConfiguration::class,
    ServletWebSecurityAutoConfiguration::class,
    SecurityFilterAutoConfiguration::class,
    SpringBootAutoConfiguration::class,
    SpringSecurityAutoConfiguration::class,
)
class WebAutoConfiguration
