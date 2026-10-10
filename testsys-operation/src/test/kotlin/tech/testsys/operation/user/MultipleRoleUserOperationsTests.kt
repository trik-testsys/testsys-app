package tech.testsys.operation.user

import io.mockk.Called
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import tech.testsys.domain.builder.api.communityInviteData
import tech.testsys.domain.builder.api.developerCommunityInvite
import tech.testsys.domain.builder.api.developerData
import tech.testsys.domain.builder.api.emailChangeRequest
import tech.testsys.domain.builder.api.managerCommunityInvite
import tech.testsys.domain.builder.api.managerData
import tech.testsys.domain.builder.api.multipleRoleUser
import tech.testsys.domain.builder.api.studentData
import tech.testsys.domain.contract.UserMailSender
import tech.testsys.domain.contract.persistence.repository.CommunityRepository
import tech.testsys.domain.contract.persistence.repository.DeveloperCommunityInviteRepository
import tech.testsys.domain.contract.persistence.repository.EmailChangeRequestRepository
import tech.testsys.domain.contract.persistence.repository.ManagerCommunityInviteRepository
import tech.testsys.domain.contract.persistence.repository.MultipleRoleUserRepository
import tech.testsys.domain.model.group.Community
import tech.testsys.domain.model.group.CommunityId
import tech.testsys.domain.model.group.CommunityInvite
import tech.testsys.domain.model.group.CommunityInviteData
import tech.testsys.domain.model.group.CommunityInviteId
import tech.testsys.domain.model.group.InviteCodeHash
import tech.testsys.domain.model.user.CommunityRole
import tech.testsys.domain.model.user.EmailChangeRequest
import tech.testsys.domain.model.user.EmailChangeRequestData
import tech.testsys.domain.model.user.EmailChangeRequestId
import tech.testsys.domain.model.user.HashAlgorithm
import tech.testsys.domain.model.user.MultipleRoleUser
import tech.testsys.domain.model.user.MultipleRoleUserId
import tech.testsys.domain.model.user.Student
import tech.testsys.operation.config.EmailConfirmationConfig
import tech.testsys.operation.error.CommunityInviteCodeExpiredError
import tech.testsys.operation.error.CommunityInviteCodeNotValidError
import tech.testsys.operation.error.ConfirmationAttemptsExhaustedError
import tech.testsys.operation.error.ConfirmationCodeExpiredError
import tech.testsys.operation.error.EmailAlreadyBoundError
import tech.testsys.operation.error.EmailChangeRequestNotExistsError
import tech.testsys.operation.error.EmailUnchangedError
import tech.testsys.operation.error.InvalidConfirmationCodeError
import tech.testsys.operation.error.InvalidEmailError
import tech.testsys.operation.error.getOrThrow
import tech.testsys.operation.util.assertRaises
import tech.testsys.operation.util.publicCommunityConfig
import tech.testsys.operation.util.testCommunity
import tech.testsys.operation.util.testDeveloper
import tech.testsys.operation.util.testEmailChangeRequest
import tech.testsys.operation.util.testManager
import tech.testsys.operation.util.testMultipleRoleUser
import tech.testsys.operation.util.testStudent
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.random.RandomGenerator
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class MultipleRoleUserOperationsTests {

    private val multipleRoleUsers = mockk<MultipleRoleUserRepository>()
    private val communities = mockk<CommunityRepository>()
    private val managerInvites = mockk<ManagerCommunityInviteRepository>()
    private val developerInvites = mockk<DeveloperCommunityInviteRepository>()
    private val emailChangeRequests = mockk<EmailChangeRequestRepository>()
    private val mailSender = mockk<UserMailSender>()
    private val clock = mockk<Clock>()
    private val random = mockk<RandomGenerator>()
    private val publicCommunityId = CommunityId(1)
    private val emailConfirmationConfig = object : EmailConfirmationConfig {
        override val confirmationCodeLifetime: Duration = Duration.ofMinutes(15)
        override val maxConfirmationAttempts = 3
    }
    private val operations = MultipleRoleUserOperations(
        multipleRoleUserRepository = multipleRoleUsers,
        communityRepository = communities,
        managerInviteRepository = managerInvites,
        developerInviteRepository = developerInvites,
        emailChangeRequestRepository = emailChangeRequests,
        mailSender = mailSender,
        emailConfirmationConfig = emailConfirmationConfig,
        clock = clock,
        randomGenerator = random,
        communityConfig = publicCommunityConfig(publicCommunityId),
    )
    private val now = Instant.parse("2026-01-01T00:00:00Z")
    private val user = testUser()

    @BeforeEach
    fun setUp() {
        every { multipleRoleUsers.findByEmail(any()) } returns null
        every { multipleRoleUsers.findById(MultipleRoleUserId(0)) } returns user
        every { multipleRoleUsers.update(any<MultipleRoleUser>()) } answers { firstArg() }
        every { multipleRoleUsers.addCommunityMembership(any(), publicCommunityId, any()) } returns user
        every { emailChangeRequests.findByUser(any()) } returns null
        every { emailChangeRequests.save(any<EmailChangeRequestData>()) } answers { savedRequest(firstArg()) }
        every { emailChangeRequests.update(any<EmailChangeRequest>()) } answers { firstArg() }
        every { emailChangeRequests.remove(any<EmailChangeRequest>()) } just runs
        every { mailSender.sendEmailChangeConfirmationCode(any(), any()) } just runs
        every { mailSender.sendEmailChangedNotice(any(), any()) } just runs
        every { clock.instant() } returns now
        every { random.nextInt(10) } returnsMany listOf(8, 7, 6, 5, 4, 3, 2, 1)
    }

    private fun testUser(name: String = "Nickname", email: String = "old@example.com"): MultipleRoleUser = testMultipleRoleUser {
        this.name = name
        this.email = email
        roles {
            student {
                memberOf = mutableListOf(CommunityId(5))
                data = studentData {}
            }
        }
    }

    private fun savedRequest(data: EmailChangeRequestData): EmailChangeRequest = emailChangeRequest {
        id = 41
        createdAt = now
        this.data = data
    }

    private fun otherUser(email: String): MultipleRoleUser = multipleRoleUser {
        id = 52
        createdAt = now
        data = testMultipleRoleUser { this.email = email }.data
    }

    @Nested
    inner class ViewProfileTests {

        @Nested
        inner class HappyPathTests {

            @Test
            fun `should return each role with its communities in ascending id order loaded at once`() {
                val user = testMultipleRoleUser {
                    roles {
                        student {
                            memberOf = mutableListOf(CommunityId(7), CommunityId(5))
                            data = studentData {}
                        }
                        developer {
                            memberOf = mutableListOf(CommunityId(5))
                            data = developerData {}
                        }
                    }
                }
                every { communities.findByIds(listOf(CommunityId(7), CommunityId(5))) } returns listOf(testCommunity(5), testCommunity(7))

                val actual = operations.viewProfile(user).getOrThrow()

                assertEquals(user.data.roles, actual.map { (role, _) -> role })
                assertEquals(
                    listOf(listOf(5L, 7L), listOf(5L)),
                    actual.map { (_, joined) -> joined.map { community -> community.id.value } },
                )
                verify(exactly = 1) { communities.findByIds(any()) }
            }

            @Test
            fun `should return no roles if the user has no roles`() {
                val user = testMultipleRoleUser {}
                every { communities.findByIds(emptyList()) } returns emptyList()

                val actual = operations.viewProfile(user).getOrThrow()

                assertEquals(emptyList(), actual)
            }

            @Test
            fun `should return a role without communities with an empty list`() {
                val user = testStudent {
                    memberOf = mutableListOf()
                    data = studentData {}
                }
                every { communities.findByIds(emptyList()) } returns emptyList()

                val actual = operations.viewProfile(user).getOrThrow()

                assertEquals(listOf(user.data.roles.single() to emptyList()), actual)
            }
        }

        @Nested
        inner class InvariantTests {

            @Test
            fun `should not change the user or communities when viewing the profile`() {
                val user = testStudent {
                    memberOf = mutableListOf(CommunityId(5))
                    data = studentData {}
                }
                every { communities.findByIds(listOf(CommunityId(5))) } returns listOf(testCommunity(5))

                operations.viewProfile(user).getOrThrow()

                verify { multipleRoleUsers wasNot Called }
                verify(exactly = 1) { communities.findByIds(any()) }
                confirmVerified(communities)
            }
        }

        @Nested
        inner class ModuleRuleTests {

            @Test
            fun `should throw if a community of the user does not exist`() {
                val user = testStudent {
                    memberOf = mutableListOf(CommunityId(5))
                    data = studentData {}
                }
                every { communities.findByIds(listOf(CommunityId(5))) } returns emptyList()

                assertFailsWith<IllegalStateException> { operations.viewProfile(user) }
            }
        }
    }

    @Nested
    inner class JoinCommunityTests {

        private val communityId = CommunityId(41)

        @Nested
        inner class HappyPathTests {

            @Test
            fun `should add the user to the community referencing the manager invite in the manager role and return the stored user`() {
                val joined = testManager {
                    memberOf(listOf(41))
                    data = managerData {}
                }
                every { managerInvites.findByCode(any()) } returns managerInvite()
                prepareCommunity(51)
                every { multipleRoleUsers.addCommunityMembership(user.id, communityId, CommunityRole.Manager) } returns joined

                val actual = operations.joinCommunity(user = user, inviteCode = "abcdefghjkmn").getOrThrow()

                assertSame(joined, actual)
                verify(exactly = 1) { multipleRoleUsers.addCommunityMembership(user.id, communityId, CommunityRole.Manager) }
            }

            @Test
            fun `should join if the user holds no roles`() {
                val noRoles = testMultipleRoleUser {}
                val joined = testManager {
                    memberOf(listOf(41))
                    data = managerData {}
                }
                every { managerInvites.findByCode(any()) } returns managerInvite()
                prepareCommunity(51)
                every { multipleRoleUsers.addCommunityMembership(noRoles.id, communityId, CommunityRole.Manager) } returns joined

                val actual = operations.joinCommunity(user = noRoles, inviteCode = "abcdefghjkmn").getOrThrow()

                assertSame(joined, actual)
            }

            @Test
            fun `should join if the invite expires one moment after the current time`() {
                val joined = testMultipleRoleUser {}
                every { managerInvites.findByCode(any()) } returns managerInvite(expiresAt = now.plusNanos(1))
                prepareCommunity(51)
                every { multipleRoleUsers.addCommunityMembership(user.id, communityId, CommunityRole.Manager) } returns joined

                val actual = operations.joinCommunity(user = user, inviteCode = "abcdefghjkmn").getOrThrow()

                assertSame(joined, actual)
            }

            @Test
            fun `should look the invite up by the lowercase entered code stored with Identity`() {
                val joined = testMultipleRoleUser {}
                every { managerInvites.findByCode(InviteCodeHash("abcdefghjkmn", HashAlgorithm.Identity)) } returns managerInvite()
                prepareCommunity(51)
                every { multipleRoleUsers.addCommunityMembership(user.id, communityId, CommunityRole.Manager) } returns joined

                val actual = operations.joinCommunity(user = user, inviteCode = "AbCdEfGhJkMn").getOrThrow()

                assertSame(joined, actual)
            }

            @Test
            fun `should join in the developer role if the user is a member of the community in another role only`() {
                val user = testManager {
                    memberOf(listOf(41))
                    data = managerData {}
                }
                val joined = testMultipleRoleUser {}
                prepareDeveloperInvite()
                prepareCommunity(52)
                every { multipleRoleUsers.addCommunityMembership(user.id, communityId, CommunityRole.Developer) } returns joined

                val actual = operations.joinCommunity(user = user, inviteCode = "abcdefghjkmn").getOrThrow()

                assertSame(joined, actual)
            }

            @Test
            fun `should join if the user holds the invite role in a different community only`() {
                val user = testDeveloper {
                    memberOf(listOf(99))
                    data = developerData {}
                }
                val joined = testMultipleRoleUser {}
                prepareDeveloperInvite()
                prepareCommunity(52)
                every { multipleRoleUsers.addCommunityMembership(user.id, communityId, CommunityRole.Developer) } returns joined

                val actual = operations.joinCommunity(user = user, inviteCode = "abcdefghjkmn").getOrThrow()

                assertSame(joined, actual)
            }

            @Test
            fun `should return the stored user unchanged if already a member in the developer invite role`() {
                val member = testDeveloper {
                    memberOf(listOf(41))
                    data = developerData {}
                }
                val stored = testDeveloper {
                    memberOf(listOf(41))
                    data = developerData {}
                }
                prepareDeveloperInvite()
                prepareCommunity(52)
                every { multipleRoleUsers.addCommunityMembership(member.id, communityId, CommunityRole.Developer) } returns stored

                val actual = operations.joinCommunity(user = member, inviteCode = "abcdefghjkmn").getOrThrow()

                assertSame(stored, actual)
            }

            @Test
            fun `should return the stored user unchanged if already a member in the manager invite role`() {
                val member = testManager {
                    memberOf(listOf(41))
                    data = managerData {}
                }
                val stored = testManager {
                    memberOf(listOf(41))
                    data = managerData {}
                }
                every { managerInvites.findByCode(any()) } returns managerInvite()
                prepareCommunity(51)
                every { multipleRoleUsers.addCommunityMembership(member.id, communityId, CommunityRole.Manager) } returns stored

                val actual = operations.joinCommunity(user = member, inviteCode = "abcdefghjkmn").getOrThrow()

                assertSame(stored, actual)
            }

            // Audit of operations against features.md: joinCommunity trusted a stale user (operation-audit branch).
            @Test
            @Tag("regression")
            fun `should add the membership and return the stored user even if the passed user looks like a member in the invite role`() {
                val staleMember = testDeveloper {
                    memberOf(listOf(41))
                    data = developerData {}
                }
                val stored = testDeveloper {
                    memberOf(listOf(41))
                    data = developerData {}
                }
                prepareDeveloperInvite()
                prepareCommunity(52)
                every {
                    multipleRoleUsers.addCommunityMembership(staleMember.id, communityId, CommunityRole.Developer)
                } returns stored

                val actual = operations.joinCommunity(user = staleMember, inviteCode = "abcdefghjkmn").getOrThrow()

                assertSame(stored, actual)
                verify(exactly = 1) {
                    multipleRoleUsers.addCommunityMembership(staleMember.id, communityId, CommunityRole.Developer)
                }
            }

            @Test
            fun `should first make a user without the invite role a member of the public community in that role`() {
                val user = testManager {
                    memberOf(listOf(41))
                    data = managerData {}
                }
                val joined = testMultipleRoleUser {}
                prepareDeveloperInvite()
                prepareCommunity(52)
                every { clock.instant() } returns now
                every { multipleRoleUsers.addCommunityMembership(user.id, any(), CommunityRole.Developer) } returns joined

                val actual = operations.joinCommunity(user = user, inviteCode = "abcdefghjkmn").getOrThrow()

                assertSame(joined, actual)
                verifyOrder {
                    multipleRoleUsers.addCommunityMembership(user.id, publicCommunityId, CommunityRole.Developer)
                    multipleRoleUsers.addCommunityMembership(user.id, communityId, CommunityRole.Developer)
                }
            }

            @Test
            fun `should not add a public community membership if the user already holds the invite role elsewhere`() {
                val user = testDeveloper {
                    memberOf(listOf(7))
                    data = developerData {}
                }
                val joined = testMultipleRoleUser {}
                prepareDeveloperInvite()
                prepareCommunity(52)
                every { clock.instant() } returns now
                every { multipleRoleUsers.addCommunityMembership(user.id, communityId, CommunityRole.Developer) } returns joined

                val actual = operations.joinCommunity(user = user, inviteCode = "abcdefghjkmn").getOrThrow()

                assertSame(joined, actual)
                verify(exactly = 0) { multipleRoleUsers.addCommunityMembership(user.id, publicCommunityId, any()) }
            }

            @Test
            fun `should join the public community by its invite code if the invite role is new`() {
                val user = testMultipleRoleUser {}
                val joined = testMultipleRoleUser {}
                prepareDeveloperInvite()
                every { communities.findByInvite(CommunityInviteId(52)) } returns testCommunity(publicCommunityId.value)
                every { clock.instant() } returns now
                every { multipleRoleUsers.addCommunityMembership(user.id, publicCommunityId, CommunityRole.Developer) } returns joined

                val actual = operations.joinCommunity(user = user, inviteCode = "abcdefghjkmn").getOrThrow()

                assertSame(joined, actual)
                verify(exactly = 0) { multipleRoleUsers.addCommunityMembership(user.id, communityId, any()) }
            }
        }

        @Nested
        inner class RefusalTests {

            @Test
            fun `should raise CommunityInviteCodeNotValidError with the entered code if no invite of either role matches`() {
                every { managerInvites.findByCode(any()) } returns null
                every { developerInvites.findByCode(any()) } returns null

                assertRaises(CommunityInviteCodeNotValidError("abcdefghjkmn")) {
                    operations.joinCommunity(user = testStudent { data = studentData {} }, inviteCode = "abcdefghjkmn")
                }

                verify { listOf(multipleRoleUsers, communities, clock) wasNot Called }
            }

            @Test
            fun `should keep spaces of the entered code when looking the invite up`() {
                every { managerInvites.findByCode(InviteCodeHash("abcdefghjkmn ", HashAlgorithm.Identity)) } returns null
                every { developerInvites.findByCode(InviteCodeHash("abcdefghjkmn ", HashAlgorithm.Identity)) } returns null

                assertRaises(CommunityInviteCodeNotValidError("abcdefghjkmn ")) {
                    operations.joinCommunity(user = testMultipleRoleUser {}, inviteCode = "abcdefghjkmn ")
                }
            }

            @Test
            fun `should raise CommunityInviteCodeExpiredError without joining if the invite expires at the current time`() {
                prepareDeveloperInvite(expiresAt = now)

                assertRaises(CommunityInviteCodeExpiredError("abcdefghjkmn")) {
                    operations.joinCommunity(user = testMultipleRoleUser {}, inviteCode = "abcdefghjkmn")
                }

                verify { listOf(multipleRoleUsers, communities) wasNot Called }
            }

            @Test
            fun `should raise CommunityInviteCodeExpiredError for a user already in the community`() {
                val member = testDeveloper {
                    memberOf(listOf(41))
                    data = developerData {}
                }
                prepareDeveloperInvite(expiresAt = now.minusSeconds(1))

                assertRaises(CommunityInviteCodeExpiredError("abcdefghjkmn")) {
                    operations.joinCommunity(user = member, inviteCode = "abcdefghjkmn")
                }
            }
        }

        @Nested
        inner class InvariantTests {

            @Test
            fun `should change neither the community nor its invites when joining`() {
                every { managerInvites.findByCode(any()) } returns managerInvite()
                prepareCommunity(51)
                every { multipleRoleUsers.addCommunityMembership(any(), any(), any()) } returns testMultipleRoleUser {}

                operations.joinCommunity(user = user, inviteCode = "abcdefghjkmn")

                verify(exactly = 0) {
                    communities.update(any<Community>())
                    managerInvites.update(any<CommunityInvite.Manager>())
                    managerInvites.remove(any<CommunityInvite.Manager>())
                    developerInvites.update(any<CommunityInvite.Developer>())
                    developerInvites.remove(any<CommunityInvite.Developer>())
                }
            }

            @Test
            fun `should change the user only through the membership port when joining`() {
                every { managerInvites.findByCode(any()) } returns managerInvite()
                prepareCommunity(51)
                every { multipleRoleUsers.addCommunityMembership(any(), any(), any()) } returns testMultipleRoleUser {}

                operations.joinCommunity(user = user, inviteCode = "abcdefghjkmn")

                verify(exactly = 0) { multipleRoleUsers.update(any<MultipleRoleUser>()) }
            }
        }

        @Nested
        inner class ModuleRuleTests {

            @Test
            fun `should read the current time once when joining`() {
                every { managerInvites.findByCode(any()) } returns managerInvite()
                prepareCommunity(51)
                every { multipleRoleUsers.addCommunityMembership(any(), any(), any()) } returns testMultipleRoleUser {}

                operations.joinCommunity(user = user, inviteCode = "abcdefghjkmn")

                verify(exactly = 1) { clock.instant() }
            }

            @Test
            fun `should propagate storage exceptions when joining`() {
                val user = testMultipleRoleUser {}
                val failure = IllegalStateException("storage failed")
                every { managerInvites.findByCode(any()) } returns managerInvite()
                prepareCommunity(51)
                every { multipleRoleUsers.addCommunityMembership(any(), any(), any()) } throws failure

                val actual = assertThrows(IllegalStateException::class.java) {
                    operations.joinCommunity(user = user, inviteCode = "abcdefghjkmn")
                }

                assertSame(failure, actual)
            }
        }

        private fun prepareDeveloperInvite(expiresAt: Instant = FAR_FUTURE) {
            every { managerInvites.findByCode(any()) } returns null
            every { developerInvites.findByCode(any()) } returns developerCommunityInvite {
                id = 52
                createdAt = Instant.EPOCH
                data = inviteData(expiresAt)
            }
        }

        private fun prepareCommunity(inviteId: Long) {
            every { communities.findByInvite(CommunityInviteId(inviteId)) } returns testCommunity(communityId.value)
        }

        private fun managerInvite(expiresAt: Instant = FAR_FUTURE): CommunityInvite.Manager = managerCommunityInvite {
            id = 51
            createdAt = Instant.EPOCH
            data = inviteData(expiresAt)
        }

        private fun inviteData(expiresAt: Instant): CommunityInviteData = communityInviteData {
            code("abcdefghjkmn", HashAlgorithm.Identity)
            this.expiresAt = expiresAt
        }
    }

    @Nested
    inner class RequestEmailChangeTests {

        private fun verifyNothingStoredOrSent() {
            verify(exactly = 0) {
                emailChangeRequests.save(any<EmailChangeRequestData>())
                emailChangeRequests.update(any<EmailChangeRequest>())
                mailSender.sendEmailChangeConfirmationCode(any(), any())
            }
        }

        @Nested
        inner class HappyPathTests {

            @Test
            fun `should save new request of the user with generated code, expiry and attempts if the user has no request`() {
                val data = slot<EmailChangeRequestData>()
                every { emailChangeRequests.save(capture(data)) } answers { savedRequest(firstArg()) }

                operations.requestEmailChange(user = user, email = "new@example.com")

                assertEquals(MultipleRoleUserId(0), data.captured.user.id)
                assertEquals("new@example.com", data.captured.email)
                assertEquals("87654321", data.captured.confirmationCode)
                assertEquals(Instant.parse("2026-01-01T00:15:00Z"), data.captured.expiresAt)
                assertEquals(3, data.captured.attemptsLeft)
            }

            @Test
            fun `should send confirmation code of the new request to the new email`() {
                operations.requestEmailChange(user = user, email = "new@example.com")

                verify(exactly = 1) {
                    mailSender.sendEmailChangeConfirmationCode(email = "new@example.com", confirmationCode = "87654321")
                }
            }

            @Test
            fun `should trim and lowercase the new email`() {
                val data = slot<EmailChangeRequestData>()
                every { emailChangeRequests.save(capture(data)) } answers { savedRequest(firstArg()) }

                operations.requestEmailChange(user = user, email = "  New@Example.COM ")

                assertEquals("new@example.com", data.captured.email)
            }

            @Test
            fun `should accept email of 255 characters`() {
                val email = "u".repeat(243) + "@example.com"

                operations.requestEmailChange(user = user, email = email).getOrThrow()

                verify(exactly = 1) { mailSender.sendEmailChangeConfirmationCode(email = email, confirmationCode = "87654321") }
            }

            @Test
            fun `should accept email of 255 code points outside the basic plane`() {
                val email = "😀".repeat(243) + "@example.com"

                operations.requestEmailChange(user = user, email = email).getOrThrow()

                verify(exactly = 1) { mailSender.sendEmailChangeConfirmationCode(email = email, confirmationCode = "87654321") }
            }

            @Test
            fun `should save the request for the email of a stale user if the stored email differs`() {
                val data = slot<EmailChangeRequestData>()
                every { emailChangeRequests.save(capture(data)) } answers { savedRequest(firstArg()) }

                operations.requestEmailChange(user = user, email = "old@example.com")

                assertEquals("old@example.com", data.captured.email)
                verify(exactly = 1) {
                    mailSender.sendEmailChangeConfirmationCode(email = "old@example.com", confirmationCode = "87654321")
                }
            }

            @Test
            fun `should overwrite an active request for another email with the new email, code, expiry and attempts`() {
                val existing = testEmailChangeRequest {
                    email = "other@example.com"
                    attemptsLeft = 2
                }
                every { emailChangeRequests.findByUser(MultipleRoleUserId(0)) } returns existing
                val updated = slot<EmailChangeRequest>()
                every { emailChangeRequests.update(capture(updated)) } answers { firstArg() }

                operations.requestEmailChange(user = user, email = "new@example.com")

                assertEquals(existing.id, updated.captured.id)
                assertEquals("new@example.com", updated.captured.data.email)
                assertEquals("87654321", updated.captured.data.confirmationCode)
                assertEquals(Instant.parse("2026-01-01T00:15:00Z"), updated.captured.data.expiresAt)
                assertEquals(3, updated.captured.data.attemptsLeft)
            }

            @Test
            fun `should send the new code to the new email if a request for another email is overwritten`() {
                every { emailChangeRequests.findByUser(MultipleRoleUserId(0)) } returns
                    testEmailChangeRequest { email = "other@example.com" }

                operations.requestEmailChange(user = user, email = "new@example.com")

                verify(exactly = 1) {
                    mailSender.sendEmailChangeConfirmationCode(email = "new@example.com", confirmationCode = "87654321")
                }
            }

            @Test
            fun `should overwrite a request for the same email expiring exactly now`() {
                every { emailChangeRequests.findByUser(MultipleRoleUserId(0)) } returns testEmailChangeRequest { expiresAt = now }
                val updated = slot<EmailChangeRequest>()
                every { emailChangeRequests.update(capture(updated)) } answers { firstArg() }

                operations.requestEmailChange(user = user, email = "new@example.com")

                assertEquals("87654321", updated.captured.data.confirmationCode)
                assertEquals(Instant.parse("2026-01-01T00:15:00Z"), updated.captured.data.expiresAt)
            }

            @Test
            fun `should keep a request for the same email expiring one moment later`() {
                every { emailChangeRequests.findByUser(MultipleRoleUserId(0)) } returns
                    testEmailChangeRequest { expiresAt = now.plusNanos(1) }

                operations.requestEmailChange(user = user, email = "new@example.com")

                verify(exactly = 1) {
                    mailSender.sendEmailChangeConfirmationCode(email = "new@example.com", confirmationCode = "12345678")
                }
            }

            @Test
            fun `should overwrite a request for the same email without attempts left`() {
                every { emailChangeRequests.findByUser(MultipleRoleUserId(0)) } returns testEmailChangeRequest { attemptsLeft = 0 }
                val updated = slot<EmailChangeRequest>()
                every { emailChangeRequests.update(capture(updated)) } answers { firstArg() }

                operations.requestEmailChange(user = user, email = "new@example.com")

                assertEquals("87654321", updated.captured.data.confirmationCode)
                assertEquals(3, updated.captured.data.attemptsLeft)
            }
        }

        @Nested
        inner class RefusalTests {

            @ParameterizedTest
            @ValueSource(strings = ["newexample.com", "new@@example.com", "a@b@example.com", "@example.com", "new@", "  "])
            fun `should raise InvalidEmailError and store nothing if email lacks exactly one at sign with non-empty parts`(email: String) {
                assertRaises(InvalidEmailError) {
                    operations.requestEmailChange(user = user, email = email)
                }

                verifyNothingStoredOrSent()
            }

            @Test
            fun `should raise InvalidEmailError and store nothing if email is longer than 255 characters`() {
                assertRaises(InvalidEmailError) {
                    operations.requestEmailChange(user = user, email = "u".repeat(244) + "@example.com")
                }

                verifyNothingStoredOrSent()
            }

            @Test
            fun `should raise InvalidEmailError if email is longer than 255 code points outside the basic plane`() {
                assertRaises(InvalidEmailError) {
                    operations.requestEmailChange(user = user, email = "😀".repeat(244) + "@example.com")
                }

                verifyNothingStoredOrSent()
            }

            @Test
            fun `should raise InvalidEmailError if flag emoji make the email longer than 255 code points`() {
                assertRaises(InvalidEmailError) {
                    operations.requestEmailChange(user = user, email = "🇷🇺".repeat(122) + "@example.com")
                }

                verifyNothingStoredOrSent()
            }

            @Test
            fun `should raise EmailUnchangedError and store or send nothing if the new email equals the current one`() {
                every { multipleRoleUsers.findByEmail("old@example.com") } returns user

                assertRaises(EmailUnchangedError) {
                    operations.requestEmailChange(user = user, email = "old@example.com")
                }

                verifyNothingStoredOrSent()
            }

            @Test
            fun `should raise EmailUnchangedError if the new email equals the current one after normalization`() {
                every { multipleRoleUsers.findByEmail("old@example.com") } returns user

                assertRaises(EmailUnchangedError) {
                    operations.requestEmailChange(user = user, email = " Old@Example.com ")
                }

                verifyNothingStoredOrSent()
            }

            @Test
            fun `should raise EmailUnchangedError and store or send nothing if the new email equals the stored email of a stale user`() {
                every { multipleRoleUsers.findByEmail("current@example.com") } returns
                    testMultipleRoleUser { email = "current@example.com" }

                assertRaises(EmailUnchangedError) {
                    operations.requestEmailChange(user = user, email = "current@example.com")
                }

                verifyNothingStoredOrSent()
            }

            @Test
            fun `should raise EmailAlreadyBoundError and store or send nothing if the new email is bound to another user`() {
                every { multipleRoleUsers.findByEmail("new@example.com") } returns otherUser(email = "new@example.com")

                assertRaises(EmailAlreadyBoundError) {
                    operations.requestEmailChange(user = user, email = "new@example.com")
                }

                verifyNothingStoredOrSent()
            }

            @Test
            fun `should raise EmailAlreadyBoundError if the normalized new email is bound to another user`() {
                every { multipleRoleUsers.findByEmail("new@example.com") } returns otherUser(email = "new@example.com")

                assertRaises(EmailAlreadyBoundError) {
                    operations.requestEmailChange(user = user, email = " New@Example.com")
                }
            }
        }

        @Nested
        inner class InvariantTests {

            @Test
            fun `should not change the email of the user when requesting`() {
                operations.requestEmailChange(user = user, email = "new@example.com")

                verify(exactly = 0) { multipleRoleUsers.update(any<MultipleRoleUser>()) }
            }

            @Test
            fun `should overwrite rather than add a request if the user already has one`() {
                every { emailChangeRequests.findByUser(MultipleRoleUserId(0)) } returns
                    testEmailChangeRequest { email = "other@example.com" }

                operations.requestEmailChange(user = user, email = "new@example.com")

                verify(exactly = 0) { emailChangeRequests.save(any<EmailChangeRequestData>()) }
            }

            @Test
            fun `should not change an active request for the same email`() {
                every { emailChangeRequests.findByUser(MultipleRoleUserId(0)) } returns testEmailChangeRequest {
                    expiresAt = Instant.parse("2026-01-01T00:05:00Z")
                    attemptsLeft = 2
                }

                operations.requestEmailChange(user = user, email = "new@example.com")

                verify(exactly = 0) {
                    emailChangeRequests.save(any<EmailChangeRequestData>())
                    emailChangeRequests.update(any<EmailChangeRequest>())
                }
            }

            @Test
            fun `should send the stored code again if the request for the same email is active`() {
                every { emailChangeRequests.findByUser(MultipleRoleUserId(0)) } returns testEmailChangeRequest()

                operations.requestEmailChange(user = user, email = "new@example.com")

                verify(exactly = 1) {
                    mailSender.sendEmailChangeConfirmationCode(email = "new@example.com", confirmationCode = "12345678")
                }
            }
        }

        @Nested
        inner class ModuleRuleTests {

            @Test
            fun `should not send the confirmation code if saving the new request fails`() {
                every { emailChangeRequests.save(any<EmailChangeRequestData>()) } throws IllegalStateException("database is down")

                assertFailsWith<IllegalStateException> {
                    operations.requestEmailChange(user = user, email = "new@example.com")
                }
                verify(exactly = 0) { mailSender.sendEmailChangeConfirmationCode(any(), any()) }
            }

            @Test
            fun `should not send the confirmation code if overwriting the request fails`() {
                every { emailChangeRequests.findByUser(MultipleRoleUserId(0)) } returns
                    testEmailChangeRequest { email = "other@example.com" }
                every { emailChangeRequests.update(any<EmailChangeRequest>()) } throws IllegalStateException("stale version")

                assertFailsWith<IllegalStateException> {
                    operations.requestEmailChange(user = user, email = "new@example.com")
                }
                verify(exactly = 0) { mailSender.sendEmailChangeConfirmationCode(any(), any()) }
            }

            @Test
            fun `should propagate mail sender exceptions`() {
                every { mailSender.sendEmailChangeConfirmationCode(any(), any()) } throws IllegalStateException("smtp is down")

                assertFailsWith<IllegalStateException> {
                    operations.requestEmailChange(user = user, email = "new@example.com")
                }
            }
        }
    }

    @Nested
    inner class ConfirmEmailChangeTests {

        private fun confirm(confirmationCode: String = "12345678") =
            operations.confirmEmailChange(user = user, confirmationCode = confirmationCode)

        @Nested
        inner class HappyPathTests {

            @Test
            fun `should replace the email of the user with the new email`() {
                every { emailChangeRequests.findByUser(MultipleRoleUserId(0)) } returns testEmailChangeRequest()
                val updated = slot<MultipleRoleUser>()
                every { multipleRoleUsers.update(capture(updated)) } answers { firstArg() }

                confirm()

                assertEquals(MultipleRoleUserId(0), updated.captured.id)
                assertEquals("new@example.com", updated.captured.data.email)
            }

            @Test
            fun `should return the user with the new email`() {
                every { emailChangeRequests.findByUser(MultipleRoleUserId(0)) } returns testEmailChangeRequest()

                val result = confirm().getOrThrow()

                assertEquals("new@example.com", result.data.email)
            }

            @Test
            fun `should change the stored user rather than the passed one`() {
                every { emailChangeRequests.findByUser(MultipleRoleUserId(0)) } returns testEmailChangeRequest()
                every { multipleRoleUsers.findById(MultipleRoleUserId(0)) } returns testUser(name = "Renamed")
                val updated = slot<MultipleRoleUser>()
                every { multipleRoleUsers.update(capture(updated)) } answers { firstArg() }

                confirm()

                assertEquals("Renamed", updated.captured.data.name)
            }

            @Test
            fun `should store one attempt less if the code matches`() {
                every { emailChangeRequests.findByUser(MultipleRoleUserId(0)) } returns testEmailChangeRequest()
                val updated = slot<EmailChangeRequest>()
                every { emailChangeRequests.update(capture(updated)) } answers { firstArg() }

                confirm()

                assertEquals(EmailChangeRequestId(37), updated.captured.id)
                assertEquals(2, updated.captured.data.attemptsLeft)
            }

            @Test
            fun `should remove the request after the change`() {
                every { emailChangeRequests.findByUser(MultipleRoleUserId(0)) } returns testEmailChangeRequest()

                confirm()

                verify(exactly = 1) { emailChangeRequests.remove(match<EmailChangeRequest> { it.id == EmailChangeRequestId(37) }) }
            }

            @Test
            fun `should notify the previous email with the nickname after the change`() {
                every { emailChangeRequests.findByUser(MultipleRoleUserId(0)) } returns testEmailChangeRequest()

                confirm()

                verify(exactly = 1) { mailSender.sendEmailChangedNotice(email = "old@example.com", name = "Nickname") }
            }

            @Test
            fun `should notify the stored previous email with the stored nickname if the passed user is stale`() {
                every { emailChangeRequests.findByUser(MultipleRoleUserId(0)) } returns testEmailChangeRequest()
                every { multipleRoleUsers.findById(MultipleRoleUserId(0)) } returns
                    testUser(name = "Renamed", email = "current@example.com")

                confirm()

                verify(exactly = 1) { mailSender.sendEmailChangedNotice(email = "current@example.com", name = "Renamed") }
            }

            @Test
            fun `should accept the code one moment before it expires`() {
                every { emailChangeRequests.findByUser(MultipleRoleUserId(0)) } returns
                    testEmailChangeRequest { expiresAt = now.plusNanos(1) }

                val result = confirm().getOrThrow()

                assertEquals("new@example.com", result.data.email)
            }

            @Test
            fun `should accept the code of the last attempt`() {
                every { emailChangeRequests.findByUser(MultipleRoleUserId(0)) } returns testEmailChangeRequest { attemptsLeft = 1 }

                val result = confirm().getOrThrow()

                assertEquals("new@example.com", result.data.email)
            }

            @Test
            fun `should accept a code with surrounding whitespace`() {
                every { emailChangeRequests.findByUser(MultipleRoleUserId(0)) } returns testEmailChangeRequest()

                val result = confirm(confirmationCode = " \t12345678 \n").getOrThrow()

                assertEquals("new@example.com", result.data.email)
            }

            // Audit of operations against features.md: confirmEmailChange refused an own e-mail (operation-audit branch).
            @Test
            @Tag("regression")
            fun `should change the email if the new email is already bound to the same user`() {
                every { emailChangeRequests.findByUser(MultipleRoleUserId(0)) } returns testEmailChangeRequest()
                every { multipleRoleUsers.findByEmail("new@example.com") } returns testUser(email = "new@example.com")

                val result = confirm().getOrThrow()

                assertEquals("new@example.com", result.data.email)
            }
        }

        @Nested
        inner class RefusalTests {

            @Test
            fun `should raise EmailChangeRequestNotExistsError if the user has no request`() {
                assertRaises(EmailChangeRequestNotExistsError) {
                    confirm()
                }
            }

            @Test
            fun `should raise ConfirmationCodeExpiredError if the code expires exactly now`() {
                every { emailChangeRequests.findByUser(MultipleRoleUserId(0)) } returns testEmailChangeRequest { expiresAt = now }

                assertRaises(ConfirmationCodeExpiredError) {
                    confirm()
                }
            }

            @Test
            fun `should raise ConfirmationAttemptsExhaustedError if no attempts are left`() {
                every { emailChangeRequests.findByUser(MultipleRoleUserId(0)) } returns testEmailChangeRequest { attemptsLeft = 0 }

                assertRaises(ConfirmationAttemptsExhaustedError) {
                    confirm()
                }
            }

            @Test
            fun `should raise InvalidConfirmationCodeError if the code differs`() {
                every { emailChangeRequests.findByUser(MultipleRoleUserId(0)) } returns testEmailChangeRequest()

                assertRaises(InvalidConfirmationCodeError) {
                    confirm(confirmationCode = "00000000")
                }
            }

            @Test
            fun `should raise InvalidConfirmationCodeError if the code of the last attempt differs`() {
                every { emailChangeRequests.findByUser(MultipleRoleUserId(0)) } returns testEmailChangeRequest { attemptsLeft = 1 }

                assertRaises(InvalidConfirmationCodeError) {
                    confirm(confirmationCode = "00000000")
                }
            }

            @Test
            fun `should not accept a code with whitespace inside`() {
                every { emailChangeRequests.findByUser(MultipleRoleUserId(0)) } returns testEmailChangeRequest()

                assertRaises(InvalidConfirmationCodeError) {
                    confirm(confirmationCode = "1234 5678")
                }
            }

            @Test
            fun `should raise EmailAlreadyBoundError and spend the attempt if the new email was bound to another user after the request`() {
                every { emailChangeRequests.findByUser(MultipleRoleUserId(0)) } returns testEmailChangeRequest()
                every { multipleRoleUsers.findByEmail("new@example.com") } returns otherUser(email = "new@example.com")

                assertRaises(EmailAlreadyBoundError) {
                    confirm()
                }
                verify(exactly = 1) { emailChangeRequests.update(match<EmailChangeRequest> { it.data.attemptsLeft == 2 }) }
                verify(exactly = 0) {
                    multipleRoleUsers.update(any<MultipleRoleUser>())
                    emailChangeRequests.remove(any<EmailChangeRequest>())
                    mailSender.sendEmailChangedNotice(any(), any())
                }
            }
        }

        @Nested
        inner class InvariantTests {

            @Test
            fun `should not spend an attempt if the code expired`() {
                every { emailChangeRequests.findByUser(MultipleRoleUserId(0)) } returns testEmailChangeRequest { expiresAt = now }

                confirm()

                verify(exactly = 0) { emailChangeRequests.update(any<EmailChangeRequest>()) }
            }

            @Test
            fun `should not spend an attempt if no attempts are left`() {
                every { emailChangeRequests.findByUser(MultipleRoleUserId(0)) } returns testEmailChangeRequest { attemptsLeft = 0 }

                confirm()

                verify(exactly = 0) { emailChangeRequests.update(any<EmailChangeRequest>()) }
            }

            @Test
            fun `should store one attempt less if the code differs`() {
                every { emailChangeRequests.findByUser(MultipleRoleUserId(0)) } returns testEmailChangeRequest()
                val updated = slot<EmailChangeRequest>()
                every { emailChangeRequests.update(capture(updated)) } answers { firstArg() }

                confirm(confirmationCode = "00000000")

                assertEquals(2, updated.captured.data.attemptsLeft)
                assertEquals("12345678", updated.captured.data.confirmationCode)
                assertEquals("new@example.com", updated.captured.data.email)
            }

            @Test
            fun `should keep access code, nickname, roles and communities of the user`() {
                every { emailChangeRequests.findByUser(MultipleRoleUserId(0)) } returns testEmailChangeRequest()
                val updated = slot<MultipleRoleUser>()
                every { multipleRoleUsers.update(capture(updated)) } answers { firstArg() }

                confirm()

                assertEquals("token", updated.captured.data.accessTokenHash.value)
                assertEquals(HashAlgorithm.Identity, updated.captured.data.accessTokenHash.algorithm)
                assertEquals("Nickname", updated.captured.data.name)
                val role = assertIs<Student>(updated.captured.data.roles.single())
                assertEquals(listOf(CommunityId(5)), role.memberOf.ids)
            }

            @Test
            fun `should keep the email and the request and send no notice if the code differs`() {
                every { emailChangeRequests.findByUser(MultipleRoleUserId(0)) } returns testEmailChangeRequest()

                confirm(confirmationCode = "00000000")

                verify(exactly = 0) {
                    multipleRoleUsers.update(any<MultipleRoleUser>())
                    emailChangeRequests.remove(any<EmailChangeRequest>())
                    mailSender.sendEmailChangedNotice(any(), any())
                }
            }

            @Test
            fun `should keep the email if the code expired`() {
                every { emailChangeRequests.findByUser(MultipleRoleUserId(0)) } returns testEmailChangeRequest { expiresAt = now }

                confirm()

                verify(exactly = 0) { multipleRoleUsers.update(any<MultipleRoleUser>()) }
            }

            @Test
            fun `should keep the email if no attempts are left`() {
                every { emailChangeRequests.findByUser(MultipleRoleUserId(0)) } returns testEmailChangeRequest { attemptsLeft = 0 }

                confirm()

                verify(exactly = 0) { multipleRoleUsers.update(any<MultipleRoleUser>()) }
            }

            @ParameterizedTest
            @ValueSource(strings = ["12345678", "00000000"])
            fun `should propagate the storage exception and keep the email if spending the attempt fails`(confirmationCode: String) {
                every { emailChangeRequests.findByUser(MultipleRoleUserId(0)) } returns testEmailChangeRequest()
                every { emailChangeRequests.update(any<EmailChangeRequest>()) } throws IllegalStateException("stale version")

                assertFailsWith<IllegalStateException> {
                    confirm(confirmationCode = confirmationCode)
                }
                verify(exactly = 0) { multipleRoleUsers.update(any<MultipleRoleUser>()) }
            }
        }

        @Nested
        inner class ModuleRuleTests {

            @Test
            fun `should throw IllegalStateException if the user of the request no longer exists in storage`() {
                every { emailChangeRequests.findByUser(MultipleRoleUserId(0)) } returns testEmailChangeRequest()
                every { multipleRoleUsers.findById(MultipleRoleUserId(0)) } returns null

                assertFailsWith<IllegalStateException> {
                    confirm()
                }
            }

            @Test
            fun `should not send the notice if updating the user fails`() {
                every { emailChangeRequests.findByUser(MultipleRoleUserId(0)) } returns testEmailChangeRequest()
                every { multipleRoleUsers.update(any<MultipleRoleUser>()) } throws IllegalStateException("stale version")

                assertFailsWith<IllegalStateException> {
                    confirm()
                }
                verify(exactly = 0) { mailSender.sendEmailChangedNotice(any(), any()) }
            }

            @Test
            fun `should not send the notice if removing the request fails`() {
                every { emailChangeRequests.findByUser(MultipleRoleUserId(0)) } returns testEmailChangeRequest()
                every { emailChangeRequests.remove(any<EmailChangeRequest>()) } throws IllegalStateException("stale version")

                assertFailsWith<IllegalStateException> {
                    confirm()
                }
                verify(exactly = 0) { mailSender.sendEmailChangedNotice(any(), any()) }
            }

            @Test
            fun `should propagate mail sender exceptions`() {
                every { emailChangeRequests.findByUser(MultipleRoleUserId(0)) } returns testEmailChangeRequest()
                every { mailSender.sendEmailChangedNotice(any(), any()) } throws IllegalStateException("smtp is down")

                assertFailsWith<IllegalStateException> {
                    confirm()
                }
            }
        }
    }

    private companion object {
        val FAR_FUTURE: Instant = Instant.parse("2030-01-01T00:00:00Z")
    }
}
