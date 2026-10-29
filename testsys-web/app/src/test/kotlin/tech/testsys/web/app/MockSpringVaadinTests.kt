package tech.testsys.web.app

import com.github.mvysny.fakeservlet.FakeHttpSession
import com.github.mvysny.fakeservlet.FakeRequest
import com.github.mvysny.kaributesting.v10.MockVaadin
import com.github.mvysny.kaributesting.v10.Routes
import com.github.mvysny.kaributesting.v10.spring.MockSpringSecurity
import com.github.mvysny.kaributesting.v10.spring.MockSpringServlet
import com.vaadin.flow.component.UI
import jakarta.servlet.ServletConnection
import jakarta.servlet.http.HttpSession
import jakarta.servlet.http.HttpSessionEvent
import jakarta.servlet.http.HttpSessionListener
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Import
import org.springframework.security.core.context.SecurityContextHolder
import tech.testsys.domain.model.user.User
import tech.testsys.web.app.security.CabinetPrincipal
import tech.testsys.web.app.security.CabinetSignIn
import java.security.Principal

/**
 * Starts a mocked Vaadin UI backed by the Spring context of the test before each test. The faked requests take the
 * signed-in user from Spring Security, as the security filter does in production, and invalidating their session
 * notifies the session listener beans of the context, as the servlet container does.
 */
@Import(AppTestConfiguration::class)
abstract class MockSpringVaadinTests {
    @Autowired
    private lateinit var context: ApplicationContext

    @Autowired
    protected lateinit var fixtures: AppFixtures

    @BeforeEach
    fun setUpVaadin() {
        MockSpringSecurity.mock()
        val securedRequest = MockVaadin.mockRequestFactory
        MockVaadin.mockRequestFactory = { session ->
            val secured = securedRequest(session)
            object : FakeRequest(session) {
                override fun getUserPrincipal(): Principal? = secured.userPrincipal

                override fun getSession(create: Boolean): HttpSession {
                    val current = super.getSession(create)
                    return (current as? FakeHttpSession)?.let(::ContainerSession) ?: current
                }

                // Servlet 6 members the faked request, built for Servlet 5, does not have.
                override fun getRequestId(): String = error("Faked requests have no request id")

                override fun getProtocolRequestId(): String = error("Faked requests have no protocol request id")

                override fun getServletConnection(): ServletConnection = error("Faked requests have no servlet connection")
            }.apply { isUserInRole = secured.isUserInRole }
        }

        val routes = Routes().autoDiscoverViews("tech.testsys.web.app")
        MockVaadin.setup({ UI() }, MockSpringServlet(routes, context) { UI() })
    }

    @AfterEach
    fun tearDownVaadin() {
        MockVaadin.tearDown()
        SecurityContextHolder.clearContext()
        MDC.clear()
    }

    /** Signs [user] in the current session the way the sign-in page does. */
    protected fun signIn(user: User<*>) {
        CabinetSignIn.signIn(CabinetPrincipal.of(user))
    }

    // Like a servlet container, notifies the session listeners while the attributes are still in the session.
    private inner class ContainerSession(private val session: FakeHttpSession) : HttpSession by session {
        override fun invalidate() {
            if (session.isValid) {
                val event = HttpSessionEvent(this)
                context.getBeansOfType(HttpSessionListener::class.java).values.forEach { listener -> listener.sessionDestroyed(event) }
            }
            session.invalidate()
        }
    }
}
