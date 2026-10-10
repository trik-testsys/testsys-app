package tech.testsys.web.service

import com.github.mvysny.fakeservlet.FakeHttpEnvironment
import com.github.mvysny.kaributesting.v10.MockVaadin
import com.github.mvysny.kaributesting.v10.Routes
import com.github.mvysny.kaributesting.v10.spring.MockSpringSecurity
import com.github.mvysny.kaributesting.v10.spring.MockSpringServlet
import com.vaadin.flow.component.UI
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationContext
import org.springframework.security.core.context.SecurityContextHolder
import java.util.concurrent.atomic.AtomicInteger

@SpringBootTest(classes = [ServiceWebConfiguration::class, ServiceTestConfiguration::class])
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

    private companion object {
        val addresses = AtomicInteger()
    }
}
