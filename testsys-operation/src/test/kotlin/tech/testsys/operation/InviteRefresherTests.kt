package tech.testsys.operation

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import tech.testsys.domain.builder.api.`class`
import tech.testsys.domain.builder.api.classInvite
import tech.testsys.domain.builder.api.classInviteData
import tech.testsys.domain.builder.api.communityInviteData
import tech.testsys.domain.builder.api.managerCommunityInvite
import tech.testsys.domain.builder.api.managerData
import tech.testsys.domain.contract.persistence.repository.ClassInviteRepository
import tech.testsys.domain.contract.persistence.repository.ClassRepository
import tech.testsys.domain.contract.persistence.repository.CommunityRepository
import tech.testsys.domain.contract.persistence.repository.DeveloperCommunityInviteRepository
import tech.testsys.domain.contract.persistence.repository.ManagerCommunityInviteRepository
import tech.testsys.domain.contract.persistence.repository.MultipleRoleUserRepository
import tech.testsys.domain.model.group.ClassId
import tech.testsys.domain.model.group.ClassInvite
import tech.testsys.domain.model.group.ClassInviteId
import tech.testsys.domain.model.group.CommunityId
import tech.testsys.domain.model.group.CommunityInvite
import tech.testsys.domain.model.group.CommunityInviteId
import tech.testsys.domain.model.user.HashAlgorithm
import tech.testsys.operation.config.ClassInviteConfig
import tech.testsys.operation.config.CommunityInviteConfig
import tech.testsys.operation.error.MissedManagerRoleError
import tech.testsys.operation.error.OperationException
import tech.testsys.operation.error.OperationResult
import tech.testsys.operation.error.RefreshClassInviteError
import tech.testsys.operation.error.RefreshCommunityInviteError
import tech.testsys.operation.user.AdministratorOperations
import tech.testsys.operation.user.ManagerOperations
import tech.testsys.operation.util.testAdministrator
import tech.testsys.operation.util.testCommunity
import tech.testsys.operation.util.testManager
import tech.testsys.operation.util.testStudyClass
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

class InviteRefresherTests {

    private val classInvites = mockk<ClassInviteRepository>()
    private val managerInvites = mockk<ManagerCommunityInviteRepository>()
    private val developerInvites = mockk<DeveloperCommunityInviteRepository>()
    private val classes = mockk<ClassRepository>()
    private val communities = mockk<CommunityRepository>()
    private val users = mockk<MultipleRoleUserRepository>()
    private val managerOperations = mockk<ManagerOperations>()
    private val administratorOperations = mockk<AdministratorOperations>()
    private val classConfig = mockk<ClassInviteConfig>()
    private val communityConfig = mockk<CommunityInviteConfig>()
    private val clock = mockk<Clock>()
    private val executor = mockk<ScheduledExecutorService>()
    private val passes = mutableListOf<Runnable>()
    private val refresher = InviteRefresher(
        classInvites = classInvites,
        managerInvites = managerInvites,
        developerInvites = developerInvites,
        classes = classes,
        communities = communities,
        users = users,
        managerOperations = managerOperations,
        administratorOperations = administratorOperations,
        classInviteConfig = classConfig,
        communityInviteConfig = communityConfig,
        clock = clock,
        executor = executor,
    )
    private val now = Instant.parse("2026-01-01T10:00:00Z")
    private val classPeriod = Duration.ofMinutes(5)
    private val communityPeriod = Duration.ofMinutes(7)

    init {
        every { classConfig.refreshPeriod } returns classPeriod
        every { communityConfig.refreshPeriod } returns communityPeriod
        every { clock.instant() } returns now
        every { executor.scheduleWithFixedDelay(capture(passes), any(), any(), any()) } returns mockk(relaxed = true)
    }

    @Nested
    inner class StartTests {

        @Test
        fun `should schedule the class and community passes immediately with their refresh periods`() {
            refresher.start()

            verify(exactly = 1) { executor.scheduleWithFixedDelay(any(), 0L, classPeriod.toNanos(), TimeUnit.NANOSECONDS) }
            verify(exactly = 1) { executor.scheduleWithFixedDelay(any(), 0L, communityPeriod.toNanos(), TimeUnit.NANOSECONDS) }
        }

        @Test
        fun `should fail on a second start without scheduling again`() {
            refresher.start()

            assertThrows(IllegalStateException::class.java) { refresher.start() }

            verify(exactly = 2) { executor.scheduleWithFixedDelay(any(), any(), any(), any()) }
        }

        @Test
        fun `should fail without scheduling if a refresh period is not positive`() {
            every { communityConfig.refreshPeriod } returns Duration.ZERO

            assertThrows(IllegalStateException::class.java) { refresher.start() }

            verify(exactly = 0) { executor.scheduleWithFixedDelay(any(), any(), any(), any()) }
        }
    }

    @Nested
    inner class PassTests {

        private val owner = testManager { data = managerData {} }
        private val administrator = testAdministrator {}

        @Test
        fun `should refresh every expired class invite on behalf of the class owner`() {
            prepareClass(inviteId = 1, classId = 11)
            prepareClass(inviteId = 2, classId = 12)
            every { classInvites.findExpired(now) } returns listOf(ClassInviteId(1), ClassInviteId(2))
            every { managerOperations.refreshClassInvite(owner, any()) } answers { classInviteResult() }
            refresher.start()

            passes[0].run()

            verify(exactly = 1) { managerOperations.refreshClassInvite(owner, ClassId(11)) }
            verify(exactly = 1) { managerOperations.refreshClassInvite(owner, ClassId(12)) }
            verify(exactly = 0) { administratorOperations.refreshCommunityInvite(any(), any(), any()) }
        }

        @Test
        fun `should refresh expired community invites of both roles on behalf of the community owner`() {
            prepareCommunity(inviteId = 5, communityId = 41)
            prepareCommunity(inviteId = 6, communityId = 42)
            every { managerInvites.findExpired(now) } returns listOf(CommunityInviteId(5))
            every { developerInvites.findExpired(now) } returns listOf(CommunityInviteId(6))
            every { administratorOperations.refreshCommunityInvite(administrator, any(), any()) } answers { communityInviteResult() }
            refresher.start()

            passes[1].run()

            verify(exactly = 1) {
                administratorOperations.refreshCommunityInvite(administrator, CommunityId(41), CommunityInvite.Kind.Manager)
            }
            verify(exactly = 1) {
                administratorOperations.refreshCommunityInvite(administrator, CommunityId(42), CommunityInvite.Kind.Developer)
            }
        }

        @Test
        fun `should continue the pass if a refresh returns an error such as an owner without the Manager role`() {
            prepareClass(inviteId = 1, classId = 11)
            prepareClass(inviteId = 2, classId = 12)
            every { classInvites.findExpired(now) } returns listOf(ClassInviteId(1), ClassInviteId(2))
            every { managerOperations.refreshClassInvite(owner, ClassId(11)) } returns OperationResult.Error(
                error = MissedManagerRoleError,
                failure = OperationException(MissedManagerRoleError, IllegalStateException("owner lost the role")),
            )
            every { managerOperations.refreshClassInvite(owner, ClassId(12)) } answers { classInviteResult() }
            refresher.start()

            assertDoesNotThrow { passes[0].run() }

            verify(exactly = 1) { managerOperations.refreshClassInvite(owner, ClassId(12)) }
        }

        @Test
        fun `should refresh the remaining invites if one refresh throws`() {
            prepareClass(inviteId = 1, classId = 11)
            prepareClass(inviteId = 2, classId = 12)
            every { classInvites.findExpired(now) } returns listOf(ClassInviteId(1), ClassInviteId(2))
            every { managerOperations.refreshClassInvite(owner, ClassId(11)) } throws IllegalStateException("Stale invite version")
            every { managerOperations.refreshClassInvite(owner, ClassId(12)) } answers { classInviteResult() }
            refresher.start()

            assertDoesNotThrow { passes[0].run() }

            verify(exactly = 1) { managerOperations.refreshClassInvite(owner, ClassId(12)) }
        }

        @Test
        fun `should skip an invite whose class was removed after the search`() {
            every { classInvites.findExpired(now) } returns listOf(ClassInviteId(1))
            every { classes.findByInvite(ClassInviteId(1)) } returns null
            refresher.start()

            passes[0].run()

            verify(exactly = 0) { managerOperations.refreshClassInvite(any(), any()) }
        }

        @Test
        fun `should skip an invite whose community was removed after the search`() {
            every { managerInvites.findExpired(now) } returns listOf(CommunityInviteId(5))
            every { developerInvites.findExpired(now) } returns emptyList()
            every { communities.findByInvite(CommunityInviteId(5)) } returns null
            refresher.start()

            passes[1].run()

            verify(exactly = 0) { administratorOperations.refreshCommunityInvite(any(), any(), any()) }
        }

        @Test
        fun `should refresh the remaining invites if the owner of a community does not exist`() {
            prepareCommunity(inviteId = 6, communityId = 42)
            every { managerInvites.findExpired(now) } returns listOf(CommunityInviteId(5))
            every { developerInvites.findExpired(now) } returns listOf(CommunityInviteId(6))
            every { communities.findByInvite(CommunityInviteId(5)) } returns testCommunity(41)
            every { users.findById(administrator.id) } returnsMany listOf(null, administrator)
            every { administratorOperations.refreshCommunityInvite(administrator, any(), any()) } answers { communityInviteResult() }
            refresher.start()

            assertDoesNotThrow { passes[1].run() }

            verify(exactly = 0) { administratorOperations.refreshCommunityInvite(any(), CommunityId(41), any()) }
            verify(exactly = 1) {
                administratorOperations.refreshCommunityInvite(administrator, CommunityId(42), CommunityInvite.Kind.Developer)
            }
        }

        @Test
        fun `should not let a failed search escape the pass`() {
            every { managerInvites.findExpired(now) } throws IllegalStateException("Storage unavailable")
            refresher.start()

            assertDoesNotThrow { passes[1].run() }

            verify(exactly = 0) { administratorOperations.refreshCommunityInvite(any(), any(), any()) }
        }

        private fun prepareClass(inviteId: Long, classId: Long) {
            val ownerId = owner.id
            every { classes.findByInvite(ClassInviteId(inviteId)) } returns `class` {
                id = classId
                createdAt = Instant.EPOCH
                data = testStudyClass { this.owner = ownerId }.data
            }
            every { users.findById(ownerId) } returns owner
        }

        private fun prepareCommunity(inviteId: Long, communityId: Long) {
            every { communities.findByInvite(CommunityInviteId(inviteId)) } returns testCommunity(communityId)
            every { users.findById(administrator.id) } returns administrator
        }

        private fun classInviteResult(): OperationResult<ClassInvite, RefreshClassInviteError> = OperationResult.Success(
            classInvite {
                id = 31
                createdAt = Instant.EPOCH
                data = classInviteData {
                    code("pqrstuvwxyz2", HashAlgorithm.Identity)
                    expiresAt = now.plusSeconds(60)
                }
            },
        )

        private fun communityInviteResult(): OperationResult<CommunityInvite, RefreshCommunityInviteError> = OperationResult.Success(
            managerCommunityInvite {
                id = 51
                createdAt = Instant.EPOCH
                data = communityInviteData {
                    code("pqrstuvwxyz2", HashAlgorithm.Identity)
                    expiresAt = now.plusSeconds(60)
                }
            },
        )
    }

    @Nested
    inner class CloseTests {

        @Test
        fun `should shut the executor down on every close`() {
            every { executor.shutdownNow() } returns emptyList()

            refresher.close()
            refresher.close()

            verify(exactly = 2) { executor.shutdownNow() }
        }
    }
}
