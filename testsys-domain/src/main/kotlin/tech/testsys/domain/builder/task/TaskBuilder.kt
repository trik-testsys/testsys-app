package tech.testsys.domain.builder.task

import tech.testsys.domain.builder.Builder
import tech.testsys.domain.builder.DomainEntityWithDataBuilder
import tech.testsys.domain.builder.util.chooser.TaskContentChooser
import tech.testsys.domain.builder.util.lazify
import tech.testsys.domain.builder.util.requireField
import tech.testsys.domain.model.group.CommunityId
import tech.testsys.domain.model.task.CommittedTaskContent
import tech.testsys.domain.model.task.DeveloperSolutionId
import tech.testsys.domain.model.task.ExerciseId
import tech.testsys.domain.model.task.StatementId
import tech.testsys.domain.model.task.Task
import tech.testsys.domain.model.task.TaskData
import tech.testsys.domain.model.task.TaskId
import tech.testsys.domain.model.task.TestId
import tech.testsys.domain.model.task.TrikStudioVersion
import tech.testsys.domain.model.task.WipTaskContent
import tech.testsys.domain.model.user.MultipleRoleUserId

/**
 * Base class of task content revision builders; holds the resource collections of a revision.
 *
 * @param T the type of the built revision.
 * @property tests the ids of the tests (polygons) of the revision.
 * @property exercise the id of the exercise, or `null` if not set yet.
 * @property statement the id of the statement, or `null` if not set yet.
 * @property developerSolutions the ids of the developer solutions of the revision.
 * @property supportedTrikStudioVersions the TRIK Studio versions supported by the revision.
 * @since %CURRENT_VERSION%
 */
abstract class TaskContentBuilder<T> : Builder<T> {

    var tests = mutableListOf<TestId>()

    var exercise: ExerciseId? = null

    var statement: StatementId? = null

    var developerSolutions = mutableListOf<DeveloperSolutionId>()

    var supportedTrikStudioVersions = mutableListOf<TrikStudioVersion>()

    /**
     * Sets [tests] from raw ids.
     *
     * @since %CURRENT_VERSION%
     */
    fun tests(tests: Iterable<Long>) {
        this.tests = tests.map { TestId(it) }.toMutableList()
    }

    /**
     * Sets [exercise] from a raw id.
     *
     * @since %CURRENT_VERSION%
     */
    fun exercise(exercise: Long) {
        this.exercise = ExerciseId(exercise)
    }

    /**
     * Sets [statement] from a raw id.
     *
     * @since %CURRENT_VERSION%
     */
    fun statement(statement: Long) {
        this.statement = StatementId(statement)
    }

    /**
     * Sets [developerSolutions] from raw ids.
     *
     * @since %CURRENT_VERSION%
     */
    fun developerSolutions(developerSolutions: Iterable<Long>) {
        this.developerSolutions = developerSolutions.map { DeveloperSolutionId(it) }.toMutableList()
    }

    /**
     * Sets [supportedTrikStudioVersions] from raw version tags.
     *
     * @since %CURRENT_VERSION%
     */
    fun supportedTrikStudioVersions(supportedTrikStudioVersions: Iterable<String>) {
        this.supportedTrikStudioVersions = supportedTrikStudioVersions.map { TrikStudioVersion(it) }.toMutableList()
    }
}

/**
 * Builder of [CommittedTaskContent]. Required: [exercise], [statement].
 *
 * @since %CURRENT_VERSION%
 */
class CommittedTaskContentBuilder : TaskContentBuilder<CommittedTaskContent>() {

    override fun build(): CommittedTaskContent {
        val exercise = requireField(exercise) { ::exercise }
        val statement = requireField(statement) { ::statement }

        return CommittedTaskContent(
            tests = tests.lazify(),
            exercise = exercise.lazify(),
            statement = statement.lazify(),
            developerSolutions = developerSolutions.lazify(),
            supportedTrikStudioVersions = supportedTrikStudioVersions,
        )
    }
}

/**
 * Builder of [WipTaskContent]; [exercise] and [statement] are optional.
 *
 * @since %CURRENT_VERSION%
 */
class WipTaskContentBuilder : TaskContentBuilder<WipTaskContent>() {

    override fun build(): WipTaskContent {
        return WipTaskContent(
            tests = tests.lazify(),
            exercise = exercise?.lazify(),
            statement = statement?.lazify(),
            developerSolutions = developerSolutions.lazify(),
            supportedTrikStudioVersions = supportedTrikStudioVersions,
        )
    }
}

/**
 * Builder of [TaskData]. Required: [owner], [name], [description], a choice in [content].
 *
 * @property owner the id of the owning developer, or `null` if not set yet.
 * @property name the name of the task, or `null` if not set yet.
 * @property description the description of the task, or `null` if not set yet.
 * @property sharedTo the ids of the communities the task is shared to.
 * @property content the chooser of the task content variant.
 * @since %CURRENT_VERSION%
 */
class TaskDataBuilder : Builder<TaskData> {

    var owner: MultipleRoleUserId? = null

    var name: String? = null

    var description: String? = null

    var sharedTo = mutableListOf<CommunityId>()

    val content = TaskContentChooser()

    /**
     * Sets [owner] from a raw id.
     *
     * @since %CURRENT_VERSION%
     */
    fun owner(owner: Long) {
        this.owner = MultipleRoleUserId(owner)
    }

    /**
     * Sets [sharedTo] from raw ids.
     *
     * @since %CURRENT_VERSION%
     */
    fun sharedTo(sharedTo: Iterable<Long>) {
        this.sharedTo = sharedTo.map { CommunityId(it) }.toMutableList()
    }

    override fun build(): TaskData {
        val owner = requireField(owner) { ::owner }
        val name = requireField(name) { ::name }
        val description = requireField(description) { ::description }

        return TaskData(
            owner = owner.lazify(),
            name = name,
            description = description,
            sharedTo = sharedTo.lazify(),
            content = content.build(),
        )
    }
}

/**
 * Builder of [Task] entities. Required: [id], [createdAt], [version], [data].
 *
 * @since %CURRENT_VERSION%
 */
class TaskBuilder : DomainEntityWithDataBuilder<Task, TaskData, TaskDataBuilder>() {

    override fun dataBuilder() = TaskDataBuilder()

    override fun build(): Task {
        val id = requireField(id) { ::id }
        val createdAt = requireField(createdAt) { ::createdAt }
        val version = requireField(version) { ::version }
        val data = requireField(data) { ::data }

        return Task(
            id = TaskId(id),
            createdAt = createdAt,
            version = version,
            data = data,
        )
    }
}
