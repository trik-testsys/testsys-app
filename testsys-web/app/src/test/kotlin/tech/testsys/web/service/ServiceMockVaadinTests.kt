package tech.testsys.web.service

import com.github.mvysny.fakeservlet.FakeHttpEnvironment
import com.github.mvysny.kaributesting.v10.MockVaadin
import com.github.mvysny.kaributesting.v10.Routes
import com.github.mvysny.kaributesting.v10._click
import com.github.mvysny.kaributesting.v10._find
import com.github.mvysny.kaributesting.v10._get
import com.github.mvysny.kaributesting.v10.spring.MockSpringSecurity
import com.github.mvysny.kaributesting.v10.spring.MockSpringServlet
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.router.RouterLink
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationContext
import org.springframework.security.core.context.SecurityContextHolder
import java.util.concurrent.atomic.AtomicInteger

@SpringBootTest(
    classes = [ServiceWebConfiguration::class, ServiceTestConfiguration::class, ServiceMetricsTestConfiguration::class],
    properties = ["spring.application.version=$SERVICE_TEST_VERSION"],
)
abstract class ServiceMockVaadinTests {
    @Autowired
    private lateinit var context: ApplicationContext

    @BeforeEach
    fun setUp() {
        FakeHttpEnvironment.remoteAddr = "192.0.2.${addresses.incrementAndGet()}"
        MockSpringSecurity.mock()
        MockVaadin.setup({ UI() }, MockSpringServlet(Routes().autoDiscoverViews("tech.testsys.web.service"), context) { UI() })
    }

    @AfterEach
    fun tearDown() {
        MockVaadin.tearDown()
        SecurityContextHolder.clearContext()
        FakeHttpEnvironment.remoteAddr = "127.0.0.1"
    }

    /** Clicks the link named [label] in the menu of the service header. */
    protected fun clickMenuLink(label: String) {
        UI.getCurrent()._find<RouterLink> { classes = "ts-mega__link" }.single { link -> link._get<Span>().text == label }._click()
    }

    private companion object {
        val addresses = AtomicInteger()
    }
}
