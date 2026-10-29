package tech.testsys.domain.model.group

import tech.testsys.domain.model.DomainEntity
import tech.testsys.domain.model.DomainId
import java.time.Instant

/**
 * Identifier of a [ClassInvite].
 *
 * @since 1.0.0-beta.1
 */
@JvmInline
value class ClassInviteId(
    override val value: Long,
) : DomainId

/**
 * Data of a [ClassInvite].
 *
 * @property codeHash the stored invite-code representation together with its hashing algorithm.
 * @property expiresAt the moment the invite stops being valid; the invite is valid strictly before it.
 * @since 1.0.0-beta.1
 */
data class ClassInviteData(
    val codeHash: InviteCodeHash,
    val expiresAt: Instant,
)

/**
 * The invite code by which a student joins the class that references it.
 *
 * @property data the data of the invite.
 * @since 1.0.0-beta.1
 */
class ClassInvite(
    id: ClassInviteId,
    createdAt: Instant,
    val data: ClassInviteData,
) : DomainEntity<ClassInviteId>(id, createdAt)
