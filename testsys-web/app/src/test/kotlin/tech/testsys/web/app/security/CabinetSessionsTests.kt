package tech.testsys.web.app.security

import com.vaadin.flow.internal.CurrentInstance
import com.vaadin.flow.server.VaadinRequest
import com.vaadin.flow.server.VaadinServletRequest
import com.vaadin.flow.server.VaadinServletService
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.security.core.session.SessionRegistryImpl
import tech.testsys.infra.database.api.session.CabinetSignInStore
import tech.testsys.infra.database.api.session.CabinetSignInUserId
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

    private companion object {
        const val USER_ID = 7L
        const val SUPERVISOR_ID = 1L
    }
}
