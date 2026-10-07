package tech.testsys.operation

import tech.testsys.domain.contract.persistence.repository.ClassInviteRepository
import tech.testsys.domain.contract.persistence.repository.ClassRepository
import tech.testsys.domain.contract.persistence.repository.CommunityRepository
import tech.testsys.domain.contract.persistence.repository.DeveloperCommunityInviteRepository
import tech.testsys.domain.contract.persistence.repository.ManagerCommunityInviteRepository
import tech.testsys.domain.contract.persistence.repository.MultipleRoleUserRepository
import tech.testsys.domain.model.group.ClassInviteId
import tech.testsys.domain.model.group.CommunityInvite
import tech.testsys.domain.model.group.CommunityInviteId
import tech.testsys.operation.config.ClassInviteConfig
import tech.testsys.operation.config.CommunityInviteConfig
import tech.testsys.operation.error.OperationResult
import tech.testsys.operation.user.AdministratorOperations
import tech.testsys.operation.user.ManagerOperations
import java.time.Clock
import java.time.Duration
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Periodically replaces expired invite codes on behalf of the owner of their class or community through
 * [ManagerOperations.refreshClassInvite] and [AdministratorOperations.refreshCommunityInvite] on [executor],
 * logging each failure without stopping other invites or later passes.
 * [close] shuts [executor] down, interrupting a running pass; repeated calls are safe.
 *
 * @since %CURRENT_VERSION%
 */
@Suppress("LongParameterList")
class InviteRefresher(
    private val classInvites: ClassInviteRepository,
    private val managerInvites: ManagerCommunityInviteRepository,
    private val developerInvites: DeveloperCommunityInviteRepository,
    private val classes: ClassRepository,
    private val communities: CommunityRepository,
    private val users: MultipleRoleUserRepository,
    private val managerOperations: ManagerOperations,
    private val administratorOperations: AdministratorOperations,
    private val classInviteConfig: ClassInviteConfig,
    private val communityInviteConfig: CommunityInviteConfig,
    private val clock: Clock,
    private val executor: ScheduledExecutorService,
) : AutoCloseable {

    private val started = AtomicBoolean(false)

    /**
     * Schedules both passes on [executor]: the first runs immediately, the next after the refresh period.
     * Called once at application startup.
     *
     * @throws IllegalStateException if already started or a refresh period is zero or negative.
     * @since %CURRENT_VERSION%
     */
    fun start() {
        val classPeriod = classInviteConfig.refreshPeriod
        val communityPeriod = communityInviteConfig.refreshPeriod
        check(classPeriod > Duration.ZERO) { "Class invite refresh period must be positive, got $classPeriod" }
        check(communityPeriod > Duration.ZERO) { "Community invite refresh period must be positive, got $communityPeriod" }
        check(started.compareAndSet(false, true)) { "Invite refresher is already started" }
        schedule(classPeriod) { refreshClassInvites() }
        schedule(communityPeriod) { refreshCommunityInvites() }
    }

    override fun close() {
        executor.shutdownNow()
    }

    private fun schedule(period: Duration, pass: () -> Unit) {
        executor.scheduleWithFixedDelay(
            { guarded("invite refresh pass") { pass() } },
            0,
            period.toNanos(),
            TimeUnit.NANOSECONDS,
        )
    }

    private fun refreshClassInvites() {
        classInvites.findExpired(clock.instant()).forEach { inviteId ->
            val subject = "class invite id=${inviteId.value}"
            guarded(subject) { refreshClassInvite(inviteId, subject) }
        }
    }

    private fun refreshCommunityInvites() {
        val now = clock.instant()
        val expired = managerInvites.findExpired(now).map { it to CommunityInvite.Kind.Manager } +
            developerInvites.findExpired(now).map { it to CommunityInvite.Kind.Developer }
        expired.forEach { (inviteId, kind) ->
            val subject = "community invite id=${inviteId.value}"
            guarded(subject) { refreshCommunityInvite(inviteId, kind, subject) }
        }
    }

    // An invite whose group was removed after the search has nothing left to replace.
    private fun refreshClassInvite(inviteId: ClassInviteId, subject: String) {
        val studyClass = classes.findByInvite(inviteId) ?: return
        val owner = checkNotNull(users.findById(studyClass.data.owner.id)) {
            "Owner id=${studyClass.data.owner.id.value} of class id=${studyClass.id.value} does not exist"
        }
        logError(subject, managerOperations.refreshClassInvite(user = owner, classId = studyClass.id))
    }

    private fun refreshCommunityInvite(inviteId: CommunityInviteId, kind: CommunityInvite.Kind, subject: String) {
        val community = communities.findByInvite(inviteId) ?: return
        val owner = checkNotNull(users.findById(community.data.owner.id)) {
            "Owner id=${community.data.owner.id.value} of community id=${community.id.value} does not exist"
        }
        logError(subject, administratorOperations.refreshCommunityInvite(user = owner, communityId = community.id, kind = kind))
    }

    private fun logError(subject: String, result: OperationResult<*, *>) {
        if (result is OperationResult.Error<*>) {
            logger.log(System.Logger.Level.WARNING, "Failed to refresh $subject: ${result.error}", result.failure)
        }
    }

    // Any exception must be contained: an exception escaping a scheduled pass would cancel all later passes.
    @Suppress("TooGenericExceptionCaught")
    private fun guarded(subject: String, action: () -> Unit) {
        try {
            action()
        } catch (exception: Exception) {
            logger.log(System.Logger.Level.WARNING, "Failed to refresh $subject", exception)
        }
    }

    private companion object {
        val logger: System.Logger = System.getLogger(InviteRefresher::class.java.name)
    }
}
