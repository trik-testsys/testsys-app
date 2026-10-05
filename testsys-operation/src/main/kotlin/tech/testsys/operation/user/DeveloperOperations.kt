package tech.testsys.operation.user

import tech.testsys.domain.builder.api.developerSolutionData
import tech.testsys.domain.builder.api.exerciseData
import tech.testsys.domain.builder.api.solutionData
import tech.testsys.domain.builder.api.statementData
import tech.testsys.domain.builder.api.taskData
import tech.testsys.domain.builder.api.testData
import tech.testsys.domain.builder.api.withData
import tech.testsys.domain.contract.StoredBlobRef
import tech.testsys.domain.contract.persistence.repository.CommunityRepository
import tech.testsys.domain.contract.persistence.repository.DeveloperSolutionRepository
import tech.testsys.domain.contract.persistence.repository.ExerciseRepository
import tech.testsys.domain.contract.persistence.repository.SolutionRepository
import tech.testsys.domain.contract.persistence.repository.StatementRepository
import tech.testsys.domain.contract.persistence.repository.TaskRepository
import tech.testsys.domain.contract.persistence.repository.TestRepository
import tech.testsys.domain.model.DomainEntity
import tech.testsys.domain.model.DomainId
import tech.testsys.domain.model.group.CommunityId
import tech.testsys.domain.model.task.DeveloperSolution
import tech.testsys.domain.model.task.DeveloperSolutionId
import tech.testsys.domain.model.task.Exercise
import tech.testsys.domain.model.task.ExerciseId
import tech.testsys.domain.model.task.FileData
import tech.testsys.domain.model.task.Score
import tech.testsys.domain.model.task.Statement
import tech.testsys.domain.model.task.StatementId
import tech.testsys.domain.model.task.Task
import tech.testsys.domain.model.task.TaskContent
import tech.testsys.domain.model.task.TaskId
import tech.testsys.domain.model.task.Test
import tech.testsys.domain.model.task.TestId
import tech.testsys.domain.model.task.TrikSupportedLanguage
import tech.testsys.domain.model.task.VersionBucket
import tech.testsys.domain.model.user.Developer
import tech.testsys.domain.model.user.MultipleRoleUser
import tech.testsys.operation.annotation.Feature
import tech.testsys.operation.annotation.InternalOperationsApi
import tech.testsys.operation.error.AddDeveloperSolutionError
import tech.testsys.operation.error.AddExerciseError
import tech.testsys.operation.error.AddStatementError
import tech.testsys.operation.error.AddTestError
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
import tech.testsys.operation.error.DownloadResourceVersionError
import tech.testsys.operation.error.ExerciseNotExistsError
import tech.testsys.operation.error.ExerciseNotUploadedToTaskError
import tech.testsys.operation.error.ExerciseVersionNotLatestError
import tech.testsys.operation.error.MissedDeveloperRoleError
import tech.testsys.operation.error.OperationResult
import tech.testsys.operation.error.ResourceAlreadyAttachedError
import tech.testsys.operation.error.ResourceNotExistsError
import tech.testsys.operation.error.ResourceNotUploadedToTaskError
import tech.testsys.operation.error.ResourceVersionNotExistsError
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
import tech.testsys.operation.error.UpdateDeveloperSolutionError
import tech.testsys.operation.error.UpdateExerciseError
import tech.testsys.operation.error.UpdateStatementError
import tech.testsys.operation.error.UpdateTestError
import tech.testsys.operation.error.ViewResourceError
import tech.testsys.operation.error.ViewResourcesError
import tech.testsys.operation.error.asSuccess
import tech.testsys.operation.error.ensure
import tech.testsys.operation.error.operation
import tech.testsys.operation.util.changeEditableContent
import tech.testsys.operation.util.getEditableContent
import tech.testsys.operation.util.hasRole
import java.util.UUID

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
    private val solutionRepository: SolutionRepository,
) {

    /**
     * Returns the latest existing resource versions uploaded to tasks owned by [user], including unattached chains.
     * Each entity's creation time is the last-change time of its resource; viewing does not change task state.
     *
     * @since %CURRENT_VERSION%
     */
    @Feature("testsys.user.multi.developer.resource.viewResources")
    fun viewResources(user: MultipleRoleUser): OperationResult<List<DomainEntity<*>>, ViewResourcesError> =
        operation<List<DomainEntity<*>>, ViewResourcesError> {
            ensure(user.hasRole<Developer>(), MissedDeveloperRoleError)
            val developer = user.data.roles.filterIsInstance<Developer>().single()
            val buckets = taskRepository.findByIds(developer.data.tasks.ids)
                .filter { it.data.owner.id == user.id }
                .flatMap { it.data.uploadedResources }.toSet()
            val resources: List<DomainEntity<*>> = buckets.flatMap { bucket ->
                listOfNotNull(
                    statementRepository.findLatestByVersionBucket(bucket),
                    exerciseRepository.findLatestByVersionBucket(bucket),
                    testRepository.findLatestByVersionBucket(bucket),
                    developerSolutionRepository.findLatestByVersionBucket(bucket),
                )
            }
            return resources.asSuccess()
        }

    /**
     * Returns existing versions of [versionBucket] uploaded to [taskId] owned by [user].
     * Entities retain their current metadata and files; viewing does not change task state.
     *
     * @since %CURRENT_VERSION%
     */
    @Feature("testsys.user.multi.developer.resource.viewResource")
    fun viewResource(
        user: MultipleRoleUser,
        taskId: TaskId,
        versionBucket: VersionBucket,
    ): OperationResult<List<DomainEntity<*>>, ViewResourceError> = operation<List<DomainEntity<*>>, ViewResourceError> {
        ensure(user.hasRole<Developer>(), MissedDeveloperRoleError)
        val task = taskRepository.findById(taskId)
        ensure(task != null) { TaskNotExistsError(taskId) }
        ensure(resourceExists(versionBucket)) { ResourceNotExistsError(versionBucket) }
        ensure(task.data.owner.id == user.id) { TaskAccessDeniedError(taskId) }
        ensure(versionBucket in task.data.uploadedResources) { ResourceNotUploadedToTaskError(taskId, versionBucket) }
        val versions = statementRepository.findVersionsByVersionBucket(versionBucket) +
            exerciseRepository.findVersionsByVersionBucket(versionBucket) +
            testRepository.findVersionsByVersionBucket(versionBucket) +
            developerSolutionRepository.findVersionsByVersionBucket(versionBucket)
        ensure(versions.isNotEmpty()) { ResourceNotExistsError(versionBucket) }
        return versions.asSuccess()
    }

    /**
     * Returns the existing file reference of [versionId] in [versionBucket] uploaded to [taskId] owned by [user].
     * The operation accepts older versions and does not load file contents or change the task.
     *
     * @since %CURRENT_VERSION%
     */
    @Feature("testsys.user.multi.developer.resource.viewResource")
    fun downloadResourceVersion(
        user: MultipleRoleUser,
        taskId: TaskId,
        versionBucket: VersionBucket,
        versionId: DomainId,
    ): OperationResult<StoredBlobRef, DownloadResourceVersionError> = operation<StoredBlobRef, DownloadResourceVersionError> {
        ensure(user.hasRole<Developer>(), MissedDeveloperRoleError)
        val task = taskRepository.findById(taskId)
        ensure(task != null) { TaskNotExistsError(taskId) }
        ensure(resourceExists(versionBucket)) { ResourceNotExistsError(versionBucket) }
        ensure(task.data.owner.id == user.id) { TaskAccessDeniedError(taskId) }
        ensure(versionBucket in task.data.uploadedResources) { ResourceNotUploadedToTaskError(taskId, versionBucket) }
        val fileRef = when (versionId) {
            is StatementId -> statementRepository.findFileRef(versionBucket, versionId)
            is ExerciseId -> exerciseRepository.findFileRef(versionBucket, versionId)
            is TestId -> testRepository.findFileRef(versionBucket, versionId)
            is DeveloperSolutionId -> developerSolutionRepository.findFileRef(versionBucket, versionId)
            else -> null
        }
        ensure(fileRef != null) { ResourceVersionNotExistsError(versionBucket, versionId) }
        return fileRef.asSuccess()
    }

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
     * Uploads [file] as a new statement named [resourceName] to [taskId] owned by [user].
     * Registers a new resource chain without attaching it or changing task revisions.
     *
     * @since %CURRENT_VERSION%
     */
    @Feature("testsys.user.multi.developer.resource.addResource")
    fun addStatement(
        user: MultipleRoleUser,
        taskId: TaskId,
        resourceName: String,
        file: FileData,
    ): OperationResult<Statement, AddStatementError> = operation<Statement, AddStatementError> {
        ensure(user.hasRole<Developer>(), MissedDeveloperRoleError)
        val task = taskRepository.findById(taskId)
        ensure(task != null) { TaskNotExistsError(taskId) }
        ensure(task.data.owner.id == user.id) { TaskAccessDeniedError(taskId) }

        val resource = statementRepository.save(
            statementData {
                name = resourceName
                description = ""
                versionBucket = VersionBucket(UUID.randomUUID())
                file(file.uploadedFilename, file.content)
            },
        )
        taskRepository.update(task.withData { uploadedResources.add(resource.data.versionBucket) })
        return resource.asSuccess()
    }

    /**
     * Uploads [file] as a new polygon named [resourceName] to [taskId] owned by [user].
     * Registers a new resource chain without attaching it or changing task revisions.
     *
     * @since %CURRENT_VERSION%
     */
    @Feature("testsys.user.multi.developer.resource.addResource")
    fun addTest(user: MultipleRoleUser, taskId: TaskId, resourceName: String, file: FileData): OperationResult<Test, AddTestError> =
        operation<Test, AddTestError> {
            ensure(user.hasRole<Developer>(), MissedDeveloperRoleError)
            val task = taskRepository.findById(taskId)
            ensure(task != null) { TaskNotExistsError(taskId) }
            ensure(task.data.owner.id == user.id) { TaskAccessDeniedError(taskId) }

            val resource = testRepository.save(
                testData {
                    name = resourceName
                    description = ""
                    versionBucket = VersionBucket(UUID.randomUUID())
                    file(file.uploadedFilename, file.content)
                },
            )
            taskRepository.update(task.withData { uploadedResources.add(resource.data.versionBucket) })
            return resource.asSuccess()
        }

    /**
     * Uploads [file] as a new exercise named [resourceName] to [taskId] owned by [user] using [language].
     * Registers a new resource chain without attaching it or changing task revisions.
     *
     * @since %CURRENT_VERSION%
     */
    @Feature("testsys.user.multi.developer.resource.addResource")
    fun addExercise(
        user: MultipleRoleUser,
        taskId: TaskId,
        resourceName: String,
        file: FileData,
        language: TrikSupportedLanguage,
    ): OperationResult<Exercise, AddExerciseError> = operation<Exercise, AddExerciseError> {
        ensure(user.hasRole<Developer>(), MissedDeveloperRoleError)
        val task = taskRepository.findById(taskId)
        ensure(task != null) { TaskNotExistsError(taskId) }
        ensure(task.data.owner.id == user.id) { TaskAccessDeniedError(taskId) }

        val resource = exerciseRepository.save(
            exerciseData {
                name = resourceName
                description = ""
                versionBucket = VersionBucket(UUID.randomUUID())
                file(file.uploadedFilename, file.content)
                when (language) {
                    TrikSupportedLanguage.Python -> this.language.python()
                    TrikSupportedLanguage.JavaScript -> this.language.javaScript()
                    TrikSupportedLanguage.VisualLanguage -> this.language.visualLanguage()
                }
            },
        )
        taskRepository.update(task.withData { uploadedResources.add(resource.data.versionBucket) })
        return resource.asSuccess()
    }

    /**
     * Uploads [file] as a new developer solution named [resourceName] to [taskId] owned by [user],
     * using [language] and [expectedScore]; registers a new chain without changing task revisions.
     * The uploaded resource is not attached to task content.
     *
     * @since %CURRENT_VERSION%
     */
    @Feature("testsys.user.multi.developer.resource.addResource")
    fun addDeveloperSolution(
        user: MultipleRoleUser,
        taskId: TaskId,
        resourceName: String,
        file: FileData,
        language: TrikSupportedLanguage,
        expectedScore: Score,
    ): OperationResult<DeveloperSolution, AddDeveloperSolutionError> = operation<DeveloperSolution, AddDeveloperSolutionError> {
        ensure(user.hasRole<Developer>(), MissedDeveloperRoleError)
        val task = taskRepository.findById(taskId)
        ensure(task != null) { TaskNotExistsError(taskId) }
        ensure(task.data.owner.id == user.id) { TaskAccessDeniedError(taskId) }

        val solution = solutionRepository.save(
            solutionData {
                file(file.uploadedFilename, file.content)
                when (language) {
                    TrikSupportedLanguage.Python -> this.language.python()
                    TrikSupportedLanguage.JavaScript -> this.language.javaScript()
                    TrikSupportedLanguage.VisualLanguage -> this.language.visualLanguage()
                }
            },
        )

        val resource = developerSolutionRepository.save(
            developerSolutionData {
                name = resourceName
                description = ""
                versionBucket = VersionBucket(UUID.randomUUID())
                this.solution = solution.id
                this.expectedScore = expectedScore
            },
        )
        taskRepository.update(task.withData { uploadedResources.add(resource.data.versionBucket) })
        return resource.asSuccess()
    }

    /**
     * Updates the latest [statementId] uploaded to [taskId] owned by [user], preserving omitted [resourceName].
     * A supplied [file] creates a new version without comparison and replaces only an existing editable chain link.
     *
     * @since %CURRENT_VERSION%
     */
    @Feature("testsys.user.multi.developer.resource.updateResource")
    fun updateStatement(
        user: MultipleRoleUser,
        taskId: TaskId,
        statementId: StatementId,
        resourceName: String? = null,
        file: FileData? = null,
    ): OperationResult<Statement, UpdateStatementError> = operation<Statement, UpdateStatementError> {
        ensure(user.hasRole<Developer>(), MissedDeveloperRoleError)
        val task = taskRepository.findById(taskId)
        ensure(task != null) { TaskNotExistsError(taskId) }
        val resource = statementRepository.findById(statementId)
        ensure(resource != null) { StatementNotExistsError(statementId) }
        ensure(task.data.owner.id == user.id) { TaskAccessDeniedError(taskId) }
        ensure(resource.data.versionBucket in task.data.uploadedResources) {
            StatementNotUploadedToTaskError(taskId, statementId)
        }
        ensure(statementRepository.findLatestByVersionBucket(resource.data.versionBucket)?.id == statementId) {
            StatementVersionNotLatestError(statementId)
        }

        if (file == null) {
            return statementRepository.update(resource.withData { name = resourceName ?: resource.data.name }).asSuccess()
        }

        val editable = task.getEditableContent()
        val attached = editable.statement?.let { reference -> statementRepository.load(reference) }
        val isChainAttached = attached?.data?.versionBucket == resource.data.versionBucket

        val updated = resource.withData {
            name = resourceName ?: resource.data.name
            file(file.uploadedFilename, file.content)
        }
        val saved = statementRepository.save(updated.data)
        if (isChainAttached) {
            taskRepository.update(
                task.changeEditableContent {
                    statement = saved.id
                },
            )
        }
        return saved.asSuccess()
    }

    /**
     * Updates the latest [exerciseId] uploaded to [taskId] owned by [user], preserving omitted [resourceName].
     * A supplied [file] creates a new version without comparison and replaces only an existing editable chain link.
     *
     * @since %CURRENT_VERSION%
     */
    @Feature("testsys.user.multi.developer.resource.updateResource")
    fun updateExercise(
        user: MultipleRoleUser,
        taskId: TaskId,
        exerciseId: ExerciseId,
        resourceName: String? = null,
        file: FileData? = null,
    ): OperationResult<Exercise, UpdateExerciseError> = operation<Exercise, UpdateExerciseError> {
        ensure(user.hasRole<Developer>(), MissedDeveloperRoleError)
        val task = taskRepository.findById(taskId)
        ensure(task != null) { TaskNotExistsError(taskId) }
        val resource = exerciseRepository.findById(exerciseId)
        ensure(resource != null) { ExerciseNotExistsError(exerciseId) }
        ensure(task.data.owner.id == user.id) { TaskAccessDeniedError(taskId) }
        ensure(resource.data.versionBucket in task.data.uploadedResources) {
            ExerciseNotUploadedToTaskError(taskId, exerciseId)
        }
        ensure(exerciseRepository.findLatestByVersionBucket(resource.data.versionBucket)?.id == exerciseId) {
            ExerciseVersionNotLatestError(exerciseId)
        }

        if (file == null) {
            return exerciseRepository.update(resource.withData { name = resourceName ?: resource.data.name }).asSuccess()
        }

        val editable = task.getEditableContent()
        val attached = editable.exercise?.let { reference -> exerciseRepository.load(reference) }
        val isChainAttached = attached?.data?.versionBucket == resource.data.versionBucket

        val updated = resource.withData {
            name = resourceName ?: resource.data.name
            file(file.uploadedFilename, file.content)
        }
        val saved = exerciseRepository.save(updated.data)
        if (isChainAttached) {
            taskRepository.update(
                task.changeEditableContent {
                    exercise = saved.id
                },
            )
        }
        return saved.asSuccess()
    }

    /**
     * Updates the latest [testId] uploaded to [taskId] owned by [user], preserving omitted [resourceName].
     * A supplied [file] creates a new version without comparison and replaces only an existing editable chain link.
     *
     * @since %CURRENT_VERSION%
     */
    @Feature("testsys.user.multi.developer.resource.updateResource")
    fun updateTest(
        user: MultipleRoleUser,
        taskId: TaskId,
        testId: TestId,
        resourceName: String? = null,
        file: FileData? = null,
    ): OperationResult<Test, UpdateTestError> = operation<Test, UpdateTestError> {
        ensure(user.hasRole<Developer>(), MissedDeveloperRoleError)
        val task = taskRepository.findById(taskId)
        ensure(task != null) { TaskNotExistsError(taskId) }
        val resource = testRepository.findById(testId)
        ensure(resource != null) { TestNotExistsError(testId) }
        ensure(task.data.owner.id == user.id) { TaskAccessDeniedError(taskId) }
        ensure(resource.data.versionBucket in task.data.uploadedResources) {
            TestNotUploadedToTaskError(taskId, testId)
        }
        ensure(testRepository.findLatestByVersionBucket(resource.data.versionBucket)?.id == testId) {
            TestVersionNotLatestError(testId)
        }

        if (file == null) {
            return testRepository.update(resource.withData { name = resourceName ?: resource.data.name }).asSuccess()
        }

        val editable = task.getEditableContent()
        val replacedIds = testRepository.load(editable.tests)
            .filter { attached -> attached.data.versionBucket == resource.data.versionBucket }
            .map { attached -> attached.id }
            .toSet()

        val updated = resource.withData {
            name = resourceName ?: resource.data.name
            file(file.uploadedFilename, file.content)
        }
        val saved = testRepository.save(updated.data)
        if (replacedIds.isNotEmpty()) {
            taskRepository.update(
                task.changeEditableContent {
                    tests = editable.tests.ids.map { attachedId ->
                        if (attachedId in replacedIds) saved.id else attachedId
                    }.toMutableList()
                },
            )
        }
        return saved.asSuccess()
    }

    /**
     * Updates the latest [developerSolutionId] uploaded to [taskId] owned by [user], preserving omitted [resourceName].
     * Supplied [file] or [expectedScore] creates a new version without comparison; replaces only an existing editable chain link.
     *
     * @since %CURRENT_VERSION%
     */
    @Feature("testsys.user.multi.developer.resource.updateResource")
    fun updateDeveloperSolution(
        user: MultipleRoleUser,
        taskId: TaskId,
        developerSolutionId: DeveloperSolutionId,
        resourceName: String? = null,
        file: FileData? = null,
        expectedScore: Score? = null,
    ): OperationResult<DeveloperSolution, UpdateDeveloperSolutionError> = operation<DeveloperSolution, UpdateDeveloperSolutionError> {
        ensure(user.hasRole<Developer>(), MissedDeveloperRoleError)
        val task = taskRepository.findById(taskId)
        ensure(task != null) { TaskNotExistsError(taskId) }
        val resource = developerSolutionRepository.findById(developerSolutionId)
        ensure(resource != null) { DeveloperSolutionNotExistsError(developerSolutionId) }
        ensure(task.data.owner.id == user.id) { TaskAccessDeniedError(taskId) }
        ensure(resource.data.versionBucket in task.data.uploadedResources) {
            DeveloperSolutionNotUploadedToTaskError(taskId, developerSolutionId)
        }
        ensure(developerSolutionRepository.findLatestByVersionBucket(resource.data.versionBucket)?.id == developerSolutionId) {
            DeveloperSolutionVersionNotLatestError(developerSolutionId)
        }

        if (file == null && expectedScore == null) {
            return developerSolutionRepository.update(resource.withData { name = resourceName ?: resource.data.name }).asSuccess()
        }

        val editable = task.getEditableContent()
        val replacedIds = developerSolutionRepository.load(editable.developerSolutions)
            .filter { attached -> attached.data.versionBucket == resource.data.versionBucket }
            .map { attached -> attached.id }
            .toSet()

        val solutionId = if (file != null) {
            val previousSolution = solutionRepository.load(resource.data.solution)
            solutionRepository.save(
                previousSolution.withData { file(file.uploadedFilename, file.content) }.data,
            ).id
        } else {
            resource.data.solution.id
        }
        val updated = resource.withData {
            name = resourceName ?: resource.data.name
            solution = solutionId
            this.expectedScore = expectedScore ?: resource.data.expectedScore
        }
        val saved = developerSolutionRepository.save(updated.data)
        if (replacedIds.isNotEmpty()) {
            taskRepository.update(
                task.changeEditableContent {
                    developerSolutions = editable.developerSolutions.ids.map { attachedId ->
                        if (attachedId in replacedIds) saved.id else attachedId
                    }.toMutableList()
                },
            )
        }
        return saved.asSuccess()
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

    private fun resourceExists(versionBucket: VersionBucket): Boolean = statementRepository.existsByVersionBucket(versionBucket) ||
        exerciseRepository.existsByVersionBucket(versionBucket) ||
        testRepository.existsByVersionBucket(versionBucket) ||
        developerSolutionRepository.existsByVersionBucket(versionBucket)
}
