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
 * Data common to every kind of [User].
 *
 * @property accessToken the access code the user logs in with.
 * @property name the name of the user.
 * @since %CURRENT_VERSION%
 */
interface UserData {
    val accessToken: String
    val name: String
}
