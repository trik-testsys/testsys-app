package tech.testsys.operation.user

import tech.testsys.domain.builder.api.taskData
import tech.testsys.domain.builder.api.withData
import tech.testsys.domain.contract.persistence.repository.CommunityRepository
import tech.testsys.domain.contract.persistence.repository.StatementRepository
import tech.testsys.domain.contract.persistence.repository.TaskRepository
import tech.testsys.domain.model.group.CommunityId
import tech.testsys.domain.model.task.StatementId
import tech.testsys.domain.model.task.Task
import tech.testsys.domain.model.task.TaskContent
import tech.testsys.domain.model.task.TaskId
import tech.testsys.domain.model.user.Developer
import tech.testsys.domain.model.user.MultipleRoleUser
import tech.testsys.operation.annotation.Feature
import tech.testsys.operation.annotation.InternalOperationsApi
import tech.testsys.operation.error.AttachStatementError
import tech.testsys.operation.error.CommunityAccessDeniedError
import tech.testsys.operation.error.CommunityNotExistsError
import tech.testsys.operation.error.CreateTaskError
import tech.testsys.operation.error.MissedDeveloperRoleError
import tech.testsys.operation.error.OperationResult
import tech.testsys.operation.error.ShareTaskError
import tech.testsys.operation.error.StatementNotExistsError
import tech.testsys.operation.error.TaskAccessDeniedError
import tech.testsys.operation.error.TaskAlreadyHasStatementError
import tech.testsys.operation.error.TaskNotCommittedError
import tech.testsys.operation.error.TaskNotExistsError
import tech.testsys.operation.error.asSuccess
import tech.testsys.operation.error.ensure
import tech.testsys.operation.error.getOrRaise
import tech.testsys.operation.error.operation
import tech.testsys.operation.util.changeWip
import tech.testsys.operation.util.getWip
import tech.testsys.operation.util.hasRole

/**
 * Operations of a user with the [Developer] role.
 *
 * @since %CURRENT_VERSION%
 */
@OptIn(InternalOperationsApi::class)
class DeveloperOperations(
    private val taskRepository: TaskRepository,
    private val statementRepository: StatementRepository,
    private val communityRepository: CommunityRepository,
) {
    /**
     * Creates a new task owned by [user] with [taskName] and [taskDescription]. The created task has
     * [TaskContent.New] content.
     *
     * @since %CURRENT_VERSION%
     */
    @Feature("testsys.user.multi.developer.task.createTask")
    fun createTask(user: MultipleRoleUser, taskName: String, taskDescription: String): OperationResult<Task, CreateTaskError> =
        operation<Task, CreateTaskError> {
            ensure(user.hasRole<Developer>(), MissedDeveloperRoleError)

            val taskData = taskData {
                owner = user.id
                name = taskName
                description = taskDescription
                content.new {}
            }

            val task = taskRepository.save(taskData)
            return task.asSuccess()
        }

    /**
     * Attaches the statement with [newStatementId] to the work-in-progress version of the task with [taskId] on
     * behalf of [user]. A task can have at most one statement.
     *
     * @since %CURRENT_VERSION%
     */
    @Feature("testsys.user.multi.developer.task.attachResource")
    fun attachStatement(user: MultipleRoleUser, taskId: TaskId, newStatementId: StatementId): OperationResult<Task, AttachStatementError> =
        operation<Task, AttachStatementError> {
            ensure(user.hasRole<Developer>(), MissedDeveloperRoleError)

            val task = taskRepository.findById(taskId)
            ensure(task != null) { TaskNotExistsError(taskId) }

            val statement = statementRepository.findById(newStatementId)
            ensure(statement != null) { StatementNotExistsError(newStatementId) }

            ensure(task.data.owner.id == user.id) { TaskAccessDeniedError(taskId) }

            val wipContent = task.getWip(TaskAlreadyHasStatementError).getOrRaise()
            ensure(wipContent.statement == null, TaskAlreadyHasStatementError)

            val updatedTask = task.changeWip(TaskAlreadyHasStatementError) {
                this.statement = newStatementId
            }.getOrRaise()

            taskRepository.update(updatedTask)
            return updatedTask.asSuccess()
        }

    /**
     * Shares the task with [taskId] owned by [user] to the communities with [communityIds], adding them to the
     * communities the task is already shared to.
     *
     * @since %CURRENT_VERSION%
     */
    @Feature("testsys.user.multi.developer.task.shareTask")
    fun shareTask(user: MultipleRoleUser, taskId: TaskId, communityIds: Set<CommunityId>): OperationResult<Task, ShareTaskError> =
        operation<Task, ShareTaskError> {
            ensure(user.hasRole<Developer>(), MissedDeveloperRoleError)

            val task = taskRepository.findById(taskId)
            ensure(task != null) { TaskNotExistsError(taskId) }

            communityIds.forEach { communityId ->
                ensure(communityRepository.findById(communityId) != null) { CommunityNotExistsError(communityId) }
            }

            ensure(task.data.owner.id == user.id) { TaskAccessDeniedError(taskId) }

            ensure(task.data.content !is TaskContent.New) { TaskNotCommittedError(taskId) }

            val sharedCommunityIds = task.data.sharedTo.ids
            val developerCommunityIds = user.data.roles.filterIsInstance<Developer>().single().memberOf.ids
            communityIds.filterNot { communityId -> communityId in sharedCommunityIds }.forEach { communityId ->
                ensure(communityId in developerCommunityIds) { CommunityAccessDeniedError(communityId) }
            }

            val sharedTask = task.withData {
                sharedTo = (sharedCommunityIds + communityIds).distinct().toMutableList()
            }

            taskRepository.update(sharedTask)
            return sharedTask.asSuccess()
        }
}
