package tech.testsys.domain.model.user

import tech.testsys.domain.model.DomainEntity
import tech.testsys.domain.model.DomainId
import java.time.Instant

/**
 * Identifier of a [RegistrationRequest].
 *
 * @since 1.0.0-beta.1
 */
@JvmInline
value class RegistrationRequestId(
    override val value: Long,
) : DomainId

/**
 * Data of a [RegistrationRequest].
 *
 * @property email the normalized e-mail address being confirmed; fixed on creation and ignored on update.
 * @property confirmationCode the confirmation code sent to [email].
 * @property expiresAt the moment from which [confirmationCode] is no longer accepted.
 * @property attemptsLeft the number of remaining attempts to enter [confirmationCode].
 * @since 1.0.0-beta.1
 */
data class RegistrationRequestData(
    val email: String,
    val confirmationCode: String,
    val expiresAt: Instant,
    val attemptsLeft: Int,
)

/**
 * A pending self-registration of a user awaiting confirmation of its e-mail address by a code.
 *
 * @property data the data of the request.
 * @since 1.0.0-beta.1
 */
class RegistrationRequest(
    id: RegistrationRequestId,
    createdAt: Instant,
    val data: RegistrationRequestData,
) : DomainEntity<RegistrationRequestId>(id, createdAt)
