package tech.testsys.web.service.security

import com.vaadin.flow.spring.security.NavigationAccessControlConfigurer
import com.vaadin.flow.spring.security.UidlRedirectStrategy
import com.vaadin.flow.spring.security.VaadinSecurityConfigurer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import org.springframework.core.task.TaskDecorator
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.concurrent.DelegatingSecurityContextRunnable
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.core.Authentication
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.logout.SimpleUrlLogoutSuccessHandler
import org.springframework.security.web.context.SecurityContextHolderFilter
import tech.testsys.bootstrap.ServiceWebSettings
import tech.testsys.infra.database.api.session.CabinetSignInStore
import tech.testsys.infra.database.api.session.StoredCabinetSignIn
import tech.testsys.web.app.security.RememberedSignIns
import tech.testsys.web.app.security.sessionTimeoutOf
import tech.testsys.web.service.view.ServiceLoginView
import java.time.Clock

/** Name of the cookie of the remembered sign-in to the service interface. */
private const val SIGN_IN_COOKIE = "TESTSYS_SERVICE_SIGN_IN"

/**
 * Service-only navigation, request authorization and logout, preserving the standard Vaadin CSRF protection; the
 * service sign-in survives a restart through [RememberedSignIns] while the startup secret stays the same.
 *
 * @since 1.0.0-beta.1
 */
@Configuration
@EnableWebSecurity
class ServiceSecurityConfiguration {
    /**
     * Propagates the submitting service identity into this child's application executor.
     *
     * @since 1.0.0-beta.1
     */
    @Bean
    fun serviceSecurityTaskDecorator(): TaskDecorator = TaskDecorator { task -> DelegatingSecurityContextRunnable(task) }

    /**
     * Checks service page annotations and sends guests to the service login page.
     *
     * @since 1.0.0-beta.1
     */
    @Bean
    fun serviceNavigationAccessControl(): NavigationAccessControlConfigurer = NavigationAccessControlConfigurer()
        .withAnnotatedViewAccessChecker()
        .withLoginView(ServiceLoginView::class.java)

    /**
     * Remembered service sign-ins in the cookie `TESTSYS_SERVICE_SIGN_IN`, idle for at most the session timeout of
     * [environment], with token digests bound to the startup secret of [settings].
     *
     * @since 1.0.0-beta.1
     */
    @Bean
    fun serviceSignIns(store: CabinetSignInStore, settings: ServiceWebSettings, environment: Environment, clock: Clock): RememberedSignIns =
        RememberedSignIns(store, SIGN_IN_COOKIE, sessionTimeoutOf(environment), clock, settings::signInTokenHash)

    /**
     * Allows framework traffic and declared routes, denying other requests by default. A request without a signed-in
     * session is signed in from its sign-in in [serviceSignIns]; signing out forgets it.
     *
     * @since 1.0.0-beta.1
     */
    @Bean
    fun serviceSecurityFilterChain(http: HttpSecurity, serviceSignIns: RememberedSignIns): SecurityFilterChain {
        val signedOut = SimpleUrlLogoutSuccessHandler().apply {
            setDefaultTargetUrl("/login")
            setRedirectStrategy(UidlRedirectStrategy())
        }

        http.authorizeHttpRequests { requests ->
            requests.requestMatchers("/testsys-ui/**").permitAll()
        }

        http.logout { logout -> logout.addLogoutHandler(serviceSignIns.logoutHandler()) }
        http.addFilterAfter(serviceSignIns.restoreFilter(::authenticationOf), SecurityContextHolderFilter::class.java)
        http.with(VaadinSecurityConfigurer.vaadin()) { vaadin ->
            vaadin.loginView(ServiceLoginView::class.java)
            vaadin.logoutSuccessHandler(signedOut)
            vaadin.anyRequest { requests -> requests.denyAll() }
        }

        return http.build()
    }
}

/** Returns the service authentication of the remembered [signIn], or `null` for a sign-in of the main interface. */
private fun authenticationOf(signIn: StoredCabinetSignIn): Authentication? = if (signIn is StoredCabinetSignIn.Service) {
    serviceAuthentication()
} else {
    null
}

/** Returns the authentication of [ServicePrincipal] with its only authority. */
internal fun serviceAuthentication(): Authentication =
    UsernamePasswordAuthenticationToken.authenticated(ServicePrincipal, null, listOf(SimpleGrantedAuthority(ServicePrincipal.AUTHORITY)))
