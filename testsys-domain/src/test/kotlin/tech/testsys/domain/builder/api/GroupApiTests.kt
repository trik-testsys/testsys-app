package tech.testsys.domain.builder.api

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import tech.testsys.domain.model.EntityVersion
import tech.testsys.domain.model.LazyEntity
import tech.testsys.domain.model.LazyEntityList
import tech.testsys.domain.model.group.ClassData
import tech.testsys.domain.model.group.ClassInviteData
import tech.testsys.domain.model.group.ClassInviteId
import tech.testsys.domain.model.group.CommunityData
import tech.testsys.domain.model.group.CommunityInvite
import tech.testsys.domain.model.group.CommunityInviteData
import tech.testsys.domain.model.group.CommunityInviteId
import tech.testsys.domain.model.group.CompetitionData
import tech.testsys.domain.model.group.InviteCodeHash
import tech.testsys.domain.model.task.ContestId
import tech.testsys.domain.model.user.HashAlgorithm
import tech.testsys.domain.model.user.MultipleRoleUserId
import tech.testsys.domain.model.user.SingleRoleUserId
import java.time.Instant

class GroupApiTests {

    @Nested
    inner class ClassTests {

        private val origin = `class` {
            id = 1
            createdAt = Instant.ofEpochSecond(1)
            version = EntityVersion(7)
            data = ClassData(
                owner = LazyEntity(MultipleRoleUserId(10)),
                name = "Original Class",
                description = "Original description",
                students = LazyEntityList(listOf(MultipleRoleUserId(20), MultipleRoleUserId(30))),
                contests = LazyEntityList(listOf(ContestId(40))),
                invite = LazyEntity(ClassInviteId(50)),
            )
        }

        @Test
        fun `should keep all fields if withData changes nothing`() {
            val copy = origin.withData { }

            Assertions.assertEquals(origin.id, copy.id)
            Assertions.assertEquals(origin.createdAt, copy.createdAt)
            Assertions.assertEquals(origin.version, copy.version)
            Assertions.assertEquals(origin.data.owner.id, copy.data.owner.id)
            Assertions.assertEquals(origin.data.name, copy.data.name)
            Assertions.assertEquals(origin.data.description, copy.data.description)
            Assertions.assertEquals(origin.data.students.ids, copy.data.students.ids)
            Assertions.assertEquals(origin.data.contests.ids, copy.data.contests.ids)
            Assertions.assertEquals(origin.data.invite.id, copy.data.invite.id)
        }

        @Test
        fun `should change owner if withData sets owner`() {
            val copy = origin.withData { owner(99) }

            Assertions.assertEquals(99L, copy.data.owner.id.value)
        }
    }

    @Nested
    inner class CommunityTests {

        private val origin = community {
            id = 1
            createdAt = Instant.ofEpochSecond(1)
            version = EntityVersion(7)
            data = CommunityData(
                owner = LazyEntity(MultipleRoleUserId(10)),
                name = "Original Community",
                description = "Original description",
                managerInvite = LazyEntity(CommunityInviteId(20)),
                developerInvite = LazyEntity(CommunityInviteId(30)),
            )
        }

        @Test
        fun `should keep all fields if withData changes nothing`() {
            val copy = origin.withData { }

            Assertions.assertEquals(origin.id, copy.id)
            Assertions.assertEquals(origin.createdAt, copy.createdAt)
            Assertions.assertEquals(origin.version, copy.version)
            Assertions.assertEquals(origin.data.owner.id, copy.data.owner.id)
            Assertions.assertEquals(origin.data.name, copy.data.name)
            Assertions.assertEquals(origin.data.description, copy.data.description)
            Assertions.assertEquals(origin.data.managerInvite.id, copy.data.managerInvite.id)
            Assertions.assertEquals(origin.data.developerInvite.id, copy.data.developerInvite.id)
        }

        @Test
        fun `should change owner if withData sets owner`() {
            val copy = origin.withData { owner(99) }

            Assertions.assertEquals(99L, copy.data.owner.id.value)
        }
    }

    @Nested
    inner class CompetitionTests {

        private val origin = competition {
            id = 1
            createdAt = Instant.ofEpochSecond(1)
            version = EntityVersion(7)
            data = CompetitionData(
                owner = LazyEntity(MultipleRoleUserId(10)),
                name = "Original Competition",
                description = "Original description",
                participants = LazyEntityList(listOf(SingleRoleUserId(20))),
                contests = LazyEntityList(listOf(ContestId(30))),
            )
        }

        @Test
        fun `should keep all fields if withData changes nothing`() {
            val copy = origin.withData { }

            Assertions.assertEquals(origin.id, copy.id)
            Assertions.assertEquals(origin.createdAt, copy.createdAt)
            Assertions.assertEquals(origin.version, copy.version)
            Assertions.assertEquals(origin.data.owner.id, copy.data.owner.id)
            Assertions.assertEquals(origin.data.name, copy.data.name)
            Assertions.assertEquals(origin.data.description, copy.data.description)
            Assertions.assertEquals(origin.data.participants.ids, copy.data.participants.ids)
            Assertions.assertEquals(origin.data.contests.ids, copy.data.contests.ids)
        }

        @Test
        fun `should change owner if withData sets owner`() {
            val copy = origin.withData { owner(99) }

            Assertions.assertEquals(99L, copy.data.owner.id.value)
        }
    }

    @Nested
    inner class ClassInviteTests {

        private val origin = classInvite {
            id = 1
            createdAt = Instant.ofEpochSecond(1)
            version = EntityVersion(7)
            data = ClassInviteData(
                codeHash = InviteCodeHash(value = "abcdefghjkmn", algorithm = HashAlgorithm.Identity),
                expiresAt = Instant.ofEpochSecond(100),
            )
        }

        @Test
        fun `should keep all fields if withData changes nothing`() {
            val copy = origin.withData { }

            Assertions.assertEquals(origin.id, copy.id)
            Assertions.assertEquals(origin.createdAt, copy.createdAt)
            Assertions.assertEquals(origin.version, copy.version)
            Assertions.assertEquals(origin.data.codeHash, copy.data.codeHash)
            Assertions.assertEquals(origin.data.expiresAt, copy.data.expiresAt)
        }

        @Test
        fun `should replace the code if withData sets a new raw code`() {
            val copy = origin.withData { code("pqrstuvwxyz2", HashAlgorithm.Identity) }

            Assertions.assertEquals(InviteCodeHash(value = "pqrstuvwxyz2", algorithm = HashAlgorithm.Identity), copy.data.codeHash)
        }

        @Test
        fun `should change the expiration moment if withData sets it`() {
            val copy = origin.withData { expiresAt = Instant.ofEpochSecond(200) }

            Assertions.assertEquals(Instant.ofEpochSecond(200), copy.data.expiresAt)
            Assertions.assertEquals(origin.data.codeHash, copy.data.codeHash)
        }
    }

    @Nested
    inner class CommunityInviteTests {

        private val data = CommunityInviteData(
            codeHash = InviteCodeHash(value = "abcdefghjkmn", algorithm = HashAlgorithm.Identity),
            expiresAt = Instant.ofEpochSecond(100),
        )
        private val developer = developerCommunityInvite {
            id = 1
            createdAt = Instant.ofEpochSecond(1)
            version = EntityVersion(7)
            data = this@CommunityInviteTests.data
        }
        private val manager = managerCommunityInvite {
            id = 2
            createdAt = Instant.ofEpochSecond(1)
            version = EntityVersion(7)
            data = this@CommunityInviteTests.data
        }

        @Test
        fun `should keep all fields and the developer variant if withData changes nothing`() {
            val copy: CommunityInvite.Developer = developer.withData { }

            Assertions.assertEquals(developer.id, copy.id)
            Assertions.assertEquals(developer.createdAt, copy.createdAt)
            Assertions.assertEquals(developer.version, copy.version)
            Assertions.assertEquals(developer.data.codeHash, copy.data.codeHash)
            Assertions.assertEquals(developer.data.expiresAt, copy.data.expiresAt)
        }

        @Test
        fun `should keep the manager variant if withData changes nothing`() {
            val copy: CommunityInvite.Manager = manager.withData { }

            Assertions.assertEquals(CommunityInvite.Kind.Manager, copy.kind)
            Assertions.assertEquals(manager.version, copy.version)
        }

        @Test
        fun `should replace the code if withData sets a new raw code`() {
            val copy = developer.withData { code("pqrstuvwxyz2", HashAlgorithm.Identity) }

            Assertions.assertEquals(InviteCodeHash(value = "pqrstuvwxyz2", algorithm = HashAlgorithm.Identity), copy.data.codeHash)
        }
    }
}
