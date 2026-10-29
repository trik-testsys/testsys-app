package tech.testsys.web.app.service.user

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.security.core.session.SessionRegistry
import tech.testsys.domain.contract.persistence.repository.AccessRestorationRequestRepository
import tech.testsys.domain.contract.persistence.repository.MultipleRoleUserRepository
import tech.testsys.domain.contract.persistence.repository.RegistrationRequestRepository
import tech.testsys.domain.contract.persistence.repository.UserRepository
import tech.testsys.operation.config.PublicCommunityOwner
import tech.testsys.operation.error.InvalidAccessTokenError
import tech.testsys.operation.error.InvalidCredentialsError
import tech.testsys.operation.error.OperationException
import tech.testsys.web.app.AppFixtures
import tech.testsys.web.app.AppTestConfiguration
import tech.testsys.web.app.RecordedMail
import tech.testsys.web.app.security.CabinetPrincipal
import tech.testsys.web.app.security.UserKind

@SpringBootTest
@Import(AppTestConfiguration::class)
class UserServiceTests {
    @Autowired
    private lateinit var service: UserService

    @Autowired
    private lateinit var fixtures: AppFixtures

    @Autowired
    private lateinit var mail: RecordedMail

    @Autowired
    private lateinit var requests: RegistrationRequestRepository

    @Autowired
    private lateinit var accessRestorationRequests: AccessRestorationRequestRepository

    @Autowired
    private lateinit var multipleRoleUsers: MultipleRoleUserRepository

    @Autowired
    private lateinit var users: UserRepository

    @Autowired
    private lateinit var sessionRegistry: SessionRegistry

    @Nested
    inner class AuthenticateTests {
        @Test
        fun `should return the principal of the user with the access code`() {
            val accessToken = fixtures.unique("token")
            val user = fixtures.userOf(UserKind.OBSERVER, rawAccessToken = accessToken)

            val principal = service.authenticate(accessToken)

            assertEquals(CabinetPrincipal.of(user), principal)
        }

        @Test
        fun `should fail with InvalidAccessTokenError if no user has the access code`() {
            val accessToken = fixtures.unique("token")

            val failure = assertThrows(OperationException::class.java) { service.authenticate(accessToken) }

            assertEquals(InvalidAccessTokenError, failure.error)
        }
    }

    @Nested
    inner class AuthenticateByPasswordTests {
        @Test
        fun `should return the principal of the user with the e-mail address and the password`() {
            val user = fixtures.multipleRoleUser()

            val principal = service.authenticateByPassword(user.data.email, fixtures.password)

            assertEquals(CabinetPrincipal.of(user), principal)
        }

        @Test
        fun `should refuse a wrong password and keep the last login`() {
            val user = fixtures.multipleRoleUser()

            val failure = assertThrows(OperationException::class.java) {
                service.authenticateByPassword(user.data.email, fixtures.password + "!")
            }

            assertEquals(InvalidCredentialsError, failure.error)
            assertEquals(emptyMap<Any, Any>(), users.findLastLogins(listOf(user.id)))
        }

        @Test
        fun `should refuse the technical owner`() {
            val owner = requireNotNull(multipleRoleUsers.findByEmail(PublicCommunityOwner.EMAIL))
            val before = users.findLastLogins(listOf(owner.id))

            val failure = assertThrows(OperationException::class.java) {
                service.authenticateByPassword(PublicCommunityOwner.EMAIL, fixtures.password)
            }

            assertEquals(InvalidCredentialsError, failure.error)
            assertEquals(before, users.findLastLogins(listOf(owner.id)))
        }
    }

    @Nested
    inner class ConfirmRegistrationTests {
        @Test
        fun `should keep the spent attempt if the confirmation code differs`() {
            val email = "${fixtures.unique("attempt")}@example.com".lowercase()
            val requestId = service.requestRegistration(email)
            val wrongCode = fixtures.otherCode(mail.confirmationCodes.getValue(email))

            assertThrows(OperationException::class.java) {
                service.confirmRegistration(requestId, confirmationCode = wrongCode, name = "Пользователь", password = PASSWORD)
            }

            assertEquals(2, requests.findById(requestId)?.data?.attemptsLeft)
        }

        @Test
        fun `should create the user who signs in with the chosen password`() {
            val email = "${fixtures.unique("registered")}@example.com".lowercase()
            val requestId = service.requestRegistration(email)
            val code = mail.confirmationCodes.getValue(email)

            service.confirmRegistration(requestId, confirmationCode = code, name = "Пользователь", password = PASSWORD)

            val user = requireNotNull(multipleRoleUsers.findByEmail(email))
            assertEquals(CabinetPrincipal.of(user), service.authenticateByPassword(email, PASSWORD))
        }
    }

    @Nested
    inner class ConfirmAccessRestorationTests {
        @Test
        fun `should keep the spent attempt if the confirmation code differs`() {
            val user = fixtures.multipleRoleUser()
            val email = user.data.email
            service.requestAccessRestoration(email)
            val wrongCode = fixtures.otherCode(mail.confirmationCodes.getValue(email))

            assertThrows(OperationException::class.java) {
                service.confirmAccessRestoration(email, confirmationCode = wrongCode, password = PASSWORD)
            }

            assertEquals(2, accessRestorationRequests.findByUser(user.id)?.data?.attemptsLeft)
        }

        @Test
        fun `should sign in with the new password and refuse the previous one`() {
            val user = fixtures.multipleRoleUser()
            val email = user.data.email
            service.requestAccessRestoration(email)

            service.confirmAccessRestoration(email, confirmationCode = mail.confirmationCodes.getValue(email), password = PASSWORD)

            assertEquals(CabinetPrincipal.of(user), service.authenticateByPassword(email, PASSWORD))
            val failure = assertThrows(OperationException::class.java) {
                service.authenticateByPassword(email, fixtures.password)
            }
            assertEquals(InvalidCredentialsError, failure.error)
        }

        @Test
        fun `should end every session of the user`() {
            val user = fixtures.multipleRoleUser()
            val email = user.data.email
            val sessionId = fixtures.unique("session")
            sessionRegistry.registerNewSession(sessionId, CabinetPrincipal.of(user))
            service.requestAccessRestoration(email)

            service.confirmAccessRestoration(email, confirmationCode = mail.confirmationCodes.getValue(email), password = PASSWORD)

            assertTrue(requireNotNull(sessionRegistry.getSessionInformation(sessionId)).isExpired)
        }
    }

    private companion object {
        const val PASSWORD = "New-password-2"
    }
}
