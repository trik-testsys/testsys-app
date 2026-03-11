package tech.testsys.infra.database.adapter

import org.springframework.stereotype.Repository
import tech.testsys.domain.contract.EntityRepository
import tech.testsys.domain.model.LazyEntity
import tech.testsys.domain.model.LazyEntityList
import tech.testsys.domain.model.LazyNullableEntity
import tech.testsys.domain.model.group.CompetitionId
import tech.testsys.domain.model.user.Observer
import tech.testsys.domain.model.user.ObserverData
import tech.testsys.domain.model.user.Participant
import tech.testsys.domain.model.user.ParticipantData
import tech.testsys.domain.model.user.Supervisor
import tech.testsys.domain.model.user.UserId
import tech.testsys.infra.database.jpa.entity.UserJpaEntity
import tech.testsys.infra.database.jpa.spring.UserSpringRepository

@Repository
class ParticipantRepositoryAdapter(
    private val userRepo: UserSpringRepository,
    private val userEntityLoader: UserEntityLoader,
) : EntityRepository<ParticipantData, UserId, Participant> {

    override fun load(field: LazyEntity<UserId, Participant>): Participant =
        userEntityLoader.loadById(field.id.value) as Participant

    override fun load(field: LazyNullableEntity<UserId, Participant>): Participant? =
        userEntityLoader.loadByIdOrNull(field.id.value) as? Participant

    override fun load(list: LazyEntityList<UserId, Participant>, pageSize: Int, page: Int): List<Participant> =
        userEntityLoader.loadByIds(list.ids.map { it.value }, pageSize, page).map { it as Participant }

    override fun save(data: ParticipantData): Participant {
        val jpa = UserJpaEntity(
            userType = "PARTICIPANT",
            competitionId = data.competition.id.value,
        )
        val saved = userRepo.save(jpa)
        return Participant(
            id = UserId(saved.id),
            data = ParticipantData(
                competition = LazyEntity(CompetitionId(saved.competitionId!!))
            )
        )
    }

    override fun save(data: List<ParticipantData>): List<Participant> = data.map { save(it) }
}

@Repository
class ObserverRepositoryAdapter(
    private val userRepo: UserSpringRepository,
    private val userEntityLoader: UserEntityLoader,
) : EntityRepository<ObserverData, UserId, Observer> {

    override fun load(field: LazyEntity<UserId, Observer>): Observer =
        userEntityLoader.loadById(field.id.value) as Observer

    override fun load(field: LazyNullableEntity<UserId, Observer>): Observer? =
        userEntityLoader.loadByIdOrNull(field.id.value) as? Observer

    override fun load(list: LazyEntityList<UserId, Observer>, pageSize: Int, page: Int): List<Observer> =
        userEntityLoader.loadByIds(list.ids.map { it.value }, pageSize, page).map { it as Observer }

    override fun save(data: ObserverData): Observer {
        val jpa = UserJpaEntity(
            userType = "OBSERVER",
            observerCompetitionIds = data.competitions.ids.map { it.value }.toMutableList(),
        )
        val saved = userRepo.save(jpa)
        return Observer(
            id = UserId(saved.id),
            data = ObserverData(
                competitions = LazyEntityList(saved.observerCompetitionIds.map { CompetitionId(it) })
            )
        )
    }

    override fun save(data: List<ObserverData>): List<Observer> = data.map { save(it) }
}

@Repository
class SupervisorRepositoryAdapter(
    private val userRepo: UserSpringRepository,
    private val userEntityLoader: UserEntityLoader,
) : EntityRepository<Unit, UserId, Supervisor> {

    override fun load(field: LazyEntity<UserId, Supervisor>): Supervisor =
        userEntityLoader.loadById(field.id.value) as Supervisor

    override fun load(field: LazyNullableEntity<UserId, Supervisor>): Supervisor? =
        userEntityLoader.loadByIdOrNull(field.id.value) as? Supervisor

    override fun load(list: LazyEntityList<UserId, Supervisor>, pageSize: Int, page: Int): List<Supervisor> =
        userEntityLoader.loadByIds(list.ids.map { it.value }, pageSize, page).map { it as Supervisor }

    override fun save(data: Unit): Supervisor {
        val jpa = UserJpaEntity(userType = "SUPERVISOR")
        val saved = userRepo.save(jpa)
        return Supervisor(id = UserId(saved.id))
    }

    override fun save(data: List<Unit>): List<Supervisor> = data.map { save(it) }
}