package tech.testsys.domain.builder.user

import tech.testsys.domain.builder.Builder
import tech.testsys.domain.builder.DomainEntityWithDataBuilder
import tech.testsys.domain.builder.util.applyVersion
import tech.testsys.domain.builder.util.lazify
import tech.testsys.domain.builder.util.requireField
import tech.testsys.domain.model.group.ClassId
import tech.testsys.domain.model.group.CommunityId
import tech.testsys.domain.model.group.CompetitionId
import tech.testsys.domain.model.task.ContestId
import tech.testsys.domain.model.task.JudgmentOrderId
import tech.testsys.domain.model.task.SubmissionId
import tech.testsys.domain.model.task.TaskId
import tech.testsys.domain.model.user.Administrator
import tech.testsys.domain.model.user.CompatibleUserRole
import tech.testsys.domain.model.user.Developer
import tech.testsys.domain.model.user.DeveloperData
import tech.testsys.domain.model.user.Judge
import tech.testsys.domain.model.user.JudgeData
import tech.testsys.domain.model.user.Manager
import tech.testsys.domain.model.user.ManagerData
import tech.testsys.domain.model.user.MultipleRoleUser
import tech.testsys.domain.model.user.MultipleRoleUserData
import tech.testsys.domain.model.user.MultipleRoleUserId
import tech.testsys.domain.model.user.PasswordHash
import tech.testsys.domain.model.user.Student
import tech.testsys.domain.model.user.StudentData
import java.time.Instant

/**
 * Base class of builders of [CompatibleUserRole]s without role data.
 *
 * @param Role the type of the built role.
 * @property memberOf the ids of the communities the role is a member of.
 * @since 1.0.0-beta.1
 */
abstract class CompatibleUserRoleBuilderWithoutData<Role : CompatibleUserRole> : Builder<Role> {

    var memberOf = mutableListOf<CommunityId>()

    /**
     * Sets [memberOf] from raw ids.
     *
     * @since 1.0.0-beta.1
     */
    fun memberOf(communities: Iterable<Long>) {
        memberOf = communities.map { CommunityId(it) }.toMutableList()
    }
}

/**
 * Base class of builders of [CompatibleUserRole]s with role data.
 *
 * @param Role the type of the built role.
 * @param Data the type of the role data.
 * @param DataBuilder the builder type of [Data].
 * @property memberOf the ids of the communities the role is a member of.
 * @since 1.0.0-beta.1
 */
abstract class CompatibleUserRoleBuilderWithData<Role : CompatibleUserRole, Data, DataBuilder : Builder<Data>> :
    DomainEntityWithDataBuilder<Role, Data, DataBuilder>() {

    var memberOf = mutableListOf<CommunityId>()

    /**
     * Sets [memberOf] from raw ids.
     *
     * @since 1.0.0-beta.1
     */
    fun memberOf(communities: Iterable<Long>) {
        memberOf = communities.map { CommunityId(it) }.toMutableList()
    }
}

/**
 * Builder of [DeveloperData].
 *
 * @property tasks the ids of the tasks authored by the developer.
 * @property contests the ids of the contests authored by the developer.
 * @since 1.0.0-beta.1
 */
class DeveloperDataBuilder : Builder<DeveloperData> {

    var tasks = mutableListOf<TaskId>()

    var contests = mutableListOf<ContestId>()

    /**
     * Sets [tasks] from raw ids.
     *
     * @since 1.0.0-beta.1
     */
    fun tasks(tasks: Iterable<Long>) {
        this.tasks = tasks.map { TaskId(it) }.toMutableList()
    }

    /**
     * Sets [contests] from raw ids.
     *
     * @since 1.0.0-beta.1
     */
    fun contests(contests: Iterable<Long>) {
        this.contests = contests.map { ContestId(it) }.toMutableList()
    }

    override fun build(): DeveloperData {
        return DeveloperData(
            tasks = tasks.lazify(),
            contests = contests.lazify(),
        )
    }
}

/**
 * Builder of the [Developer] role. Required: [data].
 *
 * @since 1.0.0-beta.1
 */
class DeveloperBuilder : CompatibleUserRoleBuilderWithData<Developer, DeveloperData, DeveloperDataBuilder>() {

    override var data: DeveloperData? = null
    override fun dataBuilder() = DeveloperDataBuilder()

    override fun build(): Developer {
        val data = requireField(data) { ::data }
        return Developer(
            memberOf = memberOf.lazify(),
            data = data,
        )
    }
}

/**
 * Builder of [StudentData].
 *
 * @property classes the ids of the classes the student is enrolled in.
 * @property submissions the ids of the submissions made by the student.
 * @since 1.0.0-beta.1
 */
class StudentDataBuilder : Builder<StudentData> {

    var classes = mutableListOf<ClassId>()

    var submissions = mutableListOf<SubmissionId>()

    /**
     * Sets [classes] from raw ids.
     *
     * @since 1.0.0-beta.1
     */
    fun classes(classes: Iterable<Long>) {
        this.classes = classes.map { ClassId(it) }.toMutableList()
    }

    /**
     * Sets [submissions] from raw ids.
     *
     * @since 1.0.0-beta.1
     */
    fun submissions(submissions: Iterable<Long>) {
        this.submissions = submissions.map { SubmissionId(it) }.toMutableList()
    }

    override fun build(): StudentData {
        return StudentData(
            classes = classes.lazify(),
            submissions = submissions.lazify(),
        )
    }
}

/**
 * Builder of the [Student] role. Required: [data].
 *
 * @since 1.0.0-beta.1
 */
class StudentBuilder : CompatibleUserRoleBuilderWithData<Student, StudentData, StudentDataBuilder>() {

    override var data: StudentData? = null
    override fun dataBuilder() = StudentDataBuilder()

    override fun build(): Student {
        val data = requireField(data) { ::data }
        return Student(
            memberOf = memberOf.lazify(),
            data = data,
        )
    }
}

/**
 * Builder of the [Administrator] role.
 *
 * @since 1.0.0-beta.1
 */
class AdministratorBuilder : CompatibleUserRoleBuilderWithoutData<Administrator>() {

    override fun build() = Administrator(
        memberOf = memberOf.lazify(),
    )
}

/**
 * Builder of [JudgeData].
 *
 * @property judgmentOrders the ids of the rulings issued by the judge.
 * @since 1.0.0-beta.1
 */
class JudgeDataBuilder : Builder<JudgeData> {

    var judgmentOrders = mutableListOf<JudgmentOrderId>()

    /**
     * Sets [judgmentOrders] from raw ids.
     *
     * @since 1.0.0-beta.1
     */
    fun judgmentOrders(judgmentOrders: Iterable<Long>) {
        this.judgmentOrders = judgmentOrders.map { JudgmentOrderId(it) }.toMutableList()
    }

    override fun build() = JudgeData(
        judgmentOrders = judgmentOrders.lazify(),
    )
}

/**
 * Builder of the [Judge] role. Required: [data].
 *
 * @since 1.0.0-beta.1
 */
class JudgeBuilder : CompatibleUserRoleBuilderWithData<Judge, JudgeData, JudgeDataBuilder>() {

    override var data: JudgeData? = null
    override fun dataBuilder() = JudgeDataBuilder()

    override fun build(): Judge {
        val data = requireField(data) { ::data }
        return Judge(
            memberOf = memberOf.lazify(),
            data = data,
        )
    }
}

/**
 * Builder of [ManagerData].
 *
 * @property classes the ids of the classes owned by the manager.
 * @property competitions the ids of the competitions owned by the manager.
 * @since 1.0.0-beta.1
 */
class ManagerDataBuilder : Builder<ManagerData> {

    var classes = mutableListOf<ClassId>()

    var competitions = mutableListOf<CompetitionId>()

    /**
     * Sets [classes] from raw ids.
     *
     * @since 1.0.0-beta.1
     */
    fun classes(classes: Iterable<Long>) {
        this.classes = classes.map { ClassId(it) }.toMutableList()
    }

    /**
     * Sets [competitions] from raw ids.
     *
     * @since 1.0.0-beta.1
     */
    fun competitions(competitions: Iterable<Long>) {
        this.competitions = competitions.map { CompetitionId(it) }.toMutableList()
    }

    override fun build() = ManagerData(
        competitions = competitions.lazify(),
        classes = classes.lazify(),
    )
}

/**
 * Builder of the [Manager] role. Required: [data].
 *
 * @since 1.0.0-beta.1
 */
class ManagerBuilder : CompatibleUserRoleBuilderWithData<Manager, ManagerData, ManagerDataBuilder>() {

    override var data: ManagerData? = null
    override fun dataBuilder() = ManagerDataBuilder()

    override fun build(): Manager {
        val data = requireField(data) { ::data }
        return Manager(
            memberOf = memberOf.lazify(),
            data = data,
        )
    }
}

private typealias Roles = MutableList<CompatibleUserRole>

/**
 * Builder of [MultipleRoleUserData]. Required: [passwordHash], [name], [email].
 * Roles are added inside a [roles] block.
 *
 * @property passwordHash the stored password representation of the user, or `null` if not set yet.
 * @property name the name of the user, or `null` if not set yet.
 * @property email the e-mail address of the user, or `null` if not set yet.
 * @since 1.0.0-beta.1
 */
class MultipleRoleUserDataBuilder : Builder<MultipleRoleUserData> {

    private var roles: Roles = mutableListOf()

    var passwordHash: PasswordHash? = null

    var name: String? = null

    var email: String? = null

    /**
     * Configures the roles of the user with [builder].
     *
     * @since 1.0.0-beta.1
     */
    fun roles(builder: Roles.() -> Unit) {
        roles.builder()
    }

    /**
     * Adds a [Developer] role configured by [builder].
     *
     * @since 1.0.0-beta.1
     */
    fun Roles.developer(builder: DeveloperBuilder.() -> Unit) {
        DeveloperBuilder()
            .apply(builder)
            .build()
            .also { add(it) }
    }

    /**
     * Adds a [Student] role configured by [builder].
     *
     * @since 1.0.0-beta.1
     */
    fun Roles.student(builder: StudentBuilder.() -> Unit) {
        StudentBuilder()
            .apply(builder)
            .build()
            .also { add(it) }
    }

    /**
     * Adds a [Judge] role configured by [builder].
     *
     * @since 1.0.0-beta.1
     */
    fun Roles.judge(builder: JudgeBuilder.() -> Unit) {
        JudgeBuilder()
            .apply(builder)
            .build()
            .also { add(it) }
    }

    /**
     * Adds a [Manager] role configured by [builder].
     *
     * @since 1.0.0-beta.1
     */
    fun Roles.manager(builder: ManagerBuilder.() -> Unit) {
        ManagerBuilder()
            .apply(builder)
            .build()
            .also { add(it) }
    }

    /**
     * Adds an [Administrator] role configured by [builder].
     *
     * @since 1.0.0-beta.1
     */
    fun Roles.administrator(builder: AdministratorBuilder.() -> Unit) {
        AdministratorBuilder()
            .apply(builder)
            .build()
            .also { add(it) }
    }

    override fun build() = MultipleRoleUserData(
        roles = roles,
        passwordHash = requireField(passwordHash) { ::passwordHash },
        name = requireField(name) { ::name },
        email = requireField(email) { ::email },
    )
}

/**
 * Builder of [MultipleRoleUser] entities. Required: [id], [createdAt], [data].
 *
 * @since 1.0.0-beta.1
 */
class MultipleRoleUserBuilder :
    UserBuilder<MultipleRoleUserId, MultipleRoleUser, MultipleRoleUserData, MultipleRoleUserDataBuilder>() {

    override var id: Long? = null
    override var createdAt: Instant? = null
    override var data: MultipleRoleUserData? = null
    override fun dataBuilder() = MultipleRoleUserDataBuilder()

    override fun build(): MultipleRoleUser {
        val id = requireField(id) { ::id }
        val createdAt = requireField(createdAt) { ::createdAt }
        val data = requireField(data) { ::data }

        return MultipleRoleUser(
            id = MultipleRoleUserId(id),
            createdAt = createdAt,
            data = data,
        ).applyVersion(version)
    }
}
