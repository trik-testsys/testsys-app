package tech.testsys.domain.builder.api

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import tech.testsys.domain.model.EntityVersion
import tech.testsys.domain.model.LazyEntity
import tech.testsys.domain.model.LazyEntityList
import tech.testsys.domain.model.group.Class
import tech.testsys.domain.model.group.ClassData
import tech.testsys.domain.model.group.ClassId
import tech.testsys.domain.model.group.Community
import tech.testsys.domain.model.group.CommunityData
import tech.testsys.domain.model.group.CommunityId
import tech.testsys.domain.model.group.Competition
import tech.testsys.domain.model.group.CompetitionData
import tech.testsys.domain.model.group.CompetitionId
import tech.testsys.domain.model.task.ContestId
import tech.testsys.domain.model.user.MultipleRoleUserId
import tech.testsys.domain.model.user.SingleRoleUserId
import java.time.Instant

class GroupApiTest {

    @Nested
    inner class ClassTests {

        private val origin = Class(
            id = ClassId(1),
            createdAt = Instant.now(),
            version = EntityVersion(0),
            data = ClassData(
                owner = LazyEntity(MultipleRoleUserId(10)),
                name = "Original Class",
                description = "Original description",
                students = LazyEntityList(listOf(MultipleRoleUserId(20), MultipleRoleUserId(30))),
                contests = LazyEntityList(listOf(ContestId(40)))
            )
        )

        @Test
        fun `withData should keep all unmodified fields`() {
            val copy = origin.withData { }

            Assertions.assertEquals(origin.id, copy.id)
            Assertions.assertEquals(origin.createdAt, copy.createdAt)
            Assertions.assertEquals(origin.data.owner.id, copy.data.owner.id)
            Assertions.assertEquals(origin.data.name, copy.data.name)
            Assertions.assertEquals(origin.data.description, copy.data.description)
            Assertions.assertEquals(origin.data.students.ids.size, copy.data.students.ids.size)
            Assertions.assertEquals(origin.data.contests.ids.size, copy.data.contests.ids.size)
        }

        @Test
        fun `withData should change modified fields`() {
            val copy = origin.withData { owner(99) }

            Assertions.assertEquals(99L, copy.data.owner.id.value)
        }
    }

    @Nested
    inner class CommunityTests {

        private val origin = Community(
            id = CommunityId(1),
            createdAt = Instant.now(),
            version = EntityVersion(0),
            data = CommunityData(
                owner = LazyEntity(MultipleRoleUserId(10)),
                name = "Original Community",
                description = "Original description",
            )
        )

        @Test
        fun `withData should keep all unmodified fields`() {
            val copy = origin.withData { }

            Assertions.assertEquals(origin.id, copy.id)
            Assertions.assertEquals(origin.createdAt, copy.createdAt)
            Assertions.assertEquals(origin.data.owner.id, copy.data.owner.id)
            Assertions.assertEquals(origin.data.name, copy.data.name)
            Assertions.assertEquals(origin.data.description, copy.data.description)
        }

        @Test
        fun `withData should change modified fields`() {
            val copy = origin.withData { owner(99) }

            Assertions.assertEquals(99L, copy.data.owner.id.value)
        }
    }

    @Nested
    inner class CompetitionTests {

        private val origin = Competition(
            id = CompetitionId(1),
            createdAt = Instant.now(),
            version = EntityVersion(0),
            data = CompetitionData(
                owner = LazyEntity(MultipleRoleUserId(10)),
                name = "Original Competition",
                description = "Original description",
                participants = LazyEntityList(listOf(SingleRoleUserId(20))),
                contests = LazyEntityList(listOf(ContestId(30)))
            )
        )

        @Test
        fun `withData should keep all unmodified fields`() {
            val copy = origin.withData { }

            Assertions.assertEquals(origin.id, copy.id)
            Assertions.assertEquals(origin.createdAt, copy.createdAt)
            Assertions.assertEquals(origin.data.owner.id, copy.data.owner.id)
            Assertions.assertEquals(origin.data.name, copy.data.name)
            Assertions.assertEquals(origin.data.description, copy.data.description)
            Assertions.assertEquals(origin.data.participants.ids.size, copy.data.participants.ids.size)
            Assertions.assertEquals(origin.data.contests.ids.size, copy.data.contests.ids.size)
        }

        @Test
        fun `withData should change modified fields`() {
            val copy = origin.withData { owner(99) }

            Assertions.assertEquals(99L, copy.data.owner.id.value)
        }
    }

}