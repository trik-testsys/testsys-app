package tech.testsys.web.app.security

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import com.vaadin.flow.component.UI
import com.vaadin.flow.server.VaadinService
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.mock.web.MockHttpSession
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextImpl
import org.springframework.security.web.context.HttpSessionSecurityContextRepository
import org.springframework.security.web.session.HttpSessionDestroyedEvent
import tech.testsys.web.app.MockSpringVaadinTests
import tech.testsys.web.app.service.CurrentUser
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicReference

@SpringBootTest
class SecurityConfigurationTests : MockSpringVaadinTests() {
    @Autowired
    private lateinit var currentUser: CurrentUser

    private val logger =
        assertInstanceOf(Logger::class.java, LoggerFactory.getLogger(SecurityConfiguration::class.java))
    private val appender = ListAppender<ILoggingEvent>().apply { start() }

    @AfterEach
    fun detachAppender() {
        logger.detachAppender(appender)
    }

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

    @Test
    fun `should write the supervisor and user ids at INFO when the session of a supervisor signed in as a user ends`() {
        val event = destroyedSession(CabinetPrincipal(userId = 42, kind = UserKind.MULTIPLE_ROLE, impersonatedBy = 7))

        val lines = logged { SecurityConfiguration().sessionDestroyed(event) }

        assertEquals(listOf(Level.INFO to "Session of supervisor 7 as user 42 ended"), lines)
    }

    @Test
    fun `should write nothing when the session of a user who signed in themselves ends`() {
        val event = destroyedSession(CabinetPrincipal(userId = 42, kind = UserKind.MULTIPLE_ROLE))

        val lines = logged { SecurityConfiguration().sessionDestroyed(event) }

        assertTrue(lines.isEmpty())
    }

    private fun destroyedSession(principal: CabinetPrincipal): HttpSessionDestroyedEvent {
        val context = SecurityContextImpl(UsernamePasswordAuthenticationToken(principal, null, emptyList()))
        val session = MockHttpSession().apply {
            setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context)
        }
        return HttpSessionDestroyedEvent(session)
    }

    private fun logged(action: () -> Unit): List<Pair<Level, String>> {
        logger.addAppender(appender)
        action()
        return appender.list.map { line -> line.level to line.formattedMessage }
    }

    private fun pendingJavaScript() = UI.getCurrent().internals.let { internals ->
        internals.stateTree.runExecutionsBeforeClientResponse()
        internals.dumpPendingJavaScriptInvocations()
    }
}
