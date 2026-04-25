package tech.testsys.infra.database.jpa.entity.user.single

import jakarta.persistence.Embeddable
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import tech.testsys.infra.database.jpa.entity.CompositeJpaEntity
import tech.testsys.infra.database.jpa.entity.CompositeId

enum class UserSingleRoleJpaEnum {

    PARTICIPANT,
    OBSERVER,
    SUPERVISOR
}

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

@Entity
class SingleRoleToUserJpaEntity(
    id: SingleRoleToUserId,
) : CompositeJpaEntity<SingleRoleToUserId>(id)
