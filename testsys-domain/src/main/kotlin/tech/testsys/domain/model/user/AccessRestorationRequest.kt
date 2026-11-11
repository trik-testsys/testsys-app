package tech.testsys.domain.model.user

import tech.testsys.domain.model.DomainEntity
import tech.testsys.domain.model.DomainId
import tech.testsys.domain.model.LazyEntity
import java.time.Instant

/**
 * Identifier of an [AccessRestorationRequest].
 *
 * @since 1.0.0-beta.1
 */
@JvmInline
value class AccessRestorationRequestId(
    override val value: Long,
) : DomainId

/**
 * Data of an [AccessRestorationRequest].
 *
 * @property user the user restoring access; fixed on creation and ignored on update.
 * @property confirmationCode the confirmation code sent to the e-mail address of [user].
 * @property expiresAt the moment from which [confirmationCode] is no longer accepted.
 * @property attemptsLeft the number of remaining attempts to enter [confirmationCode].
 * @since 1.0.0-beta.1
 */
data class AccessRestorationRequestData(
    val user: LazyEntity<MultipleRoleUserId, MultipleRoleUser>,
    val confirmationCode: String,
    val expiresAt: Instant,
    val attemptsLeft: Int,
)

/**
 * A pending restoration of access of a user awaiting confirmation by a code sent to the user's e-mail address.
 *
 * @property data the data of the request.
 * @since 1.0.0-beta.1
 */
class AccessRestorationRequest(
    id: AccessRestorationRequestId,
    createdAt: Instant,
    val data: AccessRestorationRequestData,
) : DomainEntity<AccessRestorationRequestId>(id, createdAt)
