package tech.testsys.web.app.security

import com.vaadin.flow.spring.security.NavigationAccessControlConfigurer
import com.vaadin.flow.spring.security.UidlRedirectStrategy
import com.vaadin.flow.spring.security.VaadinSecurityConfigurer
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.event.ContextClosedEvent
import org.springframework.context.event.EventListener
import org.springframework.core.env.Environment
import org.springframework.core.task.TaskDecorator
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.concurrent.DelegatingSecurityContextRunnable
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.core.Authentication
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.session.SessionRegistry
import org.springframework.security.core.session.SessionRegistryImpl
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler
import org.springframework.security.web.context.SecurityContextHolderFilter
import org.springframework.security.web.session.HttpSessionDestroyedEvent
import org.springframework.security.web.session.HttpSessionEventPublisher
import tech.testsys.infra.database.api.session.CabinetSignInStore
import tech.testsys.infra.database.api.session.CabinetSignInUserKind
import tech.testsys.infra.database.api.session.StoredCabinetSignIn
import tech.testsys.web.app.config.RequestLogContext
import tech.testsys.web.app.view.AuthenticationView
import java.time.Clock

/** Name of the cookie of the remembered sign-in to the main interface. */
private const val SIGN_IN_COOKIE = "TESTSYS_APP_SIGN_IN"

/**
 * Security of the application: every HTTP request is permitted, navigation checks the access annotations of the pages
 * and sends a guest to [AuthenticationView], and executor tasks run with the security and log contexts of their submitter.
 * Pages sign users in through [CabinetSignIn], kept over restarts by [RememberedSignIns]; signing out leads to the root.
 *
 * @since %CURRENT_VERSION%
 */
@Configuration
@EnableWebSecurity
class SecurityConfiguration(private val store: CabinetSignInStore, environment: Environment, clock: Clock) {
    private val signIns = RememberedSignIns(store, SIGN_IN_COOKIE, sessionTimeoutOf(environment), clock)

    @Volatile
    private var isClosing = false

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
     * Remembered sign-ins of the main interface in the cookie `TESTSYS_APP_SIGN_IN`, idle for at most the session
     * timeout.
     *
     * @since %CURRENT_VERSION%
     */
    @Bean
    fun cabinetSignIns(): RememberedSignIns = signIns

    /**
     * Keeps [sessionDestroyed] from deleting the remembered sign-ins of the sessions closed by the shutdown of [event]:
     * they stay and are restored after the restart.
     *
     * @since %CURRENT_VERSION%
     */
    @EventListener
    fun contextClosed(event: ContextClosedEvent) {
        isClosing = true
    }

    /**
     * Deletes the remembered sign-in of the destroyed session [event] and writes at INFO the end of a session of
     * a supervisor signed in as a user (testsys.user.single.supervisor.impersonate). A sign-in already deleted
     * elsewhere is not recorded again; one still remembered at the shutdown stays and is not recorded.
     *
     * @since %CURRENT_VERSION%
     */
    @EventListener
    fun sessionDestroyed(event: HttpSessionDestroyedEvent) {
        val hash = signIns.tokenHashOf(event.session)
        // A signed-out session has forgotten its sign-in already, so only its end is left to record.
        if (hash != null && (isClosing || !store.delete(hash))) return

        event.securityContexts
            .mapNotNull { context -> context.authentication?.principal as? CabinetPrincipal }
            .forEach(::logEndedSupervisorSession)
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
     * Filter chain with the Vaadin defaults except the HTTP access rules: every request is permitted, a guest session is
     * restored from [cabinetSignIns] into [sessionRegistry], and an expired or signed-out session leads to the root.
     *
     * @since %CURRENT_VERSION%
     */
    @Bean
    fun securityFilterChain(http: HttpSecurity, sessionRegistry: SessionRegistry, cabinetSignIns: RememberedSignIns): SecurityFilterChain {
        http.authorizeHttpRequests { requests -> requests.anyRequest().permitAll() }
        http.sessionManagement { sessions ->
            sessions.maximumSessions(-1)
                .sessionRegistry(sessionRegistry)
                .expiredSessionStrategy { event -> signedOutHandler().onLogoutSuccess(event.request, event.response, null) }
        }
        http.logout { logout -> logout.addLogoutHandler(cabinetSignIns.logoutHandler()) }
        val restore = cabinetSignIns.restoreFilter(::authenticationOf) { session, authentication ->
            val principal = authentication.principal as? CabinetPrincipal ?: return@restoreFilter
            sessionRegistry.registerNewSession(session.id, principal)
            if (principal.impersonatedBy == null) return@restoreFilter

            RequestLogContext.withUser(principal) {
                logger.info("Session of supervisor {} as user {} restored", principal.impersonatedBy, principal.userId)
            }
        }
        http.addFilterAfter(restore, SecurityContextHolderFilter::class.java)
        http.with(VaadinSecurityConfigurer.vaadin()) { vaadin ->
            vaadin.enableAuthorizedRequestsConfiguration(false)
            vaadin.logoutSuccessHandler(signedOutHandler())
        }
        return http.build()
    }
}

/** Logger of the sessions of a supervisor signed in as a user. */
private val logger: Logger = LoggerFactory.getLogger(SecurityConfiguration::class.java)

/**
 * Writes at INFO the end of every sign-in of a supervisor as a user among the deleted [signIns]
 * (testsys.user.single.supervisor.impersonate).
 */
internal fun logEndedSupervisorSessions(signIns: List<StoredCabinetSignIn>) {
    signIns.filterIsInstance<StoredCabinetSignIn.User>().map(::principalOf).forEach(::logEndedSupervisorSession)
}

/** Writes at INFO the end of the session of [principal] if it is a supervisor signed in as a user, with both ids in the log context. */
private fun logEndedSupervisorSession(principal: CabinetPrincipal) {
    val supervisorId = principal.impersonatedBy ?: return

    RequestLogContext.withUser(principal) {
        logger.info("Session of supervisor {} as user {} ended", supervisorId, principal.userId)
    }
}

/** Returns the authentication of the remembered [signIn] of the main interface, or `null` for a service sign-in. */
private fun authenticationOf(signIn: StoredCabinetSignIn): Authentication? {
    val principal = when (signIn) {
        StoredCabinetSignIn.Service -> return null
        is StoredCabinetSignIn.User -> principalOf(signIn)
    }
    val authority = SimpleGrantedAuthority(CabinetSignIn.authorityOf(principal.kind))
    return UsernamePasswordAuthenticationToken.authenticated(principal, null, listOf(authority))
}

/** Returns the principal of the remembered [signIn] of a user. */
private fun principalOf(signIn: StoredCabinetSignIn.User): CabinetPrincipal {
    val kind = when (signIn.kind) {
        CabinetSignInUserKind.MULTIPLE_ROLE -> UserKind.MULTIPLE_ROLE
        CabinetSignInUserKind.PARTICIPANT -> UserKind.PARTICIPANT
        CabinetSignInUserKind.OBSERVER -> UserKind.OBSERVER
        CabinetSignInUserKind.SUPERVISOR -> UserKind.SUPERVISOR
    }
    return CabinetPrincipal(userId = signIn.userId.value, kind = kind, impersonatedBy = signIn.impersonatedBy?.value)
}

/** Handler of a successful sign-out that sends the browser to the root of the application under its context path. */
internal fun signedOutHandler(): LogoutSuccessHandler {
    val redirect = UidlRedirectStrategy().apply { setContextRelative(true) }
    return LogoutSuccessHandler { request, response, _ ->
        // A UIDL request hands the address to the browser as is, so the address itself carries the context path.
        redirect.sendRedirect(request, response, request.contextPath + "/")
    }
}
