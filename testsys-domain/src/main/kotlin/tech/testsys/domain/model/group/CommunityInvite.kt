package tech.testsys.domain.model.group

import tech.testsys.domain.model.DomainEntity
import tech.testsys.domain.model.DomainId
import java.time.Instant

/**
 * Identifier of a [CommunityInvite].
 *
 * @since 1.0.0-beta.1
 */
@JvmInline
value class CommunityInviteId(
    override val value: Long,
) : DomainId

/**
 * Data of a [CommunityInvite].
 *
 * @property codeHash the stored invite-code representation together with its hashing algorithm.
 * @property expiresAt the moment the invite stops being valid; the invite is valid strictly before it.
 * @since 1.0.0-beta.1
 */
data class CommunityInviteData(
    val codeHash: InviteCodeHash,
    val expiresAt: Instant,
)

/**
 * The invite code by which a user joins the community that references it, in the role of the variant.
 *
 * @property data the data of the invite.
 * @property kind the role granted by the invite.
 * @since 1.0.0-beta.1
 */
sealed class CommunityInvite(
    id: CommunityInviteId,
    createdAt: Instant,
    val data: CommunityInviteData,
) : DomainEntity<CommunityInviteId>(id, createdAt) {

    abstract val kind: Kind

    /**
     * Selector of a [CommunityInvite] variant by the role it grants.
     *
     * @since 1.0.0-beta.1
     */
    sealed interface Kind {

        /**
         * Selects [CommunityInvite.Manager].
         *
         * @since 1.0.0-beta.1
         */
        data object Manager : Kind

        /**
         * Selects [CommunityInvite.Developer].
         *
         * @since 1.0.0-beta.1
         */
        data object Developer : Kind
    }

    /**
     * Invite code granting the manager role in the community.
     *
     * @since 1.0.0-beta.1
     */
    class Manager(
        id: CommunityInviteId,
        createdAt: Instant,
        data: CommunityInviteData,
    ) : CommunityInvite(id, createdAt, data) {
        override val kind: Kind = Kind.Manager
    }

    /**
     * Invite code granting the developer role in the community.
     *
     * @since 1.0.0-beta.1
     */
    class Developer(
        id: CommunityInviteId,
        createdAt: Instant,
        data: CommunityInviteData,
    ) : CommunityInvite(id, createdAt, data) {
        override val kind: Kind = Kind.Developer
    }
}
