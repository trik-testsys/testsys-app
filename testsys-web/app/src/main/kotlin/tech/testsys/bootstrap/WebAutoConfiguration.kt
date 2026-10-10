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
import org.springframework.boot.webmvc.autoconfigure.error.ErrorMvcAutoConfiguration
import org.springframework.context.annotation.Configuration

/**
 * Imports only servlet, UI and security auto-configurations; persistence belongs to the shared parent.
 * Each importing child owns its web server and application executor.
 *
 * @since %CURRENT_VERSION%
 */
@Configuration(proxyBeanMethods = false)
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
    ErrorMvcAutoConfiguration::class,
    SecurityAutoConfiguration::class,
    ServletWebSecurityAutoConfiguration::class,
    SecurityFilterAutoConfiguration::class,
    SpringBootAutoConfiguration::class,
    SpringSecurityAutoConfiguration::class,
)
class WebAutoConfiguration
