package tech.testsys.operation.util

import tech.testsys.domain.builder.api.withData
import tech.testsys.domain.builder.task.WipTaskContentBuilder
import tech.testsys.domain.model.task.Task
import tech.testsys.domain.model.task.TaskContent
import tech.testsys.domain.model.task.WipTaskContent
import tech.testsys.operation.annotation.InternalOperationsApi
import tech.testsys.operation.error.OperationError
import tech.testsys.operation.error.OperationResult
import tech.testsys.operation.error.asSuccess
import tech.testsys.operation.error.operation
import tech.testsys.operation.error.raise

@InternalOperationsApi
fun <E : OperationError> Task.getWip(onCommited: E): OperationResult<WipTaskContent, E> = operation {
    return when (val content = this@getWip.data.content) {
        is TaskContent.Committed -> onCommited.raise()
        is TaskContent.New -> content.wip.asSuccess()
        is TaskContent.Uncommitted -> content.wip.asSuccess()
    }
}

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
