package tech.testsys.infra.database.internal.jpa.entity.user

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.SnowflakeJpaEntity
import java.time.Instant

/**
 * Whether a [UserJpaEntity] holds many roles across communities ([MULTIPLE_ROLE]) or one fixed role ([SINGLE_ROLE]).
 *
 * @since 1.0.0-beta.1
 */
@InternalDatabaseApi
enum class UserTypeJpaEnum {

    MULTIPLE_ROLE,
    SINGLE_ROLE,
}

/**
 * Algorithm used to produce a stored access-code or invite-code representation.
 *
 * @since 1.0.0-beta.1
 */
@InternalDatabaseApi
enum class HashAlgorithmJpaEnum {

    IDENTITY,
    HMAC_SHA256,
}

/**
 * JPA entity of [tech.testsys.domain.model.user.User].
 *
 * @property name the name of the user.
 * @property accessToken the HMAC-SHA-256 hex of the access code, or `null` for multiple-role users.
 * @property accessTokenHashAlgorithm the algorithm used to produce the stored access-code representation, or `null`
 * for multiple-role users.
 * @property passwordHash the stored password representation, or `null` for single-role users.
 * @property email the e-mail of the user, or `null` for single-role users.
 * @property type whether the user holds multiple roles or a single one.
 * @property lastLoginAt the moment of the last login, or `null` before the first one; only a bulk update writes it, so
 * saving or updating the entity never overwrites it.
 * @since 1.0.0-beta.1
 */
@Entity
@InternalDatabaseApi
class UserJpaEntity(
    val name: String,
    val accessToken: String?,
    @Enumerated(EnumType.STRING)
    val accessTokenHashAlgorithm: HashAlgorithmJpaEnum?,
    val passwordHash: String?,
    val email: String?,
    @Enumerated(EnumType.STRING)
    val type: UserTypeJpaEnum,
    id: Long? = null,
    @field:Column(insertable = false, updatable = false)
    val lastLoginAt: Instant? = null,
) : SnowflakeJpaEntity(id)
