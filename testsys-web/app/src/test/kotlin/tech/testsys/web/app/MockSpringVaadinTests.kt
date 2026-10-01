package tech.testsys.web.app

import com.github.mvysny.kaributesting.v10.MockVaadin
import com.github.mvysny.kaributesting.v10.Routes
import com.github.mvysny.kaributesting.v10.spring.MockSpringServlet
import com.vaadin.flow.component.UI
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext

/** Starts a mocked Vaadin UI backed by the Spring context of the test before each test. */
abstract class MockSpringVaadinTests {
    @Autowired
    private lateinit var context: ApplicationContext

    @BeforeEach
    fun setUpVaadin() {
        val routes = Routes().autoDiscoverViews("tech.testsys.web.app")
        MockVaadin.setup({ UI() }, MockSpringServlet(routes, context) { UI() })
    }

    @AfterEach
    fun tearDownVaadin() = MockVaadin.tearDown()
}
