package tech.testsys.domain.model.user

import tech.testsys.domain.model.DomainEntity
import tech.testsys.domain.model.DomainId
import java.time.Instant

/**
 * Marker for identifiers of [User]s.
 *
 * @since %CURRENT_VERSION%
 */
interface UserId : DomainId

/**
 * Sealed base of every user of the system. `data` is only a constructor parameter here; each concrete
 * user kind exposes its own typed `data` property.
 *
 * @param Id the identifier type of the concrete user kind.
 * @since %CURRENT_VERSION%
 */
sealed class User<Id : UserId>(
    id: Id,
    createdAt: Instant,
    data: UserData,
) : DomainEntity<Id>(id, createdAt)

/**
 * Algorithm used to produce the stored access-code representation of a [User] and the stored invite-code representation.
 *
 * @since %CURRENT_VERSION%
 */
sealed interface HashAlgorithm {

    /**
     * Temporary algorithm that leaves the original code unchanged and provides no cryptographic protection.
     * Declarations marked with RawAccessTokenDependency or RawInviteCodeDependency must be checked and corrected
     * before enabling another algorithm.
     *
     * @since %CURRENT_VERSION%
     */
    object Identity : HashAlgorithm
}

/**
 * Stored access-code representation and the algorithm that produced it.
 *
 * @property value the stored representation.
 * @property algorithm the algorithm used to produce [value].
 * @since %CURRENT_VERSION%
 */
data class AccessTokenHash(
    val value: String,
    val algorithm: HashAlgorithm,
) {
    companion object {
        /**
         * Hashes [rawAccessToken] with [algorithm] without normalizing or validating the input.
         *
         * @since %CURRENT_VERSION%
         */
        fun hashAccessToken(rawAccessToken: String, algorithm: HashAlgorithm): AccessTokenHash {
            val value = when (algorithm) {
                HashAlgorithm.Identity -> rawAccessToken
            }
            return AccessTokenHash(value = value, algorithm = algorithm)
        }
    }
}

/**
 * Data common to every kind of [User].
 *
 * @property accessTokenHash the stored access-code representation together with its hashing algorithm.
 * @property name the name of the user.
 * @since %CURRENT_VERSION%
 */
interface UserData {
    val accessTokenHash: AccessTokenHash
    val name: String
}
