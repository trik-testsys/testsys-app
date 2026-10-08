package tech.testsys.operation.user

import tech.testsys.domain.builder.api.managerData
import tech.testsys.domain.builder.api.multipleRoleUserData
import tech.testsys.domain.builder.api.registrationRequestData
import tech.testsys.domain.builder.api.studentData
import tech.testsys.domain.builder.api.withData
import tech.testsys.domain.contract.UserMailSender
import tech.testsys.domain.contract.persistence.repository.MultipleRoleUserRepository
import tech.testsys.domain.contract.persistence.repository.ObserverRepository
import tech.testsys.domain.contract.persistence.repository.ParticipantRepository
import tech.testsys.domain.contract.persistence.repository.RegistrationRequestRepository
import tech.testsys.domain.contract.persistence.repository.SupervisorRepository
import tech.testsys.domain.model.user.HashAlgorithm
import tech.testsys.domain.model.user.MultipleRoleUser
import tech.testsys.domain.model.user.RegistrationRequestId
import tech.testsys.domain.model.user.RegistrationRole
import tech.testsys.domain.model.user.User
import tech.testsys.operation.annotation.Feature
import tech.testsys.operation.annotation.InternalOperationsApi
import tech.testsys.operation.config.CommunityConfig
import tech.testsys.operation.config.EmailConfirmationConfig
import tech.testsys.operation.error.AuthenticateError
import tech.testsys.operation.error.ConfirmRegistrationError
import tech.testsys.operation.error.ConfirmationAttemptsExhaustedError
import tech.testsys.operation.error.ConfirmationCodeExpiredError
import tech.testsys.operation.error.EmailAlreadyBoundError
import tech.testsys.operation.error.InvalidAccessTokenError
import tech.testsys.operation.error.InvalidConfirmationCodeError
import tech.testsys.operation.error.InvalidEmailError
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
import tech.testsys.operation.util.nextAccessToken
import tech.testsys.operation.util.nextConfirmationCode
import tech.testsys.operation.util.normalizeEmail
import java.time.Clock
import java.util.random.RandomGenerator

private const val MAX_USER_NAME_LENGTH = 512

/**
 * Operations available to any user of the system, whatever their roles, including registration of new users.
 *
 * @since %CURRENT_VERSION%
 */
@OptIn(InternalOperationsApi::class)
class UserOperations(
    private val multipleRoleUserRepository: MultipleRoleUserRepository,
    private val participantRepository: ParticipantRepository,
    private val observerRepository: ObserverRepository,
    private val supervisorRepository: SupervisorRepository,
    private val registrationRequestRepository: RegistrationRequestRepository,
    private val mailSender: UserMailSender,
    private val communityConfig: CommunityConfig,
    private val emailConfirmationConfig: EmailConfirmationConfig,
    private val clock: Clock,
    private val randomGenerator: RandomGenerator,
) {

    /**
     * Returns the user whose current access code equals [accessToken] exactly, without changing any user.
     *
     * @since %CURRENT_VERSION%
     */
    @Feature("testsys.user.authentication")
    fun authenticate(accessToken: String): OperationResult<User<*>, AuthenticateError> = operation<User<*>, AuthenticateError> {
        val user: User<*>? = multipleRoleUserRepository.findByAccessToken(accessToken)
            ?: participantRepository.findByAccessToken(accessToken)
            ?: observerRepository.findByAccessToken(accessToken)
            ?: supervisorRepository.findByAccessToken(accessToken)
        ensure(user != null, InvalidAccessTokenError)
        return user.asSuccess()
    }

    /**
     * Stores the only registration request of the trimmed lower-case [email] and mails its confirmation code; storage
     * and mail exceptions propagate. An active request is resent unchanged, while an expired or exhausted one gets a
     * new code under the same id.
     *
     * @since %CURRENT_VERSION%
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
     * Checks the trimmed [name], spends an attempt of [registrationRequestId] and compares the trimmed [confirmationCode]
     * with the sent one; on a match creates the user with [name] and [role] in the public community, removes the request
     * and mails the new access code. Returns the user with the original access code; storage and mail exceptions propagate.
     *
     * @since %CURRENT_VERSION%
     */
    @Feature("testsys.user.registration")
    fun confirmRegistration(
        registrationRequestId: RegistrationRequestId,
        confirmationCode: String,
        name: String,
        role: RegistrationRole,
    ): OperationResult<Pair<MultipleRoleUser, String>, ConfirmRegistrationError> =
        operation<Pair<MultipleRoleUser, String>, ConfirmRegistrationError> {
            val normalizedName = name.trim()
            // The nickname is checked first, so an invalid one does not spend an attempt.
            ensure(normalizedName.isNotEmpty() && normalizedName.codePointLength() <= MAX_USER_NAME_LENGTH, InvalidUserNameError)
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
            val rawAccessToken = randomGenerator.nextAccessToken()
            val publicCommunityId = communityConfig.publicCommunityId
            val user = multipleRoleUserRepository.save(
                multipleRoleUserData {
                    accessToken(rawAccessToken, algorithm = HashAlgorithm.Identity)
                    this.name = normalizedName
                    email = request.data.email
                    roles {
                        when (role) {
                            RegistrationRole.Student -> student {
                                memberOf = mutableListOf(publicCommunityId)
                                data = studentData {}
                            }
                            RegistrationRole.Manager -> manager {
                                memberOf = mutableListOf(publicCommunityId)
                                data = managerData {}
                            }
                        }
                    }
                },
            )
            registrationRequestRepository.remove(spentRequest)
            mailSender.sendAccessToken(email = user.data.email, name = user.data.name, accessToken = rawAccessToken)
            return (user to rawAccessToken).asSuccess()
        }

    private fun String.codePointLength(): Int = codePointCount(0, length)
}
