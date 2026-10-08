package tech.testsys.operation.user

import io.mockk.Called
import io.mockk.CapturingSlot
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.UUID
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EmptySource
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.ValueSource
import tech.testsys.domain.builder.api.community
import tech.testsys.domain.builder.api.communityData
import tech.testsys.domain.builder.api.communityInviteData
import tech.testsys.domain.builder.api.contest
import tech.testsys.domain.builder.api.developerCommunityInvite
import tech.testsys.domain.builder.api.developerData
import tech.testsys.domain.builder.api.judgeData
import tech.testsys.domain.builder.api.managerCommunityInvite
import tech.testsys.domain.builder.api.managerData
import tech.testsys.domain.builder.api.multipleRoleUser
import tech.testsys.domain.builder.api.multipleRoleUserData
import tech.testsys.domain.builder.api.observer
import tech.testsys.domain.builder.api.observerData
import tech.testsys.domain.builder.api.studentData
import tech.testsys.domain.builder.user.MultipleRoleUserDataBuilder
import tech.testsys.domain.contract.persistence.ContestFilter
import tech.testsys.domain.contract.persistence.Page
import tech.testsys.domain.contract.persistence.Pagination
import tech.testsys.domain.contract.persistence.Sort
import tech.testsys.domain.contract.persistence.UserFilter
import tech.testsys.domain.contract.persistence.repository.CommunityRepository
import tech.testsys.domain.contract.persistence.repository.ContestRepository
import tech.testsys.domain.contract.persistence.repository.DeveloperCommunityInviteRepository
import tech.testsys.domain.contract.persistence.repository.ManagerCommunityInviteRepository
import tech.testsys.domain.contract.persistence.repository.MultipleRoleUserRepository
import tech.testsys.domain.contract.persistence.repository.ObserverRepository
import tech.testsys.domain.contract.persistence.repository.UserRepository
import tech.testsys.domain.model.EntityVersion
import tech.testsys.domain.model.LazyEntity
import tech.testsys.domain.model.group.Community
import tech.testsys.domain.model.group.CommunityData
import tech.testsys.domain.model.group.CommunityId
import tech.testsys.domain.model.group.CommunityInvite
import tech.testsys.domain.model.group.CommunityInviteData
import tech.testsys.domain.model.group.CommunityInviteId
import tech.testsys.domain.model.task.Contest
import tech.testsys.domain.model.task.ContestId
import tech.testsys.domain.model.user.CommunityRole
import tech.testsys.domain.model.user.HashAlgorithm
import tech.testsys.domain.model.user.MultipleRoleUser
import tech.testsys.domain.model.user.MultipleRoleUserId
import tech.testsys.domain.model.user.Observer
import tech.testsys.domain.model.user.ObserverData
import tech.testsys.domain.model.user.SingleRoleUserId
import tech.testsys.domain.model.user.User
import tech.testsys.domain.model.user.UserId
import tech.testsys.operation.config.CommunityInviteConfig
import tech.testsys.operation.error.CommunityAccessDeniedError
import tech.testsys.operation.error.CommunityNameBlankError
import tech.testsys.operation.error.CommunityNameTooLongError
import tech.testsys.operation.error.CommunityNotExistsError
import tech.testsys.operation.error.ContestAccessDeniedError
import tech.testsys.operation.error.ContestNotExistsError
import tech.testsys.operation.error.MissedAdministratorRoleError
import tech.testsys.operation.error.ObserverContestsEmptyError
import tech.testsys.operation.error.ObserverNameBlankError
import tech.testsys.operation.error.ObserverNameTooLongError
import tech.testsys.operation.error.UserAccessDeniedError
import tech.testsys.operation.error.UserHasFixedRoleError
import tech.testsys.operation.error.UserNotExistsError
import tech.testsys.operation.error.getOrThrow
import tech.testsys.operation.util.assertRaises
import tech.testsys.operation.util.testAdministrator
import tech.testsys.operation.util.testContest
import tech.testsys.operation.util.testManager
import tech.testsys.operation.util.testMultipleRoleUser

class AdministratorOperationsTests {

    private val contests = mockk<ContestRepository>()
    private val observers = mockk<ObserverRepository>()
    private val communities = mockk<CommunityRepository>()
    private val managerInvites = mockk<ManagerCommunityInviteRepository>()
    private val developerInvites = mockk<DeveloperCommunityInviteRepository>()
    private val config = mockk<CommunityInviteConfig>()
    private val clock = mockk<Clock>()
    private val userRepository = mockk<UserRepository>()
    private val multipleRoleUsers = mockk<MultipleRoleUserRepository>()
    private val operations = AdministratorOperations(
        communityRepository = communities,
        managerInviteRepository = managerInvites,
        developerInviteRepository = developerInvites,
        communityInviteConfig = config,
        clock = clock,
        userRepository = userRepository,
        contestRepository = contests,
        observerRepository = observers,
        multipleRoleUserRepository = multipleRoleUsers,
    )
    private val administrator = testAdministrator {}
    private val nonAdministrator = testMultipleRoleUser {
        roles {
            developer { data = developerData {} }
            manager { data = managerData {} }
            judge { data = judgeData {} }
        }
    }
    private val communityId = CommunityId(41)
    private val now = Instant.parse("2026-01-01T10:00:00Z")

    @Nested
    inner class CreateObserverTests {

        private val contestId = ContestId(19)
        private val storedContests = mutableListOf<Contest>()
        private val otherContestId = ContestId(20)

        @Nested
        inner class HappyPathTests {

            @ParameterizedTest
            @ValueSource(strings = ["x", "  Mixed Case  "])
            fun `should save an observer with the exact name the community and a new Identity UUID access code`(name: String) {
                prepareCreation()
                val saved = captureSave()

                val result = create(name = name).getOrThrow()

                assertEquals(name, result.data.name)
                assertEquals(communityId, result.data.community.id)
                assertEquals(HashAlgorithm.Identity, result.data.accessTokenHash.algorithm)
                assertEquals(result.data.accessTokenHash.value, UUID.fromString(result.data.accessTokenHash.value).toString())
                assertSame(saved.captured, result.data)
            }

            @ParameterizedTest
            @ValueSource(strings = ["a", "😀"])
            fun `should accept a name of exactly 255 Unicode code points`(character: String) {
                prepareCreation()
                val saved = captureSave()
                val name = character.repeat(255)

                val result = create(name = name).getOrThrow()

                assertEquals(name, result.data.name)
                assertEquals(name, saved.captured.name)
            }

            @Test
            fun `should accept a contest owned by another user if it is shared to the community`() {
                prepareCreation()
                storedContests.clear()
                storedContests += testContest {
                    owner(99)
                    sharedTo = mutableListOf(communityId)
                }
                val saved = captureSave()

                create().getOrThrow()

                assertEquals(listOf(contestId), saved.captured.contests.ids)
            }

            @Test
            fun `should return the observer persisted by the repository`() {
                prepareCreation()
                val persisted = observer {
                    id = 73
                    createdAt = Instant.EPOCH
                    data = observerData {
                        community(communityId.value)
                        name = "Persisted"
                        contests(listOf(contestId.value))
                        accessToken("stored", HashAlgorithm.Identity)
                    }
                }
                every { observers.save(any<ObserverData>()) } returns persisted

                val result = create().getOrThrow()

                assertSame(persisted, result)
            }
        }

        @Nested
        inner class RefusalTests {

            @Test
            fun `should reject a user without administrator role without reading or saving`() {
                assertRaises(MissedAdministratorRoleError) { create(user = testMultipleRoleUser {}) }

                verify { listOf(communities, contests, observers) wasNot Called }
            }

            @Test
            fun `should raise MissedAdministratorRoleError if user has other roles but not Administrator`() {
                assertRaises(MissedAdministratorRoleError) { create(user = nonAdministrator) }

                verify { listOf(communities, contests, observers) wasNot Called }
            }

            @Test
            fun `should reject a nonexistent community without saving`() {
                every { communities.findById(communityId) } returns null

                assertRaises(CommunityNotExistsError(communityId)) { create() }

                verify { listOf(contests, observers) wasNot Called }
            }

            @Test
            fun `should reject any nonexistent requested contest before checking ownership`() {
                prepareCreation(ownerId = 99)

                assertRaises(ContestNotExistsError(otherContestId)) { create(ids = setOf(contestId, otherContestId)) }

                verify { observers wasNot Called }
            }

            @Test
            fun `should reject a community created by another user`() {
                prepareCreation(ownerId = 99)

                assertRaises(CommunityAccessDeniedError(communityId)) { create() }

                verify { observers wasNot Called }
            }

            @Test
            fun `should reject a contest shared only to another community`() {
                prepareCreation(shared = listOf(CommunityId(99)))

                assertRaises(ContestAccessDeniedError(contestId)) { create() }

                verify { observers wasNot Called }
            }

            @Test
            fun `should reject an unshared contest even when the administrator owns it`() {
                prepareCreation(shared = emptyList())

                assertRaises(ContestAccessDeniedError(contestId)) { create() }

                verify { observers wasNot Called }
            }

            @Test
            fun `should raise ContestAccessDeniedError if only one of several contests is not shared to the community`() {
                prepareCreation()
                storedContests += contestWithId(otherContestId, shared = listOf(CommunityId(99)))

                assertRaises(ContestAccessDeniedError(otherContestId)) { create(ids = setOf(contestId, otherContestId)) }

                verify { observers wasNot Called }
            }

            @ParameterizedTest
            @EmptySource
            @ValueSource(strings = [" ", "\t\r\n", " "])
            fun `should reject empty and blank names without saving`(name: String) {
                prepareCreation()

                assertRaises(ObserverNameBlankError) { create(name = name) }

                verify { observers wasNot Called }
            }

            @ParameterizedTest
            @ValueSource(strings = ["a", "😀"])
            fun `should reject names with 256 Unicode code points`(character: String) {
                prepareCreation()
                val name = character.repeat(256)

                assertRaises(ObserverNameTooLongError(name)) { create(name = name) }

                verify { observers wasNot Called }
            }

            @Test
            fun `should reject an empty contest set without saving`() {
                every { communities.findById(communityId) } returns testCommunity()

                assertRaises(ObserverContestsEmptyError) { create(ids = emptySet()) }

                verify { listOf(contests, observers) wasNot Called }
            }

            @Test
            fun `should reject the first nonexistent contest in request order when several are missing`() {
                prepareCreation()

                assertRaises(ContestNotExistsError(ContestId(21))) { create(ids = setOf(ContestId(21), contestId, ContestId(20))) }

                verify { observers wasNot Called }
            }

            @Test
            fun `should reject the first unshared contest in request order when contests are answered out of order`() {
                prepareCreation(shared = listOf(CommunityId(99)))
                storedContests += storedContest(id = 20, shared = emptyList())

                assertRaises(ContestAccessDeniedError(contestId)) { create(ids = setOf(contestId, ContestId(20))) }

                verify { observers wasNot Called }
            }
        }

        @Nested
        inner class InvariantTests {

            @Test
            fun `should create separate observers with distinct codes for repeated identical input`() {
                prepareCreation()
                val saved = mutableListOf<ObserverData>()
                every { observers.save(capture(saved)) } answers {
                    observer {
                        id = 70L + saved.size
                        createdAt = Instant.EPOCH
                        data = firstArg()
                    }
                }

                val first = create().getOrThrow()
                val second = create().getOrThrow()

                assertNotEquals(first.id, second.id)
                assertNotEquals(first.data.accessTokenHash, second.data.accessTokenHash)
                assertEquals(first.data.name, second.data.name)
                assertEquals(first.data.contests.ids, second.data.contests.ids)
                verify(exactly = 2) { observers.save(any<ObserverData>()) }
            }

            @Test
            fun `should assign exactly the requested set of contests to the observer`() {
                prepareCreation()
                storedContests += contestWithId(otherContestId, shared = listOf(communityId, CommunityId(99)))
                val saved = captureSave()

                create(ids = setOf(contestId, otherContestId)).getOrThrow()

                assertEquals(listOf(contestId, otherContestId), saved.captured.contests.ids)
            }

            @Test
            fun `should create a new observer with a single save and no other observer storage access`() {
                prepareCreation()
                captureSave()

                create().getOrThrow()

                verify(exactly = 1) { observers.save(any<ObserverData>()) }
                confirmVerified(observers)
            }

            @Test
            fun `should not update the community or the contests when creating an observer`() {
                prepareCreation()
                captureSave()

                create().getOrThrow()

                verify(exactly = 0) { communities.update(any<Community>()) }
                verify(exactly = 0) { contests.update(any<Contest>()) }
            }
        }

        @Nested
        inner class ModuleRuleTests {

            @Test
            fun `should propagate technical community repository exceptions`() {
                val failure = IllegalStateException("Community storage unavailable")
                every { communities.findById(communityId) } throws failure

                val actual = assertThrows(IllegalStateException::class.java) { create() }

                assertSame(failure, actual)
                verify { observers wasNot Called }
            }

            @Test
            fun `should propagate technical contest repository exceptions`() {
                prepareCreation()
                val failure = IllegalStateException("Contest storage unavailable")
                every { contests.findByIds(any()) } throws failure

                val actual = assertThrows(IllegalStateException::class.java) { create() }

                assertSame(failure, actual)
                verify { observers wasNot Called }
            }

            @Test
            fun `should propagate technical save exceptions without retrying`() {
                prepareCreation()
                val failure = IllegalStateException("Code collision")
                every { observers.save(any<ObserverData>()) } throws failure

                val actual = assertThrows(IllegalStateException::class.java) { create() }

                assertSame(failure, actual)
                verify(exactly = 1) { observers.save(any<ObserverData>()) }
            }

            @Test
            fun `should find all requested contests with one repository call`() {
                prepareCreation()
                storedContests += storedContest(id = 20, shared = listOf(communityId))
                captureSave()

                create(ids = setOf(contestId, ContestId(20))).getOrThrow()

                verify(exactly = 1) { contests.findByIds(listOf(contestId, ContestId(20))) }
                verify(exactly = 0) { contests.findById(any()) }
            }
        }

        private fun create(user: MultipleRoleUser = administrator, name: String = "Observer", ids: Set<ContestId> = setOf(contestId)) =
            operations.createObserver(user = user, communityId = communityId, observerName = name, contestIds = ids)

        private fun prepareCreation(ownerId: Long = administrator.id.value, shared: List<CommunityId> = listOf(communityId)) {
            every { communities.findById(communityId) } returns testCommunity(ownerId)
            storedContests += testContest {
                owner(administrator.id.value)
                sharedTo = shared.toMutableList()
            }
            every { contests.findByIds(any()) } answers {
                firstArg<List<ContestId>>().reversed().mapNotNull { id -> storedContests.find { contest -> contest.id == id } }
            }
        }

        private fun contestWithId(targetId: ContestId, shared: List<CommunityId>): Contest = contest {
            id = targetId.value
            createdAt = Instant.EPOCH
            data = testContest {
                owner(administrator.id.value)
                sharedTo = shared.toMutableList()
            }.data
        }

        private fun captureSave(): CapturingSlot<ObserverData> {
            val saved = slot<ObserverData>()
            every { observers.save(capture(saved)) } answers {
                observer {
                    id = 73
                    createdAt = Instant.EPOCH
                    version = EntityVersion(1)
                    data = firstArg()
                }
            }
            return saved
        }

        private fun storedContest(id: Long, shared: List<CommunityId>): Contest = contest {
            this.id = id
            createdAt = Instant.EPOCH
            data = testContest { sharedTo = shared.toMutableList() }.data
        }
    }

    @Nested
    inner class ViewUsersTests {

        private val pagination = Pagination(page = 0, size = 10)

        @Nested
        inner class HappyPathTests {

            @Test
            fun `should allow an Administrator who also has other roles`() {
                val user = testMultipleRoleUser {
                    roles {
                        developer { data = developerData {} }
                        administrator {}
                    }
                }
                val expected = Page<User<*>>(content = listOf(user), pagination = pagination, totalElements = 1)
                every { userRepository.findAvailableToAdministrator(administratorId = user.id, pagination = pagination) } returns expected
                every { userRepository.findLastLogins(expected.content.map { it.id }) } returns emptyMap()

                val actual = operations.viewUsers(user = user, pagination = pagination).getOrThrow()

                assertEquals(
                    Page<Pair<User<*>, Instant?>>(
                        content = expected.content.map { it to null },
                        pagination = expected.pagination,
                        totalElements = expected.totalElements,
                    ),
                    actual,
                )
            }

            @Test
            fun `should forward the administrator id pagination and every filter unchanged`() {
                val request =
                    Pagination(page = 3, size = 2, sort = Sort(listOf(Sort.Order(field = "name", direction = Sort.Direction.DESC))))
                val filter = UserFilter(
                    name = "  Alpha%_  ",
                    roles = setOf(UserFilter.Role.OBSERVER, UserFilter.Role.ADMINISTRATOR),
                    communityId = CommunityId(5),
                )
                val expected = Page<User<*>>(content = emptyList(), pagination = request, totalElements = 7)
                every {
                    userRepository.findAvailableToAdministrator(administratorId = administrator.id, pagination = request, filter = filter)
                } returns expected
                every { userRepository.findLastLogins(expected.content.map { it.id }) } returns emptyMap()

                val actual = operations.viewUsers(user = administrator, pagination = request, filter = filter).getOrThrow()

                assertEquals(
                    Page<Pair<User<*>, Instant?>>(
                        content = expected.content.map { it to null },
                        pagination = expected.pagination,
                        totalElements = expected.totalElements,
                    ),
                    actual,
                )
            }

            @Test
            fun `should return users of different kinds in the page order of the port with their last logins`() {
                val observer = testObserver()
                val found =
                    Page<User<*>>(content = listOf(observer, administrator), pagination = pagination, totalElements = 2)
                every {
                    userRepository.findAvailableToAdministrator(administratorId = administrator.id, pagination = pagination)
                } returns found
                every { userRepository.findLastLogins(listOf(observer.id, administrator.id)) } returns mapOf(observer.id to LOGGED_IN_AT)

                val actual = operations.viewUsers(user = administrator, pagination = pagination).getOrThrow()

                assertSame(observer, actual.content.first().first)
                assertSame(administrator, actual.content.last().first)
                assertEquals(listOf(LOGGED_IN_AT, null), actual.content.map { (_, lastLogin) -> lastLogin })
            }
        }

        @Nested
        inner class RefusalTests {

            @Test
            fun `should raise MissedAdministratorRoleError before reading storage if user is not an Administrator`() {
                val user = testManager { data = managerData {} }

                assertRaises(MissedAdministratorRoleError) { operations.viewUsers(user = user, pagination = pagination) }

                verify(exactly = 0) { userRepository.findAvailableToAdministrator(any(), any(), any()) }
            }
        }

        @Nested
        inner class InvariantTests {

            @Test
            fun `should not touch writable storage when viewing users`() {
                every {
                    userRepository.findAvailableToAdministrator(administratorId = administrator.id, pagination = pagination)
                } returns Page(content = emptyList(), pagination = pagination, totalElements = 0)
                every { userRepository.findLastLogins(emptyList()) } returns emptyMap()

                operations.viewUsers(user = administrator, pagination = pagination).getOrThrow()

                verify { listOf(communities, managerInvites, developerInvites, observers, contests, multipleRoleUsers) wasNot Called }
            }
        }

        @Nested
        inner class ModuleRuleTests {

            @Test
            fun `should propagate a technical storage exception when listing users`() {
                val failure = IllegalStateException("User storage unavailable")
                every {
                    userRepository.findAvailableToAdministrator(administratorId = administrator.id, pagination = pagination)
                } throws failure

                val actual = assertThrows(IllegalStateException::class.java) {
                    operations.viewUsers(user = administrator, pagination = pagination)
                }

                assertSame(failure, actual)
            }
        }

        private fun testObserver(): Observer = observer {
            id = 31
            createdAt = Instant.EPOCH
            data = observerData {
                accessToken("observer", algorithm = HashAlgorithm.Identity)
                name = "Observer"
                community(5)
            }
        }
    }

    @Nested
    inner class ViewUserTests {

        private val userId = MultipleRoleUserId(42)

        @Nested
        inner class HappyPathTests {

            @Test
            fun `should return an available multiple-role user with all roles as returned by the port`() {
                val expected = multipleRoleUser {
                    id = userId.value
                    createdAt = Instant.EPOCH
                    data = multipleRoleUserData {
                        accessToken("member", algorithm = HashAlgorithm.Identity)
                        name = "Member"
                        email = "member@example.com"
                        roles {
                            developer {
                                memberOf(listOf(5L))
                                data = developerData {}
                            }
                            judge {
                                memberOf(listOf(7L))
                                data = judgeData {}
                            }
                        }
                    }
                }
                every {
                    userRepository.findAvailableToAdministratorById(administratorId = administrator.id, userId = userId)
                } returns expected
                every { userRepository.findLastLogins(listOf(expected.id)) } returns emptyMap()

                val (actual, lastLogin) = operations.viewUser(user = administrator, userId = userId).getOrThrow()

                assertSame(expected, actual)
                assertNull(lastLogin)
            }

            @Test
            fun `should return an available observer requested by its single-role id`() {
                val expected = observer {
                    id = 31
                    createdAt = Instant.EPOCH
                    data = observerData {
                        accessToken("observer", algorithm = HashAlgorithm.Identity)
                        name = "Observer"
                        community(5)
                    }
                }
                every {
                    userRepository.findAvailableToAdministratorById(administratorId = administrator.id, userId = SingleRoleUserId(31))
                } returns expected
                every { userRepository.findLastLogins(listOf(expected.id)) } returns mapOf(expected.id to LOGGED_IN_AT)

                val (actual, lastLogin) = operations.viewUser(user = administrator, userId = SingleRoleUserId(31)).getOrThrow()

                assertSame(expected, actual)
                assertEquals(LOGGED_IN_AT, lastLogin)
            }

            @Test
            fun `should return the administrator themself with other roles`() {
                val user = testMultipleRoleUser {
                    roles {
                        developer { data = developerData {} }
                        administrator {}
                    }
                }
                every { userRepository.findAvailableToAdministratorById(administratorId = user.id, userId = user.id) } returns user
                every { userRepository.findLastLogins(listOf(user.id)) } returns emptyMap()

                val (actual, lastLogin) = operations.viewUser(user = user, userId = user.id).getOrThrow()

                assertSame(user, actual)
                assertNull(lastLogin)
            }
        }

        @Nested
        inner class RefusalTests {

            @Test
            fun `should raise MissedAdministratorRoleError before reading storage if user is not an Administrator`() {
                val user = testManager { data = managerData {} }

                assertRaises(MissedAdministratorRoleError) { operations.viewUser(user = user, userId = userId) }

                verify(exactly = 0) { userRepository.findAvailableToAdministratorById(any(), any()) }
                verify(exactly = 0) { userRepository.existsById(any()) }
            }

            @Test
            fun `should raise UserNotExistsError if the user is not available and does not exist`() {
                every { userRepository.findAvailableToAdministratorById(administratorId = administrator.id, userId = userId) } returns null
                every { userRepository.existsById(userId) } returns false

                assertRaises(UserNotExistsError(userId)) { operations.viewUser(user = administrator, userId = userId) }
            }

            @Test
            fun `should raise UserAccessDeniedError if the user exists but is not available`() {
                every { userRepository.findAvailableToAdministratorById(administratorId = administrator.id, userId = userId) } returns null
                every { userRepository.existsById(userId) } returns true

                assertRaises(UserAccessDeniedError(userId)) { operations.viewUser(user = administrator, userId = userId) }
            }
        }

        @Nested
        inner class InvariantTests {

            @Test
            fun `should not touch writable storage when viewing a user`() {
                every {
                    userRepository.findAvailableToAdministratorById(administratorId = administrator.id, userId = administrator.id)
                } returns administrator
                every { userRepository.findLastLogins(listOf(administrator.id)) } returns emptyMap()

                operations.viewUser(user = administrator, userId = administrator.id).getOrThrow()

                verify { listOf(communities, managerInvites, developerInvites, observers, contests, multipleRoleUsers) wasNot Called }
            }
        }

        @Nested
        inner class ModuleRuleTests {

            @Test
            fun `should propagate a technical storage exception when viewing a user`() {
                val failure = IllegalStateException("User storage unavailable")
                every {
                    userRepository.findAvailableToAdministratorById(administratorId = administrator.id, userId = userId)
                } throws failure

                val actual = assertThrows(IllegalStateException::class.java) {
                    operations.viewUser(user = administrator, userId = userId)
                }

                assertSame(failure, actual)
            }
        }
    }

    @Nested
    inner class CreateCommunityInviteTests {

        @Nested
        inner class HappyPathTests {

            @Test
            fun `should replace the developer code with a different one and restart its validity period`() {
                val existing = testDeveloperInvite(code = "abcdefghjkmn", expiresAt = Instant.EPOCH)
                prepare()
                every { developerInvites.load(any<LazyEntity<CommunityInviteId, CommunityInvite.Developer>>()) } returns existing
                val updated = slot<CommunityInvite.Developer>()
                every { developerInvites.update(capture(updated)) } answers { firstArg() }

                val actual = create(kind = CommunityInvite.Kind.Developer).getOrThrow()

                assertEquals(existing.id, updated.captured.id)
                assertEquals(HashAlgorithm.Identity, updated.captured.data.codeHash.algorithm)
                assertTrue(INVITE_CODE_FORMAT.matches(updated.captured.data.codeHash.value))
                assertNotEquals(existing.data.codeHash, updated.captured.data.codeHash)
                assertEquals(Instant.parse("2026-01-02T10:00:00Z"), updated.captured.data.expiresAt)
                assertSame(updated.captured, actual)
            }

            @Test
            fun `should replace the manager code with a different one and restart its validity period`() {
                val existing = testManagerInvite(code = "abcdefghjkmn", expiresAt = FAR_FUTURE)
                prepare()
                every { managerInvites.load(any<LazyEntity<CommunityInviteId, CommunityInvite.Manager>>()) } returns existing
                val updated = slot<CommunityInvite.Manager>()
                every { managerInvites.update(capture(updated)) } answers { firstArg() }

                val actual = create(kind = CommunityInvite.Kind.Manager).getOrThrow()

                assertEquals(existing.id, updated.captured.id)
                assertEquals(HashAlgorithm.Identity, updated.captured.data.codeHash.algorithm)
                assertTrue(INVITE_CODE_FORMAT.matches(updated.captured.data.codeHash.value))
                assertNotEquals(existing.data.codeHash, updated.captured.data.codeHash)
                assertEquals(Instant.parse("2026-01-02T10:00:00Z"), updated.captured.data.expiresAt)
                assertSame(updated.captured, actual)
            }
        }

        @Nested
        inner class RefusalTests {

            @Test
            fun `should raise MissedAdministratorRoleError before reading storage, time or configuration`() {
                val user = testManager { data = managerData {} }

                assertRaises(MissedAdministratorRoleError) { create(user = user) }

                verify { listOf(communities, managerInvites, developerInvites, config, clock) wasNot Called }
            }

            @Test
            fun `should raise CommunityNotExistsError without writing an invite if community does not exist`() {
                every { communities.findById(communityId) } returns null

                assertRaises(CommunityNotExistsError(communityId)) { create() }

                verify { listOf(managerInvites, developerInvites, config, clock) wasNot Called }
            }

            @Test
            fun `should raise CommunityAccessDeniedError without writing an invite if community belongs to another user`() {
                every { communities.findById(communityId) } returns testCommunity(ownerId = 99)

                assertRaises(CommunityAccessDeniedError(communityId)) { create() }

                verify { listOf(managerInvites, developerInvites, config, clock) wasNot Called }
            }

            @Test
            fun `should raise CommunityAccessDeniedError for a member who does not own the community`() {
                val member = testAdministrator { memberOf(listOf(41)) }
                every { communities.findById(communityId) } returns testCommunity(ownerId = 99)

                assertRaises(CommunityAccessDeniedError(communityId)) { create(user = member, kind = CommunityInvite.Kind.Developer) }

                verify { listOf(managerInvites, developerInvites) wasNot Called }
            }
        }

        @Nested
        inner class InvariantTests {

            @Test
            fun `should not change the manager invite when replacing the developer invite`() {
                prepare()
                every {
                    developerInvites.load(any<LazyEntity<CommunityInviteId, CommunityInvite.Developer>>())
                } returns testDeveloperInvite()
                every { developerInvites.update(any<CommunityInvite.Developer>()) } answers { firstArg() }

                create(kind = CommunityInvite.Kind.Developer).getOrThrow()

                verify { managerInvites wasNot Called }
            }

            @Test
            fun `should not change the developer invite when replacing the manager invite`() {
                prepare()
                every { managerInvites.load(any<LazyEntity<CommunityInviteId, CommunityInvite.Manager>>()) } returns testManagerInvite()
                every { managerInvites.update(any<CommunityInvite.Manager>()) } answers { firstArg() }

                create(kind = CommunityInvite.Kind.Manager).getOrThrow()

                verify { developerInvites wasNot Called }
            }

            @Test
            fun `should not change the community or the membership of its users when replacing an invite`() {
                prepare()
                every { managerInvites.load(any<LazyEntity<CommunityInviteId, CommunityInvite.Manager>>()) } returns testManagerInvite()
                every { managerInvites.update(any<CommunityInvite.Manager>()) } answers { firstArg() }

                create(kind = CommunityInvite.Kind.Manager).getOrThrow()

                verify(exactly = 0) { communities.update(any<Community>()) }
                verify { userRepository wasNot Called }
            }
        }

        @Nested
        inner class ModuleRuleTests {

            @Test
            fun `should read the current time and the ttl once`() {
                prepare()
                every { managerInvites.load(any<LazyEntity<CommunityInviteId, CommunityInvite.Manager>>()) } returns testManagerInvite()
                every { managerInvites.update(any<CommunityInvite.Manager>()) } answers { firstArg() }

                create(kind = CommunityInvite.Kind.Manager).getOrThrow()

                verify(exactly = 1) { clock.instant() }
                verify(exactly = 1) { config.ttl }
            }

            @ParameterizedTest
            @ValueSource(longs = [0, -1])
            fun `should fail without writing if the configured ttl is not positive`(ttlSeconds: Long) {
                every { communities.findById(communityId) } returns testCommunity()
                every { clock.instant() } returns now
                every { config.ttl } returns Duration.ofSeconds(ttlSeconds)

                assertThrows(IllegalStateException::class.java) { create() }

                verify { listOf(managerInvites, developerInvites) wasNot Called }
            }

            @Test
            fun `should propagate a storage exception of the update including a code collision`() {
                val failure = IllegalStateException("Invite code collision")
                prepare()
                every { managerInvites.load(any<LazyEntity<CommunityInviteId, CommunityInvite.Manager>>()) } returns testManagerInvite()
                every { managerInvites.update(any<CommunityInvite.Manager>()) } throws failure

                val actual = assertThrows(IllegalStateException::class.java) {
                    create(kind = CommunityInvite.Kind.Manager)
                }

                assertSame(failure, actual)
            }
        }

        private fun create(user: MultipleRoleUser = administrator, kind: CommunityInvite.Kind = CommunityInvite.Kind.Manager) =
            operations.createCommunityInvite(user = user, communityId = communityId, kind = kind)
    }

    @Nested
    inner class ExtendCommunityInviteTests {

        @Nested
        inner class HappyPathTests {

            @Test
            fun `should keep the code and restart the validity period of a still valid invite from the current time`() {
                val existing = testDeveloperInvite(code = "abcdefghjkmn", expiresAt = now.plusSeconds(3600))
                prepare(ttl = Duration.ofHours(2))
                every { developerInvites.load(any<LazyEntity<CommunityInviteId, CommunityInvite.Developer>>()) } returns existing
                every { developerInvites.update(any<CommunityInvite.Developer>()) } answers { firstArg() }

                val actual = extend().getOrThrow()

                assertEquals(existing.data.codeHash, actual.data.codeHash)
                assertEquals(CommunityInvite.Kind.Developer, actual.kind)
                assertEquals(Instant.parse("2026-01-01T12:00:00Z"), actual.data.expiresAt)
            }

            @Test
            fun `should keep the code and restart the validity period of an expired invite`() {
                val existing = testDeveloperInvite(code = "abcdefghjkmn", expiresAt = now.minusSeconds(1))
                prepare(ttl = Duration.ofHours(2))
                every { developerInvites.load(any<LazyEntity<CommunityInviteId, CommunityInvite.Developer>>()) } returns existing
                every { developerInvites.update(any<CommunityInvite.Developer>()) } answers { firstArg() }

                val actual = extend().getOrThrow()

                assertEquals(existing.data.codeHash, actual.data.codeHash)
                assertEquals(CommunityInvite.Kind.Developer, actual.kind)
                assertEquals(Instant.parse("2026-01-01T12:00:00Z"), actual.data.expiresAt)
            }

            @Test
            fun `should move the expiration moment earlier if the ttl was reduced`() {
                val existing = testDeveloperInvite(code = "abcdefghjkmn", expiresAt = now.plus(Duration.ofHours(5)))
                prepare(ttl = Duration.ofHours(2))
                every { developerInvites.load(any<LazyEntity<CommunityInviteId, CommunityInvite.Developer>>()) } returns existing
                every { developerInvites.update(any<CommunityInvite.Developer>()) } answers { firstArg() }

                val actual = extend().getOrThrow()

                assertEquals(Instant.parse("2026-01-01T12:00:00Z"), actual.data.expiresAt)
            }

            @Test
            fun `should keep the code and restart the validity period of the manager invite`() {
                val existing = testManagerInvite(code = "abcdefghjkmn", expiresAt = now.minusSeconds(1))
                prepare(ttl = Duration.ofHours(2))
                every { managerInvites.load(any<LazyEntity<CommunityInviteId, CommunityInvite.Manager>>()) } returns existing
                every { managerInvites.update(any<CommunityInvite.Manager>()) } answers { firstArg() }

                val actual = extend(kind = CommunityInvite.Kind.Manager).getOrThrow()

                assertEquals(existing.data.codeHash, actual.data.codeHash)
                assertEquals(CommunityInvite.Kind.Manager, actual.kind)
                assertEquals(Instant.parse("2026-01-01T12:00:00Z"), actual.data.expiresAt)
            }
        }

        @Nested
        inner class RefusalTests {

            @Test
            fun `should raise MissedAdministratorRoleError before reading storage`() {
                val user = testMultipleRoleUser {}

                assertRaises(MissedAdministratorRoleError) { extend(user = user) }

                verify { listOf(communities, managerInvites, developerInvites, config, clock) wasNot Called }
            }

            @Test
            fun `should raise MissedAdministratorRoleError if user has other roles but not Administrator`() {
                assertRaises(MissedAdministratorRoleError) { extend(user = nonAdministrator) }

                verify { listOf(communities, managerInvites, developerInvites, config, clock) wasNot Called }
            }

            @Test
            fun `should raise CommunityNotExistsError if community does not exist`() {
                every { communities.findById(communityId) } returns null

                assertRaises(CommunityNotExistsError(communityId)) { extend() }

                verify { listOf(managerInvites, developerInvites, config, clock) wasNot Called }
            }

            @Test
            fun `should raise CommunityAccessDeniedError if community belongs to another user`() {
                every { communities.findById(communityId) } returns testCommunity(ownerId = 99)

                assertRaises(CommunityAccessDeniedError(communityId)) { extend() }

                verify { listOf(managerInvites, developerInvites, config, clock) wasNot Called }
            }
        }

        @Nested
        inner class InvariantTests {

            @Test
            fun `should store the extended invite with its id and code unchanged`() {
                val existing = testDeveloperInvite(code = "abcdefghjkmn", expiresAt = now.minusSeconds(1))
                prepare()
                every { developerInvites.load(any<LazyEntity<CommunityInviteId, CommunityInvite.Developer>>()) } returns existing
                val updated = slot<CommunityInvite.Developer>()
                every { developerInvites.update(capture(updated)) } answers { firstArg() }

                extend().getOrThrow()

                assertEquals(existing.id, updated.captured.id)
                assertEquals(existing.data.codeHash, updated.captured.data.codeHash)
            }

            @Test
            fun `should not change the manager invite when extending the developer invite`() {
                prepare()
                every {
                    developerInvites.load(any<LazyEntity<CommunityInviteId, CommunityInvite.Developer>>())
                } returns testDeveloperInvite()
                every { developerInvites.update(any<CommunityInvite.Developer>()) } answers { firstArg() }

                extend().getOrThrow()

                verify { managerInvites wasNot Called }
            }

            @Test
            fun `should not change the community or the membership of its users when extending an invite`() {
                prepare()
                every {
                    developerInvites.load(any<LazyEntity<CommunityInviteId, CommunityInvite.Developer>>())
                } returns testDeveloperInvite()
                every { developerInvites.update(any<CommunityInvite.Developer>()) } answers { firstArg() }

                extend().getOrThrow()

                verify(exactly = 0) { communities.update(any<Community>()) }
                verify { userRepository wasNot Called }
            }
        }

        @Nested
        inner class ModuleRuleTests {

            @Test
            fun `should read the current time and the ttl once`() {
                prepare()
                every {
                    developerInvites.load(any<LazyEntity<CommunityInviteId, CommunityInvite.Developer>>())
                } returns testDeveloperInvite()
                every { developerInvites.update(any<CommunityInvite.Developer>()) } answers { firstArg() }

                extend().getOrThrow()

                verify(exactly = 1) { clock.instant() }
                verify(exactly = 1) { config.ttl }
            }

            @Test
            fun `should propagate a storage exception of the update`() {
                val failure = IllegalStateException("Stale invite version")
                prepare()
                every {
                    developerInvites.load(any<LazyEntity<CommunityInviteId, CommunityInvite.Developer>>())
                } returns testDeveloperInvite()
                every { developerInvites.update(any<CommunityInvite.Developer>()) } throws failure

                val actual = assertThrows(IllegalStateException::class.java) { extend() }

                assertSame(failure, actual)
            }
        }

        private fun extend(user: MultipleRoleUser = administrator, kind: CommunityInvite.Kind = CommunityInvite.Kind.Developer) =
            operations.extendCommunityInvite(user = user, communityId = communityId, kind = kind)
    }

    @Nested
    inner class ViewCommunityInvitesTests {

        @Nested
        inner class HappyPathTests {

            @Test
            fun `should return both invites as issued with the manager invite first and an expired one unchanged`() {
                val manager = testManagerInvite(code = "manager23456")
                val developer = testDeveloperInvite(code = "developer234", expiresAt = Instant.EPOCH)
                every { communities.findById(communityId) } returns testCommunity()
                every { managerInvites.load(any<LazyEntity<CommunityInviteId, CommunityInvite.Manager>>()) } returns manager
                every { developerInvites.load(any<LazyEntity<CommunityInviteId, CommunityInvite.Developer>>()) } returns developer

                val actual = view().getOrThrow()

                assertEquals(listOf(manager, developer), actual)
            }
        }

        @Nested
        inner class RefusalTests {

            @Test
            fun `should raise MissedAdministratorRoleError before reading storage`() {
                val user = testManager { data = managerData {} }

                assertRaises(MissedAdministratorRoleError) { view(user = user) }

                verify { listOf(communities, managerInvites, developerInvites) wasNot Called }
            }

            @Test
            fun `should raise CommunityNotExistsError without reading invites if community does not exist`() {
                every { communities.findById(communityId) } returns null

                assertRaises(CommunityNotExistsError(communityId)) { view() }

                verify { listOf(managerInvites, developerInvites) wasNot Called }
            }

            @Test
            fun `should raise CommunityAccessDeniedError if community belongs to another user`() {
                every { communities.findById(communityId) } returns testCommunity(ownerId = 99)

                assertRaises(CommunityAccessDeniedError(communityId)) { view() }

                verify { listOf(managerInvites, developerInvites) wasNot Called }
            }
        }

        @Nested
        inner class InvariantTests {

            @Test
            fun `should neither update nor replace an expired invite when viewing invites`() {
                every { communities.findById(communityId) } returns testCommunity()
                every { managerInvites.load(any<LazyEntity<CommunityInviteId, CommunityInvite.Manager>>()) } returns testManagerInvite()
                every { developerInvites.load(any<LazyEntity<CommunityInviteId, CommunityInvite.Developer>>()) } returns
                    testDeveloperInvite(expiresAt = Instant.EPOCH)

                view().getOrThrow()

                verify(exactly = 0) { communities.update(any<Community>()) }
                verify(exactly = 0) { managerInvites.update(any<CommunityInvite.Manager>()) }
                verify(exactly = 0) { developerInvites.update(any<CommunityInvite.Developer>()) }
                verify { listOf(clock, config) wasNot Called }
            }
        }

        private fun view(user: MultipleRoleUser = administrator) = operations.viewCommunityInvites(user = user, communityId = communityId)
    }

    @Nested
    inner class RefreshCommunityInviteTests {

        @Nested
        inner class HappyPathTests {

            @Test
            fun `should replace the code of an expired invite keeping its variant`() {
                val invite = testDeveloperInvite(code = "abcdefghjkmn", expiresAt = now.minusSeconds(1))
                prepareRefresh(invite)
                every { config.ttl } returns Duration.ofHours(1)
                every { developerInvites.update(any<CommunityInvite.Developer>()) } answers { firstArg() }

                val actual = refresh().getOrThrow()

                assertEquals(CommunityInvite.Kind.Developer, actual.kind)
                assertEquals(HashAlgorithm.Identity, actual.data.codeHash.algorithm)
                assertTrue(INVITE_CODE_FORMAT.matches(actual.data.codeHash.value))
                assertNotEquals(invite.data.codeHash, actual.data.codeHash)
                assertEquals(Instant.parse("2026-01-01T11:00:00Z"), actual.data.expiresAt)
            }

            @Test
            fun `should replace the code of an invite whose expiration moment equals the current time`() {
                val invite = testDeveloperInvite(code = "abcdefghjkmn", expiresAt = now)
                prepareRefresh(invite)
                every { config.ttl } returns Duration.ofHours(1)
                every { developerInvites.update(any<CommunityInvite.Developer>()) } answers { firstArg() }

                val actual = refresh().getOrThrow()

                assertNotEquals(invite.data.codeHash, actual.data.codeHash)
                assertEquals(Instant.parse("2026-01-01T11:00:00Z"), actual.data.expiresAt)
            }

            @Test
            fun `should replace the code of an expired manager invite through the manager invites`() {
                val invite = testManagerInvite(code = "abcdefghjkmn", expiresAt = now.minusSeconds(1))
                every { communities.findById(communityId) } returns testCommunity()
                every { managerInvites.load(any<LazyEntity<CommunityInviteId, CommunityInvite.Manager>>()) } returns invite
                every { clock.instant() } returns now
                every { config.ttl } returns Duration.ofHours(1)
                val updated = slot<CommunityInvite.Manager>()
                every { managerInvites.update(capture(updated)) } answers { firstArg() }

                val actual = refresh(kind = CommunityInvite.Kind.Manager).getOrThrow()

                assertEquals(invite.id, updated.captured.id)
                assertTrue(INVITE_CODE_FORMAT.matches(actual.data.codeHash.value))
                assertNotEquals(invite.data.codeHash, actual.data.codeHash)
                assertEquals(Instant.parse("2026-01-01T11:00:00Z"), actual.data.expiresAt)
                assertSame(updated.captured, actual)
            }
        }

        @Nested
        inner class RefusalTests {

            @Test
            fun `should raise MissedAdministratorRoleError before reading storage if the owner lost the Administrator role`() {
                val user = testManager { data = managerData {} }

                assertRaises(MissedAdministratorRoleError) { refresh(user) }

                verify { listOf(communities, managerInvites, developerInvites, config, clock) wasNot Called }
            }

            @Test
            fun `should raise CommunityNotExistsError if community does not exist`() {
                every { communities.findById(communityId) } returns null

                assertRaises(CommunityNotExistsError(communityId)) { refresh() }

                verify { listOf(managerInvites, developerInvites, config, clock) wasNot Called }
            }

            @Test
            fun `should raise CommunityAccessDeniedError if community belongs to another user`() {
                every { communities.findById(communityId) } returns testCommunity(ownerId = 99)

                assertRaises(CommunityAccessDeniedError(communityId)) { refresh() }

                verify { listOf(managerInvites, developerInvites, config, clock) wasNot Called }
            }
        }

        @Nested
        inner class InvariantTests {

            @Test
            fun `should return a still valid invite unchanged without writing`() {
                val invite = testDeveloperInvite(expiresAt = now.plusSeconds(1))
                prepareRefresh(invite)

                val actual = refresh().getOrThrow()

                assertSame(invite, actual)
                verify(exactly = 0) { developerInvites.update(any<CommunityInvite.Developer>()) }
                verify { config wasNot Called }
            }

            @Test
            fun `should not change the community or the other invite when replacing an expired invite`() {
                prepareRefresh(testDeveloperInvite(expiresAt = now.minusSeconds(1)))
                every { config.ttl } returns Duration.ofHours(1)
                every { developerInvites.update(any<CommunityInvite.Developer>()) } answers { firstArg() }

                refresh().getOrThrow()

                verify(exactly = 0) { communities.update(any<Community>()) }
                verify { listOf(managerInvites, userRepository) wasNot Called }
            }
        }

        @Nested
        inner class ModuleRuleTests {

            @Test
            fun `should read the current time and the ttl once when replacing an expired invite`() {
                prepareRefresh(testDeveloperInvite(expiresAt = now.minusSeconds(1)))
                every { config.ttl } returns Duration.ofHours(1)
                every { developerInvites.update(any<CommunityInvite.Developer>()) } answers { firstArg() }

                refresh().getOrThrow()

                verify(exactly = 1) { clock.instant() }
                verify(exactly = 1) { config.ttl }
            }

            @Test
            fun `should propagate a storage exception of the update`() {
                val failure = IllegalStateException("Stale invite version")
                prepareRefresh(testDeveloperInvite(expiresAt = Instant.EPOCH))
                every { config.ttl } returns Duration.ofHours(1)
                every { developerInvites.update(any<CommunityInvite.Developer>()) } throws failure

                val actual = assertThrows(IllegalStateException::class.java) { refresh() }

                assertSame(failure, actual)
            }
        }

        private fun refresh(user: MultipleRoleUser = administrator, kind: CommunityInvite.Kind = CommunityInvite.Kind.Developer) =
            operations.refreshCommunityInvite(user = user, communityId = communityId, kind = kind)

        private fun prepareRefresh(invite: CommunityInvite.Developer) {
            every { communities.findById(communityId) } returns testCommunity()
            every { developerInvites.load(any<LazyEntity<CommunityInviteId, CommunityInvite.Developer>>()) } returns invite
            every { clock.instant() } returns now
        }
    }

    @Nested
    inner class ViewCommunitiesTests {

        @Nested
        inner class HappyPathTests {

            @Test
            fun `should return the communities of the administrator in the port order with their user counts`() {
                val first = testCommunity()
                val second = testCommunity(id = 42)
                every { communities.findByOwner(administrator.id) } returns listOf(first, second)
                every { userRepository.countAvailableToAdministrator(administrator.id, UserFilter(communityId = communityId)) } returns 3
                every { userRepository.countAvailableToAdministrator(administrator.id, UserFilter(communityId = CommunityId(42))) } returns 1

                val actual = operations.viewCommunities(administrator).getOrThrow()

                assertEquals(listOf(first to 3L, second to 1L), actual)
            }

            @Test
            fun `should return an empty list if the administrator created no communities`() {
                every { communities.findByOwner(administrator.id) } returns emptyList()

                val actual = operations.viewCommunities(administrator).getOrThrow()

                assertEquals(emptyList<Pair<Community, Long>>(), actual)
                verify { userRepository wasNot Called }
            }
        }

        @Nested
        inner class RefusalTests {

            @Test
            fun `should raise MissedAdministratorRoleError before reading storage if user is not an Administrator`() {
                assertRaises(MissedAdministratorRoleError) { operations.viewCommunities(testManager { data = managerData {} }) }

                verify { listOf(communities, userRepository) wasNot Called }
            }
        }

    }

    @Nested
    inner class CreateCommunityTests {

        @Nested
        inner class HappyPathTests {

            @Test
            fun `should accept a name of exactly 255 code points`() {
                val name = "😀".repeat(255)
                prepareSave()

                val actual = create(name = name).getOrThrow()

                assertEquals(name, actual.data.name)
            }

            @Test
            fun `should save the community of the administrator with an empty description by default and two new invites`() {
                prepareSave()

                val actual = create(name = "Community").getOrThrow()

                assertEquals(communityId, actual.id)
                assertEquals(administrator.id, actual.data.owner.id)
                assertEquals("Community", actual.data.name)
                assertEquals("", actual.data.description)
                assertEquals(CommunityInviteId(51), actual.data.managerInvite.id)
                assertEquals(CommunityInviteId(52), actual.data.developerInvite.id)
                verify(exactly = 1) { communities.saveWithInvites(any(), any(), any()) }
                verify(exactly = 1) { clock.instant() }
                verify(exactly = 1) { config.ttl }
            }

            @Test
            fun `should keep the given description`() {
                prepareSave()

                val actual =
                    operations.createCommunity(user = administrator, communityName = "Community", description = "  About  ").getOrThrow()

                assertEquals("  About  ", actual.data.description)
            }

            @Test
            fun `should issue distinct invite codes expiring after the ttl`() {
                val managerInvite = slot<CommunityInviteData>()
                val developerInvite = slot<CommunityInviteData>()
                prepareSave(managerInvite, developerInvite)

                create().getOrThrow()

                assertTrue(INVITE_CODE_FORMAT.matches(managerInvite.captured.codeHash.value))
                assertTrue(INVITE_CODE_FORMAT.matches(developerInvite.captured.codeHash.value))
                assertNotEquals(managerInvite.captured.codeHash, developerInvite.captured.codeHash)
                assertEquals(Instant.parse("2026-01-02T10:00:00Z"), managerInvite.captured.expiresAt)
                assertEquals(Instant.parse("2026-01-02T10:00:00Z"), developerInvite.captured.expiresAt)
            }
        }

        @Nested
        inner class RefusalTests {

            @Test
            fun `should raise MissedAdministratorRoleError before reading storage, time or configuration`() {
                assertRaises(MissedAdministratorRoleError) { create(user = testManager { data = managerData {} }) }

                verify { listOf(communities, config, clock) wasNot Called }
            }

            @ParameterizedTest
            @EmptySource
            @ValueSource(strings = [" ", "\t\n"])
            fun `should raise CommunityNameBlankError without saving if the name is blank`(name: String) {
                assertRaises(CommunityNameBlankError) { create(name = name) }

                verify { listOf(communities, config, clock) wasNot Called }
            }

            @Test
            fun `should raise CommunityNameTooLongError without saving if the name exceeds 255 code points`() {
                val name = "😀".repeat(256)

                assertRaises(CommunityNameTooLongError(name)) { create(name = name) }

                verify { communities wasNot Called }
            }
        }

        private fun create(user: MultipleRoleUser = administrator, name: String = "Community") =
            operations.createCommunity(user = user, communityName = name)

        /** Stubs the time, the ttl and the save, which builds the community data with invite ids 51 and 52. */
        private fun prepareSave(
            managerInvite: CapturingSlot<CommunityInviteData> = slot(),
            developerInvite: CapturingSlot<CommunityInviteData> = slot(),
        ) {
            val buildData = slot<(CommunityInviteId, CommunityInviteId) -> CommunityData>()
            every { clock.instant() } returns now
            every { config.ttl } returns Duration.ofDays(1)
            every { communities.saveWithInvites(capture(managerInvite), capture(developerInvite), capture(buildData)) } answers {
                community {
                    id = communityId.value
                    createdAt = Instant.EPOCH
                    version = EntityVersion(0)
                    data = buildData.captured(CommunityInviteId(51), CommunityInviteId(52))
                }
            }
        }
    }

    @Nested
    inner class EditCommunityTests {

        @Nested
        inner class HappyPathTests {

            @Test
            fun `should accept a name of exactly 255 code points`() {
                val name = "😀".repeat(255)
                prepareUpdate()

                val actual = edit(name = name).getOrThrow()

                assertEquals(name, actual.data.name)
            }

            @Test
            fun `should replace the name and description keeping the owner and invites`() {
                val updated = prepareUpdate()

                val actual = edit(name = "  Renamed  ", description = "About").getOrThrow()

                assertEquals(communityId, updated.captured.id)
                assertEquals("  Renamed  ", updated.captured.data.name)
                assertEquals("About", updated.captured.data.description)
                assertEquals(administrator.id, updated.captured.data.owner.id)
                assertEquals(CommunityInviteId(51), updated.captured.data.managerInvite.id)
                assertEquals(CommunityInviteId(52), updated.captured.data.developerInvite.id)
                assertSame(updated.captured, actual)
                verify(exactly = 1) { communities.update(any<Community>()) }
            }

            @Test
            fun `should clear the description if the new description is empty`() {
                every { communities.findById(communityId) } returns testCommunity(description = "About")
                every { communities.update(any<Community>()) } answers { firstArg() }

                val actual = edit(description = "").getOrThrow()

                assertEquals("", actual.data.description)
            }
        }

        @Nested
        inner class RefusalTests {

            @Test
            fun `should raise MissedAdministratorRoleError before reading storage`() {
                assertRaises(MissedAdministratorRoleError) { edit(user = testManager { data = managerData {} }) }

                verify { communities wasNot Called }
            }

            @Test
            fun `should raise CommunityNotExistsError if community does not exist`() {
                every { communities.findById(communityId) } returns null

                assertRaises(CommunityNotExistsError(communityId)) { edit() }
            }

            @Test
            fun `should raise CommunityAccessDeniedError without updating if community belongs to another user`() {
                every { communities.findById(communityId) } returns testCommunity(ownerId = 99)

                assertRaises(CommunityAccessDeniedError(communityId)) { edit(name = "") }

                verify(exactly = 0) { communities.update(any<Community>()) }
            }

            @ParameterizedTest
            @EmptySource
            @ValueSource(strings = [" ", "\t\n"])
            fun `should raise CommunityNameBlankError without updating if the name is blank`(name: String) {
                every { communities.findById(communityId) } returns testCommunity()

                assertRaises(CommunityNameBlankError) { edit(name = name) }

                verify(exactly = 0) { communities.update(any<Community>()) }
            }

            @Test
            fun `should raise CommunityNameTooLongError without updating if the name exceeds 255 code points`() {
                val name = "😀".repeat(256)
                every { communities.findById(communityId) } returns testCommunity()

                assertRaises(CommunityNameTooLongError(name)) { edit(name = name) }

                verify(exactly = 0) { communities.update(any<Community>()) }
            }
        }

        private fun edit(user: MultipleRoleUser = administrator, name: String = "Renamed", description: String = "") =
            operations.editCommunity(user = user, communityId = communityId, communityName = name, description = description)

        private fun prepareUpdate(): CapturingSlot<Community> {
            val updated = slot<Community>()
            every { communities.findById(communityId) } returns testCommunity()
            every { communities.update(capture(updated)) } answers { firstArg() }
            return updated
        }
    }

    @Nested
    inner class GrantRoleTests {

        private val memberId = MultipleRoleUserId(42)

        @Nested
        inner class HappyPathTests {

            @ParameterizedTest
            @EnumSource(CommunityRole::class)
            fun `should add the membership of the user in the chosen role and return the stored user`(role: CommunityRole) {
                val stored = member()
                prepareGrant()
                every { multipleRoleUsers.addCommunityMembership(userId = memberId, communityId = communityId, role = role) } returns stored

                val actual = grant(role = role).getOrThrow()

                assertSame(stored, actual)
                verify(exactly = 1) { multipleRoleUsers.addCommunityMembership(userId = memberId, communityId = communityId, role = role) }
            }

            @Test
            fun `should return the user unchanged if the user is already a member in the role`() {
                val existing = member {
                    roles {
                        student {
                            memberOf(listOf(communityId.value))
                            data = studentData {}
                        }
                    }
                }
                prepareGrant(available = existing)
                every {
                    multipleRoleUsers.addCommunityMembership(userId = memberId, communityId = communityId, role = CommunityRole.Student)
                } returns existing

                val actual = grant(role = CommunityRole.Student).getOrThrow()

                assertSame(existing, actual)
            }
        }

        @Nested
        inner class RefusalTests {

            @Test
            fun `should raise MissedAdministratorRoleError before reading storage if user is not an Administrator`() {
                assertRaises(MissedAdministratorRoleError) { grant(user = testManager { data = managerData {} }) }

                verify { listOf(communities, userRepository, multipleRoleUsers) wasNot Called }
            }

            @Test
            fun `should raise UserNotExistsError if the user does not exist`() {
                every { userRepository.existsById(memberId) } returns false

                assertRaises(UserNotExistsError(memberId)) { grant() }

                verify { multipleRoleUsers wasNot Called }
            }

            @Test
            fun `should raise CommunityNotExistsError if the community does not exist`() {
                every { userRepository.existsById(memberId) } returns true
                every { communities.findById(communityId) } returns null

                assertRaises(CommunityNotExistsError(communityId)) { grant() }

                verify { multipleRoleUsers wasNot Called }
            }

            @Test
            fun `should raise UserAccessDeniedError if the user is not available to the administrator`() {
                prepareGrant(available = null)

                assertRaises(UserAccessDeniedError(memberId)) { grant() }

                verify { multipleRoleUsers wasNot Called }
            }

            @Test
            fun `should raise CommunityAccessDeniedError if the community belongs to another user`() {
                prepareGrant(ownerId = 99)

                assertRaises(CommunityAccessDeniedError(communityId)) { grant() }

                verify { multipleRoleUsers wasNot Called }
            }

            @Test
            fun `should raise UserHasFixedRoleError if the user is an observer`() {
                val observerId = SingleRoleUserId(31)
                every { userRepository.existsById(observerId) } returns true
                every { communities.findById(communityId) } returns testCommunity()
                every {
                    userRepository.findAvailableToAdministratorById(administratorId = administrator.id, userId = observerId)
                } returns observer {
                    id = observerId.value
                    createdAt = Instant.EPOCH
                    data = observerData {
                        accessToken("observer", algorithm = HashAlgorithm.Identity)
                        name = "Observer"
                        community(communityId.value)
                    }
                }

                assertRaises(UserHasFixedRoleError(observerId)) { grant(userId = observerId) }

                verify { multipleRoleUsers wasNot Called }
            }
        }

        private fun grant(
            user: MultipleRoleUser = administrator,
            userId: UserId = memberId,
            role: CommunityRole = CommunityRole.Developer,
        ) = operations.grantRole(user = user, userId = userId, communityId = communityId, role = role)

        private fun prepareGrant(ownerId: Long = administrator.id.value, available: User<*>? = member()) {
            every { userRepository.existsById(memberId) } returns true
            every { communities.findById(communityId) } returns testCommunity(ownerId = ownerId)
            every { userRepository.findAvailableToAdministratorById(administrator.id, memberId) } returns available
        }

        private fun member(otherRoles: MultipleRoleUserDataBuilder.() -> Unit = {}): MultipleRoleUser = multipleRoleUser {
            id = memberId.value
            createdAt = Instant.EPOCH
            data = multipleRoleUserData {
                accessToken("member", algorithm = HashAlgorithm.Identity)
                name = "Member"
                email = "member@example.com"
                roles {
                    developer {
                        memberOf(listOf(communityId.value))
                        data = developerData {}
                    }
                }
                otherRoles()
            }
        }
    }

    @Nested
    inner class ViewCommunityContestsTests {

        private val pagination = Pagination(page = 0, size = 10)

        @Nested
        inner class HappyPathTests {

            @Test
            fun `should return the contests shared to the community matching the name`() {
                val contest = testContest { sharedTo = mutableListOf(communityId) }
                val expected = Page(content = listOf(contest), pagination = pagination, totalElements = 1)
                every { communities.findById(communityId) } returns testCommunity()
                every {
                    contests.findAvailableToDeveloper(
                        ownerId = administrator.id,
                        communityIds = setOf(communityId),
                        pagination = pagination,
                        filter = ContestFilter(name = "Тур", communityId = communityId),
                    )
                } returns expected

                val actual = view(name = "Тур").getOrThrow()

                assertSame(expected, actual)
            }
        }

        @Nested
        inner class RefusalTests {

            @Test
            fun `should raise MissedAdministratorRoleError before reading storage if user is not an Administrator`() {
                assertRaises(MissedAdministratorRoleError) { view(user = testManager { data = managerData {} }) }

                verify { listOf(communities, contests) wasNot Called }
            }

            @Test
            fun `should raise CommunityNotExistsError if the community does not exist`() {
                every { communities.findById(communityId) } returns null

                assertRaises(CommunityNotExistsError(communityId)) { view() }

                verify { contests wasNot Called }
            }

            @Test
            fun `should raise CommunityAccessDeniedError if the community belongs to another user`() {
                every { communities.findById(communityId) } returns testCommunity(ownerId = 99)

                assertRaises(CommunityAccessDeniedError(communityId)) { view() }

                verify { contests wasNot Called }
            }
        }

        private fun view(user: MultipleRoleUser = administrator, name: String? = null) =
            operations.viewCommunityContests(user = user, communityId = communityId, pagination = pagination, name = name)
    }

    private fun prepare(ttl: Duration = Duration.ofDays(1)) {
        every { communities.findById(communityId) } returns testCommunity()
        every { clock.instant() } returns now
        every { config.ttl } returns ttl
    }

    private fun testCommunity(ownerId: Long = administrator.id.value, id: Long = communityId.value, description: String = ""): Community =
        community {
            this.id = id
            createdAt = Instant.EPOCH
            version = EntityVersion(0)
            data = communityData {
                owner(ownerId)
                name = "Community"
                this.description = description
                managerInvite(51)
                developerInvite(52)
            }
        }

    private fun testManagerInvite(code: String = "pqrstuvwxyz2", expiresAt: Instant = FAR_FUTURE): CommunityInvite.Manager =
        managerCommunityInvite {
            id = 51
            createdAt = Instant.EPOCH
            version = EntityVersion(0)
            data = inviteData(code, expiresAt)
        }

    private fun testDeveloperInvite(code: String = "pqrstuvwxyz2", expiresAt: Instant = FAR_FUTURE): CommunityInvite.Developer =
        developerCommunityInvite {
            id = 52
            createdAt = Instant.EPOCH
            version = EntityVersion(0)
            data = inviteData(code, expiresAt)
        }

    private fun inviteData(code: String, expiresAt: Instant): CommunityInviteData = communityInviteData {
        this.code(code, HashAlgorithm.Identity)
        this.expiresAt = expiresAt
    }

    private companion object {
        val INVITE_CODE_FORMAT = Regex("[a-hjkmnp-z2-9]{12}")
        val LOGGED_IN_AT: Instant = Instant.parse("2025-12-31T09:00:00Z")
        val FAR_FUTURE: Instant = Instant.parse("2030-01-01T00:00:00Z")
    }
}
