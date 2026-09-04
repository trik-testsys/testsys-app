package tech.testsys.domain.builder.api

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import tech.testsys.domain.model.EntityVersion
import tech.testsys.domain.model.LazyEntity
import tech.testsys.domain.model.LazyEntityList
import tech.testsys.domain.model.group.CommunityId
import tech.testsys.domain.model.group.CompetitionId
import tech.testsys.domain.model.task.TaskId
import tech.testsys.domain.model.user.Administrator
import tech.testsys.domain.model.user.Developer
import tech.testsys.domain.model.user.DeveloperData
import tech.testsys.domain.model.user.MultipleRoleUser
import tech.testsys.domain.model.user.MultipleRoleUserData
import tech.testsys.domain.model.user.MultipleRoleUserId
import tech.testsys.domain.model.user.Observer
import tech.testsys.domain.model.user.ObserverData
import tech.testsys.domain.model.user.Participant
import tech.testsys.domain.model.user.ParticipantData
import tech.testsys.domain.model.user.SingleRoleUserId
import tech.testsys.domain.model.user.Supervisor
import tech.testsys.domain.model.user.SupervisorData
import java.time.Instant

class UserApiTest {


    @Nested
    inner class ParticipantTests {

        private val origin = Participant(
            id = SingleRoleUserId(1),
            createdAt = Instant.now(),
            version = EntityVersion(0),
            data = ParticipantData(
                competition = LazyEntity(CompetitionId(10)),
                accessToken = "participant-token",
                name = "Participant",
            )
        )

        @Test
        fun `withData should keep all unmodified fields`() {
            val copy = origin.withData { }

            Assertions.assertEquals(origin.id, copy.id)
            Assertions.assertEquals(origin.createdAt, copy.createdAt)
            Assertions.assertEquals(origin.data.competition.id, copy.data.competition.id)
            Assertions.assertEquals(origin.data.accessToken, copy.data.accessToken)
            Assertions.assertEquals(origin.data.name, copy.data.name)
        }

        @Test
        fun `withData should change modified fields`() {
            val copy = origin.withData { competition(99) }

            Assertions.assertEquals(99L, copy.data.competition.id.value)
        }
    }

    @Nested
    inner class ObserverTests {

        private val origin = Observer(
            id = SingleRoleUserId(1),
            createdAt = Instant.now(),
            version = EntityVersion(0),
            data = ObserverData(
                community = LazyEntity(CommunityId(7)),
                competitions = LazyEntityList(listOf(CompetitionId(10), CompetitionId(20))),
                accessToken = "observer-token",
                name = "Observer",
            )
        )

        @Test
        fun `withData should keep all unmodified fields`() {
            val copy = origin.withData { }

            Assertions.assertEquals(origin.id, copy.id)
            Assertions.assertEquals(origin.createdAt, copy.createdAt)
            Assertions.assertEquals(origin.data.community.id, copy.data.community.id)
            Assertions.assertEquals(origin.data.accessToken, copy.data.accessToken)
            Assertions.assertEquals(origin.data.name, copy.data.name)
            Assertions.assertEquals(origin.data.competitions.ids.size, copy.data.competitions.ids.size)
        }

        @Test
        fun `withData should change modified fields`() {
            val copy = origin.withData { accessToken = "new-token" }

            Assertions.assertEquals("new-token", copy.data.accessToken)
        }
    }

    @Nested
    inner class MultipleRoleUserTests {

        private val origin = MultipleRoleUser(
            id = MultipleRoleUserId(1),
            createdAt = Instant.now(),
            version = EntityVersion(0),
            data = MultipleRoleUserData(
                accessToken = "user-token",
                name = "Alice",
                email = "alice@example.com",
                roles = listOf(
                    Developer(
                        memberOf = LazyEntityList(listOf(CommunityId(10))),
                        data = DeveloperData(
                            tasks = LazyEntityList(listOf(TaskId(1))),
                            contests = LazyEntityList(emptyList()),
                        )
                    ),
                    Administrator(
                        memberOf = LazyEntityList(emptyList())
                    )
                )
            )
        )

        @Test
        fun `withData should keep all unmodified fields`() {
            val copy = origin.withData { }

            Assertions.assertEquals(origin.id, copy.id)
            Assertions.assertEquals(origin.createdAt, copy.createdAt)
            Assertions.assertEquals(origin.data.accessToken, copy.data.accessToken)
            Assertions.assertEquals(origin.data.name, copy.data.name)
            Assertions.assertEquals(origin.data.email, copy.data.email)
            Assertions.assertEquals(origin.data.roles.size, copy.data.roles.size)
        }

        @Test
        fun `withData should change modified fields`() {
            val copy = origin.withData { accessToken = "new-token" }

            Assertions.assertEquals("new-token", copy.data.accessToken)
        }
    }

    @Nested
    inner class SupervisorTests {

        private val origin = Supervisor(
            id = SingleRoleUserId(1),
            createdAt = Instant.now(),
            version = EntityVersion(0),
            data = SupervisorData(
                accessToken = "supervisor-token",
                name = "Supervisor",
            )
        )

        @Test
        fun `withData should keep all unmodified fields`() {
            val copy = origin.withData { }

            Assertions.assertEquals(origin.id, copy.id)
            Assertions.assertEquals(origin.createdAt, copy.createdAt)
            Assertions.assertEquals(origin.data.accessToken, copy.data.accessToken)
            Assertions.assertEquals(origin.data.name, copy.data.name)
        }

        @Test
        fun `withData should change modified fields`() {
            val copy = origin.withData { accessToken = "new-token" }

            Assertions.assertEquals("new-token", copy.data.accessToken)
        }
    }

}