package tech.testsys.operation.error

import tech.testsys.domain.model.group.CommunityId
import tech.testsys.domain.model.task.DeveloperSolutionId
import tech.testsys.domain.model.task.ExerciseId
import tech.testsys.domain.model.task.StatementId
import tech.testsys.domain.model.task.TaskId
import tech.testsys.domain.model.task.TestId
import tech.testsys.domain.model.task.VersionBucket

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

/**
 * The user has no access to resource in current context
 *
 * @since %CURRENT_VERSION%
 */
sealed interface ResourceAccessError : OperationError

// endregion

// region DeveloperOperations

/**
 * Failure of uploading a new statement to a task.
 *
 * @since %CURRENT_VERSION%
 */
sealed interface AddStatementError : OperationError

/**
 * Failure of uploading a new exercise to a task.
 *
 * @since %CURRENT_VERSION%
 */
sealed interface AddExerciseError : OperationError

/**
 * Failure of uploading a new polygon to a task.
 *
 * @since %CURRENT_VERSION%
 */
sealed interface AddTestError : OperationError

/**
 * Failure of uploading a new developer solution to a task.
 *
 * @since %CURRENT_VERSION%
 */
sealed interface AddDeveloperSolutionError : OperationError

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
 * Failure of attaching a exercise to a task.
 *
 * @since %CURRENT_VERSION%
 */
sealed interface AttachExerciseError : OperationError

/**
 * Failure of attaching a test to a task.
 *
 * @since %CURRENT_VERSION%
 */
sealed interface AttachTestError : OperationError

/**
 * Failure of attaching a developersolution to a task.
 *
 * @since %CURRENT_VERSION%
 */
sealed interface AttachDeveloperSolutionError : OperationError

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
data object MissedDeveloperRoleError :
    AddStatementError,
    AddExerciseError,
    AddTestError,
    AddDeveloperSolutionError,
    MissedRequiredRoleError,
    CreateTaskError,
    AttachStatementError,
    ShareTaskError,
    AttachExerciseError,
    AttachTestError,
    AttachDeveloperSolutionError

/**
 * The task does not exist.
 *
 * @property taskId the id of the missing task.
 * @since %CURRENT_VERSION%
 */
data class TaskNotExistsError(val taskId: TaskId) :
    AddStatementError,
    AddExerciseError,
    AddTestError,
    AddDeveloperSolutionError,
    EntityNotExistsError,
    AttachStatementError,
    ShareTaskError,
    AttachExerciseError,
    AttachTestError,
    AttachDeveloperSolutionError

/**
 * The statement does not exist.
 *
 * @property statementId the id of the missing statement.
 * @since %CURRENT_VERSION%
 */
data class StatementNotExistsError(val statementId: StatementId) : EntityNotExistsError, AttachStatementError

/**
 * The statement's version chain is not uploaded to the task.
 *
 * @property taskId the id of the task.
 * @property statementId the id of the statement that cannot be attached.
 * @since %CURRENT_VERSION%
 */
data class StatementNotUploadedToTaskError(val taskId: TaskId, val statementId: StatementId) :
    AttachStatementError,
    ResourceAccessError

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
data class TaskAccessDeniedError(val taskId: TaskId) :
    AddStatementError,
    AddExerciseError,
    AddTestError,
    AddDeveloperSolutionError,
    AccessDeniedError,
    AttachStatementError,
    ShareTaskError,
    AttachExerciseError,
    AttachTestError,
    AttachDeveloperSolutionError

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

/**
 * The requested statement version is no longer the latest version in its chain.
 *
 * @property statementId the id of the requested version.
 * @since %CURRENT_VERSION%
 */
data class StatementVersionNotLatestError(val statementId: StatementId) : AttachStatementError

/**
 * The exercise does not exist.
 *
 * @property exerciseId the id of the missing resource.
 * @since %CURRENT_VERSION%
 */
data class ExerciseNotExistsError(val exerciseId: ExerciseId) : EntityNotExistsError, AttachExerciseError

/**
 * The exercise's version chain is not uploaded to the task.
 *
 * @property taskId the id of the task.
 * @property exerciseId the id of the resource that cannot be attached.
 * @since %CURRENT_VERSION%
 */
data class ExerciseNotUploadedToTaskError(val taskId: TaskId, val exerciseId: ExerciseId) : AttachExerciseError, ResourceAccessError

/**
 * The requested exercise version is no longer the latest version in its chain.
 *
 * @property exerciseId the id of the requested version.
 * @since %CURRENT_VERSION%
 */
data class ExerciseVersionNotLatestError(val exerciseId: ExerciseId) : AttachExerciseError

/**
 * The test does not exist.
 *
 * @property testId the id of the missing resource.
 * @since %CURRENT_VERSION%
 */
data class TestNotExistsError(val testId: TestId) : EntityNotExistsError, AttachTestError

/**
 * The test's version chain is not uploaded to the task.
 *
 * @property taskId the id of the task.
 * @property testId the id of the resource that cannot be attached.
 * @since %CURRENT_VERSION%
 */
data class TestNotUploadedToTaskError(val taskId: TaskId, val testId: TestId) : AttachTestError, ResourceAccessError

/**
 * The requested test version is no longer the latest version in its chain.
 *
 * @property testId the id of the requested version.
 * @since %CURRENT_VERSION%
 */
data class TestVersionNotLatestError(val testId: TestId) : AttachTestError

/**
 * The developer solution does not exist.
 *
 * @property developerSolutionId the id of the missing resource.
 * @since %CURRENT_VERSION%
 */
data class DeveloperSolutionNotExistsError(
    val developerSolutionId: DeveloperSolutionId,
) : EntityNotExistsError, AttachDeveloperSolutionError

/**
 * The developer solution's version chain is not uploaded to the task.
 *
 * @property taskId the id of the task.
 * @property developerSolutionId the id of the resource that cannot be attached.
 * @since %CURRENT_VERSION%
 */
data class DeveloperSolutionNotUploadedToTaskError(
    val taskId: TaskId,
    val developerSolutionId: DeveloperSolutionId,
) : AttachDeveloperSolutionError, ResourceAccessError

/**
 * The requested developer solution version is no longer the latest version in its chain.
 *
 * @property developerSolutionId the id of the requested version.
 * @since %CURRENT_VERSION%
 */
data class DeveloperSolutionVersionNotLatestError(val developerSolutionId: DeveloperSolutionId) : AttachDeveloperSolutionError

/**
 * A version from the resource chain is already attached to the editable task revision.
 *
 * @property taskId the id of the task.
 * @property versionBucket the already attached resource chain.
 * @since %CURRENT_VERSION%
 */
data class ResourceAlreadyAttachedError(
    val taskId: TaskId,
    val versionBucket: VersionBucket,
) :
    AttachStatementError,
    AttachExerciseError,
    AttachTestError,
    AttachDeveloperSolutionError

/**
 * The editable task revision already contains an exercise.
 *
 * @since %CURRENT_VERSION%
 */
data object TaskAlreadyHasExerciseError : AttachExerciseError
