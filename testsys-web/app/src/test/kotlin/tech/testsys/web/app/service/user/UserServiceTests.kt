package tech.testsys.web.app.service.user

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import tech.testsys.domain.contract.persistence.repository.MultipleRoleUserRepository
import tech.testsys.domain.contract.persistence.repository.RegistrationRequestRepository
import tech.testsys.domain.model.user.RegistrationRole
import tech.testsys.operation.error.InvalidAccessTokenError
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
    private lateinit var multipleRoleUsers: MultipleRoleUserRepository

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
    inner class ConfirmRegistrationTests {
        @Test
        fun `should keep the spent attempt if the confirmation code differs`() {
            val email = "${fixtures.unique("attempt")}@example.com".lowercase()
            val requestId = service.requestRegistration(email)
            val wrongCode = fixtures.otherCode(mail.confirmationCodes.getValue(email))

            assertThrows(OperationException::class.java) {
                service.confirmRegistration(requestId, confirmationCode = wrongCode, name = "Пользователь", role = RegistrationRole.Student)
            }

            assertEquals(2, requests.findById(requestId)?.data?.attemptsLeft)
        }

        @Test
        fun `should return the access code mailed to the new user`() {
            val email = "${fixtures.unique("registered")}@example.com".lowercase()
            val requestId = service.requestRegistration(email)
            val code = mail.confirmationCodes.getValue(email)

            val accessToken = service.confirmRegistration(
                requestId,
                confirmationCode = code,
                name = "Пользователь",
                role = RegistrationRole.Student,
            )

            assertEquals(mail.accessTokens.getValue(email), accessToken)
            assertEquals(accessToken, multipleRoleUsers.findByEmail(email)?.data?.accessTokenHash?.value)
        }
    }
}
