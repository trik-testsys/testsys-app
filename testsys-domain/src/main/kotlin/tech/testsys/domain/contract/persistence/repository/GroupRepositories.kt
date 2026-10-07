package tech.testsys.domain.contract.persistence.repository

import tech.testsys.domain.contract.persistence.ClassFilter
import tech.testsys.domain.contract.persistence.CompetitionFilter
import tech.testsys.domain.contract.persistence.Page
import tech.testsys.domain.contract.persistence.Pagination
import tech.testsys.domain.model.group.Class
import tech.testsys.domain.model.group.ClassData
import tech.testsys.domain.model.group.ClassId
import tech.testsys.domain.model.group.Community
import tech.testsys.domain.model.group.CommunityData
import tech.testsys.domain.model.group.CommunityId
import tech.testsys.domain.model.group.Competition
import tech.testsys.domain.model.group.CompetitionData
import tech.testsys.domain.model.group.CompetitionId
import tech.testsys.domain.model.user.MultipleRoleUserId

/**
 * Persistence port for [Class] entities.
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
}

/**
 * Persistence port for [Community] entities.
 *
 * @since %CURRENT_VERSION%
 */
interface CommunityRepository : EntityRepository<CommunityData, CommunityId, Community>

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
