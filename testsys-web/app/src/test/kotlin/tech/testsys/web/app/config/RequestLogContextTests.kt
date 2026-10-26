package tech.testsys.web.app.config

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import com.vaadin.flow.component.ComponentEventListener
import com.vaadin.flow.component.DetachEvent
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.html.Div
import com.vaadin.flow.router.AfterNavigationEvent
import com.vaadin.flow.router.AfterNavigationListener
import com.vaadin.flow.router.BeforeEnterEvent
import com.vaadin.flow.router.BeforeEnterListener
import com.vaadin.flow.router.Location
import com.vaadin.flow.server.ServiceInitEvent
import com.vaadin.flow.server.UIInitEvent
import com.vaadin.flow.server.UIInitListener
import com.vaadin.flow.server.VaadinRequest
import com.vaadin.flow.server.VaadinResponse
import com.vaadin.flow.server.VaadinService
import com.vaadin.flow.server.VaadinSession
import com.vaadin.flow.server.WrappedSession
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import tech.testsys.web.app.security.CabinetPrincipal
import tech.testsys.web.app.security.UserKind

class RequestLogContextTests {
    private val context = RequestLogContext()
    private val httpSession = mockk<WrappedSession>(relaxed = true)
    private val response = mockk<VaadinResponse>()

    @BeforeEach
    fun clearLogContext() {
        MDC.clear()
    }

    @AfterEach
    fun clearContexts() {
        MDC.clear()
        SecurityContextHolder.clearContext()
    }

    @Nested
    inner class ServiceInitTests {
        private val service = mockk<VaadinService>(relaxed = true)
        private val event = mockk<ServiceInitEvent>(relaxed = true) { every { source } returns service }
        private val ui = mockk<UI>(relaxed = true)
        private val uiInit = slot<UIInitListener>()
        private val beforeEnter = slot<BeforeEnterListener>()
        private val afterNavigation = slot<AfterNavigationListener>()
        private val detach = slot<ComponentEventListener<DetachEvent>>()

        @BeforeEach
        fun captureListeners() {
            val vaadinSession = mockk<VaadinSession> { every { session } returns httpSession }
            every { service.addUIInitListener(capture(uiInit)) } returns mockk()
            every { ui.uiId } returns 3
            every { ui.session } returns vaadinSession
            every { ui.addBeforeEnterListener(capture(beforeEnter)) } returns mockk()
            every { ui.addAfterNavigationListener(capture(afterNavigation)) } returns mockk()
            every { ui.addDetachListener(capture(detach)) } returns mockk()
        }

        @Test
        fun `should register the request interceptor`() {
            context.serviceInit(event)

            verify { event.addVaadinRequestInterceptor(context) }
        }

        @Test
        fun `should put the user and the page into the log context before entering a page`() {
            initUi()
            signIn(userId = 42)
            val entered = mockk<BeforeEnterEvent> { every { location } returns Location("developer/tasks/7") }

            beforeEnter.captured.beforeEnter(entered)

            assertEquals("42", MDC.get("user"))
            assertEquals("/developer/tasks/7", MDC.get("route"))
        }

        @Test
        fun `should remember the opened page for the UI`() {
            initUi()
            val navigation = mockk<AfterNavigationEvent> {
                every { location } returns Location("developer/tasks/7")
                every { activeChain } returns listOf(Div())
            }

            afterNavigation.captured.afterNavigation(navigation)

            assertEquals("/developer/tasks/7", MDC.get("route"))
            verify { httpSession.setAttribute("testsys.route.3", "/developer/tasks/7") }
        }

        @Test
        fun `should forget the page of a detached UI`() {
            initUi()

            detach.captured.onComponentEvent(mockk())

            verify { httpSession.removeAttribute("testsys.route.3") }
        }

        @Test
        fun `should ignore an invalidated session when a UI is detached`() {
            initUi()
            every { httpSession.removeAttribute(any()) } throws IllegalStateException("Session already invalidated")

            assertDoesNotThrow { detach.captured.onComponentEvent(mockk()) }
        }

        private fun initUi() {
            context.serviceInit(event)
            uiInit.captured.uiInit(UIInitEvent(ui, service))
        }
    }

    @Nested
    inner class RequestStartTests {
        @Test
        fun `should put the signed-in user and the page of the UI into the log context`() {
            signIn(userId = 42)
            every { httpSession.getAttribute("testsys.route.3") } returns "/developer/tasks"

            context.requestStart(request(uiId = "3"), response)

            assertEquals("42", MDC.get("user"))
            assertEquals("/developer/tasks", MDC.get("route"))
        }

        @Test
        fun `should put the page of the UI into the log context for a file transfer request`() {
            signIn(userId = 42)
            every { httpSession.getAttribute("testsys.route.3") } returns "/developer/tasks/7"

            context.requestStart(request(uiId = null, path = "/VAADIN/dynamic/resource/3/a1b2/tasks.zip"), response)

            assertEquals("42", MDC.get("user"))
            assertEquals("/developer/tasks/7", MDC.get("route"))
        }

        @Test
        fun `should put the user and the page of the UI into the log context for a push request without a response`() {
            signIn(userId = 42)
            every { httpSession.getAttribute("testsys.route.3") } returns "/developer/tasks"

            context.requestStart(request(uiId = "3"), null)

            assertEquals("42", MDC.get("user"))
            assertEquals("/developer/tasks", MDC.get("route"))
        }

        @Test
        fun `should put the supervisor signed in as the user into the log context`() {
            signIn(userId = 42, impersonatedBy = 7)

            context.requestStart(request(uiId = null), response)

            assertEquals("42", MDC.get("user"))
            assertEquals("7", MDC.get("impersonatedBy"))
        }

        @Test
        fun `should leave the supervisor out of the log context for a user who signed in themselves`() {
            MDC.put("impersonatedBy", "7")
            signIn(userId = 42)

            context.requestStart(request(uiId = null), response)

            assertNull(MDC.get("impersonatedBy"))
        }

        @Test
        fun `should leave the user out of the log context for a guest`() {
            context.requestStart(request(uiId = null), response)

            assertNull(MDC.get("user"))
            assertNull(MDC.get("route"))
        }
    }

    @Nested
    inner class RequestEndTests {
        @Test
        fun `should remove the user and the page from the log context`() {
            signIn(userId = 42)
            every { httpSession.getAttribute("testsys.route.3") } returns "/developer/tasks"
            context.requestStart(request(uiId = "3"), response)

            context.requestEnd(request(uiId = "3"), response, null)

            assertNull(MDC.get("user"))
            assertNull(MDC.get("route"))
        }

        @Test
        fun `should remove the supervisor signed in as the user from the log context`() {
            signIn(userId = 42, impersonatedBy = 7)
            context.requestStart(request(uiId = null), response)

            context.requestEnd(request(uiId = null), response, null)

            assertNull(MDC.get("impersonatedBy"))
        }
    }

    @Nested
    inner class EnteringTests {
        @Test
        fun `should put the user signed in during the request into the log context`() {
            context.requestStart(request(uiId = null), response)
            signIn(userId = 42)

            context.entering(entered("developer/tasks"))

            assertEquals("42", MDC.get("user"))
        }

        @Test
        fun `should put the entered page without query into the log context`() {
            every { httpSession.getAttribute("testsys.route.3") } returns "/developer/tasks"
            context.requestStart(request(uiId = "3"), response)

            context.entering(entered("developer/tasks/7?tab=history"))

            assertEquals("/developer/tasks/7", MDC.get("route"))
        }

        private fun entered(path: String): BeforeEnterEvent = mockk { every { location } returns Location(path) }
    }

    @Nested
    inner class OpenedTests {
        private val logger =
            assertInstanceOf(Logger::class.java, LoggerFactory.getLogger(RequestLogContext::class.java))
        private val appender = ListAppender<ILoggingEvent>().apply { start() }

        @AfterEach
        fun detachAppender() {
            logger.detachAppender(appender)
            logger.level = null
        }

        @Test
        fun `should write the opened page without query and remember it for the UI`() {
            logger.addAppender(appender)
            logger.level = Level.DEBUG
            val session = mockk<VaadinSession> { every { this@mockk.session } returns httpSession }
            val event = mockk<AfterNavigationEvent> {
                every { location } returns Location("developer/tasks/7?tab=history")
                every { activeChain } returns listOf(Div())
            }

            context.opened(event, uiId = 3, session = session)

            assertEquals(listOf("Opened /developer/tasks/7 (Div)"), appender.list.map { line -> line.formattedMessage })
            assertEquals("/developer/tasks/7", MDC.get("route"))
            verify { httpSession.setAttribute("testsys.route.3", "/developer/tasks/7") }
        }
    }

    private fun signIn(userId: Long, impersonatedBy: Long? = null) {
        val principal = CabinetPrincipal(
            userId = userId,
            kind = UserKind.MULTIPLE_ROLE,
            impersonatedBy = impersonatedBy,
        )
        SecurityContextHolder.getContext().authentication = UsernamePasswordAuthenticationToken(principal, null, emptyList())
    }

    private fun request(uiId: String?, path: String? = null): VaadinRequest = mockk {
        every { getParameter("v-uiId") } returns uiId
        every { pathInfo } returns path
        every { getWrappedSession(false) } returns httpSession
    }
}
