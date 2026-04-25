package tech.testsys.domain.model.user

import tech.testsys.domain.model.LazyEntity
import tech.testsys.domain.model.LazyEntityList
import tech.testsys.domain.model.group.Community
import tech.testsys.domain.model.group.CommunityId
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
    data: UserData,
) : User<SingleRoleUserId>(id, createdAt, data)

data class ParticipantData(
    override val accessToken: String,
    override val name: String,
    val competition: LazyEntity<CompetitionId, Competition>,
) : UserData

class Participant(
    id: SingleRoleUserId,
    createdAt: Instant,
    val data: ParticipantData,
) : SingleRoleUser(id, createdAt, data)

data class ObserverData(
    override val accessToken: String,
    override val name: String,
    val community: LazyEntity<CommunityId, Community>,
    val competitions: LazyEntityList<CompetitionId, Competition>,
) : UserData

class Observer(
    id: SingleRoleUserId,
    createdAt: Instant,
    val data: ObserverData,
) : SingleRoleUser(id, createdAt, data)

data class SupervisorData(
    override val accessToken: String,
    override val name: String
) : UserData

class Supervisor(
    id: SingleRoleUserId,
    createdAt: Instant,
    val data: SupervisorData,
) : SingleRoleUser(id, createdAt, data)
