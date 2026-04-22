package tech.testsys.infra.database.jpa.entity.user.multiple

import jakarta.persistence.Embeddable
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import tech.testsys.infra.database.jpa.entity.CompositeJpaEntity
import tech.testsys.infra.database.jpa.entity.CompositeId
import tech.testsys.infra.database.jpa.entity.user.single.UserSingleRoleJpaEnum

enum class UserMultipleRoleJpaEnum {

    DEVELOPER,
    STUDENT,
    ADMINISTRATOR,
    JUDGE,
    MANAGER,
}

@Embeddable
data class MultipleRoleToUserId(
    @Enumerated(EnumType.STRING)
    val multipleRole: UserSingleRoleJpaEnum,
    val userId: Long,
    val communityId: Long,
) : CompositeId {

    companion object {

        private const val serialVersionUID: Long = 1L
    }
}

@Entity
class MultipleRoleToUserJpaEntity(
    id: MultipleRoleToUserId,
) : CompositeJpaEntity<MultipleRoleToUserId>(id)
