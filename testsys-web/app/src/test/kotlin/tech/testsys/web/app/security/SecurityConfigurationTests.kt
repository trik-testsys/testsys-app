package tech.testsys.web.app.security

import com.vaadin.flow.server.VaadinService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import tech.testsys.web.app.MockSpringVaadinTests
import tech.testsys.web.app.service.CurrentUser
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicReference

@SpringBootTest
class SecurityConfigurationTests : MockSpringVaadinTests() {
    @Autowired
    private lateinit var currentUser: CurrentUser

    @Test
    fun `should give background work of a page the signed-in user`() {
        val user = fixtures.userOf(UserKind.MULTIPLE_ROLE)
        signIn(user)

        val result = CompletableFuture.supplyAsync({ currentUser.user() }, VaadinService.getCurrent().executor).get()

        assertEquals(user.id, result.id)
    }

    @Test
    fun `should give background work the log context of its submitter and clear it afterwards`() {
        val seen = AtomicReference<String?>()
        val left = AtomicReference<Map<String, String>?>()
        MDC.put("route", "/developer/tasks")
        val task = SecurityConfiguration().securityContextTaskDecorator().decorate { seen.set(MDC.get("route")) }
        MDC.clear()

        val worker = Thread {
            task.run()
            left.set(MDC.getCopyOfContextMap())
        }
        worker.start()
        worker.join()

        assertEquals("/developer/tasks", seen.get())
        assertTrue(left.get().isNullOrEmpty())
    }
}
