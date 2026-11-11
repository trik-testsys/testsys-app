package tech.testsys.web.app.security

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import jakarta.servlet.http.Cookie
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.springframework.mock.web.MockFilterChain
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.context.HttpSessionSecurityContextRepository
import tech.testsys.infra.database.api.session.CabinetSignInStore
import tech.testsys.infra.database.api.session.CabinetSignInUserId
import tech.testsys.infra.database.api.session.CabinetSignInUserKind
import tech.testsys.infra.database.api.session.StoredCabinetSignIn
import java.time.Duration

class RememberedSignInsTests {
    private val store = mockk<CabinetSignInStore>(relaxed = true)

    private val clock = MutableClock()

    private val signIns = RememberedSignIns(store, COOKIE, IDLE_TIMEOUT, clock)

    private val signIn = StoredCabinetSignIn.User(userId = CabinetSignInUserId(USER_ID), kind = CabinetSignInUserKind.MULTIPLE_ROLE)

    @AfterEach
    fun clearContext() {
        SecurityContextHolder.clearContext()
    }

    @Test
    fun `should store the digest of a new token and set it in an http-only cookie`() {
        val request = MockHttpServletRequest().apply { contextPath = "/testsys" }
        val response = MockHttpServletResponse()
        val hash = slot<String>()

        signIns.remember(request, response, signIn)

        val cookie = checkNotNull(response.getCookie(COOKIE))
        verify { store.create(capture(hash), signIn, clock.instant()) }
        verify { store.deleteIdle(before = clock.instant() - IDLE_TIMEOUT) }
        assertNotEquals(cookie.value, hash.captured)
        assertEquals(64, hash.captured.length)
        assertTrue(cookie.isHttpOnly)
        assertEquals("/testsys", cookie.path)
        assertEquals("Lax", cookie.getAttribute("SameSite"))
        assertEquals(hash.captured, signIns.tokenHashOf(request.session))
    }

    @Test
    fun `should write at INFO the end of a deleted idle sign-in of a supervisor as a user only`() {
        val asUser = signIn.copy(impersonatedBy = CabinetSignInUserId(SUPERVISOR_ID))
        every { store.deleteIdle(any()) } returns listOf(signIn, asUser, StoredCabinetSignIn.Service)

        val lines = logged { signIns.remember(MockHttpServletRequest(), MockHttpServletResponse(), signIn) }

        assertEquals(listOf(Level.INFO to "Session of supervisor $SUPERVISOR_ID as user $USER_ID ended"), lines)
    }

    @Test
    fun `should replace the sign-in the session remembered before`() {
        val request = MockHttpServletRequest()
        signIns.remember(request, MockHttpServletResponse(), signIn)
        val previous = signIns.tokenHashOf(request.session)

        signIns.remember(request, MockHttpServletResponse(), signIn)

        verify { store.delete(checkNotNull(previous)) }
    }

    @Test
    fun `should forget the sign-in and clear the cookie`() {
        val request = MockHttpServletRequest()
        signIns.remember(request, MockHttpServletResponse(), signIn)
        val hash = checkNotNull(signIns.tokenHashOf(request.session))
        val response = MockHttpServletResponse()

        signIns.forget(request, response)

        verify { store.delete(hash) }
        assertEquals(0, response.getCookie(COOKIE)?.maxAge)
        assertNull(signIns.tokenHashOf(request.session))
    }

    @Nested
    inner class RestoreTests {
        private val filter = signIns.restoreFilter({ stored ->
            (stored as? StoredCabinetSignIn.User)?.let { user ->
                UsernamePasswordAuthenticationToken.authenticated(user.userId.value, null, emptyList())
            }
        })

        @Test
        fun `should sign a request in from the remembered sign-in of its cookie`() {
            every { store.find(any(), idleSince = clock.instant() - IDLE_TIMEOUT) } returns signIn
            val request = MockHttpServletRequest().apply { setCookies(Cookie(COOKIE, "token")) }

            filter.doFilter(request, MockHttpServletResponse(), MockFilterChain())

            val context = request.session.getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY)
            assertNotNull(context)
            assertEquals(USER_ID, SecurityContextHolder.getContext().authentication?.principal)
            assertNotNull(signIns.tokenHashOf(request.session))
        }

        @Test
        fun `should clear the cookie without an active sign-in`() {
            every { store.find(any(), any()) } returns null
            val request = MockHttpServletRequest().apply { setCookies(Cookie(COOKIE, "token")) }
            val response = MockHttpServletResponse()

            filter.doFilter(request, response, MockFilterChain())

            assertNull(request.getSession(false))
            assertEquals(0, response.getCookie(COOKIE)?.maxAge)
        }

        @Test
        fun `should forget a sign-in the interface cannot restore`() {
            every { store.find(any(), any()) } returns StoredCabinetSignIn.Service
            val request = MockHttpServletRequest().apply { setCookies(Cookie(COOKIE, "token")) }
            val response = MockHttpServletResponse()

            filter.doFilter(request, response, MockFilterChain())

            assertNull(SecurityContextHolder.getContext().authentication)
            verify { store.delete(any()) }
            assertEquals(0, response.getCookie(COOKIE)?.maxAge)
        }

        @Test
        fun `should record the request of a signed-in session at most once a minute`() {
            val request = MockHttpServletRequest()
            signIns.remember(request, MockHttpServletResponse(), signIn)
            val hash = checkNotNull(signIns.tokenHashOf(request.session))
            val signedIn = UsernamePasswordAuthenticationToken.authenticated(USER_ID, null, emptyList())
            SecurityContextHolder.getContext().authentication = signedIn

            clock.now += Duration.ofSeconds(30)
            filter.doFilter(request, MockHttpServletResponse(), MockFilterChain())
            clock.now += Duration.ofSeconds(31)
            filter.doFilter(request, MockHttpServletResponse(), MockFilterChain())

            verify(exactly = 1) { store.touch(hash, any()) }
            verify { store.touch(hash, clock.instant()) }
        }
    }

    private fun logged(action: () -> Unit): List<Pair<Level, String>> {
        val logger = assertInstanceOf(Logger::class.java, LoggerFactory.getLogger(SecurityConfiguration::class.java))
        val appender = ListAppender<ILoggingEvent>().apply { start() }
        logger.addAppender(appender)
        try {
            action()
        } finally {
            logger.detachAppender(appender)
        }
        return appender.list.map { line -> line.level to line.formattedMessage }
    }

    private companion object {
        const val COOKIE = "TESTSYS_APP_SIGN_IN"
        const val USER_ID = 7L
        const val SUPERVISOR_ID = 1L
        val IDLE_TIMEOUT: Duration = Duration.ofMinutes(30)
    }
}
