package tech.testsys.infra.database.api.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import tech.testsys.domain.builder.api.developerData
import tech.testsys.domain.builder.api.judgeData
import tech.testsys.domain.builder.api.managerData
import tech.testsys.domain.builder.api.studentData
import tech.testsys.domain.builder.user.MultipleRoleUserDataBuilder
import tech.testsys.domain.contract.persistence.Page
import tech.testsys.domain.contract.persistence.Pagination
import tech.testsys.domain.contract.persistence.Sort
import tech.testsys.domain.model.group.ClassId
import tech.testsys.domain.model.group.Community
import tech.testsys.domain.model.group.CommunityId
import tech.testsys.domain.model.group.CompetitionId
import tech.testsys.domain.model.user.MultipleRoleUser
import tech.testsys.domain.model.user.Observer
import tech.testsys.domain.model.user.Participant
import tech.testsys.domain.model.user.Supervisor
import tech.testsys.domain.model.user.User
import tech.testsys.infra.database.DatabaseIntegrationTests

class DatabaseServiceUserSearchTests : DatabaseIntegrationTests() {

    @Autowired
    private lateinit var search: ServiceUserSearch

    private val allUsers = Pagination(page = 0, size = 50)

    @Nested
    inner class FindTests {

        @Test
        fun `should return users of every kind as entities of their kinds without conditions`() {
            val manager = fixtures.manager()
            val participant = fixtures.participant(fixtures.competition(owner = manager))
            val observer = fixtures.observer(fixtures.community(owner = manager))
            val supervisor = fixtures.supervisor()

            val page = search.find(ServiceUserFilter(), allUsers)

            assertEquals(listOf(manager.id.value, participant.id.value, observer.id.value, supervisor.id.value), idsOf(page))
            assertEquals(
                listOf(MultipleRoleUser::class, Participant::class, Observer::class, Supervisor::class),
                page.content.map { user -> user::class },
            )
            assertEquals(4L, page.totalElements)
        }

        @Test
        fun `should select only the user with exactly the requested id`() {
            val expected = fixtures.developer()
            fixtures.developer()
            fixtures.supervisor()

            val page = search.find(ServiceUserFilter(id = ServiceUserId(expected.id.value)), allUsers)

            assertEquals(listOf(expected.id.value), idsOf(page))
            assertEquals(1L, page.totalElements)
        }

        @Test
        fun `should select a single-role user by id`() {
            fixtures.developer()
            val supervisor = fixtures.supervisor()

            val page = search.find(ServiceUserFilter(id = ServiceUserId(supervisor.id.value)), allUsers)

            assertEquals(listOf(supervisor.id.value), idsOf(page))
        }

        @ParameterizedTest
        @ValueSource(strings = ["alpha", "ALPHA", "%", "_", "\\", "  "])
        fun `should match literal case insensitive name substrings`(substring: String) {
            val expected = fixtures.multipleRoleUser {
                name = "  Alpha%_\\Beta  "
                roles { developer { data = developerData {} } }
            }
            fixtures.multipleRoleUser {
                name = "Other plain name"
                roles { developer { data = developerData {} } }
            }

            val page = search.find(ServiceUserFilter(name = substring), allUsers)

            assertEquals(listOf(expected.id.value), idsOf(page))
            assertEquals(1L, page.totalElements)
        }

        @Test
        fun `should match single-role users by name`() {
            fixtures.developer()
            val supervisor = fixtures.supervisor()

            val page = search.find(ServiceUserFilter(name = "SUPERVISOR"), allUsers)

            assertEquals(listOf(supervisor.id.value), idsOf(page))
        }

        @ParameterizedTest
        @ValueSource(strings = ["alpha.case@", "ALPHA.CASE@EXAMPLE", "Case@Ex"])
        fun `should match case insensitive e-mail substrings`(substring: String) {
            val expected = fixtures.multipleRoleUser {
                email = "Alpha.Case@Example.org"
                roles { developer { data = developerData {} } }
            }
            fixtures.developer()

            val page = search.find(ServiceUserFilter(email = substring), allUsers)

            assertEquals(listOf(expected.id.value), idsOf(page))
            assertEquals(1L, page.totalElements)
        }

        @Test
        fun `should exclude users with fixed roles by any e-mail substring`() {
            val manager = fixtures.manager()
            fixtures.participant(fixtures.competition(owner = manager))
            fixtures.observer(fixtures.community(owner = manager))
            fixtures.supervisor()

            val page = search.find(ServiceUserFilter(email = "example"), allUsers)

            assertEquals(listOf(manager.id.value), idsOf(page))
            assertEquals(1L, page.totalElements)
        }

        @Test
        fun `should select users holding any of the requested roles`() {
            val administrator = fixtures.administrator()
            val developer = fixtures.developer()
            fixtures.student()
            val participant = fixtures.participant(fixtures.competition(owner = administrator))
            fixtures.observer(fixtures.community(owner = administrator))
            val supervisor = fixtures.supervisor()

            val page = search.find(
                ServiceUserFilter(roles = setOf(ServiceUserRole.DEVELOPER, ServiceUserRole.PARTICIPANT, ServiceUserRole.SUPERVISOR)),
                allUsers,
            )

            assertEquals(listOf(developer.id.value, participant.id.value, supervisor.id.value), idsOf(page))
            assertEquals(3L, page.totalElements)
        }

        @Test
        fun `should select each non-fixed role by its role data`() {
            val administrator = fixtures.administrator()
            fixtures.developer()
            val judge = fixtures.judge()
            val manager = fixtures.manager()
            val student = fixtures.student()

            val filter = ServiceUserFilter(
                roles = setOf(ServiceUserRole.ADMINISTRATOR, ServiceUserRole.JUDGE, ServiceUserRole.MANAGER, ServiceUserRole.STUDENT),
            )

            val page = search.find(filter, allUsers)

            assertEquals(listOf(administrator.id.value, judge.id.value, manager.id.value, student.id.value), idsOf(page))
        }

        @Test
        fun `should select only observers by the Observer role`() {
            val administrator = fixtures.administrator()
            val observer = fixtures.observer(fixtures.community(owner = administrator))

            val page = search.find(ServiceUserFilter(roles = setOf(ServiceUserRole.OBSERVER)), allUsers)

            assertEquals(listOf(observer.id.value), idsOf(page))
            assertEquals(listOf(Observer::class), page.content.map { user -> user::class })
        }

        @Test
        fun `should combine roles with the community condition`() {
            val owner = fixtures.administrator()
            val community = fixtures.community(owner = owner)
            val other = fixtures.community(owner = owner)
            val developer = member(community) { developerIn(it) }
            member(community) { studentIn(it) }
            member(other) { developerIn(it) }

            val page = search.find(ServiceUserFilter(roles = setOf(ServiceUserRole.DEVELOPER), communityId = community.id), allUsers)

            assertEquals(listOf(developer.id.value), idsOf(page))
            assertEquals(1L, page.totalElements)
        }

        @Test
        fun `should not select a user holding the requested role only in another community`() {
            val owner = fixtures.administrator()
            val community = fixtures.community(owner = owner)
            val other = fixtures.community(owner = owner)
            fixtures.multipleRoleUser {
                developerIn(listOf(community))
                studentIn(listOf(other))
            }

            val page = search.find(ServiceUserFilter(roles = setOf(ServiceUserRole.STUDENT), communityId = community.id), allUsers)

            assertEquals(emptyList<User<*>>(), page.content)
            assertEquals(0L, page.totalElements)
        }

        @ParameterizedTest
        @EnumSource(ServiceUserRole::class, names = ["DEVELOPER", "JUDGE", "MANAGER", "STUDENT"])
        fun `should select a non-fixed role by membership in the community`(role: ServiceUserRole) {
            val owner = fixtures.administrator()
            val community = fixtures.community(owner = owner)
            val member = member(community) { roleIn(role, it) }
            member(fixtures.community(owner = owner)) { roleIn(role, it) }

            val page = search.find(ServiceUserFilter(roles = setOf(role), communityId = community.id), allUsers)

            assertEquals(listOf(member.id.value), idsOf(page))
            assertEquals(1L, page.totalElements)
        }

        @Test
        fun `should select administrators of the community and its owner by the Administrator role`() {
            val owner = fixtures.developer()
            val community = fixtures.community(owner = owner)
            val administrator = member(community) { administratorIn(it) }
            member(fixtures.community(owner = fixtures.developer())) { administratorIn(it) }

            val page = search.find(ServiceUserFilter(roles = setOf(ServiceUserRole.ADMINISTRATOR), communityId = community.id), allUsers)

            assertEquals(listOf(owner.id.value, administrator.id.value), idsOf(page))
            assertEquals(2L, page.totalElements)
        }

        @Test
        fun `should not select the owner of a community by another role`() {
            val community = fixtures.community(owner = fixtures.developer())

            val page = search.find(ServiceUserFilter(roles = setOf(ServiceUserRole.DEVELOPER), communityId = community.id), allUsers)

            assertEquals(emptyList<User<*>>(), page.content)
            assertEquals(0L, page.totalElements)
        }

        @Test
        fun `should select only observers of the community by the Observer role with it`() {
            val owner = fixtures.administrator()
            val community = fixtures.community(owner = owner)
            val observer = fixtures.observer(community)
            fixtures.observer(fixtures.community(owner = owner))
            member(community) { developerIn(it) }

            val page = search.find(ServiceUserFilter(roles = setOf(ServiceUserRole.OBSERVER), communityId = community.id), allUsers)

            assertEquals(listOf(observer.id.value), idsOf(page))
            assertEquals(1L, page.totalElements)
        }

        @Test
        fun `should select no participants or supervisors by their roles with a community`() {
            val owner = fixtures.manager()
            val community = fixtures.community(owner = owner)
            fixtures.participant(fixtures.competition(owner = owner))
            fixtures.supervisor()
            member(community) { developerIn(it) }

            val page = search.find(
                ServiceUserFilter(roles = setOf(ServiceUserRole.PARTICIPANT, ServiceUserRole.SUPERVISOR), communityId = community.id),
                allUsers,
            )

            assertEquals(emptyList<User<*>>(), page.content)
            assertEquals(0L, page.totalElements)
        }

        @Test
        fun `should select members of every role observers and the owner of a community`() {
            val owner = fixtures.administrator()
            val community = fixtures.community(owner = owner)
            val developer = member(community) { developerIn(it) }
            val manager = member(community) { managerIn(it) }
            val judge = member(community) { judgeIn(it) }
            val observer = fixtures.observer(community)
            member(fixtures.community(owner = owner)) { developerIn(it) }
            fixtures.observer(fixtures.community(owner = owner))

            val page = search.find(ServiceUserFilter(communityId = community.id), allUsers)

            assertEquals(listOf(owner.id.value, developer.id.value, manager.id.value, judge.id.value, observer.id.value), idsOf(page))
            assertEquals(5L, page.totalElements)
        }

        @Test
        fun `should not add students of classes and participants of competitions of community members to the community`() {
            val owner = fixtures.administrator()
            val community = fixtures.community(owner = owner)
            val manager = member(community) { managerIn(it) }
            fixtures.studentClass(owner = manager, students = listOf(fixtures.student()))
            fixtures.participant(fixtures.competition(owner = manager))

            val page = search.find(ServiceUserFilter(communityId = community.id), allUsers)

            assertEquals(listOf(owner.id.value, manager.id.value), idsOf(page))
        }

        @Test
        fun `should select students and the owner of a class`() {
            val owner = fixtures.manager()
            val first = fixtures.student()
            val second = fixtures.student()
            val studyClass = fixtures.studentClass(owner = owner, students = listOf(first, second))
            fixtures.studentClass(owner = fixtures.manager(), students = listOf(fixtures.student()))

            val page = search.find(ServiceUserFilter(classId = studyClass.id), allUsers)

            assertEquals(listOf(owner.id.value, first.id.value, second.id.value), idsOf(page))
            assertEquals(3L, page.totalElements)
        }

        @Test
        fun `should select participants the owner and observers assigned a contest of a competition`() {
            val owner = fixtures.manager()
            val contest = fixtures.contest()
            val competition = fixtures.competition(owner = owner, contests = listOf(contest))
            val participant = fixtures.participant(competition)
            val community = fixtures.community(owner = fixtures.administrator())
            val observer = fixtures.observer(community, contests = listOf(contest))
            fixtures.participant(fixtures.competition(owner = owner))

            val page = search.find(ServiceUserFilter(competitionId = competition.id), allUsers)

            assertEquals(listOf(owner.id.value, participant.id.value, observer.id.value), idsOf(page))
            assertEquals(3L, page.totalElements)
        }

        @Test
        fun `should not select an observer assigned only contests outside the competition`() {
            val owner = fixtures.manager()
            val competition = fixtures.competition(owner = owner, contests = listOf(fixtures.contest()))
            fixtures.observer(fixtures.community(owner = owner), contests = listOf(fixtures.contest()))

            val page = search.find(ServiceUserFilter(competitionId = competition.id), allUsers)

            assertEquals(listOf(owner.id.value), idsOf(page))
        }

        @Test
        fun `should not select a judge of a competition submission by the judgment order`() {
            val owner = fixtures.manager()
            val contest = fixtures.contest()
            val competition = fixtures.competition(owner = owner, contests = listOf(contest))
            val participant = fixtures.participant(competition)
            val submission = fixtures.gradingSubmission(authorId = participant.id, contest = contest)
            fixtures.judgmentOrder(judge = fixtures.judge(), submission = submission)

            val page = search.find(ServiceUserFilter(competitionId = competition.id), allUsers)

            assertEquals(listOf(owner.id.value, participant.id.value), idsOf(page))
        }

        @Test
        fun `should select a judge owning the competition`() {
            val owner = fixtures.multipleRoleUser {
                roles {
                    manager { data = managerData {} }
                    judge { data = judgeData {} }
                }
            }
            val competition = fixtures.competition(owner = owner)
            fixtures.judge()

            val page = search.find(ServiceUserFilter(roles = setOf(ServiceUserRole.JUDGE), competitionId = competition.id), allUsers)

            assertEquals(listOf(owner.id.value), idsOf(page))
        }

        @Test
        fun `should return a member holding several requested roles in the community once`() {
            val community = fixtures.community(owner = fixtures.administrator())
            val user = fixtures.multipleRoleUser {
                developerIn(listOf(community))
                studentIn(listOf(community))
                managerIn(listOf(community))
            }

            val page = search.find(
                ServiceUserFilter(
                    roles = setOf(ServiceUserRole.DEVELOPER, ServiceUserRole.STUDENT, ServiceUserRole.MANAGER),
                    communityId = community.id,
                ),
                allUsers,
            )

            assertEquals(listOf(user.id.value), idsOf(page))
            assertEquals(1L, page.totalElements)
        }

        @Test
        fun `should return a user matching intersecting community class and competition conditions once`() {
            val community = fixtures.community(owner = fixtures.administrator())
            val manager = fixtures.multipleRoleUser {
                managerIn(listOf(community))
                studentIn(listOf(community))
            }
            val student = member(community) { studentIn(it) }
            val studyClass = fixtures.studentClass(owner = manager, students = listOf(manager, student))
            val competition = fixtures.competition(owner = manager)

            val page = search.find(
                ServiceUserFilter(communityId = community.id, classId = studyClass.id, competitionId = competition.id),
                allUsers,
            )

            assertEquals(listOf(manager.id.value), idsOf(page))
            assertEquals(1L, page.totalElements)
        }

        @ParameterizedTest
        @MethodSource("tech.testsys.infra.database.api.service.DatabaseServiceUserSearchTests#unknownGroupFilters")
        fun `should return an empty page for an unknown group`(filter: ServiceUserFilter) {
            val manager = fixtures.manager()
            fixtures.studentClass(owner = manager, students = listOf(fixtures.student()))
            fixtures.participant(fixtures.competition(owner = manager))
            fixtures.observer(fixtures.community(owner = manager))

            val page = search.find(filter, allUsers)

            assertEquals(emptyList<User<*>>(), page.content)
            assertEquals(0L, page.totalElements)
        }

        @Test
        fun `should return the first page of matching users with a next page`() {
            val first = fixtures.developer()
            val second = fixtures.developer()
            fixtures.developer()
            fixtures.supervisor()
            val request = Pagination(page = 0, size = 2)

            val page = search.find(ServiceUserFilter(roles = setOf(ServiceUserRole.DEVELOPER)), request)

            assertEquals(listOf(first.id.value, second.id.value), idsOf(page))
            assertEquals(3L, page.totalElements)
            assertTrue(page.hasNext)
            assertSame(request, page.pagination)
        }

        @Test
        fun `should return the partial last page of matching users without a next page`() {
            fixtures.developer()
            fixtures.developer()
            val third = fixtures.developer()
            fixtures.supervisor()
            val request = Pagination(page = 1, size = 2)

            val page = search.find(ServiceUserFilter(roles = setOf(ServiceUserRole.DEVELOPER)), request)

            assertEquals(listOf(third.id.value), idsOf(page))
            assertEquals(3L, page.totalElements)
            assertFalse(page.hasNext)
            assertSame(request, page.pagination)
        }

        @Test
        fun `should order users by name descending and break ties by ascending id`() {
            val alpha = named("Alpha")
            val firstEqual = named("Bravo")
            val secondEqual = named("Bravo")
            val descendingName = Sort(listOf(Sort.Order(field = "name", direction = Sort.Direction.DESC)))
            val request = Pagination(page = 0, size = 3, sort = descendingName)

            val page = search.find(ServiceUserFilter(), request)

            assertEquals(listOf(firstEqual.id.value, secondEqual.id.value, alpha.id.value), idsOf(page))
        }

        @Test
        fun `should reject sorting by a field outside the allowed ones`() {
            val request = Pagination(page = 0, size = 3, sort = Sort(listOf(Sort.Order(field = "accessToken"))))

            assertThrows(IllegalArgumentException::class.java) { search.find(ServiceUserFilter(), request) }
        }

        @Test
        fun `should return an empty page without users`() {
            val page = search.find(ServiceUserFilter(), allUsers)

            assertEquals(emptyList<User<*>>(), page.content)
            assertEquals(0L, page.totalElements)
            assertFalse(page.hasNext)
        }
    }

    @Nested
    inner class FindByIdTests {

        @Test
        fun `should find a user with non-fixed roles by id with the stored password hash`() {
            val user = fixtures.multipleRoleUser { roles { student { data = studentData {} } } }

            val found = search.findById(ServiceUserId(user.id.value))

            assertEquals(user.id, (found as? MultipleRoleUser)?.id)
            assertEquals(user.data.passwordHash, (found as? MultipleRoleUser)?.data?.passwordHash)
        }

        @Test
        fun `should find a participant by id`() {
            val participant = fixtures.participant(rawAccessToken = "participant-token")

            val found = search.findById(ServiceUserId(participant.id.value))

            assertEquals(participant.id, (found as? Participant)?.id)
            assertEquals("participant-token", (found as? Participant)?.data?.accessTokenHash?.value)
        }

        @Test
        fun `should find an observer by id`() {
            val observer = fixtures.observer(fixtures.community(owner = fixtures.administrator()))

            val found = search.findById(ServiceUserId(observer.id.value))

            assertEquals(observer.id, (found as? Observer)?.id)
        }

        @Test
        fun `should find a supervisor by id`() {
            val supervisor = fixtures.supervisor()

            val found = search.findById(ServiceUserId(supervisor.id.value))

            assertEquals(supervisor.id, (found as? Supervisor)?.id)
        }

        @Test
        fun `should return null for an unknown id`() {
            fixtures.developer()

            val found = search.findById(ServiceUserId(Long.MAX_VALUE))

            assertNull(found)
        }
    }

    private fun idsOf(page: Page<User<*>>): List<Long> = page.content.map { user -> user.id.value }

    private fun named(name: String): MultipleRoleUser = fixtures.multipleRoleUser {
        this.name = name
        roles { developer { data = developerData {} } }
    }

    private fun member(community: Community, role: MultipleRoleUserDataBuilder.(List<Community>) -> Unit): MultipleRoleUser =
        fixtures.multipleRoleUser { role(listOf(community)) }

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

    private fun MultipleRoleUserDataBuilder.roleIn(role: ServiceUserRole, communities: List<Community>) = when (role) {
        ServiceUserRole.DEVELOPER -> developerIn(communities)
        ServiceUserRole.JUDGE -> judgeIn(communities)
        ServiceUserRole.MANAGER -> managerIn(communities)
        ServiceUserRole.STUDENT -> studentIn(communities)
        ServiceUserRole.ADMINISTRATOR, ServiceUserRole.PARTICIPANT, ServiceUserRole.OBSERVER, ServiceUserRole.SUPERVISOR ->
            error("Role $role is not covered by this helper")
    }

    companion object {

        @JvmStatic
        fun unknownGroupFilters(): List<ServiceUserFilter> = listOf(
            ServiceUserFilter(communityId = CommunityId(Long.MAX_VALUE)),
            ServiceUserFilter(classId = ClassId(Long.MAX_VALUE)),
            ServiceUserFilter(competitionId = CompetitionId(Long.MAX_VALUE)),
        )
    }
}
