package tech.testsys.infra.database.api.session

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DataIntegrityViolationException
import tech.testsys.domain.contract.persistence.repository.MultipleRoleUserRepository
import tech.testsys.infra.database.DatabaseIntegrationTests
import java.time.Instant
import java.time.temporal.ChronoUnit

class DatabaseCabinetSignInStoreTests : DatabaseIntegrationTests() {
    @Autowired
    private lateinit var store: DatabaseCabinetSignInStore

    @Autowired
    private lateinit var multipleRoleUsers: MultipleRoleUserRepository

    private val now = Instant.now().truncatedTo(ChronoUnit.MICROS)

    private val longAgo = now.minus(1, ChronoUnit.DAYS)

    @Test
    fun `should find a created sign-in while it is active`() {
        val signIn = userSignIn(student())
        store.create("a", signIn, now)

        val found = store.find("a", idleSince = longAgo)

        assertEquals(signIn, found)
    }

    @Test
    fun `should keep the supervisor of a sign-in as a user`() {
        val signIn = userSignIn(student(), impersonatedBy = supervisor())
        store.create("a", signIn, now)

        val found = store.find("a", idleSince = longAgo)

        assertEquals(signIn, found)
    }

    @Test
    fun `should find a service sign-in without a user`() {
        store.create("a", StoredCabinetSignIn.Service, now)

        val found = store.find("a", idleSince = longAgo)

        assertEquals(StoredCabinetSignIn.Service, found)
    }

    @Test
    fun `should not find an idle sign-in and keep it until idle sign-ins are deleted`() {
        val signIn = userSignIn(student())
        store.create("a", signIn, longAgo)

        val found = store.find("a", idleSince = now.minus(1, ChronoUnit.HOURS))

        assertNull(found)
        assertEquals(listOf(signIn), store.deleteIdle(before = now.minus(1, ChronoUnit.HOURS)))
    }

    @Test
    fun `should keep a touched sign-in active`() {
        val signIn = userSignIn(student())
        store.create("a", signIn, longAgo)
        store.touch("a", now)

        val found = store.find("a", idleSince = now.minus(1, ChronoUnit.HOURS))

        assertEquals(signIn, found)
    }

    @Test
    fun `should refuse a second sign-in with the same token`() {
        val signIn = userSignIn(student())
        store.create("a", signIn, now)

        assertThrows<DataIntegrityViolationException> { store.create("a", signIn, now) }
    }

    @Nested
    inner class DeleteTests {
        @Test
        fun `should delete one sign-in`() {
            val signIn = userSignIn(student())
            store.create("a", signIn, now)
            store.create("b", signIn, now)

            val isDeleted = store.delete("a")

            assertTrue(isDeleted)
            assertNull(store.find("a", idleSince = longAgo))
            assertEquals(signIn, store.find("b", idleSince = longAgo))
        }

        @Test
        fun `should report that an absent sign-in was not deleted`() {
            val isDeleted = store.delete("absent")

            assertFalse(isDeleted)
        }

        @Test
        fun `should delete own sign-ins of a user except the given one and keep those of a supervisor`() {
            val supervisor = supervisor()
            val user = student()
            store.create("own", userSignIn(user), now)
            store.create("current", userSignIn(user), now)
            store.create("supervisor", userSignIn(user, impersonatedBy = supervisor), now)
            store.create("other", userSignIn(student()), now)

            store.deleteOwn(user, exceptTokenHash = "current")

            assertNull(store.find("own", idleSince = longAgo))
            assertNotNull(store.find("current", idleSince = longAgo))
            assertNotNull(store.find("supervisor", idleSince = longAgo))
            assertNotNull(store.find("other", idleSince = longAgo))
        }

        @Test
        fun `should delete all own sign-ins of a user without an exception`() {
            val user = student()
            store.create("a", userSignIn(user), now)
            store.create("b", userSignIn(user), now)

            store.deleteOwn(user)

            assertNull(store.find("a", idleSince = longAgo))
            assertNull(store.find("b", idleSince = longAgo))
        }

        @Test
        fun `should delete and return the sign-ins of a supervisor and of the supervisor as users`() {
            val supervisor = supervisor()
            val user = student()
            val own = StoredCabinetSignIn.User(userId = supervisor, kind = CabinetSignInUserKind.SUPERVISOR)
            val asUser = userSignIn(user, impersonatedBy = supervisor)
            store.create("supervisor", own, now)
            store.create("as-user", asUser, now)
            store.create("user", userSignIn(user), now)

            val deleted = store.deleteAllOf(supervisor)

            assertEquals(setOf(own, asUser), deleted.toSet())
            assertNull(store.find("supervisor", idleSince = longAgo))
            assertNull(store.find("as-user", idleSince = longAgo))
            assertEquals(userSignIn(user), store.find("user", idleSince = longAgo))
        }

        @Test
        fun `should delete and return idle sign-ins only`() {
            val idle = userSignIn(student())
            val active = userSignIn(student())
            store.create("idle", idle, longAgo)
            store.create("active", active, now)

            val deleted = store.deleteIdle(before = now.minus(1, ChronoUnit.HOURS))

            assertEquals(listOf(idle), deleted)
            assertNull(store.find("idle", idleSince = longAgo.minus(1, ChronoUnit.DAYS)))
            assertEquals(active, store.find("active", idleSince = longAgo))
        }

        @Test
        fun `should delete the sign-ins of a removed user`() {
            val user = fixtures.student()
            store.create("a", userSignIn(CabinetSignInUserId(user.id.value)), now)

            multipleRoleUsers.removeById(user.id)

            assertNull(store.find("a", idleSince = longAgo))
        }
    }

    private fun student(): CabinetSignInUserId = CabinetSignInUserId(fixtures.student().id.value)

    private fun supervisor(): CabinetSignInUserId = CabinetSignInUserId(fixtures.supervisor().id.value)

    private fun userSignIn(user: CabinetSignInUserId, impersonatedBy: CabinetSignInUserId? = null): StoredCabinetSignIn.User =
        StoredCabinetSignIn.User(userId = user, kind = CabinetSignInUserKind.MULTIPLE_ROLE, impersonatedBy = impersonatedBy)
}
