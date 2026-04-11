package tech.testsys.domain.builder.task

import tech.testsys.domain.builder.Builder
import tech.testsys.domain.builder.DomainEntityWithDataBuilder
import tech.testsys.domain.builder.util.lazify
import tech.testsys.domain.builder.util.chooser.LanguageChooser
import tech.testsys.domain.builder.util.requireField
import tech.testsys.domain.model.group.CommunityId
import tech.testsys.domain.model.task.DeveloperSolution
import tech.testsys.domain.model.task.DeveloperSolutionData
import tech.testsys.domain.model.task.DeveloperSolutionId
import tech.testsys.domain.model.task.Exercise
import tech.testsys.domain.model.task.ExerciseData
import tech.testsys.domain.model.task.ExerciseId
import tech.testsys.domain.model.task.FileData
import tech.testsys.domain.model.task.Score
import tech.testsys.domain.model.task.Solution
import tech.testsys.domain.model.task.SolutionData
import tech.testsys.domain.model.task.SolutionId
import tech.testsys.domain.model.task.StatementId
import tech.testsys.domain.model.task.Task
import tech.testsys.domain.model.task.TaskData
import tech.testsys.domain.model.task.TaskId
import tech.testsys.domain.model.task.Test
import tech.testsys.domain.model.task.TestData
import tech.testsys.domain.model.task.TestId
import tech.testsys.domain.model.task.TrikStudioVersion
import tech.testsys.domain.model.task.VersionData
import tech.testsys.domain.model.user.MultipleRoleUserId

/**
 * Builder for constructing [DeveloperSolutionData].
 *
 * @since %CURRENT_VERSION%
 */
class DeveloperSolutionDataBuilder : Builder<DeveloperSolutionData> {

    /**
     * The ID of the solution associated with this developer solution.
     *
     * @since %CURRENT_VERSION%
     */
    var solution: SolutionId? = null

    /**
     * The ID of the expected verdict for this developer solution.
     *
     * @since %CURRENT_VERSION%
     */
    var expectedScore: Score? = null

    /**
     * Sets the [solution] from a raw ID value.
     *
     * @param solution the raw solution ID.
     * @since %CURRENT_VERSION%
     */
    fun solution(solution: Long) {
        this.solution = SolutionId(solution)
    }

    /**
     * Sets the [expectedScore] from a raw ID value.
     *
     * @param expectedScore the raw verdict ID.
     * @since %CURRENT_VERSION%
     */
    fun expectedScore(expectedScore: Int) {
        this.expectedScore = Score(expectedScore)
    }

    /**
     * Builds the [DeveloperSolutionData] instance.
     *
     * @return the constructed [DeveloperSolutionData].
     * @throws IllegalArgumentException if any required field is not set.
     * @since %CURRENT_VERSION%
     */
    override fun build(): DeveloperSolutionData {
        val solution = requireField(solution) { ::solution }
        val expectedScore = requireField(expectedScore) { ::expectedScore }

        return DeveloperSolutionData(
            solution = solution.lazify(),
            expectedScore = expectedScore
        )
    }

}

/**
 * Builder for constructing [DeveloperSolution] domain entities.
 *
 * @since %CURRENT_VERSION%
 */
class DeveloperSolutionBuilder :
    DomainEntityWithDataBuilder<DeveloperSolution, DeveloperSolutionData, DeveloperSolutionDataBuilder>()
{

    override fun dataBuilder() = DeveloperSolutionDataBuilder()

    /**
     * Builds the [DeveloperSolution] instance.
     *
     * @return the constructed [DeveloperSolution].
     * @throws IllegalArgumentException if any required field is not set.
     * @since %CURRENT_VERSION%
     */
    override fun build(): DeveloperSolution {
        val id = requireField(id) { ::id }
        val createdAt = requireField(createdAt) { ::createdAt }
        val data = requireField(data) { ::data }

        return DeveloperSolution(
            id = DeveloperSolutionId(id),
            createdAt = createdAt,
            data = data,
        )
    }

}

/**
 * DSL entry point for building [DeveloperSolutionData].
 *
 * @param builder the configuration block applied to [DeveloperSolutionDataBuilder].
 * @return the constructed [DeveloperSolutionData].
 * @since %CURRENT_VERSION%
 */
inline fun buildDeveloperSolutionData(builder: DeveloperSolutionDataBuilder.() -> Unit) =
    DeveloperSolutionDataBuilder().apply(builder).build()

/**
 * DSL entry point for building a [DeveloperSolution].
 *
 * @param builder the configuration block applied to [DeveloperSolutionBuilder].
 * @return the constructed [DeveloperSolution].
 * @since %CURRENT_VERSION%
 */
inline fun buildDeveloperSolution(builder: DeveloperSolutionBuilder.() -> Unit) =
    DeveloperSolutionBuilder().apply(builder).build()

/**
 * Builder for constructing [TaskData].
 *
 * @since %CURRENT_VERSION%
 */
class TaskDataBuilder : Builder<TaskData> {

    /**
     * The owner of the task.
     *
     * @since %CURRENT_VERSION%
     */
    var owner: MultipleRoleUserId? = null

    /**
     * The name of the task.
     *
     * @since %CURRENT_VERSION%
     */
    var name: String? = null

    /**
     * The description of the task.
     *
     * @since %CURRENT_VERSION%
     */
    var description: String? = null

    /**
     * The list of test IDs associated with this task.
     *
     * @since %CURRENT_VERSION%
     */
    var tests = mutableListOf<TestId>()

    /**
     * The ID of the exercise associated with this task.
     *
     * @since %CURRENT_VERSION%
     */
    var exercise: ExerciseId? = null

    /**
     * The list of developer solution IDs for this task.
     *
     * @since %CURRENT_VERSION%
     */
    var developerSolutions = mutableListOf<DeveloperSolutionId>()

    var supportedTrikStudioVersions = mutableListOf<TrikStudioVersion>()

    var statement: StatementId? = null

    var sharedTo = mutableListOf<CommunityId>()

    fun statement(statement: Long) {
        this.statement = StatementId(statement)
    }

    fun sharedTo(sharedTo: Iterable<Long>) {
        this.sharedTo = sharedTo.map { CommunityId(it) }.toMutableList()
    }

    fun owner(owner: Long) {
        this.owner = MultipleRoleUserId(owner)
    }

    /**
     * Sets the [tests] list from raw ID values.
     *
     * @param tests the raw test IDs.
     * @since %CURRENT_VERSION%
     */
    fun tests(tests: Iterable<Long>) {
        this.tests = tests.map { TestId(it) }.toMutableList()
    }

    /**
     * Sets the [exercise] from a raw ID value.
     *
     * @param exercise the raw exercise ID.
     * @since %CURRENT_VERSION%
     */
    fun exercise(exercise: Long) {
        this.exercise = ExerciseId(exercise)
    }

    /**
     * Sets the [developerSolutions] list from raw ID values.
     *
     * @param developerSolutions the raw developer solution IDs.
     * @since %CURRENT_VERSION%
     */
    fun developerSolutions(developerSolutions: Iterable<Long>) {
        this.developerSolutions = developerSolutions.map { DeveloperSolutionId(it) }.toMutableList()
    }

    fun supportedTrikStudioVersions(supportedTrikStudioVersions: Iterable<String>) {
        this.supportedTrikStudioVersions = supportedTrikStudioVersions.map { TrikStudioVersion(it) }.toMutableList()
    }

    /**
     * Builds the [TaskData] instance.
     *
     * @return the constructed [TaskData].
     * @throws IllegalArgumentException if any required field is not set.
     * @since %CURRENT_VERSION%
     */
    override fun build(): TaskData {
        val owner = requireField(owner) { ::owner }
        val name = requireField(name) { ::name }
        val description = requireField(description) { ::description }
        val exercise = requireField(exercise) { ::exercise }
        val supportedTrikStudioVersions = requireField(supportedTrikStudioVersions) { ::supportedTrikStudioVersions }
        val statement = requireField(statement) { ::statement }

        return TaskData(
            owner = owner.lazify(),
            name = name,
            description = description,
            tests = tests.lazify(),
            exercise = exercise.lazify(),
            developerSolutions = developerSolutions.lazify(),
            statement = statement.lazify(),
            sharedTo = sharedTo.lazify(),
            supportedTrikStudioVersions = supportedTrikStudioVersions,
        )
    }

}

/**
 * Builder for constructing [Task] domain entities.
 *
 * @since %CURRENT_VERSION%
 */
class TaskBuilder : DomainEntityWithDataBuilder<Task, TaskData, TaskDataBuilder>() {

    override fun dataBuilder() = TaskDataBuilder()

    /**
     * Builds the [Task] instance.
     *
     * @return the constructed [Task].
     * @throws IllegalArgumentException if any required field is not set.
     * @since %CURRENT_VERSION%
     */
    override fun build(): Task {
        val id = requireField(id) { ::id }
        val createdAt = requireField(createdAt) { ::createdAt }
        val data = requireField(data) { ::data }

        return Task(
            id = TaskId(id),
            createdAt = createdAt,
            data = data,
        )
    }

}

/**
 * DSL entry point for building [TaskData].
 *
 * @param builder the configuration block applied to [TaskDataBuilder].
 * @return the constructed [TaskData].
 * @since %CURRENT_VERSION%
 */
inline fun buildTaskData(builder: TaskDataBuilder.() -> Unit) = TaskDataBuilder().apply(builder).build()

/**
 * DSL entry point for building a [Task].
 *
 * @param builder the configuration block applied to [TaskBuilder].
 * @return the constructed [Task].
 * @since %CURRENT_VERSION%
 */
inline fun buildTask(builder: TaskBuilder.() -> Unit) = TaskBuilder().apply(builder).build()

/**
 * Builder for constructing [TestData].
 *
 * @since %CURRENT_VERSION%
 */
class TestDataBuilder : Builder<TestData> {

    private var _file: FileData? = null
    private var _versionData: VersionData<TestId, Test>? = null

    /**
     * Sets the file data for the test.
     *
     * @param uploadedFilename the original filename of the uploaded file.
     * @param content the raw file content as a byte array.
     * @since %CURRENT_VERSION%
     */
    fun file(uploadedFilename: String, content: ByteArray) {
        _file = FileData(uploadedFilename, content)
    }

    /**
     * Sets the version data for the test.
     *
     * @param root the ID of the root test in the version chain.
     * @param index the version index.
     * @since %CURRENT_VERSION%
     */
    fun versionData(root: TestId, index: Long) {
        _versionData = VersionData(root.lazify(), index)
    }

    /**
     * Builds the [TestData] instance.
     *
     * @return the constructed [TestData].
     * @throws IllegalArgumentException if any required field is not set.
     * @since %CURRENT_VERSION%
     */
    override fun build(): TestData {
        val file = requireField(_file) { ::_file }
        val versionData = requireField(_versionData) { ::_versionData }

        return TestData(
            file = file,
            versionData = versionData,
        )
    }

}

/**
 * Builder for constructing [Test] domain entities.
 *
 * @since %CURRENT_VERSION%
 */
class TestBuilder : DomainEntityWithDataBuilder<Test, TestData, TestDataBuilder>() {

    override fun dataBuilder() = TestDataBuilder()

    /**
     * Builds the [Test] instance.
     *
     * @return the constructed [Test].
     * @throws IllegalArgumentException if any required field is not set.
     * @since %CURRENT_VERSION%
     */
    override fun build(): Test {
        val id = requireField(id) { ::id }
        val createdAt = requireField(createdAt) { ::createdAt }
        val data = requireField(data) { ::data }

        return Test(
            id = TestId(id),
            createdAt = createdAt,
            data = data,
        )
    }

}

/**
 * DSL entry point for building [TestData].
 *
 * @param builder the configuration block applied to [TestDataBuilder].
 * @return the constructed [TestData].
 * @since %CURRENT_VERSION%
 */
inline fun buildTestData(builder: TestDataBuilder.() -> Unit) = TestDataBuilder().apply(builder).build()

/**
 * DSL entry point for building a [Test].
 *
 * @param builder the configuration block applied to [TestBuilder].
 * @return the constructed [Test].
 * @since %CURRENT_VERSION%
 */
inline fun buildTest(builder: TestBuilder.() -> Unit) = TestBuilder().apply(builder).build()

/**
 * Builder for constructing [SolutionData].
 *
 * @since %CURRENT_VERSION%
 */
class SolutionDataBuilder : Builder<SolutionData> {

    private var _file: FileData? = null

    /**
     * Sets the file data for the solution.
     *
     * @param uploadedFilename the original filename of the uploaded file.
     * @param content the raw file content as a byte array.
     * @since %CURRENT_VERSION%
     */
    fun file(uploadedFilename: String, content: ByteArray) {
        _file = FileData(uploadedFilename, content)
    }

    /**
     * Chooser for selecting the programming language of the solution.
     *
     * @since %CURRENT_VERSION%
     */
    val language = LanguageChooser()

    /**
     * Builds the [SolutionData] instance.
     *
     * @return the constructed [SolutionData].
     * @throws IllegalArgumentException if any required field is not set.
     * @since %CURRENT_VERSION%
     */
    override fun build(): SolutionData {
        val file = requireField(_file) { ::_file }
        val language = requireField(language.choice) { language::choice }

        return SolutionData(
            file = file,
            language = language,
        )
    }

}

/**
 * Builder for constructing [Solution] domain entities.
 *
 * @since %CURRENT_VERSION%
 */
class SolutionBuilder : DomainEntityWithDataBuilder<Solution, SolutionData, SolutionDataBuilder>() {

    override fun dataBuilder() = SolutionDataBuilder()

    /**
     * Builds the [Solution] instance.
     *
     * @return the constructed [Solution].
     * @throws IllegalArgumentException if any required field is not set.
     * @since %CURRENT_VERSION%
     */
    override fun build(): Solution {
        val id = requireField(id) { ::id }
        val createdAt = requireField(createdAt) { ::createdAt }
        val data = requireField(data) { ::data }

        return Solution(
            id = SolutionId(id),
            createdAt = createdAt,
            data = data,
        )
    }

}

/**
 * DSL entry point for building [SolutionData].
 *
 * @param builder the configuration block applied to [SolutionDataBuilder].
 * @return the constructed [SolutionData].
 * @since %CURRENT_VERSION%
 */
inline fun buildSolutionData(builder: SolutionDataBuilder.() -> Unit) = SolutionDataBuilder().apply(builder).build()

/**
 * DSL entry point for building a [Solution].
 *
 * @param builder the configuration block applied to [SolutionBuilder].
 * @return the constructed [Solution].
 * @since %CURRENT_VERSION%
 */
inline fun buildSolution(builder: SolutionBuilder.() -> Unit) = SolutionBuilder().apply(builder).build()

/**
 * Builder for constructing [ExerciseData].
 *
 * @since %CURRENT_VERSION%
 */
class ExerciseDataBuilder : Builder<ExerciseData> {

    private var _file: FileData? = null

    /**
     * Sets the file data for the exercise.
     *
     * @param uploadedFilename the original filename of the uploaded file.
     * @param content the raw file content as a byte array.
     * @since %CURRENT_VERSION%
     */
    fun file(uploadedFilename: String, content: ByteArray) {
        _file = FileData(uploadedFilename, content)
    }

    /**
     * Chooser for selecting the programming language of the exercise.
     *
     * @since %CURRENT_VERSION%
     */
    val language = LanguageChooser()

    /**
     * Builds the [ExerciseData] instance.
     *
     * @return the constructed [ExerciseData].
     * @throws IllegalArgumentException if any required field is not set.
     * @since %CURRENT_VERSION%
     */
    override fun build(): ExerciseData {
        val file = requireField(_file) { ::_file }
        val language = requireField(language.choice) { language::choice }

        return ExerciseData(
            file = file,
            language = language,
        )
    }

}

/**
 * Builder for constructing [Exercise] domain entities.
 * Exercises support versioning via [versionData].
 *
 * @since %CURRENT_VERSION%
 */
class ExerciseBuilder : DomainEntityWithDataBuilder<Exercise, ExerciseData, ExerciseDataBuilder>() {

    private var _versionData: VersionData<ExerciseId, Exercise>? = null

    /**
     * Sets the version data for the exercise.
     *
     * @param root the ID of the root exercise in the version chain.
     * @param index the version index.
     * @since %CURRENT_VERSION%
     */
    fun versionData(root: ExerciseId, index: Long) {
        _versionData = VersionData(root.lazify(), index)
    }

    override fun dataBuilder() = ExerciseDataBuilder()

    /**
     * Builds the [Exercise] instance.
     *
     * @return the constructed [Exercise].
     * @throws IllegalArgumentException if any required field is not set.
     * @since %CURRENT_VERSION%
     */
    override fun build(): Exercise {
        val id = requireField(id) { ::id }
        val createdAt = requireField(createdAt) { ::createdAt }
        val versionData = requireField(_versionData) { ::_versionData }
        val data = requireField(data) { ::data }

        return Exercise(
            id = ExerciseId(id),
            createdAt = createdAt,
            versionData = versionData,
            data = data,
        )
    }

}

/**
 * DSL entry point for building [ExerciseData].
 *
 * @param builder the configuration block applied to [ExerciseDataBuilder].
 * @return the constructed [ExerciseData].
 * @since %CURRENT_VERSION%
 */
inline fun buildExerciseData(builder: ExerciseDataBuilder.() -> Unit) = ExerciseDataBuilder().apply(builder).build()

/**
 * DSL entry point for building an [Exercise].
 *
 * @param builder the configuration block applied to [ExerciseBuilder].
 * @return the constructed [Exercise].
 * @since %CURRENT_VERSION%
 */
inline fun buildExercise(builder: ExerciseBuilder.() -> Unit) = ExerciseBuilder().apply(builder).build()
