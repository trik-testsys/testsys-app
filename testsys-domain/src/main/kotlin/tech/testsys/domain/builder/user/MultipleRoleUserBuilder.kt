package tech.testsys.domain.builder.user

import tech.testsys.domain.builder.Builder
import tech.testsys.domain.builder.DataCapable
import tech.testsys.domain.builder.util.lazify
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

abstract class CompatibleUserRoleBuilder<Role : CompatibleUserRole> : Builder<Role> {

    var memberOf = mutableListOf<CommunityId>()

    fun memberOf(communities: Iterable<Long>) {
        memberOf = communities.map { CommunityId(it) }.toMutableList()
    }

}

class DeveloperDataBuilder : Builder<DeveloperData> {

    var tasks = mutableListOf<TaskId>()
    var contests = mutableListOf<ContestId>()
    var polygons = mutableListOf<TestId>()
    var solutions = mutableListOf<SolutionId>()
    var exercises = mutableListOf<ExerciseId>()

    fun tasks(tasks: Iterable<Long>) {
        this.tasks = tasks.map { TaskId(it) }.toMutableList()
    }

    fun contests(contests: Iterable<Long>) {
        this.contests = contests.map { ContestId(it) }.toMutableList()
    }

    fun polygons(polygons: Iterable<Long>) {
        this.polygons = polygons.map { TestId(it) }.toMutableList()
    }

    fun solutions(solutions: Iterable<Long>) {
        this.solutions = solutions.map { SolutionId(it) }.toMutableList()
    }

    fun exercises(exercises: Iterable<Long>) {
        this.exercises = exercises.map { ExerciseId(it) }.toMutableList()
    }

    override fun build(): DeveloperData {
        return DeveloperData(
            tasks = tasks.lazify(),
            contest = contests.lazify(),
            polygons = polygons.lazify(),
            solutions = solutions.lazify(),
            exercises = exercises.lazify(),
        )
    }

}

class DeveloperBuilder : CompatibleUserRoleBuilder<Developer>(), DataCapable<DeveloperData, DeveloperDataBuilder> {

    override var data: DeveloperData? = null
    override fun dataBuilder() = DeveloperDataBuilder()

    override fun build(): Developer {
        val data = requireNotNull(data)
        return Developer(
            memberOf = memberOf.lazify(),
            data = data,
        )
    }

}

class StudentDataBuilder : Builder<StudentData> {

    var classes = mutableListOf<ClassId>()
    var submissions = mutableListOf<SubmissionId>()

    fun classes(classes: Iterable<Long>) {
        this.classes = classes.map { ClassId(it) }.toMutableList()
    }

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

class StudentBuilder : CompatibleUserRoleBuilder<Student>(), DataCapable<StudentData, StudentDataBuilder> {

    override var data: StudentData? = null
    override fun dataBuilder() = StudentDataBuilder()

    override fun build(): Student {
        val data = requireNotNull(data)
        return Student(
            memberOf = memberOf.lazify(),
            data = data,
        )
    }

}

class AdministratorBuilder : CompatibleUserRoleBuilder<Administrator>() {

    override fun build() = Administrator(
        memberOf = memberOf.lazify(),
    )

}

class JudgeDataBuilder : Builder<JudgeData> {

    var judgmentOrders = mutableListOf<JudgmentOrderId>()

    fun judgmentOrders(judgmentOrders: Iterable<Long>) {
        this.judgmentOrders = judgmentOrders.map { JudgmentOrderId(it) }.toMutableList()
    }

    override fun build() = JudgeData(
        judgmentOrders = judgmentOrders.lazify(),
    )

}

class JudgeBuilder : CompatibleUserRoleBuilder<Judge>(), DataCapable<JudgeData, JudgeDataBuilder> {

    override var data: JudgeData? = null
    override fun dataBuilder() = JudgeDataBuilder()

    override fun build(): Judge {
        val data = requireNotNull(data)
        return Judge(
            memberOf = memberOf.lazify(),
            data = data,
        )
    }

}

class ManagerDataBuilder : Builder<ManagerData> {

    var classes = mutableListOf<ClassId>()
    var competitions = mutableListOf<CompetitionId>()

    fun classes(classes: Iterable<Long>) {
        this.classes = classes.map { ClassId(it) }.toMutableList()
    }

    fun competitions(competitions: Iterable<Long>) {
        this.competitions = competitions.map { CompetitionId(it) }.toMutableList()
    }

    override fun build() = ManagerData(
        competitions = competitions.lazify(),
        classes = classes.lazify(),
    )

}

class ManagerBuilder : CompatibleUserRoleBuilder<Manager>(), DataCapable<ManagerData, ManagerDataBuilder> {

    override var data: ManagerData? = null
    override fun dataBuilder() = ManagerDataBuilder()

    override fun build(): Manager {
        val data = requireNotNull(data)
        return Manager(
            memberOf = memberOf.lazify(),
            data = data,
        )
    }

}

private typealias Roles = MutableList<CompatibleUserRole>

class MultipleRoleUserDataBuilder : Builder<MultipleRoleUserData> {

    private var roles: Roles = mutableListOf()

    fun roles(builder: Roles.() -> Unit) {
        roles.builder()
    }

    fun Roles.developer(builder: DeveloperBuilder.() -> Unit) {
        DeveloperBuilder()
            .apply(builder)
            .build()
            .also { add(it) }
    }

    fun Roles.student(builder: StudentBuilder.() -> Unit) {
        StudentBuilder()
            .apply(builder)
            .build()
            .also { add(it) }
    }

    fun Roles.judge(builder: JudgeBuilder.() -> Unit) {
        JudgeBuilder()
            .apply(builder)
            .build()
            .also { add(it) }
    }

    fun Roles.manager(builder: ManagerBuilder.() -> Unit) {
        ManagerBuilder()
            .apply(builder)
            .build()
            .also { add(it) }
    }

    fun Roles.administrator(builder: AdministratorBuilder.() -> Unit) {
        AdministratorBuilder()
            .apply(builder)
            .build()
            .also { add(it) }
    }

    override fun build() = MultipleRoleUserData(
        roles = roles,
    )

}

class MultipleRoleUserBuilder :
    UserBuilder<MultipleRoleUser>(),
    DataCapable<MultipleRoleUserData, MultipleRoleUserDataBuilder>
{

    override var id: Long? = null
    override var createdAt: Instant? = null
    override var data: MultipleRoleUserData? = null
    override fun dataBuilder() = MultipleRoleUserDataBuilder()

    override fun build(): MultipleRoleUser {
        val id = requireNotNull(id)
        val createdAt = requireNotNull(createdAt)
        val accessToken = requireNotNull(accessToken)
        val data = requireNotNull(data)

        return MultipleRoleUser(
            id = UserId(id),
            createdAt = createdAt,
            accessToken = accessToken,
            data = data,
        )
    }

}

inline fun buildMultipleRoleUserData(builder: MultipleRoleUserDataBuilder.() -> Unit) =
    MultipleRoleUserDataBuilder().apply(builder).build()

inline fun buildMultipleRoleUser(builder: MultipleRoleUserBuilder.() -> Unit) =
    MultipleRoleUserBuilder().apply(builder).build()
