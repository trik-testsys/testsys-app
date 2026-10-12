package tech.testsys.web.app.service

import tech.testsys.domain.contract.persistence.SubmissionCount
import tech.testsys.domain.model.task.SolutionId
import tech.testsys.domain.model.user.UserId
import tech.testsys.web.app.service.developer.TaskValidationRequestVo
import tech.testsys.web.app.service.judge.JudgmentOrderVo
import tech.testsys.web.app.service.student.ClassVo

/**
 * Reads sections of user details under the current actor's access rules.
 *
 * @since %CURRENT_VERSION%
 */
interface UserRoleReadService {
    /**
     * Reads developer tasks.
     *
     * @since %CURRENT_VERSION%
     */
    fun viewUserTasks(userId: UserId): List<Triple<TaskVo, TaskValidationRequestVo?, List<CommunityVo>>>

    /**
     * Reads developer contests.
     *
     * @since %CURRENT_VERSION%
     */
    fun viewUserContests(userId: UserId): List<Pair<ContestVo, List<Pair<TaskVo, Long>>>>

    /**
     * Reads manager classes.
     *
     * @since %CURRENT_VERSION%
     */
    fun viewUserClasses(userId: UserId): List<Pair<ClassVo, SubmissionCount>>

    /**
     * Reads manager competitions.
     *
     * @since %CURRENT_VERSION%
     */
    fun viewUserCompetitions(userId: UserId): List<Pair<CompetitionVo, SubmissionCount>>

    /**
     * Reads judge orders.
     *
     * @since %CURRENT_VERSION%
     */
    fun viewUserJudgments(userId: UserId): List<Triple<JudgmentOrderVo, SolutionId, Long?>>

    /**
     * Reads observer assignments.
     *
     * @since %CURRENT_VERSION%
     */
    fun viewUserAssignedContests(userId: UserId): List<Pair<ContestVo, List<CompetitionVo>>>
}
