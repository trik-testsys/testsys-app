package tech.testsys.web.app

import com.github.mvysny.kaributesting.v10.MockVaadin
import com.github.mvysny.kaributesting.v10.Routes
import com.github.mvysny.kaributesting.v10.spring.MockSpringSecurity
import com.github.mvysny.kaributesting.v10.spring.MockSpringServlet
import com.vaadin.flow.component.UI
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Import
import org.springframework.security.core.context.SecurityContextHolder
import tech.testsys.domain.model.user.User
import tech.testsys.web.app.security.CabinetPrincipal
import tech.testsys.web.app.security.CabinetSignIn

/**
 * Starts a mocked Vaadin UI backed by the Spring context of the test before each test. The faked requests take the
 * signed-in user from Spring Security, as the security filter does in production.
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
        val routes = Routes().autoDiscoverViews("tech.testsys.web.app")
        MockVaadin.setup({ UI() }, MockSpringServlet(routes, context) { UI() })
    }

    @AfterEach
    fun tearDownVaadin() {
        MockVaadin.tearDown()
        SecurityContextHolder.clearContext()
    }

    /** Signs [user] in the current session the way the sign-in page does. */
    protected fun signIn(user: User<*>) {
        CabinetSignIn.signIn(CabinetPrincipal.of(user))
    }
}
