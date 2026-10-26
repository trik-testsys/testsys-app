package tech.testsys.web.app.security

import com.vaadin.flow.component.UI
import com.vaadin.flow.server.VaadinService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
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

    @ParameterizedTest
    @ValueSource(strings = ["", "/testsys"])
    fun `should send a signed-out user to the root of the application under the context path`(contextPath: String) {
        val request = MockHttpServletRequest("POST", "$contextPath/").apply {
            this.contextPath = contextPath
            addParameter("v-r", "uidl")
        }

        signedOutHandler().onLogoutSuccess(request, MockHttpServletResponse(), null)

        val transitions = pendingJavaScript().filter { call -> "window.open" in call.invocation.expression }
        assertEquals(listOf(listOf("$contextPath/", "_self")), transitions.map { call -> call.invocation.parameters })
    }

    @Test
    fun `should redirect a plain HTTP sign-out once under the context path`() {
        val request = MockHttpServletRequest("GET", "/testsys/logout").apply { contextPath = "/testsys" }
        val response = MockHttpServletResponse()

        signedOutHandler().onLogoutSuccess(request, response, null)

        assertEquals("/testsys/", response.redirectedUrl)
    }

    private fun pendingJavaScript() = UI.getCurrent().internals.let { internals ->
        internals.stateTree.runExecutionsBeforeClientResponse()
        internals.dumpPendingJavaScriptInvocations()
    }
}
