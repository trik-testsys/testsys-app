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
import tech.testsys.domain.builder.api.withData
import tech.testsys.domain.builder.user.MultipleRoleUserDataBuilder
import tech.testsys.domain.contract.persistence.Pagination
import tech.testsys.domain.contract.persistence.UserFilter
import tech.testsys.domain.contract.persistence.repository.MultipleRoleUserRepository
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
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.user.UserJpaEntity
import tech.testsys.infra.database.internal.jpa.repository.user.UserJpaEntityRepository
import java.time.Instant

@OptIn(InternalDatabaseApi::class)
class UserPersistenceAdapterQueryTests : DatabaseIntegrationTests() {

    @Autowired
    private lateinit var repository: UserRepository

    @Autowired
    private lateinit var multipleRoleUsers: MultipleRoleUserRepository

    @Autowired
    private lateinit var userJpaEntityRepository: UserJpaEntityRepository

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
    inner class FindAvailableToAdministratorTests {

        @Test
        fun `should find a page with the same statement count for one and twenty members of each kind`() {
            val oneOfEachAdministrator = newAdministrator()
            val oneOfEachCommunity = fixtures.community(owner = oneOfEachAdministrator)
            val oneOfEach = listOf(
                fixtures.multipleRoleUser { developerIn(listOf(oneOfEachCommunity)) }.id,
                fixtures.observer(oneOfEachCommunity).id,
            )
            val twentyOfEachAdministrator = newAdministrator()
            val twentyOfEachCommunity = fixtures.community(owner = twentyOfEachAdministrator)
            val twentyOfEach = List(20) { fixtures.multipleRoleUser { developerIn(listOf(twentyOfEachCommunity)) }.id } +
                List(20) { fixtures.observer(twentyOfEachCommunity).id }
            val pagination = Pagination(page = 0, size = 50)
            val filter = UserFilter(roles = setOf(UserFilter.Role.DEVELOPER, UserFilter.Role.OBSERVER))

            val (one, oneOfEachStatements) = withStatementCount {
                repository.findAvailableToAdministrator(
                    administratorId = oneOfEachAdministrator.id,
                    pagination = pagination,
                    filter = filter,
                )
            }
            val (twenty, twentyOfEachStatements) = withStatementCount {
                repository.findAvailableToAdministrator(
                    administratorId = twentyOfEachAdministrator.id,
                    pagination = pagination,
                    filter = filter,
                )
            }

            assertEquals(oneOfEach.toSet(), one.content.map { user -> user.id }.toSet())
            assertEquals(twentyOfEach.toSet(), twenty.content.map { user -> user.id }.toSet())
            assertEquals(oneOfEachStatements, twentyOfEachStatements)
        }
    }

    @Nested
    inner class CountAvailableToAdministratorTests {

        @Test
        fun `should count members observers and the administrator of created communities once`() {
            val administrator = newAdministrator()
            val first = fixtures.community(owner = administrator)
            val second = fixtures.community(owner = administrator)
            fixtures.multipleRoleUser {
                developerIn(listOf(first, second))
                studentIn(listOf(first))
            }
            fixtures.observer(second)
            fixtures.multipleRoleUser { developerIn(listOf(fixtures.community(owner = newAdministrator()))) }

            val count = repository.countAvailableToAdministrator(administratorId = administrator.id)

            assertEquals(3L, count)
        }

        @Test
        fun `should count users of the filtered community including the administrator`() {
            val administrator = newAdministrator()
            val community = fixtures.community(owner = administrator)
            val other = fixtures.community(owner = administrator)
            fixtures.multipleRoleUser { developerIn(listOf(community)) }
            fixtures.multipleRoleUser { developerIn(listOf(other)) }

            val count = repository.countAvailableToAdministrator(
                administratorId = administrator.id,
                filter = UserFilter(communityId = community.id),
            )

            assertEquals(2L, count)
        }

        @Test
        fun `should count nobody in a foreign community`() {
            val administrator = newAdministrator()
            fixtures.community(owner = administrator)
            val foreign = fixtures.community(owner = newAdministrator())
            fixtures.multipleRoleUser { developerIn(listOf(foreign)) }

            val count = repository.countAvailableToAdministrator(
                administratorId = administrator.id,
                filter = UserFilter(communityId = foreign.id),
            )

            assertEquals(0L, count)
        }
    }

    @Nested
    inner class FindLastLoginsTests {

        @Test
        fun `should return the last logins of users of every kind`() {
            val user = fixtures.developer()
            val observer = fixtures.observer()
            repository.recordLogin(user.id, LOGGED_IN_AT)
            repository.recordLogin(observer.id, LOGGED_IN_AT.plusSeconds(60))

            val logins = repository.findLastLogins(listOf(user.id, observer.id))

            assertEquals(mapOf(user.id to LOGGED_IN_AT, observer.id to LOGGED_IN_AT.plusSeconds(60)), logins)
        }

        @Test
        fun `should omit users without a login and missing users`() {
            val user = fixtures.developer()

            val logins = repository.findLastLogins(listOf(user.id, MultipleRoleUserId(Long.MAX_VALUE)))

            assertEquals(emptyMap<UserId, Instant>(), logins)
        }

        @Test
        fun `should omit a user requested by an id of another kind`() {
            val user = fixtures.developer()
            repository.recordLogin(user.id, LOGGED_IN_AT)

            val logins = repository.findLastLogins(listOf(SingleRoleUserId(user.id.value)))

            assertEquals(emptyMap<UserId, Instant>(), logins)
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

    @Nested
    inner class RecordLoginTests {

        private val newUserOf: Map<String, () -> UserId> = mapOf(
            "multipleRoleUser" to { fixtures.developer().id },
            "observer" to { fixtures.observer().id },
            "participant" to { fixtures.participant().id },
            "supervisor" to { fixtures.supervisor().id },
        )

        @ParameterizedTest
        @ValueSource(strings = ["multipleRoleUser", "observer", "participant", "supervisor"])
        fun `should record the moment of the last login of a user of every kind`(kind: String) {
            val userId = newUserOf.getValue(kind)()

            repository.recordLogin(userId, LOGGED_IN_AT)

            assertEquals(LOGGED_IN_AT, rowOf(userId).lastLoginAt)
        }

        @Test
        fun `should replace the previous last login`() {
            val userId = fixtures.developer().id
            repository.recordLogin(userId, LOGGED_IN_AT)

            repository.recordLogin(userId, LOGGED_IN_AT.plusSeconds(60))

            assertEquals(LOGGED_IN_AT.plusSeconds(60), rowOf(userId).lastLoginAt)
        }

        @Test
        fun `should increment the user version when recording a login`() {
            val userId = fixtures.developer().id
            val beforeVersion = rowOf(userId).version

            repository.recordLogin(userId, LOGGED_IN_AT)

            val after = rowOf(userId)
            assertEquals(beforeVersion + 1, after.version)
        }

        @Test
        fun `should keep the last login when the user is updated`() {
            val user = fixtures.developer()
            repository.recordLogin(user.id, LOGGED_IN_AT)

            val refreshed = multipleRoleUsers.findById(user.id)!!
            multipleRoleUsers.update(refreshed.withData { name = "Renamed" })

            assertEquals(LOGGED_IN_AT, rowOf(user.id).lastLoginAt)
        }

        @Test
        fun `should not record a login of a nonexistent user`() {
            val user = fixtures.developer()

            repository.recordLogin(MultipleRoleUserId(Long.MAX_VALUE), LOGGED_IN_AT)

            assertNull(rowOf(user.id).lastLoginAt)
        }

        @Test
        fun `should not record a login of a multiple role user requested by a single role id`() {
            val user = fixtures.developer()

            repository.recordLogin(SingleRoleUserId(user.id.value), LOGGED_IN_AT)

            assertNull(rowOf(user.id).lastLoginAt)
        }

        private fun rowOf(userId: UserId): UserJpaEntity = userJpaEntityRepository.findById(userId.value).orElseThrow()
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

    private companion object {

        val LOGGED_IN_AT: Instant = Instant.parse("2026-01-01T10:00:00.123456Z")
    }
}
