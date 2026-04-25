package tech.testsys.domain.builder.task

import tech.testsys.domain.builder.Builder
import tech.testsys.domain.builder.DomainEntityWithDataBuilder
import tech.testsys.domain.builder.util.chooser.TaskContentChooser
import tech.testsys.domain.builder.util.lazify
import tech.testsys.domain.builder.util.requireField
import tech.testsys.domain.model.group.CommunityId
import tech.testsys.domain.model.task.CommitedTaskContent
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
 * Base builder for [WipTaskContent] and [CommitedTaskContent] revisions.
 *
 * Holds the resource collections shared by both states; identity fields
 * ([owner], [name], [description], [sharedTo]) live on [TaskDataBuilder] instead.
 *
 * @param T the concrete task content type produced by this builder.
 * @since %CURRENT_VERSION%
 */
abstract class TaskContentBuilder<T> : Builder<T> {

    /**
     * The list of test (polygon) IDs included in this revision.
     *
     * @since %CURRENT_VERSION%
     */
    var tests = mutableListOf<TestId>()

    /**
     * The ID of the exercise attached to this revision.
     *
     * @since %CURRENT_VERSION%
     */
    var exercise: ExerciseId? = null

    /**
     * The ID of the statement attached to this revision.
     *
     * @since %CURRENT_VERSION%
     */
    var statement: StatementId? = null

    /**
     * The list of developer-solution IDs attached to this revision.
     *
     * @since %CURRENT_VERSION%
     */
    var developerSolutions = mutableListOf<DeveloperSolutionId>()

    /**
     * The list of TRIK Studio versions supported by this revision.
     *
     * @since %CURRENT_VERSION%
     */
    var supportedTrikStudioVersions = mutableListOf<TrikStudioVersion>()

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
     * Sets the [statement] from a raw ID value.
     *
     * @param statement the raw statement ID.
     * @since %CURRENT_VERSION%
     */
    fun statement(statement: Long) {
        this.statement = StatementId(statement)
    }

    /**
     * Sets the [developerSolutions] list from raw ID values.
     *
     * @param developerSolutions the raw developer-solution IDs.
     * @since %CURRENT_VERSION%
     */
    fun developerSolutions(developerSolutions: Iterable<Long>) {
        this.developerSolutions = developerSolutions.map { DeveloperSolutionId(it) }.toMutableList()
    }

    /**
     * Sets the [supportedTrikStudioVersions] list from raw version strings.
     *
     * @param supportedTrikStudioVersions the raw TRIK Studio version tags.
     * @since %CURRENT_VERSION%
     */
    fun supportedTrikStudioVersions(supportedTrikStudioVersions: Iterable<String>) {
        this.supportedTrikStudioVersions = supportedTrikStudioVersions.map { TrikStudioVersion(it) }.toMutableList()
    }
}

/**
 * Builder for constructing [CommitedTaskContent] revisions.
 *
 * Both [exercise] and [statement] are required for a committed revision.
 *
 * @since %CURRENT_VERSION%
 */
class CommittedTaskContentBuilder : TaskContentBuilder<CommitedTaskContent>() {

    /**
     * Builds the [CommitedTaskContent] instance.
     *
     * @return the constructed [CommitedTaskContent].
     * @throws IllegalArgumentException if [exercise] or [statement] is not set.
     * @since %CURRENT_VERSION%
     */
    override fun build(): CommitedTaskContent {
        val exercise = requireField(exercise) { ::exercise }
        val statement = requireField(statement) { ::statement }

        return CommitedTaskContent(
            tests = tests.lazify(),
            exercise = exercise.lazify(),
            statement = statement.lazify(),
            developerSolutions = developerSolutions.lazify(),
            supportedTrikStudioVersions = supportedTrikStudioVersions,
        )
    }
}

/**
 * Builder for constructing [WipTaskContent] revisions.
 *
 * [exercise] and [statement] are optional, reflecting that a work-in-progress
 * revision may not yet have those resources attached.
 *
 * @since %CURRENT_VERSION%
 */
class WipTaskContentBuilder : TaskContentBuilder<WipTaskContent>() {

    /**
     * Builds the [WipTaskContent] instance.
     *
     * @return the constructed [WipTaskContent].
     * @since %CURRENT_VERSION%
     */
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
 * Builder for constructing [TaskData].
 *
 * Holds the task identity ([owner], [name], [description]) and the share-to-community
 * relation ([sharedTo]); the versioned payload is selected via [content].
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
     * The list of community IDs the task is shared to.
     *
     * @since %CURRENT_VERSION%
     */
    var sharedTo = mutableListOf<CommunityId>()

    /**
     * Chooser for selecting the task-content variant (new / uncommited / committed).
     *
     * @since %CURRENT_VERSION%
     */
    val content = TaskContentChooser()

    /**
     * Sets the [owner] from a raw ID value.
     *
     * @param owner the raw owner ID.
     * @since %CURRENT_VERSION%
     */
    fun owner(owner: Long) {
        this.owner = MultipleRoleUserId(owner)
    }

    /**
     * Sets the [sharedTo] list from raw ID values.
     *
     * @param sharedTo the raw community IDs.
     * @since %CURRENT_VERSION%
     */
    fun sharedTo(sharedTo: Iterable<Long>) {
        this.sharedTo = sharedTo.map { CommunityId(it) }.toMutableList()
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
