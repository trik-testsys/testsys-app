package tech.testsys.web.app.security

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.security.core.session.SessionRegistryImpl

class CabinetSessionsTests {
    private val registry = SessionRegistryImpl()
    private val sessions = CabinetSessions(registry)

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

    private fun isExpired(sessionId: String): Boolean = checkNotNull(registry.getSessionInformation(sessionId)).isExpired

    private companion object {
        const val USER_ID = 7L
        const val SUPERVISOR_ID = 1L
    }
}
