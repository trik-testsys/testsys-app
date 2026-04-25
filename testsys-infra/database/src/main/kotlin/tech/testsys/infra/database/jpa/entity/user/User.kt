package tech.testsys.infra.database.jpa.entity.user

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
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
    val name: String,
    val accessToken: String,
    val email: String?,
    @Enumerated(EnumType.STRING)
    val type: UserTypeJpaEnum,
) : SequenceJpaEntity()
