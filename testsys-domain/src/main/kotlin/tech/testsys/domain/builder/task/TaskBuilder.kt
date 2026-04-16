package tech.testsys.domain.builder.task

import tech.testsys.domain.builder.Builder
import tech.testsys.domain.builder.DomainEntityWithDataBuilder
import tech.testsys.domain.builder.util.chooser.TaskDataChooser
import tech.testsys.domain.builder.util.lazify
import tech.testsys.domain.builder.util.requireField
import tech.testsys.domain.model.group.CommunityId
import tech.testsys.domain.model.task.DeveloperSolutionId
import tech.testsys.domain.model.task.ExerciseId
import tech.testsys.domain.model.task.StatementId
import tech.testsys.domain.model.task.Task
import tech.testsys.domain.model.task.CommitedTaskContent
import tech.testsys.domain.model.task.WipTaskContent
import tech.testsys.domain.model.task.TaskData
import tech.testsys.domain.model.task.TaskId
import tech.testsys.domain.model.task.TestId
import tech.testsys.domain.model.task.TrikStudioVersion
import tech.testsys.domain.model.user.MultipleRoleUserId

/**
 * Base builder for constructing [WipTaskContent] and [CommitedTaskContent].
 *
 * @since %CURRENT_VERSION%
 */
abstract class TaskContentBuilder<T> : Builder<T> {

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
}

/**
 * Base builder for constructing [CommitedTaskContent].
 *
 * @since %CURRENT_VERSION%
 */
class CommittedTaskContentBuilder : TaskContentBuilder<CommitedTaskContent>() {

    /**
     * Builds the [CommitedTaskContent] instance.
     *
     * @return the constructed [CommitedTaskContent].
     * @throws IllegalArgumentException if any required field is not set.
     * @since %CURRENT_VERSION%
     */
    override fun build(): CommitedTaskContent {
        val owner = requireField(owner) { ::owner }
        val name = requireField(name) { ::name }
        val description = requireField(description) { ::description }
        val exercise = requireField(exercise) { ::exercise }
        val supportedTrikStudioVersions = requireField(supportedTrikStudioVersions) { ::supportedTrikStudioVersions }
        val statement = requireField(statement) { ::statement }

        return CommitedTaskContent(
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
 * Base builder for constructing [WipTaskContent].
 *
 * @since %CURRENT_VERSION%
 */
class WipTaskContentBuilder : TaskContentBuilder<WipTaskContent>() {

    /**
     * Builds the [WipTaskContent] instance.
     *
     * @return the constructed [WipTaskContent].
     * @throws IllegalArgumentException if any required field is not set.
     * @since %CURRENT_VERSION%
     */
    override fun build(): WipTaskContent {
        val owner = requireField(owner) { ::owner }
        val name = requireField(name) { ::name }

        return WipTaskContent(
            owner = owner.lazify(),
            name = name,
            description = description,
            tests = tests.lazify(),
            exercise = exercise?.lazify(),
            developerSolutions = developerSolutions.lazify(),
            statement = statement?.lazify(),
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
class TaskBuilder : DomainEntityWithDataBuilder<Task, TaskData, TaskDataChooser>() {

    override fun dataBuilder(): TaskDataChooser = TaskDataChooser()

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
