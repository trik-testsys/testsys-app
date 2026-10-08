package tech.testsys.infra.database.api.persistence.adapter.user

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import tech.testsys.domain.builder.api.developerData
import tech.testsys.domain.builder.api.judgeData
import tech.testsys.domain.builder.api.managerData
import tech.testsys.domain.builder.api.studentData
import tech.testsys.domain.builder.user.MultipleRoleUserDataBuilder
import tech.testsys.domain.contract.persistence.repository.UserRepository
import tech.testsys.domain.model.group.Community
import tech.testsys.domain.model.user.Developer
import tech.testsys.domain.model.user.MultipleRoleUser
import tech.testsys.domain.model.user.MultipleRoleUserId
import tech.testsys.domain.model.user.Observer
import tech.testsys.domain.model.user.SingleRoleUserId
import tech.testsys.domain.model.user.Student
import tech.testsys.domain.model.user.UserId
import tech.testsys.infra.database.DatabaseIntegrationTests

class UserPersistenceAdapterQueryTests : DatabaseIntegrationTests() {

    @Autowired
    private lateinit var repository: UserRepository

    @Nested
    inner class FindAvailableToAdministratorByIdTests {

        private val roleIn: Map<String, MultipleRoleUserDataBuilder.(List<Community>) -> Unit> = mapOf(
            "administrator" to { communities -> administratorIn(communities) },
            "developer" to { communities -> developerIn(communities) },
            "judge" to { communities -> judgeIn(communities) },
            "manager" to { communities -> managerIn(communities) },
            "student" to { communities -> studentIn(communities) },
        )

        @ParameterizedTest
        @ValueSource(strings = ["administrator", "developer", "judge", "manager", "student"])
        fun `should find a member of a created community in every role`(role: String) {
            val administrator = newAdministrator()
            val community = fixtures.community(owner = administrator)
            val member = fixtures.multipleRoleUser { roleIn.getValue(role)(this, listOf(community)) }

            val actual = repository.findAvailableToAdministratorById(administratorId = administrator.id, userId = member.id)

            assertEquals(member.id, assertInstanceOf(MultipleRoleUser::class.java, actual).id)
        }

        @Test
        fun `should find an observer of a created community`() {
            val administrator = newAdministrator()
            val observer = fixtures.observer(fixtures.community(owner = administrator))

            val actual = repository.findAvailableToAdministratorById(administratorId = administrator.id, userId = observer.id)

            assertEquals(observer.id, assertInstanceOf(Observer::class.java, actual).id)
        }

        @Test
        fun `should find the administrator themself if they created a community`() {
            val administrator = newAdministrator()
            fixtures.community(owner = administrator)

            val actual = repository.findAvailableToAdministratorById(administratorId = administrator.id, userId = administrator.id)

            assertEquals(administrator.id, assertInstanceOf(MultipleRoleUser::class.java, actual).id)
        }

        @Test
        fun `should return all roles of the user including membership in foreign communities`() {
            val administrator = newAdministrator()
            val community = fixtures.community(owner = administrator)
            val foreign = fixtures.community(owner = newAdministrator())
            val member = fixtures.multipleRoleUser {
                developerIn(listOf(community))
                studentIn(listOf(foreign))
            }

            val actual = repository.findAvailableToAdministratorById(administratorId = administrator.id, userId = member.id)

            assertEquals(
                mapOf(Developer::class to setOf(community.id), Student::class to setOf(foreign.id)),
                assertInstanceOf(MultipleRoleUser::class.java, actual).data.roles
                    .associate { role -> role::class to role.memberOf.ids.toSet() },
            )
        }

        @Test
        fun `should return null for the administrator themself if they created no communities`() {
            val foreign = fixtures.community(owner = newAdministrator())
            val administrator = fixtures.multipleRoleUser { administratorIn(listOf(foreign)) }

            val actual = repository.findAvailableToAdministratorById(administratorId = administrator.id, userId = administrator.id)

            assertNull(actual)
        }

        @Test
        fun `should return null for a member of only foreign communities even if the administrator is an Administrator there`() {
            val foreign = fixtures.community(owner = newAdministrator())
            val administrator = fixtures.multipleRoleUser { administratorIn(listOf(foreign)) }
            fixtures.community(owner = administrator)
            val member = fixtures.multipleRoleUser { developerIn(listOf(foreign)) }

            val actual = repository.findAvailableToAdministratorById(administratorId = administrator.id, userId = member.id)

            assertNull(actual)
        }

        @Test
        fun `should return null for an observer of a foreign community`() {
            val administrator = newAdministrator()
            fixtures.community(owner = administrator)
            val observer = fixtures.observer(fixtures.community(owner = newAdministrator()))

            val actual = repository.findAvailableToAdministratorById(administratorId = administrator.id, userId = observer.id)

            assertNull(actual)
        }

        @Test
        fun `should return null for a participant of a competition of a community member`() {
            val administrator = newAdministrator()
            val community = fixtures.community(owner = administrator)
            val manager = fixtures.multipleRoleUser { managerIn(listOf(community)) }
            val participant = fixtures.participant(fixtures.competition(owner = manager))

            val actual = repository.findAvailableToAdministratorById(administratorId = administrator.id, userId = participant.id)

            assertNull(actual)
        }

        @Test
        fun `should return null for a supervisor`() {
            val administrator = newAdministrator()
            fixtures.community(owner = administrator)
            val supervisor = fixtures.supervisor()

            val actual = repository.findAvailableToAdministratorById(administratorId = administrator.id, userId = supervisor.id)

            assertNull(actual)
        }

        @Test
        fun `should return null for a nonexistent user`() {
            val administrator = newAdministrator()
            fixtures.community(owner = administrator)

            val actual = repository.findAvailableToAdministratorById(
                administratorId = administrator.id,
                userId = MultipleRoleUserId(Long.MAX_VALUE),
            )

            assertNull(actual)
        }

        @Test
        fun `should return null for an available multiple role user requested by a single role id`() {
            val administrator = newAdministrator()
            val member = fixtures.multipleRoleUser { developerIn(listOf(fixtures.community(owner = administrator))) }

            val actual = repository.findAvailableToAdministratorById(
                administratorId = administrator.id,
                userId = SingleRoleUserId(member.id.value),
            )

            assertNull(actual)
        }

        @Test
        fun `should return null for an available observer requested by a multiple role id`() {
            val administrator = newAdministrator()
            val observer = fixtures.observer(fixtures.community(owner = administrator))

            val actual = repository.findAvailableToAdministratorById(
                administratorId = administrator.id,
                userId = MultipleRoleUserId(observer.id.value),
            )

            assertNull(actual)
        }
    }

    @Nested
    inner class ExistsByIdTests {

        private val newUserOf: Map<String, () -> UserId> = mapOf(
            "multipleRoleUser" to { fixtures.developer().id },
            "observer" to { fixtures.observer().id },
            "participant" to { fixtures.participant().id },
            "supervisor" to { fixtures.supervisor().id },
        )

        @ParameterizedTest
        @ValueSource(strings = ["multipleRoleUser", "observer", "participant", "supervisor"])
        fun `should find a user of every kind`(kind: String) {
            val userId = newUserOf.getValue(kind)()

            val isFound = repository.existsById(userId)

            assertTrue(isFound)
        }

        @Test
        fun `should not find a nonexistent user`() {
            fixtures.developer()

            val isFound = repository.existsById(MultipleRoleUserId(Long.MAX_VALUE))

            assertFalse(isFound)
        }

        @Test
        fun `should not find a multiple role user by a single role id`() {
            val user = fixtures.developer()

            val isFound = repository.existsById(SingleRoleUserId(user.id.value))

            assertFalse(isFound)
        }

        @Test
        fun `should not find an observer by a multiple role id`() {
            val observer = fixtures.observer()

            val isFound = repository.existsById(MultipleRoleUserId(observer.id.value))

            assertFalse(isFound)
        }
    }

    private fun newAdministrator(): MultipleRoleUser = fixtures.multipleRoleUser { administratorIn(emptyList()) }

    private fun MultipleRoleUserDataBuilder.developerIn(communities: List<Community>) = roles {
        developer {
            memberOf(communities.map { community -> community.id.value })
            data = developerData {}
        }
    }

    private fun MultipleRoleUserDataBuilder.studentIn(communities: List<Community>) = roles {
        student {
            memberOf(communities.map { community -> community.id.value })
            data = studentData {}
        }
    }

    private fun MultipleRoleUserDataBuilder.judgeIn(communities: List<Community>) = roles {
        judge {
            memberOf(communities.map { community -> community.id.value })
            data = judgeData {}
        }
    }

    private fun MultipleRoleUserDataBuilder.managerIn(communities: List<Community>) = roles {
        manager {
            memberOf(communities.map { community -> community.id.value })
            data = managerData {}
        }
    }

    private fun MultipleRoleUserDataBuilder.administratorIn(communities: List<Community>) = roles {
        administrator { memberOf(communities.map { community -> community.id.value }) }
    }
}
