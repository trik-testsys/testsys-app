package tech.testsys.operation.user

import tech.testsys.domain.builder.api.multipleRoleUserData
import tech.testsys.domain.builder.api.registrationRequestData
import tech.testsys.domain.builder.api.studentData
import tech.testsys.domain.builder.api.withData
import tech.testsys.domain.contract.PasswordHasher
import tech.testsys.domain.contract.UserMailSender
import tech.testsys.domain.contract.persistence.repository.MultipleRoleUserRepository
import tech.testsys.domain.contract.persistence.repository.ObserverRepository
import tech.testsys.domain.contract.persistence.repository.ParticipantRepository
import tech.testsys.domain.contract.persistence.repository.RegistrationRequestRepository
import tech.testsys.domain.contract.persistence.repository.SupervisorRepository
import tech.testsys.domain.contract.persistence.repository.UserRepository
import tech.testsys.domain.model.user.MultipleRoleUser
import tech.testsys.domain.model.user.RegistrationRequestId
import tech.testsys.domain.model.user.User
import tech.testsys.operation.annotation.Feature
import tech.testsys.operation.annotation.InternalOperationsApi
import tech.testsys.operation.config.CommunityConfig
import tech.testsys.operation.config.EmailConfirmationConfig
import tech.testsys.operation.config.PublicCommunityOwner
import tech.testsys.operation.error.AuthenticateByPasswordError
import tech.testsys.operation.error.AuthenticateError
import tech.testsys.operation.error.ConfirmRegistrationError
import tech.testsys.operation.error.ConfirmationAttemptsExhaustedError
import tech.testsys.operation.error.ConfirmationCodeExpiredError
import tech.testsys.operation.error.EmailAlreadyBoundError
import tech.testsys.operation.error.InvalidAccessTokenError
import tech.testsys.operation.error.InvalidConfirmationCodeError
import tech.testsys.operation.error.InvalidCredentialsError
import tech.testsys.operation.error.InvalidEmailError
import tech.testsys.operation.error.InvalidPasswordError
import tech.testsys.operation.error.InvalidUserNameError
import tech.testsys.operation.error.OperationResult
import tech.testsys.operation.error.RegistrationRequestNotExistsError
import tech.testsys.operation.error.RequestRegistrationError
import tech.testsys.operation.error.asSuccess
import tech.testsys.operation.error.ensure
import tech.testsys.operation.error.operation
import tech.testsys.operation.util.confirmationCodeExpiresAt
import tech.testsys.operation.util.isConfirmationActive
import tech.testsys.operation.util.isValidEmail
import tech.testsys.operation.util.isValidPassword
import tech.testsys.operation.util.nextConfirmationCode
import tech.testsys.operation.util.normalizeEmail
import java.time.Clock
import java.time.temporal.ChronoUnit
import java.util.random.RandomGenerator

private const val MAX_USER_NAME_LENGTH = 512

/**
 * Operations available to any user of the system, whatever their roles, including registration of new users.
 *
 * @since 1.0.0-beta.1
 */
@OptIn(InternalOperationsApi::class)
class UserOperations(
    private val multipleRoleUserRepository: MultipleRoleUserRepository,
    private val participantRepository: ParticipantRepository,
    private val observerRepository: ObserverRepository,
    private val supervisorRepository: SupervisorRepository,
    private val userRepository: UserRepository,
    private val registrationRequestRepository: RegistrationRequestRepository,
    private val mailSender: UserMailSender,
    private val passwordHasher: PasswordHasher,
    private val communityConfig: CommunityConfig,
    private val emailConfirmationConfig: EmailConfirmationConfig,
    private val clock: Clock,
    private val randomGenerator: RandomGenerator,
) {

    /**
     * Returns the fixed-role user whose current access code equals [accessToken] exactly and records the current moment
     * as their last login, without changing the user data. Users without a fixed role have no access code.
     *
     * @since 1.0.0-beta.1
     */
    @Feature("testsys.user.authentication")
    fun authenticate(accessToken: String): OperationResult<User<*>, AuthenticateError> = operation<User<*>, AuthenticateError> {
        val user: User<*>? = participantRepository.findByAccessToken(accessToken)
            ?: observerRepository.findByAccessToken(accessToken)
            ?: supervisorRepository.findByAccessToken(accessToken)
        ensure(user != null, InvalidAccessTokenError)

        userRepository.recordLogin(user.id, clock.instant().truncatedTo(ChronoUnit.MICROS))
        return user.asSuccess()
    }

    /**
     * Returns the user bound to the trimmed lower-case [email] whose password equals [password] exactly and records the
     * current moment as their last login, without changing the user data. An unbound address takes a password check
     * too, and it, a wrong password and the technical public-community owner are refused with the same error.
     *
     * @since 1.0.0-beta.1
     */
    @Feature("testsys.user.authentication")
    fun authenticateByPassword(email: String, password: String): OperationResult<MultipleRoleUser, AuthenticateByPasswordError> =
        operation<MultipleRoleUser, AuthenticateByPasswordError> {
            val user = multipleRoleUserRepository.findByEmail(normalizeEmail(email))
            // The check runs for an unbound address as well, so the response time does not reveal bound addresses.
            val isPasswordMatched = passwordHasher.matches(password, user?.data?.passwordHash)
            ensure(user != null && isPasswordMatched, InvalidCredentialsError)
            ensure(user.data.email != PublicCommunityOwner.EMAIL, InvalidCredentialsError)

            userRepository.recordLogin(user.id, clock.instant().truncatedTo(ChronoUnit.MICROS))
            return user.asSuccess()
        }

    /**
     * Stores the only registration request of the trimmed lower-case [email] and mails its confirmation code; storage
     * and mail exceptions propagate. An active request is resent unchanged, while an expired or exhausted one gets a
     * new code under the same id.
     *
     * @since 1.0.0-beta.1
     */
    @Feature("testsys.user.registration")
    fun requestRegistration(email: String): OperationResult<RegistrationRequestId, RequestRegistrationError> =
        operation<RegistrationRequestId, RequestRegistrationError> {
            val normalizedEmail = normalizeEmail(email)
            ensure(isValidEmail(normalizedEmail), InvalidEmailError)
            ensure(multipleRoleUserRepository.findByEmail(normalizedEmail) == null, EmailAlreadyBoundError)
            val now = clock.instant()
            val existing = registrationRequestRepository.findByEmail(normalizedEmail)
            val request = when {
                existing == null -> registrationRequestRepository.save(
                    registrationRequestData {
                        this.email = normalizedEmail
                        confirmationCode = randomGenerator.nextConfirmationCode()
                        expiresAt = emailConfirmationConfig.confirmationCodeExpiresAt(now)
                        attemptsLeft = emailConfirmationConfig.maxConfirmationAttempts
                    },
                )
                isConfirmationActive(expiresAt = existing.data.expiresAt, attemptsLeft = existing.data.attemptsLeft, now = now) -> existing
                else -> registrationRequestRepository.update(
                    existing.withData {
                        confirmationCode = randomGenerator.nextConfirmationCode()
                        expiresAt = emailConfirmationConfig.confirmationCodeExpiresAt(now)
                        attemptsLeft = emailConfirmationConfig.maxConfirmationAttempts
                    },
                )
            }
            mailSender.sendRegistrationConfirmationCode(email = request.data.email, confirmationCode = request.data.confirmationCode)
            return request.id.asSuccess()
        }

    /**
     * Checks the trimmed [name] and the untrimmed [password], spends an attempt of [registrationRequestId] and compares
     * the trimmed [confirmationCode] with the sent one; on a match creates a student with [name] and [password] in the
     * public community and removes the request. Returns the created user; storage exceptions propagate.
     *
     * @since 1.0.0-beta.1
     */
    @Feature("testsys.user.registration")
    fun confirmRegistration(
        registrationRequestId: RegistrationRequestId,
        confirmationCode: String,
        name: String,
        password: String,
    ): OperationResult<MultipleRoleUser, ConfirmRegistrationError> = operation<MultipleRoleUser, ConfirmRegistrationError> {
        val normalizedName = name.trim()
        // The nickname and the password are checked first, so invalid ones do not spend an attempt.
        ensure(normalizedName.isNotEmpty() && normalizedName.codePointLength() <= MAX_USER_NAME_LENGTH, InvalidUserNameError)
        ensure(isValidPassword(password), InvalidPasswordError)
        val request = registrationRequestRepository.findById(registrationRequestId)
        ensure(request != null) { RegistrationRequestNotExistsError(registrationRequestId) }
        ensure(clock.instant().isBefore(request.data.expiresAt), ConfirmationCodeExpiredError)
        ensure(request.data.attemptsLeft > 0, ConfirmationAttemptsExhaustedError)
        val attemptsLeft = request.data.attemptsLeft - 1
        // The versioned update spends the attempt before the comparison, so concurrent confirmations with a stale
        // version fail without comparing their codes.
        val spentRequest = registrationRequestRepository.update(request.withData { this.attemptsLeft = attemptsLeft })
        ensure(confirmationCode.trim() == request.data.confirmationCode, InvalidConfirmationCodeError)
        ensure(multipleRoleUserRepository.findByEmail(request.data.email) == null, EmailAlreadyBoundError)
        val publicCommunityId = communityConfig.publicCommunityId
        val user = multipleRoleUserRepository.save(
            multipleRoleUserData {
                passwordHash = passwordHasher.hash(password)
                this.name = normalizedName
                email = request.data.email
                roles {
                    student {
                        memberOf = mutableListOf(publicCommunityId)
                        data = studentData {}
                    }
                }
            },
        )
        registrationRequestRepository.remove(spentRequest)
        return user.asSuccess()
    }

    private fun String.codePointLength(): Int = codePointCount(0, length)
}
