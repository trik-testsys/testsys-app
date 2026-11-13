package tech.testsys.web.app.security

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import com.vaadin.flow.internal.CurrentInstance
import com.vaadin.flow.server.VaadinRequest
import com.vaadin.flow.server.VaadinServletRequest
import com.vaadin.flow.server.VaadinServletService
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.security.core.session.SessionRegistryImpl
import tech.testsys.infra.database.api.session.CabinetSignInStore
import tech.testsys.infra.database.api.session.CabinetSignInUserId
import tech.testsys.infra.database.api.session.CabinetSignInUserKind
import tech.testsys.infra.database.api.session.StoredCabinetSignIn
import java.time.Clock
import java.time.Duration

class CabinetSessionsTests {
    private val registry = SessionRegistryImpl()

    private val store = mockk<CabinetSignInStore>(relaxed = true)

    private val signIns = RememberedSignIns(store, "TESTSYS_APP_SIGN_IN", Duration.ofMinutes(30), Clock.systemUTC())

    private val sessions = CabinetSessions(registry, store, signIns)

    @AfterEach
    fun clearCurrentInstances() {
        CurrentInstance.clearAll()
    }

    @Test
    fun `should expire every session of the user`() {
        registry.registerNewSession("first", CabinetPrincipal(userId = USER_ID, kind = UserKind.MULTIPLE_ROLE))
        registry.registerNewSession("second", CabinetPrincipal(userId = USER_ID, kind = UserKind.MULTIPLE_ROLE))

        sessions.expire(USER_ID)

        assertTrue(isExpired("first"))
        assertTrue(isExpired("second"))
    }

    @Test
    fun `should keep the excepted session of the user`() {
        registry.registerNewSession("current", CabinetPrincipal(userId = USER_ID, kind = UserKind.MULTIPLE_ROLE))
        registry.registerNewSession("other", CabinetPrincipal(userId = USER_ID, kind = UserKind.MULTIPLE_ROLE))

        sessions.expire(USER_ID, except = "current")

        assertFalse(isExpired("current"))
        assertTrue(isExpired("other"))
    }

    @Test
    fun `should keep the sessions of a supervisor signed in as the user and of other users`() {
        registry.registerNewSession(
            "impersonated",
            CabinetPrincipal(userId = USER_ID, kind = UserKind.MULTIPLE_ROLE, impersonatedBy = SUPERVISOR_ID),
        )
        registry.registerNewSession("another", CabinetPrincipal(userId = USER_ID + 1, kind = UserKind.MULTIPLE_ROLE))

        sessions.expire(USER_ID)

        assertFalse(isExpired("impersonated"))
        assertFalse(isExpired("another"))
    }

    @Test
    fun `should expire the sessions of the user as a supervisor signed in as other users`() {
        registry.registerNewSession(
            "as-user",
            CabinetPrincipal(userId = USER_ID + 1, kind = UserKind.MULTIPLE_ROLE, impersonatedBy = USER_ID),
        )
        registry.registerNewSession(
            "as-observer",
            CabinetPrincipal(userId = USER_ID + 2, kind = UserKind.OBSERVER, impersonatedBy = USER_ID),
        )
        registry.registerNewSession(
            "by-another",
            CabinetPrincipal(userId = USER_ID + 1, kind = UserKind.MULTIPLE_ROLE, impersonatedBy = SUPERVISOR_ID),
        )

        sessions.expire(USER_ID)

        assertTrue(isExpired("as-user"))
        assertTrue(isExpired("as-observer"))
        assertFalse(isExpired("by-another"))
    }

    @Test
    fun `should delete the remembered sign-ins of the user as a supervisor signed in as other users`() {
        sessions.expire(USER_ID)

        verify { store.deleteImpersonatedBy(CabinetSignInUserId(USER_ID)) }
    }

    @Test
    fun `should write at INFO the end of a deleted sign-in of the user as a supervisor signed in as another user`() {
        val asUser = StoredCabinetSignIn.User(
            userId = CabinetSignInUserId(USER_ID + 1),
            kind = CabinetSignInUserKind.MULTIPLE_ROLE,
            impersonatedBy = CabinetSignInUserId(USER_ID),
        )
        val own = StoredCabinetSignIn.User(userId = CabinetSignInUserId(USER_ID + 2), kind = CabinetSignInUserKind.MULTIPLE_ROLE)
        every { store.deleteImpersonatedBy(CabinetSignInUserId(USER_ID)) } returns listOf(asUser, own)

        val lines = logged { sessions.expire(USER_ID) }

        assertEquals(listOf(Level.INFO to "Session of supervisor $USER_ID as user ${USER_ID + 1} ended"), lines)
    }

    @Test
    fun `should delete the remembered own sign-ins of the user outside a request`() {
        sessions.expire(USER_ID, except = "current")

        verify { store.deleteOwn(CabinetSignInUserId(USER_ID), exceptTokenHash = null) }
    }

    @Test
    fun `should keep the remembered sign-in of the excepted session of the current request`() {
        val request = MockHttpServletRequest()
        val session = request.getSession(true)
        signIns.attach(checkNotNull(session), "hash")
        CurrentInstance.set(VaadinRequest::class.java, VaadinServletRequest(request, mockk<VaadinServletService>()))

        sessions.expire(USER_ID, except = session.id)

        verify { store.deleteOwn(CabinetSignInUserId(USER_ID), exceptTokenHash = "hash") }
    }

    private fun isExpired(sessionId: String): Boolean = checkNotNull(registry.getSessionInformation(sessionId)).isExpired

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
        const val USER_ID = 7L
        const val SUPERVISOR_ID = 1L
    }
}
