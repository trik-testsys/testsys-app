package tech.testsys.operation.user

import io.mockk.Called
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import tech.testsys.domain.builder.api.communityInviteData
import tech.testsys.domain.builder.api.developerCommunityInvite
import tech.testsys.domain.builder.api.developerData
import tech.testsys.domain.builder.api.judgmentOrder
import tech.testsys.domain.builder.api.judgmentOrderData
import tech.testsys.domain.builder.api.managerCommunityInvite
import tech.testsys.domain.contract.persistence.ContestFilter
import tech.testsys.domain.contract.persistence.Page
import tech.testsys.domain.contract.persistence.Pagination
import tech.testsys.domain.contract.persistence.SubmissionCount
import tech.testsys.domain.contract.persistence.SupervisorCommunityReadPort
import tech.testsys.domain.contract.persistence.SupervisorUserReadPort
import tech.testsys.domain.contract.persistence.UserFilter
import tech.testsys.domain.contract.persistence.repository.CommunityRepository
import tech.testsys.domain.contract.persistence.repository.ContestRepository
import tech.testsys.domain.contract.persistence.repository.MultipleRoleUserRepository
import tech.testsys.domain.contract.persistence.repository.ObserverRepository
import tech.testsys.domain.contract.persistence.repository.UserRepository
import tech.testsys.domain.model.EntityVersion
import tech.testsys.domain.model.group.CommunityId
import tech.testsys.domain.model.group.CommunityInvite
import tech.testsys.domain.model.task.ContestId
import tech.testsys.domain.model.task.SolutionId
import tech.testsys.domain.model.user.CommunityRole
import tech.testsys.domain.model.user.HashAlgorithm
import tech.testsys.domain.model.user.ObserverData
import tech.testsys.operation.error.AdministratorInPublicCommunityError
import tech.testsys.operation.error.CommunityNotExistsError
import tech.testsys.operation.error.ContestAccessDeniedError
import tech.testsys.operation.error.MissedSupervisorRoleError
import tech.testsys.operation.error.UserHasFixedRoleError
import tech.testsys.operation.error.UserNotExistsError
import tech.testsys.operation.error.getOrThrow
import tech.testsys.operation.util.assertRaises
import tech.testsys.operation.util.publicCommunityConfig
import tech.testsys.operation.util.testAdministrator
import tech.testsys.operation.util.testCommunity
import tech.testsys.operation.util.testCompetition
import tech.testsys.operation.util.testContest
import tech.testsys.operation.util.testDeveloper
import tech.testsys.operation.util.testNewTask
import tech.testsys.operation.util.testObserver
import tech.testsys.operation.util.testStudyClass
import tech.testsys.operation.util.testSupervisor
import tech.testsys.operation.util.testTaskValidationRequest
import java.time.Instant

class SupervisorOperationsTests {
    private val users = mockk<SupervisorUserReadPort>()
    private val communities = mockk<SupervisorCommunityReadPort>()
    private val communityRepository = mockk<CommunityRepository>()
    private val multipleRoleUsers = mockk<MultipleRoleUserRepository>()
    private val userRepository = mockk<UserRepository>()
    private val administratorOperations = mockk<AdministratorOperations>()
    private val contests = mockk<ContestRepository>()
    private val observers = mockk<ObserverRepository>()
    private val publicId = CommunityId(1)
    private val targetId = CommunityId(41)
    private val supervisor = testSupervisor()
    private val member = testDeveloper { data = developerData {} }
    private val pagination = Pagination(0, 20)
    private val operations = SupervisorOperations(
        users, communities, communityRepository, multipleRoleUsers, userRepository,
        publicCommunityConfig(publicId), administratorOperations, contests, observers,
    )

    private fun prepareMember() {
        every { users.findUserById(member.id) } returns member
        every { communityRepository.findById(targetId) } returns testCommunity(targetId.value)
    }

    private fun managerInvite(): CommunityInvite.Manager = managerCommunityInvite {
        id = 51
        createdAt = Instant.EPOCH
        data = communityInviteData {
            code("manager-code", HashAlgorithm.Identity)
            expiresAt = Instant.parse("2027-01-01T00:00:00Z")
        }
    }

    private fun developerInvite(): CommunityInvite.Developer = developerCommunityInvite {
        id = 52
        createdAt = Instant.EPOCH
        data = communityInviteData {
            code("developer-code", HashAlgorithm.Identity)
            expiresAt = Instant.parse("2027-01-01T00:00:00Z")
        }
    }

    @Nested
    inner class ViewUsersTests {
        @Nested
        inner class HappyPathTests {
            @Test
            fun `should list users returned by the supervisor port with last logins`() {
                val login = Instant.parse("2026-01-01T10:00:00Z")
                every { users.findUsers(pagination, UserFilter()) } returns Page(listOf(member), pagination, 1)
                every { userRepository.findLastLogins(listOf(member.id)) } returns mapOf(member.id to login)

                val result = operations.viewUsers(supervisor, pagination).getOrThrow()

                assertEquals(1, result.totalElements)
                assertEquals(member to login, result.content.single())
            }
        }

        @Nested
        inner class RefusalTests {
            @Test
            fun `should reject an actor without the supervisor role before reading users`() {
                assertRaises(MissedSupervisorRoleError) { operations.viewUsers(member, pagination) }

                verify { users wasNot Called }
            }
        }
    }

    @Nested
    inner class ViewUserTests {
        @Nested
        inner class HappyPathTests {
            @Test
            fun `should return a non-supervisor user and their last login`() {
                val login = Instant.parse("2026-01-01T10:00:00Z")
                every { users.findUserById(member.id) } returns member
                every { userRepository.findLastLogins(listOf(member.id)) } returns mapOf(member.id to login)

                val result = operations.viewUser(supervisor, member.id).getOrThrow()

                assertEquals(member to login, result)
                verify(exactly = 1) { users.findUserById(member.id) }
            }
        }

        @Nested
        inner class RefusalTests {
            @Test
            fun `should reject a non-supervisor before reading the user`() {
                assertRaises(MissedSupervisorRoleError) { operations.viewUser(member, member.id) }

                verify { users wasNot Called }
            }

            @Test
            fun `should reject a missing user before reading a last login`() {
                every { users.findUserById(member.id) } returns null

                assertRaises(UserNotExistsError(member.id)) { operations.viewUser(supervisor, member.id) }

                verify { userRepository wasNot Called }
            }
        }
    }

    @Nested
    inner class ViewUserTasksTests {
        @Nested
        inner class HappyPathTests {
            @Test
            fun `should read tasks of a selected user through administrator operations`() {
                val task = testNewTask()
                val request = testTaskValidationRequest()
                val community = testCommunity(targetId.value)
                val expected = listOf(Triple(task, request, listOf(community)))
                every { users.findUserById(member.id) } returns member
                every { userRepository.findLastLogins(listOf(member.id)) } returns emptyMap()
                every { administratorOperations.userTasks(member) } returns expected

                val result = operations.viewUserTasks(supervisor, member.id).getOrThrow()

                assertEquals(expected, result)
                verify(exactly = 1) { administratorOperations.userTasks(member) }
            }
        }

        @Nested
        inner class RefusalTests {
            @Test
            fun `should reject a non-supervisor before reading a user's tasks`() {
                assertRaises(MissedSupervisorRoleError) { operations.viewUserTasks(member, member.id) }

                verify { users wasNot Called }
            }
        }
    }

    @Nested
    inner class ViewUserContestsTests {
        @Nested
        inner class HappyPathTests {
            @Test
            fun `should read contests of a selected user through administrator operations`() {
                val contest = testContest()
                val task = testNewTask()
                val expected = listOf(contest to listOf(task to 3L))
                every { users.findUserById(member.id) } returns member
                every { userRepository.findLastLogins(listOf(member.id)) } returns emptyMap()
                every { administratorOperations.userContests(member) } returns expected

                val result = operations.viewUserContests(supervisor, member.id).getOrThrow()

                assertEquals(expected, result)
                verify(exactly = 1) { administratorOperations.userContests(member) }
            }
        }

        @Nested
        inner class RefusalTests {
            @Test
            fun `should reject a non-supervisor before reading a user's contests`() {
                assertRaises(MissedSupervisorRoleError) { operations.viewUserContests(member, member.id) }

                verify { users wasNot Called }
            }
        }
    }

    @Nested
    inner class ViewUserClassesTests {
        @Nested
        inner class HappyPathTests {
            @Test
            fun `should read classes of a selected user through administrator operations`() {
                val expected = listOf(testStudyClass() to SubmissionCount(3, 2))
                every { users.findUserById(member.id) } returns member
                every { userRepository.findLastLogins(listOf(member.id)) } returns emptyMap()
                every { administratorOperations.userClasses(member) } returns expected

                val result = operations.viewUserClasses(supervisor, member.id).getOrThrow()

                assertEquals(expected, result)
                verify(exactly = 1) { administratorOperations.userClasses(member) }
            }
        }

        @Nested
        inner class RefusalTests {
            @Test
            fun `should reject a non-supervisor before reading a user's classes`() {
                assertRaises(MissedSupervisorRoleError) { operations.viewUserClasses(member, member.id) }

                verify { users wasNot Called }
            }
        }
    }

    @Nested
    inner class ViewUserCompetitionsTests {
        @Nested
        inner class HappyPathTests {
            @Test
            fun `should read competitions of a selected user through administrator operations`() {
                val expected = listOf(testCompetition() to SubmissionCount(5, 2))
                every { users.findUserById(member.id) } returns member
                every { userRepository.findLastLogins(listOf(member.id)) } returns emptyMap()
                every { administratorOperations.userCompetitions(member) } returns expected

                val result = operations.viewUserCompetitions(supervisor, member.id).getOrThrow()

                assertEquals(expected, result)
                verify(exactly = 1) { administratorOperations.userCompetitions(member) }
            }
        }

        @Nested
        inner class RefusalTests {
            @Test
            fun `should reject a non-supervisor before reading a user's competitions`() {
                assertRaises(MissedSupervisorRoleError) { operations.viewUserCompetitions(member, member.id) }

                verify { users wasNot Called }
            }
        }
    }

    @Nested
    inner class ViewUserJudgmentsTests {
        @Nested
        inner class HappyPathTests {
            @Test
            fun `should read judgments of a selected user through administrator operations`() {
                val order = judgmentOrder {
                    id = 25
                    createdAt = Instant.EPOCH
                    data = judgmentOrderData {
                        judge(member.id.value)
                        submission(31)
                        score = 50
                        reason = "Ruling"
                    }
                }
                val expected = listOf(Triple(order, SolutionId(4), 40L))
                every { users.findUserById(member.id) } returns member
                every { userRepository.findLastLogins(listOf(member.id)) } returns emptyMap()
                every { administratorOperations.userJudgments(member) } returns expected

                val result = operations.viewUserJudgments(supervisor, member.id).getOrThrow()

                assertEquals(expected, result)
                verify(exactly = 1) { administratorOperations.userJudgments(member) }
            }
        }

        @Nested
        inner class RefusalTests {
            @Test
            fun `should reject a non-supervisor before reading a user's judgments`() {
                assertRaises(MissedSupervisorRoleError) { operations.viewUserJudgments(member, member.id) }

                verify { users wasNot Called }
            }
        }
    }

    @Nested
    inner class ViewUserAssignedContestsTests {
        @Nested
        inner class HappyPathTests {
            @Test
            fun `should read observer assignments of a selected user through administrator operations`() {
                val observer = testObserver()
                val expected = listOf(testContest() to listOf(testCompetition()))
                every { users.findUserById(observer.id) } returns observer
                every { userRepository.findLastLogins(listOf(observer.id)) } returns emptyMap()
                every { administratorOperations.userAssignedContests(observer) } returns expected

                val result = operations.viewUserAssignedContests(supervisor, observer.id).getOrThrow()

                assertEquals(expected, result)
                verify(exactly = 1) { administratorOperations.userAssignedContests(observer) }
            }
        }

        @Nested
        inner class RefusalTests {
            @Test
            fun `should reject a non-supervisor before reading observer assignments`() {
                val observer = testObserver()

                assertRaises(MissedSupervisorRoleError) { operations.viewUserAssignedContests(member, observer.id) }

                verify { users wasNot Called }
            }
        }
    }

    @Nested
    inner class ViewCommunitiesTests {
        @Nested
        inner class HappyPathTests {
            @Test
            fun `should list every community with its distinct user count`() {
                val community = testCommunity(targetId.value)
                every { communities.findAllCommunities() } returns listOf(community)
                every { users.countUsersInCommunity(targetId) } returns 3L

                val result = operations.viewCommunities(supervisor).getOrThrow()

                assertEquals(listOf(community to 3L), result)
                verify(exactly = 1) { users.countUsersInCommunity(targetId) }
            }
        }

        @Nested
        inner class RefusalTests {
            @Test
            fun `should reject a non-supervisor before listing communities`() {
                assertRaises(MissedSupervisorRoleError) { operations.viewCommunities(member) }

                verify { communities wasNot Called }
            }
        }
    }

    @Nested
    inner class ViewCommunityTests {
        @Nested
        inner class HappyPathTests {
            @Test
            fun `should return a community regardless of its owner`() {
                val community = testCommunity(targetId.value)
                every { communityRepository.findById(targetId) } returns community

                val result = operations.viewCommunity(supervisor, targetId).getOrThrow()

                assertSame(community, result)
            }
        }

        @Nested
        inner class RefusalTests {
            @Test
            fun `should reject a missing community`() {
                every { communityRepository.findById(targetId) } returns null

                assertRaises(CommunityNotExistsError(targetId)) { operations.viewCommunity(supervisor, targetId) }
            }

            @Test
            fun `should reject a non-supervisor before loading a community`() {
                assertRaises(MissedSupervisorRoleError) { operations.viewCommunity(member, targetId) }

                verify { communityRepository wasNot Called }
            }
        }
    }

    @Nested
    inner class AddToCommunityTests {
        @Nested
        inner class HappyPathTests {
            @Test
            fun `should add a held role in the selected community and return the stored user`() {
                prepareMember()
                every { multipleRoleUsers.addCommunityMembership(member.id, targetId, CommunityRole.Developer) } returns member
                every { multipleRoleUsers.findById(member.id) } returns member

                val result = operations.addToCommunity(supervisor, member.id, targetId, CommunityRole.Developer).getOrThrow()

                assertSame(member, result)
                verify(exactly = 1) { multipleRoleUsers.addCommunityMembership(member.id, targetId, CommunityRole.Developer) }
                verify { administratorOperations wasNot Called }
            }

            @ParameterizedTest
            @EnumSource(value = CommunityRole::class, names = ["Manager", "Judge", "Student"])
            fun `should first join the public community when granting a new non-administrator role`(role: CommunityRole) {
                prepareMember()
                every { multipleRoleUsers.addCommunityMembership(member.id, publicId, role) } returns member
                every { multipleRoleUsers.addCommunityMembership(member.id, targetId, role) } returns member
                every { multipleRoleUsers.findById(member.id) } returns member

                val result = operations.addToCommunity(supervisor, member.id, targetId, role).getOrThrow()

                assertSame(member, result)
                verifyOrder {
                    multipleRoleUsers.addCommunityMembership(member.id, publicId, role)
                    multipleRoleUsers.addCommunityMembership(member.id, targetId, role)
                }
            }

            @Test
            fun `should create a first community when granting the administrator role`() {
                val administrator = testAdministrator {}
                prepareMember()
                every { multipleRoleUsers.addCommunityMembership(member.id, targetId, CommunityRole.Administrator) } returns administrator
                every { administratorOperations.createFirstCommunity(administrator) } returns testCommunity(88)
                every { multipleRoleUsers.findById(member.id) } returns administrator

                val result = operations.addToCommunity(supervisor, member.id, targetId, CommunityRole.Administrator).getOrThrow()

                assertSame(administrator, result)
                verify(exactly = 1) { administratorOperations.createFirstCommunity(administrator) }
            }
        }

        @Nested
        inner class RefusalTests {
            @Test
            fun `should reject a non-supervisor before reading the user`() {
                assertRaises(MissedSupervisorRoleError) {
                    operations.addToCommunity(member, member.id, targetId, CommunityRole.Developer)
                }

                verify { users wasNot Called }
            }

            @Test
            fun `should reject a missing user before loading a community`() {
                every { users.findUserById(member.id) } returns null

                assertRaises(UserNotExistsError(member.id)) {
                    operations.addToCommunity(supervisor, member.id, targetId, CommunityRole.Developer)
                }

                verify { communityRepository wasNot Called }
            }

            @Test
            fun `should reject a fixed-role user`() {
                val observer = testObserver()
                every { users.findUserById(observer.id) } returns observer
                every { communityRepository.findById(targetId) } returns testCommunity(targetId.value)

                assertRaises(UserHasFixedRoleError(observer.id)) {
                    operations.addToCommunity(supervisor, observer.id, targetId, CommunityRole.Student)
                }

                verify { multipleRoleUsers wasNot Called }
            }

            @Test
            fun `should reject a missing community before changing membership`() {
                every { users.findUserById(member.id) } returns member
                every { communityRepository.findById(targetId) } returns null

                assertRaises(CommunityNotExistsError(targetId)) {
                    operations.addToCommunity(supervisor, member.id, targetId, CommunityRole.Developer)
                }

                verify { multipleRoleUsers wasNot Called }
            }

            @Test
            fun `should reject assigning an administrator in the public community`() {
                every { users.findUserById(member.id) } returns member
                every { communityRepository.findById(publicId) } returns testCommunity(publicId.value)

                assertRaises(AdministratorInPublicCommunityError(publicId)) {
                    operations.addToCommunity(supervisor, member.id, publicId, CommunityRole.Administrator)
                }

                verify { multipleRoleUsers wasNot Called }
            }
        }

        @Nested
        inner class InvariantTests {
            @Test
            fun `should return an unchanged user when already a member of the community in the selected role`() {
                val existing = testDeveloper {
                    memberOf(listOf(targetId.value))
                    data = developerData {}
                }
                every { users.findUserById(existing.id) } returns existing
                every { communityRepository.findById(targetId) } returns testCommunity(targetId.value)

                val result = operations.addToCommunity(supervisor, existing.id, targetId, CommunityRole.Developer).getOrThrow()

                assertSame(existing, result)
                assertEquals(EntityVersion(0), result.version)
                assertEquals(listOf(targetId), result.data.roles.single().memberOf.ids)
                verify { multipleRoleUsers wasNot Called }
                verify { administratorOperations wasNot Called }
            }
        }
    }

    @Nested
    inner class GrantAdministratorTests {
        @Nested
        inner class HappyPathTests {
            @Test
            fun `should create a first community and return the stored user when granting the administrator role`() {
                val administrator = testAdministrator {}
                every { users.findUserById(member.id) } returns member
                every { administratorOperations.createFirstCommunity(member) } returns testCommunity(88)
                every { multipleRoleUsers.findById(member.id) } returns administrator

                val result = operations.grantAdministrator(supervisor, member.id).getOrThrow()

                assertSame(administrator, result)
                verify(exactly = 1) { administratorOperations.createFirstCommunity(member) }
                verify(exactly = 1) { multipleRoleUsers.findById(member.id) }
            }
        }

        @Nested
        inner class RefusalTests {
            @Test
            fun `should reject a non-supervisor before reading the user`() {
                assertRaises(MissedSupervisorRoleError) {
                    operations.grantAdministrator(member, member.id)
                }

                verify { users wasNot Called }
                verify { administratorOperations wasNot Called }
            }

            @Test
            fun `should reject a missing user without creating a community`() {
                every { users.findUserById(member.id) } returns null

                assertRaises(UserNotExistsError(member.id)) {
                    operations.grantAdministrator(supervisor, member.id)
                }

                verify { multipleRoleUsers wasNot Called }
                verify { administratorOperations wasNot Called }
            }

            @Test
            fun `should reject a fixed-role user without creating a community`() {
                val observer = testObserver()
                every { users.findUserById(observer.id) } returns observer

                assertRaises(UserHasFixedRoleError(observer.id)) {
                    operations.grantAdministrator(supervisor, observer.id)
                }

                verify { multipleRoleUsers wasNot Called }
                verify { administratorOperations wasNot Called }
            }
        }

        @Nested
        inner class InvariantTests {
            @Test
            fun `should return an unchanged user when already holding the administrator role`() {
                val administrator = testAdministrator { memberOf(listOf(targetId.value)) }
                every { users.findUserById(administrator.id) } returns administrator

                val result = operations.grantAdministrator(supervisor, administrator.id).getOrThrow()

                assertSame(administrator, result)
                assertEquals(EntityVersion(0), result.version)
                verify { multipleRoleUsers wasNot Called }
                verify { communityRepository wasNot Called }
                verify { administratorOperations wasNot Called }
            }

            @Test
            fun `should not add a membership in the public or another community when granting the administrator role`() {
                every { users.findUserById(member.id) } returns member
                every { administratorOperations.createFirstCommunity(member) } returns testCommunity(88)
                every { multipleRoleUsers.findById(member.id) } returns testAdministrator {}

                operations.grantAdministrator(supervisor, member.id).getOrThrow()

                verify(exactly = 0) { multipleRoleUsers.addCommunityMembership(any(), any(), any()) }
                verify { communityRepository wasNot Called }
            }
        }
    }

    @Nested
    inner class EditCommunityTests {
        @Nested
        inner class HappyPathTests {
            @Test
            fun `should edit a community owned by another user`() {
                val community = testCommunity(targetId.value)
                every { communityRepository.findById(targetId) } returns community
                every { administratorOperations.updateCommunity(community, "Renamed", "Details") } returns community

                val result = operations.editCommunity(supervisor, targetId, "Renamed", "Details").getOrThrow()

                assertSame(community, result)
                verify(exactly = 1) { administratorOperations.updateCommunity(community, "Renamed", "Details") }
            }
        }

        @Nested
        inner class RefusalTests {
            @Test
            fun `should reject a non-supervisor before loading the community`() {
                assertRaises(MissedSupervisorRoleError) { operations.editCommunity(member, targetId, "Name", "Details") }

                verify { communityRepository wasNot Called }
            }
        }
    }

    @Nested
    inner class RemoveFromCommunityTests {
        @Nested
        inner class HappyPathTests {
            @Test
            fun `should remove a developer membership in another user's community`() {
                val existing = testDeveloper {
                    memberOf(listOf(targetId.value))
                    data = developerData {}
                }
                val stored = testDeveloper { data = developerData {} }
                every { multipleRoleUsers.findById(existing.id) } returns existing
                every { communityRepository.findById(targetId) } returns testCommunity(targetId.value)
                every { multipleRoleUsers.removeCommunityMembership(existing.id, targetId, CommunityRole.Developer) } returns stored

                val result = operations.removeFromCommunity(supervisor, existing.id, targetId, CommunityRole.Developer).getOrThrow()

                assertSame(stored, result)
                verify(exactly = 1) { multipleRoleUsers.removeCommunityMembership(existing.id, targetId, CommunityRole.Developer) }
            }
        }

        @Nested
        inner class RefusalTests {
            @Test
            fun `should reject a non-supervisor before reading membership`() {
                assertRaises(MissedSupervisorRoleError) {
                    operations.removeFromCommunity(member, member.id, targetId, CommunityRole.Developer)
                }

                verify { multipleRoleUsers wasNot Called }
            }
        }
    }

    @Nested
    inner class CreateObserverTests {
        @Nested
        inner class HappyPathTests {
            @Test
            fun `should save an observer assigned to a contest shared with the community`() {
                val contest = testContest { sharedTo = mutableListOf(targetId) }
                val saved = testObserver()
                val data = slot<ObserverData>()
                every { communityRepository.findById(targetId) } returns testCommunity(targetId.value)
                every { contests.findByIds(listOf(contest.id)) } returns listOf(contest)
                every { observers.save(capture(data)) } returns saved

                val result = operations.createObserver(supervisor, targetId, "New observer", setOf(contest.id)).getOrThrow()

                assertSame(saved, result)
                assertEquals("New observer", data.captured.name)
                assertEquals(targetId, data.captured.community.id)
                assertEquals(listOf(contest.id), data.captured.contests.ids)
            }

            @Test
            fun `should save an observer assigned to a contest shared only with the public community`() {
                val contest = testContest { sharedTo = mutableListOf(publicId) }
                val data = slot<ObserverData>()
                every { communityRepository.findById(targetId) } returns testCommunity(targetId.value)
                every { contests.findByIds(listOf(contest.id)) } returns listOf(contest)
                every { observers.save(capture(data)) } returns testObserver()

                operations.createObserver(supervisor, targetId, "New observer", setOf(contest.id)).getOrThrow()

                assertEquals(targetId, data.captured.community.id)
                assertEquals(listOf(contest.id), data.captured.contests.ids)
            }
        }

        @Nested
        inner class RefusalTests {
            @Test
            fun `should reject a contest shared only with another community`() {
                val contest = testContest { sharedTo = mutableListOf(CommunityId(99)) }
                every { communityRepository.findById(targetId) } returns testCommunity(targetId.value)
                every { contests.findByIds(listOf(contest.id)) } returns listOf(contest)

                assertRaises(ContestAccessDeniedError(contest.id)) {
                    operations.createObserver(supervisor, targetId, "Observer", setOf(contest.id))
                }

                verify { observers wasNot Called }
            }

            @Test
            fun `should reject a non-supervisor before loading a community`() {
                assertRaises(MissedSupervisorRoleError) {
                    operations.createObserver(member, targetId, "Observer", setOf(ContestId(19)))
                }

                verify { communityRepository wasNot Called }
            }
        }
    }

    @Nested
    inner class ViewCommunityContestsTests {
        @Nested
        inner class HappyPathTests {
            @Test
            fun `should list contests shared with a selected community or the public community`() {
                val contest = testContest { sharedTo = mutableListOf(targetId) }
                every { communityRepository.findById(targetId) } returns testCommunity(targetId.value)
                every { contests.findSharedTo(setOf(targetId, publicId), pagination, ContestFilter(name = "Round")) } returns
                    Page(listOf(contest), pagination, 1)

                val result = operations.viewCommunityContests(supervisor, targetId, pagination, "Round").getOrThrow()

                assertEquals(listOf(contest), result.content)
                assertEquals(1, result.totalElements)
            }
        }

        @Nested
        inner class RefusalTests {
            @Test
            fun `should reject a non-supervisor before searching contests`() {
                assertRaises(MissedSupervisorRoleError) { operations.viewCommunityContests(member, targetId, pagination) }

                verify { contests wasNot Called }
            }
        }
    }

    @Nested
    inner class ViewCommunityInvitesTests {
        @Nested
        inner class HappyPathTests {
            @Test
            fun `should read both invites of a selected community`() {
                val community = testCommunity(targetId.value)
                val expected = listOf(managerInvite(), developerInvite())
                every { communityRepository.findById(targetId) } returns community
                every { administratorOperations.communityInvites(community) } returns expected

                val result = operations.viewCommunityInvites(supervisor, targetId).getOrThrow()

                assertEquals(expected, result)
                verify(exactly = 1) { administratorOperations.communityInvites(community) }
            }
        }

        @Nested
        inner class RefusalTests {
            @Test
            fun `should reject a non-supervisor before reading community invites`() {
                assertRaises(MissedSupervisorRoleError) { operations.viewCommunityInvites(member, targetId) }

                verify { administratorOperations wasNot Called }
            }
        }
    }

    @Nested
    inner class CreateCommunityInviteTests {
        @Nested
        inner class HappyPathTests {
            @Test
            fun `should replace an invite of a selected community`() {
                val community = testCommunity(targetId.value)
                val invite = managerInvite()
                every { communityRepository.findById(targetId) } returns community
                every { administratorOperations.replaceCommunityInvite(community, CommunityInvite.Kind.Manager) } returns invite

                val result = operations.createCommunityInvite(supervisor, targetId, CommunityInvite.Kind.Manager).getOrThrow()

                assertSame(invite, result)
                verify(exactly = 1) { administratorOperations.replaceCommunityInvite(community, CommunityInvite.Kind.Manager) }
            }
        }

        @Nested
        inner class RefusalTests {
            @Test
            fun `should reject a non-supervisor before replacing a community invite`() {
                assertRaises(MissedSupervisorRoleError) {
                    operations.createCommunityInvite(member, targetId, CommunityInvite.Kind.Manager)
                }

                verify { administratorOperations wasNot Called }
            }
        }
    }

    @Nested
    inner class ExtendCommunityInviteTests {
        @Nested
        inner class HappyPathTests {
            @Test
            fun `should extend an invite of a selected community`() {
                val community = testCommunity(targetId.value)
                val invite = managerInvite()
                every { communityRepository.findById(targetId) } returns community
                every { administratorOperations.extendCommunityInvite(community, CommunityInvite.Kind.Manager) } returns invite

                val result = operations.extendCommunityInvite(supervisor, targetId, CommunityInvite.Kind.Manager).getOrThrow()

                assertSame(invite, result)
                verify(exactly = 1) { administratorOperations.extendCommunityInvite(community, CommunityInvite.Kind.Manager) }
            }
        }

        @Nested
        inner class RefusalTests {
            @Test
            fun `should reject a non-supervisor before extending a community invite`() {
                assertRaises(MissedSupervisorRoleError) {
                    operations.extendCommunityInvite(member, targetId, CommunityInvite.Kind.Manager)
                }

                verify { administratorOperations wasNot Called }
            }
        }
    }

    @Nested
    inner class RefreshCommunityInviteTests {
        @Nested
        inner class HappyPathTests {
            @Test
            fun `should refresh an invite of a selected community`() {
                val community = testCommunity(targetId.value)
                val invite = managerInvite()
                every { communityRepository.findById(targetId) } returns community
                every { administratorOperations.refreshCommunityInvite(community, CommunityInvite.Kind.Manager) } returns invite

                val result = operations.refreshCommunityInvite(supervisor, targetId, CommunityInvite.Kind.Manager).getOrThrow()

                assertSame(invite, result)
                verify(exactly = 1) { administratorOperations.refreshCommunityInvite(community, CommunityInvite.Kind.Manager) }
            }
        }

        @Nested
        inner class RefusalTests {
            @Test
            fun `should reject a non-supervisor before refreshing a community invite`() {
                assertRaises(MissedSupervisorRoleError) {
                    operations.refreshCommunityInvite(member, targetId, CommunityInvite.Kind.Manager)
                }

                verify { administratorOperations wasNot Called }
            }
        }
    }
}
