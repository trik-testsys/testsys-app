package tech.testsys.infra.database.jpa.entity.user.single

import jakarta.persistence.Embeddable
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import tech.testsys.infra.codegen.jpa.CompositeKeyConstructor
import tech.testsys.infra.database.jpa.entity.CompositeId
import tech.testsys.infra.database.jpa.entity.CompositeJpaEntity

/**
 * The single role a [tech.testsys.infra.database.jpa.entity.user.UserJpaEntity] of type
 * [tech.testsys.infra.database.jpa.entity.user.UserTypeJpaEnum.SINGLE_ROLE] holds.
 *
 * @see tech.testsys.domain.model.user.UserRole
 * @since %CURRENT_VERSION%
 */
enum class UserSingleRoleJpaEnum {

    PARTICIPANT,
    OBSERVER,
    SUPERVISOR,
}

/**
 * Composite primary key for [SingleRoleToUserJpaEntity].
 *
 * @since %CURRENT_VERSION%
 */
@Embeddable
data class SingleRoleToUserId(
    @Enumerated(EnumType.STRING)
    val singleRole: UserSingleRoleJpaEnum,
    val userId: Long,
) : CompositeId {

    companion object {

        private const val serialVersionUID: Long = 1L
    }
}

/**
 * JPA entity representing the (role, user) pairing for a single-role user.
 *
 * @since %CURRENT_VERSION%
 */
@Entity
@CompositeKeyConstructor
class SingleRoleToUserJpaEntity(id: SingleRoleToUserId) : CompositeJpaEntity<SingleRoleToUserId>(id)
