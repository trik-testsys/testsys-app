package tech.testsys.web.app.service

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import tech.testsys.domain.builder.api.multipleRoleUser
import tech.testsys.domain.builder.api.multipleRoleUserData
import tech.testsys.domain.builder.api.observer
import tech.testsys.domain.builder.api.observerData
import tech.testsys.domain.contract.persistence.repository.MultipleRoleUserRepository
import tech.testsys.domain.contract.persistence.repository.ObserverRepository
import tech.testsys.domain.contract.persistence.repository.ParticipantRepository
import tech.testsys.domain.model.user.AccessTokenHash
import tech.testsys.domain.model.user.HashAlgorithm
import tech.testsys.domain.model.user.MultipleRoleUserId
import tech.testsys.domain.model.user.PasswordHash
import tech.testsys.domain.model.user.SingleRoleUserId
import tech.testsys.web.app.security.CabinetPrincipal
import tech.testsys.web.app.security.UserKind
import java.time.Instant

class SecurityCurrentUserTests {
    private val multipleRoleUsers = mockk<MultipleRoleUserRepository>()
    private val participants = mockk<ParticipantRepository>()
    private val observers = mockk<ObserverRepository>()
    private val currentUser = SecurityCurrentUser(multipleRoleUsers, participants, observers)
    private val multipleRoleUser = multipleRoleUser {
        id = USER_ID
        createdAt = Instant.EPOCH
        data = multipleRoleUserData {
            passwordHash = PasswordHash("password-hash")
            name = "Пользователь"
            email = "user@example.com"
        }
    }
    private val observer = observer {
        id = USER_ID
        createdAt = Instant.EPOCH
        data = observerData {
            storedAccessToken(AccessTokenHash("observer-hash", HashAlgorithm.HmacSha256))
            name = "Наблюдатель"
            community(1)
        }
    }

    @AfterEach
    fun signOut() = SecurityContextHolder.clearContext()

    @Nested
    inner class UserTests {
        @Test
        fun `should load the signed-in user of their kind`() {
            every { observers.findById(SingleRoleUserId(USER_ID)) } returns observer
            signIn(UserKind.OBSERVER)

            val user = currentUser.user()

            assertSame(observer, user)
        }

        @Test
        fun `should throw IllegalStateException without a signed-in user`() {
            assertThrows(IllegalStateException::class.java) { currentUser.user() }
        }

        @Test
        fun `should throw IllegalStateException if the signed-in user no longer exists`() {
            every { multipleRoleUsers.findById(MultipleRoleUserId(USER_ID)) } returns null
            signIn(UserKind.MULTIPLE_ROLE)

            assertThrows(IllegalStateException::class.java) { currentUser.user() }
        }
    }

    @Nested
    inner class MultipleRoleUserTests {
        @Test
        fun `should return the signed-in user with non-fixed roles`() {
            every { multipleRoleUsers.findById(MultipleRoleUserId(USER_ID)) } returns multipleRoleUser
            signIn(UserKind.MULTIPLE_ROLE)

            val user = currentUser.multipleRoleUser()

            assertSame(multipleRoleUser, user)
        }

        @Test
        fun `should throw IllegalStateException if the signed-in user has a fixed role`() {
            every { observers.findById(SingleRoleUserId(USER_ID)) } returns observer
            signIn(UserKind.OBSERVER)

            assertThrows(IllegalStateException::class.java) { currentUser.multipleRoleUser() }
        }
    }

    @Nested
    inner class SingleRoleUserTests {
        @Test
        fun `should return the signed-in user with a fixed role`() {
            every { observers.findById(SingleRoleUserId(USER_ID)) } returns observer
            signIn(UserKind.OBSERVER)

            val user = currentUser.singleRoleUser()

            assertSame(observer, user)
        }

        @Test
        fun `should throw IllegalStateException if the signed-in user has non-fixed roles`() {
            every { multipleRoleUsers.findById(MultipleRoleUserId(USER_ID)) } returns multipleRoleUser
            signIn(UserKind.MULTIPLE_ROLE)

            assertThrows(IllegalStateException::class.java) { currentUser.singleRoleUser() }
        }
    }

    private fun signIn(kind: UserKind) {
        val principal = CabinetPrincipal(userId = USER_ID, kind = kind)
        SecurityContextHolder.getContext().authentication = UsernamePasswordAuthenticationToken.authenticated(principal, null, emptyList())
    }

    private companion object {
        const val USER_ID = 7L
    }
}
