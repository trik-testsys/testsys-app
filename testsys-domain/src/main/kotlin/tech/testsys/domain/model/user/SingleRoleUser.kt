package tech.testsys.domain.model.user


import tech.testsys.domain.model.LazyEntity
import tech.testsys.domain.model.LazyEntityList
import tech.testsys.domain.model.group.Competition
import tech.testsys.domain.model.group.CompetitionId
import java.time.Instant

sealed class SingleRoleUser(
    id: UserId,
    accessToken: String,
    createdAt: Instant,
) : User(id, accessToken, createdAt)

data class ParticipantData(
    val competition: LazyEntity<CompetitionId, Competition>
)

class Participant(
    id: UserId,
    accessToken: String,
    val data: ParticipantData,
    createdAt: Instant,
) : SingleRoleUser(id, accessToken, createdAt)

data class ObserverData(
    val competitions: LazyEntityList<CompetitionId, Competition>
)

class Observer(
    id: UserId,
    accessToken: String,
    val data: ObserverData,
    createdAt: Instant,
) : SingleRoleUser(id, accessToken, createdAt)

class Supervisor(
    id: UserId,
    accessToken: String,
    createdAt: Instant,
) : SingleRoleUser(id, accessToken, createdAt)
