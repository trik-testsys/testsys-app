package tech.testsys.web.app.service.administrator

import tech.testsys.domain.model.group.ClassId
import tech.testsys.domain.model.group.CommunityId
import tech.testsys.domain.model.group.CompetitionId
import tech.testsys.domain.model.task.ContestId
import tech.testsys.domain.model.task.JudgmentOrderId
import tech.testsys.domain.model.task.SubmissionId
import tech.testsys.domain.model.task.TaskId
import tech.testsys.domain.model.user.AccessTokenHash
import tech.testsys.domain.model.user.Administrator
import tech.testsys.domain.model.user.CompatibleUserRole
import tech.testsys.domain.model.user.Developer
import tech.testsys.domain.model.user.Judge
import tech.testsys.domain.model.user.Manager
import tech.testsys.domain.model.user.MultipleRoleUser
import tech.testsys.domain.model.user.MultipleRoleUserId
import tech.testsys.domain.model.user.Observer
import tech.testsys.domain.model.user.Participant
import tech.testsys.domain.model.user.SingleRoleUserId
import tech.testsys.domain.model.user.Student
import tech.testsys.domain.model.user.Supervisor
import tech.testsys.domain.model.user.User
import tech.testsys.domain.model.user.UserId
import java.time.Instant

/**
 * Data for pages of a user available to an administrator, with links replaced by identifiers.
 *
 * @property id the identifier of the user.
 * @property createdAt the moment the user was created.
 * @property accessTokenHash the stored access code with its hashing algorithm.
 * @property name the nickname of the user.
 * @since %CURRENT_VERSION%
 */
sealed interface AdminUserVo {
    val id: UserId
    val createdAt: Instant
    val accessTokenHash: AccessTokenHash
    val name: String
}

/**
 * Data of a user with non-fixed roles for an administrator.
 *
 * @property email the e-mail address of the user.
 * @property roles the roles of the user, including memberships in communities of other administrators.
 * @since %CURRENT_VERSION%
 */
data class MultipleRoleUserVo(
    override val id: MultipleRoleUserId,
    override val createdAt: Instant,
    override val accessTokenHash: AccessTokenHash,
    override val name: String,
    val email: String,
    val roles: List<RoleVo>,
) : AdminUserVo

/**
 * Data of an observer for an administrator.
 *
 * @property community the identifier of the community of the observer.
 * @property contests the identifiers of the contests assigned to the observer.
 * @since %CURRENT_VERSION%
 */
data class ObserverVo(
    override val id: SingleRoleUserId,
    override val createdAt: Instant,
    override val accessTokenHash: AccessTokenHash,
    override val name: String,
    val community: CommunityId,
    val contests: List<ContestId>,
) : AdminUserVo

/**
 * Non-fixed role of a [MultipleRoleUserVo] with the identifiers of its data.
 *
 * @property memberOf the identifiers of the communities the user is a member of in this role.
 * @since %CURRENT_VERSION%
 */
sealed interface RoleVo {
    val memberOf: List<CommunityId>
}

/**
 * The administrator role.
 *
 * @since %CURRENT_VERSION%
 */
data class AdministratorRoleVo(override val memberOf: List<CommunityId>) : RoleVo

/**
 * The developer role.
 *
 * @property tasks the identifiers of the tasks created by the developer.
 * @property contests the identifiers of the contests created by the developer.
 * @since %CURRENT_VERSION%
 */
data class DeveloperRoleVo(override val memberOf: List<CommunityId>, val tasks: List<TaskId>, val contests: List<ContestId>) : RoleVo

/**
 * The manager role.
 *
 * @property classes the identifiers of the classes created by the manager.
 * @property competitions the identifiers of the competitions created by the manager.
 * @since %CURRENT_VERSION%
 */
data class ManagerRoleVo(override val memberOf: List<CommunityId>, val classes: List<ClassId>, val competitions: List<CompetitionId>) :
    RoleVo

/**
 * The judge role.
 *
 * @property judgmentOrders the identifiers of the judgment orders issued by the judge.
 * @since %CURRENT_VERSION%
 */
data class JudgeRoleVo(override val memberOf: List<CommunityId>, val judgmentOrders: List<JudgmentOrderId>) : RoleVo

/**
 * The student role.
 *
 * @property classes the identifiers of the classes of the student.
 * @property submissions the identifiers of the submissions of the student.
 * @since %CURRENT_VERSION%
 */
data class StudentRoleVo(override val memberOf: List<CommunityId>, val classes: List<ClassId>, val submissions: List<SubmissionId>) : RoleVo

internal fun User<*>.toAdminVo(): AdminUserVo = when (this) {
    is MultipleRoleUser -> toVo()
    is Observer -> toVo()
    is Participant, is Supervisor -> error("User id=${id.value} of a kind unavailable to administrators was returned")
}

internal fun Observer.toVo(): ObserverVo = ObserverVo(
    id = id,
    createdAt = createdAt,
    accessTokenHash = data.accessTokenHash,
    name = data.name,
    community = data.community.id,
    contests = data.contests.ids,
)

internal fun MultipleRoleUser.toVo(): MultipleRoleUserVo = MultipleRoleUserVo(
    id = id,
    createdAt = createdAt,
    accessTokenHash = data.accessTokenHash,
    name = data.name,
    email = data.email,
    roles = data.roles.map { role -> role.toVo() },
)

private fun CompatibleUserRole.toVo(): RoleVo = when (this) {
    is Administrator -> AdministratorRoleVo(memberOf = memberOf.ids)
    is Developer -> DeveloperRoleVo(memberOf = memberOf.ids, tasks = data.tasks.ids, contests = data.contests.ids)
    is Manager -> ManagerRoleVo(memberOf = memberOf.ids, classes = data.classes.ids, competitions = data.competitions.ids)
    is Judge -> JudgeRoleVo(memberOf = memberOf.ids, judgmentOrders = data.judgmentOrders.ids)
    is Student -> StudentRoleVo(memberOf = memberOf.ids, classes = data.classes.ids, submissions = data.submissions.ids)
}
