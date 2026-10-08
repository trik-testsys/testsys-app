package tech.testsys.domain.builder.user

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import tech.testsys.domain.builder.DomainEntityBuilderTests
import tech.testsys.domain.builder.api.observerData
import tech.testsys.domain.builder.api.participantData
import tech.testsys.domain.builder.api.supervisorData
import tech.testsys.domain.model.user.HashAlgorithm
import tech.testsys.domain.model.user.Observer
import tech.testsys.domain.model.user.ObserverData
import tech.testsys.domain.model.user.Participant
import tech.testsys.domain.model.user.ParticipantData
import tech.testsys.domain.model.user.Supervisor
import tech.testsys.domain.model.user.SupervisorData

class ParticipantBuilderTests : DomainEntityBuilderTests<Participant, ParticipantData, ParticipantDataBuilder>(
    ParticipantBuilder(),
    ParticipantDataBuilder(),
) {
    override fun buildDataWithAllFields() = listOf(
        participantData {
            competition(1)
            accessToken("token", algorithm = HashAlgorithm.Identity)
            name = "Participant"
        },
    )
}

class ObserverBuilderTests : DomainEntityBuilderTests<Observer, ObserverData, ObserverDataBuilder>(
    ObserverBuilder(),
    ObserverDataBuilder(),
) {
    override fun buildDataWithAllFields() = listOf(
        observerData {
            community(7)
            accessToken("token", algorithm = HashAlgorithm.Identity)
            name = "Observer"
        },
        observerData {
            community(7)
            accessToken("token", algorithm = HashAlgorithm.Identity)
            name = "Observer"
            contests(listOf(1L, 2L))
        },
    )
}

class SupervisorBuilderTests : DomainEntityBuilderTests<Supervisor, SupervisorData, SupervisorDataBuilder>(
    SupervisorBuilder(),
    SupervisorDataBuilder(),
) {
    override fun buildDataWithAllFields() = listOf(
        supervisorData {
            accessToken("token", algorithm = HashAlgorithm.Identity)
            name = "Supervisor"
        },
    )
}

class ParticipantDataBuilderTests {

    @Test
    fun `should build data with the provided hash algorithm`() {
        val data = participantData {
            competition(1)
            accessToken("token", algorithm = HashAlgorithm.Identity)
            name = "User"
        }

        assertEquals("token", data.accessTokenHash.value)
        assertEquals(HashAlgorithm.Identity, data.accessTokenHash.algorithm)
    }

    @Test
    fun `should fail to build data if the access token is not set`() {
        assertThrows<IllegalArgumentException> {
            participantData {
                competition(1)
                name = "User"
            }
        }
    }
}

class ObserverDataBuilderTests {

    @Test
    fun `should build data with the provided hash algorithm`() {
        val data = observerData {
            community(1)
            accessToken("token", algorithm = HashAlgorithm.Identity)
            name = "User"
        }

        assertEquals("token", data.accessTokenHash.value)
        assertEquals(HashAlgorithm.Identity, data.accessTokenHash.algorithm)
    }

    @Test
    fun `should fail to build data if the access token is not set`() {
        assertThrows<IllegalArgumentException> {
            observerData {
                community(1)
                name = "User"
            }
        }
    }
}

class SupervisorDataBuilderTests {

    @Test
    fun `should build data with the provided hash algorithm`() {
        val data = supervisorData {
            accessToken("token", algorithm = HashAlgorithm.Identity)
            name = "User"
        }

        assertEquals("token", data.accessTokenHash.value)
        assertEquals(HashAlgorithm.Identity, data.accessTokenHash.algorithm)
    }

    @Test
    fun `should fail to build data if the access token is not set`() {
        assertThrows<IllegalArgumentException> {
            supervisorData {
                name = "User"
            }
        }
    }
}
