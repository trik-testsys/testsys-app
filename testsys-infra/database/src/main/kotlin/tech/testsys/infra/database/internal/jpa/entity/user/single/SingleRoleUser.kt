package tech.testsys.infra.database.internal.jpa.entity.user.single

import jakarta.persistence.Embeddable
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import tech.testsys.infra.database.codegen.api.jpa.CompositeKeyConstructor
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.CompositeId
import tech.testsys.infra.database.internal.jpa.entity.CompositeJpaEntity

/**
 * The single role of a [tech.testsys.domain.model.user.SingleRoleUser].
 *
 * @since %CURRENT_VERSION%
 */
@InternalDatabaseApi
enum class UserSingleRoleJpaEnum {

    PARTICIPANT,
    OBSERVER,
    SUPERVISOR,
}

/**
 * Composite key of [SingleRoleToUserJpaEntity].
 *
 * @property singleRole the role held.
 * @property userId id of the user.
 * @since %CURRENT_VERSION%
 */
@Embeddable
@InternalDatabaseApi
data class SingleRoleToUserId(
    @Enumerated(EnumType.STRING)
    val singleRole: UserSingleRoleJpaEnum,
    val userId: Long,
) : CompositeId

/**
 * Join row: the role a single-role user holds.
 *
 * @since %CURRENT_VERSION%
 */
@Entity
@CompositeKeyConstructor
@InternalDatabaseApi
class SingleRoleToUserJpaEntity(id: SingleRoleToUserId) : CompositeJpaEntity<SingleRoleToUserId>(id)
