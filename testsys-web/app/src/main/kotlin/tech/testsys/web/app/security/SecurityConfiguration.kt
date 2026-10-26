package tech.testsys.web.app.security

import com.vaadin.flow.spring.security.NavigationAccessControlConfigurer
import com.vaadin.flow.spring.security.UidlRedirectStrategy
import com.vaadin.flow.spring.security.VaadinSecurityConfigurer
import org.slf4j.MDC
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.task.TaskDecorator
import org.springframework.security.concurrent.DelegatingSecurityContextRunnable
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler
import tech.testsys.web.app.view.AuthenticationView

/**
 * Security of the application: every HTTP request is permitted, navigation checks the access annotations of the pages
 * and sends a guest to [AuthenticationView], and executor tasks run with the security and log contexts of their submitter.
 * Pages sign users in through [CabinetSignIn] instead of a login form; signing out leads to the root under the context path.
 *
 * @since %CURRENT_VERSION%
 */
@Configuration
@EnableWebSecurity
class SecurityConfiguration {
    /**
     * Decorator of the application executor tasks that runs each task with the security context and the log context
     * of its submitter.
     *
     * @since %CURRENT_VERSION%
     */
    @Bean
    fun securityContextTaskDecorator(): TaskDecorator = TaskDecorator { task ->
        val logContext = MDC.getCopyOfContextMap()
        val withLogContext = Runnable {
            logContext?.let(MDC::setContextMap)
            try {
                task.run()
            } finally {
                MDC.clear()
            }
        }

        DelegatingSecurityContextRunnable(withLogContext)
    }

    /**
     * Navigation access control checking the access annotations of the pages with [AuthenticationView] as login view.
     *
     * @since %CURRENT_VERSION%
     */
    @Bean
    fun navigationAccessControlConfigurer(): NavigationAccessControlConfigurer = NavigationAccessControlConfigurer()
        .withAnnotatedViewAccessChecker()
        .withLoginView(AuthenticationView::class.java)

    /**
     * Filter chain with the Vaadin defaults except the HTTP access rules: every request is permitted.
     *
     * @since %CURRENT_VERSION%
     */
    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http.authorizeHttpRequests { requests -> requests.anyRequest().permitAll() }
        http.with(VaadinSecurityConfigurer.vaadin()) { vaadin ->
            vaadin.enableAuthorizedRequestsConfiguration(false)
            vaadin.logoutSuccessHandler(signedOutHandler())
        }
        return http.build()
    }
}

/** Handler of a successful sign-out that sends the browser to the root of the application under its context path. */
internal fun signedOutHandler(): LogoutSuccessHandler {
    val redirect = UidlRedirectStrategy().apply { setContextRelative(true) }
    return LogoutSuccessHandler { request, response, _ ->
        // A UIDL request hands the address to the browser as is, so the address itself carries the context path.
        redirect.sendRedirect(request, response, request.contextPath + "/")
    }
}
