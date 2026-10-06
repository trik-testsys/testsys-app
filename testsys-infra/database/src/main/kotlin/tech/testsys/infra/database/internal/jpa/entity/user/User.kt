package tech.testsys.infra.database.internal.jpa.entity.user

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import tech.testsys.domain.model.user.RawAccessTokenDependency
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.SnowflakeJpaEntity

/**
 * Whether a [UserJpaEntity] holds many roles across communities ([MULTIPLE_ROLE]) or one fixed role ([SINGLE_ROLE]).
 *
 * @since %CURRENT_VERSION%
 */
@InternalDatabaseApi
enum class UserTypeJpaEnum {

    MULTIPLE_ROLE,
    SINGLE_ROLE,
}

/**
 * Algorithm used to produce the stored access-code representation of a [UserJpaEntity].
 *
 * @since %CURRENT_VERSION%
 */
@InternalDatabaseApi
enum class HashAlgorithmJpaEnum {

    IDENTITY,
}

/**
 * JPA entity of [tech.testsys.domain.model.user.User].
 *
 * @property name the name of the user.
 * @property accessToken the stored access-code representation.
 * @property accessTokenHashAlgorithm the algorithm used to produce the stored access-code representation.
 * @property email the e-mail of the user, or `null` for single-role users.
 * @property type whether the user holds multiple roles or a single one.
 * @since %CURRENT_VERSION%
 */
@Entity
@InternalDatabaseApi
@RawAccessTokenDependency(
    reason = "The uk_ts_user_access_token constraint enforces original access-code uniqueness only with Identity.",
)
class UserJpaEntity(
    val name: String,
    val accessToken: String,
    @Enumerated(EnumType.STRING)
    val accessTokenHashAlgorithm: HashAlgorithmJpaEnum,
    val email: String?,
    @Enumerated(EnumType.STRING)
    val type: UserTypeJpaEnum,
    id: Long? = null,
) : SnowflakeJpaEntity(id)
