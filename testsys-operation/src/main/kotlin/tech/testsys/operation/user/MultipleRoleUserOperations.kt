package tech.testsys.operation.user

import tech.testsys.domain.builder.api.emailChangeRequestData
import tech.testsys.domain.builder.api.withData
import tech.testsys.domain.contract.UserMailSender
import tech.testsys.domain.contract.persistence.repository.CommunityRepository
import tech.testsys.domain.contract.persistence.repository.DeveloperCommunityInviteRepository
import tech.testsys.domain.contract.persistence.repository.EmailChangeRequestRepository
import tech.testsys.domain.contract.persistence.repository.ManagerCommunityInviteRepository
import tech.testsys.domain.contract.persistence.repository.MultipleRoleUserRepository
import tech.testsys.domain.model.group.CommunityInvite
import tech.testsys.domain.model.group.InviteCodeHash
import tech.testsys.domain.model.group.RawInviteCodeDependency
import tech.testsys.domain.model.user.CommunityRole
import tech.testsys.domain.model.user.HashAlgorithm
import tech.testsys.domain.model.user.MultipleRoleUser
import tech.testsys.operation.annotation.Feature
import tech.testsys.operation.annotation.InternalOperationsApi
import tech.testsys.operation.config.EmailConfirmationConfig
import tech.testsys.operation.error.CommunityInviteCodeExpiredError
import tech.testsys.operation.error.CommunityInviteCodeNotValidError
import tech.testsys.operation.error.ConfirmEmailChangeError
import tech.testsys.operation.error.ConfirmationAttemptsExhaustedError
import tech.testsys.operation.error.ConfirmationCodeExpiredError
import tech.testsys.operation.error.EmailAlreadyBoundError
import tech.testsys.operation.error.EmailChangeRequestNotExistsError
import tech.testsys.operation.error.EmailUnchangedError
import tech.testsys.operation.error.InvalidConfirmationCodeError
import tech.testsys.operation.error.InvalidEmailError
import tech.testsys.operation.error.JoinCommunityError
import tech.testsys.operation.error.OperationResult
import tech.testsys.operation.error.RequestEmailChangeError
import tech.testsys.operation.error.asSuccess
import tech.testsys.operation.error.ensure
import tech.testsys.operation.error.operation
import tech.testsys.operation.util.confirmationCodeExpiresAt
import tech.testsys.operation.util.isConfirmationActive
import tech.testsys.operation.util.isValidEmail
import tech.testsys.operation.util.nextConfirmationCode
import tech.testsys.operation.util.normalizeEmail
import tech.testsys.operation.util.normalizeInviteCode
import java.time.Clock
import java.util.random.RandomGenerator

/**
 * Operations available to any user without a fixed role, whatever their roles.
 *
 * @since %CURRENT_VERSION%
 */
@OptIn(InternalOperationsApi::class)
class MultipleRoleUserOperations(
    private val multipleRoleUserRepository: MultipleRoleUserRepository,
    private val communityRepository: CommunityRepository,
    private val managerInviteRepository: ManagerCommunityInviteRepository,
    private val developerInviteRepository: DeveloperCommunityInviteRepository,
    private val emailChangeRequestRepository: EmailChangeRequestRepository,
    private val mailSender: UserMailSender,
    private val emailConfirmationConfig: EmailConfirmationConfig,
    private val clock: Clock,
    private val randomGenerator: RandomGenerator,
) {

    /**
     * Makes [user] a member, in the invite role, of the community whose valid invite code matches [inviteCode]
     * case-insensitively, granting the role if needed. Membership is decided by the stored user, not by [user]:
     * a stored member in that role is returned unchanged.
     *
     * @since %CURRENT_VERSION%
     */
    @Feature("testsys.user.multi.joinCommunity")
    @RawInviteCodeDependency(reason = "Finds the invite by hashing the normalized input with the current deterministic algorithm.")
    fun joinCommunity(user: MultipleRoleUser, inviteCode: String): OperationResult<MultipleRoleUser, JoinCommunityError> =
        operation<MultipleRoleUser, JoinCommunityError> {
            val codeHash = InviteCodeHash.hashInviteCode(normalizeInviteCode(inviteCode), HashAlgorithm.Identity)
            val invite = managerInviteRepository.findByCode(codeHash) ?: developerInviteRepository.findByCode(codeHash)
            ensure(invite != null) { CommunityInviteCodeNotValidError(inviteCode) }
            ensure(clock.instant() < invite.data.expiresAt) { CommunityInviteCodeExpiredError(inviteCode) }
            val community = checkNotNull(communityRepository.findByInvite(invite.id)) {
                "No community references community invite id=${invite.id.value}"
            }
            // The port is idempotent and reads the stored memberships, so a stale [user] cannot skip joining.
            val role = when (invite.kind) {
                CommunityInvite.Kind.Manager -> CommunityRole.Manager
                CommunityInvite.Kind.Developer -> CommunityRole.Developer
            }
            return multipleRoleUserRepository.addCommunityMembership(userId = user.id, communityId = community.id, role = role)
                .asSuccess()
        }

    /**
     * Stores the only e-mail change request of [user] for the trimmed lower-case [email] and mails its confirmation code
     * to [email]; storage and mail exceptions propagate. An active request for the same address is resent unchanged,
     * while any other request of the user is overwritten with a new address, code, expiry and attempts.
     *
     * @since %CURRENT_VERSION%
     */
    @Feature("testsys.user.multi.changeMail")
    fun requestEmailChange(user: MultipleRoleUser, email: String): OperationResult<Unit, RequestEmailChangeError> =
        operation<Unit, RequestEmailChangeError> {
            val normalizedEmail = normalizeEmail(email)
            ensure(isValidEmail(normalizedEmail), InvalidEmailError)
            // The stored users decide both checks, so the e-mail address of a stale [user] does not affect them.
            val boundUser = multipleRoleUserRepository.findByEmail(normalizedEmail)
            ensure(boundUser?.id != user.id, EmailUnchangedError)
            ensure(boundUser == null, EmailAlreadyBoundError)
            val now = clock.instant()
            val existing = emailChangeRequestRepository.findByUser(user.id)
            val request = when {
                existing == null -> emailChangeRequestRepository.save(
                    emailChangeRequestData {
                        this.user = user.id
                        this.email = normalizedEmail
                        confirmationCode = randomGenerator.nextConfirmationCode()
                        expiresAt = emailConfirmationConfig.confirmationCodeExpiresAt(now)
                        attemptsLeft = emailConfirmationConfig.maxConfirmationAttempts
                    },
                )
                existing.data.email == normalizedEmail &&
                    isConfirmationActive(
                        expiresAt = existing.data.expiresAt,
                        attemptsLeft = existing.data.attemptsLeft,
                        now = now,
                    ) -> existing
                else -> emailChangeRequestRepository.update(
                    existing.withData {
                        this.email = normalizedEmail
                        confirmationCode = randomGenerator.nextConfirmationCode()
                        expiresAt = emailConfirmationConfig.confirmationCodeExpiresAt(now)
                        attemptsLeft = emailConfirmationConfig.maxConfirmationAttempts
                    },
                )
            }
            mailSender.sendEmailChangeConfirmationCode(email = request.data.email, confirmationCode = request.data.confirmationCode)
            return Unit.asSuccess()
        }

    /**
     * Spends an attempt of the e-mail change request of [user] and compares the trimmed [confirmationCode] with the sent
     * one; on a match replaces the e-mail address of the stored user with the new one, removes the request and notifies
     * the previous address. Returns the updated user; storage and mail exceptions propagate.
     *
     * @since %CURRENT_VERSION%
     */
    @Feature("testsys.user.multi.changeMail")
    fun confirmEmailChange(user: MultipleRoleUser, confirmationCode: String): OperationResult<MultipleRoleUser, ConfirmEmailChangeError> =
        operation<MultipleRoleUser, ConfirmEmailChangeError> {
            val request = emailChangeRequestRepository.findByUser(user.id)
            ensure(request != null, EmailChangeRequestNotExistsError)
            ensure(clock.instant().isBefore(request.data.expiresAt), ConfirmationCodeExpiredError)
            ensure(request.data.attemptsLeft > 0, ConfirmationAttemptsExhaustedError)
            val attemptsLeft = request.data.attemptsLeft - 1
            // The versioned update spends the attempt before the comparison, so concurrent confirmations with a stale
            // version fail without comparing their codes.
            val spentRequest = emailChangeRequestRepository.update(request.withData { this.attemptsLeft = attemptsLeft })
            ensure(confirmationCode.trim() == request.data.confirmationCode, InvalidConfirmationCodeError)
            val boundUser = multipleRoleUserRepository.findByEmail(request.data.email)
            ensure(boundUser == null || boundUser.id == user.id, EmailAlreadyBoundError)
            // The stored user is reread, so the change keeps the current access code, nickname and roles.
            val currentUser = checkNotNull(multipleRoleUserRepository.findById(user.id)) {
                "User ${user.id.value} of e-mail change request ${request.id.value} does not exist"
            }
            val previousEmail = currentUser.data.email
            val updatedUser = multipleRoleUserRepository.update(currentUser.withData { email = request.data.email })
            emailChangeRequestRepository.remove(spentRequest)
            mailSender.sendEmailChangedNotice(email = previousEmail, name = updatedUser.data.name)
            return updatedUser.asSuccess()
        }
}
