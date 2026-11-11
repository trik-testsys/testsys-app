package tech.testsys.domain.contract.persistence.repository

import tech.testsys.domain.contract.persistence.ClassFilter
import tech.testsys.domain.contract.persistence.CompetitionFilter
import tech.testsys.domain.contract.persistence.ObserverContestFilter
import tech.testsys.domain.contract.persistence.Page
import tech.testsys.domain.contract.persistence.Pagination
import tech.testsys.domain.model.group.Class
import tech.testsys.domain.model.group.ClassData
import tech.testsys.domain.model.group.ClassId
import tech.testsys.domain.model.group.ClassInvite
import tech.testsys.domain.model.group.ClassInviteData
import tech.testsys.domain.model.group.ClassInviteId
import tech.testsys.domain.model.group.Community
import tech.testsys.domain.model.group.CommunityData
import tech.testsys.domain.model.group.CommunityId
import tech.testsys.domain.model.group.CommunityInvite
import tech.testsys.domain.model.group.CommunityInviteData
import tech.testsys.domain.model.group.CommunityInviteId
import tech.testsys.domain.model.group.Competition
import tech.testsys.domain.model.group.CompetitionData
import tech.testsys.domain.model.group.CompetitionId
import tech.testsys.domain.model.group.InviteCodeHash
import tech.testsys.domain.model.group.RawInviteCodeDependency
import tech.testsys.domain.model.task.Contest
import tech.testsys.domain.model.task.ContestId
import tech.testsys.domain.model.user.MultipleRoleUserId
import java.time.Instant

/**
 * Persistence port for [Class] entities. The invite of a class is fixed on creation; [save] needs a saved invite.
 *
 * @since 1.0.0-beta.1
 */
interface ClassRepository : EntityRepository<ClassData, ClassId, Class> {

    /**
     * Synchronously finds current classes owned by [ownerId], without changing stored state.
     * Repeated calls reflect current data; storage exceptions propagate to the caller.
     *
     * @param ownerId the manager whose own classes are included; other owners are excluded before paging and counting.
     * @param pagination the requested page; id ascending is the default order and breaks ties unless explicitly sorted.
     * @param filter conditions combined with AND before paging and counting; unspecified conditions do not restrict selection.
     * @return owned classes, the original pagination and exact filtered total; absent matches or missing pages are empty.
     * @since 1.0.0-beta.1
     */
    fun findAvailableToManager(ownerId: MultipleRoleUserId, pagination: Pagination, filter: ClassFilter = ClassFilter()): Page<Class>

    /**
     * Synchronously and atomically enrolls [studentId] in [classId] unless already enrolled, leaving other class data unchanged.
     * The caller checks the student role; storage exceptions propagate to the caller.
     *
     * @param classId the class to enroll the student in.
     * @param studentId the enrolled user; an already enrolled user is not added twice.
     * @return the stored class after enrollment.
     * @throws IllegalArgumentException if the class does not exist.
     * @since 1.0.0-beta.1
     */
    fun addStudent(classId: ClassId, studentId: MultipleRoleUserId): Class

    /**
     * Synchronously saves a new invite from [invite] and a new class referencing it, both or none.
     * Storage exceptions, including an invite code already held by another class invite, propagate and leave nothing saved.
     *
     * @param invite the data of the invite of the new class.
     * @param data the data of the new class computed from the id assigned to its invite.
     * @return the saved class.
     * @since 1.0.0-beta.1
     */
    fun saveWithInvite(invite: ClassInviteData, data: (ClassInviteId) -> ClassData): Class

    /**
     * Synchronously finds the class referencing [inviteId], without changing stored state.
     *
     * @param inviteId the invite of the searched class.
     * @return the class of the invite, or `null` if no class references it.
     * @since 1.0.0-beta.1
     */
    fun findByInvite(inviteId: ClassInviteId): Class?
}

/**
 * Persistence port for [ClassInvite] entities.
 *
 * @since 1.0.0-beta.1
 */
interface ClassInviteRepository : EntityRepository<ClassInviteData, ClassInviteId, ClassInvite> {

    /**
     * Synchronously finds the invite whose stored value and algorithm equal [codeHash], without changing stored state.
     *
     * @param codeHash the stored representation of the searched code; the caller normalizes and hashes the input.
     * @return the matching invite, or `null` if there is none.
     * @since 1.0.0-beta.1
     */
    @RawInviteCodeDependency(reason = "Finds the invite by comparing the stored value produced by a deterministic algorithm.")
    fun findByCode(codeHash: InviteCodeHash): ClassInvite?

    /**
     * Synchronously finds invites whose expiration moment is at or before [now], without changing stored state.
     *
     * @param now the moment compared with the expiration moments.
     * @return the ids of the expired invites in ascending order; empty if there are none.
     * @since 1.0.0-beta.1
     */
    fun findExpired(now: Instant): List<ClassInviteId>
}

/**
 * Persistence port for [Community] entities. The invites of a community are fixed on creation; [save] needs saved invites.
 *
 * @since 1.0.0-beta.1
 */
interface CommunityRepository : EntityRepository<CommunityData, CommunityId, Community> {

    /**
     * Synchronously saves new manager and developer invites and a new community referencing them, all or none.
     * Storage exceptions, including an invite code already held by another community invite, propagate and leave nothing saved.
     *
     * @param managerInvite the data of the manager-role invite of the new community.
     * @param developerInvite the data of the developer-role invite of the new community.
     * @param data the data of the new community computed from the ids assigned to its manager and developer invites.
     * @return the saved community.
     * @since 1.0.0-beta.1
     */
    fun saveWithInvites(
        managerInvite: CommunityInviteData,
        developerInvite: CommunityInviteData,
        data: (CommunityInviteId, CommunityInviteId) -> CommunityData,
    ): Community

    /**
     * Synchronously inserts a new community with exactly [id] and its two new invites, all or none.
     * An occupied id or invite code fails the transaction; this method never updates an existing community.
     *
     * @param id the required id of the new community.
     * @param managerInvite the manager-role invite data.
     * @param developerInvite the developer-role invite data.
     * @param data community data computed from the saved invite ids.
     * @return the inserted community with [id].
     * @since 1.0.0-beta.1
     */
    fun saveWithIdAndInvites(
        id: CommunityId,
        managerInvite: CommunityInviteData,
        developerInvite: CommunityInviteData,
        data: (CommunityInviteId, CommunityInviteId) -> CommunityData,
    ): Community

    /**
     * Synchronously finds the community referencing [inviteId] in either role, without changing stored state.
     *
     * @param inviteId the invite of the searched community.
     * @return the community of the invite, or `null` if no community references it.
     * @since 1.0.0-beta.1
     */
    fun findByInvite(inviteId: CommunityInviteId): Community?

    /**
     * Synchronously finds communities created by [ownerId], without changing stored state.
     * Storage exceptions propagate to the caller.
     *
     * @param ownerId the creator of the communities.
     * @return the communities of the owner in ascending id order; empty if there are none.
     * @since 1.0.0-beta.1
     */
    fun findByOwner(ownerId: MultipleRoleUserId): List<Community>
}

/**
 * Persistence port for the [Invite] variant of [CommunityInvite] entities; invites of other variants are absent from it.
 *
 * @param Invite the stored variant.
 * @since 1.0.0-beta.1
 */
interface CommunityInviteRepository<Invite : CommunityInvite> : EntityRepository<CommunityInviteData, CommunityInviteId, Invite> {

    /**
     * Synchronously finds the invite of this variant whose stored value and algorithm equal [codeHash], without changing stored state.
     *
     * @param codeHash the stored representation of the searched code; the caller normalizes and hashes the input.
     * @return the matching invite, or `null` if there is none.
     * @since 1.0.0-beta.1
     */
    @RawInviteCodeDependency(reason = "Finds the invite by comparing the stored value produced by a deterministic algorithm.")
    fun findByCode(codeHash: InviteCodeHash): Invite?

    /**
     * Synchronously finds invites of this variant whose expiration moment is at or before [now], without changing stored state.
     *
     * @param now the moment compared with the expiration moments.
     * @return the ids of the expired invites in ascending order; empty if there are none.
     * @since 1.0.0-beta.1
     */
    fun findExpired(now: Instant): List<CommunityInviteId>
}

/**
 * Persistence port for [CommunityInvite.Manager] entities.
 *
 * @since 1.0.0-beta.1
 */
interface ManagerCommunityInviteRepository : CommunityInviteRepository<CommunityInvite.Manager>

/**
 * Persistence port for [CommunityInvite.Developer] entities.
 *
 * @since 1.0.0-beta.1
 */
interface DeveloperCommunityInviteRepository : CommunityInviteRepository<CommunityInvite.Developer>

/**
 * Persistence port for [Competition] entities.
 *
 * @since 1.0.0-beta.1
 */
interface CompetitionRepository : EntityRepository<CompetitionData, CompetitionId, Competition> {

    /**
     * Finds one page of current competition and assigned contest pairs, filtering and counting before paging.
     * A contest in several competitions appears once for each competition.
     *
     * @param contestIds contests assigned to the observer; an empty set matches nothing.
     * @param pagination requested page; competition id and contest id ascending break sorting ties.
     * @param filter literal, case-insensitive names and exact ids combined with AND.
     * @return matching pairs and their exact total, including when the requested page is empty.
     * @since 1.0.0-beta.1
     */
    fun findAvailableToObserver(
        contestIds: Set<ContestId>,
        pagination: Pagination,
        filter: ObserverContestFilter = ObserverContestFilter(),
    ): Page<ObserverContest>

    /**
     * Synchronously finds current competitions owned by [ownerId], without changing stored state.
     * Repeated calls reflect current data; storage exceptions propagate to the caller.
     *
     * @param ownerId the manager whose own competitions are included; other owners are excluded before paging and counting.
     * @param pagination the requested page; id ascending is the default order and breaks ties unless explicitly sorted.
     * @param filter conditions combined with AND before paging and counting; unspecified conditions do not restrict selection.
     * @return owned competitions, the original pagination and exact filtered total; absent matches or missing pages are empty.
     * @since 1.0.0-beta.1
     */
    fun findAvailableToManager(
        ownerId: MultipleRoleUserId,
        pagination: Pagination,
        filter: CompetitionFilter = CompetitionFilter(),
    ): Page<Competition>

    /**
     * Synchronously finds competitions currently containing any of [contestIds], without changing stored state.
     * Storage exceptions propagate to the caller.
     *
     * @param contestIds the contests to look for; an empty set matches nothing.
     * @return distinct matching competitions in ascending id order; empty if there are none.
     * @since 1.0.0-beta.1
     */
    fun findByContestIds(contestIds: Set<ContestId>): List<Competition>
}

/**
 * One current competition and contest association visible to an observer.
 *
 * @property competition the competition containing the contest.
 * @property contest the assigned contest.
 * @since 1.0.0-beta.1
 */
data class ObserverContest(val competition: Competition, val contest: Contest)
