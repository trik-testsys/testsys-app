package tech.testsys.infra.database.jpa.entity.user

import jakarta.persistence.Entity
import jakarta.persistence.Enumerated
import jakarta.persistence.MappedSuperclass
import tech.testsys.infra.database.jpa.entity.DescribableJpaEntity
import tech.testsys.infra.database.jpa.entity.SequenceJpaEntity

enum class UserTypeJpaEnum {

    MULTIPLE_ROLE,
    SINGLE_ROLE
}
/**
 * JPA entity representing a user domain entity.
 *
 * @see tech.testsys.domain.model.user.User
 * @since %CURRENT_VERSION%
 */
@Entity
class UserJpaEntity(
    name: String,
    description: String,
    val accessToken: String,
    val email: String?,
    @Enumerated
    val type: UserTypeJpaEnum,
) : DescribableJpaEntity(name, description)

/**
 * Base JPA entity for user role domain entities.
 *
 * @see tech.testsys.domain.model.user.CompatibleUserRole
 * @since %CURRENT_VERSION%
 */
@MappedSuperclass
abstract class RoleEntity(
    val userId: Long
) : SequenceJpaEntity()
