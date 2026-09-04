package tech.testsys.infra.database.internal.jpa.entity.user

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
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
 * JPA entity of [tech.testsys.domain.model.user.User].
 *
 * @property name the name of the user.
 * @property accessToken the token the user authenticates with.
 * @property email the e-mail of the user, or `null` for single-role users.
 * @property type whether the user holds multiple roles or a single one.
 * @since %CURRENT_VERSION%
 */
@Entity
@InternalDatabaseApi
class UserJpaEntity(
    val name: String,
    val accessToken: String,
    val email: String?,
    @Enumerated(EnumType.STRING)
    val type: UserTypeJpaEnum,
    id: Long? = null,
) : SnowflakeJpaEntity(id)
