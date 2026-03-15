package tech.testsys.domain.builder.task

import tech.testsys.domain.builder.Builder
import tech.testsys.domain.builder.DomainEntityWithDataBuilder
import tech.testsys.domain.builder.util.lazify
import tech.testsys.domain.builder.user.MultipleRoleUserBuilder
import tech.testsys.domain.builder.util.chooser.LanguageChooser
import tech.testsys.domain.builder.util.requireField
import tech.testsys.domain.model.task.DeveloperSolution
import tech.testsys.domain.model.task.DeveloperSolutionData
import tech.testsys.domain.model.task.DeveloperSolutionId
import tech.testsys.domain.model.task.Exercise
import tech.testsys.domain.model.task.ExerciseData
import tech.testsys.domain.model.task.ExerciseId
import tech.testsys.domain.model.task.FileData
import tech.testsys.domain.model.task.Solution
import tech.testsys.domain.model.task.SolutionData
import tech.testsys.domain.model.task.SolutionId
import tech.testsys.domain.model.task.Task
import tech.testsys.domain.model.task.TaskData
import tech.testsys.domain.model.task.TaskId
import tech.testsys.domain.model.task.Test
import tech.testsys.domain.model.task.TestData
import tech.testsys.domain.model.task.TestId
import tech.testsys.domain.model.task.TrikStudioVersion
import tech.testsys.domain.model.task.VerdictId
import tech.testsys.domain.model.task.VersionData
import tech.testsys.domain.model.user.MultipleRoleUser

class DeveloperSolutionDataBuilder : Builder<DeveloperSolutionData> {

    var solution: SolutionId? = null
    var expectedVerdict: VerdictId? = null

    fun solution(solution: Long) {
        this.solution = SolutionId(solution)
    }

    fun expectedVerdict(expectedVerdict: Long) {
        this.expectedVerdict = VerdictId(expectedVerdict)
    }

    override fun build(): DeveloperSolutionData {
        val solution = requireField(solution, "solution")
        val expectedVerdict = requireField(expectedVerdict, "expectedVerdict")

        return DeveloperSolutionData(
            solution = solution.lazify(),
            expectedVerdict = expectedVerdict.lazify(),
        )
    }

}

class DeveloperSolutionBuilder :
    DomainEntityWithDataBuilder<DeveloperSolution, DeveloperSolutionData, DeveloperSolutionDataBuilder>()
{

    override fun dataBuilder() = DeveloperSolutionDataBuilder()

    override fun build(): DeveloperSolution {
        val id = requireField(id, "id")
        val createdAt = requireField(createdAt, "createdAt")
        val data = requireField(data, "data")

        return DeveloperSolution(
            id = DeveloperSolutionId(id),
            createdAt = createdAt,
            data = data,
        )
    }

}

inline fun buildDeveloperSolutionData(builder: DeveloperSolutionDataBuilder.() -> Unit) =
    DeveloperSolutionDataBuilder().apply(builder).build()

inline fun buildDeveloperSolution(builder: DeveloperSolutionBuilder.() -> Unit) =
    DeveloperSolutionBuilder().apply(builder).build()

class TaskDataBuilder : Builder<TaskData> {

    var owner: MultipleRoleUser? = null
    var name: String? = null
    var description: String? = null
    var tests = mutableListOf<TestId>()
    var exercise: ExerciseId? = null
    var developerSolutions = mutableListOf<DeveloperSolutionId>()
    private var _trikStudioVersion: TrikStudioVersion? = null

    fun owner(builder: MultipleRoleUserBuilder.() -> Unit) {
        owner = MultipleRoleUserBuilder().apply(builder).build()
    }

    fun tests(tests: Iterable<Long>) {
        this.tests = tests.map { TestId(it) }.toMutableList()
    }

    fun exercise(exercise: Long) {
        this.exercise = ExerciseId(exercise)
    }

    fun developerSolutions(developerSolutions: Iterable<Long>) {
        this.developerSolutions = developerSolutions.map { DeveloperSolutionId(it) }.toMutableList()
    }

    fun trikStudioVersion(image: String, tag: String) {
        _trikStudioVersion = TrikStudioVersion(
            image = image,
            tag = tag,
        )
    }

    override fun build(): TaskData {
        val owner = requireField(owner, "owner")
        val name = requireField(name, "name")
        val description = requireField(description, "description")
        val exercise = requireField(exercise, "exercise")
        val trikStudioVersion = requireField(_trikStudioVersion, "trikStudioVersion")

        return TaskData(
            owner = owner,
            name = name,
            description = description,
            tests = tests.lazify(),
            exercise = exercise.lazify(),
            developerSolutions = developerSolutions.lazify(),
            trikStudioVersion = trikStudioVersion,
        )
    }

}

class TaskBuilder : DomainEntityWithDataBuilder<Task, TaskData, TaskDataBuilder>() {

    override fun dataBuilder() = TaskDataBuilder()

    override fun build(): Task {
        val id = requireField(id, "id")
        val createdAt = requireField(createdAt, "createdAt")
        val data = requireField(data, "data")

        return Task(
            id = TaskId(id),
            createdAt = createdAt,
            data = data,
        )
    }

}

inline fun buildTaskData(builder: TaskDataBuilder.() -> Unit) = TaskDataBuilder().apply(builder).build()

inline fun buildTask(builder: TaskBuilder.() -> Unit) = TaskBuilder().apply(builder).build()

class TestDataBuilder : Builder<TestData> {

    private var _file: FileData? = null
    private var _versionData: VersionData<TestId, Test>? = null

    fun file(uploadedFilename: String, content: ByteArray) {
        _file = FileData(uploadedFilename, content)
    }

    fun versionData(root: TestId, index: Long) {
        _versionData = VersionData(root.lazify(), index)
    }

    val language = LanguageChooser()

    override fun build(): TestData {
        val file = requireField(_file, "file")
        val versionData = requireField(_versionData, "versionData")
        val language = requireField(language.choice, "language")

        return TestData(
            file = file,
            versionData = versionData,
            language = language,
        )
    }

}

class TestBuilder : DomainEntityWithDataBuilder<Test, TestData, TestDataBuilder>() {

    override fun dataBuilder() = TestDataBuilder()

    override fun build(): Test {
        val id = requireField(id, "id")
        val createdAt = requireField(createdAt, "createdAt")
        val data = requireField(data, "data")

        return Test(
            id = TestId(id),
            createdAt = createdAt,
            data = data,
        )
    }

}

inline fun buildTestData(builder: TestDataBuilder.() -> Unit) = TestDataBuilder().apply(builder).build()

inline fun buildTest(builder: TestBuilder.() -> Unit) = TestBuilder().apply(builder).build()

class SolutionDataBuilder : Builder<SolutionData> {

    private var _file: FileData? = null

    fun file(uploadedFilename: String, content: ByteArray) {
        _file = FileData(uploadedFilename, content)
    }

    val language = LanguageChooser()

    override fun build(): SolutionData {
        val file = requireField(_file, "file")
        val language = requireField(language.choice, "language")

        return SolutionData(
            file = file,
            language = language,
        )
    }

}

class SolutionBuilder : DomainEntityWithDataBuilder<Solution, SolutionData, SolutionDataBuilder>() {

    override fun dataBuilder() = SolutionDataBuilder()

    override fun build(): Solution {
        val id = requireField(id, "id")
        val createdAt = requireField(createdAt, "createdAt")
        val data = requireField(data, "data")

        return Solution(
            id = SolutionId(id),
            createdAt = createdAt,
            data = data,
        )
    }

}

inline fun buildSolutionData(builder: SolutionDataBuilder.() -> Unit) = SolutionDataBuilder().apply(builder).build()

inline fun buildSolution(builder: SolutionBuilder.() -> Unit) = SolutionBuilder().apply(builder).build()

class ExerciseDataBuilder : Builder<ExerciseData> {

    private var _file: FileData? = null

    fun file(uploadedFilename: String, content: ByteArray) {
        _file = FileData(uploadedFilename, content)
    }

    val language = LanguageChooser()

    override fun build(): ExerciseData {
        val file = requireField(_file, "file")
        val language = requireField(language.choice, "language")

        return ExerciseData(
            file = file,
            language = language,
        )
    }

}

class ExerciseBuilder : DomainEntityWithDataBuilder<Exercise, ExerciseData, ExerciseDataBuilder>() {

    private var _versionData: VersionData<ExerciseId, Exercise>? = null

    fun versionData(root: ExerciseId, index: Long) {
        _versionData = VersionData(root.lazify(), index)
    }

    override fun dataBuilder() = ExerciseDataBuilder()

    override fun build(): Exercise {
        val id = requireField(id, "id")
        val createdAt = requireField(createdAt, "createdAt")
        val versionData = requireField(_versionData, "versionData")
        val data = requireField(data, "data")

        return Exercise(
            id = ExerciseId(id),
            createdAt = createdAt,
            versionData = versionData,
            data = data,
        )
    }

}

inline fun buildExerciseData(builder: ExerciseDataBuilder.() -> Unit) = ExerciseDataBuilder().apply(builder).build()

inline fun buildExercise(builder: ExerciseBuilder.() -> Unit) = ExerciseBuilder().apply(builder).build()
