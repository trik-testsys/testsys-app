package tech.testsys.infra.database.internal.jpa.entity.user

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.SequenceJpaEntity

/**
 * Top-level discriminator between users that may hold many roles within different
 * communities ([MULTIPLE_ROLE]) and users that hold exactly one fixed role ([SINGLE_ROLE]).
 *
 * @since %CURRENT_VERSION%
 */
@InternalDatabaseApi
enum class UserTypeJpaEnum {

    MULTIPLE_ROLE,
    SINGLE_ROLE,
}

/**
 * JPA entity representing a user domain entity.
 *
 * @see tech.testsys.domain.model.user.User
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
) : SequenceJpaEntity(id)
