package tech.testsys.operation.user

import io.mockk.Called
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EmptySource
import org.junit.jupiter.params.provider.ValueSource
import tech.testsys.domain.builder.api.community
import tech.testsys.domain.builder.api.communityData
import tech.testsys.domain.builder.api.communityInviteData
import tech.testsys.domain.builder.api.developerCommunityInvite
import tech.testsys.domain.builder.api.developerData
import tech.testsys.domain.builder.api.judgeData
import tech.testsys.domain.builder.api.managerCommunityInvite
import tech.testsys.domain.builder.api.managerData
import tech.testsys.domain.builder.api.multipleRoleUser
import tech.testsys.domain.builder.api.multipleRoleUserData
import tech.testsys.domain.builder.api.observer
import tech.testsys.domain.builder.api.observerData
import tech.testsys.domain.contract.persistence.Page
import tech.testsys.domain.contract.persistence.Pagination
import tech.testsys.domain.contract.persistence.Sort
import tech.testsys.domain.contract.persistence.UserFilter
import tech.testsys.domain.contract.persistence.repository.CommunityRepository
import tech.testsys.domain.contract.persistence.repository.ContestRepository
import tech.testsys.domain.contract.persistence.repository.DeveloperCommunityInviteRepository
import tech.testsys.domain.contract.persistence.repository.ManagerCommunityInviteRepository
import tech.testsys.domain.contract.persistence.repository.ObserverRepository
import tech.testsys.domain.contract.persistence.repository.UserRepository
import tech.testsys.domain.model.EntityVersion
import tech.testsys.domain.model.LazyEntity
import tech.testsys.domain.model.group.Community
import tech.testsys.domain.model.group.CommunityId
import tech.testsys.domain.model.group.CommunityInvite
import tech.testsys.domain.model.group.CommunityInviteData
import tech.testsys.domain.model.group.CommunityInviteId
import tech.testsys.domain.model.task.ContestId
import tech.testsys.domain.model.user.HashAlgorithm
import tech.testsys.domain.model.user.MultipleRoleUser
import tech.testsys.domain.model.user.MultipleRoleUserId
import tech.testsys.domain.model.user.Observer
import tech.testsys.domain.model.user.ObserverData
import tech.testsys.domain.model.user.SingleRoleUserId
import tech.testsys.domain.model.user.User
import tech.testsys.operation.config.CommunityInviteConfig
import tech.testsys.operation.error.CommunityAccessDeniedError
import tech.testsys.operation.error.CommunityNotExistsError
import tech.testsys.operation.error.ContestAccessDeniedError
import tech.testsys.operation.error.ContestNotExistsError
import tech.testsys.operation.error.MissedAdministratorRoleError
import tech.testsys.operation.error.ObserverContestsEmptyError
import tech.testsys.operation.error.ObserverNameBlankError
import tech.testsys.operation.error.ObserverNameTooLongError
import tech.testsys.operation.error.UserAccessDeniedError
import tech.testsys.operation.error.UserNotExistsError
import tech.testsys.operation.error.getOrThrow
import tech.testsys.operation.util.assertRaises
import tech.testsys.operation.util.testAdministrator
import tech.testsys.operation.util.testContest
import tech.testsys.operation.util.testManager
import tech.testsys.operation.util.testMultipleRoleUser
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.UUID

class AdministratorOperationsTests {

    private val contests = mockk<ContestRepository>()
    private val observers = mockk<ObserverRepository>()
    private val communities = mockk<CommunityRepository>()
    private val managerInvites = mockk<ManagerCommunityInviteRepository>()
    private val developerInvites = mockk<DeveloperCommunityInviteRepository>()
    private val config = mockk<CommunityInviteConfig>()
    private val clock = mockk<Clock>()
    private val userRepository = mockk<UserRepository>()
    private val operations = AdministratorOperations(
        communityRepository = communities,
        managerInviteRepository = managerInvites,
        developerInviteRepository = developerInvites,
        communityInviteConfig = config,
        clock = clock,
        userRepository = userRepository,
        contestRepository = contests,
        observerRepository = observers,
    )
    private val administrator = testAdministrator {}
    private val communityId = CommunityId(41)
    private val now = Instant.parse("2026-01-01T10:00:00Z")

    @Nested
    inner class CreateObserverTests {

        private val contestId = ContestId(19)

        @Test
        fun `should reject a user without administrator role without reading or saving`() {
            assertRaises(MissedAdministratorRoleError) { create(user = testMultipleRoleUser {}) }

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
            every { contests.findById(ContestId(20)) } returns null

            assertRaises(ContestNotExistsError(ContestId(20))) { create(ids = setOf(contestId, ContestId(20))) }

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

        @ParameterizedTest
        @EmptySource
        @ValueSource(strings = [" ", "\t\r\n", "\u2003"])
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

        @ParameterizedTest
        @ValueSource(strings = ["a", "😀"])
        fun `should accept exactly 255 Unicode code points`(character: String) {
            prepareCreation()
            val saved = captureSave()
            val name = character.repeat(255)

            val result = create(name = name).getOrThrow()

            assertEquals(name, result.data.name)
            assertEquals(name, saved.captured.name)
        }

        @ParameterizedTest
        @ValueSource(strings = ["x", "  Mixed Case  "])
        fun `should save one observer with the exact name community contests and fresh UUID code`(name: String) {
            prepareCreation()
            val second = tech.testsys.domain.builder.api.contest {
                id = 20
                createdAt = Instant.EPOCH
                data = testContest { sharedTo = mutableListOf(communityId, CommunityId(99)) }.data
            }
            every { contests.findById(second.id) } returns second
            val saved = captureSave()

            val result = create(name = name, ids = setOf(contestId, second.id)).getOrThrow()

            assertEquals(name, result.data.name)
            assertEquals(communityId, result.data.community.id)
            assertEquals(setOf(contestId, second.id), result.data.contests.ids.toSet())
            assertEquals(2, result.data.contests.ids.size)
            assertEquals(HashAlgorithm.Identity, result.data.accessTokenHash.algorithm)
            assertEquals(result.data.accessTokenHash.value, UUID.fromString(result.data.accessTokenHash.value).toString())
            assertSame(saved.captured, result.data)
            assertEquals(SingleRoleUserId(73), result.id)
            assertEquals(EntityVersion(1), result.version)
            verify(exactly = 1) { observers.save(any<ObserverData>()) }
            verify(exactly = 0) { communities.update(any<Community>()) }
            verify(exactly = 0) { contests.update(any<tech.testsys.domain.model.task.Contest>()) }
        }

        @Test
        fun `should return the persisted observer instance`() {
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
            every { contests.findById(contestId) } throws failure

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

        private fun create(user: MultipleRoleUser = administrator, name: String = "Observer", ids: Set<ContestId> = setOf(contestId)) =
            operations.createObserver(user = user, communityId = communityId, observerName = name, contestIds = ids)

        private fun prepareCreation(ownerId: Long = administrator.id.value, shared: List<CommunityId> = listOf(communityId)) {
            every { communities.findById(communityId) } returns testCommunity(ownerId)
            every { contests.findById(contestId) } returns testContest {
                owner(administrator.id.value)
                sharedTo = shared.toMutableList()
            }
        }

        private fun captureSave(): io.mockk.CapturingSlot<ObserverData> {
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
    }

    @Nested
    inner class ViewUsersTests {

        private val pagination = Pagination(page = 0, size = 10)

        @Test
        fun `should raise MissedAdministratorRoleError before reading storage if user is not an Administrator`() {
            val user = testManager { data = managerData {} }

            assertRaises(MissedAdministratorRoleError) { operations.viewUsers(user = user, pagination = pagination) }

            verify(exactly = 0) { userRepository.findAvailableToAdministrator(any(), any(), any()) }
        }

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

            val actual = operations.viewUsers(user = user, pagination = pagination).getOrThrow()

            assertSame(expected, actual)
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

            val actual = operations.viewUsers(user = administrator, pagination = request, filter = filter).getOrThrow()

            assertSame(expected, actual)
        }

        @Test
        fun `should return users of different kinds in the page order of the port`() {
            val observer = testObserver()
            val expected =
                Page<User<*>>(content = listOf(observer, administrator), pagination = pagination, totalElements = 2)
            every {
                userRepository.findAvailableToAdministrator(administratorId = administrator.id, pagination = pagination)
            } returns expected

            val actual = operations.viewUsers(user = administrator, pagination = pagination).getOrThrow()

            assertSame(expected, actual)
            assertSame(observer, actual.content.first())
            assertSame(administrator, actual.content.last())
        }

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

        @Test
        fun `should return the user with roles in foreign communities as returned by the port`() {
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
            every { userRepository.findAvailableToAdministratorById(administratorId = administrator.id, userId = userId) } returns expected

            val actual = operations.viewUser(user = administrator, userId = userId).getOrThrow()

            assertSame(expected, actual)
        }

        @Test
        fun `should return an observer as returned by the port`() {
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

            val actual = operations.viewUser(user = administrator, userId = SingleRoleUserId(31)).getOrThrow()

            assertSame(expected, actual)
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

            val actual = operations.viewUser(user = user, userId = user.id).getOrThrow()

            assertSame(user, actual)
        }

        @Test
        fun `should propagate a technical storage exception when viewing a user`() {
            val failure = IllegalStateException("User storage unavailable")
            every { userRepository.findAvailableToAdministratorById(administratorId = administrator.id, userId = userId) } throws failure

            val actual = assertThrows(IllegalStateException::class.java) {
                operations.viewUser(user = administrator, userId = userId)
            }

            assertSame(failure, actual)
        }
    }

    @Nested
    inner class CreateCommunityInviteTests {

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
        }

        @Test
        fun `should replace only the code of the selected role with a different one and restart its validity period`() {
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
            verify { managerInvites wasNot Called }
            verify(exactly = 1) { clock.instant() }
            verify(exactly = 1) { config.ttl }
        }

        @Test
        fun `should replace the manager code through the manager invites`() {
            prepare()
            every { managerInvites.load(any<LazyEntity<CommunityInviteId, CommunityInvite.Manager>>()) } returns testManagerInvite()
            every { managerInvites.update(any<CommunityInvite.Manager>()) } answers { firstArg() }

            val actual = create(kind = CommunityInvite.Kind.Manager).getOrThrow()

            assertEquals(CommunityInvite.Kind.Manager, actual.kind)
            verify { developerInvites wasNot Called }
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

        private fun create(user: MultipleRoleUser = administrator, kind: CommunityInvite.Kind = CommunityInvite.Kind.Manager) =
            operations.createCommunityInvite(user = user, communityId = communityId, kind = kind)
    }

    @Nested
    inner class ExtendCommunityInviteTests {

        @Test
        fun `should raise MissedAdministratorRoleError before reading storage`() {
            val user = testMultipleRoleUser {}

            assertRaises(MissedAdministratorRoleError) { extend(user = user) }

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

        @Test
        fun `should keep the code and restart the validity period of an expired invite`() {
            val expiredAt = now.minusSeconds(1)
            val existing = testDeveloperInvite(code = "abcdefghjkmn", expiresAt = expiredAt)
            prepare(ttl = Duration.ofHours(2))
            every { developerInvites.load(any<LazyEntity<CommunityInviteId, CommunityInvite.Developer>>()) } returns existing
            every { developerInvites.update(any<CommunityInvite.Developer>()) } answers { firstArg() }

            val actual = extend().getOrThrow()

            assertEquals(existing.data.codeHash, actual.data.codeHash)
            assertEquals(CommunityInvite.Kind.Developer, actual.kind)
            assertEquals(Instant.parse("2026-01-01T12:00:00Z"), actual.data.expiresAt)
            verify(exactly = 1) { developerInvites.update(any<CommunityInvite.Developer>()) }
            verify(exactly = 1) { clock.instant() }
            verify(exactly = 1) { config.ttl }
        }

        private fun extend(user: MultipleRoleUser = administrator) =
            operations.extendCommunityInvite(user = user, communityId = communityId, kind = CommunityInvite.Kind.Developer)
    }

    @Nested
    inner class ViewCommunityInvitesTests {

        @Test
        fun `should raise MissedAdministratorRoleError before reading storage`() {
            val user = testManager { data = managerData {} }

            assertRaises(MissedAdministratorRoleError) { operations.viewCommunityInvites(user = user, communityId = communityId) }

            verify { listOf(communities, managerInvites, developerInvites) wasNot Called }
        }

        @Test
        fun `should raise CommunityNotExistsError if community does not exist`() {
            every { communities.findById(communityId) } returns null

            assertRaises(CommunityNotExistsError(communityId)) {
                operations.viewCommunityInvites(user = administrator, communityId = communityId)
            }
        }

        @Test
        fun `should raise CommunityAccessDeniedError if community belongs to another user`() {
            every { communities.findById(communityId) } returns testCommunity(ownerId = 99)

            assertRaises(CommunityAccessDeniedError(communityId)) {
                operations.viewCommunityInvites(user = administrator, communityId = communityId)
            }

            verify { listOf(managerInvites, developerInvites) wasNot Called }
        }

        @Test
        fun `should return both referenced invites as issued with the manager invite first`() {
            val manager = testManagerInvite(code = "manager23456")
            val developer = testDeveloperInvite(code = "developer234", expiresAt = Instant.EPOCH)
            every { communities.findById(communityId) } returns testCommunity()
            every { managerInvites.load(any<LazyEntity<CommunityInviteId, CommunityInvite.Manager>>()) } returns manager
            every { developerInvites.load(any<LazyEntity<CommunityInviteId, CommunityInvite.Developer>>()) } returns developer

            val actual = operations.viewCommunityInvites(user = administrator, communityId = communityId).getOrThrow()

            assertEquals(listOf(manager, developer), actual)
            assertEquals("developer234", actual.last().data.codeHash.value)
            assertEquals(Instant.EPOCH, actual.last().data.expiresAt)
            verify(exactly = 0) { managerInvites.update(any<CommunityInvite.Manager>()) }
            verify(exactly = 0) { developerInvites.update(any<CommunityInvite.Developer>()) }
            verify { listOf(clock, config) wasNot Called }
        }
    }

    @Nested
    inner class RefreshCommunityInviteTests {

        private val kind = CommunityInvite.Kind.Developer

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
        fun `should replace the code of an expired invite keeping its variant`() {
            val expiredAt = now.minusSeconds(1)
            val invite = testDeveloperInvite(code = "abcdefghjkmn", expiresAt = expiredAt)
            prepareRefresh(invite)
            every { config.ttl } returns Duration.ofHours(1)
            every { developerInvites.update(any<CommunityInvite.Developer>()) } answers { firstArg() }

            val actual = refresh().getOrThrow()

            assertEquals(CommunityInvite.Kind.Developer, actual.kind)
            assertEquals(HashAlgorithm.Identity, actual.data.codeHash.algorithm)
            assertTrue(INVITE_CODE_FORMAT.matches(actual.data.codeHash.value))
            assertNotEquals(invite.data.codeHash, actual.data.codeHash)
            assertEquals(Instant.parse("2026-01-01T11:00:00Z"), actual.data.expiresAt)
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

        private fun refresh(user: MultipleRoleUser = administrator) =
            operations.refreshCommunityInvite(user = user, communityId = communityId, kind = kind)

        private fun prepareRefresh(invite: CommunityInvite.Developer) {
            every { communities.findById(communityId) } returns testCommunity()
            every { developerInvites.load(any<LazyEntity<CommunityInviteId, CommunityInvite.Developer>>()) } returns invite
            every { clock.instant() } returns now
        }
    }

    private fun prepare(ttl: Duration = Duration.ofDays(1)) {
        every { communities.findById(communityId) } returns testCommunity()
        every { clock.instant() } returns now
        every { config.ttl } returns ttl
    }

    private fun testCommunity(ownerId: Long = administrator.id.value): Community = community {
        id = communityId.value
        createdAt = Instant.EPOCH
        version = EntityVersion(0)
        data = communityData {
            owner(ownerId)
            name = "Community"
            description = ""
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
        val FAR_FUTURE: Instant = Instant.parse("2030-01-01T00:00:00Z")
    }
}
