package tech.testsys.infra.database.internal.jpa.entity.user.multiple

import jakarta.persistence.Embeddable
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import tech.testsys.infra.database.codegen.api.jpa.CompositeKeyConstructor
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.CompositeId
import tech.testsys.infra.database.internal.jpa.entity.CompositeJpaEntity

/**
 * Roles a [tech.testsys.domain.model.user.MultipleRoleUser] may hold within a community.
 *
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
 * Composite key of [MultipleRoleToUserJpaEntity].
 *
 * @property multipleRole the role held.
 * @property userId id of the user.
 * @property communityId id of the community the role is held in.
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
 * Join row: a multiple-role user holds a role in a community.
 *
 * @since %CURRENT_VERSION%
 */
@Entity
@CompositeKeyConstructor
@InternalDatabaseApi
class MultipleRoleToUserJpaEntity(id: MultipleRoleToUserId) : CompositeJpaEntity<MultipleRoleToUserId>(id)
