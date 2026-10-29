package tech.testsys.infra.database.api.session

import java.time.Instant

/**
 * Storage of the sign-ins to the main and service interfaces that survive a restart, keyed by the digest of the token
 * kept in the browser cookie; the token itself is never stored.
 *
 * @since 1.0.0-beta.1
 */
interface CabinetSignInStore {
    /**
     * Stores [signIn] under [tokenHash] as seen at [now].
     *
     * @since 1.0.0-beta.1
     */
    fun create(tokenHash: String, signIn: StoredCabinetSignIn, now: Instant)

    /**
     * Returns the sign-in of [tokenHash] last seen not before [idleSince], or `null` if there is no such active
     * sign-in; an idle sign-in stays until [deleteIdle].
     *
     * @since 1.0.0-beta.1
     */
    fun find(tokenHash: String, idleSince: Instant): StoredCabinetSignIn?

    /**
     * Records [now] as the last request of the sign-in of [tokenHash]; an absent sign-in is ignored.
     *
     * @since 1.0.0-beta.1
     */
    fun touch(tokenHash: String, now: Instant)

    /**
     * Deletes the sign-in of [tokenHash] and returns whether it existed.
     *
     * @since 1.0.0-beta.1
     */
    fun delete(tokenHash: String): Boolean

    /**
     * Deletes the sign-ins in which the user [userId] signed in themselves, except the one of [exceptTokenHash];
     * sign-ins of a supervisor as the user stay.
     *
     * @since 1.0.0-beta.1
     */
    fun deleteOwn(userId: CabinetSignInUserId, exceptTokenHash: String? = null)

    /**
     * Deletes every sign-in of the user [userId], including those in which the user as a supervisor signed in as
     * another user, and returns the deleted sign-ins.
     *
     * @since 1.0.0-beta.1
     */
    fun deleteAllOf(userId: CabinetSignInUserId): List<StoredCabinetSignIn>

    /**
     * Deletes the sign-ins last seen before [before] and returns them.
     *
     * @since 1.0.0-beta.1
     */
    fun deleteIdle(before: Instant): List<StoredCabinetSignIn>
}

/**
 * Identifier of a user signed in through a [StoredCabinetSignIn].
 *
 * @property value the raw id of the user.
 * @since 1.0.0-beta.1
 */
@JvmInline
value class CabinetSignInUserId(val value: Long)

/**
 * Kind of a user signed in through a [StoredCabinetSignIn.User].
 *
 * @since 1.0.0-beta.1
 */
enum class CabinetSignInUserKind {
    MULTIPLE_ROLE,
    PARTICIPANT,
    OBSERVER,
    SUPERVISOR,
}

/**
 * Sign-in stored by [CabinetSignInStore]: the [Service] interface or a [User].
 *
 * @since 1.0.0-beta.1
 */
sealed interface StoredCabinetSignIn {
    /**
     * Sign-in to the service interface, which has no user and cannot be impersonated.
     *
     * @since 1.0.0-beta.1
     */
    data object Service : StoredCabinetSignIn

    /**
     * Sign-in of a user to the main interface.
     *
     * @property userId the id of the signed-in user.
     * @property kind the kind of the signed-in user.
     * @property impersonatedBy the id of the supervisor signed in as the user, or `null` if the user signed in themselves.
     * @since 1.0.0-beta.1
     */
    data class User(
        val userId: CabinetSignInUserId,
        val kind: CabinetSignInUserKind,
        val impersonatedBy: CabinetSignInUserId? = null,
    ) : StoredCabinetSignIn
}
