package tech.testsys.operation.user

import io.mockk.Called
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import tech.testsys.domain.builder.api.multipleRoleUser
import tech.testsys.domain.builder.api.registrationRequest
import tech.testsys.domain.contract.PasswordHasher
import tech.testsys.domain.contract.UserMailSender
import tech.testsys.domain.contract.persistence.repository.MultipleRoleUserRepository
import tech.testsys.domain.contract.persistence.repository.ObserverRepository
import tech.testsys.domain.contract.persistence.repository.ParticipantRepository
import tech.testsys.domain.contract.persistence.repository.RegistrationRequestRepository
import tech.testsys.domain.contract.persistence.repository.SupervisorRepository
import tech.testsys.domain.contract.persistence.repository.UserRepository
import tech.testsys.domain.model.group.CommunityId
import tech.testsys.domain.model.user.MultipleRoleUser
import tech.testsys.domain.model.user.MultipleRoleUserData
import tech.testsys.domain.model.user.Observer
import tech.testsys.domain.model.user.Participant
import tech.testsys.domain.model.user.PasswordHash
import tech.testsys.domain.model.user.RegistrationRequest
import tech.testsys.domain.model.user.RegistrationRequestData
import tech.testsys.domain.model.user.RegistrationRequestId
import tech.testsys.domain.model.user.Student
import tech.testsys.domain.model.user.Supervisor
import tech.testsys.operation.config.CommunityConfig
import tech.testsys.operation.config.EmailConfirmationConfig
import tech.testsys.operation.config.PublicCommunityOwner
import tech.testsys.operation.error.ConfirmationAttemptsExhaustedError
import tech.testsys.operation.error.ConfirmationCodeExpiredError
import tech.testsys.operation.error.EmailAlreadyBoundError
import tech.testsys.operation.error.InvalidAccessTokenError
import tech.testsys.operation.error.InvalidConfirmationCodeError
import tech.testsys.operation.error.InvalidCredentialsError
import tech.testsys.operation.error.InvalidEmailError
import tech.testsys.operation.error.InvalidPasswordError
import tech.testsys.operation.error.InvalidUserNameError
import tech.testsys.operation.error.RegistrationRequestNotExistsError
import tech.testsys.operation.error.getOrThrow
import tech.testsys.operation.util.assertRaises
import tech.testsys.operation.util.testMultipleRoleUser
import tech.testsys.operation.util.testObserver
import tech.testsys.operation.util.testParticipant
import tech.testsys.operation.util.testRegistrationRequest
import tech.testsys.operation.util.testSupervisor
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.random.RandomGenerator
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertSame

class UserOperationsTests {

    private val multipleRoleUsers = mockk<MultipleRoleUserRepository>()
    private val participants = mockk<ParticipantRepository>()
    private val observers = mockk<ObserverRepository>()
    private val supervisors = mockk<SupervisorRepository>()
    private val users = mockk<UserRepository>()
    private val registrationRequests = mockk<RegistrationRequestRepository>()
    private val mailSender = mockk<UserMailSender>()
    private val passwordHasher = mockk<PasswordHasher>()
    private val clock = mockk<Clock>()
    private val random = mockk<RandomGenerator>()
    private val communityConfig = object : CommunityConfig {
        override val publicCommunityId = CommunityId(77)
    }
    private val emailConfirmationConfig = object : EmailConfirmationConfig {
        override val confirmationCodeLifetime: Duration = Duration.ofMinutes(15)
        override val maxConfirmationAttempts = 3
    }
    private val operations = UserOperations(
        multipleRoleUserRepository = multipleRoleUsers,
        participantRepository = participants,
        observerRepository = observers,
        supervisorRepository = supervisors,
        userRepository = users,
        registrationRequestRepository = registrationRequests,
        mailSender = mailSender,
        passwordHasher = passwordHasher,
        communityConfig = communityConfig,
        emailConfirmationConfig = emailConfirmationConfig,
        clock = clock,
        randomGenerator = random,
    )
    private val now = Instant.parse("2026-01-01T00:00:00Z")

    @BeforeEach
    fun setUp() {
        every { participants.findByAccessToken(any()) } returns null
        every { observers.findByAccessToken(any()) } returns null
        every { supervisors.findByAccessToken(any()) } returns null
        every { users.recordLogin(any(), any()) } just runs
        every { multipleRoleUsers.findByEmail(any()) } returns null
        every { registrationRequests.findByEmail(any()) } returns null
        every { registrationRequests.save(any<RegistrationRequestData>()) } answers { savedRequest(firstArg()) }
        every { registrationRequests.update(any<RegistrationRequest>()) } answers { firstArg() }
        every { registrationRequests.remove(any<RegistrationRequest>()) } just runs
        every { multipleRoleUsers.save(any<MultipleRoleUserData>()) } answers { savedUser(firstArg()) }
        every { mailSender.sendRegistrationConfirmationCode(any(), any()) } just runs
        every { passwordHasher.hash(any()) } answers { hashOf(firstArg()) }
        every { passwordHasher.matches(any(), any()) } returns false
        every { passwordHasher.matches("correct horse", hashOf("correct horse")) } returns true
        every { clock.instant() } returns now
        every { random.nextInt(10) } returnsMany listOf(8, 7, 6, 5, 4, 3, 2, 1)
    }

    private fun hashOf(password: String) = PasswordHash("hash:$password")

    private fun savedRequest(data: RegistrationRequestData): RegistrationRequest = registrationRequest {
        id = 41
        createdAt = now
        this.data = data
    }

    private fun savedUser(data: MultipleRoleUserData): MultipleRoleUser = multipleRoleUser {
        id = 51
        createdAt = now
        this.data = data
    }

    @Nested
    inner class AuthenticateTests {

        @Nested
        inner class HappyPathTests {

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
        }

        @Nested
        inner class RefusalTests {

            @Test
            fun `should raise InvalidAccessTokenError if no user has the access code`() {
                assertRaises(InvalidAccessTokenError) {
                    operations.authenticate(accessToken = "unknown")
                }
            }

            @Test
            fun `should raise InvalidAccessTokenError if access code differs only by surrounding whitespace`() {
                every { participants.findByAccessToken("participant") } returns testParticipant()

                assertRaises(InvalidAccessTokenError) {
                    operations.authenticate(accessToken = " participant ")
                }
            }
        }

        @Nested
        inner class InvariantTests {

            @Test
            fun `should record the current moment as the last login of the found user`() {
                val user = testParticipant()
                every { participants.findByAccessToken("participant") } returns user

                operations.authenticate(accessToken = "participant")

                verify(exactly = 1) { users.recordLogin(user.id, now) }
            }

            @Test
            fun `should record the last login truncated to microseconds`() {
                val user = testSupervisor()
                every { supervisors.findByAccessToken("supervisor") } returns user
                every { clock.instant() } returns Instant.parse("2026-01-01T00:00:00.123456789Z")

                operations.authenticate(accessToken = "supervisor")

                verify(exactly = 1) { users.recordLogin(user.id, Instant.parse("2026-01-01T00:00:00.123456Z")) }
            }

            @Test
            fun `should not record a login if no user has the access code`() {
                operations.authenticate(accessToken = "unknown")

                verify(exactly = 0) { users.recordLogin(any(), any()) }
            }

            @Test
            fun `should not look for users without a fixed role by an access code`() {
                operations.authenticate(accessToken = "unknown")

                verify { multipleRoleUsers wasNot Called }
            }

            @Test
            fun `should not update any user when authenticating`() {
                every { observers.findByAccessToken("observer") } returns testObserver()

                operations.authenticate(accessToken = "observer")

                verify(exactly = 0) {
                    participants.update(any<Participant>())
                    observers.update(any<Observer>())
                    supervisors.update(any<Supervisor>())
                }
            }
        }
    }

    @Nested
    inner class AuthenticateByPasswordTests {

        private val user = testMultipleRoleUser { passwordHash = hashOf("correct horse") }

        @Nested
        inner class HappyPathTests {

            @Test
            fun `should return the user whose email and password match`() {
                every { multipleRoleUsers.findByEmail("user@example.com") } returns user

                val result = operations.authenticateByPassword(email = "user@example.com", password = "correct horse").getOrThrow()

                assertSame(user, result)
            }

            @Test
            fun `should find the user by the trimmed lower-case email`() {
                every { multipleRoleUsers.findByEmail("user@example.com") } returns user

                val result = operations.authenticateByPassword(email = "  User@Example.COM ", password = "correct horse").getOrThrow()

                assertSame(user, result)
            }
        }

        @Nested
        inner class RefusalTests {

            @Test
            fun `should raise InvalidCredentialsError if the email is bound to no user`() {
                assertRaises(InvalidCredentialsError) {
                    operations.authenticateByPassword(email = "unknown@example.com", password = "correct horse")
                }
            }

            @ParameterizedTest
            @ValueSource(strings = ["wrong horse", "Correct horse", " correct horse", "correct horse "])
            fun `should raise InvalidCredentialsError if the password differs exactly`(password: String) {
                every { multipleRoleUsers.findByEmail("user@example.com") } returns user

                assertRaises(InvalidCredentialsError) {
                    operations.authenticateByPassword(email = "user@example.com", password = password)
                }
            }

            @Test
            fun `should raise InvalidCredentialsError for the technical public community owner even with its password`() {
                val owner = testMultipleRoleUser {
                    email = PublicCommunityOwner.EMAIL
                    passwordHash = hashOf("correct horse")
                }
                every { multipleRoleUsers.findByEmail(PublicCommunityOwner.EMAIL) } returns owner

                assertRaises(InvalidCredentialsError) {
                    operations.authenticateByPassword(email = PublicCommunityOwner.EMAIL, password = "correct horse")
                }
            }
        }

        @Nested
        inner class InvariantTests {

            @Test
            fun `should check the password even if the email is bound to no user`() {
                operations.authenticateByPassword(email = "unknown@example.com", password = "correct horse")

                verify(exactly = 1) { passwordHasher.matches("correct horse", null) }
            }

            @Test
            fun `should record the current moment truncated to microseconds as the last login`() {
                every { multipleRoleUsers.findByEmail("user@example.com") } returns user
                every { clock.instant() } returns Instant.parse("2026-01-01T00:00:00.123456789Z")

                operations.authenticateByPassword(email = "user@example.com", password = "correct horse")

                verify(exactly = 1) { users.recordLogin(user.id, Instant.parse("2026-01-01T00:00:00.123456Z")) }
            }

            @Test
            fun `should not record a login if the password differs`() {
                every { multipleRoleUsers.findByEmail("user@example.com") } returns user

                operations.authenticateByPassword(email = "user@example.com", password = "wrong horse")

                verify(exactly = 0) { users.recordLogin(any(), any()) }
            }

            @Test
            fun `should not record a login of the technical public community owner`() {
                every { multipleRoleUsers.findByEmail(PublicCommunityOwner.EMAIL) } returns testMultipleRoleUser {
                    email = PublicCommunityOwner.EMAIL
                    passwordHash = hashOf("correct horse")
                }

                operations.authenticateByPassword(email = PublicCommunityOwner.EMAIL, password = "correct horse")

                verify(exactly = 0) { users.recordLogin(any(), any()) }
            }

            @Test
            fun `should not update the user when authenticating`() {
                every { multipleRoleUsers.findByEmail("user@example.com") } returns user

                operations.authenticateByPassword(email = "user@example.com", password = "correct horse")

                verify(exactly = 0) { multipleRoleUsers.update(any<MultipleRoleUser>()) }
            }
        }
    }

    @Nested
    inner class RequestRegistrationTests {

        @Nested
        inner class HappyPathTests {

            @Test
            fun `should save new request with generated code, expiry and attempts if email has no request`() {
                val data = slot<RegistrationRequestData>()
                every { registrationRequests.save(capture(data)) } answers { savedRequest(firstArg()) }

                operations.requestRegistration(email = "user@example.com")

                assertEquals("user@example.com", data.captured.email)
                assertEquals("87654321", data.captured.confirmationCode)
                assertEquals(Instant.parse("2026-01-01T00:15:00Z"), data.captured.expiresAt)
                assertEquals(3, data.captured.attemptsLeft)
            }

            @Test
            fun `should return id of the saved request if email has no request`() {
                val result = operations.requestRegistration(email = "user@example.com")

                assertEquals(RegistrationRequestId(41), result.getOrThrow())
            }

            @Test
            fun `should send confirmation code of the new request to the email if email has no request`() {
                operations.requestRegistration(email = "user@example.com")

                verify(exactly = 1) {
                    mailSender.sendRegistrationConfirmationCode(email = "user@example.com", confirmationCode = "87654321")
                }
            }

            @Test
            fun `should save trimmed lower-case email if email has surrounding whitespace and capital letters`() {
                val data = slot<RegistrationRequestData>()
                every { registrationRequests.save(capture(data)) } answers { savedRequest(firstArg()) }

                operations.requestRegistration(email = "  User@Example.COM ")

                assertEquals("user@example.com", data.captured.email)
            }

            @Test
            fun `should accept email of 255 characters`() {
                val email = "u".repeat(243) + "@example.com"

                val result = operations.requestRegistration(email = email)

                assertEquals(RegistrationRequestId(41), result.getOrThrow())
            }

            @Test
            fun `should accept email of 255 code points outside the basic plane`() {
                val email = "😀".repeat(243) + "@example.com"

                val result = operations.requestRegistration(email = email)

                assertEquals(RegistrationRequestId(41), result.getOrThrow())
            }

            @Test
            fun `should accept email of 255 characters if surrounding whitespace makes it longer`() {
                val email = "  " + "u".repeat(243) + "@example.com  "

                val result = operations.requestRegistration(email = email)

                assertEquals(RegistrationRequestId(41), result.getOrThrow())
            }

            @Test
            fun `should overwrite an expired request with a new code, expiry and attempts under the same id`() {
                val existing = testRegistrationRequest {
                    expiresAt = Instant.parse("2025-12-31T23:59:59Z")
                    attemptsLeft = 2
                }
                every { registrationRequests.findByEmail("user@example.com") } returns existing
                val updated = slot<RegistrationRequest>()
                every { registrationRequests.update(capture(updated)) } answers { firstArg() }

                operations.requestRegistration(email = "user@example.com")

                assertEquals(existing.id, updated.captured.id)
                assertEquals("87654321", updated.captured.data.confirmationCode)
                assertEquals(Instant.parse("2026-01-01T00:15:00Z"), updated.captured.data.expiresAt)
                assertEquals(3, updated.captured.data.attemptsLeft)
            }

            @Test
            fun `should overwrite a request expiring exactly now with a new code`() {
                every { registrationRequests.findByEmail("user@example.com") } returns testRegistrationRequest { expiresAt = now }
                val updated = slot<RegistrationRequest>()
                every { registrationRequests.update(capture(updated)) } answers { firstArg() }

                operations.requestRegistration(email = "user@example.com")

                assertEquals("87654321", updated.captured.data.confirmationCode)
            }

            @Test
            fun `should overwrite a request without attempts left with a new code`() {
                every { registrationRequests.findByEmail("user@example.com") } returns testRegistrationRequest { attemptsLeft = 0 }
                val updated = slot<RegistrationRequest>()
                every { registrationRequests.update(capture(updated)) } answers { firstArg() }

                operations.requestRegistration(email = "user@example.com")

                assertEquals("87654321", updated.captured.data.confirmationCode)
                assertEquals(3, updated.captured.data.attemptsLeft)
                verify(exactly = 1) {
                    mailSender.sendRegistrationConfirmationCode(email = "user@example.com", confirmationCode = "87654321")
                }
            }
        }

        @Nested
        inner class RefusalTests {

            @ParameterizedTest
            @ValueSource(strings = ["userexample.com", "user@@example.com", "a@b@example.com", "@example.com", "user@", "  "])
            fun `should raise InvalidEmailError and store and send nothing if email lacks exactly one at sign with non-empty parts`(
                email: String,
            ) {
                assertRaises(InvalidEmailError) {
                    operations.requestRegistration(email = email)
                }
                verify(exactly = 0) {
                    registrationRequests.save(any<RegistrationRequestData>())
                    registrationRequests.update(any<RegistrationRequest>())
                    mailSender.sendRegistrationConfirmationCode(any(), any())
                }
            }

            @Test
            fun `should raise InvalidEmailError if email is longer than 255 characters`() {
                val email = "u".repeat(244) + "@example.com"

                assertRaises(InvalidEmailError) {
                    operations.requestRegistration(email = email)
                }
            }

            @Test
            fun `should raise InvalidEmailError if email is longer than 255 code points outside the basic plane`() {
                val email = "😀".repeat(244) + "@example.com"

                assertRaises(InvalidEmailError) {
                    operations.requestRegistration(email = email)
                }
            }

            @Test
            fun `should raise InvalidEmailError if flag emojis make email longer than 255 code points`() {
                val email = "🇷🇺".repeat(122) + "@example.com"

                assertRaises(InvalidEmailError) {
                    operations.requestRegistration(email = email)
                }
            }

            @Test
            fun `should raise EmailAlreadyBoundError and store and send nothing if email is bound to a user`() {
                every { multipleRoleUsers.findByEmail("user@example.com") } returns testMultipleRoleUser {}

                assertRaises(EmailAlreadyBoundError) {
                    operations.requestRegistration(email = "user@example.com")
                }
                verify(exactly = 0) {
                    registrationRequests.save(any<RegistrationRequestData>())
                    registrationRequests.update(any<RegistrationRequest>())
                    mailSender.sendRegistrationConfirmationCode(any(), any())
                }
            }

            @Test
            fun `should raise EmailAlreadyBoundError if bound email differs only by case and surrounding whitespace`() {
                every { multipleRoleUsers.findByEmail("user@example.com") } returns testMultipleRoleUser {}

                assertRaises(EmailAlreadyBoundError) {
                    operations.requestRegistration(email = " User@Example.com")
                }
            }
        }

        @Nested
        inner class InvariantTests {

            @Test
            fun `should not save or update the request if it is active`() {
                every { registrationRequests.findByEmail("user@example.com") } returns testRegistrationRequest {
                    expiresAt = Instant.parse("2026-01-01T00:05:00Z")
                    attemptsLeft = 2
                }

                operations.requestRegistration(email = "user@example.com")

                verify(exactly = 0) {
                    registrationRequests.save(any<RegistrationRequestData>())
                    registrationRequests.update(any<RegistrationRequest>())
                }
            }

            @Test
            fun `should not save or update the request if it expires one nanosecond after now`() {
                every { registrationRequests.findByEmail("user@example.com") } returns
                    testRegistrationRequest { expiresAt = now.plusNanos(1) }

                operations.requestRegistration(email = "user@example.com")

                verify(exactly = 0) {
                    registrationRequests.save(any<RegistrationRequestData>())
                    registrationRequests.update(any<RegistrationRequest>())
                }
            }

            @Test
            fun `should resend the same code and return the same id if the request is active`() {
                every { registrationRequests.findByEmail("user@example.com") } returns testRegistrationRequest()

                val result = operations.requestRegistration(email = "user@example.com")

                assertEquals(RegistrationRequestId(31), result.getOrThrow())
                verify(exactly = 1) {
                    mailSender.sendRegistrationConfirmationCode(email = "user@example.com", confirmationCode = "12345678")
                }
            }
        }

        @Nested
        inner class ModuleRuleTests {

            @Test
            fun `should not send the confirmation code if saving the new request fails`() {
                every { registrationRequests.save(any<RegistrationRequestData>()) } throws IllegalStateException("database is down")

                assertFailsWith<IllegalStateException> {
                    operations.requestRegistration(email = "user@example.com")
                }
                verify(exactly = 0) { mailSender.sendRegistrationConfirmationCode(any(), any()) }
            }

            @Test
            fun `should not send the confirmation code if overwriting the expired request fails`() {
                every { registrationRequests.findByEmail("user@example.com") } returns testRegistrationRequest { expiresAt = now }
                every { registrationRequests.update(any<RegistrationRequest>()) } throws IllegalStateException("stale version")

                assertFailsWith<IllegalStateException> {
                    operations.requestRegistration(email = "user@example.com")
                }
                verify(exactly = 0) { mailSender.sendRegistrationConfirmationCode(any(), any()) }
            }

            @Test
            fun `should propagate the exception if sending the confirmation code fails`() {
                every { mailSender.sendRegistrationConfirmationCode(any(), any()) } throws IllegalStateException("smtp is down")

                assertFailsWith<IllegalStateException> {
                    operations.requestRegistration(email = "user@example.com")
                }
            }
        }
    }

    @Nested
    inner class ConfirmRegistrationTests {

        private fun confirm(confirmationCode: String = "12345678", name: String = "Nickname", password: String = "correct horse") =
            operations.confirmRegistration(
                registrationRequestId = RegistrationRequestId(31),
                confirmationCode = confirmationCode,
                name = name,
                password = password,
            )

        @Nested
        inner class HappyPathTests {

            @Test
            fun `should create student in the public community with nickname, email and hash of the password`() {
                every { registrationRequests.findById(RegistrationRequestId(31)) } returns testRegistrationRequest()
                val data = slot<MultipleRoleUserData>()
                every { multipleRoleUsers.save(capture(data)) } answers { savedUser(firstArg()) }

                confirm(name = "Nickname", password = "correct horse")

                assertEquals("Nickname", data.captured.name)
                assertEquals("user@example.com", data.captured.email)
                assertEquals(hashOf("correct horse"), data.captured.passwordHash)
                val role = assertIs<Student>(data.captured.roles.single())
                assertEquals(listOf(CommunityId(77)), role.memberOf.ids)
            }

            @Test
            fun `should hash the password without trimming it`() {
                every { registrationRequests.findById(RegistrationRequestId(31)) } returns testRegistrationRequest()
                val data = slot<MultipleRoleUserData>()
                every { multipleRoleUsers.save(capture(data)) } answers { savedUser(firstArg()) }

                confirm(password = "  correct horse  ")

                assertEquals(hashOf("  correct horse  "), data.captured.passwordHash)
            }

            @ParameterizedTest
            @CsvSource("a,8", "😀,8", "a,64", "п,36")
            fun `should accept a password of 8 to 64 code points taking at most 72 bytes in UTF-8`(symbol: String, count: Int) {
                every { registrationRequests.findById(RegistrationRequestId(31)) } returns testRegistrationRequest()

                val user = confirm(password = symbol.repeat(count)).getOrThrow()

                assertEquals(hashOf(symbol.repeat(count)), user.data.passwordHash)
            }

            @Test
            fun `should save trimmed nickname if nickname has surrounding whitespace`() {
                every { registrationRequests.findById(RegistrationRequestId(31)) } returns testRegistrationRequest()
                val data = slot<MultipleRoleUserData>()
                every { multipleRoleUsers.save(capture(data)) } answers { savedUser(firstArg()) }

                confirm(name = "  Nickname  ")

                assertEquals("Nickname", data.captured.name)
            }

            @Test
            fun `should return the created user if the code matches`() {
                every { registrationRequests.findById(RegistrationRequestId(31)) } returns testRegistrationRequest()

                val user = confirm().getOrThrow()

                assertEquals(51L, user.id.value)
            }

            @Test
            fun `should remove the request if the code matches`() {
                every { registrationRequests.findById(RegistrationRequestId(31)) } returns testRegistrationRequest()

                confirm()

                verify(exactly = 1) { registrationRequests.remove(match<RegistrationRequest> { it.id == RegistrationRequestId(31) }) }
            }

            @Test
            fun `should send no letter if the code matches`() {
                every { registrationRequests.findById(RegistrationRequestId(31)) } returns testRegistrationRequest()

                confirm()

                verify { mailSender wasNot Called }
            }

            @Test
            fun `should accept nickname of 512 characters`() {
                every { registrationRequests.findById(RegistrationRequestId(31)) } returns testRegistrationRequest()
                val name = "n".repeat(512)

                val user = confirm(name = name).getOrThrow()

                assertEquals(name, user.data.name)
            }

            @Test
            fun `should accept nickname of 512 code points outside the basic plane`() {
                every { registrationRequests.findById(RegistrationRequestId(31)) } returns testRegistrationRequest()
                val name = "😀".repeat(512)

                val user = confirm(name = name).getOrThrow()

                assertEquals(name, user.data.name)
            }

            @Test
            fun `should accept nickname of 512 characters if surrounding whitespace makes it longer`() {
                every { registrationRequests.findById(RegistrationRequestId(31)) } returns testRegistrationRequest()

                val user = confirm(name = "  " + "n".repeat(512) + "  ").getOrThrow()

                assertEquals("n".repeat(512), user.data.name)
            }

            @Test
            fun `should accept the code one moment before it expires`() {
                every { registrationRequests.findById(RegistrationRequestId(31)) } returns
                    testRegistrationRequest { expiresAt = now.plusNanos(1) }

                val user = confirm().getOrThrow()

                assertEquals(51L, user.id.value)
            }

            @Test
            fun `should accept the matching code on the last attempt`() {
                every { registrationRequests.findById(RegistrationRequestId(31)) } returns testRegistrationRequest { attemptsLeft = 1 }

                val user = confirm().getOrThrow()

                assertEquals(51L, user.id.value)
            }

            @Test
            fun `should store one attempt less if the code matches`() {
                every { registrationRequests.findById(RegistrationRequestId(31)) } returns testRegistrationRequest()
                val updated = slot<RegistrationRequest>()
                every { registrationRequests.update(capture(updated)) } answers { firstArg() }

                confirm()

                assertEquals(RegistrationRequestId(31), updated.captured.id)
                assertEquals(2, updated.captured.data.attemptsLeft)
            }

            @Test
            fun `should accept a code with surrounding whitespace`() {
                every { registrationRequests.findById(RegistrationRequestId(31)) } returns testRegistrationRequest()

                val user = confirm(confirmationCode = " \t12345678 \n").getOrThrow()

                assertEquals("user@example.com", user.data.email)
            }
        }

        @Nested
        inner class RefusalTests {

            @ParameterizedTest
            @ValueSource(strings = ["", "   "])
            fun `should raise InvalidUserNameError if nickname is blank`(name: String) {
                assertRaises(InvalidUserNameError) {
                    confirm(name = name)
                }
            }

            @Test
            fun `should raise InvalidUserNameError if nickname is longer than 512 characters`() {
                assertRaises(InvalidUserNameError) {
                    confirm(name = "n".repeat(513))
                }
            }

            @Test
            fun `should raise InvalidUserNameError if nickname is longer than 512 code points outside the basic plane`() {
                assertRaises(InvalidUserNameError) {
                    confirm(name = "😀".repeat(513))
                }
            }

            @Test
            fun `should raise InvalidUserNameError without looking up the request if nickname is blank and the request does not exist`() {
                every { registrationRequests.findById(RegistrationRequestId(31)) } returns null

                assertRaises(InvalidUserNameError) {
                    confirm(name = "   ")
                }
                verify(exactly = 0) { registrationRequests.findById(any()) }
            }

            @ParameterizedTest
            @CsvSource("a,7", "😀,7", "a,65")
            fun `should raise InvalidPasswordError if the password is shorter than 8 or longer than 64 code points`(
                symbol: String,
                count: Int,
            ) {
                assertRaises(InvalidPasswordError) {
                    confirm(password = symbol.repeat(count))
                }
            }

            @Test
            fun `should raise InvalidPasswordError if the password takes more than 72 bytes in UTF-8`() {
                assertRaises(InvalidPasswordError) {
                    confirm(password = "п".repeat(36) + "a")
                }
            }

            @Test
            fun `should raise InvalidUserNameError before checking the password if both are invalid`() {
                assertRaises(InvalidUserNameError) {
                    confirm(name = "   ", password = "short")
                }
            }

            @Test
            fun `should raise InvalidPasswordError without looking up the request if the password is invalid`() {
                assertRaises(InvalidPasswordError) {
                    confirm(password = "short")
                }
                verify(exactly = 0) { registrationRequests.findById(any()) }
            }

            @Test
            fun `should raise RegistrationRequestNotExistsError if the request does not exist`() {
                every { registrationRequests.findById(RegistrationRequestId(99)) } returns null

                assertRaises(RegistrationRequestNotExistsError(RegistrationRequestId(99))) {
                    operations.confirmRegistration(
                        registrationRequestId = RegistrationRequestId(99),
                        confirmationCode = "12345678",
                        name = "Nickname",
                        password = "correct horse",
                    )
                }
            }

            @Test
            fun `should raise ConfirmationCodeExpiredError if the code expires exactly now`() {
                every { registrationRequests.findById(RegistrationRequestId(31)) } returns testRegistrationRequest { expiresAt = now }

                assertRaises(ConfirmationCodeExpiredError) {
                    confirm()
                }
            }

            @Test
            fun `should raise ConfirmationAttemptsExhaustedError if no attempts are left`() {
                every { registrationRequests.findById(RegistrationRequestId(31)) } returns testRegistrationRequest { attemptsLeft = 0 }

                assertRaises(ConfirmationAttemptsExhaustedError) {
                    confirm()
                }
            }

            @Test
            fun `should raise InvalidConfirmationCodeError if the code differs`() {
                every { registrationRequests.findById(RegistrationRequestId(31)) } returns testRegistrationRequest()

                assertRaises(InvalidConfirmationCodeError) {
                    confirm(confirmationCode = "00000000")
                }
            }

            @Test
            fun `should raise InvalidConfirmationCodeError if the code of the last attempt differs`() {
                every { registrationRequests.findById(RegistrationRequestId(31)) } returns testRegistrationRequest { attemptsLeft = 1 }

                assertRaises(InvalidConfirmationCodeError) {
                    confirm(confirmationCode = "00000000")
                }
            }

            @Test
            fun `should raise InvalidConfirmationCodeError if the code has whitespace inside`() {
                every { registrationRequests.findById(RegistrationRequestId(31)) } returns testRegistrationRequest()

                assertRaises(InvalidConfirmationCodeError) {
                    confirm(confirmationCode = "1234 5678")
                }
            }

            @Test
            fun `should raise EmailAlreadyBoundError, spend the attempt and keep the request if the email was bound after the request`() {
                every { registrationRequests.findById(RegistrationRequestId(31)) } returns testRegistrationRequest()
                every { multipleRoleUsers.findByEmail("user@example.com") } returns testMultipleRoleUser {}
                val updated = slot<RegistrationRequest>()
                every { registrationRequests.update(capture(updated)) } answers { firstArg() }

                assertRaises(EmailAlreadyBoundError) {
                    confirm()
                }
                assertEquals(2, updated.captured.data.attemptsLeft)
                verify(exactly = 0) {
                    multipleRoleUsers.save(any<MultipleRoleUserData>())
                    registrationRequests.remove(any<RegistrationRequest>())
                }
            }
        }

        @Nested
        inner class InvariantTests {

            @Test
            fun `should not spend an attempt if the nickname is invalid`() {
                every { registrationRequests.findById(RegistrationRequestId(31)) } returns testRegistrationRequest()

                confirm(name = "   ")

                verify(exactly = 0) { registrationRequests.update(any<RegistrationRequest>()) }
            }

            @Test
            fun `should not spend an attempt if the password is invalid`() {
                every { registrationRequests.findById(RegistrationRequestId(31)) } returns testRegistrationRequest()

                confirm(password = "short")

                verify(exactly = 0) { registrationRequests.update(any<RegistrationRequest>()) }
            }

            @Test
            fun `should not spend an attempt if the code expired`() {
                every { registrationRequests.findById(RegistrationRequestId(31)) } returns testRegistrationRequest { expiresAt = now }

                confirm()

                verify(exactly = 0) { registrationRequests.update(any<RegistrationRequest>()) }
            }

            @Test
            fun `should not spend an attempt if no attempts are left`() {
                every { registrationRequests.findById(RegistrationRequestId(31)) } returns testRegistrationRequest { attemptsLeft = 0 }

                confirm()

                verify(exactly = 0) { registrationRequests.update(any<RegistrationRequest>()) }
            }

            @Test
            fun `should store one attempt less if the code differs`() {
                every { registrationRequests.findById(RegistrationRequestId(31)) } returns testRegistrationRequest()
                val updated = slot<RegistrationRequest>()
                every { registrationRequests.update(capture(updated)) } answers { firstArg() }

                confirm(confirmationCode = "00000000")

                assertEquals(2, updated.captured.data.attemptsLeft)
                assertEquals("12345678", updated.captured.data.confirmationCode)
            }

            @ParameterizedTest
            @ValueSource(strings = ["12345678", "00000000"])
            fun `should propagate the storage exception and create no user if spending the attempt fails`(confirmationCode: String) {
                every { registrationRequests.findById(RegistrationRequestId(31)) } returns testRegistrationRequest()
                every { registrationRequests.update(any<RegistrationRequest>()) } throws IllegalStateException("stale version")

                assertFailsWith<IllegalStateException> {
                    confirm(confirmationCode = confirmationCode)
                }
                verify(exactly = 0) { multipleRoleUsers.save(any<MultipleRoleUserData>()) }
            }

            @Test
            fun `should not create a user if the code differs`() {
                every { registrationRequests.findById(RegistrationRequestId(31)) } returns testRegistrationRequest()

                confirm(confirmationCode = "00000000")

                verify(exactly = 0) { multipleRoleUsers.save(any<MultipleRoleUserData>()) }
            }

            @Test
            fun `should keep the request if the code differs`() {
                every { registrationRequests.findById(RegistrationRequestId(31)) } returns testRegistrationRequest()

                confirm(confirmationCode = "00000000")

                verify(exactly = 0) { registrationRequests.remove(any<RegistrationRequest>()) }
            }
        }

        @Nested
        inner class ModuleRuleTests {

            @Test
            fun `should propagate the exception and keep the request if saving the user fails`() {
                every { registrationRequests.findById(RegistrationRequestId(31)) } returns testRegistrationRequest()
                every { multipleRoleUsers.save(any<MultipleRoleUserData>()) } throws IllegalStateException("database is down")

                assertFailsWith<IllegalStateException> {
                    confirm()
                }
                verify(exactly = 0) { registrationRequests.remove(any<RegistrationRequest>()) }
            }
        }
    }
}
