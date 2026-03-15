package tech.testsys.domain.builder.user

import tech.testsys.domain.builder.Builder
import tech.testsys.domain.builder.DataCapable
import tech.testsys.domain.builder.util.lazify
import tech.testsys.domain.model.group.CompetitionId
import tech.testsys.domain.model.user.Observer
import tech.testsys.domain.model.user.ObserverData
import tech.testsys.domain.model.user.Participant
import tech.testsys.domain.model.user.ParticipantData
import tech.testsys.domain.model.user.SingleRoleUser
import tech.testsys.domain.model.user.Supervisor
import tech.testsys.domain.model.user.UserId
import java.time.Instant

abstract class SingleRoleUserBuilder<U: SingleRoleUser> : UserBuilder<U>() {

    override var id: Long? = null
    override var createdAt: Instant? = null

}

class ParticipantDataBuilder : Builder<ParticipantData> {

    var competition: CompetitionId? = null

    fun competition(competitionId: Long) {
        this.competition = CompetitionId(competitionId)
    }

    override fun build(): ParticipantData {
        val competition = requireNotNull(competition)

        return ParticipantData(
            competition = competition.lazify(),
        )
    }

}

class ParticipantBuilder : SingleRoleUserBuilder<Participant>(), DataCapable<ParticipantData, ParticipantDataBuilder> {

    override var data: ParticipantData? = null
    override fun dataBuilder() = ParticipantDataBuilder()

    override fun build(): Participant {
        val id = requireNotNull(id)
        val createdAt = requireNotNull(createdAt)
        val accessToken = requireNotNull(accessToken)
        val data = requireNotNull(data)

        return Participant(
            id = UserId(id),
            createdAt = createdAt,
            accessToken = accessToken,
            data = data,
        )
    }

}

fun buildParticipantData(builder: ParticipantDataBuilder.() -> Unit) =
    ParticipantDataBuilder().apply(builder).build()

fun buildParticipant(builder: ParticipantBuilder.() -> Unit) =
    ParticipantBuilder().apply(builder).build()

class ObserverDataBuilder : Builder<ObserverData> {

    var competitions = mutableListOf<CompetitionId>()

    fun competitions(competitions: Iterable<Long>) {
        this.competitions = competitions.map { CompetitionId(it) }.toMutableList()
    }

    override fun build() = ObserverData(
        competitions = competitions.lazify(),
    )

}

class ObserverBuilder : SingleRoleUserBuilder<Observer>(), DataCapable<ObserverData, ObserverDataBuilder> {

    override var data: ObserverData? = null
    override fun dataBuilder() = ObserverDataBuilder()

    override fun build(): Observer {
        val id = requireNotNull(id)
        val createdAt = requireNotNull(createdAt)
        val accessToken = requireNotNull(accessToken)
        val data = requireNotNull(data)

        return Observer(
            id = UserId(id),
            createdAt = createdAt,
            accessToken = accessToken,
            data = data,
        )
    }
}

fun buildObserverData(builder: ObserverDataBuilder.() -> Unit) = ObserverDataBuilder().apply(builder).build()

fun buildObserver(builder: ObserverBuilder.() -> Unit) = ObserverBuilder().apply(builder).build()

class SupervisorBuilder : SingleRoleUserBuilder<Supervisor>() {

    override fun build(): Supervisor {
        val id = requireNotNull(id)
        val createdAt = requireNotNull(createdAt)
        val accessToken = requireNotNull(accessToken)

        return Supervisor(
            id = UserId(id),
            createdAt = createdAt,
            accessToken = accessToken,
        )
    }

}

fun buildSupervisor(builder: SupervisorBuilder.() -> Unit) = SupervisorBuilder().apply(builder).build()
