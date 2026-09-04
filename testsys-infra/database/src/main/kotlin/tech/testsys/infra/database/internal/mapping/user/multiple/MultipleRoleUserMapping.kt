package tech.testsys.infra.database.internal.mapping.user.multiple

import tech.testsys.domain.builder.api.multipleRoleUser
import tech.testsys.domain.builder.data
import tech.testsys.domain.model.group.ClassId
import tech.testsys.domain.model.group.CommunityId
import tech.testsys.domain.model.group.CompetitionId
import tech.testsys.domain.model.task.ContestId
import tech.testsys.domain.model.task.JudgmentOrderId
import tech.testsys.domain.model.task.SubmissionId
import tech.testsys.domain.model.task.TaskId
import tech.testsys.domain.model.user.MultipleRoleUser
import tech.testsys.domain.model.user.MultipleRoleUserData
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.user.UserJpaEntity
import tech.testsys.infra.database.internal.jpa.entity.user.UserTypeJpaEnum
import tech.testsys.infra.database.internal.utils.populateFields

/**
 * Role rows of a [MultipleRoleUser] collected from the per-role tables; a `null` role is not held by the user.
 *
 * @property administrator the administrator role, or `null` if not held.
 * @property developer the developer role, or `null` if not held.
 * @property student the student role, or `null` if not held.
 * @property judge the judge role, or `null` if not held.
 * @property manager the manager role, or `null` if not held.
 * @since %CURRENT_VERSION%
 */
@InternalDatabaseApi
data class MultipleRoleUserRoles(
    val administrator: AdministratorRoleInfo?,
    val developer: DeveloperRoleInfo?,
    val student: StudentRoleInfo?,
    val judge: JudgeRoleInfo?,
    val manager: ManagerRoleInfo?,
) {

    /**
     * Administrator role of a user.
     *
     * @property memberOf ids of the communities the role is held in.
     * @since %CURRENT_VERSION%
     */
    data class AdministratorRoleInfo(val memberOf: List<CommunityId>)

    /**
     * Developer role of a user.
     *
     * @property memberOf ids of the communities the role is held in.
     * @property tasks ids of the tasks owned by the user.
     * @property contests ids of the contests owned by the user.
     * @since %CURRENT_VERSION%
     */
    data class DeveloperRoleInfo(
        val memberOf: List<CommunityId>,
        val tasks: List<TaskId>,
        val contests: List<ContestId>,
    )

    /**
     * Student role of a user.
     *
     * @property memberOf ids of the communities the role is held in.
     * @property classes ids of the classes the user is enrolled in.
     * @property submissions ids of the submissions authored by the user.
     * @since %CURRENT_VERSION%
     */
    data class StudentRoleInfo(
        val memberOf: List<CommunityId>,
        val classes: List<ClassId>,
        val submissions: List<SubmissionId>,
    )

    /**
     * Judge role of a user.
     *
     * @property memberOf ids of the communities the role is held in.
     * @property judgmentOrders ids of the judgment orders issued by the user.
     * @since %CURRENT_VERSION%
     */
    data class JudgeRoleInfo(
        val memberOf: List<CommunityId>,
        val judgmentOrders: List<JudgmentOrderId>,
    )

    /**
     * Manager role of a user.
     *
     * @property memberOf ids of the communities the role is held in.
     * @property classes ids of the classes owned by the user.
     * @property competitions ids of the competitions owned by the user.
     * @since %CURRENT_VERSION%
     */
    data class ManagerRoleInfo(
        val memberOf: List<CommunityId>,
        val classes: List<ClassId>,
        val competitions: List<CompetitionId>,
    )
}

/**
 * Mapping between [MultipleRoleUser] and [UserJpaEntity] with its [MultipleRoleUserRoles].
 *
 * @since %CURRENT_VERSION%
 */
@InternalDatabaseApi
object MultipleRoleUserMapping {

    /**
     * Assembles a [MultipleRoleUser] from [userJpaEntity] and its collected [roles]; fails when the e-mail is missing.
     *
     * @since %CURRENT_VERSION%
     */
    fun toDomain(userJpaEntity: UserJpaEntity, roles: MultipleRoleUserRoles) = multipleRoleUser {
        populateFields(userJpaEntity)
        data {
            accessToken = userJpaEntity.accessToken
            name = userJpaEntity.name
            email = requireNotNull(userJpaEntity.email) {
                "User ${userJpaEntity.id} is MULTIPLE_ROLE but email is null"
            }
            roles {
                roles.administrator?.let { info ->
                    administrator { memberOf(info.memberOf.map { it.value }) }
                }
                roles.developer?.let { info ->
                    developer {
                        memberOf(info.memberOf.map { it.value })
                        data {
                            tasks(info.tasks.map { it.value })
                            contests(info.contests.map { it.value })
                        }
                    }
                }
                roles.student?.let { info ->
                    student {
                        memberOf(info.memberOf.map { it.value })
                        data {
                            classes(info.classes.map { it.value })
                            submissions(info.submissions.map { it.value })
                        }
                    }
                }
                roles.judge?.let { info ->
                    judge {
                        memberOf(info.memberOf.map { it.value })
                        data {
                            judgmentOrders(info.judgmentOrders.map { it.value })
                        }
                    }
                }
                roles.manager?.let { info ->
                    manager {
                        memberOf(info.memberOf.map { it.value })
                        data {
                            classes(info.classes.map { it.value })
                            competitions(info.competitions.map { it.value })
                        }
                    }
                }
            }
        }
    }

    /**
     * Creates a new multiple-role [UserJpaEntity] row from [data].
     *
     * @since %CURRENT_VERSION%
     */
    fun toUserJpaEntity(data: MultipleRoleUserData) = UserJpaEntity(
        name = data.name,
        accessToken = data.accessToken,
        email = data.email,
        type = UserTypeJpaEnum.MULTIPLE_ROLE,
    )

    /**
     * Creates the [UserJpaEntity] row replacing [current] from [entity], keeping `createdAt` and `version`.
     *
     * @since %CURRENT_VERSION%
     */
    fun toUserJpaEntity(entity: MultipleRoleUser, current: UserJpaEntity) = UserJpaEntity(
        name = entity.data.name,
        accessToken = entity.data.accessToken,
        email = entity.data.email,
        type = UserTypeJpaEnum.MULTIPLE_ROLE,
        id = entity.id.value,
    ).also {
        it.createdAt = current.createdAt
        it.version = current.version
    }
}
