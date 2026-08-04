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
 * Information collected from per-role JPA tables, grouped per the role variants
 * supported by [tech.testsys.domain.model.user.CompatibleUserRole].
 *
 * Nullable variants mark roles the user does not currently hold.
 */
@InternalDatabaseApi
data class MultipleRoleUserRoles(
    val administrator: AdministratorRoleInfo?,
    val developer: DeveloperRoleInfo?,
    val student: StudentRoleInfo?,
    val judge: JudgeRoleInfo?,
    val manager: ManagerRoleInfo?,
) {

    data class AdministratorRoleInfo(val memberOf: List<CommunityId>)

    data class DeveloperRoleInfo(
        val memberOf: List<CommunityId>,
        val tasks: List<TaskId>,
        val contests: List<ContestId>,
    )

    data class StudentRoleInfo(
        val memberOf: List<CommunityId>,
        val classes: List<ClassId>,
        val submissions: List<SubmissionId>,
    )

    data class JudgeRoleInfo(
        val memberOf: List<CommunityId>,
        val judgmentOrders: List<JudgmentOrderId>,
    )

    data class ManagerRoleInfo(
        val memberOf: List<CommunityId>,
        val classes: List<ClassId>,
        val competitions: List<CompetitionId>,
    )
}

@InternalDatabaseApi
object MultipleRoleUserMapping {

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

    fun toUserJpaEntity(data: MultipleRoleUserData) = UserJpaEntity(
        name = data.name,
        accessToken = data.accessToken,
        email = data.email,
        type = UserTypeJpaEnum.MULTIPLE_ROLE,
    )

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
