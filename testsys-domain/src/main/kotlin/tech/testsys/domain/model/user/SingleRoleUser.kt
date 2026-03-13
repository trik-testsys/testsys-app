package tech.testsys.domain.model.user


import tech.testsys.domain.model.LazyEntity
import tech.testsys.domain.model.LazyEntityList
import tech.testsys.domain.model.group.Competition
import tech.testsys.domain.model.group.CompetitionId
import java.time.Instant

sealed class SingleRoleUser(
    id: UserId,
    createdAt: Instant,
    accessToken: String,
) : User(id, createdAt, accessToken)

data class ParticipantData(
    val competition: LazyEntity<CompetitionId, Competition>
)

class Participant(
    id: UserId,
    createdAt: Instant,
    accessToken: String,
    val data: ParticipantData,
) : SingleRoleUser(id, createdAt, accessToken)

data class ObserverData(
    val competitions: LazyEntityList<CompetitionId, Competition>
)

class Observer(
    id: UserId,
    createdAt: Instant,
    accessToken: String,
    val data: ObserverData,
) : SingleRoleUser(id, createdAt, accessToken)

class Supervisor(
    id: UserId,
    createdAt: Instant,
    accessToken: String,
) : SingleRoleUser(id, createdAt, accessToken)
