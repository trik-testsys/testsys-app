package tech.testsys.domain.model.user

import tech.testsys.domain.model.DomainEntity
import tech.testsys.domain.model.DomainId
import tech.testsys.domain.model.LazyEntity
import java.time.Instant

/**
 * Identifier of an [EmailChangeRequest].
 *
 * @since %CURRENT_VERSION%
 */
@JvmInline
value class EmailChangeRequestId(
    override val value: Long,
) : DomainId

/**
 * Data of an [EmailChangeRequest].
 *
 * @property user the user changing the e-mail address; fixed on creation and ignored on update.
 * @property email the normalized new e-mail address being confirmed.
 * @property confirmationCode the confirmation code sent to [email].
 * @property expiresAt the moment from which [confirmationCode] is no longer accepted.
 * @property attemptsLeft the number of remaining attempts to enter [confirmationCode].
 * @since %CURRENT_VERSION%
 */
data class EmailChangeRequestData(
    val user: LazyEntity<MultipleRoleUserId, MultipleRoleUser>,
    val email: String,
    val confirmationCode: String,
    val expiresAt: Instant,
    val attemptsLeft: Int,
)

/**
 * A pending change of the e-mail address of a user awaiting confirmation of the new address by a code.
 *
 * @property data the data of the request.
 * @since %CURRENT_VERSION%
 */
class EmailChangeRequest(
    id: EmailChangeRequestId,
    createdAt: Instant,
    val data: EmailChangeRequestData,
) : DomainEntity<EmailChangeRequestId>(id, createdAt)
