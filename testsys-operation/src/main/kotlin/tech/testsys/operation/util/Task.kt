package tech.testsys.operation.util

import tech.testsys.domain.builder.api.withData
import tech.testsys.domain.builder.task.CommittedTaskContentBuilder
import tech.testsys.domain.builder.task.WipTaskContentBuilder
import tech.testsys.domain.model.task.CommittedTaskContent
import tech.testsys.domain.model.task.Task
import tech.testsys.domain.model.task.TaskContent
import tech.testsys.domain.model.task.WipTaskContent
import tech.testsys.operation.annotation.InternalOperationsApi
import tech.testsys.operation.error.OperationError
import tech.testsys.operation.error.OperationResult
import tech.testsys.operation.error.asSuccess
import tech.testsys.operation.error.operation
import tech.testsys.operation.error.raise

/**
 * Returns the work-in-progress content of this task, or fails with [onCommited] if the task has
 * [TaskContent.Committed] content.
 *
 * @param E the type of the error returned for a committed task.
 * @since %CURRENT_VERSION%
 */
@InternalOperationsApi
fun <E : OperationError> Task.getWip(onCommited: E): OperationResult<WipTaskContent, E> = operation {
    return when (val content = this@getWip.data.content) {
        is TaskContent.Committed -> onCommited.raise()
        is TaskContent.New -> content.wip.asSuccess()
        is TaskContent.Uncommitted -> content.wip.asSuccess()
    }
}

/**
 * Returns a copy of this task with [change] applied to its work-in-progress content, or fails with [onCommited]
 * if the task has [TaskContent.Committed] content.
 *
 * @param E the type of the error returned for a committed task.
 * @since %CURRENT_VERSION%
 */
@InternalOperationsApi
fun <E : OperationError> Task.changeWip(onCommited: E, change: WipTaskContentBuilder.() -> Unit): OperationResult<Task, E> = operation {
    val task = this@changeWip
    return when (task.data.content) {
        is TaskContent.Committed -> onCommited.raise()
        is TaskContent.New -> task.withData {
            content.new { change() }
        }.asSuccess()
        is TaskContent.Uncommitted -> task.withData {
            content.uncommitted(
                wipBuilder = { change() },
                lastCommittedBuilder = { },
            )
        }.asSuccess()
    }
}

/**
 * Returns the editable content, copying the last committed revision when this task has no WIP revision yet.
 *
 * @since %CURRENT_VERSION%
 */
@InternalOperationsApi
fun Task.getEditableContent(): WipTaskContent = when (val content = data.content) {
    is TaskContent.New -> content.wip
    is TaskContent.Uncommitted -> content.wip
    is TaskContent.Committed -> WipTaskContentBuilder().apply { populateFrom(content.lastCommitted) }.build()
}

/**
 * Applies [change] to the WIP revision, creating it from the last committed revision when necessary.
 * Preserves task metadata, uploaded chains, the optimistic locking token and the last committed content.
 *
 * @since %CURRENT_VERSION%
 */
@InternalOperationsApi
fun Task.changeEditableContent(change: WipTaskContentBuilder.() -> Unit): Task = withData {
    when (val previous = this@changeEditableContent.data.content) {
        is TaskContent.New -> content.new { change() }
        is TaskContent.Uncommitted -> content.uncommitted(wipBuilder = { change() }, lastCommittedBuilder = {})
        is TaskContent.Committed -> content.uncommitted(
            wipBuilder = {
                populateFrom(previous.lastCommitted)
                change()
            },
            lastCommittedBuilder = { populateFrom(previous.lastCommitted) },
        )
    }
}

private fun WipTaskContentBuilder.populateFrom(content: CommittedTaskContent) {
    tests = content.tests.ids.toMutableList()
    exercises = content.exercises.ids.toMutableList()
    statement = content.statement.id
    developerSolutions = content.developerSolutions.ids.toMutableList()
    supportedTrikStudioVersions = content.supportedTrikStudioVersions.toMutableList()
}

private fun CommittedTaskContentBuilder.populateFrom(content: CommittedTaskContent) {
    tests = content.tests.ids.toMutableList()
    exercises = content.exercises.ids.toMutableList()
    statement = content.statement.id
    developerSolutions = content.developerSolutions.ids.toMutableList()
    supportedTrikStudioVersions = content.supportedTrikStudioVersions.toMutableList()
}
