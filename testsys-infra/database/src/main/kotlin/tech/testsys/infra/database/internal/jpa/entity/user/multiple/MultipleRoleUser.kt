package tech.testsys.infra.database.internal.jpa.entity.user.multiple

import jakarta.persistence.Embeddable
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import tech.testsys.infra.codegen.jpa.CompositeKeyConstructor
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.CompositeId
import tech.testsys.infra.database.internal.jpa.entity.CompositeJpaEntity

/**
 * Roles a multiple-role user may take within a community.
 *
 * @see tech.testsys.domain.model.user.MultipleRoleUser
 * @since %CURRENT_VERSION%
 */
@InternalDatabaseApi
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
@InternalDatabaseApi
data class MultipleRoleToUserId(
    @Enumerated(EnumType.STRING)
    val multipleRole: UserMultipleRoleJpaEnum,
    val userId: Long,
    val communityId: Long,
) : CompositeId

/**
 * JPA entity representing a (role, user, community) membership for multiple-role users.
 *
 * @since %CURRENT_VERSION%
 */
@Entity
@CompositeKeyConstructor
@InternalDatabaseApi
class MultipleRoleToUserJpaEntity(id: MultipleRoleToUserId) : CompositeJpaEntity<MultipleRoleToUserId>(id)
