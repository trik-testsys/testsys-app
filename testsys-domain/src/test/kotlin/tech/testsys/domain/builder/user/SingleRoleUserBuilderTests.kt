package tech.testsys.domain.builder.user

import tech.testsys.domain.builder.DomainEntityBuilderTests
import tech.testsys.domain.builder.api.observerData
import tech.testsys.domain.builder.api.participantData
import tech.testsys.domain.builder.api.supervisorData
import tech.testsys.domain.model.user.Observer
import tech.testsys.domain.model.user.ObserverData
import tech.testsys.domain.model.user.Participant
import tech.testsys.domain.model.user.ParticipantData
import tech.testsys.domain.model.user.Supervisor
import tech.testsys.domain.model.user.SupervisorData

class ParticipantBuilderTests : DomainEntityBuilderTests<Participant, ParticipantData, ParticipantDataBuilder>(
    ParticipantBuilder(),
    ParticipantDataBuilder()
) {
    override fun buildDataWithAllFields() = listOf(participantData {
        competition(1)
        accessToken = "token"
    })
}

class ObserverBuilderTests : DomainEntityBuilderTests<Observer, ObserverData, ObserverDataBuilder>(
    ObserverBuilder(),
    ObserverDataBuilder()
) {
    override fun buildDataWithAllFields() = listOf(
        observerData { accessToken = "token" },
        observerData {
            accessToken = "token"
            competitions(listOf(1L, 2L))
        },
    )
}

class SupervisorBuilderTests : DomainEntityBuilderTests<Supervisor, SupervisorData, SupervisorDataBuilder>(
    SupervisorBuilder(),
    SupervisorDataBuilder()
) {
    override fun buildDataWithAllFields() = listOf(supervisorData {
        accessToken = "token"
    })
}
