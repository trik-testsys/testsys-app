package tech.testsys.operation.user

import tech.testsys.domain.builder.api.taskData
import tech.testsys.domain.contract.persistence.repository.StatementRepository
import tech.testsys.domain.contract.persistence.repository.TaskRepository
import tech.testsys.domain.model.task.StatementId
import tech.testsys.domain.model.task.Task
import tech.testsys.domain.model.task.TaskId
import tech.testsys.domain.model.user.Developer
import tech.testsys.domain.model.user.MultipleRoleUser
import tech.testsys.operation.annotation.Feature
import tech.testsys.operation.error.AttachStatementError
import tech.testsys.operation.error.AuthorizationError
import tech.testsys.operation.error.CreateTaskError
import tech.testsys.operation.error.OperationResult
import tech.testsys.operation.error.StatementNotExistsError
import tech.testsys.operation.error.TaskAlreadyHasStatementError
import tech.testsys.operation.error.TaskNotExistsError
import tech.testsys.operation.error.asSuccess
import tech.testsys.operation.error.ensure
import tech.testsys.operation.error.getOrThrow
import tech.testsys.operation.error.operation
import tech.testsys.operation.util.changeWip
import tech.testsys.operation.util.getWip
import tech.testsys.operation.util.hasRole

class DeveloperOperations(
    private val taskRepository: TaskRepository,
    private val statementRepository: StatementRepository
) {
    @Feature("testsys.user.multi.developer.task.createTask")
    fun createTask(
        user: MultipleRoleUser,
        taskName: String,
        taskDescription: String,
    ): OperationResult<Task, CreateTaskError> = operation<Task, CreateTaskError> {
        ensure(user.hasRole<Developer>(), AuthorizationError)

        val taskData = taskData {
            owner = user.id
            name = taskName
            description = taskDescription
            content.new {}
        }

        val task = taskRepository.save(taskData)
        return task.asSuccess()
    }

    @Feature("testsys.user.multi.developer.task.attachResource")
    fun attachStatement(
        user: MultipleRoleUser,
        taskId: TaskId,
        newStatementId: StatementId,
    ): OperationResult<Task, AttachStatementError> = operation<Task, AttachStatementError> {
        ensure(user.hasRole<Developer>(), AuthorizationError)

        val task = taskRepository.findById(taskId)
        ensure(task != null) { TaskNotExistsError(taskId) }

        val statement = statementRepository.findById(newStatementId)
        ensure(statement != null) { StatementNotExistsError(newStatementId) }

        val wipContent = task.getWip(TaskAlreadyHasStatementError).getOrThrow()
        ensure(wipContent.statement == null, TaskAlreadyHasStatementError)

        val updatedTask = task.changeWip(TaskAlreadyHasStatementError) {
            this.statement = newStatementId
        }.getOrThrow()

        taskRepository.update(updatedTask)
        return updatedTask.asSuccess()
    }

}