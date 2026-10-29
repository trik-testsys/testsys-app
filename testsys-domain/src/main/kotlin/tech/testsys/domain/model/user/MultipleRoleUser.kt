package tech.testsys.domain.model.user

import tech.testsys.domain.model.LazyEntityList
import tech.testsys.domain.model.group.Class
import tech.testsys.domain.model.group.ClassId
import tech.testsys.domain.model.group.Community
import tech.testsys.domain.model.group.CommunityId
import tech.testsys.domain.model.group.Competition
import tech.testsys.domain.model.group.CompetitionId
import tech.testsys.domain.model.task.Contest
import tech.testsys.domain.model.task.ContestId
import tech.testsys.domain.model.task.JudgmentOrder
import tech.testsys.domain.model.task.JudgmentOrderId
import tech.testsys.domain.model.task.Submission
import tech.testsys.domain.model.task.SubmissionId
import tech.testsys.domain.model.task.Task
import tech.testsys.domain.model.task.TaskId
import java.time.Instant

/**
 * Stored password representation of a [MultipleRoleUser]; only the password hashing port interprets it.
 *
 * @property value the salted slow hash together with what the hashing port needs to verify it.
 * @since 1.0.0-beta.1
 */
@JvmInline
value class PasswordHash(
    val value: String,
)

/**
 * Data of a [MultipleRoleUser].
 *
 * @property passwordHash the stored password representation of the user.
 * @property email the e-mail address of the user.
 * @property roles the roles held by the user.
 * @since 1.0.0-beta.1
 */
data class MultipleRoleUserData(
    val passwordHash: PasswordHash,
    override val name: String,
    val email: String,
    val roles: List<CompatibleUserRole>,
) : UserData

/**
 * Identifier of a [MultipleRoleUser].
 *
 * @since 1.0.0-beta.1
 */
@JvmInline
value class MultipleRoleUserId(
    override val value: Long,
) : UserId

/**
 * A user that may hold several non-fixed roles at once and be a member of several communities.
 * Owners of groups, tasks and contests are always users of this kind.
 *
 * @property data the data of the user.
 * @since 1.0.0-beta.1
 */
class MultipleRoleUser(
    id: MultipleRoleUserId,
    createdAt: Instant,
    val data: MultipleRoleUserData,
) : User<MultipleRoleUserId>(id, createdAt, data)

/**
 * A non-fixed role of a [MultipleRoleUser]: one that can be held together with other roles of this kind.
 *
 * @property memberOf the communities the user is a member of in this role.
 * @since 1.0.0-beta.1
 */
sealed class CompatibleUserRole(
    val memberOf: LazyEntityList<CommunityId, Community>,
)

/**
 * Data of the [Developer] role.
 *
 * @property tasks the tasks authored by the developer.
 * @property contests the contests authored by the developer.
 * @since 1.0.0-beta.1
 */
data class DeveloperData(
    val tasks: LazyEntityList<TaskId, Task>,
    val contests: LazyEntityList<ContestId, Contest>,
)

/**
 * The developer role: grants the right to create tasks and contests.
 *
 * @property data the data of the role.
 * @since 1.0.0-beta.1
 */
class Developer(
    memberOf: LazyEntityList<CommunityId, Community>,
    val data: DeveloperData,
) : CompatibleUserRole(memberOf)

/**
 * Data of the [Student] role.
 *
 * @property classes the classes the student is enrolled in.
 * @property submissions the submissions made by the student.
 * @since 1.0.0-beta.1
 */
data class StudentData(
    val classes: LazyEntityList<ClassId, Class>,
    val submissions: LazyEntityList<SubmissionId, Submission>,
)

/**
 * The student role: grants the right to be enrolled in classes and take part in their contests.
 *
 * @property data the data of the role.
 * @since 1.0.0-beta.1
 */
class Student(
    memberOf: LazyEntityList<CommunityId, Community>,
    val data: StudentData,
) : CompatibleUserRole(memberOf)

/**
 * The administrator role: grants the right to create and manage communities.
 *
 * @since 1.0.0-beta.1
 */
class Administrator(
    memberOf: LazyEntityList<CommunityId, Community>,
) : CompatibleUserRole(memberOf)

/**
 * Data of the [Judge] role.
 *
 * @property judgmentOrders the rulings issued by the judge.
 * @since 1.0.0-beta.1
 */
data class JudgeData(
    val judgmentOrders: LazyEntityList<JudgmentOrderId, JudgmentOrder>,
)

/**
 * The judge role: grants the right to review solutions and change their verdicts.
 *
 * @property data the data of the role.
 * @since 1.0.0-beta.1
 */
class Judge(
    memberOf: LazyEntityList<CommunityId, Community>,
    val data: JudgeData,
) : CompatibleUserRole(memberOf)

/**
 * Data of the [Manager] role.
 *
 * @property classes the classes owned by the manager.
 * @property competitions the competitions owned by the manager.
 * @since 1.0.0-beta.1
 */
data class ManagerData(
    val classes: LazyEntityList<ClassId, Class>,
    val competitions: LazyEntityList<CompetitionId, Competition>,
)

/**
 * The manager role: grants the right to create and manage classes and competitions.
 *
 * @property data the data of the role.
 * @since 1.0.0-beta.1
 */
class Manager(
    memberOf: LazyEntityList<CommunityId, Community>,
    val data: ManagerData,
) : CompatibleUserRole(memberOf)
