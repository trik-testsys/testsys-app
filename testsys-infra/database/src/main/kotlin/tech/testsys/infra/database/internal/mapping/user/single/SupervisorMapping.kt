package tech.testsys.infra.database.internal.mapping.user.single

import tech.testsys.domain.builder.api.supervisor
import tech.testsys.domain.builder.data
import tech.testsys.domain.model.user.Supervisor
import tech.testsys.domain.model.user.SupervisorData
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.user.UserJpaEntity
import tech.testsys.infra.database.internal.jpa.entity.user.UserTypeJpaEnum
import tech.testsys.infra.database.internal.jpa.entity.user.single.SupervisorDataJpaEntity
import tech.testsys.infra.database.internal.utils.populateFields

@InternalDatabaseApi
object SupervisorMapping {

    fun toDomain(userJpaEntity: UserJpaEntity, dataJpaEntity: SupervisorDataJpaEntity) = supervisor {
        populateFields(userJpaEntity)
        // dataJpaEntity is consumed only for presence; it has no extra fields.
        check(dataJpaEntity.userId == userJpaEntity.id) {
            "SupervisorData ${dataJpaEntity.id} bound to user ${dataJpaEntity.userId} != ${userJpaEntity.id}"
        }
        data {
            accessToken = userJpaEntity.accessToken
            name = userJpaEntity.name
        }
    }

    fun toUserJpaEntity(data: SupervisorData) = UserJpaEntity(
        name = data.name,
        accessToken = data.accessToken,
        email = null,
        type = UserTypeJpaEnum.SINGLE_ROLE,
    )

    fun toUserJpaEntity(entity: Supervisor, current: UserJpaEntity) = UserJpaEntity(
        name = entity.data.name,
        accessToken = entity.data.accessToken,
        email = current.email,
        type = UserTypeJpaEnum.SINGLE_ROLE,
        id = entity.id.value,
    ).also {
        it.createdAt = current.createdAt
        it.version = current.version
    }

    fun toDataJpaEntity(userId: Long) = SupervisorDataJpaEntity(userId = userId)
}
