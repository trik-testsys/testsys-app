package tech.testsys.domain.model.user


import tech.testsys.domain.model.LazyEntity
import tech.testsys.domain.model.LazyEntityList
import tech.testsys.domain.model.group.Competition
import tech.testsys.domain.model.group.CompetitionId

sealed class SingleRoleUser(
    id: UserId,
    accessToken: String,
) : User(id, accessToken)

data class ParticipantData(
    val competition: LazyEntity<CompetitionId, Competition>
)

class Participant(
    id: UserId,
    accessToken: String,
    val data: ParticipantData
) : SingleRoleUser(id,accessToken)

data class ObserverData(
    val competitions: LazyEntityList<CompetitionId, Competition>
)

class Observer(
    id: UserId,
    accessToken: String,
    val data: ObserverData
) : SingleRoleUser(id, accessToken)

class Supervisor(
    id: UserId,
    accessToken: String,
) : SingleRoleUser(id, accessToken)
