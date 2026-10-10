package tech.testsys.web.service.security

import com.vaadin.flow.spring.security.NavigationAccessControlConfigurer
import com.vaadin.flow.spring.security.UidlRedirectStrategy
import com.vaadin.flow.spring.security.VaadinSecurityConfigurer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.task.TaskDecorator
import org.springframework.security.concurrent.DelegatingSecurityContextRunnable
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.logout.SimpleUrlLogoutSuccessHandler
import tech.testsys.web.service.view.ServiceLoginView

/**
 * Service-only navigation, request authorization and logout, preserving the standard Vaadin CSRF protection.
 *
 * @since %CURRENT_VERSION%
 */
@Configuration
@EnableWebSecurity
class ServiceSecurityConfiguration {
    /**
     * Propagates the submitting service identity into this child's application executor.
     *
     * @since %CURRENT_VERSION%
     */
    @Bean
    fun serviceSecurityTaskDecorator(): TaskDecorator = TaskDecorator { task -> DelegatingSecurityContextRunnable(task) }

    /**
     * Checks service page annotations and sends guests to the service login page.
     *
     * @since %CURRENT_VERSION%
     */
    @Bean
    fun serviceNavigationAccessControl(): NavigationAccessControlConfigurer = NavigationAccessControlConfigurer()
        .withAnnotatedViewAccessChecker()
        .withLoginView(ServiceLoginView::class.java)

    /**
     * Allows framework traffic and declared routes, denying other requests by default.
     *
     * @since %CURRENT_VERSION%
     */
    @Bean
    fun serviceSecurityFilterChain(http: HttpSecurity): SecurityFilterChain {
        val signedOut = SimpleUrlLogoutSuccessHandler().apply {
            setDefaultTargetUrl("/login")
            setRedirectStrategy(UidlRedirectStrategy())
        }

        http.authorizeHttpRequests { requests ->
            requests.requestMatchers("/testsys-ui/**").permitAll()
        }

        http.with(VaadinSecurityConfigurer.vaadin()) { vaadin ->
            vaadin.loginView(ServiceLoginView::class.java)
            vaadin.logoutSuccessHandler(signedOut)
            vaadin.anyRequest { requests -> requests.denyAll() }
        }

        return http.build()
    }
}
