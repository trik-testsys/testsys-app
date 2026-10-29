package tech.testsys.domain.contract

import tech.testsys.domain.model.user.PasswordHash

/**
 * Port for turning passwords into stored representations that cannot be reversed and for checking them.
 * Calls are synchronous, stateless and safe to repeat; technical exceptions of the adapter are propagated.
 *
 * @since 1.0.0-beta.1
 */
interface PasswordHasher {

    /**
     * Hashes [password] exactly as given, with a new random salt, so repeated calls return different hashes.
     *
     * @param password the password as entered by the user, without normalization.
     * @return the stored representation of [password].
     * @throws IllegalArgumentException if the adapter cannot hash [password]; callers validate passwords first.
     * @since 1.0.0-beta.1
     */
    fun hash(password: String): PasswordHash

    /**
     * Checks [password] against [passwordHash] exactly. A `null` [passwordHash] or a password the adapter cannot hash
     * takes about as long as a real check, so the time does not reveal whether a user exists.
     *
     * @param password the password as entered by the user, without normalization.
     * @param passwordHash the stored representation to check against, or `null` if there is no user to check.
     * @return `true` only if [passwordHash] is not `null` and was produced from [password].
     * @since 1.0.0-beta.1
     */
    fun matches(password: String, passwordHash: PasswordHash?): Boolean
}
