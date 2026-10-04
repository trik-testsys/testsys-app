package tech.testsys.operation.user

import tech.testsys.domain.builder.api.taskData
import tech.testsys.domain.builder.api.withData
import tech.testsys.domain.contract.persistence.repository.CommunityRepository
import tech.testsys.domain.contract.persistence.repository.DeveloperSolutionRepository
import tech.testsys.domain.contract.persistence.repository.ExerciseRepository
import tech.testsys.domain.contract.persistence.repository.StatementRepository
import tech.testsys.domain.contract.persistence.repository.TaskRepository
import tech.testsys.domain.contract.persistence.repository.TestRepository
import tech.testsys.domain.model.group.CommunityId
import tech.testsys.domain.model.task.DeveloperSolutionId
import tech.testsys.domain.model.task.ExerciseId
import tech.testsys.domain.model.task.StatementId
import tech.testsys.domain.model.task.Task
import tech.testsys.domain.model.task.TaskContent
import tech.testsys.domain.model.task.TaskId
import tech.testsys.domain.model.task.TestId
import tech.testsys.domain.model.user.Developer
import tech.testsys.domain.model.user.MultipleRoleUser
import tech.testsys.operation.annotation.Feature
import tech.testsys.operation.annotation.InternalOperationsApi
import tech.testsys.operation.error.AttachDeveloperSolutionError
import tech.testsys.operation.error.AttachExerciseError
import tech.testsys.operation.error.AttachStatementError
import tech.testsys.operation.error.AttachTestError
import tech.testsys.operation.error.CommunityAccessDeniedError
import tech.testsys.operation.error.CommunityNotExistsError
import tech.testsys.operation.error.CreateTaskError
import tech.testsys.operation.error.DeveloperSolutionNotExistsError
import tech.testsys.operation.error.DeveloperSolutionNotUploadedToTaskError
import tech.testsys.operation.error.DeveloperSolutionVersionNotLatestError
import tech.testsys.operation.error.ExerciseNotExistsError
import tech.testsys.operation.error.ExerciseNotUploadedToTaskError
import tech.testsys.operation.error.ExerciseVersionNotLatestError
import tech.testsys.operation.error.MissedDeveloperRoleError
import tech.testsys.operation.error.OperationResult
import tech.testsys.operation.error.ResourceAlreadyAttachedError
import tech.testsys.operation.error.ShareTaskError
import tech.testsys.operation.error.StatementNotExistsError
import tech.testsys.operation.error.StatementNotUploadedToTaskError
import tech.testsys.operation.error.StatementVersionNotLatestError
import tech.testsys.operation.error.TaskAccessDeniedError
import tech.testsys.operation.error.TaskAlreadyHasExerciseError
import tech.testsys.operation.error.TaskAlreadyHasStatementError
import tech.testsys.operation.error.TaskNotCommittedError
import tech.testsys.operation.error.TaskNotExistsError
import tech.testsys.operation.error.TestNotExistsError
import tech.testsys.operation.error.TestNotUploadedToTaskError
import tech.testsys.operation.error.TestVersionNotLatestError
import tech.testsys.operation.error.asSuccess
import tech.testsys.operation.error.ensure
import tech.testsys.operation.error.operation
import tech.testsys.operation.util.changeEditableContent
import tech.testsys.operation.util.getEditableContent
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
    private val exerciseRepository: ExerciseRepository,
    private val testRepository: TestRepository,
    private val developerSolutionRepository: DeveloperSolutionRepository,
) {
    /**
     * Creates a new task owned by [user] with [taskName] and [taskDescription]. The created task has
     * [TaskContent.New] content and no uploaded resources.
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
     * behalf of [user]. It must be the latest version of an uploaded chain; the editable revision allows one statement.
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

            ensure(statement.data.versionBucket in task.data.uploadedResources) {
                StatementNotUploadedToTaskError(taskId = taskId, statementId = newStatementId)
            }

            ensure(statementRepository.findLatestByVersionBucket(statement.data.versionBucket)?.id == newStatementId) {
                StatementVersionNotLatestError(newStatementId)
            }

            val wipContent = task.getEditableContent()
            wipContent.statement?.let { attached ->
                ensure(statementRepository.load(attached).data.versionBucket != statement.data.versionBucket) {
                    ResourceAlreadyAttachedError(taskId, statement.data.versionBucket)
                }
            }
            ensure(wipContent.statement == null, TaskAlreadyHasStatementError)

            val updatedTask = task.changeEditableContent { this.statement = newStatementId }

            val savedTask = taskRepository.update(updatedTask)
            return savedTask.asSuccess()
        }

    /**
     * Attaches [newExerciseId] to the editable revision of [taskId] on behalf of [user]. It must be the latest version
     * of an uploaded chain; rejects an attached chain or an occupied exercise slot.
     *
     * @since %CURRENT_VERSION%
     */
    @Feature("testsys.user.multi.developer.task.attachResource")
    fun attachExercise(user: MultipleRoleUser, taskId: TaskId, newExerciseId: ExerciseId): OperationResult<Task, AttachExerciseError> =
        operation<Task, AttachExerciseError> {
            ensure(user.hasRole<Developer>(), MissedDeveloperRoleError)
            val task = taskRepository.findById(taskId)
            ensure(task != null) { TaskNotExistsError(taskId) }
            val resource = exerciseRepository.findById(newExerciseId)
            ensure(resource != null) { ExerciseNotExistsError(newExerciseId) }
            ensure(task.data.owner.id == user.id) { TaskAccessDeniedError(taskId) }
            ensure(resource.data.versionBucket in task.data.uploadedResources) {
                ExerciseNotUploadedToTaskError(taskId, newExerciseId)
            }
            ensure(exerciseRepository.findLatestByVersionBucket(resource.data.versionBucket)?.id == newExerciseId) {
                ExerciseVersionNotLatestError(newExerciseId)
            }
            val wipContent = task.getEditableContent()
            wipContent.exercise?.let { attached ->
                ensure(exerciseRepository.load(attached).data.versionBucket != resource.data.versionBucket) {
                    ResourceAlreadyAttachedError(taskId, resource.data.versionBucket)
                }
            }
            ensure(wipContent.exercise == null, TaskAlreadyHasExerciseError)
            val updatedTask = task.changeEditableContent { exercise = newExerciseId }
            return taskRepository.update(updatedTask).asSuccess()
        }

    /**
     * Attaches [newTestId] to the editable revision of [taskId] on behalf of [user].
     * Requires the latest version of an uploaded chain and rejects an already attached chain.
     *
     * @since %CURRENT_VERSION%
     */
    @Feature("testsys.user.multi.developer.task.attachResource")
    fun attachTest(user: MultipleRoleUser, taskId: TaskId, newTestId: TestId): OperationResult<Task, AttachTestError> =
        operation<Task, AttachTestError> {
            ensure(user.hasRole<Developer>(), MissedDeveloperRoleError)
            val task = taskRepository.findById(taskId)
            ensure(task != null) { TaskNotExistsError(taskId) }
            val resource = testRepository.findById(newTestId)
            ensure(resource != null) { TestNotExistsError(newTestId) }
            ensure(task.data.owner.id == user.id) { TaskAccessDeniedError(taskId) }
            ensure(resource.data.versionBucket in task.data.uploadedResources) {
                TestNotUploadedToTaskError(taskId, newTestId)
            }
            ensure(testRepository.findLatestByVersionBucket(resource.data.versionBucket)?.id == newTestId) {
                TestVersionNotLatestError(newTestId)
            }
            val wipContent = task.getEditableContent()
            val attached = testRepository.load(wipContent.tests)
            ensure(attached.none { it.data.versionBucket == resource.data.versionBucket }) {
                ResourceAlreadyAttachedError(taskId, resource.data.versionBucket)
            }
            val updatedTask = task.changeEditableContent { tests.add(newTestId) }
            return taskRepository.update(updatedTask).asSuccess()
        }

    /**
     * Attaches [newDeveloperSolutionId] to the editable revision of [taskId] on behalf of [user].
     * Requires the latest version of an uploaded chain and rejects an already attached chain.
     *
     * @since %CURRENT_VERSION%
     */
    @Feature("testsys.user.multi.developer.task.attachResource")
    fun attachDeveloperSolution(
        user: MultipleRoleUser,
        taskId: TaskId,
        newDeveloperSolutionId: DeveloperSolutionId,
    ): OperationResult<Task, AttachDeveloperSolutionError> = operation<Task, AttachDeveloperSolutionError> {
        ensure(user.hasRole<Developer>(), MissedDeveloperRoleError)
        val task = taskRepository.findById(taskId)
        ensure(task != null) { TaskNotExistsError(taskId) }
        val resource = developerSolutionRepository.findById(newDeveloperSolutionId)
        ensure(resource != null) { DeveloperSolutionNotExistsError(newDeveloperSolutionId) }
        ensure(task.data.owner.id == user.id) { TaskAccessDeniedError(taskId) }
        ensure(resource.data.versionBucket in task.data.uploadedResources) {
            DeveloperSolutionNotUploadedToTaskError(taskId, newDeveloperSolutionId)
        }
        ensure(developerSolutionRepository.findLatestByVersionBucket(resource.data.versionBucket)?.id == newDeveloperSolutionId) {
            DeveloperSolutionVersionNotLatestError(newDeveloperSolutionId)
        }
        val wipContent = task.getEditableContent()
        val attached = developerSolutionRepository.load(wipContent.developerSolutions)
        ensure(attached.none { it.data.versionBucket == resource.data.versionBucket }) {
            ResourceAlreadyAttachedError(taskId, resource.data.versionBucket)
        }
        val updatedTask = task.changeEditableContent { developerSolutions.add(newDeveloperSolutionId) }
        return taskRepository.update(updatedTask).asSuccess()
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

            val sharedCommunityIds = task.data.sharedTo.ids
            val developerCommunityIds = user.data.roles.filterIsInstance<Developer>().single().memberOf.ids
            communityIds.filterNot { communityId -> communityId in sharedCommunityIds }.forEach { communityId ->
                ensure(communityId in developerCommunityIds) { CommunityAccessDeniedError(communityId) }
            }

            ensure(task.data.content !is TaskContent.New) { TaskNotCommittedError(taskId) }

            val sharedTask = task.withData {
                sharedTo = (sharedCommunityIds + communityIds).distinct().toMutableList()
            }

            val savedTask = taskRepository.update(sharedTask)
            return savedTask.asSuccess()
        }
}
