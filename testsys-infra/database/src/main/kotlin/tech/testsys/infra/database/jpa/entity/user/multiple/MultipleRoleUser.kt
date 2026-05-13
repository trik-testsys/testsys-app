package tech.testsys.infra.database.jpa.entity.user.multiple

import jakarta.persistence.Embeddable
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import tech.testsys.infra.codegen.jpa.CompositeKeyConstructor
import tech.testsys.infra.database.jpa.entity.CompositeId
import tech.testsys.infra.database.jpa.entity.CompositeJpaEntity

/**
 * Roles a multiple-role user may take within a community.
 *
 * @see tech.testsys.domain.model.user.MultipleRoleUser
 * @since %CURRENT_VERSION%
 */
enum class UserMultipleRoleJpaEnum {

    DEVELOPER,
    STUDENT,
    ADMINISTRATOR,
    JUDGE,
    MANAGER,
}

/**
 * Composite primary key for [MultipleRoleToUserJpaEntity], pairing a (role, user, community)
 * triple so a user can hold different roles in different communities.
 *
 * @since %CURRENT_VERSION%
 */
@Embeddable
data class MultipleRoleToUserId(
    @Enumerated(EnumType.STRING)
    val multipleRole: UserMultipleRoleJpaEnum,
    val userId: Long,
    val communityId: Long,
) : CompositeId {

    companion object {

        private const val serialVersionUID: Long = 1L
    }
}

/**
 * JPA entity representing a (role, user, community) membership for multiple-role users.
 *
 * @since %CURRENT_VERSION%
 */
@Entity
@CompositeKeyConstructor
class MultipleRoleToUserJpaEntity(id: MultipleRoleToUserId) : CompositeJpaEntity<MultipleRoleToUserId>(id)
