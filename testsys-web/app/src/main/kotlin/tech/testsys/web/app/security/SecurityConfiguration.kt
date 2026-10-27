package tech.testsys.web.app.security

import com.vaadin.flow.spring.security.NavigationAccessControlConfigurer
import com.vaadin.flow.spring.security.UidlRedirectStrategy
import com.vaadin.flow.spring.security.VaadinSecurityConfigurer
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.event.EventListener
import org.springframework.core.task.TaskDecorator
import org.springframework.security.concurrent.DelegatingSecurityContextRunnable
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.core.session.SessionRegistry
import org.springframework.security.core.session.SessionRegistryImpl
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler
import org.springframework.security.web.session.HttpSessionDestroyedEvent
import org.springframework.security.web.session.HttpSessionEventPublisher
import tech.testsys.web.app.config.RequestLogContext
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
    private val logger: Logger = LoggerFactory.getLogger(SecurityConfiguration::class.java)

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
     * Publisher of the HTTP session events of the servlet container as application events.
     *
     * @since %CURRENT_VERSION%
     */
    @Bean
    fun httpSessionEventPublisher(): HttpSessionEventPublisher = HttpSessionEventPublisher()

    /**
     * Writes at INFO the end of the session of a supervisor signed in as a user, by signing out or by expiry, from the
     * security contexts of the destroyed session [event], with both ids in the log context of the record
     * (testsys.user.single.supervisor.impersonate).
     *
     * @since %CURRENT_VERSION%
     */
    @EventListener
    fun sessionDestroyed(event: HttpSessionDestroyedEvent) {
        event.securityContexts
            .mapNotNull { context -> context.authentication?.principal as? CabinetPrincipal }
            .filter { principal -> principal.impersonatedBy != null }
            .forEach { principal ->
                RequestLogContext.withUser(principal) {
                    logger.info("Session of supervisor {} as user {} ended", principal.impersonatedBy, principal.userId)
                }
            }
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
     * Registry of the cabinet sessions in the memory of this instance, filled by [CabinetSignIn] and used by
     * [CabinetSessions] to end the sessions of a user.
     *
     * @since %CURRENT_VERSION%
     */
    @Bean
    fun sessionRegistry(): SessionRegistry = SessionRegistryImpl()

    /**
     * Filter chain with the Vaadin defaults except the HTTP access rules: every request is permitted. An expired
     * session in [sessionRegistry] is signed out on its next request, which leads to the root under the context path.
     *
     * @since %CURRENT_VERSION%
     */
    @Bean
    fun securityFilterChain(http: HttpSecurity, sessionRegistry: SessionRegistry): SecurityFilterChain {
        http.authorizeHttpRequests { requests -> requests.anyRequest().permitAll() }
        http.sessionManagement { sessions ->
            sessions.maximumSessions(-1)
                .sessionRegistry(sessionRegistry)
                .expiredSessionStrategy { event -> signedOutHandler().onLogoutSuccess(event.request, event.response, null) }
        }
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
