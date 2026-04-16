package tech.testsys.domain.model.user

import tech.testsys.domain.model.LazyEntity
import tech.testsys.domain.model.LazyEntityList
import tech.testsys.domain.model.group.Competition
import tech.testsys.domain.model.group.CompetitionId
import java.time.Instant

@JvmInline
value class SingleRoleUserId(
    override val value: Long,
) : UserId

sealed class SingleRoleUser(
    id: SingleRoleUserId,
    createdAt: Instant,
) : User<SingleRoleUserId>(id, createdAt)

data class ParticipantData(
    val accessToken: String,
    val competition: LazyEntity<CompetitionId, Competition>
)

class Participant(
    id: SingleRoleUserId,
    createdAt: Instant,
    val data: ParticipantData,
) : SingleRoleUser(id, createdAt) {

    override val accessToken = data.accessToken
}

data class ObserverData(
    val accessToken: String,
    val competitions: LazyEntityList<CompetitionId, Competition>
)

class Observer(
    id: SingleRoleUserId,
    createdAt: Instant,
    val data: ObserverData,
) : SingleRoleUser(id, createdAt) {

    override val accessToken = data.accessToken
}

data class SupervisorData(
    val accessToken: String,
)

class Supervisor(
    id: SingleRoleUserId,
    createdAt: Instant,
    val data: SupervisorData,
) : SingleRoleUser(id, createdAt) {

    override val accessToken = data.accessToken
}
