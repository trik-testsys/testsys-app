package tech.testsys.domain.builder.user

import tech.testsys.domain.builder.Builder
import tech.testsys.domain.builder.DataCapable
import tech.testsys.domain.builder.util.lazify
import tech.testsys.domain.builder.util.requireField
import tech.testsys.domain.model.group.ClassId
import tech.testsys.domain.model.group.CommunityId
import tech.testsys.domain.model.group.CompetitionId
import tech.testsys.domain.model.task.ContestId
import tech.testsys.domain.model.task.ExerciseId
import tech.testsys.domain.model.task.JudgmentOrderId
import tech.testsys.domain.model.task.SolutionId
import tech.testsys.domain.model.task.SubmissionId
import tech.testsys.domain.model.task.TaskId
import tech.testsys.domain.model.task.TestId
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
import tech.testsys.domain.model.user.Student
import tech.testsys.domain.model.user.StudentData
import tech.testsys.domain.model.user.UserId
import java.time.Instant

/**
 * Abstract base builder for [CompatibleUserRole] instances.
 * Provides community membership configuration common to all compatible roles.
 *
 * @param Role the concrete role type being built.
 * @since %CURRENT_VERSION%
 */
abstract class CompatibleUserRoleBuilder<Role : CompatibleUserRole> : Builder<Role> {

    /**
     * The list of communities this role is a member of.
     *
     * @since %CURRENT_VERSION%
     */
    var memberOf = mutableListOf<CommunityId>()

    /**
     * Sets the community membership list from raw ID values.
     *
     * @param communities the raw community IDs.
     * @since %CURRENT_VERSION%
     */
    fun memberOf(communities: Iterable<Long>) {
        memberOf = communities.map { CommunityId(it) }.toMutableList()
    }

}

/**
 * Builder for constructing [DeveloperData].
 *
 * @since %CURRENT_VERSION%
 */
class DeveloperDataBuilder : Builder<DeveloperData> {

    /**
     * The list of tasks owned by the developer.
     *
     * @since %CURRENT_VERSION%
     */
    var tasks = mutableListOf<TaskId>()

    /**
     * The list of contests owned by the developer.
     *
     * @since %CURRENT_VERSION%
     */
    var contests = mutableListOf<ContestId>()

    /**
     * The list of test polygons owned by the developer.
     *
     * @since %CURRENT_VERSION%
     */
    var polygons = mutableListOf<TestId>()

    /**
     * The list of solutions owned by the developer.
     *
     * @since %CURRENT_VERSION%
     */
    var solutions = mutableListOf<SolutionId>()

    /**
     * The list of exercises owned by the developer.
     *
     * @since %CURRENT_VERSION%
     */
    var exercises = mutableListOf<ExerciseId>()

    /**
     * Sets the [tasks] list from raw ID values.
     *
     * @param tasks the raw task IDs.
     * @since %CURRENT_VERSION%
     */
    fun tasks(tasks: Iterable<Long>) {
        this.tasks = tasks.map { TaskId(it) }.toMutableList()
    }

    /**
     * Sets the [contests] list from raw ID values.
     *
     * @param contests the raw contest IDs.
     * @since %CURRENT_VERSION%
     */
    fun contests(contests: Iterable<Long>) {
        this.contests = contests.map { ContestId(it) }.toMutableList()
    }

    /**
     * Sets the [polygons] list from raw ID values.
     *
     * @param polygons the raw test polygon IDs.
     * @since %CURRENT_VERSION%
     */
    fun polygons(polygons: Iterable<Long>) {
        this.polygons = polygons.map { TestId(it) }.toMutableList()
    }

    /**
     * Sets the [solutions] list from raw ID values.
     *
     * @param solutions the raw solution IDs.
     * @since %CURRENT_VERSION%
     */
    fun solutions(solutions: Iterable<Long>) {
        this.solutions = solutions.map { SolutionId(it) }.toMutableList()
    }

    /**
     * Sets the [exercises] list from raw ID values.
     *
     * @param exercises the raw exercise IDs.
     * @since %CURRENT_VERSION%
     */
    fun exercises(exercises: Iterable<Long>) {
        this.exercises = exercises.map { ExerciseId(it) }.toMutableList()
    }

    /**
     * Builds the [DeveloperData] instance.
     *
     * @return the constructed [DeveloperData].
     * @since %CURRENT_VERSION%
     */
    override fun build(): DeveloperData {
        return DeveloperData(
            tasks = tasks.lazify(),
            contests = contests.lazify(),
            polygons = polygons.lazify(),
            solutions = solutions.lazify(),
            exercises = exercises.lazify(),
        )
    }

}

/**
 * Builder for constructing a [Developer] role.
 * Supports configuring developer data via [DataCapable].
 *
 * @since %CURRENT_VERSION%
 */
class DeveloperBuilder : CompatibleUserRoleBuilder<Developer>(), DataCapable<DeveloperData, DeveloperDataBuilder> {

    override var data: DeveloperData? = null
    override fun dataBuilder() = DeveloperDataBuilder()

    /**
     * Builds the [Developer] role instance.
     *
     * @return the constructed [Developer].
     * @throws IllegalArgumentException if [data] is not set.
     * @since %CURRENT_VERSION%
     */
    override fun build(): Developer {
        val data = requireField(data, "data")
        return Developer(
            memberOf = memberOf.lazify(),
            data = data,
        )
    }

}

/**
 * Builder for constructing [StudentData].
 *
 * @since %CURRENT_VERSION%
 */
class StudentDataBuilder : Builder<StudentData> {

    /**
     * The list of classes the student belongs to.
     *
     * @since %CURRENT_VERSION%
     */
    var classes = mutableListOf<ClassId>()

    /**
     * The list of submissions made by the student.
     *
     * @since %CURRENT_VERSION%
     */
    var submissions = mutableListOf<SubmissionId>()

    /**
     * Sets the [classes] list from raw ID values.
     *
     * @param classes the raw class IDs.
     * @since %CURRENT_VERSION%
     */
    fun classes(classes: Iterable<Long>) {
        this.classes = classes.map { ClassId(it) }.toMutableList()
    }

    /**
     * Sets the [submissions] list from raw ID values.
     *
     * @param submissions the raw submission IDs.
     * @since %CURRENT_VERSION%
     */
    fun submissions(submissions: Iterable<Long>) {
        this.submissions = submissions.map { SubmissionId(it) }.toMutableList()
    }

    /**
     * Builds the [StudentData] instance.
     *
     * @return the constructed [StudentData].
     * @since %CURRENT_VERSION%
     */
    override fun build(): StudentData {
        return StudentData(
            classes = classes.lazify(),
            submissions = submissions.lazify(),
        )
    }

}

/**
 * Builder for constructing a [Student] role.
 * Supports configuring student data via [DataCapable].
 *
 * @since %CURRENT_VERSION%
 */
class StudentBuilder : CompatibleUserRoleBuilder<Student>(), DataCapable<StudentData, StudentDataBuilder> {

    override var data: StudentData? = null
    override fun dataBuilder() = StudentDataBuilder()

    /**
     * Builds the [Student] role instance.
     *
     * @return the constructed [Student].
     * @throws IllegalArgumentException if [data] is not set.
     * @since %CURRENT_VERSION%
     */
    override fun build(): Student {
        val data = requireField(data, "data")
        return Student(
            memberOf = memberOf.lazify(),
            data = data,
        )
    }

}

/**
 * Builder for constructing an [Administrator] role.
 * Administrators have no additional data beyond community membership.
 *
 * @since %CURRENT_VERSION%
 */
class AdministratorBuilder : CompatibleUserRoleBuilder<Administrator>() {

    /**
     * Builds the [Administrator] role instance.
     *
     * @return the constructed [Administrator].
     * @since %CURRENT_VERSION%
     */
    override fun build() = Administrator(
        memberOf = memberOf.lazify(),
    )

}

/**
 * Builder for constructing [JudgeData].
 *
 * @since %CURRENT_VERSION%
 */
class JudgeDataBuilder : Builder<JudgeData> {

    /**
     * The list of judgment orders assigned to this judge.
     *
     * @since %CURRENT_VERSION%
     */
    var judgmentOrders = mutableListOf<JudgmentOrderId>()

    /**
     * Sets the [judgmentOrders] list from raw ID values.
     *
     * @param judgmentOrders the raw judgment order IDs.
     * @since %CURRENT_VERSION%
     */
    fun judgmentOrders(judgmentOrders: Iterable<Long>) {
        this.judgmentOrders = judgmentOrders.map { JudgmentOrderId(it) }.toMutableList()
    }

    /**
     * Builds the [JudgeData] instance.
     *
     * @return the constructed [JudgeData].
     * @since %CURRENT_VERSION%
     */
    override fun build() = JudgeData(
        judgmentOrders = judgmentOrders.lazify(),
    )

}

/**
 * Builder for constructing a [Judge] role.
 * Supports configuring judge data via [DataCapable].
 *
 * @since %CURRENT_VERSION%
 */
class JudgeBuilder : CompatibleUserRoleBuilder<Judge>(), DataCapable<JudgeData, JudgeDataBuilder> {

    override var data: JudgeData? = null
    override fun dataBuilder() = JudgeDataBuilder()

    /**
     * Builds the [Judge] role instance.
     *
     * @return the constructed [Judge].
     * @throws IllegalArgumentException if [data] is not set.
     * @since %CURRENT_VERSION%
     */
    override fun build(): Judge {
        val data = requireField(data, "data")
        return Judge(
            memberOf = memberOf.lazify(),
            data = data,
        )
    }

}

/**
 * Builder for constructing [ManagerData].
 *
 * @since %CURRENT_VERSION%
 */
class ManagerDataBuilder : Builder<ManagerData> {

    /**
     * The list of classes managed by this manager.
     *
     * @since %CURRENT_VERSION%
     */
    var classes = mutableListOf<ClassId>()

    /**
     * The list of competitions managed by this manager.
     *
     * @since %CURRENT_VERSION%
     */
    var competitions = mutableListOf<CompetitionId>()

    /**
     * Sets the [classes] list from raw ID values.
     *
     * @param classes the raw class IDs.
     * @since %CURRENT_VERSION%
     */
    fun classes(classes: Iterable<Long>) {
        this.classes = classes.map { ClassId(it) }.toMutableList()
    }

    /**
     * Sets the [competitions] list from raw ID values.
     *
     * @param competitions the raw competition IDs.
     * @since %CURRENT_VERSION%
     */
    fun competitions(competitions: Iterable<Long>) {
        this.competitions = competitions.map { CompetitionId(it) }.toMutableList()
    }

    /**
     * Builds the [ManagerData] instance.
     *
     * @return the constructed [ManagerData].
     * @since %CURRENT_VERSION%
     */
    override fun build() = ManagerData(
        competitions = competitions.lazify(),
        classes = classes.lazify(),
    )

}

/**
 * Builder for constructing a [Manager] role.
 * Supports configuring manager data via [DataCapable].
 *
 * @since %CURRENT_VERSION%
 */
class ManagerBuilder : CompatibleUserRoleBuilder<Manager>(), DataCapable<ManagerData, ManagerDataBuilder> {

    override var data: ManagerData? = null
    override fun dataBuilder() = ManagerDataBuilder()

    /**
     * Builds the [Manager] role instance.
     *
     * @return the constructed [Manager].
     * @throws IllegalArgumentException if [data] is not set.
     * @since %CURRENT_VERSION%
     */
    override fun build(): Manager {
        val data = requireField(data, "data")
        return Manager(
            memberOf = memberOf.lazify(),
            data = data,
        )
    }

}

private typealias Roles = MutableList<CompatibleUserRole>

/**
 * Builder for constructing [MultipleRoleUserData], which holds a collection of compatible user roles.
 *
 * @since %CURRENT_VERSION%
 */
class MultipleRoleUserDataBuilder : Builder<MultipleRoleUserData> {

    private var roles: Roles = mutableListOf()

    /**
     * Configures the roles list using a DSL block.
     *
     * @param builder the configuration block applied to the roles list.
     * @since %CURRENT_VERSION%
     */
    fun roles(builder: Roles.() -> Unit) {
        roles.builder()
    }

    /**
     * Adds a [Developer] role to the roles list.
     *
     * @param builder the configuration block applied to [DeveloperBuilder].
     * @since %CURRENT_VERSION%
     */
    fun Roles.developer(builder: DeveloperBuilder.() -> Unit) {
        DeveloperBuilder()
            .apply(builder)
            .build()
            .also { add(it) }
    }

    /**
     * Adds a [Student] role to the roles list.
     *
     * @param builder the configuration block applied to [StudentBuilder].
     * @since %CURRENT_VERSION%
     */
    fun Roles.student(builder: StudentBuilder.() -> Unit) {
        StudentBuilder()
            .apply(builder)
            .build()
            .also { add(it) }
    }

    /**
     * Adds a [Judge] role to the roles list.
     *
     * @param builder the configuration block applied to [JudgeBuilder].
     * @since %CURRENT_VERSION%
     */
    fun Roles.judge(builder: JudgeBuilder.() -> Unit) {
        JudgeBuilder()
            .apply(builder)
            .build()
            .also { add(it) }
    }

    /**
     * Adds a [Manager] role to the roles list.
     *
     * @param builder the configuration block applied to [ManagerBuilder].
     * @since %CURRENT_VERSION%
     */
    fun Roles.manager(builder: ManagerBuilder.() -> Unit) {
        ManagerBuilder()
            .apply(builder)
            .build()
            .also { add(it) }
    }

    /**
     * Adds an [Administrator] role to the roles list.
     *
     * @param builder the configuration block applied to [AdministratorBuilder].
     * @since %CURRENT_VERSION%
     */
    fun Roles.administrator(builder: AdministratorBuilder.() -> Unit) {
        AdministratorBuilder()
            .apply(builder)
            .build()
            .also { add(it) }
    }

    /**
     * Builds the [MultipleRoleUserData] instance.
     *
     * @return the constructed [MultipleRoleUserData].
     * @since %CURRENT_VERSION%
     */
    override fun build() = MultipleRoleUserData(
        roles = roles,
    )

}

/**
 * Builder for constructing [MultipleRoleUser] domain entities.
 * Supports configuring user data (roles) via [DataCapable].
 *
 * @since %CURRENT_VERSION%
 */
class MultipleRoleUserBuilder :
    UserBuilder<MultipleRoleUser>(),
    DataCapable<MultipleRoleUserData, MultipleRoleUserDataBuilder>
{

    override var id: Long? = null
    override var createdAt: Instant? = null
    override var data: MultipleRoleUserData? = null
    override fun dataBuilder() = MultipleRoleUserDataBuilder()

    /**
     * Builds the [MultipleRoleUser] instance.
     *
     * @return the constructed [MultipleRoleUser].
     * @throws IllegalArgumentException if any required field is not set.
     * @since %CURRENT_VERSION%
     */
    override fun build(): MultipleRoleUser {
        val id = requireField(id, "id")
        val createdAt = requireField(createdAt, "createdAt")
        val accessToken = requireField(accessToken, "accessToken")
        val data = requireField(data, "data")

        return MultipleRoleUser(
            id = UserId(id),
            createdAt = createdAt,
            accessToken = accessToken,
            data = data,
        )
    }

}

/**
 * DSL entry point for building [MultipleRoleUserData].
 *
 * @param builder the configuration block applied to [MultipleRoleUserDataBuilder].
 * @return the constructed [MultipleRoleUserData].
 * @since %CURRENT_VERSION%
 */
inline fun buildMultipleRoleUserData(builder: MultipleRoleUserDataBuilder.() -> Unit) =
    MultipleRoleUserDataBuilder().apply(builder).build()

/**
 * DSL entry point for building a [MultipleRoleUser].
 *
 * @param builder the configuration block applied to [MultipleRoleUserBuilder].
 * @return the constructed [MultipleRoleUser].
 * @since %CURRENT_VERSION%
 */
inline fun buildMultipleRoleUser(builder: MultipleRoleUserBuilder.() -> Unit) =
    MultipleRoleUserBuilder().apply(builder).build()
