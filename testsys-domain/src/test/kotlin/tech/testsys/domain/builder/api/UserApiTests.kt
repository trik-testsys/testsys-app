package tech.testsys.domain.builder.api

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import tech.testsys.domain.model.EntityVersion
import tech.testsys.domain.model.user.HashAlgorithm
import java.time.Instant

class UserApiTests {

    @Nested
    inner class ParticipantTests {

        private val origin = participant {
            id = 1
            createdAt = Instant.ofEpochSecond(1)
            version = EntityVersion(7)
            data = participantData {
                competition(10)
                accessToken("participant-token", algorithm = HashAlgorithm.Identity)
                name = "Participant"
            }
        }

        @Test
        fun `should keep all fields if withData changes nothing`() {
            val copy = origin.withData { }

            Assertions.assertEquals(origin.id, copy.id)
            Assertions.assertEquals(origin.createdAt, copy.createdAt)
            Assertions.assertEquals(origin.version, copy.version)
            Assertions.assertEquals(origin.data.competition.id, copy.data.competition.id)
            Assertions.assertEquals(origin.data.accessToken, copy.data.accessToken)
            Assertions.assertEquals(HashAlgorithm.Identity, copy.data.accessTokenHashAlgorithm)
            Assertions.assertEquals(origin.data.name, copy.data.name)
        }

        @Test
        fun `should change competition if withData sets competition`() {
            val copy = origin.withData { competition(99) }

            Assertions.assertEquals(99L, copy.data.competition.id.value)
        }
    }

    @Nested
    inner class ObserverTests {

        private val origin = observer {
            id = 1
            createdAt = Instant.ofEpochSecond(1)
            version = EntityVersion(7)
            data = observerData {
                community(7)
                competitions(listOf(10L, 20L))
                accessToken("observer-token", algorithm = HashAlgorithm.Identity)
                name = "Observer"
            }
        }

        @Test
        fun `should keep all fields if withData changes nothing`() {
            val copy = origin.withData { }

            Assertions.assertEquals(origin.id, copy.id)
            Assertions.assertEquals(origin.createdAt, copy.createdAt)
            Assertions.assertEquals(origin.version, copy.version)
            Assertions.assertEquals(origin.data.community.id, copy.data.community.id)
            Assertions.assertEquals(origin.data.accessToken, copy.data.accessToken)
            Assertions.assertEquals(HashAlgorithm.Identity, copy.data.accessTokenHashAlgorithm)
            Assertions.assertEquals(origin.data.name, copy.data.name)
            Assertions.assertEquals(origin.data.competitions.ids, copy.data.competitions.ids)
        }

        @Test
        fun `should change accessToken if withData sets accessToken`() {
            val copy = origin.withData { accessToken("new-token", algorithm = HashAlgorithm.Identity) }

            Assertions.assertEquals("new-token", copy.data.accessToken)
            Assertions.assertEquals(HashAlgorithm.Identity, copy.data.accessTokenHashAlgorithm)
        }
    }

    @Nested
    inner class MultipleRoleUserTests {

        private val origin = multipleRoleUser {
            id = 1
            createdAt = Instant.ofEpochSecond(1)
            version = EntityVersion(7)
            data = multipleRoleUserData {
                accessToken("user-token", algorithm = HashAlgorithm.Identity)
                name = "Alice"
                email = "alice@example.com"
                roles {
                    developer {
                        memberOf(listOf(10))
                        data = developerData {
                            tasks(listOf(1))
                        }
                    }
                    administrator {}
                }
            }
        }

        @Test
        fun `should keep all fields if withData changes nothing`() {
            val copy = origin.withData { }

            Assertions.assertEquals(origin.id, copy.id)
            Assertions.assertEquals(origin.createdAt, copy.createdAt)
            Assertions.assertEquals(origin.version, copy.version)
            Assertions.assertEquals(origin.data.accessToken, copy.data.accessToken)
            Assertions.assertEquals(HashAlgorithm.Identity, copy.data.accessTokenHashAlgorithm)
            Assertions.assertEquals(origin.data.name, copy.data.name)
            Assertions.assertEquals(origin.data.email, copy.data.email)
            Assertions.assertEquals(origin.data.roles, copy.data.roles)
        }

        @Test
        fun `should change accessToken if withData sets accessToken`() {
            val copy = origin.withData { accessToken("new-token", algorithm = HashAlgorithm.Identity) }

            Assertions.assertEquals("new-token", copy.data.accessToken)
            Assertions.assertEquals(HashAlgorithm.Identity, copy.data.accessTokenHashAlgorithm)
        }
    }

    @Nested
    inner class SupervisorTests {

        private val origin = supervisor {
            id = 1
            createdAt = Instant.ofEpochSecond(1)
            version = EntityVersion(7)
            data = supervisorData {
                accessToken("supervisor-token", algorithm = HashAlgorithm.Identity)
                name = "Supervisor"
            }
        }

        @Test
        fun `should keep all fields if withData changes nothing`() {
            val copy = origin.withData { }

            Assertions.assertEquals(origin.id, copy.id)
            Assertions.assertEquals(origin.createdAt, copy.createdAt)
            Assertions.assertEquals(origin.version, copy.version)
            Assertions.assertEquals(origin.data.accessToken, copy.data.accessToken)
            Assertions.assertEquals(HashAlgorithm.Identity, copy.data.accessTokenHashAlgorithm)
            Assertions.assertEquals(origin.data.name, copy.data.name)
        }

        @Test
        fun `should change accessToken if withData sets accessToken`() {
            val copy = origin.withData { accessToken("new-token", algorithm = HashAlgorithm.Identity) }

            Assertions.assertEquals("new-token", copy.data.accessToken)
            Assertions.assertEquals(HashAlgorithm.Identity, copy.data.accessTokenHashAlgorithm)
        }
    }
}
