package tech.testsys.operation.user

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import tech.testsys.domain.contract.persistence.repository.MultipleRoleUserRepository
import tech.testsys.domain.contract.persistence.repository.ObserverRepository
import tech.testsys.domain.contract.persistence.repository.ParticipantRepository
import tech.testsys.domain.contract.persistence.repository.SupervisorRepository
import tech.testsys.domain.model.user.MultipleRoleUser
import tech.testsys.domain.model.user.Observer
import tech.testsys.domain.model.user.Participant
import tech.testsys.domain.model.user.Supervisor
import tech.testsys.operation.error.InvalidAccessTokenError
import tech.testsys.operation.error.getOrThrow
import tech.testsys.operation.util.assertRaises
import tech.testsys.operation.util.testMultipleRoleUser
import tech.testsys.operation.util.testObserver
import tech.testsys.operation.util.testParticipant
import tech.testsys.operation.util.testSupervisor
import kotlin.test.assertSame

class UserOperationsTests {

    private val multipleRoleUsers = mockk<MultipleRoleUserRepository>()
    private val participants = mockk<ParticipantRepository>()
    private val observers = mockk<ObserverRepository>()
    private val supervisors = mockk<SupervisorRepository>()
    private val operations = UserOperations(
        multipleRoleUserRepository = multipleRoleUsers,
        participantRepository = participants,
        observerRepository = observers,
        supervisorRepository = supervisors,
    )

    @BeforeEach
    fun setUp() {
        every { multipleRoleUsers.findByAccessToken(any()) } returns null
        every { participants.findByAccessToken(any()) } returns null
        every { observers.findByAccessToken(any()) } returns null
        every { supervisors.findByAccessToken(any()) } returns null
    }

    @Nested
    inner class AuthenticateTests {

        @Test
        fun `should return multiple-role user whose access code matches`() {
            val user = testMultipleRoleUser {}
            every { multipleRoleUsers.findByAccessToken("token") } returns user

            val result = operations.authenticate(accessToken = "token").getOrThrow()

            assertSame(user, result)
        }

        @Test
        fun `should return participant whose access code matches`() {
            val user = testParticipant()
            every { participants.findByAccessToken("participant") } returns user

            val result = operations.authenticate(accessToken = "participant").getOrThrow()

            assertSame(user, result)
        }

        @Test
        fun `should return observer whose access code matches`() {
            val user = testObserver()
            every { observers.findByAccessToken("observer") } returns user

            val result = operations.authenticate(accessToken = "observer").getOrThrow()

            assertSame(user, result)
        }

        @Test
        fun `should return supervisor whose access code matches`() {
            val user = testSupervisor()
            every { supervisors.findByAccessToken("supervisor") } returns user

            val result = operations.authenticate(accessToken = "supervisor").getOrThrow()

            assertSame(user, result)
        }

        @Test
        fun `should raise InvalidAccessTokenError if no user has the access code`() {
            assertRaises(InvalidAccessTokenError) {
                operations.authenticate(accessToken = "unknown")
            }
        }

        @Test
        fun `should raise InvalidAccessTokenError if access code differs only by surrounding whitespace`() {
            every { multipleRoleUsers.findByAccessToken("token") } returns testMultipleRoleUser {}

            assertRaises(InvalidAccessTokenError) {
                operations.authenticate(accessToken = " token ")
            }
        }

        @Test
        fun `should not update any user when authenticating`() {
            every { multipleRoleUsers.findByAccessToken("token") } returns testMultipleRoleUser {}

            operations.authenticate(accessToken = "token")

            verify(exactly = 0) {
                multipleRoleUsers.update(any<MultipleRoleUser>())
                participants.update(any<Participant>())
                observers.update(any<Observer>())
                supervisors.update(any<Supervisor>())
            }
        }
    }
}
