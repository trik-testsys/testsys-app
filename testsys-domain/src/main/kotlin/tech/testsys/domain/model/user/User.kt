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
 * Algorithm used to produce the stored access-code representation of a [User].
 *
 * @since %CURRENT_VERSION%
 */
sealed interface HashAlgorithm {

    /**
     * Temporary algorithm that leaves the original access code unchanged and provides no cryptographic protection.
     * Declarations marked with RawAccessTokenDependency must be checked and corrected before enabling another algorithm.
     *
     * @since %CURRENT_VERSION%
     */
    object Identity : HashAlgorithm
}

/**
 * Data common to every kind of [User].
 *
 * @property accessToken the stored access-code representation; with Identity it equals the original access code.
 * @property accessTokenHashAlgorithm the algorithm used to produce the stored access-code representation.
 * @property name the name of the user.
 * @since %CURRENT_VERSION%
 */
interface UserData {
    val accessToken: String
    val accessTokenHashAlgorithm: HashAlgorithm
    val name: String
}
