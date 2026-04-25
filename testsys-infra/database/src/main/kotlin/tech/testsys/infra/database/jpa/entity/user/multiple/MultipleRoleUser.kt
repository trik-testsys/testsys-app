package tech.testsys.infra.database.jpa.entity.user.multiple

import jakarta.persistence.Embeddable
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import tech.testsys.infra.database.jpa.entity.CompositeJpaEntity
import tech.testsys.infra.database.jpa.entity.CompositeId

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
    val multipleRole: UserMultipleRoleJpaEnum,
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
) : CompositeJpaEntity<MultipleRoleToUserId>(id) {

    constructor(multipleRole: UserMultipleRoleJpaEnum, userId: Long, communityId: Long): this(MultipleRoleToUserId(multipleRole, userId, communityId))
}
