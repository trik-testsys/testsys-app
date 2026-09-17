package tech.testsys.operation.error

import tech.testsys.domain.model.task.StatementId
import tech.testsys.domain.model.task.TaskId

sealed interface OperationError

// region CommonTypes
sealed interface EntityNotExistsError : OperationError
// endregion

// region DeveloperOperations
sealed interface CreateTaskError: OperationError
sealed interface AttachStatementError: OperationError
// endregion

// region Errors
data object AuthorizationError : CreateTaskError, AttachStatementError
data class TaskNotExistsError(val taskId: TaskId) : EntityNotExistsError, AttachStatementError
data class StatementNotExistsError(val statementId: StatementId) : EntityNotExistsError, AttachStatementError
data object TaskAlreadyHasStatementError : AttachStatementError
// endregion