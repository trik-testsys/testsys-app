package tech.testsys.operation.error

import tech.testsys.domain.model.group.CommunityId
import tech.testsys.domain.model.task.StatementId
import tech.testsys.domain.model.task.TaskId

/**
 * Expected failure of an operation, returned in [OperationResult.Error].
 *
 * @since %CURRENT_VERSION%
 */
sealed interface OperationError

// region CommonTypes
// Kinds of failure shared by many operations: they let callers handle errors by kind, whatever operation returned them.

/**
 * An entity the operation refers to does not exist.
 *
 * @since %CURRENT_VERSION%
 */
sealed interface EntityNotExistsError : OperationError

/**
 * The user has no access to an entity the operation refers to.
 *
 * @since %CURRENT_VERSION%
 */
sealed interface AccessDeniedError : OperationError

/**
 * The user does not have the role required by the operation.
 *
 * @since %CURRENT_VERSION%
 */
sealed interface MissedRequiredRoleError : OperationError
// endregion

// region DeveloperOperations

/**
 * Failure of creating a task.
 *
 * @since %CURRENT_VERSION%
 */
sealed interface CreateTaskError : OperationError

/**
 * Failure of attaching a statement to a task.
 *
 * @since %CURRENT_VERSION%
 */
sealed interface AttachStatementError : OperationError

/**
 * Failure of sharing a task to communities.
 *
 * @since %CURRENT_VERSION%
 */
sealed interface ShareTaskError : OperationError
// endregion

// region Errors

/**
 * The user does not have the developer role.
 *
 * @since %CURRENT_VERSION%
 */
data object MissedDeveloperRoleError : MissedRequiredRoleError, CreateTaskError, AttachStatementError, ShareTaskError

/**
 * The task does not exist.
 *
 * @property taskId the id of the missing task.
 * @since %CURRENT_VERSION%
 */
data class TaskNotExistsError(val taskId: TaskId) : EntityNotExistsError, AttachStatementError, ShareTaskError

/**
 * The statement does not exist.
 *
 * @property statementId the id of the missing statement.
 * @since %CURRENT_VERSION%
 */
data class StatementNotExistsError(val statementId: StatementId) : EntityNotExistsError, AttachStatementError

/**
 * The task already has a statement.
 *
 * @since %CURRENT_VERSION%
 */
data object TaskAlreadyHasStatementError : AttachStatementError

/**
 * The community does not exist.
 *
 * @property communityId the id of the missing community.
 * @since %CURRENT_VERSION%
 */
data class CommunityNotExistsError(val communityId: CommunityId) : EntityNotExistsError, ShareTaskError

/**
 * The user is not the owner of the task.
 *
 * @property taskId the id of the task.
 * @since %CURRENT_VERSION%
 */
data class TaskAccessDeniedError(val taskId: TaskId) : AccessDeniedError, AttachStatementError, ShareTaskError

/**
 * The user is not a member of the community.
 *
 * @property communityId the id of the community.
 * @since %CURRENT_VERSION%
 */
data class CommunityAccessDeniedError(val communityId: CommunityId) : AccessDeniedError, ShareTaskError

/**
 * The task has no committed version.
 *
 * @property taskId the id of the task.
 * @since %CURRENT_VERSION%
 */
data class TaskNotCommittedError(val taskId: TaskId) : ShareTaskError
// endregion
