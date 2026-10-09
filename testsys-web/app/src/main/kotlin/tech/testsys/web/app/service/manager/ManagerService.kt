package tech.testsys.web.app.service.manager

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tech.testsys.domain.contract.persistence.ClassFilter
import tech.testsys.domain.contract.persistence.CompetitionFilter
import tech.testsys.domain.contract.persistence.Page
import tech.testsys.domain.contract.persistence.Pagination
import tech.testsys.domain.model.group.ClassId
import tech.testsys.domain.model.group.CompetitionId
import tech.testsys.domain.model.task.ContestId
import tech.testsys.domain.model.user.SingleRoleUserId
import tech.testsys.operation.error.getOrThrow
import tech.testsys.operation.user.ManagerOperations
import tech.testsys.web.app.service.CompetitionVo
import tech.testsys.web.app.service.ContestVo
import tech.testsys.web.app.service.CurrentUser
import tech.testsys.web.app.service.map
import tech.testsys.web.app.service.student.ClassVo
import tech.testsys.web.app.service.student.toVo
import tech.testsys.web.app.service.toVo

/**
 * Runs [ManagerOperations] for the current user in one transaction per call.
 * A failed operation throws `OperationException`, which rolls the transaction back.
 *
 * @since %CURRENT_VERSION%
 */
@Service
@Transactional
class ManagerService(private val operations: ManagerOperations, private val currentUser: CurrentUser) {
    /**
     * Runs [ManagerOperations.createClass].
     *
     * @since %CURRENT_VERSION%
     */
    fun createClass(className: String): ClassVo = operations.createClass(currentUser.multipleRoleUser(), className).getOrThrow().toVo()

    /**
     * Runs [ManagerOperations.createCompetition].
     *
     * @since %CURRENT_VERSION%
     */
    fun createCompetition(competitionName: String): CompetitionVo =
        operations.createCompetition(currentUser.multipleRoleUser(), competitionName).getOrThrow().toVo()

    /**
     * Runs [ManagerOperations.viewClasses].
     *
     * @since %CURRENT_VERSION%
     */
    @Transactional(readOnly = true)
    fun viewClasses(pagination: Pagination, filter: ClassFilter = ClassFilter()): Page<ClassVo> =
        operations.viewClasses(currentUser.multipleRoleUser(), pagination, filter).getOrThrow().map { studyClass -> studyClass.toVo() }

    /**
     * Runs [ManagerOperations.viewClass].
     *
     * @since %CURRENT_VERSION%
     */
    @Transactional(readOnly = true)
    fun viewClass(classId: ClassId): ClassDetailsVo = operations.viewClass(currentUser.multipleRoleUser(), classId).getOrThrow().toVo()

    /**
     * Runs [ManagerOperations.createClassInvite].
     *
     * @since %CURRENT_VERSION%
     */
    fun createClassInvite(classId: ClassId): ClassInviteVo =
        operations.createClassInvite(currentUser.multipleRoleUser(), classId).getOrThrow().toVo()

    /**
     * Runs [ManagerOperations.extendClassInvite].
     *
     * @since %CURRENT_VERSION%
     */
    fun extendClassInvite(classId: ClassId): ClassInviteVo =
        operations.extendClassInvite(currentUser.multipleRoleUser(), classId).getOrThrow().toVo()

    /**
     * Runs [ManagerOperations.viewCompetitions].
     *
     * @since %CURRENT_VERSION%
     */
    @Transactional(readOnly = true)
    fun viewCompetitions(pagination: Pagination, filter: CompetitionFilter = CompetitionFilter()): Page<CompetitionVo> =
        operations.viewCompetitions(currentUser.multipleRoleUser(), pagination, filter).getOrThrow()
            .map { competition -> competition.toVo() }

    /**
     * Runs [ManagerOperations.viewCompetition].
     *
     * @since %CURRENT_VERSION%
     */
    @Transactional(readOnly = true)
    fun viewCompetition(competitionId: CompetitionId): CompetitionDetailsVo =
        operations.viewCompetition(currentUser.multipleRoleUser(), competitionId).getOrThrow().toVo()

    /**
     * Runs [ManagerOperations.viewClassContest].
     *
     * @since %CURRENT_VERSION%
     */
    @Transactional(readOnly = true)
    fun viewClassContest(classId: ClassId, contestId: ContestId): ContestResultsVo =
        operations.viewClassContest(currentUser.multipleRoleUser(), classId, contestId).getOrThrow().toVo()

    /**
     * Runs [ManagerOperations.viewCompetitionContest].
     *
     * @since %CURRENT_VERSION%
     */
    @Transactional(readOnly = true)
    fun viewCompetitionContest(competitionId: CompetitionId, contestId: ContestId): ContestResultsVo =
        operations.viewCompetitionContest(currentUser.multipleRoleUser(), competitionId, contestId).getOrThrow().toVo()

    /**
     * Runs [ManagerOperations.addClassContest].
     *
     * @since %CURRENT_VERSION%
     */
    fun addClassContest(classId: ClassId, contestId: ContestId): ClassVo =
        operations.addClassContest(currentUser.multipleRoleUser(), classId, contestId).getOrThrow().toVo()

    /**
     * Runs [ManagerOperations.addCompetitionContest].
     *
     * @since %CURRENT_VERSION%
     */
    fun addCompetitionContest(competitionId: CompetitionId, contestId: ContestId): CompetitionVo =
        operations.addCompetitionContest(currentUser.multipleRoleUser(), competitionId, contestId).getOrThrow().toVo()

    /**
     * Runs [ManagerOperations.createParticipants].
     *
     * @since %CURRENT_VERSION%
     */
    fun createParticipants(competitionId: CompetitionId, participantCount: Int): List<ParticipantVo> =
        operations.createParticipants(currentUser.multipleRoleUser(), competitionId, participantCount).getOrThrow()
            .map { participant -> participant.toVo() }

    /**
     * Runs [ManagerOperations.deleteParticipant].
     *
     * @since %CURRENT_VERSION%
     */
    fun deleteParticipant(competitionId: CompetitionId, participantId: SingleRoleUserId): ParticipantVo =
        operations.deleteParticipant(currentUser.multipleRoleUser(), competitionId, participantId).getOrThrow().toVo()

    /**
     * Runs [ManagerOperations.viewAvailableContests].
     *
     * @since %CURRENT_VERSION%
     */
    @Transactional(readOnly = true)
    fun viewAvailableContests(pagination: Pagination, name: String? = null): Page<ContestVo> =
        operations.viewAvailableContests(currentUser.multipleRoleUser(), pagination, name).getOrThrow().map { contest -> contest.toVo() }
}
