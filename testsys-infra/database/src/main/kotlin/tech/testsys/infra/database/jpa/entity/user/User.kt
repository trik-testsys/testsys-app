package tech.testsys.infra.database.jpa.entity.user

import jakarta.persistence.Entity
import jakarta.persistence.MappedSuperclass
import tech.testsys.infra.database.jpa.entity.JpaEntity

/**
 * JPA entity representing a user domain entity.
 *
 * @see tech.testsys.domain.model.user.User
 * @since %CURRENT_VERSION%
 */
@Entity
class UserJpaEntity(
    val accessToken: String,
) : JpaEntity()

/**
 * Base JPA entity for user role domain entities.
 *
 * @see tech.testsys.domain.model.user.CompatibleUserRole
 * @since %CURRENT_VERSION%
 */
@MappedSuperclass
abstract class RoleEntity(
    val userId: Long
) : JpaEntity()
