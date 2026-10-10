package tech.testsys.infra.database.api.persistence.adapter.user

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import tech.testsys.domain.builder.api.developerData
import tech.testsys.domain.builder.api.judgeData
import tech.testsys.domain.builder.api.managerData
import tech.testsys.domain.builder.api.studentData
import tech.testsys.domain.builder.user.MultipleRoleUserDataBuilder
import tech.testsys.domain.contract.persistence.Pagination
import tech.testsys.domain.contract.persistence.Sort
import tech.testsys.domain.contract.persistence.UserFilter
import tech.testsys.domain.contract.persistence.repository.UserRepository
import tech.testsys.domain.model.group.Community
import tech.testsys.domain.model.group.CommunityId
import tech.testsys.domain.model.user.MultipleRoleUser
import tech.testsys.domain.model.user.Observer
import tech.testsys.domain.model.user.User
import tech.testsys.infra.database.DatabaseIntegrationTests
import java.time.Instant

class UserPaginationQueryTests : DatabaseIntegrationTests() {

    @Autowired
    private lateinit var repository: UserRepository

    private val allUsers = Pagination(page = 0, size = 20)

    @Test
    fun `should return members of every role observers and the administrator of created communities`() {
        val administrator = newAdministrator()
        val community = fixtures.community(owner = administrator)
        val developer = member(community) { developerIn(it) }
        val student = member(community) { studentIn(it) }
        val judge = member(community) { judgeIn(it) }
        val manager = member(community) { managerIn(it) }
        val coAdministrator = member(community) { administratorIn(it) }
        val observer = fixtures.observer(community)

        val page = repository.findAvailableToAdministrator(administratorId = administrator.id, pagination = allUsers)

        assertEquals(
            listOf(administrator.id, developer.id, student.id, judge.id, manager.id, coAdministrator.id, observer.id),
            page.content.map { user -> user.id },
        )
        assertEquals(List(6) { MultipleRoleUser::class } + Observer::class, page.content.map { user -> user::class })
        assertEquals(7L, page.totalElements)
    }

    @Test
    fun `should exclude users of foreign communities even if the administrator is an Administrator there`() {
        val foreign = fixtures.community(owner = newAdministrator())
        val administrator = fixtures.multipleRoleUser { administratorIn(listOf(foreign)) }
        val community = fixtures.community(owner = administrator)
        val member = member(community) { developerIn(it) }
        member(foreign) { developerIn(it) }
        fixtures.observer(foreign)

        val page = repository.findAvailableToAdministrator(administratorId = administrator.id, pagination = allUsers)

        assertEquals(listOf(administrator.id, member.id), page.content.map { user -> user.id })
        assertEquals(2L, page.totalElements)
    }

    @Test
    fun `should exclude participants and supervisors`() {
        val administrator = newAdministrator()
        val community = fixtures.community(owner = administrator)
        val manager = member(community) { managerIn(it) }
        fixtures.participant(fixtures.competition(owner = manager))
        fixtures.supervisor()

        val page = repository.findAvailableToAdministrator(administratorId = administrator.id, pagination = allUsers)

        assertEquals(listOf(administrator.id, manager.id), page.content.map { user -> user.id })
        assertEquals(2L, page.totalElements)
    }

    @Test
    fun `should return an empty page without the administrator if they created no communities`() {
        val foreign = fixtures.community(owner = newAdministrator())
        val administrator = fixtures.multipleRoleUser { administratorIn(listOf(foreign)) }
        member(foreign) { developerIn(it) }

        val page = repository.findAvailableToAdministrator(administratorId = administrator.id, pagination = allUsers)

        assertEquals(emptyList<User<*>>(), page.content)
        assertEquals(0L, page.totalElements)
        assertEquals(0, page.totalPages)
        assertFalse(page.hasNext)
    }

    @Test
    fun `should count a user of several communities and roles once`() {
        val administrator = newAdministrator()
        val first = fixtures.community(owner = administrator)
        val second = fixtures.community(owner = administrator)
        val member = fixtures.multipleRoleUser {
            developerIn(listOf(first, second))
            studentIn(listOf(first))
        }

        val page = repository.findAvailableToAdministrator(administratorId = administrator.id, pagination = allUsers)

        assertEquals(listOf(administrator.id, member.id), page.content.map { user -> user.id })
        assertEquals(2L, page.totalElements)
    }

    @ParameterizedTest
    @ValueSource(strings = ["alpha", "ALPHA", "%", "_", "\\", "  "])
    fun `should match literal case insensitive name substrings and preserve spaces`(substring: String) {
        val administrator = newAdministrator()
        val community = fixtures.community(owner = administrator)
        val expected = member(community, name = "  Alpha%_\\Beta  ") { developerIn(it) }
        member(community, name = "Other plain name") { developerIn(it) }
        member(fixtures.community(owner = newAdministrator()), name = "  Alpha%_\\Beta  ") { developerIn(it) }

        val page = repository.findAvailableToAdministrator(
            administratorId = administrator.id,
            pagination = Pagination(page = 0, size = 1),
            filter = UserFilter(name = substring),
        )

        assertEquals(listOf(expected.id), page.content.map { user -> user.id })
        assertEquals(1L, page.totalElements)
    }

    @Test
    fun `should select users holding any of the requested roles`() {
        val administrator = newAdministrator()
        val community = fixtures.community(owner = administrator)
        val developer = member(community) { developerIn(it) }
        member(community) { studentIn(it) }
        member(community) { judgeIn(it) }
        val observer = fixtures.observer(community)

        val page = repository.findAvailableToAdministrator(
            administratorId = administrator.id,
            pagination = allUsers,
            filter = UserFilter(roles = setOf(UserFilter.Role.DEVELOPER, UserFilter.Role.OBSERVER)),
        )

        assertEquals(listOf(developer.id, observer.id), page.content.map { user -> user.id })
        assertEquals(2L, page.totalElements)
    }

    @Test
    fun `should check requested roles only in communities created by the administrator`() {
        val administrator = newAdministrator()
        val community = fixtures.community(owner = administrator)
        val foreign = fixtures.community(owner = newAdministrator())
        val developer = member(community) { developerIn(it) }
        fixtures.multipleRoleUser {
            developerIn(listOf(foreign))
            studentIn(listOf(community))
        }

        val page = repository.findAvailableToAdministrator(
            administratorId = administrator.id,
            pagination = allUsers,
            filter = UserFilter(roles = setOf(UserFilter.Role.DEVELOPER)),
        )

        assertEquals(listOf(developer.id), page.content.map { user -> user.id })
        assertEquals(1L, page.totalElements)
    }

    @Test
    fun `should select the administrator and Administrator members by the Administrator role`() {
        val administrator = newAdministrator()
        val community = fixtures.community(owner = administrator)
        val coAdministrator = member(community) { administratorIn(it) }
        member(community) { developerIn(it) }
        fixtures.observer(community)

        val page = repository.findAvailableToAdministrator(
            administratorId = administrator.id,
            pagination = allUsers,
            filter = UserFilter(roles = setOf(UserFilter.Role.ADMINISTRATOR)),
        )

        assertEquals(listOf(administrator.id, coAdministrator.id), page.content.map { user -> user.id })
        assertEquals(2L, page.totalElements)
    }

    @Test
    fun `should select only observers by the Observer role`() {
        val administrator = newAdministrator()
        val community = fixtures.community(owner = administrator)
        member(community) { developerIn(it) }
        val observer = fixtures.observer(community)

        val page = repository.findAvailableToAdministrator(
            administratorId = administrator.id,
            pagination = allUsers,
            filter = UserFilter(roles = setOf(UserFilter.Role.OBSERVER)),
        )

        assertEquals(listOf(observer.id), page.content.map { user -> user.id })
        assertEquals(listOf(Observer::class), page.content.map { user -> user::class })
    }

    @Test
    fun `should count only membership in the selected community`() {
        val administrator = newAdministrator()
        val first = fixtures.community(owner = administrator)
        val second = fixtures.community(owner = administrator)
        member(first) { developerIn(it) }
        val student = member(second) { studentIn(it) }
        val observer = fixtures.observer(second)

        val page = repository.findAvailableToAdministrator(
            administratorId = administrator.id,
            pagination = allUsers,
            filter = UserFilter(communityId = second.id),
        )

        assertEquals(listOf(administrator.id, student.id, observer.id), page.content.map { user -> user.id })
        assertEquals(3L, page.totalElements)
    }

    @Test
    fun `should check requested roles within the selected community`() {
        val administrator = newAdministrator()
        val first = fixtures.community(owner = administrator)
        val second = fixtures.community(owner = administrator)
        val developer = member(first) { developerIn(it) }
        fixtures.multipleRoleUser {
            developerIn(listOf(second))
            studentIn(listOf(first))
        }

        val page = repository.findAvailableToAdministrator(
            administratorId = administrator.id,
            pagination = allUsers,
            filter = UserFilter(roles = setOf(UserFilter.Role.DEVELOPER), communityId = first.id),
        )

        assertEquals(listOf(developer.id), page.content.map { user -> user.id })
        assertEquals(1L, page.totalElements)
    }

    @Test
    fun `should return an empty page for a community created by another user`() {
        val administrator = newAdministrator()
        val community = fixtures.community(owner = administrator)
        val foreign = fixtures.community(owner = newAdministrator())
        fixtures.multipleRoleUser { developerIn(listOf(community, foreign)) }
        fixtures.observer(foreign)

        val page = repository.findAvailableToAdministrator(
            administratorId = administrator.id,
            pagination = allUsers,
            filter = UserFilter(communityId = foreign.id),
        )

        assertEquals(emptyList<User<*>>(), page.content)
        assertEquals(0L, page.totalElements)
    }

    @Test
    fun `should return an empty page for a nonexistent community`() {
        val administrator = newAdministrator()
        val community = fixtures.community(owner = administrator)
        member(community) { developerIn(it) }

        val page = repository.findAvailableToAdministrator(
            administratorId = administrator.id,
            pagination = allUsers,
            filter = UserFilter(communityId = CommunityId(Long.MAX_VALUE)),
        )

        assertEquals(emptyList<User<*>>(), page.content)
        assertEquals(0L, page.totalElements)
    }

    @ParameterizedTest
    @CsvSource("0,2,true", "1,1,false", "2,0,false")
    fun `should slice available users after filtering and counting`(index: Int, size: Int, hasNext: Boolean) {
        val administrator = newAdministrator()
        val community = fixtures.community(owner = administrator)
        val first = member(community) { developerIn(it) }
        val second = member(community) { studentIn(it) }
        member(fixtures.community(owner = newAdministrator())) { developerIn(it) }
        val expected = listOf(administrator.id, first.id, second.id).drop(index * 2).take(2)
        val request = Pagination(page = index, size = 2)

        val page = repository.findAvailableToAdministrator(administratorId = administrator.id, pagination = request)

        assertEquals(expected, page.content.map { user -> user.id })
        assertEquals(size, page.content.size)
        assertEquals(3L, page.totalElements)
        assertEquals(2, page.totalPages)
        assertEquals(hasNext, page.hasNext)
        assertSame(request, page.pagination)
    }

    @Test
    fun `should order users by the requested field`() {
        val administrator = newAdministrator()
        val community = fixtures.community(owner = administrator)
        val bravo = member(community, name = "Bravo") { developerIn(it) }
        val alpha = member(community, name = "Alpha") { developerIn(it) }
        val request = Pagination(page = 0, size = 3, sort = Sort(listOf(Sort.Order(field = "name"))))

        val page = repository.findAvailableToAdministrator(administratorId = administrator.id, pagination = request)

        assertEquals(listOf(alpha.id, bravo.id, administrator.id), page.content.map { user -> user.id })
    }

    @ParameterizedTest
    @CsvSource("0,first", "1,second")
    fun `should break equal custom sort values by ascending id across pages`(index: Int, expected: String) {
        val administrator = newAdministrator()
        val community = fixtures.community(owner = administrator)
        val first = member(community, name = "Equal") { developerIn(it) }
        val second = member(community, name = "Equal") { studentIn(it) }
        val saved = mapOf("first" to first.id, "second" to second.id)
        val request =
            Pagination(page = index, size = 1, sort = Sort(listOf(Sort.Order(field = "name", direction = Sort.Direction.DESC))))

        val page = repository.findAvailableToAdministrator(
            administratorId = administrator.id,
            pagination = request,
            filter = UserFilter(name = "Equal"),
        )

        assertEquals(listOf(saved.getValue(expected)), page.content.map { user -> user.id })
        assertEquals(2L, page.totalElements)
    }

    @Test
    fun `should order users by the last login`() {
        val administrator = newAdministrator()
        val community = fixtures.community(owner = administrator)
        val first = member(community) { developerIn(it) }
        val second = member(community) { studentIn(it) }
        repository.recordLogin(second.id, LOGGED_IN_AT)
        repository.recordLogin(administrator.id, LOGGED_IN_AT.plusSeconds(30))
        repository.recordLogin(first.id, LOGGED_IN_AT.plusSeconds(60))
        val request = Pagination(page = 0, size = 3, sort = Sort(listOf(Sort.Order(field = "lastLoginAt"))))

        val page = repository.findAvailableToAdministrator(administratorId = administrator.id, pagination = request)

        assertEquals(listOf(second.id, administrator.id, first.id), page.content.map { user -> user.id })
    }

    private fun newAdministrator(): MultipleRoleUser = fixtures.multipleRoleUser { administratorIn(emptyList()) }

    private fun member(
        community: Community,
        name: String? = null,
        role: MultipleRoleUserDataBuilder.(List<Community>) -> Unit,
    ): MultipleRoleUser = fixtures.multipleRoleUser {
        name?.let { userName -> this.name = userName }
        role(listOf(community))
    }

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

        val LOGGED_IN_AT: Instant = Instant.parse("2026-01-01T10:00:00Z")
    }
}
