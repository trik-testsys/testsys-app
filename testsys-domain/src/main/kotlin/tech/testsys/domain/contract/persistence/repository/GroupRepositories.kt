package tech.testsys.domain.contract.persistence.repository

import tech.testsys.domain.contract.persistence.ClassFilter
import tech.testsys.domain.contract.persistence.CompetitionFilter
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
import tech.testsys.domain.model.user.MultipleRoleUserId
import java.time.Instant

/**
 * Persistence port for [Class] entities. The invite of a class is fixed on creation; [save] needs a saved invite.
 *
 * @since %CURRENT_VERSION%
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
     * @since %CURRENT_VERSION%
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
     * @since %CURRENT_VERSION%
     */
    fun addStudent(classId: ClassId, studentId: MultipleRoleUserId): Class

    /**
     * Synchronously saves a new invite from [invite] and a new class referencing it, both or none.
     * Storage exceptions, including an invite code already held by another class invite, propagate and leave nothing saved.
     *
     * @param invite the data of the invite of the new class.
     * @param data the data of the new class computed from the id assigned to its invite.
     * @return the saved class.
     * @since %CURRENT_VERSION%
     */
    fun saveWithInvite(invite: ClassInviteData, data: (ClassInviteId) -> ClassData): Class

    /**
     * Synchronously finds the class referencing [inviteId], without changing stored state.
     *
     * @param inviteId the invite of the searched class.
     * @return the class of the invite, or `null` if no class references it.
     * @since %CURRENT_VERSION%
     */
    fun findByInvite(inviteId: ClassInviteId): Class?
}

/**
 * Persistence port for [ClassInvite] entities.
 *
 * @since %CURRENT_VERSION%
 */
interface ClassInviteRepository : EntityRepository<ClassInviteData, ClassInviteId, ClassInvite> {

    /**
     * Synchronously finds the invite whose stored value and algorithm equal [codeHash], without changing stored state.
     *
     * @param codeHash the stored representation of the searched code; the caller normalizes and hashes the input.
     * @return the matching invite, or `null` if there is none.
     * @since %CURRENT_VERSION%
     */
    @RawInviteCodeDependency(reason = "Finds the invite by comparing the stored value produced by a deterministic algorithm.")
    fun findByCode(codeHash: InviteCodeHash): ClassInvite?

    /**
     * Synchronously finds invites whose expiration moment is at or before [now], without changing stored state.
     *
     * @param now the moment compared with the expiration moments.
     * @return the ids of the expired invites in ascending order; empty if there are none.
     * @since %CURRENT_VERSION%
     */
    fun findExpired(now: Instant): List<ClassInviteId>
}

/**
 * Persistence port for [Community] entities. The invites of a community are fixed on creation; [save] needs saved invites.
 *
 * @since %CURRENT_VERSION%
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
     * @since %CURRENT_VERSION%
     */
    fun saveWithInvites(
        managerInvite: CommunityInviteData,
        developerInvite: CommunityInviteData,
        data: (CommunityInviteId, CommunityInviteId) -> CommunityData,
    ): Community

    /**
     * Synchronously finds the community referencing [inviteId] in either role, without changing stored state.
     *
     * @param inviteId the invite of the searched community.
     * @return the community of the invite, or `null` if no community references it.
     * @since %CURRENT_VERSION%
     */
    fun findByInvite(inviteId: CommunityInviteId): Community?

    /**
     * Synchronously finds communities created by [ownerId], without changing stored state.
     * Storage exceptions propagate to the caller.
     *
     * @param ownerId the creator of the communities.
     * @return the communities of the owner in ascending id order; empty if there are none.
     * @since %CURRENT_VERSION%
     */
    fun findByOwner(ownerId: MultipleRoleUserId): List<Community>
}

/**
 * Persistence port for the [Invite] variant of [CommunityInvite] entities; invites of other variants are absent from it.
 *
 * @param Invite the stored variant.
 * @since %CURRENT_VERSION%
 */
interface CommunityInviteRepository<Invite : CommunityInvite> : EntityRepository<CommunityInviteData, CommunityInviteId, Invite> {

    /**
     * Synchronously finds the invite of this variant whose stored value and algorithm equal [codeHash], without changing stored state.
     *
     * @param codeHash the stored representation of the searched code; the caller normalizes and hashes the input.
     * @return the matching invite, or `null` if there is none.
     * @since %CURRENT_VERSION%
     */
    @RawInviteCodeDependency(reason = "Finds the invite by comparing the stored value produced by a deterministic algorithm.")
    fun findByCode(codeHash: InviteCodeHash): Invite?

    /**
     * Synchronously finds invites of this variant whose expiration moment is at or before [now], without changing stored state.
     *
     * @param now the moment compared with the expiration moments.
     * @return the ids of the expired invites in ascending order; empty if there are none.
     * @since %CURRENT_VERSION%
     */
    fun findExpired(now: Instant): List<CommunityInviteId>
}

/**
 * Persistence port for [CommunityInvite.Manager] entities.
 *
 * @since %CURRENT_VERSION%
 */
interface ManagerCommunityInviteRepository : CommunityInviteRepository<CommunityInvite.Manager>

/**
 * Persistence port for [CommunityInvite.Developer] entities.
 *
 * @since %CURRENT_VERSION%
 */
interface DeveloperCommunityInviteRepository : CommunityInviteRepository<CommunityInvite.Developer>

/**
 * Persistence port for [Competition] entities.
 *
 * @since %CURRENT_VERSION%
 */
interface CompetitionRepository : EntityRepository<CompetitionData, CompetitionId, Competition> {

    /**
     * Synchronously finds current competitions owned by [ownerId], without changing stored state.
     * Repeated calls reflect current data; storage exceptions propagate to the caller.
     *
     * @param ownerId the manager whose own competitions are included; other owners are excluded before paging and counting.
     * @param pagination the requested page; id ascending is the default order and breaks ties unless explicitly sorted.
     * @param filter conditions combined with AND before paging and counting; unspecified conditions do not restrict selection.
     * @return owned competitions, the original pagination and exact filtered total; absent matches or missing pages are empty.
     * @since %CURRENT_VERSION%
     */
    fun findAvailableToManager(
        ownerId: MultipleRoleUserId,
        pagination: Pagination,
        filter: CompetitionFilter = CompetitionFilter(),
    ): Page<Competition>
}
