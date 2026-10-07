package tech.testsys.operation.user

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.NullSource
import org.junit.jupiter.params.provider.ValueSource
import tech.testsys.domain.builder.api.*
import tech.testsys.domain.builder.task.TaskContentBuilder
import tech.testsys.domain.builder.task.WipTaskContentBuilder
import tech.testsys.domain.builder.util.chooser.TaskValidationExecutionChooser
import tech.testsys.domain.contract.Grader
import tech.testsys.domain.contract.GradingAdmission
import tech.testsys.domain.contract.StoredBlobRef
import tech.testsys.domain.contract.persistence.ContestFilter
import tech.testsys.domain.contract.persistence.Page
import tech.testsys.domain.contract.persistence.Pagination
import tech.testsys.domain.contract.persistence.Sort
import tech.testsys.domain.contract.persistence.TaskFilter
import tech.testsys.domain.contract.persistence.repository.CommunityRepository
import tech.testsys.domain.contract.persistence.repository.ContestRepository
import tech.testsys.domain.contract.persistence.repository.DeveloperSolutionRepository
import tech.testsys.domain.contract.persistence.repository.ExerciseRepository
import tech.testsys.domain.contract.persistence.repository.SolutionRepository
import tech.testsys.domain.contract.persistence.repository.StatementRepository
import tech.testsys.domain.contract.persistence.repository.SubmissionRepository
import tech.testsys.domain.contract.persistence.repository.TaskRepository
import tech.testsys.domain.contract.persistence.repository.TaskValidationRequestRepository
import tech.testsys.domain.contract.persistence.repository.TestRepository
import tech.testsys.domain.model.DomainEntity
import tech.testsys.domain.model.EntityVersion
import tech.testsys.domain.model.LazyEntity
import tech.testsys.domain.model.LazyEntityList
import tech.testsys.domain.model.group.CommunityId
import tech.testsys.domain.model.task.AuthorSubmissionFailure
import tech.testsys.domain.model.task.CommittedTaskContent
import tech.testsys.domain.model.task.Contest
import tech.testsys.domain.model.task.ContestData
import tech.testsys.domain.model.task.ContestId
import tech.testsys.domain.model.task.DeveloperSolution
import tech.testsys.domain.model.task.DeveloperSolutionData
import tech.testsys.domain.model.task.DeveloperSolutionId
import tech.testsys.domain.model.task.Exercise
import tech.testsys.domain.model.task.ExerciseData
import tech.testsys.domain.model.task.ExerciseId
import tech.testsys.domain.model.task.FileData
import tech.testsys.domain.model.task.Score
import tech.testsys.domain.model.task.Solution
import tech.testsys.domain.model.task.SolutionData
import tech.testsys.domain.model.task.SolutionId
import tech.testsys.domain.model.task.Statement
import tech.testsys.domain.model.task.StatementData
import tech.testsys.domain.model.task.StatementId
import tech.testsys.domain.model.task.Submission
import tech.testsys.domain.model.task.SubmissionId
import tech.testsys.domain.model.task.Task
import tech.testsys.domain.model.task.TaskContent
import tech.testsys.domain.model.task.TaskData
import tech.testsys.domain.model.task.TaskId
import tech.testsys.domain.model.task.TaskValidationRequest
import tech.testsys.domain.model.task.TestData
import tech.testsys.domain.model.task.TestId
import tech.testsys.domain.model.task.TrikStudioVersion
import tech.testsys.domain.model.task.TrikSupportedLanguage
import tech.testsys.domain.model.task.VersionBucket
import tech.testsys.domain.model.task.WipTaskContent
import tech.testsys.domain.model.user.MultipleRoleUser
import tech.testsys.domain.model.user.MultipleRoleUserId
import tech.testsys.operation.TaskValidationDispatcher
import tech.testsys.operation.annotation.InternalOperationsApi
import tech.testsys.operation.error.AttemptDurationExceedsContestDurationError
import tech.testsys.operation.error.CommunityAccessDeniedError
import tech.testsys.operation.error.CommunityNotExistsError
import tech.testsys.operation.error.ContestAccessDeniedError
import tech.testsys.operation.error.ContestAlreadySharedError
import tech.testsys.operation.error.ContestEndNotAfterStartError
import tech.testsys.operation.error.ContestEndWithoutStartError
import tech.testsys.operation.error.ContestNotExistsError
import tech.testsys.operation.error.DeveloperSolutionNotExistsError
import tech.testsys.operation.error.DeveloperSolutionNotUploadedToTaskError
import tech.testsys.operation.error.DeveloperSolutionVersionNotLatestError
import tech.testsys.operation.error.ExerciseLanguageAlreadyAttachedError
import tech.testsys.operation.error.ExerciseNotExistsError
import tech.testsys.operation.error.ExerciseNotUploadedToTaskError
import tech.testsys.operation.error.ExerciseVersionNotLatestError
import tech.testsys.operation.error.MissedDeveloperRoleError
import tech.testsys.operation.error.NonPositiveAttemptDurationError
import tech.testsys.operation.error.ResourceAlreadyAttachedError
import tech.testsys.operation.error.ResourceNotExistsError
import tech.testsys.operation.error.ResourceNotUploadedToTaskError
import tech.testsys.operation.error.ResourceVersionNotAttachedError
import tech.testsys.operation.error.ResourceVersionNotExistsError
import tech.testsys.operation.error.StatementNotExistsError
import tech.testsys.operation.error.StatementNotUploadedToTaskError
import tech.testsys.operation.error.StatementVersionNotLatestError
import tech.testsys.operation.error.TaskAccessDeniedError
import tech.testsys.operation.error.TaskAlreadyAttachedToContestError
import tech.testsys.operation.error.TaskAlreadyCommittedError
import tech.testsys.operation.error.TaskAlreadyHasStatementError
import tech.testsys.operation.error.TaskNotAttachedToContestError
import tech.testsys.operation.error.TaskNotCommittedError
import tech.testsys.operation.error.TaskNotExistsError
import tech.testsys.operation.error.TaskNotTestedError
import tech.testsys.operation.error.TaskTestingNoDeveloperSolutionsError
import tech.testsys.operation.error.TaskTestingNoExerciseForLanguageError
import tech.testsys.operation.error.TaskTestingNoPolygonsError
import tech.testsys.operation.error.TaskTestingNoStatementError
import tech.testsys.operation.error.TaskTestingNoTrikStudioVersionsError
import tech.testsys.operation.error.TaskTrikStudioVersionNotSupportedError
import tech.testsys.operation.error.TestNotExistsError
import tech.testsys.operation.error.TestNotUploadedToTaskError
import tech.testsys.operation.error.TestVersionNotLatestError
import tech.testsys.operation.error.getOrThrow
import tech.testsys.operation.util.assertRaises
import tech.testsys.operation.util.getEditableContent
import tech.testsys.operation.util.savedTaskVersion
import tech.testsys.operation.util.testAdministrator
import tech.testsys.operation.util.testCommitedTask
import tech.testsys.operation.util.testCommunity
import tech.testsys.operation.util.testContest
import tech.testsys.operation.util.testDeveloper
import tech.testsys.operation.util.testMultipleRoleUser
import tech.testsys.operation.util.testNewTask
import tech.testsys.operation.util.testSavedContest
import tech.testsys.operation.util.testSavedTask
import tech.testsys.operation.util.testStatement
import tech.testsys.operation.util.testTaskValidationRequest
import tech.testsys.operation.util.testUncommittedTask
import java.time.Duration
import java.time.Instant
import java.util.UUID
import kotlin.test.Test
import tech.testsys.domain.model.task.Test as Polygon

@OptIn(InternalOperationsApi::class)
class DeveloperOperationsTests {

    private val taskRepository = mockk<TaskRepository>()
    private val statementRepository = mockk<StatementRepository>()
    private val communityRepository = mockk<CommunityRepository>()
    private val exerciseRepository = mockk<ExerciseRepository>()
    private val testRepository = mockk<TestRepository>()
    private val developerSolutionRepository = mockk<DeveloperSolutionRepository>()
    private val solutionRepository = mockk<SolutionRepository>()
    private val contestRepository = mockk<ContestRepository>()
    private val taskValidationRequestRepository = mockk<TaskValidationRequestRepository>()
    private val taskValidationDispatcher = mockk<TaskValidationDispatcher>(relaxUnitFun = true)
    private val submissionRepository = mockk<SubmissionRepository>()
    private val grader = mockk<Grader>()
    private val developerOperations = DeveloperOperations(
        taskRepository,
        statementRepository,
        communityRepository,
        exerciseRepository,
        testRepository,
        developerSolutionRepository,
        solutionRepository,
        contestRepository,
        taskValidationRequestRepository,
        taskValidationDispatcher,
        submissionRepository,
        grader,
    )

    private lateinit var developer: MultipleRoleUser

    private val uploadTaskId = TaskId(1)
    private val uploadName = "new resource"
    private val uploadFile = FileData(uploadedFilename = "uploaded.bin", content = byteArrayOf(0, 1, -1))
    private val uploadUuid = UUID(0, 10)
    private val uploadBucket = VersionBucket(uploadUuid)
    private val uploadScore = Score(42)

    @Nested
    inner class TestTaskTests {
        @Test
        fun `should reject a user without the Developer role`() {
            assertRaises(MissedDeveloperRoleError) {
                developerOperations.testTask(testAdministrator {}, TaskId(0))
            }

            verify(exactly = 0) { taskValidationRequestRepository.findOrCreateActive(any(), any()) }
        }

        @Test
        fun `should reject a missing task`() {
            every { taskRepository.findById(TaskId(0)) } returns null

            assertRaises(TaskNotExistsError(TaskId(0))) {
                developerOperations.testTask(developer, TaskId(0))
            }

            verify(exactly = 0) { taskValidationRequestRepository.findOrCreateActive(any(), any()) }
        }

        @Test
        fun `should reject a task owned by another developer`() {
            every { taskRepository.findById(TaskId(0)) } returns testNewTask().withData { owner(99) }

            assertRaises(TaskAccessDeniedError(TaskId(0))) {
                developerOperations.testTask(developer, TaskId(0))
            }

            verify(exactly = 0) { taskValidationRequestRepository.findOrCreateActive(any(), any()) }
        }

        @Test
        fun `should schedule processing of the persisted request`() {
            val task = validTask()
            prepareContent(task)
            val request = testTaskValidationRequest()
            every { taskRepository.findById(task.id) } returns task
            every { taskValidationRequestRepository.findOrCreateActive(task.id, developer.id) } returns request

            developerOperations.testTask(developer, task.id).getOrThrow()

            verify(exactly = 1) { taskValidationDispatcher.schedule(request.id) }
        }

        @Test
        fun `should not schedule processing if a request is rejected`() {
            every { taskRepository.findById(TaskId(0)) } returns null

            assertRaises(TaskNotExistsError(TaskId(0))) { developerOperations.testTask(developer, TaskId(0)) }

            verify(exactly = 0) { taskValidationDispatcher.schedule(any()) }
        }

        @ParameterizedTest
        @ValueSource(strings = ["new", "uncommitted"])
        fun `should return the persisted request without changing any task state`(state: String) {
            val task = validTask(state)
            prepareContent(task)
            val request = testTaskValidationRequest()
            every { taskRepository.findById(task.id) } returns task
            every { taskValidationRequestRepository.findOrCreateActive(task.id, developer.id) } returns request

            val result = developerOperations.testTask(developer, task.id).getOrThrow()

            Assertions.assertSame(request, result)
            verify(exactly = 0) { taskRepository.update(any<Task>()) }
            verify(exactly = 0) { testRepository.load(any<LazyEntityList<TestId, Polygon>>()) }
        }

        @Test
        fun `should reject a committed task before creating a testing request`() {
            val task = testCommitedTask()
            every { taskRepository.findById(task.id) } returns task

            assertRaises(TaskAlreadyCommittedError(task.id)) {
                developerOperations.testTask(developer, task.id)
            }

            verify(exactly = 0) { taskValidationRequestRepository.findOrCreateActive(any(), any()) }
        }

        @Test
        fun `should propagate technical persistence exceptions to the external caller`() {
            val task = validTask()
            prepareContent(task)
            every { taskRepository.findById(TaskId(0)) } returns task
            val failure = IllegalStateException("Database unavailable")
            every { taskValidationRequestRepository.findOrCreateActive(TaskId(0), developer.id) } throws failure

            val actual = Assertions.assertThrows(IllegalStateException::class.java) {
                developerOperations.testTask(developer, TaskId(0))
            }

            Assertions.assertSame(failure, actual)
        }

        @ParameterizedTest
        @ValueSource(strings = ["statement", "polygons", "authors", "versions"])
        fun `should reject missing required content before saving a request`(missing: String) {
            val task = validTask().withData {
                content.new {
                    statement = if (missing == "statement") null else StatementId(1)
                    tests = if (missing == "polygons") mutableListOf() else mutableListOf(TestId(3))
                    developerSolutions = if (missing == "authors") mutableListOf() else mutableListOf(DeveloperSolutionId(4))
                    exercises(listOf(2))
                    supportedTrikStudioVersions = if (missing == "versions") mutableListOf() else mutableListOf(TrikStudioVersion("v1"))
                }
            }
            every { taskRepository.findById(task.id) } returns task
            val expected = when (missing) {
                "statement" -> TaskTestingNoStatementError(task.id)
                "polygons" -> TaskTestingNoPolygonsError(task.id)
                "authors" -> TaskTestingNoDeveloperSolutionsError(task.id)
                else -> TaskTestingNoTrikStudioVersionsError(task.id)
            }

            assertRaises(expected) { developerOperations.testTask(developer, task.id) }

            verify(exactly = 0) { taskValidationRequestRepository.findOrCreateActive(any(), any()) }
        }

        @Test
        fun `should reject an author language without an attached exercise`() {
            val task = validTask()
            prepareContent(task)
            every { exerciseRepository.load(task.getEditableContent().exercises) } returns emptyList()
            every { taskRepository.findById(task.id) } returns task

            assertRaises(TaskTestingNoExerciseForLanguageError(task.id, TrikSupportedLanguage.Python)) {
                developerOperations.testTask(developer, task.id)
            }

            verify(exactly = 0) { taskValidationRequestRepository.findOrCreateActive(any(), any()) }
        }

        @Test
        fun `should reject an incompatible attached contest before saving a request`() {
            val task = validTask()
            prepareContent(task)
            every { taskRepository.findById(task.id) } returns task
            every { contestRepository.findByTaskId(task.id) } returns listOf(testContest { trikStudioVersion("other") })

            assertRaises(TaskTrikStudioVersionNotSupportedError(task.id, TrikStudioVersion("other"))) {
                developerOperations.testTask(developer, task.id)
            }

            verify(exactly = 0) { taskValidationRequestRepository.findOrCreateActive(any(), any()) }
        }

        private fun validTask(state: String = "new"): Task {
            val original = if (state == "new") testNewTask() else testUncommittedTask()
            return original.withData {
                val contentBuilder: tech.testsys.domain.builder.task.WipTaskContentBuilder.() -> Unit = {
                    statement(1)
                    exercises(listOf(2))
                    tests(listOf(3))
                    developerSolutions(listOf(4))
                    supportedTrikStudioVersions(listOf("v1"))
                }
                if (state == "new") {
                    content.new(contentBuilder)
                } else {
                    content.uncommitted(
                        wipBuilder = contentBuilder,
                        lastCommittedBuilder = {
                            statement(99)
                            exercises(listOf(99))
                        },
                    )
                }
            }
        }

        private fun prepareContent(task: Task) {
            val author = viewDeveloperSolution()
            every { exerciseRepository.load(task.getEditableContent().exercises) } returns listOf(viewExercise())
            every { developerSolutionRepository.load(task.getEditableContent().developerSolutions) } returns listOf(author)
            every { solutionRepository.load(author.data.solution) } returns solution {
                id = 5
                createdAt = Instant.EPOCH
                data = solutionData {
                    file("solution.py", byteArrayOf(1))
                    language.python()
                }
            }
            every { contestRepository.findByTaskId(task.id) } returns emptyList()
        }
    }

    @Nested
    inner class ViewTaskValidationRequestsTests {
        @Test
        fun `should reject a user without the Developer role`() {
            assertRaises(MissedDeveloperRoleError) {
                developerOperations.viewTaskValidationRequests(testAdministrator {}, TaskId(0))
            }

            verify(exactly = 0) { taskValidationRequestRepository.findHistory(any()) }
        }

        @Test
        fun `should reject a missing task`() {
            every { taskRepository.findById(TaskId(0)) } returns null

            assertRaises(TaskNotExistsError(TaskId(0))) {
                developerOperations.viewTaskValidationRequests(developer, TaskId(0))
            }
        }

        @Test
        fun `should reject a task owned by another developer`() {
            every { taskRepository.findById(TaskId(0)) } returns testNewTask().withData { owner(99) }

            assertRaises(TaskAccessDeniedError(TaskId(0))) {
                developerOperations.viewTaskValidationRequests(developer, TaskId(0))
            }

            verify(exactly = 0) { taskValidationRequestRepository.findHistory(any()) }
        }

        @ParameterizedTest
        @ValueSource(strings = ["new", "uncommitted", "committed"])
        fun `should return the saved testing history of an owned task in any state`(state: String) {
            val task = when (state) {
                "new" -> testNewTask()
                "uncommitted" -> testUncommittedTask()
                else -> testCommitedTask()
            }
            val history = listOf(testTaskValidationRequest())
            every { taskRepository.findById(task.id) } returns task
            every { taskValidationRequestRepository.findHistory(task.id) } returns history

            val result = developerOperations.viewTaskValidationRequests(developer, task.id).getOrThrow()

            Assertions.assertSame(history, result)
            verify(exactly = 0) { taskRepository.update(any<Task>()) }
        }
    }

    @Nested
    inner class CommitTaskTests {

        private val taskId = TaskId(0)
        private val uploadedBucket = VersionBucket(UUID(0, 2))
        private val pythonSolution = solution {
            id = 5
            createdAt = Instant.EPOCH
            data = solutionData {
                file("solution.py", byteArrayOf(1))
                language.python()
            }
        }

        @BeforeEach
        fun prepareCommit() {
            every { taskRepository.update(any<Task>()) } answers { testSavedTask(firstArg()) }
            every { exerciseRepository.load(any<LazyEntityList<ExerciseId, Exercise>>()) } returns listOf(viewExercise())
            every { developerSolutionRepository.load(any<LazyEntityList<DeveloperSolutionId, DeveloperSolution>>()) } returns
                listOf(viewDeveloperSolution())
            every { solutionRepository.load(any<LazyEntity<SolutionId, Solution>>()) } returns pythonSolution
            every { contestRepository.findByTaskId(taskId) } returns emptyList()
            every { taskValidationRequestRepository.findHistory(taskId) } returns listOf(validationRequest())
            every { submissionRepository.findGradingByTaskId(taskId) } returns listOf(contestSubmission(31), contestSubmission(32))
            every { grader.sendToGrade(any()) } returns GradingAdmission.Accepted
        }

        @Test
        fun `should reject a user without the Developer role before loading the task`() {
            assertRaises(MissedDeveloperRoleError) {
                developerOperations.commitTask(testAdministrator {}, taskId, regradeSubmissions = true)
            }

            verify(exactly = 0) { taskRepository.findById(any()) }
            verifyNoChanges()
        }

        @Test
        fun `should reject a missing task`() {
            every { taskRepository.findById(taskId) } returns null

            assertRaises(TaskNotExistsError(taskId)) { developerOperations.commitTask(developer, taskId, regradeSubmissions = true) }

            verifyNoChanges()
        }

        @Test
        fun `should reject a task owned by another developer`() {
            every { taskRepository.findById(taskId) } returns uncommittedTask().withData { owner(99) }

            assertRaises(TaskAccessDeniedError(taskId)) { developerOperations.commitTask(developer, taskId, regradeSubmissions = true) }

            verifyNoChanges()
        }

        @Test
        fun `should reject a Committed task without a working revision`() {
            every { taskRepository.findById(taskId) } returns testCommitedTask()

            assertRaises(TaskAlreadyCommittedError(taskId)) {
                developerOperations.commitTask(developer, taskId, regradeSubmissions = true)
            }

            verifyNoChanges()
        }

        @Test
        fun `should reject a working revision without validation history`() {
            every { taskRepository.findById(taskId) } returns uncommittedTask()
            every { taskValidationRequestRepository.findHistory(taskId) } returns emptyList()

            assertRaises(TaskNotTestedError(taskId)) { developerOperations.commitTask(developer, taskId, regradeSubmissions = true) }

            verifyNoChanges()
        }

        @ParameterizedTest
        @ValueSource(strings = ["failed", "stoppedByDiagnostics", "active"])
        fun `should reject a working revision whose matching validation request has not succeeded`(execution: String) {
            every { taskRepository.findById(taskId) } returns uncommittedTask()
            every { taskValidationRequestRepository.findHistory(taskId) } returns listOf(unsuccessfulRequest(execution))

            assertRaises(TaskNotTestedError(taskId)) { developerOperations.commitTask(developer, taskId, regradeSubmissions = true) }

            verifyNoChanges()
        }

        @Test
        fun `should reject a working revision if successful validation used another polygon set`() {
            every { taskRepository.findById(taskId) } returns uncommittedTask()
            every { taskValidationRequestRepository.findHistory(taskId) } returns listOf(validationRequest(polygonIds = listOf(3, 7)))

            assertRaises(TaskNotTestedError(taskId)) { developerOperations.commitTask(developer, taskId, regradeSubmissions = true) }

            verifyNoChanges()
        }

        @Test
        fun `should reject a working revision if successful validation used another author solution set`() {
            every { taskRepository.findById(taskId) } returns uncommittedTask()
            every { taskValidationRequestRepository.findHistory(taskId) } returns listOf(validationRequest(authorIds = listOf(4, 8)))

            assertRaises(TaskNotTestedError(taskId)) { developerOperations.commitTask(developer, taskId, regradeSubmissions = true) }

            verifyNoChanges()
        }

        @Test
        fun `should reject a working revision if successful validation used another TRIK Studio version set`() {
            every { taskRepository.findById(taskId) } returns uncommittedTask()
            every { taskValidationRequestRepository.findHistory(taskId) } returns listOf(validationRequest(versionTags = listOf("3.0.0")))

            assertRaises(TaskNotTestedError(taskId)) { developerOperations.commitTask(developer, taskId, regradeSubmissions = true) }

            verifyNoChanges()
        }

        @Test
        fun `should reject a working revision without a statement`() {
            every { taskRepository.findById(taskId) } returns uncommittedTask { statement = null }

            assertRaises(TaskTestingNoStatementError(taskId)) {
                developerOperations.commitTask(developer, taskId, regradeSubmissions = true)
            }

            verifyNoChanges()
        }

        @Test
        fun `should reject an author language without an attached exercise`() {
            every { taskRepository.findById(taskId) } returns uncommittedTask()
            every { exerciseRepository.load(any<LazyEntityList<ExerciseId, Exercise>>()) } returns emptyList()

            assertRaises(TaskTestingNoExerciseForLanguageError(taskId, TrikSupportedLanguage.Python)) {
                developerOperations.commitTask(developer, taskId, regradeSubmissions = true)
            }

            verifyNoChanges()
        }

        @Test
        fun `should reject an attached contest with an unsupported TRIK Studio version`() {
            every { taskRepository.findById(taskId) } returns uncommittedTask()
            every { contestRepository.findByTaskId(taskId) } returns listOf(testContest { trikStudioVersion("2.0.0") })

            assertRaises(TaskTrikStudioVersionNotSupportedError(taskId, TrikStudioVersion("2.0.0"))) {
                developerOperations.commitTask(developer, taskId, regradeSubmissions = true)
            }

            verifyNoChanges()
        }

        @ParameterizedTest
        @ValueSource(strings = ["New", "Uncommitted"])
        fun `should replace the last committed revision with the working revision`(state: String) {
            every { taskRepository.findById(taskId) } returns committableTask(state)

            val result = developerOperations.commitTask(developer, taskId, regradeSubmissions = false).getOrThrow()

            val content = Assertions.assertInstanceOf(TaskContent.Committed::class.java, result.data.content).lastCommitted
            Assertions.assertEquals(StatementId(1), content.statement.id)
            Assertions.assertEquals(listOf(ExerciseId(2)), content.exercises.ids)
            Assertions.assertEquals(listOf(TestId(3), TestId(6)), content.tests.ids)
            Assertions.assertEquals(listOf(DeveloperSolutionId(4)), content.developerSolutions.ids)
            Assertions.assertEquals(listOf(TrikStudioVersion("3.0.0"), TrikStudioVersion("4.0.0")), content.supportedTrikStudioVersions)
            Assertions.assertEquals(savedTaskVersion, result.version)
            verify(exactly = 1) { taskRepository.update(any<Task>()) }
        }

        @Test
        fun `should commit a task compatible with every attached contest`() {
            every { taskRepository.findById(taskId) } returns uncommittedTask()
            every { contestRepository.findByTaskId(taskId) } returns listOf(
                testContest { trikStudioVersion("3.0.0") },
                testContest { trikStudioVersion("4.0.0") },
            )

            val result = developerOperations.commitTask(developer, taskId, regradeSubmissions = false).getOrThrow()

            Assertions.assertInstanceOf(TaskContent.Committed::class.java, result.data.content)
        }

        @Test
        fun `should match a successful snapshot regardless of the order of inputs`() {
            every { taskRepository.findById(taskId) } returns uncommittedTask {
                tests(listOf(6, 3))
                developerSolutions(listOf(8, 4))
                supportedTrikStudioVersions(listOf("4.0.0", "3.0.0"))
            }
            every { taskValidationRequestRepository.findHistory(taskId) } returns listOf(validationRequest(authorIds = listOf(4, 8)))

            val result = developerOperations.commitTask(developer, taskId, regradeSubmissions = false).getOrThrow()

            val content = Assertions.assertInstanceOf(TaskContent.Committed::class.java, result.data.content).lastCommitted
            Assertions.assertEquals(listOf(TestId(6), TestId(3)), content.tests.ids)
        }

        @Test
        fun `should accept an earlier successful validation request after a later failed one`() {
            every { taskRepository.findById(taskId) } returns uncommittedTask()
            every { taskValidationRequestRepository.findHistory(taskId) } returns listOf(
                validationRequest(),
                unsuccessfulRequest("failed"),
            )

            val result = developerOperations.commitTask(developer, taskId, regradeSubmissions = false).getOrThrow()

            Assertions.assertInstanceOf(TaskContent.Committed::class.java, result.data.content)
        }

        @Test
        fun `should preserve task metadata access and uploaded chains when committing`() {
            val original = uncommittedTask()
            every { taskRepository.findById(taskId) } returns original

            val result = developerOperations.commitTask(developer, taskId, regradeSubmissions = false).getOrThrow()

            Assertions.assertEquals("Task name", result.data.name)
            Assertions.assertEquals("Task description", result.data.description)
            Assertions.assertEquals(original.data.owner.id, result.data.owner.id)
            Assertions.assertEquals(listOf(CommunityId(4)), result.data.sharedTo.ids)
            Assertions.assertEquals(setOf(uploadedBucket), result.data.uploadedResources)
        }

        @Test
        fun `should send every contest submission to grading if requested and the polygon set changed`() {
            val sent = mutableListOf<Submission>()
            every { taskRepository.findById(taskId) } returns uncommittedTask(committedPolygonIds = listOf(3))
            every { grader.sendToGrade(capture(sent)) } returns GradingAdmission.Accepted

            developerOperations.commitTask(developer, taskId, regradeSubmissions = true).getOrThrow()

            Assertions.assertEquals(listOf(SubmissionId(31), SubmissionId(32)), sent.map { submission -> submission.id })
        }

        @Test
        fun `should send contest submissions to grading if a polygon is replaced with its new version`() {
            every { taskRepository.findById(taskId) } returns uncommittedTask(committedPolygonIds = listOf(3, 5))

            developerOperations.commitTask(developer, taskId, regradeSubmissions = true).getOrThrow()

            verify(exactly = 2) { grader.sendToGrade(any()) }
        }

        @Test
        fun `should not search or send submissions if regrading is not requested even when the polygon set changed`() {
            every { taskRepository.findById(taskId) } returns uncommittedTask(committedPolygonIds = listOf(3))

            developerOperations.commitTask(developer, taskId, regradeSubmissions = false).getOrThrow()

            verify(exactly = 0) { submissionRepository.findGradingByTaskId(any()) }
            verify(exactly = 0) { grader.sendToGrade(any()) }
        }

        @Test
        fun `should not search submissions if the polygon set is unchanged`() {
            every { taskRepository.findById(taskId) } returns uncommittedTask(committedPolygonIds = listOf(6, 3))

            developerOperations.commitTask(developer, taskId, regradeSubmissions = true).getOrThrow()

            verify(exactly = 0) { submissionRepository.findGradingByTaskId(any()) }
            verify(exactly = 0) { grader.sendToGrade(any()) }
        }

        @Test
        fun `should not search submissions when committing a New task`() {
            every { taskRepository.findById(taskId) } returns newTask()

            developerOperations.commitTask(developer, taskId, regradeSubmissions = true).getOrThrow()

            verify(exactly = 0) { submissionRepository.findGradingByTaskId(any()) }
            verify(exactly = 0) { grader.sendToGrade(any()) }
        }

        @Test
        fun `should propagate a storage exception without sending submissions to grading`() {
            val failure = IllegalStateException("Task was changed concurrently")
            every { taskRepository.findById(taskId) } returns uncommittedTask(committedPolygonIds = listOf(3))
            every { taskRepository.update(any<Task>()) } throws failure

            val actual = Assertions.assertThrows(IllegalStateException::class.java) {
                developerOperations.commitTask(developer, taskId, regradeSubmissions = true)
            }

            Assertions.assertSame(failure, actual)
            verify(exactly = 0) { grader.sendToGrade(any()) }
        }

        private fun committableTask(state: String): Task = when (state) {
            "New" -> newTask()
            "Uncommitted" -> uncommittedTask(committedPolygonIds = listOf(9))
            else -> error("Unsupported test state: $state")
        }

        private fun newTask(): Task = testNewTask().withData {
            content.new { committableResources() }
        }

        private fun uncommittedTask(
            committedPolygonIds: List<Long> = listOf(3, 6),
            customizeWip: WipTaskContentBuilder.() -> Unit = {},
        ): Task = testUncommittedTask().withData {
            name = "Task name"
            description = "Task description"
            sharedTo(listOf(4))
            uploadedResources = mutableSetOf(uploadedBucket)
            content.uncommitted(
                wipBuilder = {
                    committableResources()
                    customizeWip()
                },
                lastCommittedBuilder = {
                    statement(99)
                    exercises(listOf(99))
                    tests(committedPolygonIds)
                    developerSolutions(listOf(4))
                    supportedTrikStudioVersions(listOf("3.0.0", "4.0.0"))
                },
            )
        }

        private fun TaskContentBuilder<*>.committableResources() {
            statement(1)
            exercises(listOf(2))
            tests(listOf(3, 6))
            developerSolutions(listOf(4))
            supportedTrikStudioVersions(listOf("3.0.0", "4.0.0"))
        }

        private fun validationRequest(
            polygonIds: List<Long> = listOf(3, 6),
            authorIds: List<Long> = listOf(4),
            versionTags: List<String> = listOf("3.0.0", "4.0.0"),
            chooseExecution: TaskValidationExecutionChooser.() -> Unit = { completed { completedAt = Instant.EPOCH } },
        ): TaskValidationRequest = testTaskValidationRequest {
            snapshot = taskValidationSnapshot {
                tests(polygonIds)
                developerSolutions = authorIds.map { authorId ->
                    developerSolutionValidationInput {
                        developerSolution(authorId)
                        solution(5)
                        expectedScore = Score(42)
                    }
                }.toMutableList()
                supportedTrikStudioVersions = versionTags.map { tag -> TrikStudioVersion(tag) }.toMutableList()
            }
            execution.chooseExecution()
        }

        private fun unsuccessfulRequest(execution: String): TaskValidationRequest = when (execution) {
            "failed" -> validationRequest {
                completed {
                    submissions(listOf(21))
                    failures = mutableListOf(AuthorSubmissionFailure.ScoreMismatch(submission = SubmissionId(21), actualScore = 0))
                    completedAt = Instant.EPOCH
                }
            }
            "stoppedByDiagnostics" -> validationRequest { stoppedByDiagnostics { completedAt = Instant.EPOCH } }
            "active" -> validationRequest { pendingDiagnostics() }
            else -> error("Unsupported test execution: $execution")
        }

        private fun contestSubmission(submissionId: Long): Submission = submission {
            id = submissionId
            createdAt = Instant.ofEpochSecond(submissionId)
            data = submissionData {
                author(20)
                solution(5)
                task(0)
                status.queued()
                kind.grading { contest(19) }
            }
        }

        private fun verifyNoChanges() {
            verify(exactly = 0) {
                taskRepository.update(any<Task>())
                submissionRepository.findGradingByTaskId(any())
                grader.sendToGrade(any())
            }
        }
    }

    @Nested
    inner class ViewContestsTests {

        private val pagination = Pagination(page = 0, size = 10)

        @Test
        fun `should consider shared communities only from the Developer role`() {
            val user = testMultipleRoleUser {
                roles {
                    developer {
                        memberOf(listOf(1))
                        data = developerData {}
                    }
                    student {
                        memberOf(listOf(2))
                        data = studentData {}
                    }
                    administrator { memberOf(listOf(3)) }
                }
            }
            every {
                contestRepository.findAvailableToDeveloper(ownerId = user.id, communityIds = setOf(CommunityId(1)), pagination = pagination)
            } returns Page(content = emptyList(), pagination = pagination, totalElements = 0)

            val result = developerOperations.viewContests(user, pagination = pagination).getOrThrow().content

            Assertions.assertEquals(emptyList<Contest>(), result)
        }

        @Test
        fun `should raise MissedDeveloperRoleError if user is not a Developer`() {
            assertRaises(MissedDeveloperRoleError) { developerOperations.viewContests(testAdministrator {}, pagination = pagination) }

            verify(exactly = 0) {
                contestRepository.findAvailableToDeveloper(any(), any(), any(), any())
            }
        }

        @Test
        fun `should return an empty page when developer has no contests`() {
            every {
                contestRepository.findAvailableToDeveloper(ownerId = developer.id, communityIds = emptySet(), pagination = pagination)
            } returns Page(content = emptyList(), pagination = pagination, totalElements = 0)

            val result = developerOperations.viewContests(developer, pagination = pagination).getOrThrow().content

            Assertions.assertEquals(emptyList<Contest>(), result)
        }

        @Test
        fun `should return contest information including task count and shared communities`() {
            val user = testDeveloper { data = developerData { contests(listOf(19)) } }
            val original = testContest {
                name = "Viewed contest"
                tasks(listOf(1, 2))
                sharedTo(listOf(3, 4))
            }
            every {
                contestRepository.findAvailableToDeveloper(ownerId = user.id, communityIds = emptySet(), pagination = pagination)
            } returns Page(content = listOf(original), pagination = pagination, totalElements = 1)

            val result = developerOperations.viewContests(user, pagination = pagination).getOrThrow().content.single()

            Assertions.assertSame(original, result)
            Assertions.assertEquals(ContestId(19), result.id)
            Assertions.assertEquals("Viewed contest", result.data.name)
            Assertions.assertEquals(2, result.data.tasks.ids.size)
            Assertions.assertEquals(listOf(CommunityId(3), CommunityId(4)), result.data.sharedTo.ids)
            Assertions.assertEquals(EntityVersion(0), result.version)
            verify(exactly = 0) { contestRepository.update(any<Contest>()) }
            verify(exactly = 0) { contestRepository.save(any<ContestData>()) }
        }

        @Test
        fun `should return available contests absent from the Developer contest list`() {
            val user = testDeveloper { data = developerData {} }
            val original = testContest()
            every {
                contestRepository.findAvailableToDeveloper(ownerId = user.id, communityIds = emptySet(), pagination = pagination)
            } returns Page(content = listOf(original), pagination = pagination, totalElements = 1)

            val result = developerOperations.viewContests(user, pagination = pagination).getOrThrow().content

            Assertions.assertEquals(listOf(original), result)
        }

        @Test
        fun `should include another owner's contest shared to the Developer community`() {
            val user = testDeveloper {
                memberOf(listOf(4))
                data = developerData {}
            }
            val foreign = testContest {
                owner = MultipleRoleUserId(99)
                sharedTo(listOf(4))
            }
            every {
                contestRepository.findAvailableToDeveloper(ownerId = user.id, communityIds = setOf(CommunityId(4)), pagination = pagination)
            } returns Page(content = listOf(foreign), pagination = pagination, totalElements = 1)

            val result = developerOperations.viewContests(user, pagination = pagination).getOrThrow().content

            Assertions.assertEquals(listOf(foreign), result)
        }

        @Test
        fun `should propagate a storage exception when listing contests`() {
            val failure = IllegalStateException("Contest storage unavailable")
            every {
                contestRepository.findAvailableToDeveloper(ownerId = developer.id, communityIds = emptySet(), pagination = pagination)
            } throws failure

            val actual = Assertions.assertThrows(IllegalStateException::class.java) {
                developerOperations.viewContests(developer, pagination = pagination)
            }

            Assertions.assertSame(failure, actual)
        }

        @Test
        fun `should forward pagination and all filters unchanged`() {
            val request = Pagination(page = 3, size = 2, sort = Sort(listOf(Sort.Order("name", Sort.Direction.DESC))))
            val filter = ContestFilter(name = " %_ ", ownerId = MultipleRoleUserId(99), communityId = CommunityId(100))
            val page = Page<Contest>(content = emptyList(), pagination = request, totalElements = 5)
            every {
                contestRepository.findAvailableToDeveloper(
                    ownerId = developer.id,
                    communityIds = emptySet(),
                    pagination = refEq(request),
                    filter = refEq(filter),
                )
            } returns page

            val result = developerOperations.viewContests(user = developer, pagination = request, filter = filter).getOrThrow()

            Assertions.assertSame(page, result)
            Assertions.assertSame(request, result.pagination)
            Assertions.assertEquals(5L, result.totalElements)
        }
    }

    @Nested
    inner class ViewContestTests {

        private val contestId = ContestId(19)

        @Test
        fun `should deny a contest shared only to communities of other roles`() {
            val user = testMultipleRoleUser {
                roles {
                    developer {
                        memberOf(listOf(1))
                        data = developerData {}
                    }
                    student {
                        memberOf(listOf(2))
                        data = studentData {}
                    }
                    administrator { memberOf(listOf(3)) }
                }
            }
            val foreign = testContest {
                owner = MultipleRoleUserId(99)
                sharedTo(listOf(2, 3))
            }
            every { contestRepository.findById(contestId) } returns foreign

            assertRaises(ContestAccessDeniedError(contestId)) { developerOperations.viewContest(user, contestId) }
        }

        @Test
        fun `should raise MissedDeveloperRoleError if user is not a Developer`() {
            assertRaises(MissedDeveloperRoleError) { developerOperations.viewContest(testAdministrator {}, contestId) }

            verify(exactly = 0) { contestRepository.findById(any()) }
        }

        @Test
        fun `should raise ContestNotExistsError if contest does not exist`() {
            every { contestRepository.findById(contestId) } returns null

            assertRaises(ContestNotExistsError(contestId)) { developerOperations.viewContest(developer, contestId) }
        }

        @Test
        fun `should raise ContestAccessDeniedError if contest belongs to another user`() {
            val foreign = testContest { owner = MultipleRoleUserId(99) }
            every { contestRepository.findById(contestId) } returns foreign

            assertRaises(ContestAccessDeniedError(contestId)) { developerOperations.viewContest(developer, contestId) }
        }

        @Test
        fun `should allow viewing another owner's contest shared to the Developer community`() {
            val user = testDeveloper {
                memberOf(listOf(4))
                data = developerData {}
            }
            val foreign = testContest {
                owner = MultipleRoleUserId(99)
                sharedTo(listOf(4))
            }
            every { contestRepository.findById(contestId) } returns foreign

            val result = developerOperations.viewContest(user, contestId).getOrThrow()

            Assertions.assertSame(foreign, result)
        }

        @Test
        fun `should return owned contest data and preserve task references and shared communities`() {
            val original = testContest {
                name = "Viewed contest"
                tasks(listOf(2, 1))
                sharedTo(listOf(3, 4))
            }
            every { contestRepository.findById(contestId) } returns original

            val result = developerOperations.viewContest(developer, contestId).getOrThrow()

            Assertions.assertSame(original, result)
            Assertions.assertEquals(contestId, result.id)
            Assertions.assertEquals("Viewed contest", result.data.name)
            Assertions.assertEquals(listOf(TaskId(2), TaskId(1)), result.data.tasks.ids)
            Assertions.assertEquals(listOf(CommunityId(3), CommunityId(4)), result.data.sharedTo.ids)
            Assertions.assertEquals(EntityVersion(0), result.version)
            verify(exactly = 0) { contestRepository.update(any<Contest>()) }
            verify(exactly = 0) { contestRepository.save(any<ContestData>()) }
        }

        @Test
        fun `should return an owned contest absent from the Developer contest list`() {
            val original = testContest()
            every { contestRepository.findById(contestId) } returns original

            val result = developerOperations.viewContest(developer, contestId).getOrThrow()

            Assertions.assertSame(original, result)
        }

        @Test
        fun `should propagate a storage exception when viewing a contest`() {
            val failure = IllegalStateException("Contest storage unavailable")
            every { contestRepository.findById(contestId) } throws failure

            val actual = Assertions.assertThrows(IllegalStateException::class.java) {
                developerOperations.viewContest(developer, contestId)
            }

            Assertions.assertSame(failure, actual)
        }
    }

    @Nested
    inner class RevertTaskTests {

        private val taskId = TaskId(0)
        private val statement = statementVersion(11)
        private val exercise = exerciseVersion(12)
        private val polygon = polygonVersion(13)
        private val referenceSolution = developerSolutionVersion(14)
        private val original = testUncommittedTask().withData {
            name = "Current task name"
            description = "Current task description"
            sharedTo = mutableListOf(CommunityId(7))
            uploadedResources = mutableSetOf(
                statement.data.versionBucket,
                exercise.data.versionBucket,
                polygon.data.versionBucket,
                referenceSolution.data.versionBucket,
                VersionBucket(UUID(0, 99)),
            )
            content.uncommitted(
                wipBuilder = {
                    statement(21)
                    exercises(listOf(22, 99))
                    tests(listOf(23, 98))
                    developerSolutions(listOf(24, 97))
                    supportedTrikStudioVersions(listOf("4.0.0"))
                },
                lastCommittedBuilder = {
                    statement = this@RevertTaskTests.statement.id
                    exercises = mutableListOf(exercise.id)
                    tests = mutableListOf(polygon.id)
                    developerSolutions = mutableListOf(referenceSolution.id)
                    supportedTrikStudioVersions(listOf("3.0.0"))
                },
            )
        }

        @BeforeEach
        fun prepareRevert() {
            every { taskRepository.findById(taskId) } returns original
            every { taskRepository.update(any<Task>()) } answers { testSavedTask(firstArg()) }
            every { statementRepository.load(any<LazyEntity<StatementId, Statement>>()) } returns statement
            every { exerciseRepository.load(any<LazyEntityList<ExerciseId, Exercise>>()) } returns listOf(exercise)
            every { testRepository.load(any<LazyEntityList<TestId, Polygon>>()) } returns listOf(polygon)
            every { developerSolutionRepository.load(any<LazyEntityList<DeveloperSolutionId, DeveloperSolution>>()) } returns
                listOf(referenceSolution)
            every { statementRepository.findLatestByVersionBucket(statement.data.versionBucket) } returns statement
            every { exerciseRepository.findLatestByVersionBucket(exercise.data.versionBucket) } returns exercise
            every { testRepository.findLatestByVersionBucket(polygon.data.versionBucket) } returns polygon
            every { developerSolutionRepository.findLatestByVersionBucket(referenceSolution.data.versionBucket) } returns referenceSolution
        }

        @Test
        fun `should reject a user without Developer role before loading the task`() {
            assertRaises(MissedDeveloperRoleError) {
                developerOperations.revertTask(testAdministrator {}, taskId)
            }

            verify(exactly = 0) { taskRepository.findById(any()) }
            verifyNoWrites()
        }

        @Test
        fun `should reject a missing task`() {
            every { taskRepository.findById(taskId) } returns null

            assertRaises(TaskNotExistsError(taskId)) { developerOperations.revertTask(developer, taskId) }
            verifyNoWrites()
        }

        @Test
        fun `should reject a task owned by another user before checking its state`() {
            every { taskRepository.findById(taskId) } returns testNewTask().withData { owner(99) }

            assertRaises(TaskAccessDeniedError(taskId)) { developerOperations.revertTask(developer, taskId) }
            verifyNoWrites()
        }

        @Test
        fun `should reject a New task without a committed revision`() {
            every { taskRepository.findById(taskId) } returns testNewTask()

            assertRaises(TaskNotCommittedError(taskId)) { developerOperations.revertTask(developer, taskId) }
            verifyNoWrites()
        }

        @Test
        fun `should reject a Committed task without pending changes`() {
            every { taskRepository.findById(taskId) } returns testCommitedTask()

            assertRaises(TaskAlreadyCommittedError(taskId)) { developerOperations.revertTask(developer, taskId) }
            verifyNoWrites()
        }

        @Test
        fun `should restore committed resources and TRIK versions without copying latest resources`() {
            val result = developerOperations.revertTask(developer, taskId).getOrThrow()

            val content = Assertions.assertInstanceOf(TaskContent.Committed::class.java, result.data.content).lastCommitted
            Assertions.assertEquals(StatementId(11), content.statement.id)
            Assertions.assertEquals(listOf(ExerciseId(12)), content.exercises.ids)
            Assertions.assertEquals(listOf(TestId(13)), content.tests.ids)
            Assertions.assertEquals(listOf(DeveloperSolutionId(14)), content.developerSolutions.ids)
            Assertions.assertEquals(listOf(TrikStudioVersion("3.0.0")), content.supportedTrikStudioVersions)
            verify(exactly = 1) { taskRepository.update(any<Task>()) }
            verify(exactly = 0) { statementRepository.save(any<StatementData>()) }
            verify(exactly = 0) { exerciseRepository.save(any<ExerciseData>()) }
            verify(exactly = 0) { testRepository.save(any<TestData>()) }
            verify(exactly = 0) { developerSolutionRepository.save(any<DeveloperSolutionData>()) }
        }

        @Test
        fun `should preserve task metadata uploaded chains and input revision when reverting`() {
            val result = developerOperations.revertTask(developer, taskId).getOrThrow()

            Assertions.assertEquals("Current task name", result.data.name)
            Assertions.assertEquals("Current task description", result.data.description)
            Assertions.assertEquals(original.data.owner.id, result.data.owner.id)
            Assertions.assertEquals(listOf(CommunityId(7)), result.data.sharedTo.ids)
            Assertions.assertEquals(original.data.uploadedResources, result.data.uploadedResources)
            Assertions.assertEquals(savedTaskVersion, result.version)
            Assertions.assertInstanceOf(TaskContent.Uncommitted::class.java, original.data.content)
            verify { taskRepository.update(match<Task> { it.version == original.version }) }
        }

        @Test
        fun `should restore detached committed resources even if WIP contains no resources`() {
            every { taskRepository.findById(taskId) } returns original.withData {
                content.uncommitted(
                    wipBuilder = {
                        statement = null
                        exercises.clear()
                        tests.clear()
                        developerSolutions.clear()
                    },
                    lastCommittedBuilder = {},
                )
            }

            val result = developerOperations.revertTask(developer, taskId).getOrThrow()

            val content = Assertions.assertInstanceOf(TaskContent.Committed::class.java, result.data.content).lastCommitted
            Assertions.assertEquals(StatementId(11), content.statement.id)
            Assertions.assertEquals(listOf(ExerciseId(12)), content.exercises.ids)
            Assertions.assertEquals(listOf(TestId(13)), content.tests.ids)
            Assertions.assertEquals(listOf(DeveloperSolutionId(14)), content.developerSolutions.ids)
        }

        @Test
        fun `should save committed content as latest versions while retaining current resource metadata`() {
            prepareNewLatestVersions()

            val result = developerOperations.revertTask(developer, taskId).getOrThrow()

            val content = Assertions.assertInstanceOf(TaskContent.Committed::class.java, result.data.content).lastCommitted
            Assertions.assertEquals(StatementId(101), content.statement.id)
            Assertions.assertEquals(listOf(ExerciseId(102)), content.exercises.ids)
            Assertions.assertEquals(listOf(TestId(103)), content.tests.ids)
            Assertions.assertEquals(listOf(DeveloperSolutionId(104)), content.developerSolutions.ids)
            verify(exactly = 1) {
                statementRepository.save(
                    match<StatementData> {
                        it.versionBucket == statement.data.versionBucket && it.name == "resource 21" &&
                            it.description == "description 21" && it.file.uploadedFilename == "file-11" &&
                            it.file.content.contentEquals(byteArrayOf(11))
                    },
                )
                exerciseRepository.save(
                    match<ExerciseData> {
                        it.versionBucket == exercise.data.versionBucket && it.name == "resource 22" &&
                            it.description == "description 22" && it.file.uploadedFilename == "file-12" &&
                            it.file.content.contentEquals(byteArrayOf(12)) && it.language == TrikSupportedLanguage.Python
                    },
                )
                testRepository.save(
                    match<TestData> {
                        it.versionBucket == polygon.data.versionBucket && it.name == "resource 23" &&
                            it.description == "description 23" && it.file.uploadedFilename == "file-13" &&
                            it.file.content.contentEquals(byteArrayOf(13))
                    },
                )
                developerSolutionRepository.save(
                    match<DeveloperSolutionData> {
                        it.versionBucket == referenceSolution.data.versionBucket && it.name == "resource 24" &&
                            it.description == "description 24" && it.solution.id == SolutionId(14) && it.expectedScore == Score(14)
                    },
                )
                taskRepository.update(any<Task>())
            }
            verify(exactly = 0) { solutionRepository.save(any<SolutionData>()) }
        }

        @Test
        fun `should reject a second revert after the task becomes Committed`() {
            val reverted = developerOperations.revertTask(developer, taskId).getOrThrow()
            every { taskRepository.findById(taskId) } returns reverted

            assertRaises(TaskAlreadyCommittedError(taskId)) { developerOperations.revertTask(developer, taskId) }
            verify(exactly = 1) { taskRepository.update(any<Task>()) }
        }

        private fun prepareNewLatestVersions() {
            every { statementRepository.findLatestByVersionBucket(statement.data.versionBucket) } returns statementVersion(21)
            every { exerciseRepository.findLatestByVersionBucket(exercise.data.versionBucket) } returns exerciseVersion(22)
            every { testRepository.findLatestByVersionBucket(polygon.data.versionBucket) } returns polygonVersion(23)
            every { developerSolutionRepository.findLatestByVersionBucket(referenceSolution.data.versionBucket) } returns
                developerSolutionVersion(24)
            every { statementRepository.save(any<StatementData>()) } answers {
                statement {
                    id = 101
                    createdAt = Instant.ofEpochSecond(101)
                    data = firstArg<StatementData>()
                }
            }
            every { exerciseRepository.save(any<ExerciseData>()) } answers {
                exercise {
                    id = 102
                    createdAt = Instant.ofEpochSecond(102)
                    data = firstArg<ExerciseData>()
                }
            }
            every { testRepository.save(any<TestData>()) } answers {
                test {
                    id = 103
                    createdAt = Instant.ofEpochSecond(103)
                    data = firstArg<TestData>()
                }
            }
            every { developerSolutionRepository.save(any<DeveloperSolutionData>()) } answers {
                developerSolution {
                    id = 104
                    createdAt = Instant.ofEpochSecond(104)
                    data = firstArg<DeveloperSolutionData>()
                }
            }
        }

        private fun verifyNoWrites() {
            verify(exactly = 0) {
                taskRepository.update(any<Task>())
                statementRepository.save(any<StatementData>())
                exerciseRepository.save(any<ExerciseData>())
                testRepository.save(any<TestData>())
                developerSolutionRepository.save(any<DeveloperSolutionData>())
            }
        }

        private fun statementVersion(versionId: Long): Statement = statement {
            id = versionId
            createdAt = Instant.ofEpochSecond(versionId)
            data = statementData {
                name = "resource $versionId"
                description = "description $versionId"
                versionBucket = VersionBucket(UUID(0, 11))
                file("file-$versionId", byteArrayOf(versionId.toByte()))
            }
        }

        private fun exerciseVersion(versionId: Long): Exercise = exercise {
            id = versionId
            createdAt = Instant.ofEpochSecond(versionId)
            data = exerciseData {
                name = "resource $versionId"
                description = "description $versionId"
                versionBucket = VersionBucket(UUID(0, 12))
                language.python()
                file("file-$versionId", byteArrayOf(versionId.toByte()))
            }
        }

        private fun polygonVersion(versionId: Long): Polygon = test {
            id = versionId
            createdAt = Instant.ofEpochSecond(versionId)
            data = testData {
                name = "resource $versionId"
                description = "description $versionId"
                versionBucket = VersionBucket(UUID(0, 13))
                file("file-$versionId", byteArrayOf(versionId.toByte()))
            }
        }

        private fun developerSolutionVersion(versionId: Long): DeveloperSolution = developerSolution {
            id = versionId
            createdAt = Instant.ofEpochSecond(versionId)
            data = developerSolutionData {
                name = "resource $versionId"
                description = "description $versionId"
                versionBucket = VersionBucket(UUID(0, 14))
                solution(versionId)
                expectedScore = Score(versionId.toInt())
            }
        }
    }

    @Nested
    inner class CreateContestTests {

        private val version = TrikStudioVersion("3.0.0")
        private val start = Instant.parse("2020-01-01T10:00:00Z")
        private val end = Instant.parse("2020-01-01T12:00:00Z")

        @BeforeEach
        fun prepareContestSave() {
            every { contestRepository.save(any<ContestData>()) } answers {
                contest {
                    id = 19
                    createdAt = Instant.parse("2019-01-01T00:00:00Z")
                    data = firstArg<ContestData>()
                }
            }
        }

        @Test
        fun `should raise MissedDeveloperRoleError without saving if user is not a Developer`() {
            assertRaises(MissedDeveloperRoleError) {
                developerOperations.createContest(testAdministrator {}, "Contest", version)
            }

            verify(exactly = 0) { contestRepository.save(any<ContestData>()) }
        }

        @Test
        fun `should save a contest with supplied metadata and limits owned by the Developer`() {
            val dataSlot = slot<ContestData>()
            every { contestRepository.save(capture(dataSlot)) } answers {
                contest {
                    id = 19
                    createdAt = Instant.parse("2019-01-01T00:00:00Z")
                    data = firstArg<ContestData>()
                }
            }

            val result = developerOperations.createContest(
                user = developer,
                contestName = "Contest",
                trikStudioVersion = version,
                attemptDuration = Duration.ofMinutes(30),
                startsAt = start,
                endsAt = end,
                contestDescription = "Description",
            ).getOrThrow()

            Assertions.assertEquals(19L, result.id.value)
            Assertions.assertEquals(dataSlot.captured, result.data)
            Assertions.assertEquals(developer.id, result.data.owner.id)
            Assertions.assertEquals("Contest", result.data.name)
            Assertions.assertEquals("Description", result.data.description)
            Assertions.assertEquals(version, result.data.trikStudioVersion)
            Assertions.assertEquals(start, result.data.startsAt)
            Assertions.assertEquals(end, result.data.endsAt)
            Assertions.assertEquals(Duration.ofHours(2), result.data.contestDuration)
            Assertions.assertEquals(Duration.ofMinutes(30), result.data.attemptDuration)
            Assertions.assertEquals(emptyList<TaskId>(), result.data.tasks.ids)
            Assertions.assertEquals(emptyList<CommunityId>(), result.data.sharedTo.ids)
            verify(exactly = 1) { contestRepository.save(any<ContestData>()) }
        }

        @Test
        fun `should create a contest without dates or individual limit if optional values are omitted`() {
            val result = developerOperations.createContest(developer, "Contest", version).getOrThrow()

            Assertions.assertNull(result.data.startsAt)
            Assertions.assertNull(result.data.endsAt)
            Assertions.assertNull(result.data.contestDuration)
            Assertions.assertNull(result.data.attemptDuration)
            Assertions.assertEquals("", result.data.description)
        }

        @Test
        fun `should create a finite contest without an individual limit`() {
            val result = developerOperations.createContest(
                user = developer,
                contestName = "Contest",
                trikStudioVersion = version,
                startsAt = start,
                endsAt = end,
            ).getOrThrow()

            Assertions.assertEquals(Duration.ofHours(2), result.data.contestDuration)
            Assertions.assertEquals(end, result.data.endsAt)
            Assertions.assertNull(result.data.attemptDuration)
        }

        @Test
        fun `should accept an individual limit equal to the total interval and a past start`() {
            val result = developerOperations.createContest(
                user = developer,
                contestName = "Contest",
                trikStudioVersion = version,
                attemptDuration = Duration.ofHours(2),
                startsAt = start,
                endsAt = end,
            ).getOrThrow()

            Assertions.assertEquals(Duration.ofHours(2), result.data.attemptDuration)
            Assertions.assertEquals(start, result.data.startsAt)
        }

        @Test
        fun `should accept a start and an individual limit without an end`() {
            val result = developerOperations.createContest(
                user = developer,
                contestName = "Contest",
                trikStudioVersion = version,
                attemptDuration = Duration.ofMillis(1),
                startsAt = start,
            ).getOrThrow()

            Assertions.assertEquals(Duration.ofMillis(1), result.data.attemptDuration)
            Assertions.assertEquals(start, result.data.startsAt)
            Assertions.assertNull(result.data.contestDuration)
            Assertions.assertNull(result.data.endsAt)
        }

        @Test
        fun `should raise ContestEndWithoutStartError without saving if only an end is supplied`() {
            assertRaises(ContestEndWithoutStartError(end)) {
                developerOperations.createContest(developer, "Contest", version, endsAt = end)
            }

            verify(exactly = 0) { contestRepository.save(any<ContestData>()) }
        }

        @ParameterizedTest
        @ValueSource(longs = [0, -1])
        fun `should raise ContestEndNotAfterStartError without saving if the interval is not positive`(offset: Long) {
            val invalidEnd = start.plusSeconds(offset)

            assertRaises(ContestEndNotAfterStartError(startsAt = start, endsAt = invalidEnd)) {
                developerOperations.createContest(developer, "Contest", version, startsAt = start, endsAt = invalidEnd)
            }

            verify(exactly = 0) { contestRepository.save(any<ContestData>()) }
        }

        @ParameterizedTest
        @ValueSource(longs = [0, -1])
        fun `should raise NonPositiveAttemptDurationError without saving for a nonpositive limit`(millis: Long) {
            val duration = Duration.ofMillis(millis)

            assertRaises(NonPositiveAttemptDurationError(duration)) {
                developerOperations.createContest(developer, "Contest", version, attemptDuration = duration)
            }

            verify(exactly = 0) { contestRepository.save(any<ContestData>()) }
        }

        @Test
        fun `should raise AttemptDurationExceedsContestDurationError without saving if the individual limit is too long`() {
            val duration = Duration.ofHours(2).plusMillis(1)

            assertRaises(
                AttemptDurationExceedsContestDurationError(
                    attemptDuration = duration,
                    contestDuration = Duration.ofHours(2),
                ),
            ) {
                developerOperations.createContest(
                    user = developer,
                    contestName = "Contest",
                    trikStudioVersion = version,
                    attemptDuration = duration,
                    startsAt = start,
                    endsAt = end,
                )
            }

            verify(exactly = 0) { contestRepository.save(any<ContestData>()) }
        }

        @ParameterizedTest
        @ValueSource(strings = ["PT0.000000001S", "PT9223372036854775.808S"])
        fun `should reject an individual limit outside the exact millisecond contract before saving`(value: String) {
            val duration = Duration.parse(value)

            Assertions.assertThrows(IllegalArgumentException::class.java) {
                developerOperations.createContest(developer, "Contest", version, attemptDuration = duration)
            }

            verify(exactly = 0) { contestRepository.save(any<ContestData>()) }
        }

        @ParameterizedTest
        @ValueSource(strings = ["PT0.000000001S", "PT9223372036854775.808S"])
        fun `should reject a total interval outside the exact millisecond contract before saving`(value: String) {
            val invalidEnd = start.plus(Duration.parse(value))

            Assertions.assertThrows(IllegalArgumentException::class.java) {
                developerOperations.createContest(developer, "Contest", version, startsAt = start, endsAt = invalidEnd)
            }

            verify(exactly = 0) { contestRepository.save(any<ContestData>()) }
        }

        @Test
        fun `should accept the largest individual limit representable in Long milliseconds`() {
            val duration = Duration.ofMillis(Long.MAX_VALUE)

            val result = developerOperations.createContest(developer, "Contest", version, attemptDuration = duration).getOrThrow()

            Assertions.assertEquals(duration, result.data.attemptDuration)
        }

        @Test
        fun `should propagate a storage exception when saving the contest fails`() {
            val failure = IllegalStateException("Storage failure")
            every { contestRepository.save(any<ContestData>()) } throws failure

            val thrown = Assertions.assertThrows(IllegalStateException::class.java) {
                developerOperations.createContest(developer, "Contest", version)
            }

            Assertions.assertSame(failure, thrown)
        }
    }

    @Nested
    inner class EditContestTests {

        private val contestId = ContestId(19)
        private val start = Instant.parse("2020-01-01T10:00:00Z")
        private val end = Instant.parse("2020-01-01T12:00:00Z")

        @Test
        fun `should raise MissedDeveloperRoleError without reading or updating for a user without the Developer role`() {
            assertRaises(MissedDeveloperRoleError) {
                developerOperations.editContest(testAdministrator {}, contestId, "Updated", startsAt = start, endsAt = end)
            }

            verify(exactly = 0) { contestRepository.findById(any()) }
            verify(exactly = 0) { contestRepository.update(any<Contest>()) }
        }

        @Test
        fun `should raise ContestNotExistsError without updating if the contest is missing`() {
            every { contestRepository.findById(contestId) } returns null

            assertRaises(ContestNotExistsError(contestId)) {
                developerOperations.editContest(developer, contestId, "Updated", startsAt = start, endsAt = end)
            }

            verify(exactly = 0) { contestRepository.update(any<Contest>()) }
        }

        @Test
        fun `should deny editing another owners contest even if it is shared to the Developer community`() {
            val user = testDeveloper {
                memberOf(listOf(4))
                data = developerData {}
            }
            val original = testContest {
                owner = MultipleRoleUserId(99)
                sharedTo(listOf(4))
            }
            prepare(original)

            assertRaises(ContestAccessDeniedError(contestId)) {
                developerOperations.editContest(user, contestId, "Updated", startsAt = null, endsAt = end)
            }

            verify(exactly = 0) { contestRepository.update(any<Contest>()) }
        }

        @Test
        fun `should reject a shared contest before validating the requested dates`() {
            prepare(testContest { sharedTo(listOf(4)) })

            assertRaises(ContestAlreadySharedError(contestId)) {
                developerOperations.editContest(developer, contestId, "Updated", startsAt = null, endsAt = end)
            }

            verify(exactly = 0) { contestRepository.update(any<Contest>()) }
        }

        @Test
        fun `should reject a shared contest even if its name and dates are unchanged`() {
            val original = testContest { sharedTo(listOf(4)) }
            prepare(original)

            assertRaises(ContestAlreadySharedError(contestId)) {
                developerOperations.editContest(developer, contestId, original.data.name, startsAt = null, endsAt = null)
            }

            verify(exactly = 0) { contestRepository.update(any<Contest>()) }
        }

        @ParameterizedTest
        @ValueSource(strings = ["Updated", "", "  Contest  "])
        fun `should change only the name and preserve all contest limits`(name: String) {
            val original = scheduledContest()
            prepare(original)

            val result = developerOperations.editContest(developer, contestId, name, startsAt = start, endsAt = end).getOrThrow()

            Assertions.assertEquals(name, result.data.name)
            Assertions.assertEquals(start, result.data.startsAt)
            Assertions.assertEquals(end, result.data.endsAt)
            Assertions.assertEquals(Duration.ofHours(2), result.data.contestDuration)
            verify(exactly = 1) { contestRepository.update(any<Contest>()) }
        }

        @Test
        fun `should change the start while keeping the supplied end absolute and recalculating the interval`() {
            prepare(scheduledContest())
            val newStart = start.plusSeconds(3600)

            val result = developerOperations.editContest(developer, contestId, "Contest", startsAt = newStart, endsAt = end).getOrThrow()

            Assertions.assertEquals(newStart, result.data.startsAt)
            Assertions.assertEquals(end, result.data.endsAt)
            Assertions.assertEquals(Duration.ofHours(1), result.data.contestDuration)
            verify(exactly = 1) { contestRepository.update(any<Contest>()) }
        }

        @Test
        fun `should change only the end and recalculate the interval`() {
            prepare(scheduledContest())
            val newEnd = end.plusSeconds(3600)

            val result = developerOperations.editContest(developer, contestId, "Contest", startsAt = start, endsAt = newEnd).getOrThrow()

            Assertions.assertEquals(start, result.data.startsAt)
            Assertions.assertEquals(newEnd, result.data.endsAt)
            Assertions.assertEquals(Duration.ofHours(3), result.data.contestDuration)
            verify(exactly = 1) { contestRepository.update(any<Contest>()) }
        }

        @Test
        fun `should change all editable fields preserve other data and return the saved contest with its new version`() {
            val original = scheduledContest().withData {
                tasks(listOf(2, 7))
                attemptDuration = Duration.ofMinutes(30)
            }
            val updated = slot<Contest>()
            val newStart = start.plusSeconds(3600)
            val newEnd = end.plusSeconds(3600)
            val saved = testSavedContest(
                original.withData {
                    name = "Updated"
                    startsAt = newStart
                },
            )
            every { contestRepository.findById(contestId) } returns original
            every { contestRepository.update(capture(updated)) } returns saved

            val result = developerOperations.editContest(developer, contestId, "Updated", startsAt = newStart, endsAt = newEnd).getOrThrow()

            Assertions.assertEquals("Updated", updated.captured.data.name)
            Assertions.assertEquals(newStart, updated.captured.data.startsAt)
            Assertions.assertEquals(newEnd, updated.captured.data.endsAt)
            Assertions.assertEquals(Duration.ofHours(2), updated.captured.data.contestDuration)
            Assertions.assertEquals(original.data.owner.id, updated.captured.data.owner.id)
            Assertions.assertEquals("Description", updated.captured.data.description)
            Assertions.assertEquals(listOf(TaskId(2), TaskId(7)), updated.captured.data.tasks.ids)
            Assertions.assertEquals(Duration.ofMinutes(30), updated.captured.data.attemptDuration)
            Assertions.assertEquals(TrikStudioVersion("3.0.0"), updated.captured.data.trikStudioVersion)
            Assertions.assertEquals(emptyList<CommunityId>(), updated.captured.data.sharedTo.ids)
            Assertions.assertEquals(original.id, updated.captured.id)
            Assertions.assertEquals(original.createdAt, updated.captured.createdAt)
            Assertions.assertEquals(original.version, updated.captured.version)
            Assertions.assertSame(saved, result)
            Assertions.assertEquals(saved.version, result.version)
            Assertions.assertEquals(start, original.data.startsAt)
            Assertions.assertEquals("Contest", original.data.name)
            verify(exactly = 1) { contestRepository.update(any<Contest>()) }
        }

        @Test
        fun `should clear the end and total interval while retaining the start and individual limit`() {
            prepare(scheduledContest().withData { attemptDuration = Duration.ofMinutes(30) })

            val result = developerOperations.editContest(developer, contestId, "Contest", startsAt = start, endsAt = null).getOrThrow()

            Assertions.assertEquals(start, result.data.startsAt)
            Assertions.assertNull(result.data.endsAt)
            Assertions.assertNull(result.data.contestDuration)
            Assertions.assertEquals(Duration.ofMinutes(30), result.data.attemptDuration)
            verify(exactly = 1) { contestRepository.update(any<Contest>()) }
        }

        @Test
        fun `should clear both dates and total interval while retaining the individual limit`() {
            prepare(scheduledContest().withData { attemptDuration = Duration.ofMinutes(30) })

            val result = developerOperations.editContest(developer, contestId, "Contest", startsAt = null, endsAt = null).getOrThrow()

            Assertions.assertNull(result.data.startsAt)
            Assertions.assertNull(result.data.endsAt)
            Assertions.assertNull(result.data.contestDuration)
            Assertions.assertEquals(Duration.ofMinutes(30), result.data.attemptDuration)
            verify(exactly = 1) { contestRepository.update(any<Contest>()) }
        }

        @Test
        fun `should schedule a contest without dates using the requested start and end`() {
            prepare(testContest())

            val result = developerOperations.editContest(developer, contestId, "Contest", startsAt = start, endsAt = end).getOrThrow()

            Assertions.assertEquals(start, result.data.startsAt)
            Assertions.assertEquals(end, result.data.endsAt)
            Assertions.assertEquals(Duration.ofHours(2), result.data.contestDuration)
        }

        @Test
        fun `should set a start without an end for a contest without dates`() {
            prepare(testContest())

            val result = developerOperations.editContest(developer, contestId, "Contest", startsAt = start, endsAt = null).getOrThrow()

            Assertions.assertEquals(start, result.data.startsAt)
            Assertions.assertNull(result.data.endsAt)
            Assertions.assertNull(result.data.contestDuration)
        }

        @Test
        fun `should raise ContestEndWithoutStartError without updating if only an end is supplied`() {
            prepare(scheduledContest())

            assertRaises(ContestEndWithoutStartError(end)) {
                developerOperations.editContest(developer, contestId, "Contest", startsAt = null, endsAt = end)
            }

            verify(exactly = 0) { contestRepository.update(any<Contest>()) }
        }

        @ParameterizedTest
        @ValueSource(longs = [0, -1])
        fun `should raise ContestEndNotAfterStartError without updating if the interval is not positive`(offset: Long) {
            prepare(scheduledContest())
            val invalidEnd = start.plusSeconds(offset)

            assertRaises(ContestEndNotAfterStartError(startsAt = start, endsAt = invalidEnd)) {
                developerOperations.editContest(developer, contestId, "Contest", startsAt = start, endsAt = invalidEnd)
            }

            verify(exactly = 0) { contestRepository.update(any<Contest>()) }
        }

        @Test
        fun `should reject a new interval shorter than the existing individual limit without updating`() {
            prepare(scheduledContest().withData { attemptDuration = Duration.ofHours(1) })
            val newEnd = start.plusSeconds(3600).minusMillis(1)

            assertRaises(
                AttemptDurationExceedsContestDurationError(
                    attemptDuration = Duration.ofHours(1),
                    contestDuration = Duration.ofHours(1).minusMillis(1),
                ),
            ) {
                developerOperations.editContest(developer, contestId, "Contest", startsAt = start, endsAt = newEnd)
            }

            verify(exactly = 0) { contestRepository.update(any<Contest>()) }
        }

        @Test
        fun `should accept a new interval equal to the existing individual limit`() {
            prepare(scheduledContest().withData { attemptDuration = Duration.ofHours(1) })
            val newEnd = start.plusSeconds(3600)

            val result = developerOperations.editContest(developer, contestId, "Contest", startsAt = start, endsAt = newEnd).getOrThrow()

            Assertions.assertEquals(Duration.ofHours(1), result.data.contestDuration)
            Assertions.assertEquals(Duration.ofHours(1), result.data.attemptDuration)
            Assertions.assertEquals(newEnd, result.data.endsAt)
        }

        @Test
        fun `should accept a one millisecond interval`() {
            prepare(scheduledContest())
            val newEnd = start.plusMillis(1)

            val result = developerOperations.editContest(developer, contestId, "Contest", startsAt = start, endsAt = newEnd).getOrThrow()

            Assertions.assertEquals(Duration.ofMillis(1), result.data.contestDuration)
            Assertions.assertEquals(newEnd, result.data.endsAt)
        }

        @Test
        fun `should accept the largest total interval representable in Long milliseconds`() {
            prepare(scheduledContest())
            val newEnd = start.plusMillis(Long.MAX_VALUE)

            val result = developerOperations.editContest(developer, contestId, "Contest", startsAt = start, endsAt = newEnd).getOrThrow()

            Assertions.assertEquals(Duration.ofMillis(Long.MAX_VALUE), result.data.contestDuration)
            Assertions.assertEquals(newEnd, result.data.endsAt)
        }

        @ParameterizedTest
        @ValueSource(strings = ["PT0.000000001S", "PT9223372036854775.808S"])
        fun `should reject a total interval outside the exact millisecond contract without updating`(value: String) {
            prepare(scheduledContest())
            val invalidEnd = start.plus(Duration.parse(value))

            Assertions.assertThrows(IllegalArgumentException::class.java) {
                developerOperations.editContest(developer, contestId, "Contest", startsAt = start, endsAt = invalidEnd)
            }

            verify(exactly = 0) { contestRepository.update(any<Contest>()) }
        }

        @ParameterizedTest
        @ValueSource(strings = ["PT0.000000001S", "PT9223372036854775.808S"])
        fun `should validate the exact millisecond contract even when the name and dates are unchanged`(value: String) {
            val original = testContest {
                startsAt = start
                contestDuration = Duration.parse(value)
            }
            prepare(original)

            Assertions.assertThrows(IllegalArgumentException::class.java) {
                developerOperations.editContest(developer, contestId, original.data.name, startsAt = start, endsAt = original.data.endsAt)
            }

            verify(exactly = 0) { contestRepository.update(any<Contest>()) }
        }

        @Test
        fun `should return the loaded contest without updating if its name and dates are unchanged`() {
            val original = scheduledContest()
            prepare(original)

            val result = developerOperations.editContest(
                user = developer,
                contestId = contestId,
                contestName = original.data.name,
                startsAt = start,
                endsAt = end,
            ).getOrThrow()

            Assertions.assertSame(original, result)
            verify(exactly = 0) { contestRepository.update(any<Contest>()) }
        }

        @Test
        fun `should return a contest without dates without updating if its values are unchanged`() {
            val original = testContest()
            prepare(original)

            val result = developerOperations.editContest(
                user = developer,
                contestId = contestId,
                contestName = original.data.name,
                startsAt = null,
                endsAt = null,
            ).getOrThrow()

            Assertions.assertSame(original, result)
            verify(exactly = 0) { contestRepository.update(any<Contest>()) }
        }

        @Test
        fun `should propagate a storage exception without updating when loading the contest fails`() {
            val failure = IllegalStateException("Storage read failure")
            every { contestRepository.findById(contestId) } throws failure

            val thrown = Assertions.assertThrows(IllegalStateException::class.java) {
                developerOperations.editContest(developer, contestId, "Updated", startsAt = start, endsAt = end)
            }

            Assertions.assertSame(failure, thrown)
            verify(exactly = 0) { contestRepository.update(any<Contest>()) }
        }

        @Test
        fun `should propagate a storage exception when updating the contest fails`() {
            prepare(scheduledContest())
            val failure = IllegalStateException("Storage update failure")
            every { contestRepository.update(any<Contest>()) } throws failure

            val thrown = Assertions.assertThrows(IllegalStateException::class.java) {
                developerOperations.editContest(developer, contestId, "Updated", startsAt = start, endsAt = end)
            }

            Assertions.assertSame(failure, thrown)
        }

        private fun scheduledContest(): Contest = testContest {
            startsAt = start
            contestDuration = Duration.ofHours(2)
        }

        private fun prepare(original: Contest) {
            every { contestRepository.findById(contestId) } returns original
            every { contestRepository.update(any<Contest>()) } answers { testSavedContest(firstArg()) }
        }
    }

    @Nested
    inner class ShareContestTests {

        private val contestId = ContestId(19)
        private val firstCommunityId = CommunityId(1)
        private val secondCommunityId = CommunityId(2)
        private val foreignCommunityId = CommunityId(3)
        private val sharingDeveloper = testDeveloper {
            memberOf(listOf(1, 2))
            data = developerData {}
        }

        @Test
        fun `should raise MissedDeveloperRoleError without reading or updating for a user without the Developer role`() {
            assertRaises(MissedDeveloperRoleError) {
                developerOperations.shareContest(testAdministrator {}, contestId, setOf(firstCommunityId))
            }

            verify(exactly = 0) { contestRepository.findById(any()) }
            verify(exactly = 0) { communityRepository.findById(any()) }
            verify(exactly = 0) { contestRepository.update(any<Contest>()) }
        }

        @Test
        fun `should require the Developer role even when the requested set is empty`() {
            assertRaises(MissedDeveloperRoleError) {
                developerOperations.shareContest(testAdministrator {}, contestId, emptySet())
            }

            verify(exactly = 0) { contestRepository.update(any<Contest>()) }
        }

        @Test
        fun `should raise ContestNotExistsError without updating if the contest is missing`() {
            every { contestRepository.findById(contestId) } returns null

            assertRaises(ContestNotExistsError(contestId)) {
                developerOperations.shareContest(sharingDeveloper, contestId, setOf(firstCommunityId))
            }

            verify(exactly = 0) { contestRepository.update(any<Contest>()) }
        }

        @Test
        fun `should reject a missing selected community before checking contest ownership`() {
            prepare(testContest { owner = MultipleRoleUserId(99) })
            every { communityRepository.findById(foreignCommunityId) } returns null

            assertRaises(CommunityNotExistsError(foreignCommunityId)) {
                developerOperations.shareContest(sharingDeveloper, contestId, setOf(firstCommunityId, foreignCommunityId))
            }

            verify(exactly = 0) { contestRepository.update(any<Contest>()) }
        }

        @Test
        fun `should reject a missing community even if it already has access`() {
            prepare(testContest { sharedTo(listOf(3)) })
            every { communityRepository.findById(foreignCommunityId) } returns null

            assertRaises(CommunityNotExistsError(foreignCommunityId)) {
                developerOperations.shareContest(sharingDeveloper, contestId, setOf(foreignCommunityId))
            }

            verify(exactly = 0) { contestRepository.update(any<Contest>()) }
        }

        @Test
        fun `should deny sharing another owners contest even if it is shared to the Developer community`() {
            prepare(
                testContest {
                    owner = MultipleRoleUserId(99)
                    sharedTo(listOf(1))
                },
            )

            assertRaises(ContestAccessDeniedError(contestId)) {
                developerOperations.shareContest(sharingDeveloper, contestId, setOf(firstCommunityId))
            }

            verify(exactly = 0) { contestRepository.update(any<Contest>()) }
        }

        @Test
        fun `should require contest ownership even when the requested set is empty`() {
            prepare(testContest { owner = MultipleRoleUserId(99) })

            assertRaises(ContestAccessDeniedError(contestId)) {
                developerOperations.shareContest(sharingDeveloper, contestId, emptySet())
            }

            verify(exactly = 0) { contestRepository.update(any<Contest>()) }
        }

        @Test
        fun `should reject a new community outside Developer membership without saving any recipients`() {
            prepare()

            assertRaises(CommunityAccessDeniedError(foreignCommunityId)) {
                developerOperations.shareContest(sharingDeveloper, contestId, setOf(firstCommunityId, foreignCommunityId))
            }

            verify(exactly = 0) { contestRepository.update(any<Contest>()) }
        }

        @Test
        fun `should reject membership in another role when Developer membership is absent`() {
            val user = testMultipleRoleUser {
                roles {
                    developer { data = developerData {} }
                    administrator { memberOf(listOf(1)) }
                }
            }
            prepare()

            assertRaises(CommunityAccessDeniedError(firstCommunityId)) {
                developerOperations.shareContest(user, contestId, setOf(firstCommunityId))
            }

            verify(exactly = 0) { contestRepository.update(any<Contest>()) }
        }

        @Test
        fun `should share a contest without tasks or schedule to selected communities`() {
            prepare()
            val saved = slot<Contest>()
            every { contestRepository.update(capture(saved)) } answers { testSavedContest(firstArg()) }

            val result = developerOperations.shareContest(sharingDeveloper, contestId, setOf(firstCommunityId, secondCommunityId))
                .getOrThrow()

            Assertions.assertEquals(listOf(firstCommunityId, secondCommunityId), result.data.sharedTo.ids)
            Assertions.assertEquals(listOf(firstCommunityId, secondCommunityId), saved.captured.data.sharedTo.ids)
            Assertions.assertEquals(emptyList<TaskId>(), result.data.tasks.ids)
            Assertions.assertNull(result.data.startsAt)
            Assertions.assertNull(result.data.contestDuration)
            verify(exactly = 1) { contestRepository.update(any<Contest>()) }
        }

        @Test
        fun `should add recipients while retaining communities omitted from the request`() {
            prepare(testContest { sharedTo(listOf(1)) })

            val result = developerOperations.shareContest(sharingDeveloper, contestId, setOf(secondCommunityId)).getOrThrow()

            Assertions.assertEquals(listOf(firstCommunityId, secondCommunityId), result.data.sharedTo.ids)
            verify(exactly = 1) { contestRepository.update(any<Contest>()) }
        }

        @Test
        fun `should avoid duplicate recipients when existing and new communities are requested together`() {
            prepare(testContest { sharedTo(listOf(1)) })

            val result = developerOperations.shareContest(sharingDeveloper, contestId, setOf(firstCommunityId, secondCommunityId))
                .getOrThrow()

            Assertions.assertEquals(listOf(firstCommunityId, secondCommunityId), result.data.sharedTo.ids)
        }

        @Test
        fun `should return an unshared contest unchanged without saving when the requested set is empty`() {
            val original = testContest()
            prepare(original)

            val result = developerOperations.shareContest(sharingDeveloper, contestId, emptySet()).getOrThrow()

            Assertions.assertSame(original, result)
            verify(exactly = 0) { contestRepository.update(any<Contest>()) }
        }

        @Test
        fun `should retain all recipients without saving when an empty set is requested for a shared contest`() {
            val original = testContest { sharedTo(listOf(3)) }
            prepare(original)

            val result = developerOperations.shareContest(sharingDeveloper, contestId, emptySet()).getOrThrow()

            Assertions.assertSame(original, result)
            Assertions.assertEquals(listOf(foreignCommunityId), result.data.sharedTo.ids)
            verify(exactly = 0) { contestRepository.update(any<Contest>()) }
        }

        @Test
        fun `should return the loaded contest without saving when every requested recipient already has access`() {
            val original = testContest { sharedTo(listOf(1, 2)) }
            prepare(original)

            val result = developerOperations.shareContest(sharingDeveloper, contestId, setOf(firstCommunityId, secondCommunityId))
                .getOrThrow()

            Assertions.assertSame(original, result)
            verify(exactly = 0) { contestRepository.update(any<Contest>()) }
        }

        @Test
        fun `should permit repeating a previous recipient after leaving the community without saving`() {
            val original = testContest { sharedTo(listOf(3)) }
            prepare(original)

            val result = developerOperations.shareContest(sharingDeveloper, contestId, setOf(foreignCommunityId)).getOrThrow()

            Assertions.assertSame(original, result)
            verify(exactly = 0) { contestRepository.update(any<Contest>()) }
        }

        @Test
        fun `should retain a previous recipient after leaving while adding a new community`() {
            prepare(testContest { sharedTo(listOf(3)) })

            val result = developerOperations.shareContest(sharingDeveloper, contestId, setOf(foreignCommunityId, firstCommunityId))
                .getOrThrow()

            Assertions.assertEquals(listOf(foreignCommunityId, firstCommunityId), result.data.sharedTo.ids)
        }

        @Test
        fun `should preserve contest data and the loaded version when saving new recipients`() {
            val start = Instant.parse("2020-01-01T10:00:00Z")
            val original = testContest {
                tasks(listOf(10, 11))
                startsAt = start
                contestDuration = Duration.ofHours(2)
                attemptDuration = Duration.ofMinutes(30)
                sharedTo(listOf(3))
            }
            prepare(original)
            val saved = slot<Contest>()
            every { contestRepository.update(capture(saved)) } answers { testSavedContest(firstArg()) }

            developerOperations.shareContest(sharingDeveloper, contestId, setOf(firstCommunityId)).getOrThrow()

            val updated = saved.captured
            Assertions.assertEquals(original.id, updated.id)
            Assertions.assertEquals(original.createdAt, updated.createdAt)
            Assertions.assertEquals(original.version, updated.version)
            Assertions.assertEquals(original.data.owner.id, updated.data.owner.id)
            Assertions.assertEquals(original.data.name, updated.data.name)
            Assertions.assertEquals(original.data.description, updated.data.description)
            Assertions.assertEquals(listOf(TaskId(10), TaskId(11)), updated.data.tasks.ids)
            Assertions.assertEquals(start, updated.data.startsAt)
            Assertions.assertEquals(Duration.ofHours(2), updated.data.contestDuration)
            Assertions.assertEquals(Duration.ofMinutes(30), updated.data.attemptDuration)
            Assertions.assertEquals(start.plus(Duration.ofHours(2)), updated.data.endsAt)
            Assertions.assertEquals(original.data.trikStudioVersion, updated.data.trikStudioVersion)
            Assertions.assertEquals(listOf(foreignCommunityId, firstCommunityId), updated.data.sharedTo.ids)
            Assertions.assertEquals(listOf(foreignCommunityId), original.data.sharedTo.ids)
        }

        @Test
        fun `should return the repository result with its updated version`() {
            prepare()
            val repositoryResult = testSavedContest(testContest { sharedTo(listOf(1)) })
            every { contestRepository.update(any<Contest>()) } returns repositoryResult

            val result = developerOperations.shareContest(sharingDeveloper, contestId, setOf(firstCommunityId)).getOrThrow()

            Assertions.assertSame(repositoryResult, result)
        }

        @Test
        fun `should propagate a storage exception without saving when loading the contest fails`() {
            val failure = IllegalStateException("Storage read failure")
            every { contestRepository.findById(contestId) } throws failure

            val thrown = Assertions.assertThrows(IllegalStateException::class.java) {
                developerOperations.shareContest(sharingDeveloper, contestId, setOf(firstCommunityId))
            }

            Assertions.assertSame(failure, thrown)
            verify(exactly = 0) { contestRepository.update(any<Contest>()) }
        }

        @Test
        fun `should propagate a storage exception without saving when loading a selected community fails`() {
            prepare()
            val failure = IllegalStateException("Community storage read failure")
            every { communityRepository.findById(firstCommunityId) } throws failure

            val thrown = Assertions.assertThrows(IllegalStateException::class.java) {
                developerOperations.shareContest(sharingDeveloper, contestId, setOf(firstCommunityId))
            }

            Assertions.assertSame(failure, thrown)
            verify(exactly = 0) { contestRepository.update(any<Contest>()) }
        }

        @Test
        fun `should propagate a storage exception when updating the contest fails`() {
            prepare()
            val failure = IllegalStateException("Storage update failure")
            every { contestRepository.update(any<Contest>()) } throws failure

            val thrown = Assertions.assertThrows(IllegalStateException::class.java) {
                developerOperations.shareContest(sharingDeveloper, contestId, setOf(firstCommunityId))
            }

            Assertions.assertSame(failure, thrown)
        }

        private fun prepare(original: Contest = testContest()) {
            every { contestRepository.findById(contestId) } returns original
            every { communityRepository.findById(firstCommunityId) } returns testCommunity(1)
            every { communityRepository.findById(secondCommunityId) } returns testCommunity(2)
            every { communityRepository.findById(foreignCommunityId) } returns testCommunity(3)
            every { contestRepository.update(any<Contest>()) } answers { testSavedContest(firstArg()) }
        }
    }

    @Nested
    inner class AttachTaskTests {

        private val contestId = ContestId(19)
        private val taskId = TaskId(0)
        private val version = TrikStudioVersion("3.0.0")

        @AfterEach
        fun verifyNoTaskWrites() {
            verify(exactly = 0) { taskRepository.update(any<Task>()) }
            verify(exactly = 0) { taskRepository.update(any<List<Task>>()) }
            verify(exactly = 0) { taskRepository.save(any<TaskData>()) }
            verify(exactly = 0) { taskRepository.save(any<List<TaskData>>()) }
            verify(exactly = 0) { contestRepository.save(any<ContestData>()) }
            verify(exactly = 0) { contestRepository.save(any<List<ContestData>>()) }
        }

        @Test
        fun `should reject a user without Developer role before reading entities`() {
            assertRaises(MissedDeveloperRoleError) {
                developerOperations.attachTask(testAdministrator {}, contestId, taskId)
            }

            verify(exactly = 0) { contestRepository.findById(any()) }
            verify(exactly = 0) { taskRepository.findById(any()) }
            assertNoContestUpdate()
        }

        @Test
        fun `should raise ContestNotExistsError without updating if contest is missing`() {
            every { contestRepository.findById(contestId) } returns null

            assertRaises(ContestNotExistsError(contestId)) {
                developerOperations.attachTask(developer, contestId, taskId)
            }

            assertNoContestUpdate()
        }

        @Test
        fun `should reject a missing task before checking contest ownership`() {
            prepare(originalContest = testContest { owner(99) })
            every { taskRepository.findById(taskId) } returns null

            assertRaises(TaskNotExistsError(taskId)) {
                developerOperations.attachTask(developer, contestId, taskId)
            }

            assertNoContestUpdate()
        }

        @ParameterizedTest
        @ValueSource(ints = [0, 1])
        fun `should deny another owners contest even when shared to Developer communities`(recipientCount: Int) {
            val user = testDeveloper {
                memberOf(listOf(1))
                data = developerData {}
            }
            val recipients = listOf(1L).take(recipientCount)
            prepare(
                originalContest = testContest {
                    owner(99)
                    sharedTo(recipients)
                },
            )

            assertRaises(ContestAccessDeniedError(contestId)) {
                developerOperations.attachTask(user, contestId, taskId)
            }

            assertNoContestUpdate()
        }

        @Test
        fun `should deny a foreign task shared outside Developer communities`() {
            prepare(
                originalTask = committedTask().withData {
                    owner(99)
                    sharedTo(listOf(2))
                },
            )

            assertRaises(TaskAccessDeniedError(taskId)) {
                developerOperations.attachTask(developer, contestId, taskId)
            }

            assertNoContestUpdate()
        }

        @Test
        fun `should attach a foreign task shared to one of the Developer communities`() {
            val user = testDeveloper {
                memberOf(listOf(1, 2))
                data = developerData {}
            }
            prepare(
                originalTask = committedTask().withData {
                    owner(99)
                    sharedTo(listOf(2, 3))
                },
            )

            val result = developerOperations.attachTask(user, contestId, taskId).getOrThrow()

            Assertions.assertEquals(listOf(taskId), result.data.tasks.ids)
        }

        @Test
        fun `should deny a foreign task when only another role belongs to its community`() {
            val user = testMultipleRoleUser {
                roles {
                    developer { data = developerData {} }
                    administrator { memberOf(listOf(1)) }
                }
            }
            prepare(
                originalTask = committedTask().withData {
                    owner(99)
                    sharedTo(listOf(1))
                },
            )

            assertRaises(TaskAccessDeniedError(taskId)) {
                developerOperations.attachTask(user, contestId, taskId)
            }

            assertNoContestUpdate()
        }

        @Test
        fun `should use Developer membership for a user with several roles`() {
            val user = testMultipleRoleUser {
                roles {
                    developer {
                        memberOf(listOf(1))
                        data = developerData {}
                    }
                    administrator { memberOf(listOf(2)) }
                }
            }
            prepare(
                originalTask = committedTask().withData {
                    owner(99)
                    sharedTo(listOf(1))
                },
            )

            val result = developerOperations.attachTask(user, contestId, taskId).getOrThrow()

            Assertions.assertEquals(listOf(taskId), result.data.tasks.ids)
        }

        @ParameterizedTest
        @ValueSource(ints = [0, 1])
        fun `should reject a shared contest before repeated attachment`(attachedTaskCount: Int) {
            val tasks = listOf(0L).take(attachedTaskCount)
            prepare(
                originalContest = testContest {
                    sharedTo(listOf(1))
                    tasks(tasks)
                },
            )

            assertRaises(ContestAlreadySharedError(contestId)) {
                developerOperations.attachTask(developer, contestId, taskId)
            }

            assertNoContestUpdate()
        }

        @Test
        fun `should reject New task even if its working revision supports the contest version`() {
            val newTask = testNewTask().withData { content.new { supportedTrikStudioVersions(listOf("3.0.0")) } }
            prepare(originalTask = newTask)

            assertRaises(TaskNotCommittedError(taskId)) {
                developerOperations.attachTask(developer, contestId, taskId)
            }

            assertNoContestUpdate()
        }

        @ParameterizedTest
        @ValueSource(strings = ["Committed", "Uncommitted"])
        fun `should attach an owned task with a committed revision without community membership`(state: String) {
            val originalTask = compatibleTask(state)
            val originalData = originalTask.data
            prepare(originalTask = originalTask)
            val saved = slot<Contest>()
            every { contestRepository.update(capture(saved)) } answers { testSavedContest(firstArg()) }

            val result = developerOperations.attachTask(developer, contestId, taskId).getOrThrow()

            Assertions.assertEquals(listOf(taskId), result.data.tasks.ids)
            Assertions.assertEquals(listOf(taskId), saved.captured.data.tasks.ids)
            Assertions.assertSame(originalData, originalTask.data)
            Assertions.assertEquals(0L, originalTask.version?.value)
            verify(exactly = 1) { contestRepository.update(any<Contest>()) }
        }

        @Test
        fun `should accept compatible committed revision when working revision is incompatible`() {
            prepare(originalTask = uncommittedTask(committedVersions = listOf("3.0.0"), workingVersions = listOf("4.0.0")))

            val result = developerOperations.attachTask(developer, contestId, taskId).getOrThrow()

            Assertions.assertEquals(listOf(taskId), result.data.tasks.ids)
        }

        @Test
        fun `should reject incompatible committed revision when working revision is compatible`() {
            prepare(originalTask = uncommittedTask(committedVersions = listOf("4.0.0"), workingVersions = listOf("3.0.0")))

            assertRaises(TaskTrikStudioVersionNotSupportedError(taskId, version)) {
                developerOperations.attachTask(developer, contestId, taskId)
            }

            assertNoContestUpdate()
        }

        @Test
        fun `should reject a committed revision without supported versions`() {
            prepare(originalTask = committedTask(versions = emptyList()))

            assertRaises(TaskTrikStudioVersionNotSupportedError(taskId, version)) {
                developerOperations.attachTask(developer, contestId, taskId)
            }

            assertNoContestUpdate()
        }

        @ParameterizedTest
        @ValueSource(strings = ["3.0", "3.0.1", "4.0.0"])
        fun `should require the exact contest version in committed supported versions`(supportedVersion: String) {
            prepare(originalTask = committedTask(versions = listOf(supportedVersion)))

            assertRaises(TaskTrikStudioVersionNotSupportedError(taskId, version)) {
                developerOperations.attachTask(developer, contestId, taskId)
            }

            assertNoContestUpdate()
        }

        @Test
        fun `should accept one exact matching version among several supported versions`() {
            prepare(originalTask = committedTask(versions = listOf("2.0.0", "3.0.0", "4.0.0")))

            val result = developerOperations.attachTask(developer, contestId, taskId).getOrThrow()

            Assertions.assertEquals(listOf(taskId), result.data.tasks.ids)
        }

        @Test
        fun `should append a task while preserving existing tasks and contest metadata`() {
            val original = testContest {
                tasks(listOf(7, 8))
                name = "Original contest"
                description = "Original description"
                startsAt = Instant.parse("2020-01-01T10:00:00Z")
                contestDuration = Duration.ofHours(2)
                attemptDuration = Duration.ofMinutes(30)
            }
            prepare(originalContest = original)
            val saved = slot<Contest>()
            every { contestRepository.update(capture(saved)) } answers { testSavedContest(firstArg()) }

            val result = developerOperations.attachTask(developer, contestId, taskId).getOrThrow()

            Assertions.assertEquals(listOf(TaskId(7), TaskId(8), taskId), saved.captured.data.tasks.ids)
            Assertions.assertEquals(original.id, saved.captured.id)
            Assertions.assertEquals(original.createdAt, saved.captured.createdAt)
            Assertions.assertEquals(0L, saved.captured.version?.value)
            Assertions.assertEquals(original.data.owner.id, saved.captured.data.owner.id)
            Assertions.assertEquals("Original contest", saved.captured.data.name)
            Assertions.assertEquals("Original description", saved.captured.data.description)
            Assertions.assertEquals(original.data.startsAt, saved.captured.data.startsAt)
            Assertions.assertEquals(Duration.ofHours(2), saved.captured.data.contestDuration)
            Assertions.assertEquals(Duration.ofMinutes(30), saved.captured.data.attemptDuration)
            Assertions.assertEquals(original.data.endsAt, saved.captured.data.endsAt)
            Assertions.assertEquals(version, saved.captured.data.trikStudioVersion)
            Assertions.assertEquals(emptyList<CommunityId>(), saved.captured.data.sharedTo.ids)
            Assertions.assertEquals(listOf(TaskId(7), TaskId(8)), original.data.tasks.ids)
            Assertions.assertEquals(1L, result.version?.value)
            verify(exactly = 1) { contestRepository.update(any<Contest>()) }
        }

        @Test
        fun `should return the actual updated repository object and its version`() {
            prepare()
            val repositoryResult = testSavedContest(testContest { tasks(listOf(0)) })
            every { contestRepository.update(any<Contest>()) } returns repositoryResult

            val result = developerOperations.attachTask(developer, contestId, taskId).getOrThrow()

            Assertions.assertSame(repositoryResult, result)
            Assertions.assertEquals(1L, result.version?.value)
        }

        @Test
        fun `should reject repeated attachment without updating the contest`() {
            prepare(originalContest = testContest { tasks(listOf(0)) })

            assertRaises(TaskAlreadyAttachedToContestError(contestId, taskId)) {
                developerOperations.attachTask(developer, contestId, taskId)
            }

            assertNoContestUpdate()
        }

        @Test
        fun `should check task access before repeated attachment`() {
            prepare(
                originalContest = testContest { tasks(listOf(0)) },
                originalTask = committedTask().withData { owner(99) },
            )

            assertRaises(TaskAccessDeniedError(taskId)) {
                developerOperations.attachTask(developer, contestId, taskId)
            }

            assertNoContestUpdate()
        }

        @Test
        fun `should check committed revision existence before repeated attachment`() {
            prepare(originalContest = testContest { tasks(listOf(0)) }, originalTask = testNewTask())

            assertRaises(TaskNotCommittedError(taskId)) {
                developerOperations.attachTask(developer, contestId, taskId)
            }

            assertNoContestUpdate()
        }

        @Test
        fun `should check version compatibility before repeated attachment`() {
            prepare(
                originalContest = testContest { tasks(listOf(0)) },
                originalTask = committedTask(versions = listOf("4.0.0")),
            )

            assertRaises(TaskTrikStudioVersionNotSupportedError(taskId, version)) {
                developerOperations.attachTask(developer, contestId, taskId)
            }

            assertNoContestUpdate()
        }

        @Test
        fun `should propagate storage exceptions when loading the contest`() {
            val failure = IllegalStateException("Contest storage read failure")
            every { contestRepository.findById(contestId) } throws failure

            val thrown = Assertions.assertThrows(IllegalStateException::class.java) {
                developerOperations.attachTask(developer, contestId, taskId)
            }

            Assertions.assertSame(failure, thrown)
            assertNoContestUpdate()
        }

        @Test
        fun `should propagate storage exceptions when loading the task`() {
            prepare()
            val failure = IllegalStateException("Task storage read failure")
            every { taskRepository.findById(taskId) } throws failure

            val thrown = Assertions.assertThrows(IllegalStateException::class.java) {
                developerOperations.attachTask(developer, contestId, taskId)
            }

            Assertions.assertSame(failure, thrown)
            assertNoContestUpdate()
        }

        @ParameterizedTest
        @ValueSource(strings = ["Storage update failure", "Optimistic lock conflict"])
        fun `should propagate update exceptions including optimistic lock conflicts`(message: String) {
            prepare()
            val failure = IllegalStateException(message)
            every { contestRepository.update(any<Contest>()) } throws failure

            val thrown = Assertions.assertThrows(IllegalStateException::class.java) {
                developerOperations.attachTask(developer, contestId, taskId)
            }

            Assertions.assertSame(failure, thrown)
            verify(exactly = 1) { contestRepository.update(any<Contest>()) }
        }

        private fun prepare(originalContest: Contest = testContest(), originalTask: Task = committedTask()) {
            every { contestRepository.findById(contestId) } returns originalContest
            every { taskRepository.findById(taskId) } returns originalTask
            every { contestRepository.update(any<Contest>()) } answers { testSavedContest(firstArg()) }
        }

        private fun committedTask(versions: List<String> = listOf("3.0.0")): Task = testCommitedTask().withData {
            content.committed { supportedTrikStudioVersions(versions) }
        }

        private fun uncommittedTask(
            committedVersions: List<String> = listOf("3.0.0"),
            workingVersions: List<String> = listOf("3.0.0"),
        ): Task = testUncommittedTask().withData {
            content.uncommitted(
                wipBuilder = { supportedTrikStudioVersions(workingVersions) },
                lastCommittedBuilder = { supportedTrikStudioVersions(committedVersions) },
            )
        }

        private fun compatibleTask(state: String): Task = when (state) {
            "Committed" -> committedTask()
            "Uncommitted" -> uncommittedTask()
            else -> error("Unsupported attach-task test state: $state")
        }

        private fun assertNoContestUpdate() {
            verify(exactly = 0) { contestRepository.update(any<Contest>()) }
            verify(exactly = 0) { contestRepository.update(any<List<Contest>>()) }
        }
    }

    @Nested
    inner class DetachTaskTests {

        private val contestId = ContestId(19)
        private val taskId = TaskId(0)

        @AfterEach
        fun verifyNoTaskWrites() {
            verify(exactly = 0) { taskRepository.update(any<Task>()) }
            verify(exactly = 0) { taskRepository.update(any<List<Task>>()) }
            verify(exactly = 0) { taskRepository.save(any<TaskData>()) }
            verify(exactly = 0) { taskRepository.save(any<List<TaskData>>()) }
            verify(exactly = 0) { contestRepository.save(any<ContestData>()) }
            verify(exactly = 0) { contestRepository.save(any<List<ContestData>>()) }
        }

        @Test
        fun `should reject a user without Developer role before reading entities`() {
            assertRaises(MissedDeveloperRoleError) {
                developerOperations.detachTask(testAdministrator {}, contestId, taskId)
            }

            verify(exactly = 0) { contestRepository.findById(any()) }
            verify(exactly = 0) { taskRepository.findById(any()) }
            assertNoContestUpdate()
        }

        @Test
        fun `should raise ContestNotExistsError without updating if contest is missing`() {
            every { contestRepository.findById(contestId) } returns null

            assertRaises(ContestNotExistsError(contestId)) {
                developerOperations.detachTask(developer, contestId, taskId)
            }

            assertNoContestUpdate()
        }

        @Test
        fun `should reject a missing task before checking contest ownership`() {
            prepare(originalContest = testContest { owner(99) })
            every { taskRepository.findById(taskId) } returns null

            assertRaises(TaskNotExistsError(taskId)) {
                developerOperations.detachTask(developer, contestId, taskId)
            }

            assertNoContestUpdate()
        }

        @ParameterizedTest
        @ValueSource(ints = [0, 1])
        fun `should deny another owners contest even when shared to Developer communities`(recipientCount: Int) {
            val user = testDeveloper {
                memberOf(listOf(1))
                data = developerData {}
            }
            val recipients = listOf(1L).take(recipientCount)
            prepare(
                originalContest = testContest {
                    owner(99)
                    sharedTo(recipients)
                    tasks(listOf(0))
                },
            )

            assertRaises(ContestAccessDeniedError(contestId)) {
                developerOperations.detachTask(user, contestId, taskId)
            }

            assertNoContestUpdate()
        }

        @ParameterizedTest
        @ValueSource(ints = [0, 1])
        fun `should reject a shared contest before checking task attachment`(attachedTaskCount: Int) {
            val tasks = listOf(0L).take(attachedTaskCount)
            prepare(
                originalContest = testContest {
                    sharedTo(listOf(1))
                    tasks(tasks)
                },
            )

            assertRaises(ContestAlreadySharedError(contestId)) {
                developerOperations.detachTask(developer, contestId, taskId)
            }

            assertNoContestUpdate()
        }

        @ParameterizedTest
        @ValueSource(ints = [0, 2])
        fun `should raise TaskNotAttachedToContestError if contest does not contain the task`(attachedTaskCount: Int) {
            val tasks = listOf(7L, 8L).take(attachedTaskCount)
            prepare(originalContest = testContest { tasks(tasks) })

            assertRaises(TaskNotAttachedToContestError(contestId, taskId)) {
                developerOperations.detachTask(developer, contestId, taskId)
            }

            assertNoContestUpdate()
        }

        @Test
        fun `should remove the task while preserving other tasks and contest metadata`() {
            val original = testContest {
                tasks(listOf(7, 0, 8))
                name = "Original contest"
                description = "Original description"
                startsAt = Instant.parse("2020-01-01T10:00:00Z")
                contestDuration = Duration.ofHours(2)
                attemptDuration = Duration.ofMinutes(30)
            }
            prepare(originalContest = original)
            val saved = slot<Contest>()
            every { contestRepository.update(capture(saved)) } answers { testSavedContest(firstArg()) }

            val result = developerOperations.detachTask(developer, contestId, taskId).getOrThrow()

            Assertions.assertEquals(listOf(TaskId(7), TaskId(8)), saved.captured.data.tasks.ids)
            Assertions.assertEquals(original.id, saved.captured.id)
            Assertions.assertEquals(original.createdAt, saved.captured.createdAt)
            Assertions.assertEquals(0L, saved.captured.version?.value)
            Assertions.assertEquals(original.data.owner.id, saved.captured.data.owner.id)
            Assertions.assertEquals("Original contest", saved.captured.data.name)
            Assertions.assertEquals("Original description", saved.captured.data.description)
            Assertions.assertEquals(original.data.startsAt, saved.captured.data.startsAt)
            Assertions.assertEquals(Duration.ofHours(2), saved.captured.data.contestDuration)
            Assertions.assertEquals(Duration.ofMinutes(30), saved.captured.data.attemptDuration)
            Assertions.assertEquals(original.data.endsAt, saved.captured.data.endsAt)
            Assertions.assertEquals(TrikStudioVersion("3.0.0"), saved.captured.data.trikStudioVersion)
            Assertions.assertEquals(emptyList<CommunityId>(), saved.captured.data.sharedTo.ids)
            Assertions.assertEquals(listOf(TaskId(7), taskId, TaskId(8)), original.data.tasks.ids)
            Assertions.assertEquals(listOf(TaskId(7), TaskId(8)), result.data.tasks.ids)
            verify(exactly = 1) { contestRepository.update(any<Contest>()) }
        }

        @Test
        fun `should leave the contest without tasks when its only task is detached`() {
            prepare(originalContest = testContest { tasks(listOf(0)) })

            val result = developerOperations.detachTask(developer, contestId, taskId).getOrThrow()

            Assertions.assertEquals(emptyList<TaskId>(), result.data.tasks.ids)
            verify(exactly = 1) { contestRepository.update(any<Contest>()) }
        }

        @Test
        fun `should detach a foreign task that is no longer shared to Developer communities`() {
            val user = testDeveloper {
                memberOf(listOf(1))
                data = developerData {}
            }
            prepare(
                originalContest = testContest { tasks(listOf(0)) },
                originalTask = testCommitedTask().withData {
                    owner(99)
                    sharedTo(listOf(2))
                },
            )

            val result = developerOperations.detachTask(user, contestId, taskId).getOrThrow()

            Assertions.assertEquals(emptyList<TaskId>(), result.data.tasks.ids)
        }

        @ParameterizedTest
        @ValueSource(strings = ["New", "Committed", "Uncommitted"])
        fun `should keep the detached task unchanged in every state`(state: String) {
            val originalTask = taskInState(state)
            val originalData = originalTask.data
            prepare(originalContest = testContest { tasks(listOf(0)) }, originalTask = originalTask)

            developerOperations.detachTask(developer, contestId, taskId).getOrThrow()

            Assertions.assertSame(originalData, originalTask.data)
            Assertions.assertEquals(0L, originalTask.version?.value)
        }

        @Test
        fun `should return the actual updated repository object and its version`() {
            prepare(originalContest = testContest { tasks(listOf(0)) })
            val repositoryResult = testSavedContest(testContest())
            every { contestRepository.update(any<Contest>()) } returns repositoryResult

            val result = developerOperations.detachTask(developer, contestId, taskId).getOrThrow()

            Assertions.assertSame(repositoryResult, result)
            Assertions.assertEquals(1L, result.version?.value)
        }

        @Test
        fun `should propagate storage exceptions when loading the contest`() {
            val failure = IllegalStateException("Contest storage read failure")
            every { contestRepository.findById(contestId) } throws failure

            val thrown = Assertions.assertThrows(IllegalStateException::class.java) {
                developerOperations.detachTask(developer, contestId, taskId)
            }

            Assertions.assertSame(failure, thrown)
            assertNoContestUpdate()
        }

        @Test
        fun `should propagate storage exceptions when loading the task`() {
            prepare(originalContest = testContest { tasks(listOf(0)) })
            val failure = IllegalStateException("Task storage read failure")
            every { taskRepository.findById(taskId) } throws failure

            val thrown = Assertions.assertThrows(IllegalStateException::class.java) {
                developerOperations.detachTask(developer, contestId, taskId)
            }

            Assertions.assertSame(failure, thrown)
            assertNoContestUpdate()
        }

        @ParameterizedTest
        @ValueSource(strings = ["Storage update failure", "Optimistic lock conflict"])
        fun `should propagate update exceptions including optimistic lock conflicts`(message: String) {
            prepare(originalContest = testContest { tasks(listOf(0)) })
            val failure = IllegalStateException(message)
            every { contestRepository.update(any<Contest>()) } throws failure

            val thrown = Assertions.assertThrows(IllegalStateException::class.java) {
                developerOperations.detachTask(developer, contestId, taskId)
            }

            Assertions.assertSame(failure, thrown)
            verify(exactly = 1) { contestRepository.update(any<Contest>()) }
        }

        private fun prepare(originalContest: Contest = testContest(), originalTask: Task = testCommitedTask()) {
            every { contestRepository.findById(contestId) } returns originalContest
            every { taskRepository.findById(taskId) } returns originalTask
            every { contestRepository.update(any<Contest>()) } answers { testSavedContest(firstArg()) }
        }

        private fun taskInState(state: String): Task = when (state) {
            "New" -> testNewTask()
            "Committed" -> testCommitedTask()
            "Uncommitted" -> testUncommittedTask()
            else -> error("Unsupported detach-task test state: $state")
        }

        private fun assertNoContestUpdate() {
            verify(exactly = 0) { contestRepository.update(any<Contest>()) }
            verify(exactly = 0) { contestRepository.update(any<List<Contest>>()) }
        }
    }

    @Nested
    inner class DeleteContestTests {

        private val contestId = ContestId(19)

        @AfterEach
        fun verifyNoWrites() {
            verify(exactly = 0) { taskRepository.update(any<Task>()) }
            verify(exactly = 0) { taskRepository.update(any<List<Task>>()) }
            verify(exactly = 0) { taskRepository.save(any<TaskData>()) }
            verify(exactly = 0) { taskRepository.save(any<List<TaskData>>()) }
            verify(exactly = 0) { contestRepository.update(any<Contest>()) }
            verify(exactly = 0) { contestRepository.update(any<List<Contest>>()) }
            verify(exactly = 0) { contestRepository.save(any<ContestData>()) }
            verify(exactly = 0) { contestRepository.save(any<List<ContestData>>()) }
        }

        @Test
        fun `should raise MissedDeveloperRoleError without reading or deleting the contest if user is not a Developer`() {
            assertRaises(MissedDeveloperRoleError) {
                developerOperations.deleteContest(testAdministrator {}, contestId)
            }

            verify(exactly = 0) { contestRepository.findById(any()) }
            assertNoContestRemoval()
        }

        @Test
        fun `should raise ContestNotExistsError without deleting if contest is missing`() {
            every { contestRepository.findById(contestId) } returns null

            assertRaises(ContestNotExistsError(contestId)) {
                developerOperations.deleteContest(developer, contestId)
            }

            assertNoContestRemoval()
        }

        @ParameterizedTest
        @ValueSource(ints = [0, 1])
        fun `should raise ContestAccessDeniedError for another owners contest even when shared to Developer communities`(
            recipientCount: Int,
        ) {
            val user = testDeveloper {
                memberOf(listOf(1))
                data = developerData {}
            }
            val recipients = listOf(1L).take(recipientCount)
            prepare(
                originalContest = testContest {
                    owner(99)
                    sharedTo(recipients)
                },
            )

            assertRaises(ContestAccessDeniedError(contestId)) {
                developerOperations.deleteContest(user, contestId)
            }

            assertNoContestRemoval()
        }

        @ParameterizedTest
        @ValueSource(ints = [1, 2])
        fun `should raise ContestAlreadySharedError without deleting if contest is shared to communities`(recipientCount: Int) {
            val recipients = listOf(1L, 2L).take(recipientCount)
            prepare(originalContest = testContest { sharedTo(recipients) })

            assertRaises(ContestAlreadySharedError(contestId)) {
                developerOperations.deleteContest(developer, contestId)
            }

            assertNoContestRemoval()
        }

        @Test
        fun `should delete an unshared own contest by its id and return it as it was before deletion`() {
            val original = testContest {
                tasks(listOf(7))
                name = "Original contest"
                description = "Original description"
                startsAt = Instant.parse("2020-01-01T10:00:00Z")
                contestDuration = Duration.ofHours(2)
                attemptDuration = Duration.ofMinutes(30)
            }
            prepare(originalContest = original)

            val result = developerOperations.deleteContest(developer, contestId).getOrThrow()

            Assertions.assertSame(original, result)
            Assertions.assertEquals(contestId, result.id)
            Assertions.assertEquals(0L, result.version?.value)
            Assertions.assertEquals("Original contest", result.data.name)
            Assertions.assertEquals(listOf(TaskId(7)), result.data.tasks.ids)
            verify(exactly = 1) { contestRepository.removeById(contestId) }
            verify(exactly = 1) { contestRepository.removeById(any()) }
        }

        @ParameterizedTest
        @ValueSource(ints = [0, 2])
        fun `should delete the contest without accessing tasks with any number of attached tasks`(attachedTaskCount: Int) {
            val tasks = listOf(7L, 8L).take(attachedTaskCount)
            prepare(originalContest = testContest { tasks(tasks) })

            val result = developerOperations.deleteContest(developer, contestId).getOrThrow()

            Assertions.assertEquals(tasks.map(::TaskId), result.data.tasks.ids)
            verify(exactly = 1) { contestRepository.removeById(contestId) }
            verify(exactly = 0) { taskRepository.findById(any()) }
        }

        @Test
        fun `should delete a contest without schedule and limits`() {
            prepare(originalContest = testContest())

            val result = developerOperations.deleteContest(developer, contestId).getOrThrow()

            Assertions.assertNull(result.data.startsAt)
            Assertions.assertNull(result.data.contestDuration)
            Assertions.assertNull(result.data.attemptDuration)
            verify(exactly = 1) { contestRepository.removeById(contestId) }
        }

        @ParameterizedTest
        @ValueSource(strings = ["New", "Committed", "Uncommitted"])
        fun `should keep an attached task unchanged in every state`(state: String) {
            val attachedTask = taskInState(state)
            val attachedData = attachedTask.data
            prepare(originalContest = testContest { tasks(listOf(attachedTask.id.value)) })

            developerOperations.deleteContest(developer, contestId).getOrThrow()

            Assertions.assertSame(attachedData, attachedTask.data)
            Assertions.assertEquals(0L, attachedTask.version?.value)
            verify(exactly = 0) { taskRepository.findById(any()) }
        }

        @Test
        fun `should propagate storage exceptions when loading the contest`() {
            val failure = IllegalStateException("Contest storage read failure")
            every { contestRepository.findById(contestId) } throws failure

            val thrown = Assertions.assertThrows(IllegalStateException::class.java) {
                developerOperations.deleteContest(developer, contestId)
            }

            Assertions.assertSame(failure, thrown)
            assertNoContestRemoval()
        }

        @Test
        fun `should propagate storage exceptions when removing the contest`() {
            prepare(originalContest = testContest())
            val failure = IllegalStateException("Contest storage remove failure")
            every { contestRepository.removeById(contestId) } throws failure

            val thrown = Assertions.assertThrows(IllegalStateException::class.java) {
                developerOperations.deleteContest(developer, contestId)
            }

            Assertions.assertSame(failure, thrown)
            verify(exactly = 1) { contestRepository.removeById(contestId) }
        }

        private fun prepare(originalContest: Contest) {
            every { contestRepository.findById(contestId) } returns originalContest
            every { contestRepository.removeById(contestId) } just Runs
        }

        private fun assertNoContestRemoval() {
            verify(exactly = 0) { contestRepository.removeById(any()) }
            verify(exactly = 0) { contestRepository.removeByIds(any()) }
            verify(exactly = 0) { contestRepository.remove(any<Contest>()) }
            verify(exactly = 0) { contestRepository.remove(any<List<Contest>>()) }
        }
    }

    @Nested
    inner class ViewTasksTests {

        private val pagination = Pagination(page = 0, size = 10)

        @Test
        fun `should raise MissedDeveloperRoleError if user is not a Developer`() {
            assertRaises(MissedDeveloperRoleError) { developerOperations.viewTasks(testAdministrator {}, pagination = pagination) }

            verify(exactly = 0) {
                taskRepository.findAvailableToDeveloper(any(), any(), any(), any())
            }
        }

        @Test
        fun `should return an empty page when no tasks are available`() {
            every {
                taskRepository.findAvailableToDeveloper(ownerId = developer.id, communityIds = emptySet(), pagination = pagination)
            } returns Page(content = emptyList(), pagination = pagination, totalElements = 0)

            val result = developerOperations.viewTasks(developer, pagination = pagination).getOrThrow().content

            Assertions.assertEquals(emptyList<Task>(), result)
        }

        @Test
        fun `should return owned and shared tasks with their identifiers names owners and states`() {
            val user = testDeveloper {
                memberOf(listOf(1, 2))
                data = developerData {}
            }
            val owned = task {
                id = 1
                createdAt = Instant.MIN
                data = testNewTask().data
            }
            val shared = task {
                id = 2
                createdAt = Instant.MIN
                data = testCommitedTask().withData {
                    owner = MultipleRoleUserId(99)
                    name = "Shared task"
                    sharedTo(listOf(2))
                }.data
            }
            every {
                taskRepository.findAvailableToDeveloper(
                    ownerId = user.id,
                    communityIds = setOf(CommunityId(1), CommunityId(2)),
                    pagination = pagination,
                )
            } returns Page(content = listOf(owned, shared), pagination = pagination, totalElements = 2)

            val result = developerOperations.viewTasks(user, pagination = pagination).getOrThrow().content

            Assertions.assertEquals(listOf(TaskId(1), TaskId(2)), result.map { it.id })
            Assertions.assertEquals(listOf("name", "Shared task"), result.map { it.data.name })
            Assertions.assertEquals(listOf(user.id, MultipleRoleUserId(99)), result.map { it.data.owner.id })
            Assertions.assertInstanceOf(TaskContent.New::class.java, result[0].data.content)
            Assertions.assertInstanceOf(TaskContent.Committed::class.java, result[1].data.content)
            verify(exactly = 0) { taskRepository.findByIds(any()) }
        }

        @Test
        fun `should consider community membership only in the Developer role when user has several roles`() {
            val user = testMultipleRoleUser {
                roles {
                    developer {
                        memberOf(listOf(1))
                        data = developerData {}
                    }
                    student {
                        memberOf(listOf(2))
                        data = studentData {}
                    }
                    administrator { memberOf(listOf(3)) }
                }
            }
            every {
                taskRepository.findAvailableToDeveloper(ownerId = user.id, communityIds = setOf(CommunityId(1)), pagination = pagination)
            } returns Page(content = emptyList(), pagination = pagination, totalElements = 0)

            developerOperations.viewTasks(user, pagination = pagination).getOrThrow().content

            verify(exactly = 1) {
                taskRepository.findAvailableToDeveloper(ownerId = user.id, communityIds = setOf(CommunityId(1)), pagination = pagination)
            }
        }

        @ParameterizedTest
        @ValueSource(strings = ["New", "Uncommitted", "Committed"])
        fun `should retain task state and data when viewing tasks`(state: String) {
            val original = taskInState(state)
            every {
                taskRepository.findAvailableToDeveloper(ownerId = developer.id, communityIds = emptySet(), pagination = pagination)
            } returns Page(content = listOf(original), pagination = pagination, totalElements = 1)

            val result = developerOperations.viewTasks(developer, pagination = pagination).getOrThrow().content.single()

            Assertions.assertSame(original, result)
            Assertions.assertEquals(original.data, result.data)
            Assertions.assertEquals(original.version, result.version)
            verify(exactly = 0) { taskRepository.update(any<Task>()) }
            verify(exactly = 0) { taskRepository.save(any<TaskData>()) }
        }

        @Test
        fun `should propagate a storage exception when listing tasks`() {
            val failure = IllegalStateException("Task storage unavailable")
            every {
                taskRepository.findAvailableToDeveloper(ownerId = developer.id, communityIds = emptySet(), pagination = pagination)
            } throws failure

            val actual = Assertions.assertThrows(IllegalStateException::class.java) {
                developerOperations.viewTasks(developer, pagination = pagination)
            }

            Assertions.assertSame(failure, actual)
        }

        @Test
        fun `should forward pagination and all filters unchanged`() {
            val request = Pagination(page = 3, size = 2, sort = Sort(listOf(Sort.Order("name", Sort.Direction.DESC))))
            val filter =
                TaskFilter(
                    name = " %_ ",
                    ownerId = MultipleRoleUserId(99),
                    state = TaskFilter.State.UNCOMMITTED,
                    communityId = CommunityId(100),
                )
            val page = Page<Task>(content = emptyList(), pagination = request, totalElements = 5)
            every {
                taskRepository.findAvailableToDeveloper(
                    ownerId = developer.id,
                    communityIds = emptySet(),
                    pagination = refEq(request),
                    filter = refEq(filter),
                )
            } returns page

            val result = developerOperations.viewTasks(user = developer, pagination = request, filter = filter).getOrThrow()

            Assertions.assertSame(page, result)
            Assertions.assertSame(request, result.pagination)
            Assertions.assertEquals(5L, result.totalElements)
        }
    }

    @Nested
    inner class EditTaskInfoTests {

        private val taskId = TaskId(42)

        @Test
        fun `should raise MissedDeveloperRoleError if user is not a Developer`() {
            assertRaises(MissedDeveloperRoleError) { developerOperations.editTaskInfo(testAdministrator {}, taskId, taskName = "New") }

            verify(exactly = 0) { taskRepository.findById(any()) }
            verify(exactly = 0) { taskRepository.update(any<Task>()) }
        }

        @Test
        fun `should raise TaskNotExistsError if task does not exist`() {
            every { taskRepository.findById(taskId) } returns null

            assertRaises(TaskNotExistsError(taskId)) { developerOperations.editTaskInfo(developer, taskId) }

            verify(exactly = 0) { taskRepository.update(any<Task>()) }
        }

        @Test
        fun `should deny editing another owner's task even when shared to the Developer community`() {
            val user = testDeveloper {
                memberOf(listOf(4))
                data = developerData {}
            }
            val original = taskInState("Committed").withData { owner = MultipleRoleUserId(99) }
            every { taskRepository.findById(taskId) } returns original

            assertRaises(TaskAccessDeniedError(taskId)) { developerOperations.editTaskInfo(user, taskId, taskName = "New") }

            verify(exactly = 0) { taskRepository.update(any<Task>()) }
        }

        @ParameterizedTest
        @ValueSource(strings = ["New", "Uncommitted", "Committed"])
        fun `should edit only name without changing content or state`(state: String) {
            val original = taskInState(state)
            every { taskRepository.findById(taskId) } returns original
            every { taskRepository.update(any<Task>()) } answers { testSavedTask(firstArg()) }

            val result = developerOperations.editTaskInfo(developer, taskId, taskName = "Updated name").getOrThrow()

            Assertions.assertEquals("Updated name", result.data.name)
            Assertions.assertEquals(original.data.description, result.data.description)
            assertMetadataEditPreservesContent(original, result)
            verify(exactly = 1) { taskRepository.update(any<Task>()) }
        }

        @ParameterizedTest
        @CsvSource("New, Updated description", "Uncommitted, Updated description", "Committed, Updated description", "Committed, ''")
        fun `should edit only description including clearing it without changing content or state`(state: String, description: String) {
            val original = taskInState(state)
            every { taskRepository.findById(taskId) } returns original
            every { taskRepository.update(any<Task>()) } answers { testSavedTask(firstArg()) }

            val result = developerOperations.editTaskInfo(developer, taskId, taskDescription = description).getOrThrow()

            Assertions.assertEquals(description, result.data.description)
            Assertions.assertEquals(original.data.name, result.data.name)
            assertMetadataEditPreservesContent(original, result)
            verify(exactly = 1) { taskRepository.update(any<Task>()) }
        }

        @ParameterizedTest
        @CsvSource(
            "New, '3.0.0 4.0.0 4.0.0', '3.0.0 4.0.0'",
            "Uncommitted, '3.0.0 4.0.0 4.0.0', '3.0.0 4.0.0'",
            "Committed, '3.0.0 4.0.0 4.0.0', '3.0.0 4.0.0'",
            "New, '4.0.0', '4.0.0'",
            "Uncommitted, '4.0.0', '4.0.0'",
            "Committed, '4.0.0', '4.0.0'",
            "New, '', ''",
            "Uncommitted, '', ''",
            "Committed, '', ''",
        )
        fun `should replace editable versions and retain resources and committed content when the version set changes`(
            state: String,
            supplied: String,
            expected: String,
        ) {
            val original = taskInState(state)
            val saved = slot<Task>()
            every { taskRepository.findById(taskId) } returns original
            every { taskRepository.update(capture(saved)) } answers { testSavedTask(firstArg()) }

            val result = developerOperations.editTaskInfo(
                user = developer,
                taskId = taskId,
                supportedTrikStudioVersions = versions(supplied),
            ).getOrThrow()

            val expectedContent = taskContentNew {
                existingResources()
                supportedTrikStudioVersions = versions(expected).toMutableList()
                exercises = original.getEditableContent().exercises.ids.toMutableList()
                statement = original.getEditableContent().statement?.id
            }.wip
            assertDetachedTask(original = original, result = result, expected = expectedContent)
            Assertions.assertEquals(original.version, saved.captured.version)
            verify(exactly = 1) { taskRepository.update(any<Task>()) }
        }

        @ParameterizedTest
        @ValueSource(strings = ["New", "Uncommitted", "Committed"])
        fun `should remove one supported version while retaining another in every state`(state: String) {
            val original = taskForDetach(state)
            every { taskRepository.findById(taskId) } returns original
            every { taskRepository.update(any<Task>()) } answers { testSavedTask(firstArg()) }

            val result = developerOperations.editTaskInfo(developer, taskId, supportedTrikStudioVersions = versions("3.0.0")).getOrThrow()

            val expected = taskContentNew {
                detachResources()
                supportedTrikStudioVersions(listOf("3.0.0"))
            }.wip
            assertDetachedTask(original = original, result = result, expected = expected)
            verify(exactly = 1) { taskRepository.update(any<Task>()) }
        }

        @Test
        fun `should edit name description and versions together and return the saved task`() {
            val original = taskInState("Committed")
            val saved = slot<Task>()
            every { taskRepository.findById(taskId) } returns original
            every { taskRepository.update(capture(saved)) } answers { testSavedTask(firstArg()) }

            val result = developerOperations.editTaskInfo(
                user = developer,
                taskId = taskId,
                taskName = "Updated name",
                taskDescription = "Updated description",
                supportedTrikStudioVersions = versions("4.0.0"),
            ).getOrThrow()

            Assertions.assertEquals("Updated name", result.data.name)
            Assertions.assertEquals("Updated description", result.data.description)
            Assertions.assertEquals(versions("4.0.0"), result.getEditableContent().supportedTrikStudioVersions)
            Assertions.assertEquals(saved.captured.data, result.data)
            Assertions.assertEquals(savedTaskVersion, result.version)
            Assertions.assertEquals(original.version, saved.captured.version)
            assertCommittedUnchanged(
                expected = Assertions.assertInstanceOf(TaskContent.Committed::class.java, original.data.content).lastCommitted,
                result = result,
            )
            verify(exactly = 1) { taskRepository.update(any<Task>()) }
        }

        @ParameterizedTest
        @ValueSource(strings = ["New", "Uncommitted", "Committed"])
        fun `should return original task without saving when no values are supplied`(state: String) {
            val original = taskInState(state)
            every { taskRepository.findById(taskId) } returns original

            val result = developerOperations.editTaskInfo(developer, taskId).getOrThrow()

            Assertions.assertSame(original, result)
            verify(exactly = 0) { taskRepository.update(any<Task>()) }
        }

        @ParameterizedTest
        @ValueSource(strings = ["New", "Uncommitted", "Committed"])
        fun `should ignore reordering and duplicates when the version set and metadata are unchanged`(state: String) {
            val original = taskForDetach(state)
            every { taskRepository.findById(taskId) } returns original

            val result = developerOperations.editTaskInfo(
                user = developer,
                taskId = taskId,
                taskName = original.data.name,
                taskDescription = original.data.description,
                supportedTrikStudioVersions = versions("4.0.0 3.0.0 4.0.0"),
            ).getOrThrow()

            Assertions.assertSame(original, result)
            verify(exactly = 0) { taskRepository.update(any<Task>()) }
        }

        @Test
        fun `should compare supplied versions to WIP rather than committed versions`() {
            val original = taskInState("Uncommitted").withData {
                content.uncommitted(wipBuilder = { supportedTrikStudioVersions(listOf("4.0.0")) }, lastCommittedBuilder = {})
            }
            every { taskRepository.findById(taskId) } returns original
            every { taskRepository.update(any<Task>()) } answers { testSavedTask(firstArg()) }

            val result = developerOperations.editTaskInfo(developer, taskId, supportedTrikStudioVersions = versions("3.0.0")).getOrThrow()

            Assertions.assertEquals(versions("3.0.0"), result.getEditableContent().supportedTrikStudioVersions)
            assertCommittedUnchanged(
                expected = Assertions.assertInstanceOf(TaskContent.Uncommitted::class.java, original.data.content).lastCommitted,
                result = result,
            )
            verify(exactly = 1) { taskRepository.update(any<Task>()) }
        }

        @Test
        fun `should propagate storage exceptions when loading a task`() {
            val failure = IllegalStateException("Task storage unavailable")
            every { taskRepository.findById(taskId) } throws failure

            val actual = Assertions.assertThrows(IllegalStateException::class.java) { developerOperations.editTaskInfo(developer, taskId) }

            Assertions.assertSame(failure, actual)
        }

        @Test
        fun `should propagate storage exceptions when saving edited information`() {
            val original = taskInState("Committed")
            val failure = IllegalStateException("Task update failed")
            every { taskRepository.findById(taskId) } returns original
            every { taskRepository.update(any<Task>()) } throws failure

            val actual = Assertions.assertThrows(IllegalStateException::class.java) {
                developerOperations.editTaskInfo(developer, taskId, taskName = "Updated name")
            }

            Assertions.assertSame(failure, actual)
            Assertions.assertEquals("name", original.data.name)
        }

        private fun assertMetadataEditPreservesContent(original: Task, result: Task) {
            Assertions.assertEquals(original.data.content.javaClass, result.data.content.javaClass)
            assertEditableContentEquals(original.getEditableContent(), result.getEditableContent())
            Assertions.assertEquals(original.id, result.id)
            Assertions.assertEquals(original.createdAt, result.createdAt)
            Assertions.assertEquals(original.data.owner.id, result.data.owner.id)
            Assertions.assertEquals(original.data.sharedTo.ids, result.data.sharedTo.ids)
            Assertions.assertEquals(original.data.uploadedResources, result.data.uploadedResources)
            Assertions.assertEquals(savedTaskVersion, result.version)
            when (val content = original.data.content) {
                is TaskContent.New -> Unit
                is TaskContent.Uncommitted -> assertCommittedUnchanged(content.lastCommitted, result)
                is TaskContent.Committed -> {
                    val actual = Assertions.assertInstanceOf(TaskContent.Committed::class.java, result.data.content).lastCommitted
                    Assertions.assertEquals(content.lastCommitted.tests.ids, actual.tests.ids)
                    Assertions.assertEquals(content.lastCommitted.exercises.ids, actual.exercises.ids)
                    Assertions.assertEquals(content.lastCommitted.statement.id, actual.statement.id)
                    Assertions.assertEquals(content.lastCommitted.developerSolutions.ids, actual.developerSolutions.ids)
                    Assertions.assertEquals(content.lastCommitted.supportedTrikStudioVersions, actual.supportedTrikStudioVersions)
                }
            }
        }

        private fun versions(tags: String): List<TrikStudioVersion> =
            tags.split(' ').filter { it.isNotEmpty() }.map { TrikStudioVersion(it) }
    }

    @Nested
    inner class ViewTaskTests {

        private val taskId = TaskId(42)

        @Test
        fun `should raise MissedDeveloperRoleError if user is not a Developer`() {
            assertRaises(MissedDeveloperRoleError) { developerOperations.viewTask(testAdministrator {}, taskId) }

            verify(exactly = 0) { taskRepository.findById(any()) }
        }

        @Test
        fun `should raise TaskNotExistsError if task does not exist`() {
            every { taskRepository.findById(taskId) } returns null

            assertRaises(TaskNotExistsError(taskId)) { developerOperations.viewTask(developer, taskId) }
        }

        @Test
        fun `should raise TaskAccessDeniedError if task belongs to another user`() {
            val otherTask = testNewTask().withData { owner = MultipleRoleUserId(99) }
            every { taskRepository.findById(taskId) } returns otherTask

            assertRaises(TaskAccessDeniedError(taskId)) { developerOperations.viewTask(developer, taskId) }
        }

        @Test
        fun `should deny viewing another owner's task shared to the Developer community`() {
            val user = testDeveloper {
                memberOf(listOf(4))
                data = developerData {}
            }
            val sharedTask = testCommitedTask().withData {
                owner = MultipleRoleUserId(99)
                sharedTo(listOf(4))
            }
            every { taskRepository.findById(taskId) } returns sharedTask

            assertRaises(TaskAccessDeniedError(taskId)) { developerOperations.viewTask(user, taskId) }
        }

        @ParameterizedTest
        @ValueSource(strings = ["New", "Uncommitted", "Committed"])
        fun `should return owned task data and retain revisions and state when viewing a task`(state: String) {
            val original = task {
                id = taskId.value
                createdAt = Instant.MIN
                version = EntityVersion(7)
                data = taskInState(state).withData {
                    name = "Viewed task"
                    description = "Task details"
                }.data
            }
            every { taskRepository.findById(taskId) } returns original

            val result = developerOperations.viewTask(developer, taskId).getOrThrow()

            Assertions.assertSame(original, result)
            Assertions.assertEquals(taskId, result.id)
            Assertions.assertEquals("Viewed task", result.data.name)
            Assertions.assertEquals("Task details", result.data.description)
            Assertions.assertSame(original.data.content, result.data.content)
            Assertions.assertEquals(EntityVersion(7), result.version)
            verify(exactly = 0) { taskRepository.update(any<Task>()) }
            verify(exactly = 0) { taskRepository.save(any<TaskData>()) }
        }

        @Test
        fun `should return owned task even when it is absent from the Developer task list`() {
            val ownedTask = testNewTask()
            every { taskRepository.findById(taskId) } returns ownedTask

            val result = developerOperations.viewTask(developer, taskId).getOrThrow()

            Assertions.assertSame(ownedTask, result)
        }

        @Test
        fun `should propagate a storage exception when viewing a task`() {
            val failure = IllegalStateException("Task storage unavailable")
            every { taskRepository.findById(taskId) } throws failure

            val actual = Assertions.assertThrows(IllegalStateException::class.java) { developerOperations.viewTask(developer, taskId) }

            Assertions.assertSame(failure, actual)
        }
    }

    @Nested
    inner class ViewResourcesTests {

        @Test
        fun `should raise MissedDeveloperRoleError if user is not a Developer`() {
            assertRaises(MissedDeveloperRoleError) { developerOperations.viewResources(testAdministrator {}) }
        }

        @Test
        fun `should return an empty list when developer has no tasks`() {
            every { taskRepository.findByIds(emptyList()) } returns emptyList()

            val result = developerOperations.viewResources(developer).getOrThrow()

            Assertions.assertEquals(emptyList<DomainEntity<*>>(), result)
        }

        @Test
        fun `should return latest existing entities of all four resource types from owned tasks`() {
            val statement = viewStatement(id = 1, createdAt = Instant.ofEpochSecond(10))
            val exercise = viewExercise()
            val polygon = viewPolygon()
            val solution = viewDeveloperSolution()
            val user = testDeveloper { data = developerData { tasks(listOf(0, 1)) } }
            val owned = testNewTask().withData {
                uploadedResources = mutableSetOf(
                    statement.data.versionBucket,
                    exercise.data.versionBucket,
                    polygon.data.versionBucket,
                    solution.data.versionBucket,
                )
            }
            val foreign = testNewTask().withData {
                owner = MultipleRoleUserId(99)
                uploadedResources = mutableSetOf(VersionBucket(UUID(0, 99)))
            }
            every { taskRepository.findByIds(listOf(TaskId(0), TaskId(1))) } returns listOf(owned, foreign)
            every { statementRepository.findLatestByVersionBucket(any()) } returns null
            every { exerciseRepository.findLatestByVersionBucket(any()) } returns null
            every { testRepository.findLatestByVersionBucket(any()) } returns null
            every { developerSolutionRepository.findLatestByVersionBucket(any()) } returns null
            every { statementRepository.findLatestByVersionBucket(statement.data.versionBucket) } returns statement
            every { exerciseRepository.findLatestByVersionBucket(exercise.data.versionBucket) } returns exercise
            every { testRepository.findLatestByVersionBucket(polygon.data.versionBucket) } returns polygon
            every { developerSolutionRepository.findLatestByVersionBucket(solution.data.versionBucket) } returns solution

            val result = developerOperations.viewResources(user).getOrThrow()

            Assertions.assertEquals(setOf(solution, polygon, exercise, statement), result.toSet())
            val actualStatement = result.single { it is Statement }
            Assertions.assertSame(statement, actualStatement)
            Assertions.assertEquals(Instant.ofEpochSecond(10), actualStatement.createdAt)
            verify(exactly = 0) { statementRepository.findLatestByVersionBucket(VersionBucket(UUID(0, 99))) }
            verify(exactly = 0) { taskRepository.update(any<Task>()) }
        }

        @Test
        fun `should omit an uploaded chain with no existing versions`() {
            val user = testDeveloper { data = developerData { tasks(listOf(0)) } }
            every { taskRepository.findByIds(listOf(TaskId(0))) } returns listOf(testNewTask())
            every { statementRepository.findLatestByVersionBucket(any()) } returns null
            every { exerciseRepository.findLatestByVersionBucket(any()) } returns null
            every { testRepository.findLatestByVersionBucket(any()) } returns null
            every { developerSolutionRepository.findLatestByVersionBucket(any()) } returns null

            val result = developerOperations.viewResources(user).getOrThrow()

            Assertions.assertEquals(emptyList<DomainEntity<*>>(), result)
        }
    }

    @Nested
    inner class ViewResourceTests {

        private val bucket = VersionBucket(UUID(0, 0))
        private val taskId = TaskId(0)

        @BeforeEach
        fun setUp() {
            every { statementRepository.existsByVersionBucket(any()) } returns false
            every { exerciseRepository.existsByVersionBucket(any()) } returns false
            every { testRepository.existsByVersionBucket(any()) } returns false
            every { developerSolutionRepository.existsByVersionBucket(any()) } returns false
            every { statementRepository.findVersionsByVersionBucket(any()) } returns emptyList()
            every { exerciseRepository.findVersionsByVersionBucket(any()) } returns emptyList()
            every { testRepository.findVersionsByVersionBucket(any()) } returns emptyList()
            every { developerSolutionRepository.findVersionsByVersionBucket(any()) } returns emptyList()
        }

        @Test
        fun `should raise MissedDeveloperRoleError if user is not a Developer`() {
            assertRaises(MissedDeveloperRoleError) {
                developerOperations.viewResource(testAdministrator {}, taskId, bucket)
            }
        }

        @Test
        fun `should raise TaskNotExistsError if task does not exist`() {
            every { taskRepository.findById(taskId) } returns null

            assertRaises(TaskNotExistsError(taskId)) { developerOperations.viewResource(developer, taskId, bucket) }
        }

        @Test
        fun `should raise ResourceNotExistsError if chain does not exist`() {
            every { taskRepository.findById(taskId) } returns testNewTask()

            assertRaises(ResourceNotExistsError(bucket)) { developerOperations.viewResource(developer, taskId, bucket) }
        }

        @Test
        fun `should raise TaskAccessDeniedError if task belongs to another user`() {
            every { taskRepository.findById(taskId) } returns testNewTask().withData { owner = MultipleRoleUserId(99) }
            every { statementRepository.existsByVersionBucket(bucket) } returns true

            assertRaises(TaskAccessDeniedError(taskId)) { developerOperations.viewResource(developer, taskId, bucket) }
        }

        @Test
        fun `should raise ResourceNotUploadedToTaskError if chain is outside the task`() {
            every { taskRepository.findById(taskId) } returns testNewTask().withData { uploadedResources = mutableSetOf() }
            every { statementRepository.existsByVersionBucket(bucket) } returns true

            assertRaises(ResourceNotUploadedToTaskError(taskId, bucket)) {
                developerOperations.viewResource(developer, taskId, bucket)
            }
        }

        @ParameterizedTest
        @ValueSource(strings = ["New", "Uncommitted", "Committed"])
        fun `should return the existing history without changing task state`(state: String) {
            val old = viewStatement(id = 1, createdAt = Instant.ofEpochSecond(10))
            val latest = viewStatement(id = 2, createdAt = Instant.ofEpochSecond(20))
            every { taskRepository.findById(taskId) } returns taskInState(state)
            every { statementRepository.existsByVersionBucket(bucket) } returns true
            every { statementRepository.findVersionsByVersionBucket(bucket) } returns listOf(old, latest)

            val result = developerOperations.viewResource(developer, taskId, bucket).getOrThrow()

            Assertions.assertEquals(setOf(latest, old), result.toSet())
            Assertions.assertSame(latest, result.single { it.id == latest.id })
            Assertions.assertEquals("file.pdf", latest.data.file.uploadedFilename)
            verify(exactly = 0) { taskRepository.update(any<Task>()) }
        }

        @Test
        fun `should return an existing exercise entity`() {
            val version = viewExercise()
            val exerciseBucket = version.data.versionBucket
            every { taskRepository.findById(taskId) } returns testNewTask().withData { uploadedResources = mutableSetOf(exerciseBucket) }
            every { exerciseRepository.existsByVersionBucket(exerciseBucket) } returns true
            every { exerciseRepository.findVersionsByVersionBucket(exerciseBucket) } returns listOf(version)

            val result = developerOperations.viewResource(developer, taskId, exerciseBucket).getOrThrow()

            Assertions.assertSame(version, result.single())
        }

        @Test
        fun `should return an existing test entity`() {
            val version = viewPolygon()
            val testBucket = version.data.versionBucket
            every { taskRepository.findById(taskId) } returns testNewTask().withData { uploadedResources = mutableSetOf(testBucket) }
            every { testRepository.existsByVersionBucket(testBucket) } returns true
            every { testRepository.findVersionsByVersionBucket(testBucket) } returns listOf(version)

            val result = developerOperations.viewResource(developer, taskId, testBucket).getOrThrow()

            Assertions.assertSame(version, result.single())
        }

        @Test
        fun `should return an existing developer solution entity`() {
            val version = viewDeveloperSolution()
            val solutionBucket = version.data.versionBucket
            every { taskRepository.findById(taskId) } returns testNewTask().withData { uploadedResources = mutableSetOf(solutionBucket) }
            every { developerSolutionRepository.existsByVersionBucket(solutionBucket) } returns true
            every { developerSolutionRepository.findVersionsByVersionBucket(solutionBucket) } returns listOf(version)

            val result = developerOperations.viewResource(developer, taskId, solutionBucket).getOrThrow()

            Assertions.assertSame(version, result.single())
        }
    }

    @Nested
    inner class DownloadResourceVersionTests {

        private val versionId = StatementId(1)
        private val bucket = VersionBucket(UUID(0, 0))
        private val taskId = TaskId(0)

        @BeforeEach
        fun setUp() {
            every { statementRepository.existsByVersionBucket(bucket) } returns false
            every { exerciseRepository.existsByVersionBucket(bucket) } returns false
            every { testRepository.existsByVersionBucket(bucket) } returns false
            every { developerSolutionRepository.existsByVersionBucket(bucket) } returns false
        }

        @Test
        fun `should raise MissedDeveloperRoleError if user is not a Developer`() {
            val user = testAdministrator {}

            assertRaises(MissedDeveloperRoleError) {
                developerOperations.downloadResourceVersion(user, taskId, bucket, versionId)
            }
        }

        @Test
        fun `should raise TaskNotExistsError if task does not exist`() {
            every { taskRepository.findById(taskId) } returns null

            assertRaises(TaskNotExistsError(taskId)) {
                developerOperations.downloadResourceVersion(developer, taskId, bucket, versionId)
            }
        }

        @Test
        fun `should raise ResourceNotExistsError if chain does not exist`() {
            every { taskRepository.findById(taskId) } returns testNewTask()
            every { statementRepository.existsByVersionBucket(bucket) } returns false

            assertRaises(ResourceNotExistsError(bucket)) {
                developerOperations.downloadResourceVersion(developer, taskId, bucket, versionId)
            }
        }

        @Test
        fun `should raise TaskAccessDeniedError if task belongs to another user`() {
            every { taskRepository.findById(taskId) } returns testNewTask().withData { owner = MultipleRoleUserId(99) }
            every { statementRepository.existsByVersionBucket(bucket) } returns true

            assertRaises(TaskAccessDeniedError(taskId)) {
                developerOperations.downloadResourceVersion(developer, taskId, bucket, versionId)
            }
        }

        @Test
        fun `should raise ResourceNotUploadedToTaskError if chain is outside the task`() {
            every { taskRepository.findById(taskId) } returns testNewTask().withData { uploadedResources = mutableSetOf() }
            every { statementRepository.existsByVersionBucket(bucket) } returns true

            assertRaises(ResourceNotUploadedToTaskError(taskId, bucket)) {
                developerOperations.downloadResourceVersion(developer, taskId, bucket, versionId)
            }
        }

        @Test
        fun `should raise ResourceVersionNotExistsError if version is missing from the selected chain`() {
            every { taskRepository.findById(taskId) } returns testNewTask()
            every { statementRepository.existsByVersionBucket(bucket) } returns true
            every { statementRepository.findFileRef(bucket, versionId) } returns null

            assertRaises(ResourceVersionNotExistsError(bucket, versionId)) {
                developerOperations.downloadResourceVersion(developer, taskId, bucket, versionId)
            }
        }

        @Test
        fun `should return the existing exercise file reference`() {
            val exerciseId = ExerciseId(2)
            every { taskRepository.findById(taskId) } returns testNewTask()
            every { exerciseRepository.existsByVersionBucket(bucket) } returns true
            every { exerciseRepository.findFileRef(bucket, exerciseId) } returns StoredBlobRef("exercise-file")

            val result = developerOperations.downloadResourceVersion(developer, taskId, bucket, exerciseId).getOrThrow()

            Assertions.assertEquals(StoredBlobRef("exercise-file"), result)
        }

        @Test
        fun `should return the existing test file reference`() {
            val testId = TestId(3)
            every { taskRepository.findById(taskId) } returns testNewTask()
            every { testRepository.existsByVersionBucket(bucket) } returns true
            every { testRepository.findFileRef(bucket, testId) } returns StoredBlobRef("test-file")

            val result = developerOperations.downloadResourceVersion(developer, taskId, bucket, testId).getOrThrow()

            Assertions.assertEquals(StoredBlobRef("test-file"), result)
        }

        @Test
        fun `should return the existing developer solution file reference`() {
            val solutionId = DeveloperSolutionId(4)
            every { taskRepository.findById(taskId) } returns testNewTask()
            every { developerSolutionRepository.existsByVersionBucket(bucket) } returns true
            every { developerSolutionRepository.findFileRef(bucket, solutionId) } returns StoredBlobRef("solution-file")

            val result = developerOperations.downloadResourceVersion(developer, taskId, bucket, solutionId).getOrThrow()

            Assertions.assertEquals(StoredBlobRef("solution-file"), result)
        }

        @Test
        fun `should reject an id of an unrelated entity kind`() {
            val solutionId = SolutionId(4)
            every { taskRepository.findById(taskId) } returns testNewTask()
            every { statementRepository.existsByVersionBucket(bucket) } returns true

            assertRaises(ResourceVersionNotExistsError(bucket, solutionId)) {
                developerOperations.downloadResourceVersion(developer, taskId, bucket, solutionId)
            }
        }

        @ParameterizedTest
        @ValueSource(strings = ["New", "Uncommitted", "Committed"])
        fun `should return existing file reference without changing task state or requiring latest version`(state: String) {
            val task = taskInState(state)
            every { taskRepository.findById(taskId) } returns task
            every { statementRepository.existsByVersionBucket(bucket) } returns true
            every { statementRepository.findFileRef(bucket, versionId) } returns StoredBlobRef("old-version-file")

            val result = developerOperations.downloadResourceVersion(developer, taskId, bucket, versionId).getOrThrow()

            Assertions.assertEquals(StoredBlobRef("old-version-file"), result)
            verify(exactly = 0) { taskRepository.update(any<Task>()) }
        }
    }

    @BeforeEach
    fun beforeEach() {
        developer = testDeveloper { data = developerData { } }
    }

    private fun taskInState(state: String): Task {
        val original = when (state) {
            "New" -> testNewTask()
            "Uncommitted" -> testUncommittedTask()
            "Committed" -> testCommitedTask()
            else -> error("Unsupported test state: $state")
        }
        return original.withData {
            sharedTo(listOf(4))
            when (original.data.content) {
                is TaskContent.New -> content.new { existingResources() }
                is TaskContent.Uncommitted -> content.uncommitted(
                    wipBuilder = { existingResources() },
                    lastCommittedBuilder = { existingResources() },
                )
                is TaskContent.Committed -> content.committed { existingResources() }
            }
        }
    }

    private fun viewStatement(id: Long, createdAt: Instant): Statement = statement {
        this.id = id
        this.createdAt = createdAt
        data = testStatement().data
    }

    private fun viewExercise(): Exercise = exercise {
        id = 2
        createdAt = Instant.ofEpochSecond(20)
        data = exerciseData {
            name = "exercise"
            description = ""
            file("exercise.qrs", byteArrayOf(1))
            language.python()
            versionBucket = VersionBucket(UUID(0, 2))
        }
    }

    private fun viewPolygon(): Polygon = test {
        id = 3
        createdAt = Instant.ofEpochSecond(30)
        data = testData {
            name = "test"
            description = ""
            file("world.xml", byteArrayOf(2))
            versionBucket = VersionBucket(UUID(0, 3))
        }
    }

    private fun viewDeveloperSolution(): DeveloperSolution = developerSolution {
        id = 4
        createdAt = Instant.ofEpochSecond(40)
        data = developerSolutionData {
            name = "solution"
            description = ""
            solution(5)
            expectedScore(42)
            versionBucket = VersionBucket(UUID(0, 4))
        }
    }

    private fun tech.testsys.domain.builder.task.TaskContentBuilder<*>.existingResources() {
        tests(listOf(7))
        developerSolutions(listOf(8))
        supportedTrikStudioVersions(listOf("3.0.0"))
    }

    private fun assertCommittedUnchanged(expected: CommittedTaskContent, result: Task) {
        val content = Assertions.assertInstanceOf(TaskContent.Uncommitted::class.java, result.data.content)
        Assertions.assertEquals(expected.tests.ids, content.lastCommitted.tests.ids)
        Assertions.assertEquals(expected.exercises.ids, content.lastCommitted.exercises.ids)
        Assertions.assertEquals(expected.statement.id, content.lastCommitted.statement.id)
        Assertions.assertEquals(expected.developerSolutions.ids, content.lastCommitted.developerSolutions.ids)
        Assertions.assertEquals(expected.supportedTrikStudioVersions, content.lastCommitted.supportedTrikStudioVersions)
    }

    private fun prepareUpload(original: Task = testNewTask()) {
        every { taskRepository.findById(uploadTaskId) } returns original
        every { taskRepository.update(any<Task>()) } answers { testSavedTask(firstArg()) }
        every { statementRepository.save(any<StatementData>()) } answers {
            statement {
                id = 11
                createdAt = Instant.MIN
                version = EntityVersion(1)
                data = firstArg<StatementData>()
            }
        }
        every { exerciseRepository.save(any<ExerciseData>()) } answers {
            exercise {
                id = 12
                createdAt = Instant.MIN
                version = EntityVersion(1)
                data = firstArg<ExerciseData>()
            }
        }
        every { testRepository.save(any<TestData>()) } answers {
            test {
                id = 13
                createdAt = Instant.MIN
                version = EntityVersion(1)
                data = firstArg<TestData>()
            }
        }
        every { solutionRepository.save(any<SolutionData>()) } answers {
            solution {
                id = 14
                createdAt = Instant.MIN
                version = EntityVersion(1)
                data = firstArg<SolutionData>()
            }
        }
        every { developerSolutionRepository.save(any<DeveloperSolutionData>()) } answers {
            developerSolution {
                id = 15
                createdAt = Instant.MIN
                version = EntityVersion(1)
                data = firstArg<DeveloperSolutionData>()
            }
        }
    }

    private fun assertNoUploadWrites() {
        verify(exactly = 0) { statementRepository.save(any<StatementData>()) }
        verify(exactly = 0) { exerciseRepository.save(any<ExerciseData>()) }
        verify(exactly = 0) { testRepository.save(any<TestData>()) }
        verify(exactly = 0) { solutionRepository.save(any<SolutionData>()) }
        verify(exactly = 0) { developerSolutionRepository.save(any<DeveloperSolutionData>()) }
        verify(exactly = 0) { taskRepository.update(any<Task>()) }
    }

    private fun assertUploadedTask(original: Task, buckets: Set<VersionBucket> = setOf(uploadBucket)) {
        val saved = slot<Task>()
        verify(exactly = 1) { taskRepository.update(capture(saved)) }
        val updated = saved.captured
        Assertions.assertEquals(original.id, updated.id)
        Assertions.assertEquals(original.createdAt, updated.createdAt)
        Assertions.assertEquals(original.version, updated.version)
        Assertions.assertEquals(original.data.owner.id, updated.data.owner.id)
        Assertions.assertEquals(original.data.name, updated.data.name)
        Assertions.assertEquals(original.data.description, updated.data.description)
        Assertions.assertEquals(original.data.sharedTo.ids, updated.data.sharedTo.ids)
        Assertions.assertEquals(original.data.uploadedResources + buckets, updated.data.uploadedResources)
        Assertions.assertEquals(original.data.content::class, updated.data.content::class)
        when (val expected = original.data.content) {
            is TaskContent.New -> {
                val actual = Assertions.assertInstanceOf(TaskContent.New::class.java, updated.data.content)
                assertWipUnchanged(expected.wip, actual.wip)
            }
            is TaskContent.Uncommitted -> {
                val actual = Assertions.assertInstanceOf(TaskContent.Uncommitted::class.java, updated.data.content)
                assertWipUnchanged(expected.wip, actual.wip)
                assertCommittedRevisionUnchanged(expected.lastCommitted, actual.lastCommitted)
            }
            is TaskContent.Committed -> {
                val actual = Assertions.assertInstanceOf(TaskContent.Committed::class.java, updated.data.content)
                assertCommittedRevisionUnchanged(expected.lastCommitted, actual.lastCommitted)
            }
        }
    }

    private fun assertWipUnchanged(expected: WipTaskContent, actual: WipTaskContent) {
        Assertions.assertEquals(expected.tests.ids, actual.tests.ids)
        Assertions.assertEquals(expected.exercises.ids, actual.exercises.ids)
        Assertions.assertEquals(expected.statement?.id, actual.statement?.id)
        Assertions.assertEquals(expected.developerSolutions.ids, actual.developerSolutions.ids)
        Assertions.assertEquals(expected.supportedTrikStudioVersions, actual.supportedTrikStudioVersions)
    }

    private fun assertCommittedRevisionUnchanged(expected: CommittedTaskContent, actual: CommittedTaskContent) {
        Assertions.assertEquals(expected.tests.ids, actual.tests.ids)
        Assertions.assertEquals(expected.exercises.ids, actual.exercises.ids)
        Assertions.assertEquals(expected.statement.id, actual.statement.id)
        Assertions.assertEquals(expected.developerSolutions.ids, actual.developerSolutions.ids)
        Assertions.assertEquals(expected.supportedTrikStudioVersions, actual.supportedTrikStudioVersions)
    }

    private fun assertUploadedFile(actual: FileData) {
        Assertions.assertEquals(uploadFile.uploadedFilename, actual.uploadedFilename)
        Assertions.assertArrayEquals(uploadFile.content, actual.content)
    }

    private fun uploadLanguage(language: String): TrikSupportedLanguage = when (language) {
        "Python" -> TrikSupportedLanguage.Python
        "JavaScript" -> TrikSupportedLanguage.JavaScript
        "VisualLanguage" -> TrikSupportedLanguage.VisualLanguage
        else -> error("Unsupported test language: $language")
    }

    @Nested
    inner class AddStatementTests {

        @BeforeEach
        fun mockUuid() {
            mockkStatic(UUID::class)
            every { UUID.randomUUID() } returns uploadUuid
        }

        @AfterEach
        fun unmockUuid() {
            unmockkStatic(UUID::class)
        }

        @Test
        fun `should reject upload before accessing storage if user is not a Developer`() {
            val user = testAdministrator { }

            assertRaises(MissedDeveloperRoleError) { upload(user) }

            verify(exactly = 0) { taskRepository.findById(any()) }
            assertNoUploadWrites()
        }

        @Test
        fun `should reject upload without saving if task does not exist`() {
            every { taskRepository.findById(uploadTaskId) } returns null

            assertRaises(TaskNotExistsError(uploadTaskId)) { upload() }

            assertNoUploadWrites()
        }

        @Test
        fun `should reject upload without saving if task belongs to another user even when shared`() {
            val original = testNewTask().withData {
                owner(99)
                sharedTo(listOf(4))
            }
            every { taskRepository.findById(uploadTaskId) } returns original

            assertRaises(TaskAccessDeniedError(uploadTaskId)) { upload() }

            assertNoUploadWrites()
        }

        @Test
        fun `should save a new resource and return the stored version`() {
            val original = testNewTask()
            prepareUpload(original)

            val result = upload().getOrThrow()

            Assertions.assertEquals(StatementId(11), result.id)
            Assertions.assertEquals(EntityVersion(1), result.version)
            Assertions.assertEquals(uploadName, result.data.name)
            Assertions.assertEquals("", result.data.description)
            Assertions.assertEquals(uploadBucket, result.data.versionBucket)
            assertUploadedFile(result.data.file)

            verify(exactly = 1) { statementRepository.save(any<StatementData>()) }
            assertUploadedTask(original)
        }

        @ParameterizedTest
        @ValueSource(strings = ["New", "Uncommitted", "Committed"])
        fun `should register upload without attaching or changing any task revision`(state: String) {
            val original = taskInState(state)
            prepareUpload(original)

            upload().getOrThrow()

            assertUploadedTask(original)
            Assertions.assertEquals(setOf(testStatement().data.versionBucket), original.data.uploadedResources)
        }

        @Test
        fun `should create separate chains for repeated names and files`() {
            val original = testNewTask()
            prepareUpload(original)
            val expectedBuckets = setOf(testStatement().data.versionBucket, uploadBucket, VersionBucket(UUID(0, 20)))
            every { UUID.randomUUID() } returnsMany listOf(uploadUuid, UUID(0, 20))
            every { taskRepository.update(any<Task>()) } answers {
                val saved = testSavedTask(firstArg())
                every { taskRepository.findById(uploadTaskId) } returns saved
                saved
            }

            val first = upload().getOrThrow()
            val second = upload().getOrThrow()

            Assertions.assertEquals(uploadBucket, first.data.versionBucket)
            Assertions.assertEquals(VersionBucket(UUID(0, 20)), second.data.versionBucket)
            verify(exactly = 1) {
                taskRepository.update(
                    match<Task> { updated ->
                        updated.data.uploadedResources == expectedBuckets
                    },
                )
            }
        }

        @Test
        fun `should propagate storage exception if task registration fails`() {
            prepareUpload()
            val failure = IllegalStateException("Task registration failed")
            every { taskRepository.update(any<Task>()) } throws failure

            val thrown = Assertions.assertThrows(IllegalStateException::class.java) { upload() }

            Assertions.assertSame(failure, thrown)
            verify(exactly = 1) { statementRepository.save(any<StatementData>()) }
        }

        private fun upload(user: MultipleRoleUser = developer) = developerOperations.addStatement(
            user = user,
            taskId = uploadTaskId,
            resourceName = uploadName,
            file = uploadFile,
        )
    }

    @Nested
    inner class AddExerciseTests {

        @BeforeEach
        fun mockUuid() {
            mockkStatic(UUID::class)
            every { UUID.randomUUID() } returns uploadUuid
        }

        @AfterEach
        fun unmockUuid() {
            unmockkStatic(UUID::class)
        }

        @Test
        fun `should reject upload before accessing storage if user is not a Developer`() {
            val user = testAdministrator { }

            assertRaises(MissedDeveloperRoleError) { upload(user) }

            verify(exactly = 0) { taskRepository.findById(any()) }
            assertNoUploadWrites()
        }

        @Test
        fun `should reject upload without saving if task does not exist`() {
            every { taskRepository.findById(uploadTaskId) } returns null

            assertRaises(TaskNotExistsError(uploadTaskId)) { upload() }

            assertNoUploadWrites()
        }

        @Test
        fun `should reject upload without saving if task belongs to another user even when shared`() {
            val original = testNewTask().withData {
                owner(99)
                sharedTo(listOf(4))
            }
            every { taskRepository.findById(uploadTaskId) } returns original

            assertRaises(TaskAccessDeniedError(uploadTaskId)) { upload() }

            assertNoUploadWrites()
        }

        @Test
        fun `should save a new resource and return the stored version`() {
            val original = testNewTask()
            prepareUpload(original)

            val result = upload().getOrThrow()

            Assertions.assertEquals(ExerciseId(12), result.id)
            Assertions.assertEquals(EntityVersion(1), result.version)
            Assertions.assertEquals(uploadName, result.data.name)
            Assertions.assertEquals("", result.data.description)
            Assertions.assertEquals(uploadBucket, result.data.versionBucket)
            assertUploadedFile(result.data.file)

            verify(exactly = 1) { exerciseRepository.save(any<ExerciseData>()) }
            assertUploadedTask(original)
        }

        @ParameterizedTest
        @ValueSource(strings = ["New", "Uncommitted", "Committed"])
        fun `should register upload without attaching or changing any task revision`(state: String) {
            val original = taskInState(state)
            prepareUpload(original)

            upload().getOrThrow()

            assertUploadedTask(original)
            Assertions.assertEquals(setOf(testStatement().data.versionBucket), original.data.uploadedResources)
        }

        @Test
        fun `should create separate chains for repeated names and files`() {
            val original = testNewTask()
            prepareUpload(original)
            val expectedBuckets = setOf(testStatement().data.versionBucket, uploadBucket, VersionBucket(UUID(0, 20)))
            every { UUID.randomUUID() } returnsMany listOf(uploadUuid, UUID(0, 20))
            every { taskRepository.update(any<Task>()) } answers {
                val saved = testSavedTask(firstArg())
                every { taskRepository.findById(uploadTaskId) } returns saved
                saved
            }

            val first = upload().getOrThrow()
            val second = upload().getOrThrow()

            Assertions.assertEquals(uploadBucket, first.data.versionBucket)
            Assertions.assertEquals(VersionBucket(UUID(0, 20)), second.data.versionBucket)
            verify(exactly = 1) {
                taskRepository.update(
                    match<Task> { updated ->
                        updated.data.uploadedResources == expectedBuckets
                    },
                )
            }
        }

        @Test
        fun `should propagate storage exception if task registration fails`() {
            prepareUpload()
            val failure = IllegalStateException("Task registration failed")
            every { taskRepository.update(any<Task>()) } throws failure

            val thrown = Assertions.assertThrows(IllegalStateException::class.java) { upload() }

            Assertions.assertSame(failure, thrown)
            verify(exactly = 1) { exerciseRepository.save(any<ExerciseData>()) }
        }

        @ParameterizedTest
        @ValueSource(strings = ["Python", "JavaScript", "VisualLanguage"])
        fun `should preserve every explicitly selected language`(language: String) {
            prepareUpload()
            val selected = uploadLanguage(language)

            val result = upload(language = selected).getOrThrow()

            Assertions.assertEquals(selected, result.data.language)
        }

        private fun upload(user: MultipleRoleUser = developer, language: TrikSupportedLanguage = TrikSupportedLanguage.Python) =
            developerOperations.addExercise(
                user = user,
                taskId = uploadTaskId,
                resourceName = uploadName,
                file = uploadFile,
                language = language,
            )
    }

    @Nested
    inner class AddTestTests {

        @BeforeEach
        fun mockUuid() {
            mockkStatic(UUID::class)
            every { UUID.randomUUID() } returns uploadUuid
        }

        @AfterEach
        fun unmockUuid() {
            unmockkStatic(UUID::class)
        }

        @Test
        fun `should reject upload before accessing storage if user is not a Developer`() {
            val user = testAdministrator { }

            assertRaises(MissedDeveloperRoleError) { upload(user) }

            verify(exactly = 0) { taskRepository.findById(any()) }
            assertNoUploadWrites()
        }

        @Test
        fun `should reject upload without saving if task does not exist`() {
            every { taskRepository.findById(uploadTaskId) } returns null

            assertRaises(TaskNotExistsError(uploadTaskId)) { upload() }

            assertNoUploadWrites()
        }

        @Test
        fun `should reject upload without saving if task belongs to another user even when shared`() {
            val original = testNewTask().withData {
                owner(99)
                sharedTo(listOf(4))
            }
            every { taskRepository.findById(uploadTaskId) } returns original

            assertRaises(TaskAccessDeniedError(uploadTaskId)) { upload() }

            assertNoUploadWrites()
        }

        @Test
        fun `should save a new resource and return the stored version`() {
            val original = testNewTask()
            prepareUpload(original)

            val result = upload().getOrThrow()

            Assertions.assertEquals(tech.testsys.domain.model.task.TestId(13), result.id)
            Assertions.assertEquals(EntityVersion(1), result.version)
            Assertions.assertEquals(uploadName, result.data.name)
            Assertions.assertEquals("", result.data.description)
            Assertions.assertEquals(uploadBucket, result.data.versionBucket)
            assertUploadedFile(result.data.file)

            verify(exactly = 1) { testRepository.save(any<TestData>()) }
            assertUploadedTask(original)
        }

        @ParameterizedTest
        @ValueSource(strings = ["New", "Uncommitted", "Committed"])
        fun `should register upload without attaching or changing any task revision`(state: String) {
            val original = taskInState(state)
            prepareUpload(original)

            upload().getOrThrow()

            assertUploadedTask(original)
            Assertions.assertEquals(setOf(testStatement().data.versionBucket), original.data.uploadedResources)
        }

        @Test
        fun `should create separate chains for repeated names and files`() {
            val original = testNewTask()
            prepareUpload(original)
            val expectedBuckets = setOf(testStatement().data.versionBucket, uploadBucket, VersionBucket(UUID(0, 20)))
            every { UUID.randomUUID() } returnsMany listOf(uploadUuid, UUID(0, 20))
            every { taskRepository.update(any<Task>()) } answers {
                val saved = testSavedTask(firstArg())
                every { taskRepository.findById(uploadTaskId) } returns saved
                saved
            }

            val first = upload().getOrThrow()
            val second = upload().getOrThrow()

            Assertions.assertEquals(uploadBucket, first.data.versionBucket)
            Assertions.assertEquals(VersionBucket(UUID(0, 20)), second.data.versionBucket)
            verify(exactly = 1) {
                taskRepository.update(
                    match<Task> { updated ->
                        updated.data.uploadedResources == expectedBuckets
                    },
                )
            }
        }

        @Test
        fun `should propagate storage exception if task registration fails`() {
            prepareUpload()
            val failure = IllegalStateException("Task registration failed")
            every { taskRepository.update(any<Task>()) } throws failure

            val thrown = Assertions.assertThrows(IllegalStateException::class.java) { upload() }

            Assertions.assertSame(failure, thrown)
            verify(exactly = 1) { testRepository.save(any<TestData>()) }
        }

        private fun upload(user: MultipleRoleUser = developer) = developerOperations.addTest(
            user = user,
            taskId = uploadTaskId,
            resourceName = uploadName,
            file = uploadFile,
        )
    }

    @Nested
    inner class AddDeveloperSolutionTests {

        @BeforeEach
        fun mockUuid() {
            mockkStatic(UUID::class)
            every { UUID.randomUUID() } returns uploadUuid
        }

        @AfterEach
        fun unmockUuid() {
            unmockkStatic(UUID::class)
        }

        @Test
        fun `should reject upload before accessing storage if user is not a Developer`() {
            val user = testAdministrator { }

            assertRaises(MissedDeveloperRoleError) { upload(user) }

            verify(exactly = 0) { taskRepository.findById(any()) }
            assertNoUploadWrites()
        }

        @Test
        fun `should reject upload without saving if task does not exist`() {
            every { taskRepository.findById(uploadTaskId) } returns null

            assertRaises(TaskNotExistsError(uploadTaskId)) { upload() }

            assertNoUploadWrites()
        }

        @Test
        fun `should reject upload without saving if task belongs to another user even when shared`() {
            val original = testNewTask().withData {
                owner(99)
                sharedTo(listOf(4))
            }
            every { taskRepository.findById(uploadTaskId) } returns original

            assertRaises(TaskAccessDeniedError(uploadTaskId)) { upload() }

            assertNoUploadWrites()
        }

        @Test
        fun `should save a new resource and return the stored version`() {
            val original = testNewTask()
            prepareUpload(original)

            val result = upload().getOrThrow()

            Assertions.assertEquals(DeveloperSolutionId(15), result.id)
            Assertions.assertEquals(EntityVersion(1), result.version)
            Assertions.assertEquals(uploadName, result.data.name)
            Assertions.assertEquals("", result.data.description)
            Assertions.assertEquals(uploadBucket, result.data.versionBucket)
            Assertions.assertEquals(tech.testsys.domain.model.task.SolutionId(14), result.data.solution.id)
            Assertions.assertEquals(uploadScore, result.data.expectedScore)
            val storedSolution = slot<SolutionData>()
            verify(exactly = 1) { solutionRepository.save(capture(storedSolution)) }
            assertUploadedFile(storedSolution.captured.file)
            Assertions.assertEquals(TrikSupportedLanguage.Python, storedSolution.captured.language)

            verify(exactly = 1) { developerSolutionRepository.save(any<DeveloperSolutionData>()) }
            assertUploadedTask(original)
        }

        @ParameterizedTest
        @ValueSource(strings = ["New", "Uncommitted", "Committed"])
        fun `should register upload without attaching or changing any task revision`(state: String) {
            val original = taskInState(state)
            prepareUpload(original)

            upload().getOrThrow()

            assertUploadedTask(original)
            Assertions.assertEquals(setOf(testStatement().data.versionBucket), original.data.uploadedResources)
        }

        @Test
        fun `should create separate chains for repeated names and files`() {
            val original = testNewTask()
            prepareUpload(original)
            val expectedBuckets = setOf(testStatement().data.versionBucket, uploadBucket, VersionBucket(UUID(0, 20)))
            every { UUID.randomUUID() } returnsMany listOf(uploadUuid, UUID(0, 20))
            every { taskRepository.update(any<Task>()) } answers {
                val saved = testSavedTask(firstArg())
                every { taskRepository.findById(uploadTaskId) } returns saved
                saved
            }

            val first = upload().getOrThrow()
            val second = upload().getOrThrow()

            Assertions.assertEquals(uploadBucket, first.data.versionBucket)
            Assertions.assertEquals(VersionBucket(UUID(0, 20)), second.data.versionBucket)
            verify(exactly = 1) {
                taskRepository.update(
                    match<Task> { updated ->
                        updated.data.uploadedResources == expectedBuckets
                    },
                )
            }
        }

        @Test
        fun `should propagate storage exception if task registration fails`() {
            prepareUpload()
            val failure = IllegalStateException("Task registration failed")
            every { taskRepository.update(any<Task>()) } throws failure

            val thrown = Assertions.assertThrows(IllegalStateException::class.java) { upload() }

            Assertions.assertSame(failure, thrown)
            verify(exactly = 1) { developerSolutionRepository.save(any<DeveloperSolutionData>()) }
        }

        @ParameterizedTest
        @ValueSource(strings = ["Python", "JavaScript", "VisualLanguage"])
        fun `should preserve every explicitly selected language`(language: String) {
            prepareUpload()
            val selected = uploadLanguage(language)

            val result = upload(language = selected).getOrThrow()

            Assertions.assertEquals(tech.testsys.domain.model.task.SolutionId(14), result.data.solution.id)
            val saved = slot<SolutionData>()
            verify(exactly = 1) { solutionRepository.save(capture(saved)) }
            Assertions.assertEquals(selected, saved.captured.language)
        }

        private fun upload(user: MultipleRoleUser = developer, language: TrikSupportedLanguage = TrikSupportedLanguage.Python) =
            developerOperations.addDeveloperSolution(
                user = user,
                taskId = uploadTaskId,
                resourceName = uploadName,
                file = uploadFile,
                language = language,
                expectedScore = uploadScore,
            )
    }

    @Nested
    inner class CreateTaskTests {

        private val taskName = "testTask"
        private val taskDescription = "testTaskDescription"

        @BeforeEach
        fun beforeEach() {
            every { taskRepository.save(any<TaskData>()) } answers {
                task {
                    id = 1L
                    createdAt = Instant.MIN
                    version = EntityVersion(0)
                    data = firstArg<TaskData>()
                }
            }
        }

        @Test
        fun `should raise MissedDeveloperRoleError if user is not a Developer`() {
            val nonDeveloper = testAdministrator { }

            assertRaises(MissedDeveloperRoleError) {
                developerOperations.createTask(nonDeveloper, taskName, taskDescription)
            }
        }

        @Test
        fun `should create a New Task`() {
            val result = developerOperations.createTask(developer, taskName, taskDescription).getOrThrow()

            Assertions.assertTrue { result.data.content is TaskContent.New }
        }

        @Test
        fun `should create task with provided name and description`() {
            val result = developerOperations.createTask(developer, taskName, taskDescription).getOrThrow()

            val taskData = result.data
            Assertions.assertEquals(taskName, taskData.name)
            Assertions.assertEquals(taskDescription, taskData.description)
            Assertions.assertEquals(developer.id.value, taskData.owner.id.value)
        }

        @Test
        fun `should create task with empty data except name, owner and description`() {
            val result = developerOperations.createTask(developer, taskName, taskDescription).getOrThrow()

            val taskData = result.data
            Assertions.assertTrue { taskData.sharedTo.ids.isEmpty() }
            Assertions.assertEquals(emptySet<VersionBucket>(), taskData.uploadedResources)
            val taskContent = Assertions.assertInstanceOf(TaskContent.New::class.java, taskData.content)
            Assertions.assertTrue { taskContent.wip.tests.ids.isEmpty() }
            Assertions.assertTrue { taskContent.wip.exercises.ids.isEmpty() }
            Assertions.assertTrue { taskContent.wip.statement == null }
            Assertions.assertTrue { taskContent.wip.developerSolutions.ids.isEmpty() }
            Assertions.assertTrue { taskContent.wip.supportedTrikStudioVersions.isEmpty() }
        }

        @Test
        fun `should save task`() {
            developerOperations.createTask(developer, taskName, taskDescription).getOrThrow()

            verify(exactly = 1) { taskRepository.save(any<TaskData>()) }
        }
    }

    @Nested
    inner class AttachStatementTests {
        val taskId = TaskId(1L)
        val statementId = StatementId(1)

        @BeforeEach
        fun beforeEach() {
            every { statementRepository.findLatestByVersionBucket(any()) } returns testStatement(1)
            every { statementRepository.load(any<LazyEntity<StatementId, Statement>>()) } answers {
                testStatement(firstArg<LazyEntity<StatementId, Statement>>().id.value)
            }
        }

        @Test
        fun `should reject an older statement version without saving`() {
            every { taskRepository.findById(taskId) } returns testNewTask()
            every { statementRepository.findById(statementId) } returns testStatement(1)
            every { statementRepository.findLatestByVersionBucket(any()) } returns testStatement(2)

            assertRaises(StatementVersionNotLatestError(statementId)) {
                developerOperations.attachStatement(developer, taskId, statementId)
            }
            verify(exactly = 0) { taskRepository.update(any<Task>()) }
        }

        @Test
        fun `should reject another statement chain when the statement slot is occupied`() {
            val existing = testStatement(3).withData { versionBucket = VersionBucket(UUID(0, 3)) }
            every { taskRepository.findById(taskId) } returns testNewTask().withData { content.new { statement(3) } }
            every { statementRepository.findById(statementId) } returns testStatement(1)
            every { statementRepository.load(any<LazyEntity<StatementId, Statement>>()) } returns existing

            assertRaises(TaskAlreadyHasStatementError) {
                developerOperations.attachStatement(developer, taskId, statementId)
            }
            verify(exactly = 0) { taskRepository.update(any<Task>()) }
        }

        @Test
        fun `should reject a statement not uploaded to the task before checking committed content`() {
            every { taskRepository.findById(taskId) } returns testCommitedTask().withData { uploadedResources.clear() }
            every { statementRepository.findById(statementId) } returns testStatement(1)

            assertRaises(StatementNotUploadedToTaskError(taskId = taskId, statementId = statementId)) {
                developerOperations.attachStatement(developer, taskId, statementId)
            }
            verify(exactly = 0) { taskRepository.update(any<Task>()) }
        }

        @Test
        fun `should reject a statement whose chain is uploaded to another task`() {
            val foreign = testNewTask()
            val target = testNewTask().withData { uploadedResources.clear() }
            every { taskRepository.findById(taskId) } returns target
            every { statementRepository.findById(statementId) } returns testStatement(1)
            Assertions.assertTrue(testStatement().data.versionBucket in foreign.data.uploadedResources)

            assertRaises(StatementNotUploadedToTaskError(taskId = taskId, statementId = statementId)) {
                developerOperations.attachStatement(developer, taskId, statementId)
            }
            verify(exactly = 0) { taskRepository.update(any<Task>()) }
        }

        @Test
        fun `should attach another version from an uploaded statement chain`() {
            val anotherVersionId = StatementId(2)
            every { taskRepository.findById(taskId) } returns testNewTask()
            every { statementRepository.findById(anotherVersionId) } returns testStatement(2)
            every { statementRepository.findLatestByVersionBucket(any()) } returns testStatement(2)
            every { taskRepository.update(any<Task>()) } answers { testSavedTask(firstArg()) }

            val result = developerOperations.attachStatement(developer, taskId, anotherVersionId).getOrThrow()

            val content = Assertions.assertInstanceOf(TaskContent.New::class.java, result.data.content)
            Assertions.assertEquals(anotherVersionId, content.wip.statement?.id)
            Assertions.assertEquals(testNewTask().data.uploadedResources, result.data.uploadedResources)
        }

        @Test
        fun `should raise MissedDeveloperRoleError if user is not a Developer`() {
            val nonDeveloper = testAdministrator { }

            assertRaises(MissedDeveloperRoleError) {
                developerOperations.attachStatement(nonDeveloper, taskId, statementId)
            }
        }

        @Test
        fun `should raise TaskNotExistsError if task not exists`() {
            every { statementRepository.findById(eq(statementId)) } answers { testStatement(1) }
            every { taskRepository.findById(any()) } answers { null }

            assertRaises(TaskNotExistsError(taskId)) {
                developerOperations.attachStatement(developer, taskId, statementId)
            }
        }

        @Test
        fun `should raise StatementNotExistsError if statement not exists`() {
            every { statementRepository.findById(any()) } answers { null }
            every { taskRepository.findById(eq(taskId)) } answers { testUncommittedTask() }

            assertRaises(StatementNotExistsError(statementId)) {
                developerOperations.attachStatement(developer, taskId, statementId)
            }
        }

        @Test
        fun `should raise TaskAccessDeniedError if task is owned by another user`() {
            every { statementRepository.findById(eq(statementId)) } answers { testStatement(1) }
            every { taskRepository.findById(eq(taskId)) } answers {
                testUncommittedTask().withData {
                    owner = MultipleRoleUserId(1L)
                    uploadedResources.clear()
                }
            }

            assertRaises(TaskAccessDeniedError(taskId)) {
                developerOperations.attachStatement(developer, taskId, statementId)
            }
        }

        @Test
        fun `should raise TaskAlreadyHasStatementError if task has statement`() {
            every { statementRepository.findById(eq(statementId)) } answers { testStatement(1) }
            every { taskRepository.findById(eq(taskId)) } answers {
                testUncommittedTask().withData {
                    content.uncommitted(
                        wipBuilder = { statement = statementId },
                        lastCommittedBuilder = {},
                    )
                }
            }

            assertRaises(ResourceAlreadyAttachedError(taskId, testStatement().data.versionBucket)) {
                developerOperations.attachStatement(developer, taskId, statementId)
            }
        }

        @Test
        fun `should raise TaskAlreadyHasStatementError if task is Committed`() {
            every { statementRepository.findById(eq(statementId)) } answers { testStatement(1) }
            every { taskRepository.findById(eq(taskId)) } answers { testCommitedTask() }

            assertRaises(ResourceAlreadyAttachedError(taskId, testStatement().data.versionBucket)) {
                developerOperations.attachStatement(developer, taskId, statementId)
            }
        }

        @Test
        fun `should attach statement to the wip revision if task is New`() {
            every { statementRepository.findById(eq(statementId)) } answers { testStatement(1) }
            every { taskRepository.findById(eq(taskId)) } answers { testNewTask() }
            every { taskRepository.update(any<Task>()) } answers { testSavedTask(firstArg()) }

            val result = developerOperations.attachStatement(developer, taskId, statementId)
                .getOrThrow()

            val content = Assertions.assertInstanceOf(TaskContent.New::class.java, result.data.content)
            Assertions.assertEquals(statementId, content.wip.statement?.id)
        }

        @Test
        fun `should keep the last committed revision if task is Uncommitted`() {
            every { statementRepository.findById(eq(statementId)) } answers { testStatement(1) }
            every { taskRepository.findById(eq(taskId)) } answers { testUncommittedTask() }
            every { taskRepository.update(any<Task>()) } answers { testSavedTask(firstArg()) }

            val result = developerOperations.attachStatement(developer, taskId, statementId)
                .getOrThrow()

            val content = Assertions.assertInstanceOf(TaskContent.Uncommitted::class.java, result.data.content)
            Assertions.assertEquals(statementId, content.wip.statement?.id)
            Assertions.assertEquals(listOf(ExerciseId(1L)), content.lastCommitted.exercises.ids)
            Assertions.assertEquals(StatementId(1L), content.lastCommitted.statement.id)
        }

        @Test
        fun `should change statement`() {
            every { statementRepository.findById(eq(statementId)) } answers { testStatement(1) }
            every { taskRepository.findById(eq(taskId)) } answers { testUncommittedTask() }
            every { taskRepository.update(any<Task>()) } answers { testSavedTask(firstArg()) }

            val result = developerOperations.attachStatement(developer, taskId, statementId)
                .getOrThrow()

            val content = Assertions.assertInstanceOf(TaskContent.Uncommitted::class.java, result.data.content)
            Assertions.assertEquals(statementId, content.wip.statement?.id)
        }

        @Test
        fun `should return the task with the version assigned on update`() {
            every { statementRepository.findById(eq(statementId)) } answers { testStatement(1) }
            every { taskRepository.findById(eq(taskId)) } answers { testUncommittedTask() }
            every { taskRepository.update(any<Task>()) } answers { testSavedTask(firstArg()) }

            val result = developerOperations.attachStatement(developer, taskId, statementId)
                .getOrThrow()

            Assertions.assertEquals(savedTaskVersion, result.version)
        }

        @Test
        fun `should update task`() {
            every { statementRepository.findById(eq(statementId)) } answers { testStatement(1) }
            every { taskRepository.findById(eq(taskId)) } answers { testUncommittedTask() }
            every { taskRepository.update(any<Task>()) } answers { testSavedTask(firstArg()) }

            developerOperations.attachStatement(developer, taskId, statementId)
                .getOrThrow()

            verify(exactly = 1) { taskRepository.update(any<Task>()) }
        }
    }

    @Nested
    inner class UpdateStatementTests {

        private val bucket = VersionBucket(UUID(0, 0))
        private val resourceId = StatementId(21)
        private val originalResource = resource(21)

        @Test
        fun `should save new version without filling an empty WIP slot`() {
            prepare(taskForUpdate("New").withData { content.new { statement = null } })

            val result = request(file = uploadFile).getOrThrow()

            Assertions.assertEquals(StatementId(31), result.id)
            verify(exactly = 0) { taskRepository.update(any<Task>()) }
            verify(exactly = 0) { statementRepository.update(any<Statement>()) }
        }

        @Test
        fun `should reject update before accessing storage if user is not a Developer`() {
            val user = testAdministrator {}

            assertRaises(MissedDeveloperRoleError) { request(user = user, file = uploadFile) }

            verify(exactly = 0) { taskRepository.findById(any()) }
            assertNoWrites()
        }

        @Test
        fun `should reject update without writes if task does not exist`() {
            every { taskRepository.findById(uploadTaskId) } returns null

            assertRaises(TaskNotExistsError(uploadTaskId)) { request(file = uploadFile) }

            assertNoWrites()
        }

        @Test
        fun `should reject update without writes if resource does not exist`() {
            prepare()
            every { statementRepository.findById(resourceId) } returns null

            assertRaises(StatementNotExistsError(resourceId)) { request(file = uploadFile) }

            assertNoWrites()
        }

        @Test
        fun `should reject update of another owners task even when shared`() {
            prepare(taskForUpdate("New").withData { owner(99) })

            assertRaises(TaskAccessDeniedError(uploadTaskId)) { request(file = uploadFile) }

            assertNoWrites()
        }

        @Test
        fun `should reject update without writes if chain was not uploaded to task`() {
            prepare(taskForUpdate("New").withData { uploadedResources.clear() })

            assertRaises(StatementNotUploadedToTaskError(uploadTaskId, resourceId)) { request(file = uploadFile) }

            assertNoWrites()
        }

        @ParameterizedTest
        @ValueSource(strings = ["name", "file"])
        fun `should reject an older source version for every change kind`(mode: String) {
            prepare()
            every { statementRepository.findLatestByVersionBucket(bucket) } returns resource(22)

            assertRaises(StatementVersionNotLatestError(resourceId)) {
                request(resourceName = "renamed", file = fileForMode(mode))
            }

            assertNoWrites()
        }

        @Test
        fun `should reject update if latest lookup returns no version`() {
            prepare()
            every { statementRepository.findLatestByVersionBucket(bucket) } returns null

            assertRaises(StatementVersionNotLatestError(resourceId)) { request(file = uploadFile) }

            assertNoWrites()
        }

        @ParameterizedTest
        @ValueSource(strings = ["New", "Uncommitted", "Committed"])
        fun `should rename latest version in place without changing task in any state`(state: String) {
            val original = taskForUpdate(state)
            prepare(original)

            val result = request(resourceName = "renamed").getOrThrow()

            Assertions.assertEquals(resourceId, result.id)
            Assertions.assertEquals(originalResource.createdAt, result.createdAt)
            Assertions.assertEquals(originalResource.version, result.version)
            Assertions.assertEquals("renamed", result.data.name)
            Assertions.assertEquals("name", originalResource.data.name)
            assertResourceFields(result)
            verify(exactly = 1) { statementRepository.update(any<Statement>()) }
            assertNoUploadWrites()
        }

        @ParameterizedTest
        @NullSource
        @ValueSource(strings = ["name", ""])
        fun `should persist metadata without checking unchanged or omitted name`(resourceName: String?) {
            prepare()

            val result = request(resourceName = resourceName).getOrThrow()

            Assertions.assertEquals(resourceId, result.id)
            Assertions.assertEquals(resourceName ?: "name", result.data.name)
            assertResourceFields(result)
            verify(exactly = 1) { statementRepository.update(any<Statement>()) }
            assertNoUploadWrites()
        }

        @ParameterizedTest
        @ValueSource(strings = ["New", "Uncommitted", "Committed"])
        fun `should replace an older attached chain version and preserve task revisions in every state`(state: String) {
            val original = taskForUpdate(state)
            prepare(original)

            val result = request(file = uploadFile).getOrThrow()

            Assertions.assertEquals(StatementId(31), result.id)
            Assertions.assertEquals("name", result.data.name)
            Assertions.assertEquals("description", result.data.description)
            Assertions.assertEquals(bucket, result.data.versionBucket)
            assertUploadedFile(result.data.file)
            assertTaskReplacement(original)
            verify(exactly = 0) { statementRepository.update(any<Statement>()) }
            Assertions.assertEquals("name", originalResource.data.name)
            Assertions.assertArrayEquals(byteArrayOf(1, 2), originalResource.data.file.content)
        }

        @Test
        fun `should give new version the supplied name without renaming the previous version`() {
            prepare()

            val result = request(resourceName = "renamed", file = uploadFile).getOrThrow()

            Assertions.assertEquals(StatementId(31), result.id)
            Assertions.assertEquals("renamed", result.data.name)
            Assertions.assertEquals("name", originalResource.data.name)
            verify(exactly = 0) { statementRepository.update(any<Statement>()) }
        }

        @Test
        fun `should create another version even for identical file contents`() {
            prepare()
            val identical = FileData(uploadedFilename = "original.bin", content = byteArrayOf(1, 2))

            val result = request(file = identical).getOrThrow()

            Assertions.assertEquals(StatementId(31), result.id)
            verify(exactly = 1) { statementRepository.save(any<StatementData>()) }
            verify(exactly = 0) { statementRepository.update(any<Statement>()) }
        }

        @Test
        fun `should leave existing WIP unchanged when chain remains only in last committed revision`() {
            val original = taskForUpdate("Uncommitted").withData {
                content.uncommitted(
                    wipBuilder = { statement = StatementId(9) },
                    lastCommittedBuilder = {},
                )
            }
            prepare(original)

            val result = request(file = uploadFile).getOrThrow()

            Assertions.assertEquals(StatementId(31), result.id)
            verify(exactly = 0) { taskRepository.update(any<Task>()) }
            verify(exactly = 0) { statementRepository.update(any<Statement>()) }
        }

        @ParameterizedTest
        @ValueSource(strings = ["New", "Uncommitted", "Committed"])
        fun `should save detached chain without attaching or changing task in any state`(state: String) {
            val original = taskForUpdate(state, attached = false)
            prepare(original)

            val result = request(file = uploadFile).getOrThrow()

            Assertions.assertEquals(StatementId(31), result.id)
            verify(exactly = 0) { taskRepository.update(any<Task>()) }
            verify(exactly = 0) { statementRepository.update(any<Statement>()) }
        }

        private fun resource(id: Long, chain: VersionBucket = bucket): Statement = statement {
            this.id = id
            createdAt = Instant.MIN
            version = EntityVersion(0)
            data = statementData {
                name = "name"
                description = "description"
                versionBucket = chain
                file("original.bin", byteArrayOf(1, 2))
            }
        }

        private fun taskForUpdate(state: String, attached: Boolean = true): Task = taskInState(state).withData {
            uploadedResources.add(bucket)
            when (state) {
                "New" -> content.new { configureContent(attached) }
                "Uncommitted" -> content.uncommitted(
                    wipBuilder = { configureContent(attached) },
                    lastCommittedBuilder = { configureContent(attached) },
                )
                "Committed" -> content.committed {
                    statement = StatementId(1)
                    exercises = mutableListOf(ExerciseId(1))
                    configureContent(attached)
                }
                else -> error("Unsupported test state: $state")
            }
        }

        private fun tech.testsys.domain.builder.task.TaskContentBuilder<*>.configureContent(attached: Boolean) {
            if (attached) {
                statement = StatementId(10)
            } else {
                statement = StatementId(9)
            }
        }

        private fun prepare(original: Task = taskForUpdate("New")) {
            prepareUpload(original)
            every { statementRepository.findById(resourceId) } returns originalResource
            every { statementRepository.findLatestByVersionBucket(bucket) } returns originalResource
            every { statementRepository.update(any<Statement>()) } answers { firstArg<Statement>() }
            every { statementRepository.save(any<StatementData>()) } answers {
                statement {
                    id = 31
                    createdAt = Instant.EPOCH
                    version = EntityVersion(1)
                    data = firstArg<StatementData>()
                }
            }
            every { statementRepository.load(any<LazyEntity<StatementId, Statement>>()) } answers {
                val attachedId = firstArg<LazyEntity<StatementId, Statement>>().id
                resource(attachedId.value, if (attachedId.value == 10L) bucket else VersionBucket(UUID(0, 9)))
            }
        }

        private fun request(user: MultipleRoleUser = developer, resourceName: String? = null, file: FileData? = null) =
            developerOperations.updateStatement(
                user = user,
                taskId = uploadTaskId,
                statementId = resourceId,
                resourceName = resourceName,
                file = file,
            )

        private fun assertNoWrites() {
            assertNoUploadWrites()
            verify(exactly = 0) { statementRepository.update(any<Statement>()) }
        }

        private fun fileForMode(mode: String): FileData? = when (mode) {
            "file" -> uploadFile
            "name" -> null
            else -> error("Unsupported update mode: $mode")
        }

        private fun assertResourceFields(result: Statement) {
            Assertions.assertEquals("description", result.data.description)
            Assertions.assertEquals(bucket, result.data.versionBucket)
            Assertions.assertEquals("original.bin", result.data.file.uploadedFilename)
            Assertions.assertArrayEquals(byteArrayOf(1, 2), result.data.file.content)
        }

        private fun assertTaskReplacement(original: Task) {
            val captured = slot<Task>()
            verify(exactly = 1) { taskRepository.update(capture(captured)) }
            val updated = captured.captured
            val before = original.getEditableContent()
            val after = updated.getEditableContent()
            Assertions.assertEquals(StatementId(31), after.statement?.id)
            Assertions.assertEquals(before.exercises.ids, after.exercises.ids)
            Assertions.assertEquals(before.tests.ids, after.tests.ids)
            Assertions.assertEquals(before.developerSolutions.ids, after.developerSolutions.ids)
            Assertions.assertEquals(before.supportedTrikStudioVersions, after.supportedTrikStudioVersions)
            Assertions.assertEquals(original.id, updated.id)
            Assertions.assertEquals(original.createdAt, updated.createdAt)
            Assertions.assertEquals(original.version, updated.version)
            Assertions.assertEquals(original.data.owner.id, updated.data.owner.id)
            Assertions.assertEquals(original.data.name, updated.data.name)
            Assertions.assertEquals(original.data.description, updated.data.description)
            Assertions.assertEquals(original.data.sharedTo.ids, updated.data.sharedTo.ids)
            Assertions.assertEquals(original.data.uploadedResources, updated.data.uploadedResources)
            when (val content = original.data.content) {
                is TaskContent.New -> Assertions.assertInstanceOf(TaskContent.New::class.java, updated.data.content)
                is TaskContent.Uncommitted -> assertCommittedUnchanged(content.lastCommitted, updated)
                is TaskContent.Committed -> assertCommittedUnchanged(content.lastCommitted, updated)
            }
        }
    }

    @Nested
    inner class UpdateExerciseTests {

        private val bucket = VersionBucket(UUID(0, 0))
        private val resourceId = ExerciseId(21)
        private val originalResource = resource(21)

        @ParameterizedTest
        @ValueSource(strings = ["Python", "JavaScript", "VisualLanguage"])
        fun `should preserve exercise language when uploading a replacement file`(language: String) {
            prepare()
            every { exerciseRepository.findById(resourceId) } returns originalResource.withData {
                when (uploadLanguage(language)) {
                    TrikSupportedLanguage.Python -> this.language.python()
                    TrikSupportedLanguage.JavaScript -> this.language.javaScript()
                    TrikSupportedLanguage.VisualLanguage -> this.language.visualLanguage()
                }
            }

            val result = request(file = uploadFile).getOrThrow()

            Assertions.assertEquals(ExerciseId(31), result.id)
            Assertions.assertEquals(uploadLanguage(language), result.data.language)
            assertUploadedFile(result.data.file)
        }

        @Test
        fun `should save new version without filling an empty WIP slot`() {
            prepare(taskForUpdate("New").withData { content.new { exercises.clear() } })

            val result = request(file = uploadFile).getOrThrow()

            Assertions.assertEquals(ExerciseId(31), result.id)
            verify(exactly = 0) { taskRepository.update(any<Task>()) }
            verify(exactly = 0) { exerciseRepository.update(any<Exercise>()) }
        }

        @Test
        fun `should reject update before accessing storage if user is not a Developer`() {
            val user = testAdministrator {}

            assertRaises(MissedDeveloperRoleError) { request(user = user, file = uploadFile) }

            verify(exactly = 0) { taskRepository.findById(any()) }
            assertNoWrites()
        }

        @Test
        fun `should reject update without writes if task does not exist`() {
            every { taskRepository.findById(uploadTaskId) } returns null

            assertRaises(TaskNotExistsError(uploadTaskId)) { request(file = uploadFile) }

            assertNoWrites()
        }

        @Test
        fun `should reject update without writes if resource does not exist`() {
            prepare()
            every { exerciseRepository.findById(resourceId) } returns null

            assertRaises(ExerciseNotExistsError(resourceId)) { request(file = uploadFile) }

            assertNoWrites()
        }

        @Test
        fun `should reject update of another owners task even when shared`() {
            prepare(taskForUpdate("New").withData { owner(99) })

            assertRaises(TaskAccessDeniedError(uploadTaskId)) { request(file = uploadFile) }

            assertNoWrites()
        }

        @Test
        fun `should reject update without writes if chain was not uploaded to task`() {
            prepare(taskForUpdate("New").withData { uploadedResources.clear() })

            assertRaises(ExerciseNotUploadedToTaskError(uploadTaskId, resourceId)) { request(file = uploadFile) }

            assertNoWrites()
        }

        @ParameterizedTest
        @ValueSource(strings = ["name", "file"])
        fun `should reject an older source version for every change kind`(mode: String) {
            prepare()
            every { exerciseRepository.findLatestByVersionBucket(bucket) } returns resource(22)

            assertRaises(ExerciseVersionNotLatestError(resourceId)) {
                request(resourceName = "renamed", file = fileForMode(mode))
            }

            assertNoWrites()
        }

        @Test
        fun `should reject update if latest lookup returns no version`() {
            prepare()
            every { exerciseRepository.findLatestByVersionBucket(bucket) } returns null

            assertRaises(ExerciseVersionNotLatestError(resourceId)) { request(file = uploadFile) }

            assertNoWrites()
        }

        @ParameterizedTest
        @ValueSource(strings = ["New", "Uncommitted", "Committed"])
        fun `should rename latest version in place without changing task in any state`(state: String) {
            val original = taskForUpdate(state)
            prepare(original)

            val result = request(resourceName = "renamed").getOrThrow()

            Assertions.assertEquals(resourceId, result.id)
            Assertions.assertEquals(originalResource.createdAt, result.createdAt)
            Assertions.assertEquals(originalResource.version, result.version)
            Assertions.assertEquals("renamed", result.data.name)
            Assertions.assertEquals("name", originalResource.data.name)
            assertResourceFields(result)
            verify(exactly = 1) { exerciseRepository.update(any<Exercise>()) }
            assertNoUploadWrites()
        }

        @ParameterizedTest
        @NullSource
        @ValueSource(strings = ["name", ""])
        fun `should persist metadata without checking unchanged or omitted name`(resourceName: String?) {
            prepare()

            val result = request(resourceName = resourceName).getOrThrow()

            Assertions.assertEquals(resourceId, result.id)
            Assertions.assertEquals(resourceName ?: "name", result.data.name)
            assertResourceFields(result)
            verify(exactly = 1) { exerciseRepository.update(any<Exercise>()) }
            assertNoUploadWrites()
        }

        @ParameterizedTest
        @ValueSource(strings = ["New", "Uncommitted", "Committed"])
        fun `should replace an older attached chain version and preserve task revisions in every state`(state: String) {
            val original = taskForUpdate(state)
            prepare(original)

            val result = request(file = uploadFile).getOrThrow()

            Assertions.assertEquals(ExerciseId(31), result.id)
            Assertions.assertEquals("name", result.data.name)
            Assertions.assertEquals("description", result.data.description)
            Assertions.assertEquals(bucket, result.data.versionBucket)
            assertUploadedFile(result.data.file)
            Assertions.assertEquals(TrikSupportedLanguage.Python, result.data.language)
            assertTaskReplacement(original)
            verify(exactly = 0) { exerciseRepository.update(any<Exercise>()) }
            Assertions.assertEquals("name", originalResource.data.name)
            Assertions.assertArrayEquals(byteArrayOf(1, 2), originalResource.data.file.content)
        }

        @Test
        fun `should give new version the supplied name without renaming the previous version`() {
            prepare()

            val result = request(resourceName = "renamed", file = uploadFile).getOrThrow()

            Assertions.assertEquals(ExerciseId(31), result.id)
            Assertions.assertEquals("renamed", result.data.name)
            Assertions.assertEquals("name", originalResource.data.name)
            verify(exactly = 0) { exerciseRepository.update(any<Exercise>()) }
        }

        @Test
        fun `should create another version even for identical file contents`() {
            prepare()
            val identical = FileData(uploadedFilename = "original.bin", content = byteArrayOf(1, 2))

            val result = request(file = identical).getOrThrow()

            Assertions.assertEquals(ExerciseId(31), result.id)
            verify(exactly = 1) { exerciseRepository.save(any<ExerciseData>()) }
            verify(exactly = 0) { exerciseRepository.update(any<Exercise>()) }
        }

        @Test
        fun `should leave existing WIP unchanged when chain remains only in last committed revision`() {
            val original = taskForUpdate("Uncommitted").withData {
                content.uncommitted(
                    wipBuilder = { exercises = mutableListOf(ExerciseId(9), ExerciseId(11)) },
                    lastCommittedBuilder = {},
                )
            }
            prepare(original)

            val result = request(file = uploadFile).getOrThrow()

            Assertions.assertEquals(ExerciseId(31), result.id)
            verify(exactly = 0) { taskRepository.update(any<Task>()) }
            verify(exactly = 0) { exerciseRepository.update(any<Exercise>()) }
        }

        @ParameterizedTest
        @ValueSource(strings = ["New", "Uncommitted", "Committed"])
        fun `should save detached chain without attaching or changing task in any state`(state: String) {
            val original = taskForUpdate(state, attached = false)
            prepare(original)

            val result = request(file = uploadFile).getOrThrow()

            Assertions.assertEquals(ExerciseId(31), result.id)
            verify(exactly = 0) { taskRepository.update(any<Task>()) }
            verify(exactly = 0) { exerciseRepository.update(any<Exercise>()) }
        }

        private fun resource(id: Long, chain: VersionBucket = bucket): Exercise = exercise {
            this.id = id
            createdAt = Instant.MIN
            version = EntityVersion(0)
            data = exerciseData {
                name = "name"
                description = "description"
                versionBucket = chain
                file("original.bin", byteArrayOf(1, 2))
                language.python()
            }
        }

        private fun taskForUpdate(state: String, attached: Boolean = true): Task = taskInState(state).withData {
            uploadedResources.add(bucket)
            when (state) {
                "New" -> content.new { configureContent(attached) }
                "Uncommitted" -> content.uncommitted(
                    wipBuilder = { configureContent(attached) },
                    lastCommittedBuilder = { configureContent(attached) },
                )
                "Committed" -> content.committed {
                    statement = StatementId(1)
                    exercises = mutableListOf(ExerciseId(1))
                    configureContent(attached)
                }
                else -> error("Unsupported test state: $state")
            }
        }

        private fun tech.testsys.domain.builder.task.TaskContentBuilder<*>.configureContent(attached: Boolean) {
            if (attached) {
                exercises = mutableListOf(ExerciseId(10), ExerciseId(11))
            } else {
                exercises = mutableListOf(ExerciseId(9), ExerciseId(11))
            }
        }

        private fun prepare(original: Task = taskForUpdate("New")) {
            prepareUpload(original)
            every { exerciseRepository.findById(resourceId) } returns originalResource
            every { exerciseRepository.findLatestByVersionBucket(bucket) } returns originalResource
            every { exerciseRepository.update(any<Exercise>()) } answers { firstArg<Exercise>() }
            every { exerciseRepository.save(any<ExerciseData>()) } answers {
                exercise {
                    id = 31
                    createdAt = Instant.EPOCH
                    version = EntityVersion(1)
                    data = firstArg<ExerciseData>()
                }
            }
            every { exerciseRepository.load(any<LazyEntityList<ExerciseId, Exercise>>()) } answers {
                firstArg<LazyEntityList<ExerciseId, Exercise>>().ids.map { attachedId ->
                    resource(attachedId.value, if (attachedId.value == 10L) bucket else VersionBucket(UUID(0, 9)))
                        .withData { if (attachedId.value == 11L) language.javaScript() }
                }
            }
        }

        private fun request(user: MultipleRoleUser = developer, resourceName: String? = null, file: FileData? = null) =
            developerOperations.updateExercise(
                user = user,
                taskId = uploadTaskId,
                exerciseId = resourceId,
                resourceName = resourceName,
                file = file,
            )

        private fun assertNoWrites() {
            assertNoUploadWrites()
            verify(exactly = 0) { exerciseRepository.update(any<Exercise>()) }
        }

        private fun fileForMode(mode: String): FileData? = when (mode) {
            "file" -> uploadFile
            "name" -> null
            else -> error("Unsupported update mode: $mode")
        }

        private fun assertResourceFields(result: Exercise) {
            Assertions.assertEquals("description", result.data.description)
            Assertions.assertEquals(bucket, result.data.versionBucket)
            Assertions.assertEquals("original.bin", result.data.file.uploadedFilename)
            Assertions.assertArrayEquals(byteArrayOf(1, 2), result.data.file.content)
            Assertions.assertEquals(TrikSupportedLanguage.Python, result.data.language)
        }

        private fun assertTaskReplacement(original: Task) {
            val captured = slot<Task>()
            verify(exactly = 1) { taskRepository.update(capture(captured)) }
            val updated = captured.captured
            val before = original.getEditableContent()
            val after = updated.getEditableContent()
            Assertions.assertEquals(listOf(ExerciseId(31), ExerciseId(11)), after.exercises.ids)
            Assertions.assertEquals(before.statement?.id, after.statement?.id)
            Assertions.assertEquals(before.tests.ids, after.tests.ids)
            Assertions.assertEquals(before.developerSolutions.ids, after.developerSolutions.ids)
            Assertions.assertEquals(before.supportedTrikStudioVersions, after.supportedTrikStudioVersions)
            Assertions.assertEquals(original.id, updated.id)
            Assertions.assertEquals(original.createdAt, updated.createdAt)
            Assertions.assertEquals(original.version, updated.version)
            Assertions.assertEquals(original.data.owner.id, updated.data.owner.id)
            Assertions.assertEquals(original.data.name, updated.data.name)
            Assertions.assertEquals(original.data.description, updated.data.description)
            Assertions.assertEquals(original.data.sharedTo.ids, updated.data.sharedTo.ids)
            Assertions.assertEquals(original.data.uploadedResources, updated.data.uploadedResources)
            when (val content = original.data.content) {
                is TaskContent.New -> Assertions.assertInstanceOf(TaskContent.New::class.java, updated.data.content)
                is TaskContent.Uncommitted -> assertCommittedUnchanged(content.lastCommitted, updated)
                is TaskContent.Committed -> assertCommittedUnchanged(content.lastCommitted, updated)
            }
        }
    }

    @Nested
    inner class UpdateTestTests {

        private val bucket = VersionBucket(UUID(0, 0))
        private val resourceId = TestId(21)
        private val originalResource = resource(21)

        @Test
        fun `should reject update before accessing storage if user is not a Developer`() {
            val user = testAdministrator {}

            assertRaises(MissedDeveloperRoleError) { request(user = user, file = uploadFile) }

            verify(exactly = 0) { taskRepository.findById(any()) }
            assertNoWrites()
        }

        @Test
        fun `should reject update without writes if task does not exist`() {
            every { taskRepository.findById(uploadTaskId) } returns null

            assertRaises(TaskNotExistsError(uploadTaskId)) { request(file = uploadFile) }

            assertNoWrites()
        }

        @Test
        fun `should reject update without writes if resource does not exist`() {
            prepare()
            every { testRepository.findById(resourceId) } returns null

            assertRaises(TestNotExistsError(resourceId)) { request(file = uploadFile) }

            assertNoWrites()
        }

        @Test
        fun `should reject update of another owners task even when shared`() {
            prepare(taskForUpdate("New").withData { owner(99) })

            assertRaises(TaskAccessDeniedError(uploadTaskId)) { request(file = uploadFile) }

            assertNoWrites()
        }

        @Test
        fun `should reject update without writes if chain was not uploaded to task`() {
            prepare(taskForUpdate("New").withData { uploadedResources.clear() })

            assertRaises(TestNotUploadedToTaskError(uploadTaskId, resourceId)) { request(file = uploadFile) }

            assertNoWrites()
        }

        @ParameterizedTest
        @ValueSource(strings = ["name", "file"])
        fun `should reject an older source version for every change kind`(mode: String) {
            prepare()
            every { testRepository.findLatestByVersionBucket(bucket) } returns resource(22)

            assertRaises(TestVersionNotLatestError(resourceId)) {
                request(resourceName = "renamed", file = fileForMode(mode))
            }

            assertNoWrites()
        }

        @Test
        fun `should reject update if latest lookup returns no version`() {
            prepare()
            every { testRepository.findLatestByVersionBucket(bucket) } returns null

            assertRaises(TestVersionNotLatestError(resourceId)) { request(file = uploadFile) }

            assertNoWrites()
        }

        @ParameterizedTest
        @ValueSource(strings = ["New", "Uncommitted", "Committed"])
        fun `should rename latest version in place without changing task in any state`(state: String) {
            val original = taskForUpdate(state)
            prepare(original)

            val result = request(resourceName = "renamed").getOrThrow()

            Assertions.assertEquals(resourceId, result.id)
            Assertions.assertEquals(originalResource.createdAt, result.createdAt)
            Assertions.assertEquals(originalResource.version, result.version)
            Assertions.assertEquals("renamed", result.data.name)
            Assertions.assertEquals("name", originalResource.data.name)
            assertResourceFields(result)
            verify(exactly = 1) { testRepository.update(any<Polygon>()) }
            assertNoUploadWrites()
        }

        @ParameterizedTest
        @NullSource
        @ValueSource(strings = ["name", ""])
        fun `should persist metadata without checking unchanged or omitted name`(resourceName: String?) {
            prepare()

            val result = request(resourceName = resourceName).getOrThrow()

            Assertions.assertEquals(resourceId, result.id)
            Assertions.assertEquals(resourceName ?: "name", result.data.name)
            assertResourceFields(result)
            verify(exactly = 1) { testRepository.update(any<Polygon>()) }
            assertNoUploadWrites()
        }

        @ParameterizedTest
        @ValueSource(strings = ["New", "Uncommitted", "Committed"])
        fun `should replace an older attached chain version and preserve task revisions in every state`(state: String) {
            val original = taskForUpdate(state)
            prepare(original)

            val result = request(file = uploadFile).getOrThrow()

            Assertions.assertEquals(TestId(31), result.id)
            Assertions.assertEquals("name", result.data.name)
            Assertions.assertEquals("description", result.data.description)
            Assertions.assertEquals(bucket, result.data.versionBucket)
            assertUploadedFile(result.data.file)
            assertTaskReplacement(original)
            verify(exactly = 0) { testRepository.update(any<Polygon>()) }
            Assertions.assertEquals("name", originalResource.data.name)
            Assertions.assertArrayEquals(byteArrayOf(1, 2), originalResource.data.file.content)
        }

        @Test
        fun `should give new version the supplied name without renaming the previous version`() {
            prepare()

            val result = request(resourceName = "renamed", file = uploadFile).getOrThrow()

            Assertions.assertEquals(TestId(31), result.id)
            Assertions.assertEquals("renamed", result.data.name)
            Assertions.assertEquals("name", originalResource.data.name)
            verify(exactly = 0) { testRepository.update(any<Polygon>()) }
        }

        @Test
        fun `should create another version even for identical file contents`() {
            prepare()
            val identical = FileData(uploadedFilename = "original.bin", content = byteArrayOf(1, 2))

            val result = request(file = identical).getOrThrow()

            Assertions.assertEquals(TestId(31), result.id)
            verify(exactly = 1) { testRepository.save(any<TestData>()) }
            verify(exactly = 0) { testRepository.update(any<Polygon>()) }
        }

        @Test
        fun `should leave existing WIP unchanged when chain remains only in last committed revision`() {
            val original = taskForUpdate("Uncommitted").withData {
                content.uncommitted(
                    wipBuilder = { tests(listOf(9, 11)) },
                    lastCommittedBuilder = {},
                )
            }
            prepare(original)

            val result = request(file = uploadFile).getOrThrow()

            Assertions.assertEquals(TestId(31), result.id)
            verify(exactly = 0) { taskRepository.update(any<Task>()) }
            verify(exactly = 0) { testRepository.update(any<Polygon>()) }
        }

        @ParameterizedTest
        @ValueSource(strings = ["New", "Uncommitted", "Committed"])
        fun `should save detached chain without attaching or changing task in any state`(state: String) {
            val original = taskForUpdate(state, attached = false)
            prepare(original)

            val result = request(file = uploadFile).getOrThrow()

            Assertions.assertEquals(TestId(31), result.id)
            verify(exactly = 0) { taskRepository.update(any<Task>()) }
            verify(exactly = 0) { testRepository.update(any<Polygon>()) }
        }

        private fun resource(id: Long, chain: VersionBucket = bucket): Polygon = test {
            this.id = id
            createdAt = Instant.MIN
            version = EntityVersion(0)
            data = testData {
                name = "name"
                description = "description"
                versionBucket = chain
                file("original.bin", byteArrayOf(1, 2))
            }
        }

        private fun taskForUpdate(state: String, attached: Boolean = true): Task = taskInState(state).withData {
            uploadedResources.add(bucket)
            when (state) {
                "New" -> content.new { configureContent(attached) }
                "Uncommitted" -> content.uncommitted(
                    wipBuilder = { configureContent(attached) },
                    lastCommittedBuilder = { configureContent(attached) },
                )
                "Committed" -> content.committed {
                    statement = StatementId(1)
                    exercises = mutableListOf(ExerciseId(1))
                    configureContent(attached)
                }
                else -> error("Unsupported test state: $state")
            }
        }

        private fun tech.testsys.domain.builder.task.TaskContentBuilder<*>.configureContent(attached: Boolean) {
            if (attached) {
                tests(listOf(9, 10, 11))
            } else {
                tests(listOf(9, 11))
            }
        }

        private fun prepare(original: Task = taskForUpdate("New")) {
            prepareUpload(original)
            every { testRepository.findById(resourceId) } returns originalResource
            every { testRepository.findLatestByVersionBucket(bucket) } returns originalResource
            every { testRepository.update(any<Polygon>()) } answers { firstArg<Polygon>() }
            every { testRepository.save(any<TestData>()) } answers {
                test {
                    id = 31
                    createdAt = Instant.EPOCH
                    version = EntityVersion(1)
                    data = firstArg<TestData>()
                }
            }
            every { testRepository.load(any<LazyEntityList<TestId, Polygon>>()) } answers {
                firstArg<LazyEntityList<TestId, Polygon>>().ids.map { attachedId ->
                    resource(attachedId.value, if (attachedId.value == 10L) bucket else VersionBucket(UUID(0, 9)))
                }
            }
        }

        private fun request(user: MultipleRoleUser = developer, resourceName: String? = null, file: FileData? = null) =
            developerOperations.updateTest(
                user = user,
                taskId = uploadTaskId,
                testId = resourceId,
                resourceName = resourceName,
                file = file,
            )

        private fun assertNoWrites() {
            assertNoUploadWrites()
            verify(exactly = 0) { testRepository.update(any<Polygon>()) }
        }

        private fun fileForMode(mode: String): FileData? = when (mode) {
            "file" -> uploadFile
            "name" -> null
            else -> error("Unsupported update mode: $mode")
        }

        private fun assertResourceFields(result: Polygon) {
            Assertions.assertEquals("description", result.data.description)
            Assertions.assertEquals(bucket, result.data.versionBucket)
            Assertions.assertEquals("original.bin", result.data.file.uploadedFilename)
            Assertions.assertArrayEquals(byteArrayOf(1, 2), result.data.file.content)
        }

        private fun assertTaskReplacement(original: Task) {
            val captured = slot<Task>()
            verify(exactly = 1) { taskRepository.update(capture(captured)) }
            val updated = captured.captured
            val before = original.getEditableContent()
            val after = updated.getEditableContent()
            Assertions.assertEquals(listOf(TestId(9), TestId(31), TestId(11)), after.tests.ids)
            Assertions.assertEquals(before.statement?.id, after.statement?.id)
            Assertions.assertEquals(before.exercises.ids, after.exercises.ids)
            Assertions.assertEquals(before.developerSolutions.ids, after.developerSolutions.ids)
            Assertions.assertEquals(before.supportedTrikStudioVersions, after.supportedTrikStudioVersions)
            Assertions.assertEquals(original.id, updated.id)
            Assertions.assertEquals(original.createdAt, updated.createdAt)
            Assertions.assertEquals(original.version, updated.version)
            Assertions.assertEquals(original.data.owner.id, updated.data.owner.id)
            Assertions.assertEquals(original.data.name, updated.data.name)
            Assertions.assertEquals(original.data.description, updated.data.description)
            Assertions.assertEquals(original.data.sharedTo.ids, updated.data.sharedTo.ids)
            Assertions.assertEquals(original.data.uploadedResources, updated.data.uploadedResources)
            when (val content = original.data.content) {
                is TaskContent.New -> Assertions.assertInstanceOf(TaskContent.New::class.java, updated.data.content)
                is TaskContent.Uncommitted -> assertCommittedUnchanged(content.lastCommitted, updated)
                is TaskContent.Committed -> assertCommittedUnchanged(content.lastCommitted, updated)
            }
        }
    }

    @Nested
    inner class UpdateDeveloperSolutionTests {

        private val bucket = VersionBucket(UUID(0, 0))
        private val resourceId = DeveloperSolutionId(21)
        private val originalResource = resource(21)

        @ParameterizedTest
        @NullSource
        @ValueSource(strings = ["renamed"])
        fun `should preserve or replace name when score changes without file`(resourceName: String?) {
            prepare()

            val result = request(resourceName = resourceName, expectedScore = Score(57)).getOrThrow()

            Assertions.assertEquals(DeveloperSolutionId(31), result.id)
            Assertions.assertEquals(resourceName ?: "name", result.data.name)
            Assertions.assertEquals(Score(57), result.data.expectedScore)
            Assertions.assertEquals(SolutionId(20), result.data.solution.id)
            verify(exactly = 0) { solutionRepository.save(any<SolutionData>()) }
        }

        @ParameterizedTest
        @NullSource
        @ValueSource(strings = ["renamed"])
        fun `should preserve or replace name when file and score change together`(resourceName: String?) {
            prepare()

            val result = request(resourceName = resourceName, file = uploadFile, expectedScore = Score(57)).getOrThrow()

            Assertions.assertEquals(DeveloperSolutionId(31), result.id)
            Assertions.assertEquals(resourceName ?: "name", result.data.name)
            Assertions.assertEquals(Score(57), result.data.expectedScore)
            Assertions.assertEquals(SolutionId(14), result.data.solution.id)
            val saved = slot<SolutionData>()
            verify(exactly = 1) { solutionRepository.save(capture(saved)) }
            assertUploadedFile(saved.captured.file)
        }

        @Test
        fun `should reject update before accessing storage if user is not a Developer`() {
            val user = testAdministrator {}

            assertRaises(MissedDeveloperRoleError) { request(user = user, file = uploadFile) }

            verify(exactly = 0) { taskRepository.findById(any()) }
            assertNoWrites()
        }

        @Test
        fun `should reject update without writes if task does not exist`() {
            every { taskRepository.findById(uploadTaskId) } returns null

            assertRaises(TaskNotExistsError(uploadTaskId)) { request(file = uploadFile) }

            assertNoWrites()
        }

        @Test
        fun `should reject update without writes if resource does not exist`() {
            prepare()
            every { developerSolutionRepository.findById(resourceId) } returns null

            assertRaises(DeveloperSolutionNotExistsError(resourceId)) { request(file = uploadFile) }

            assertNoWrites()
        }

        @Test
        fun `should reject update of another owners task even when shared`() {
            prepare(taskForUpdate("New").withData { owner(99) })

            assertRaises(TaskAccessDeniedError(uploadTaskId)) { request(file = uploadFile) }

            assertNoWrites()
        }

        @Test
        fun `should reject update without writes if chain was not uploaded to task`() {
            prepare(taskForUpdate("New").withData { uploadedResources.clear() })

            assertRaises(DeveloperSolutionNotUploadedToTaskError(uploadTaskId, resourceId)) { request(file = uploadFile) }

            assertNoWrites()
        }

        @ParameterizedTest
        @ValueSource(strings = ["name", "file", "score"])
        fun `should reject an older source version for every change kind`(mode: String) {
            prepare()
            every { developerSolutionRepository.findLatestByVersionBucket(bucket) } returns resource(22)

            assertRaises(DeveloperSolutionVersionNotLatestError(resourceId)) {
                request(resourceName = "renamed", file = fileForMode(mode), expectedScore = scoreForMode(mode))
            }

            assertNoWrites()
        }

        @Test
        fun `should reject update if latest lookup returns no version`() {
            prepare()
            every { developerSolutionRepository.findLatestByVersionBucket(bucket) } returns null

            assertRaises(DeveloperSolutionVersionNotLatestError(resourceId)) { request(file = uploadFile) }

            assertNoWrites()
        }

        @ParameterizedTest
        @ValueSource(strings = ["New", "Uncommitted", "Committed"])
        fun `should rename latest version in place without changing task in any state`(state: String) {
            val original = taskForUpdate(state)
            prepare(original)

            val result = request(resourceName = "renamed").getOrThrow()

            Assertions.assertEquals(resourceId, result.id)
            Assertions.assertEquals(originalResource.createdAt, result.createdAt)
            Assertions.assertEquals(originalResource.version, result.version)
            Assertions.assertEquals("renamed", result.data.name)
            Assertions.assertEquals("name", originalResource.data.name)
            assertResourceFields(result)
            verify(exactly = 1) { developerSolutionRepository.update(any<DeveloperSolution>()) }
            assertNoUploadWrites()
        }

        @ParameterizedTest
        @NullSource
        @ValueSource(strings = ["name", ""])
        fun `should persist metadata without checking unchanged or omitted name`(resourceName: String?) {
            prepare()

            val result = request(resourceName = resourceName).getOrThrow()

            Assertions.assertEquals(resourceId, result.id)
            Assertions.assertEquals(resourceName ?: "name", result.data.name)
            assertResourceFields(result)
            verify(exactly = 1) { developerSolutionRepository.update(any<DeveloperSolution>()) }
            assertNoUploadWrites()
        }

        @ParameterizedTest
        @ValueSource(strings = ["New", "Uncommitted", "Committed"])
        fun `should replace an older attached chain version and preserve task revisions in every state`(state: String) {
            val original = taskForUpdate(state)
            prepare(original)

            val result = request(file = uploadFile).getOrThrow()

            Assertions.assertEquals(DeveloperSolutionId(31), result.id)
            Assertions.assertEquals("name", result.data.name)
            Assertions.assertEquals("description", result.data.description)
            Assertions.assertEquals(bucket, result.data.versionBucket)
            Assertions.assertEquals(SolutionId(14), result.data.solution.id)
            Assertions.assertEquals(Score(42), result.data.expectedScore)
            assertTaskReplacement(original)
            verify(exactly = 0) { developerSolutionRepository.update(any<DeveloperSolution>()) }
            Assertions.assertEquals("name", originalResource.data.name)
            Assertions.assertEquals(SolutionId(20), originalResource.data.solution.id)
        }

        @Test
        fun `should give new version the supplied name without renaming the previous version`() {
            prepare()

            val result = request(resourceName = "renamed", file = uploadFile).getOrThrow()

            Assertions.assertEquals(DeveloperSolutionId(31), result.id)
            Assertions.assertEquals("renamed", result.data.name)
            Assertions.assertEquals("name", originalResource.data.name)
            verify(exactly = 0) { developerSolutionRepository.update(any<DeveloperSolution>()) }
        }

        @Test
        fun `should create another version even for identical file contents`() {
            prepare()
            val identical = FileData(uploadedFilename = "original.bin", content = byteArrayOf(1, 2))

            val result = request(file = identical).getOrThrow()

            Assertions.assertEquals(DeveloperSolutionId(31), result.id)
            verify(exactly = 1) { developerSolutionRepository.save(any<DeveloperSolutionData>()) }
            verify(exactly = 0) { developerSolutionRepository.update(any<DeveloperSolution>()) }
        }

        @Test
        fun `should leave existing WIP unchanged when chain remains only in last committed revision`() {
            val original = taskForUpdate("Uncommitted").withData {
                content.uncommitted(
                    wipBuilder = { developerSolutions(listOf(9, 11)) },
                    lastCommittedBuilder = {},
                )
            }
            prepare(original)

            val result = request(file = uploadFile).getOrThrow()

            Assertions.assertEquals(DeveloperSolutionId(31), result.id)
            verify(exactly = 0) { taskRepository.update(any<Task>()) }
            verify(exactly = 0) { developerSolutionRepository.update(any<DeveloperSolution>()) }
        }

        @ParameterizedTest
        @ValueSource(strings = ["New", "Uncommitted", "Committed"])
        fun `should save detached chain without attaching or changing task in any state`(state: String) {
            val original = taskForUpdate(state, attached = false)
            prepare(original)

            val result = request(file = uploadFile).getOrThrow()

            Assertions.assertEquals(DeveloperSolutionId(31), result.id)
            verify(exactly = 0) { taskRepository.update(any<Task>()) }
            verify(exactly = 0) { developerSolutionRepository.update(any<DeveloperSolution>()) }
        }

        @ParameterizedTest
        @ValueSource(ints = [42, 57])
        fun `should create score version reusing previous solution even for identical score`(score: Int) {
            val original = taskForUpdate("Committed")
            prepare(original)

            val result = request(expectedScore = Score(score)).getOrThrow()

            Assertions.assertEquals(DeveloperSolutionId(31), result.id)
            Assertions.assertEquals(Score(score), result.data.expectedScore)
            Assertions.assertEquals(SolutionId(20), result.data.solution.id)
            Assertions.assertEquals(Score(42), originalResource.data.expectedScore)
            verify(exactly = 0) { solutionRepository.save(any<SolutionData>()) }
            verify(exactly = 0) { solutionRepository.load(any<LazyEntity<SolutionId, Solution>>()) }
            assertTaskReplacement(original)
        }

        @ParameterizedTest
        @ValueSource(strings = ["Python", "JavaScript", "VisualLanguage"])
        fun `should preserve language when saving new solution with file name and score together`(language: String) {
            prepare()
            every { solutionRepository.load(originalResource.data.solution) } returns solution {
                id = 20
                createdAt = Instant.MIN
                data = solutionData {
                    file("original.bin", byteArrayOf(1, 2))
                    when (uploadLanguage(language)) {
                        TrikSupportedLanguage.Python -> this.language.python()
                        TrikSupportedLanguage.JavaScript -> this.language.javaScript()
                        TrikSupportedLanguage.VisualLanguage -> this.language.visualLanguage()
                    }
                }
            }

            val result = request(resourceName = "renamed", file = uploadFile, expectedScore = Score(57)).getOrThrow()

            Assertions.assertEquals(DeveloperSolutionId(31), result.id)
            Assertions.assertEquals("renamed", result.data.name)
            Assertions.assertEquals(Score(57), result.data.expectedScore)
            Assertions.assertEquals(SolutionId(14), result.data.solution.id)
            val saved = slot<SolutionData>()
            verify(exactly = 1) { solutionRepository.save(capture(saved)) }
            Assertions.assertEquals(uploadLanguage(language), saved.captured.language)
            assertUploadedFile(saved.captured.file)
            Assertions.assertEquals(Score(42), originalResource.data.expectedScore)
        }

        @Test
        fun `should save detached score version without attaching it to WIP`() {
            prepare(
                taskForUpdate("Uncommitted").withData {
                    content.uncommitted(wipBuilder = { developerSolutions.clear() }, lastCommittedBuilder = {})
                },
            )

            val result = request(expectedScore = Score(57)).getOrThrow()

            Assertions.assertEquals(Score(57), result.data.expectedScore)
            verify(exactly = 0) { taskRepository.update(any<Task>()) }
            verify(exactly = 0) { solutionRepository.save(any<SolutionData>()) }
        }

        private fun resource(id: Long, chain: VersionBucket = bucket): DeveloperSolution = developerSolution {
            this.id = id
            createdAt = Instant.MIN
            version = EntityVersion(0)
            data = developerSolutionData {
                name = "name"
                description = "description"
                versionBucket = chain
                solution(20)
                expectedScore(42)
            }
        }

        private fun taskForUpdate(state: String, attached: Boolean = true): Task = taskInState(state).withData {
            uploadedResources.add(bucket)
            when (state) {
                "New" -> content.new { configureContent(attached) }
                "Uncommitted" -> content.uncommitted(
                    wipBuilder = { configureContent(attached) },
                    lastCommittedBuilder = { configureContent(attached) },
                )
                "Committed" -> content.committed {
                    statement = StatementId(1)
                    exercises = mutableListOf(ExerciseId(1))
                    configureContent(attached)
                }
                else -> error("Unsupported test state: $state")
            }
        }

        private fun tech.testsys.domain.builder.task.TaskContentBuilder<*>.configureContent(attached: Boolean) {
            if (attached) {
                developerSolutions(listOf(9, 10, 11))
            } else {
                developerSolutions(listOf(9, 11))
            }
        }

        private fun prepare(original: Task = taskForUpdate("New")) {
            prepareUpload(original)
            every { developerSolutionRepository.findById(resourceId) } returns originalResource
            every { developerSolutionRepository.findLatestByVersionBucket(bucket) } returns originalResource
            every { developerSolutionRepository.update(any<DeveloperSolution>()) } answers { firstArg<DeveloperSolution>() }
            every { developerSolutionRepository.save(any<DeveloperSolutionData>()) } answers {
                developerSolution {
                    id = 31
                    createdAt = Instant.EPOCH
                    version = EntityVersion(1)
                    data = firstArg<DeveloperSolutionData>()
                }
            }
            every { developerSolutionRepository.load(any<LazyEntityList<DeveloperSolutionId, DeveloperSolution>>()) } answers {
                firstArg<LazyEntityList<DeveloperSolutionId, DeveloperSolution>>().ids.map { attachedId ->
                    resource(attachedId.value, if (attachedId.value == 10L) bucket else VersionBucket(UUID(0, 9)))
                }
            }
            every { solutionRepository.load(originalResource.data.solution) } returns solution {
                id = 20
                createdAt = Instant.MIN
                data = solutionData {
                    file("original.bin", byteArrayOf(1, 2))
                    language.python()
                }
            }
        }

        private fun request(
            user: MultipleRoleUser = developer,
            resourceName: String? = null,
            file: FileData? = null,
            expectedScore: Score? = null,
        ) = developerOperations.updateDeveloperSolution(
            user = user,
            taskId = uploadTaskId,
            developerSolutionId = resourceId,
            resourceName = resourceName,
            file = file,
            expectedScore = expectedScore,
        )

        private fun assertNoWrites() {
            assertNoUploadWrites()
            verify(exactly = 0) { developerSolutionRepository.update(any<DeveloperSolution>()) }
        }

        private fun fileForMode(mode: String): FileData? = when (mode) {
            "file" -> uploadFile
            "name", "score" -> null
            else -> error("Unsupported update mode: $mode")
        }

        private fun scoreForMode(mode: String): Score? = when (mode) {
            "score" -> Score(42)
            "name", "file" -> null
            else -> error("Unsupported update mode: $mode")
        }

        private fun assertResourceFields(result: DeveloperSolution) {
            Assertions.assertEquals("description", result.data.description)
            Assertions.assertEquals(bucket, result.data.versionBucket)
            Assertions.assertEquals(SolutionId(20), result.data.solution.id)
            Assertions.assertEquals(Score(42), result.data.expectedScore)
        }

        private fun assertTaskReplacement(original: Task) {
            val captured = slot<Task>()
            verify(exactly = 1) { taskRepository.update(capture(captured)) }
            val updated = captured.captured
            val before = original.getEditableContent()
            val after = updated.getEditableContent()
            Assertions.assertEquals(
                listOf(DeveloperSolutionId(9), DeveloperSolutionId(31), DeveloperSolutionId(11)),
                after.developerSolutions.ids,
            )
            Assertions.assertEquals(before.statement?.id, after.statement?.id)
            Assertions.assertEquals(before.exercises.ids, after.exercises.ids)
            Assertions.assertEquals(before.tests.ids, after.tests.ids)
            Assertions.assertEquals(before.supportedTrikStudioVersions, after.supportedTrikStudioVersions)
            Assertions.assertEquals(original.id, updated.id)
            Assertions.assertEquals(original.createdAt, updated.createdAt)
            Assertions.assertEquals(original.version, updated.version)
            Assertions.assertEquals(original.data.owner.id, updated.data.owner.id)
            Assertions.assertEquals(original.data.name, updated.data.name)
            Assertions.assertEquals(original.data.description, updated.data.description)
            Assertions.assertEquals(original.data.sharedTo.ids, updated.data.sharedTo.ids)
            Assertions.assertEquals(original.data.uploadedResources, updated.data.uploadedResources)
            when (val content = original.data.content) {
                is TaskContent.New -> Assertions.assertInstanceOf(TaskContent.New::class.java, updated.data.content)
                is TaskContent.Uncommitted -> assertCommittedUnchanged(content.lastCommitted, updated)
                is TaskContent.Committed -> assertCommittedUnchanged(content.lastCommitted, updated)
            }
        }
    }

    @Nested
    inner class ShareTaskTests {
        val taskId = TaskId(0L)
        val firstCommunityId = CommunityId(1L)
        val secondCommunityId = CommunityId(2L)
        val foreignCommunityId = CommunityId(3L)

        @BeforeEach
        fun beforeEach() {
            developer = testDeveloper {
                memberOf(listOf(1L, 2L))
                data = developerData { }
            }
            every { communityRepository.findById(eq(firstCommunityId)) } answers { testCommunity(1L) }
            every { communityRepository.findById(eq(secondCommunityId)) } answers { testCommunity(2L) }
            every { taskRepository.update(any<Task>()) } answers { testSavedTask(firstArg()) }
        }

        @Test
        fun `should raise MissedDeveloperRoleError if user is not a Developer`() {
            val nonDeveloper = testAdministrator { }

            assertRaises(MissedDeveloperRoleError) {
                developerOperations.shareTask(nonDeveloper, taskId, setOf(firstCommunityId))
            }
        }

        @Test
        fun `should raise TaskNotExistsError if task not exists`() {
            every { taskRepository.findById(any()) } answers { null }

            assertRaises(TaskNotExistsError(taskId)) {
                developerOperations.shareTask(developer, taskId, setOf(firstCommunityId))
            }
        }

        @Test
        fun `should raise CommunityNotExistsError if community not exists`() {
            every { taskRepository.findById(eq(taskId)) } answers { testCommitedTask() }
            every { communityRepository.findById(eq(foreignCommunityId)) } answers { null }

            assertRaises(CommunityNotExistsError(foreignCommunityId)) {
                developerOperations.shareTask(developer, taskId, setOf(firstCommunityId, foreignCommunityId))
            }
        }

        @Test
        fun `should raise TaskAccessDeniedError if task is owned by another user`() {
            every { taskRepository.findById(eq(taskId)) } answers {
                testCommitedTask().withData { owner = MultipleRoleUserId(1L) }
            }

            assertRaises(TaskAccessDeniedError(taskId)) {
                developerOperations.shareTask(developer, taskId, setOf(firstCommunityId))
            }
        }

        @Test
        fun `should raise TaskNotCommittedError if task is New`() {
            every { taskRepository.findById(eq(taskId)) } answers { testNewTask() }

            assertRaises(TaskNotCommittedError(taskId)) {
                developerOperations.shareTask(developer, taskId, setOf(firstCommunityId))
            }
        }

        @Test
        fun `should raise CommunityAccessDeniedError if developer is not a member of new community`() {
            every { taskRepository.findById(eq(taskId)) } answers { testCommitedTask() }
            every { communityRepository.findById(eq(foreignCommunityId)) } answers { testCommunity(3L) }

            assertRaises(CommunityAccessDeniedError(foreignCommunityId)) {
                developerOperations.shareTask(developer, taskId, setOf(firstCommunityId, foreignCommunityId))
            }
        }

        @Test
        fun `should raise CommunityAccessDeniedError if developer is not a member of new community and task is New`() {
            every { taskRepository.findById(eq(taskId)) } answers { testNewTask() }
            every { communityRepository.findById(eq(foreignCommunityId)) } answers { testCommunity(3L) }

            assertRaises(CommunityAccessDeniedError(foreignCommunityId)) {
                developerOperations.shareTask(developer, taskId, setOf(foreignCommunityId))
            }
        }

        @Test
        fun `should share task to chosen communities`() {
            every { taskRepository.findById(eq(taskId)) } answers { testCommitedTask() }

            val result = developerOperations.shareTask(developer, taskId, setOf(firstCommunityId, secondCommunityId))
                .getOrThrow()

            Assertions.assertEquals(listOf(firstCommunityId, secondCommunityId), result.data.sharedTo.ids)
        }

        @Test
        fun `should update task`() {
            every { taskRepository.findById(eq(taskId)) } answers { testCommitedTask() }

            developerOperations.shareTask(developer, taskId, setOf(firstCommunityId)).getOrThrow()

            verify(exactly = 1) { taskRepository.update(any<Task>()) }
        }

        @Test
        fun `should add chosen communities to previously shared communities`() {
            every { taskRepository.findById(eq(taskId)) } answers {
                testCommitedTask().withData { sharedTo = mutableListOf(firstCommunityId) }
            }

            val result = developerOperations.shareTask(developer, taskId, setOf(secondCommunityId)).getOrThrow()

            Assertions.assertEquals(listOf(firstCommunityId, secondCommunityId), result.data.sharedTo.ids)
        }

        @Test
        fun `should keep shared communities unchanged when chosen set is empty`() {
            every { taskRepository.findById(eq(taskId)) } answers {
                testCommitedTask().withData { sharedTo = mutableListOf(firstCommunityId) }
            }

            val result = developerOperations.shareTask(developer, taskId, emptySet()).getOrThrow()

            Assertions.assertEquals(listOf(firstCommunityId), result.data.sharedTo.ids)
        }

        @Test
        fun `should not duplicate community when it is already shared`() {
            every { taskRepository.findById(eq(taskId)) } answers {
                testCommitedTask().withData { sharedTo = mutableListOf(firstCommunityId) }
            }

            val result = developerOperations.shareTask(developer, taskId, setOf(firstCommunityId)).getOrThrow()

            Assertions.assertEquals(listOf(firstCommunityId), result.data.sharedTo.ids)
        }

        @Test
        fun `should allow previously shared community if developer is not its member`() {
            every { taskRepository.findById(eq(taskId)) } answers {
                testCommitedTask().withData { sharedTo = mutableListOf(foreignCommunityId) }
            }
            every { communityRepository.findById(eq(foreignCommunityId)) } answers { testCommunity(3L) }

            val result = developerOperations.shareTask(developer, taskId, setOf(foreignCommunityId, firstCommunityId))
                .getOrThrow()

            Assertions.assertEquals(listOf(foreignCommunityId, firstCommunityId), result.data.sharedTo.ids)
        }

        @Test
        fun `should keep Uncommitted task content when task is shared`() {
            every { taskRepository.findById(eq(taskId)) } answers { testUncommittedTask() }

            val result = developerOperations.shareTask(developer, taskId, setOf(firstCommunityId)).getOrThrow()

            val content = Assertions.assertInstanceOf(TaskContent.Uncommitted::class.java, result.data.content)
            Assertions.assertEquals(listOf(ExerciseId(1L)), content.lastCommitted.exercises.ids)
            Assertions.assertEquals(StatementId(1L), content.lastCommitted.statement.id)
            Assertions.assertNull(content.wip.statement)
        }

        @Test
        fun `should keep Committed task content when task is shared`() {
            every { taskRepository.findById(eq(taskId)) } answers { testCommitedTask() }

            val result = developerOperations.shareTask(developer, taskId, setOf(firstCommunityId)).getOrThrow()

            val content = Assertions.assertInstanceOf(TaskContent.Committed::class.java, result.data.content)
            Assertions.assertEquals(listOf(ExerciseId(1L)), content.lastCommitted.exercises.ids)
            Assertions.assertEquals(StatementId(1L), content.lastCommitted.statement.id)
        }

        @Test
        fun `should return the task with the version assigned on update`() {
            every { taskRepository.findById(eq(taskId)) } answers { testCommitedTask() }

            val result = developerOperations.shareTask(developer, taskId, setOf(firstCommunityId)).getOrThrow()

            Assertions.assertEquals(savedTaskVersion, result.version)
        }
    }

    @Nested
    inner class AttachExerciseTests {
        private val taskId = TaskId(1)
        private val resourceId = ExerciseId(2)
        private val bucket = VersionBucket(UUID(0, 2))

        private fun resource(id: Long = 2, versionBucket: VersionBucket = bucket) = exercise {
            this.id = id
            createdAt = Instant.EPOCH
            data = exerciseData {
                name = "Resource"
                description = "Description"
                this.versionBucket = versionBucket
                language.python()
                file("exercise.qrs", byteArrayOf(1))
            }
        }

        @BeforeEach
        fun prepareResource() {
            every { taskRepository.findById(taskId) } returns testNewTask().withData { uploadedResources.add(bucket) }
            every { exerciseRepository.findById(resourceId) } returns resource()
            every { exerciseRepository.findLatestByVersionBucket(bucket) } returns resource()
            every { exerciseRepository.load(any<LazyEntityList<ExerciseId, Exercise>>()) } answers {
                firstArg<LazyEntityList<ExerciseId, Exercise>>().ids.map { attachedId ->
                    resource(attachedId.value, VersionBucket(UUID(0, 3)))
                }
            }
            every { taskRepository.update(any<Task>()) } answers { testSavedTask(firstArg()) }
        }

        @Test
        fun `should reject a user without the Developer role`() {
            val user = testAdministrator {}

            assertRaises(MissedDeveloperRoleError) { developerOperations.attachExercise(user, taskId, resourceId) }
            verify(exactly = 0) { taskRepository.update(any<Task>()) }
        }

        @Test
        fun `should reject a missing task`() {
            every { taskRepository.findById(taskId) } returns null

            assertRaises(TaskNotExistsError(taskId)) { developerOperations.attachExercise(developer, taskId, resourceId) }
            verify(exactly = 0) { taskRepository.update(any<Task>()) }
        }

        @Test
        fun `should reject a missing resource version`() {
            every { exerciseRepository.findById(resourceId) } returns null

            assertRaises(ExerciseNotExistsError(resourceId)) { developerOperations.attachExercise(developer, taskId, resourceId) }
            verify(exactly = 0) { taskRepository.update(any<Task>()) }
        }

        @Test
        fun `should reject a task owned by another developer`() {
            every { taskRepository.findById(taskId) } returns testNewTask().withData { owner(9) }

            assertRaises(TaskAccessDeniedError(taskId)) { developerOperations.attachExercise(developer, taskId, resourceId) }
            verify(exactly = 0) { taskRepository.update(any<Task>()) }
        }

        @Test
        fun `should reject a chain not uploaded to the target task`() {
            every { taskRepository.findById(taskId) } returns testNewTask()

            assertRaises(ExerciseNotUploadedToTaskError(taskId, resourceId)) {
                developerOperations.attachExercise(developer, taskId, resourceId)
            }
            verify(exactly = 0) { taskRepository.update(any<Task>()) }
        }

        @Test
        fun `should reject an older resource version without substituting the latest`() {
            every { exerciseRepository.findLatestByVersionBucket(bucket) } returns resource(3)

            assertRaises(ExerciseVersionNotLatestError(resourceId)) {
                developerOperations.attachExercise(developer, taskId, resourceId)
            }
            verify(exactly = 0) { taskRepository.update(any<Task>()) }
        }

        @Test
        fun `should reject another version of an already attached chain`() {
            every { taskRepository.findById(taskId) } returns testNewTask().withData {
                uploadedResources.add(bucket)
                content.new { exercises(listOf(3, 1)) }
            }
            every { exerciseRepository.load(any<LazyEntityList<ExerciseId, Exercise>>()) } returns listOf(
                resource(3, VersionBucket(UUID(0, 3))).withData { language.javaScript() },
                resource(1),
            )

            assertRaises(ResourceAlreadyAttachedError(taskId, bucket)) {
                developerOperations.attachExercise(developer, taskId, resourceId)
            }
            verify(exactly = 0) { taskRepository.update(any<Task>()) }
        }

        @ParameterizedTest
        @ValueSource(strings = ["New", "Uncommitted"])
        fun `should attach the latest version while preserving existing content in every state`(state: String) {
            val original = taskInState(state).withData { uploadedResources.add(bucket) }
            every { taskRepository.findById(taskId) } returns original

            val result = developerOperations.attachExercise(developer, taskId, resourceId).getOrThrow()

            assertAttachedContent(original, result, resourceId)
            verify(exactly = 1) { taskRepository.update(any<Task>()) }
        }

        private fun assertAttachedContent(original: Task, result: Task, attachedId: ExerciseId) {
            val before = original.getEditableContent()
            val after = result.getEditableContent()
            Assertions.assertEquals(before.exercises.ids + attachedId, after.exercises.ids)
            Assertions.assertEquals(before.tests.ids, after.tests.ids)
            Assertions.assertEquals(before.developerSolutions.ids, after.developerSolutions.ids)
            Assertions.assertEquals(before.statement?.id, after.statement?.id)
            Assertions.assertEquals(before.supportedTrikStudioVersions, after.supportedTrikStudioVersions)
            Assertions.assertEquals(original.data.uploadedResources, result.data.uploadedResources)
            Assertions.assertEquals(original.data.owner.id, result.data.owner.id)
            Assertions.assertEquals(original.data.name, result.data.name)
            Assertions.assertEquals(original.data.description, result.data.description)
            Assertions.assertEquals(original.data.sharedTo.ids, result.data.sharedTo.ids)
            Assertions.assertEquals(savedTaskVersion, result.version)
            when (val content = original.data.content) {
                is TaskContent.New -> Assertions.assertInstanceOf(TaskContent.New::class.java, result.data.content)
                is TaskContent.Uncommitted -> assertCommittedUnchanged(content.lastCommitted, result)
                is TaskContent.Committed -> assertCommittedUnchanged(content.lastCommitted, result)
            }
        }

        @Test
        fun `should reject another exercise for the same language`() {
            every { taskRepository.findById(taskId) } returns testNewTask().withData {
                uploadedResources.add(bucket)
                content.new { exercises(listOf(1)) }
            }
            every { exerciseRepository.load(any<LazyEntityList<ExerciseId, Exercise>>()) } returns
                listOf(resource(1, VersionBucket(UUID(0, 3))))

            assertRaises(ExerciseLanguageAlreadyAttachedError(taskId = taskId, language = TrikSupportedLanguage.Python)) {
                developerOperations.attachExercise(developer, taskId, resourceId)
            }
            verify(exactly = 0) { taskRepository.update(any<Task>()) }
        }

        @ParameterizedTest
        @ValueSource(strings = ["New", "Uncommitted", "Committed"])
        fun `should attach another exercise for a different language in every state`(state: String) {
            val existing = resource(1, VersionBucket(UUID(0, 3))).withData { language.javaScript() }
            val original = taskInState(state).withData {
                uploadedResources.add(bucket)
                when (state) {
                    "New" -> content.new { exercises(listOf(1)) }
                    "Uncommitted" -> content.uncommitted(
                        wipBuilder = { exercises(listOf(1)) },
                        lastCommittedBuilder = {},
                    )
                    "Committed" -> content.committed { exercises(listOf(1)) }
                    else -> error("Unsupported test state: $state")
                }
            }
            every { taskRepository.findById(taskId) } returns original
            every { exerciseRepository.load(any<LazyEntityList<ExerciseId, Exercise>>()) } returns listOf(existing)

            val result = developerOperations.attachExercise(developer, taskId, resourceId).getOrThrow()

            assertAttachedContent(original, result, resourceId)
            verify(exactly = 1) { taskRepository.update(any<Task>()) }
        }

        @Test
        fun `should reject an occupied exercise slot in a Committed task`() {
            val original = taskInState("Committed").withData { uploadedResources.add(bucket) }
            every { taskRepository.findById(taskId) } returns original

            assertRaises(ExerciseLanguageAlreadyAttachedError(taskId = taskId, language = TrikSupportedLanguage.Python)) {
                developerOperations.attachExercise(developer, taskId, resourceId)
            }
            Assertions.assertInstanceOf(TaskContent.Committed::class.java, original.data.content)
            verify(exactly = 0) { taskRepository.update(any<Task>()) }
        }

        @Test
        fun `should reject an attached exercise chain in a Committed task`() {
            val original = testCommitedTask().withData { uploadedResources.add(bucket) }
            every { taskRepository.findById(taskId) } returns original
            every { exerciseRepository.load(any<LazyEntityList<ExerciseId, Exercise>>()) } returns listOf(resource(1))

            assertRaises(ResourceAlreadyAttachedError(taskId, bucket)) {
                developerOperations.attachExercise(developer, taskId, resourceId)
            }
            Assertions.assertInstanceOf(TaskContent.Committed::class.java, original.data.content)
            verify(exactly = 0) { taskRepository.update(any<Task>()) }
        }

        @Test
        fun `should attach a third exercise when both other programming languages are already present`() {
            val original = testNewTask().withData {
                uploadedResources.add(bucket)
                content.new { exercises(listOf(1, 3)) }
            }
            every { taskRepository.findById(taskId) } returns original
            every { exerciseRepository.load(any<LazyEntityList<ExerciseId, Exercise>>()) } returns listOf(
                resource(1, VersionBucket(UUID(0, 3))).withData { language.javaScript() },
                resource(3, VersionBucket(UUID(0, 4))).withData { language.visualLanguage() },
            )

            val result = developerOperations.attachExercise(developer, taskId, resourceId).getOrThrow()

            assertAttachedContent(original, result, resourceId)
            Assertions.assertEquals(listOf(ExerciseId(1), ExerciseId(3), resourceId), result.getEditableContent().exercises.ids)
        }

        @Test
        fun `should attach an archive as one exercise resource`() {
            val archive = resource().withData { file("exercises.zip", byteArrayOf(1, 2)) }
            every { exerciseRepository.findById(resourceId) } returns archive
            every { exerciseRepository.findLatestByVersionBucket(bucket) } returns archive

            val result = developerOperations.attachExercise(developer, taskId, resourceId).getOrThrow()

            Assertions.assertEquals(listOf(resourceId), result.getEditableContent().exercises.ids)
            verify(exactly = 1) { taskRepository.update(any<Task>()) }
        }
    }

    @Nested
    inner class AttachTestTests {
        private val taskId = TaskId(1)
        private val resourceId = TestId(2)
        private val bucket = VersionBucket(UUID(0, 2))

        private fun resource(id: Long = 2, versionBucket: VersionBucket = bucket) = test {
            this.id = id
            createdAt = Instant.EPOCH
            data = testData {
                name = "Resource"
                description = "Description"
                this.versionBucket = versionBucket
                file("test.xml", byteArrayOf(1))
            }
        }

        @BeforeEach
        fun prepareResource() {
            every { taskRepository.findById(taskId) } returns testNewTask().withData { uploadedResources.add(bucket) }
            every { testRepository.findById(resourceId) } returns resource()
            every { testRepository.findLatestByVersionBucket(bucket) } returns resource()
            every { testRepository.load(any<LazyEntityList<TestId, Polygon>>()) } answers {
                firstArg<LazyEntityList<TestId, Polygon>>().ids.map { referenceId ->
                    resource(referenceId.value, VersionBucket(UUID(0, 3)))
                }
            }
            every { taskRepository.update(any<Task>()) } answers { testSavedTask(firstArg()) }
        }

        @Test
        fun `should reject a user without the Developer role`() {
            val user = testAdministrator {}

            assertRaises(MissedDeveloperRoleError) { developerOperations.attachTest(user, taskId, resourceId) }
            verify(exactly = 0) { taskRepository.update(any<Task>()) }
        }

        @Test
        fun `should reject a missing task`() {
            every { taskRepository.findById(taskId) } returns null

            assertRaises(TaskNotExistsError(taskId)) { developerOperations.attachTest(developer, taskId, resourceId) }
            verify(exactly = 0) { taskRepository.update(any<Task>()) }
        }

        @Test
        fun `should reject a missing resource version`() {
            every { testRepository.findById(resourceId) } returns null

            assertRaises(TestNotExistsError(resourceId)) { developerOperations.attachTest(developer, taskId, resourceId) }
            verify(exactly = 0) { taskRepository.update(any<Task>()) }
        }

        @Test
        fun `should reject a task owned by another developer`() {
            every { taskRepository.findById(taskId) } returns testNewTask().withData { owner(9) }

            assertRaises(TaskAccessDeniedError(taskId)) { developerOperations.attachTest(developer, taskId, resourceId) }
            verify(exactly = 0) { taskRepository.update(any<Task>()) }
        }

        @Test
        fun `should reject a chain not uploaded to the target task`() {
            every { taskRepository.findById(taskId) } returns testNewTask()

            assertRaises(TestNotUploadedToTaskError(taskId, resourceId)) {
                developerOperations.attachTest(developer, taskId, resourceId)
            }
            verify(exactly = 0) { taskRepository.update(any<Task>()) }
        }

        @Test
        fun `should reject an older resource version without substituting the latest`() {
            every { testRepository.findLatestByVersionBucket(bucket) } returns resource(3)

            assertRaises(TestVersionNotLatestError(resourceId)) {
                developerOperations.attachTest(developer, taskId, resourceId)
            }
            verify(exactly = 0) { taskRepository.update(any<Task>()) }
        }

        @Test
        fun `should reject another version of an already attached chain`() {
            every { taskRepository.findById(taskId) } returns testNewTask().withData {
                uploadedResources.add(bucket)
                content.new { tests(listOf(1)) }
            }
            every { testRepository.load(any<LazyEntityList<TestId, Polygon>>()) } returns
                listOf(resource(1))

            assertRaises(ResourceAlreadyAttachedError(taskId, bucket)) {
                developerOperations.attachTest(developer, taskId, resourceId)
            }
            verify(exactly = 0) { taskRepository.update(any<Task>()) }
        }

        @ParameterizedTest
        @ValueSource(strings = ["New", "Uncommitted", "Committed"])
        fun `should attach the latest version while preserving existing content in every state`(state: String) {
            val original = taskInState(state).withData { uploadedResources.add(bucket) }
            every { taskRepository.findById(taskId) } returns original

            val result = developerOperations.attachTest(developer, taskId, resourceId).getOrThrow()

            assertAttachedContent(original, result, resourceId)
            verify(exactly = 1) { taskRepository.update(any<Task>()) }
        }

        private fun assertAttachedContent(original: Task, result: Task, attachedId: TestId) {
            val before = original.getEditableContent()
            val after = result.getEditableContent()
            Assertions.assertEquals(before.tests.ids + attachedId, after.tests.ids)
            Assertions.assertEquals(before.exercises.ids, after.exercises.ids)
            Assertions.assertEquals(before.developerSolutions.ids, after.developerSolutions.ids)
            Assertions.assertEquals(before.statement?.id, after.statement?.id)
            Assertions.assertEquals(before.supportedTrikStudioVersions, after.supportedTrikStudioVersions)
            Assertions.assertEquals(original.data.uploadedResources, result.data.uploadedResources)
            Assertions.assertEquals(original.data.owner.id, result.data.owner.id)
            Assertions.assertEquals(original.data.name, result.data.name)
            Assertions.assertEquals(original.data.description, result.data.description)
            Assertions.assertEquals(original.data.sharedTo.ids, result.data.sharedTo.ids)
            Assertions.assertEquals(savedTaskVersion, result.version)
            when (val content = original.data.content) {
                is TaskContent.New -> Assertions.assertInstanceOf(TaskContent.New::class.java, result.data.content)
                is TaskContent.Uncommitted -> assertCommittedUnchanged(content.lastCommitted, result)
                is TaskContent.Committed -> assertCommittedUnchanged(content.lastCommitted, result)
            }
        }

        @Test
        fun `should attach another resource with no count limit`() {
            every { taskRepository.findById(taskId) } returns testNewTask().withData {
                uploadedResources.add(bucket)
                content.new { tests(listOf(1)) }
            }
            every { testRepository.load(any<LazyEntityList<TestId, Polygon>>()) } returns
                listOf(resource(1, VersionBucket(UUID(0, 3))))

            val result = developerOperations.attachTest(developer, taskId, resourceId).getOrThrow()

            Assertions.assertEquals(listOf(TestId(1), TestId(2)), result.getEditableContent().tests.ids)
        }
    }

    @Nested
    inner class AttachDeveloperSolutionTests {
        private val taskId = TaskId(1)
        private val resourceId = DeveloperSolutionId(2)
        private val bucket = VersionBucket(UUID(0, 2))

        private fun resource(id: Long = 2, versionBucket: VersionBucket = bucket) = developerSolution {
            this.id = id
            createdAt = Instant.EPOCH
            data = developerSolutionData {
                name = "Resource"
                description = "Description"
                this.versionBucket = versionBucket
                solution(1)
                expectedScore(100)
            }
        }

        @BeforeEach
        fun prepareResource() {
            every { taskRepository.findById(taskId) } returns testNewTask().withData { uploadedResources.add(bucket) }
            every { developerSolutionRepository.findById(resourceId) } returns resource()
            every { developerSolutionRepository.findLatestByVersionBucket(bucket) } returns resource()
            every { developerSolutionRepository.load(any<LazyEntityList<DeveloperSolutionId, DeveloperSolution>>()) } answers {
                firstArg<LazyEntityList<DeveloperSolutionId, DeveloperSolution>>().ids.map { referenceId ->
                    resource(referenceId.value, VersionBucket(UUID(0, 3)))
                }
            }
            every { taskRepository.update(any<Task>()) } answers { testSavedTask(firstArg()) }
        }

        @Test
        fun `should reject a user without the Developer role`() {
            val user = testAdministrator {}

            assertRaises(MissedDeveloperRoleError) {
                developerOperations.attachDeveloperSolution(user, taskId, resourceId)
            }
            verify(exactly = 0) { taskRepository.update(any<Task>()) }
        }

        @Test
        fun `should reject a missing task`() {
            every { taskRepository.findById(taskId) } returns null

            assertRaises(TaskNotExistsError(taskId)) {
                developerOperations.attachDeveloperSolution(developer, taskId, resourceId)
            }
            verify(exactly = 0) { taskRepository.update(any<Task>()) }
        }

        @Test
        fun `should reject a missing resource version`() {
            every { developerSolutionRepository.findById(resourceId) } returns null

            assertRaises(DeveloperSolutionNotExistsError(resourceId)) {
                developerOperations.attachDeveloperSolution(developer, taskId, resourceId)
            }
            verify(exactly = 0) { taskRepository.update(any<Task>()) }
        }

        @Test
        fun `should reject a task owned by another developer`() {
            every { taskRepository.findById(taskId) } returns testNewTask().withData { owner(9) }

            assertRaises(TaskAccessDeniedError(taskId)) {
                developerOperations.attachDeveloperSolution(developer, taskId, resourceId)
            }
            verify(exactly = 0) { taskRepository.update(any<Task>()) }
        }

        @Test
        fun `should reject a chain not uploaded to the target task`() {
            every { taskRepository.findById(taskId) } returns testNewTask()

            assertRaises(DeveloperSolutionNotUploadedToTaskError(taskId, resourceId)) {
                developerOperations.attachDeveloperSolution(developer, taskId, resourceId)
            }
            verify(exactly = 0) { taskRepository.update(any<Task>()) }
        }

        @Test
        fun `should reject an older resource version without substituting the latest`() {
            every { developerSolutionRepository.findLatestByVersionBucket(bucket) } returns resource(3)

            assertRaises(DeveloperSolutionVersionNotLatestError(resourceId)) {
                developerOperations.attachDeveloperSolution(developer, taskId, resourceId)
            }
            verify(exactly = 0) { taskRepository.update(any<Task>()) }
        }

        @Test
        fun `should reject another version of an already attached chain`() {
            every { taskRepository.findById(taskId) } returns testNewTask().withData {
                uploadedResources.add(bucket)
                content.new { developerSolutions(listOf(1)) }
            }
            every { developerSolutionRepository.load(any<LazyEntityList<DeveloperSolutionId, DeveloperSolution>>()) } returns
                listOf(resource(1))

            assertRaises(ResourceAlreadyAttachedError(taskId, bucket)) {
                developerOperations.attachDeveloperSolution(developer, taskId, resourceId)
            }
            verify(exactly = 0) { taskRepository.update(any<Task>()) }
        }

        @ParameterizedTest
        @ValueSource(strings = ["New", "Uncommitted", "Committed"])
        fun `should attach the latest version while preserving existing content in every state`(state: String) {
            val original = taskInState(state).withData { uploadedResources.add(bucket) }
            every { taskRepository.findById(taskId) } returns original

            val result = developerOperations.attachDeveloperSolution(developer, taskId, resourceId).getOrThrow()

            assertAttachedContent(original, result, resourceId)
            verify(exactly = 1) { taskRepository.update(any<Task>()) }
        }

        private fun assertAttachedContent(original: Task, result: Task, attachedId: DeveloperSolutionId) {
            val before = original.getEditableContent()
            val after = result.getEditableContent()
            Assertions.assertEquals(before.developerSolutions.ids + attachedId, after.developerSolutions.ids)
            Assertions.assertEquals(before.exercises.ids, after.exercises.ids)
            Assertions.assertEquals(before.tests.ids, after.tests.ids)
            Assertions.assertEquals(before.statement?.id, after.statement?.id)
            Assertions.assertEquals(before.supportedTrikStudioVersions, after.supportedTrikStudioVersions)
            Assertions.assertEquals(original.data.uploadedResources, result.data.uploadedResources)
            Assertions.assertEquals(original.data.owner.id, result.data.owner.id)
            Assertions.assertEquals(original.data.name, result.data.name)
            Assertions.assertEquals(original.data.description, result.data.description)
            Assertions.assertEquals(original.data.sharedTo.ids, result.data.sharedTo.ids)
            Assertions.assertEquals(savedTaskVersion, result.version)
            when (val content = original.data.content) {
                is TaskContent.New -> Assertions.assertInstanceOf(TaskContent.New::class.java, result.data.content)
                is TaskContent.Uncommitted -> assertCommittedUnchanged(content.lastCommitted, result)
                is TaskContent.Committed -> assertCommittedUnchanged(content.lastCommitted, result)
            }
        }

        @Test
        fun `should attach another resource with no count limit`() {
            every { taskRepository.findById(taskId) } returns testNewTask().withData {
                uploadedResources.add(bucket)
                content.new { developerSolutions(listOf(1)) }
            }
            every { developerSolutionRepository.load(any<LazyEntityList<DeveloperSolutionId, DeveloperSolution>>()) } returns
                listOf(resource(1, VersionBucket(UUID(0, 3))))

            val result = developerOperations.attachDeveloperSolution(developer, taskId, resourceId).getOrThrow()

            Assertions.assertEquals(
                listOf(DeveloperSolutionId(1), DeveloperSolutionId(2)),
                result.getEditableContent().developerSolutions.ids,
            )
        }
    }

    @Nested
    inner class DetachStatementTests {
        private val taskId = TaskId(0)
        private val resourceId = StatementId(11)
        private val bucket = VersionBucket(UUID(0, 1))
        private val resource = testStatement(11).withData { versionBucket = bucket }

        @BeforeEach
        fun prepareDetach() {
            every { taskRepository.findById(taskId) } returns taskForDetach("New")
            every { statementRepository.findById(resourceId) } returns resource
            every { taskRepository.update(any<Task>()) } answers { testSavedTask(firstArg()) }
        }

        @Test
        fun `should reject a user without the Developer role`() {
            assertRaises(MissedDeveloperRoleError) {
                developerOperations.detachStatement(testAdministrator {}, taskId, resourceId)
            }
            verify(exactly = 0) { taskRepository.update(any<Task>()) }
        }

        @Test
        fun `should reject a missing task`() {
            every { taskRepository.findById(taskId) } returns null

            assertRaises(TaskNotExistsError(taskId)) {
                developerOperations.detachStatement(developer, taskId, resourceId)
            }
            verify(exactly = 0) { taskRepository.update(any<Task>()) }
        }

        @Test
        fun `should reject a missing statement version`() {
            every { statementRepository.findById(resourceId) } returns null

            assertRaises(StatementNotExistsError(resourceId)) {
                developerOperations.detachStatement(developer, taskId, resourceId)
            }
            verify(exactly = 0) { taskRepository.update(any<Task>()) }
        }

        @Test
        fun `should reject a task owned by another developer`() {
            every { taskRepository.findById(taskId) } returns taskForDetach("New").withData { owner(9) }

            assertRaises(TaskAccessDeniedError(taskId)) {
                developerOperations.detachStatement(developer, taskId, resourceId)
            }
            verify(exactly = 0) { taskRepository.update(any<Task>()) }
        }

        @Test
        fun `should reject a chain not uploaded to the target task`() {
            every { taskRepository.findById(taskId) } returns taskForDetach("New").withData { uploadedResources.remove(bucket) }

            assertRaises(StatementNotUploadedToTaskError(taskId = taskId, statementId = resourceId)) {
                developerOperations.detachStatement(developer, taskId, resourceId)
            }
            verify(exactly = 0) { taskRepository.update(any<Task>()) }
        }

        @ParameterizedTest
        @ValueSource(strings = ["New", "Uncommitted", "Committed"])
        fun `should detach the exact version while preserving other data in every state`(state: String) {
            val original = taskForDetach(state)
            val originalContent = original.getEditableContent()
            val originalVersion = original.version
            val saved = slot<Task>()
            every { taskRepository.findById(taskId) } returns original
            every { taskRepository.update(capture(saved)) } answers { testSavedTask(firstArg()) }

            val result = developerOperations.detachStatement(developer, taskId, resourceId).getOrThrow()

            val expected = taskContentNew {
                detachResources()
                statement = null
            }.wip
            assertDetachedTask(original = original, result = result, expected = expected)
            Assertions.assertEquals(originalVersion, saved.captured.version)
            assertEditableContentEquals(expected, saved.captured.getEditableContent())
            assertEditableContentEquals(originalContent, original.getEditableContent())
            Assertions.assertEquals(originalVersion, original.version)
            Assertions.assertNotSame(original, saved.captured)
            verify(exactly = 1) { taskRepository.update(any<Task>()) }
        }

        @Test
        fun `should detach an older attached version when a newer version exists`() {
            val newer = testStatement(111).withData { versionBucket = bucket }
            every { statementRepository.findLatestByVersionBucket(bucket) } returns newer

            val result = developerOperations.detachStatement(developer, taskId, resourceId).getOrThrow()

            assertEditableContentEquals(
                taskContentNew {
                    detachResources()
                    statement = null
                }.wip,
                result.getEditableContent(),
            )
            verify(exactly = 0) { statementRepository.findLatestByVersionBucket(any()) }
        }

        @ParameterizedTest
        @ValueSource(strings = ["New", "Uncommitted", "Committed"])
        fun `should reject another attached version from the same chain in every state`(state: String) {
            val otherId = StatementId(111)
            every { taskRepository.findById(taskId) } returns taskForDetach(state)
            every { statementRepository.findById(otherId) } returns testStatement(otherId.value).withData { versionBucket = bucket }

            assertRaises(ResourceVersionNotAttachedError(taskId = taskId, versionId = otherId)) {
                developerOperations.detachStatement(developer, taskId, otherId)
            }
            verify(exactly = 0) { taskRepository.update(any<Task>()) }
        }

        @Test
        fun `should reject a version present only in the committed revision when WIP exists`() {
            val original = taskForDetach("Uncommitted").withData {
                content.uncommitted(
                    wipBuilder = { statement(99) },
                    lastCommittedBuilder = {},
                )
            }
            val originalContent = original.data.content
            every { taskRepository.findById(taskId) } returns original

            assertRaises(ResourceVersionNotAttachedError(taskId = taskId, versionId = resourceId)) {
                developerOperations.detachStatement(developer, taskId, resourceId)
            }
            Assertions.assertSame(originalContent, original.data.content)
            verify(exactly = 0) { taskRepository.update(any<Task>()) }
        }

        @Test
        fun `should reject an unattached version in a New task`() {
            every { taskRepository.findById(taskId) } returns taskForDetach("New").withData {
                content.new { statement = null }
            }

            assertRaises(ResourceVersionNotAttachedError(taskId = taskId, versionId = resourceId)) {
                developerOperations.detachStatement(developer, taskId, resourceId)
            }
            verify(exactly = 0) { taskRepository.update(any<Task>()) }
        }

        @Test
        fun `should reject repeated detachment without updating the task again`() {
            val detached = developerOperations.detachStatement(developer, taskId, resourceId).getOrThrow()
            every { taskRepository.findById(taskId) } returns detached

            assertRaises(ResourceVersionNotAttachedError(taskId = taskId, versionId = resourceId)) {
                developerOperations.detachStatement(developer, taskId, resourceId)
            }
            verify(exactly = 1) { taskRepository.update(any<Task>()) }
        }

        @ParameterizedTest
        @ValueSource(strings = ["New", "Uncommitted", "Committed"])
        fun `should detach the last statement in every state`(state: String) {
            val original = taskForDetach(state)
            every { taskRepository.findById(taskId) } returns original

            val result = developerOperations.detachStatement(developer, taskId, resourceId).getOrThrow()

            val expected = taskContentNew {
                detachResources()
                statement = null
            }.wip
            assertDetachedTask(original = original, result = result, expected = expected)
        }
    }

    @Nested
    inner class DetachExerciseTests {
        private val taskId = TaskId(0)
        private val resourceId = ExerciseId(12)
        private val bucket = VersionBucket(UUID(0, 2))
        private val resource = exercise {
            this.id = 12
            createdAt = Instant.EPOCH
            data = viewExercise().data
        }.withData { versionBucket = bucket }

        @BeforeEach
        fun prepareDetach() {
            every { taskRepository.findById(taskId) } returns taskForDetach("New")
            every { exerciseRepository.findById(resourceId) } returns resource
            every { taskRepository.update(any<Task>()) } answers { testSavedTask(firstArg()) }
        }

        @Test
        fun `should reject a user without the Developer role`() {
            assertRaises(MissedDeveloperRoleError) {
                developerOperations.detachExercise(testAdministrator {}, taskId, resourceId)
            }
            verify(exactly = 0) { taskRepository.update(any<Task>()) }
        }

        @Test
        fun `should reject a missing task`() {
            every { taskRepository.findById(taskId) } returns null

            assertRaises(TaskNotExistsError(taskId)) {
                developerOperations.detachExercise(developer, taskId, resourceId)
            }
            verify(exactly = 0) { taskRepository.update(any<Task>()) }
        }

        @Test
        fun `should reject a missing exercise version`() {
            every { exerciseRepository.findById(resourceId) } returns null

            assertRaises(ExerciseNotExistsError(resourceId)) {
                developerOperations.detachExercise(developer, taskId, resourceId)
            }
            verify(exactly = 0) { taskRepository.update(any<Task>()) }
        }

        @Test
        fun `should reject a task owned by another developer`() {
            every { taskRepository.findById(taskId) } returns taskForDetach("New").withData { owner(9) }

            assertRaises(TaskAccessDeniedError(taskId)) {
                developerOperations.detachExercise(developer, taskId, resourceId)
            }
            verify(exactly = 0) { taskRepository.update(any<Task>()) }
        }

        @Test
        fun `should reject a chain not uploaded to the target task`() {
            every { taskRepository.findById(taskId) } returns taskForDetach("New").withData { uploadedResources.remove(bucket) }

            assertRaises(ExerciseNotUploadedToTaskError(taskId = taskId, exerciseId = resourceId)) {
                developerOperations.detachExercise(developer, taskId, resourceId)
            }
            verify(exactly = 0) { taskRepository.update(any<Task>()) }
        }

        @ParameterizedTest
        @ValueSource(strings = ["New", "Uncommitted", "Committed"])
        fun `should detach the exact version while preserving other data in every state`(state: String) {
            val original = taskForDetach(state)
            val originalContent = original.getEditableContent()
            val originalVersion = original.version
            val saved = slot<Task>()
            every { taskRepository.findById(taskId) } returns original
            every { taskRepository.update(capture(saved)) } answers { testSavedTask(firstArg()) }

            val result = developerOperations.detachExercise(developer, taskId, resourceId).getOrThrow()

            val expected = taskContentNew {
                detachResources()
                exercises.clear()
            }.wip
            assertDetachedTask(original = original, result = result, expected = expected)
            Assertions.assertEquals(originalVersion, saved.captured.version)
            assertEditableContentEquals(expected, saved.captured.getEditableContent())
            assertEditableContentEquals(originalContent, original.getEditableContent())
            Assertions.assertEquals(originalVersion, original.version)
            Assertions.assertNotSame(original, saved.captured)
            verify(exactly = 1) { taskRepository.update(any<Task>()) }
        }

        @ParameterizedTest
        @ValueSource(strings = ["New", "Uncommitted", "Committed"])
        fun `should detach only the requested exercise among several in every state`(state: String) {
            val original = taskForDetach(state).withData {
                when (state) {
                    "New" -> content.new { exercises.add(ExerciseId(99)) }
                    "Uncommitted" -> content.uncommitted(
                        wipBuilder = { exercises.add(ExerciseId(99)) },
                        lastCommittedBuilder = {},
                    )
                    "Committed" -> content.committed { exercises.add(ExerciseId(99)) }
                    else -> error("Unsupported test state: $state")
                }
            }
            every { taskRepository.findById(taskId) } returns original

            val result = developerOperations.detachExercise(developer, taskId, resourceId).getOrThrow()

            val expected = taskContentNew {
                detachResources()
                exercises(listOf(99))
            }.wip
            assertDetachedTask(original = original, result = result, expected = expected)
        }

        @Test
        fun `should detach an older attached version when a newer version exists`() {
            val newer = exercise {
                this.id = 112
                createdAt = Instant.ofEpochSecond(1)
                data = resource.data
            }
            every { exerciseRepository.findLatestByVersionBucket(bucket) } returns newer

            val result = developerOperations.detachExercise(developer, taskId, resourceId).getOrThrow()

            assertEditableContentEquals(
                taskContentNew {
                    detachResources()
                    exercises.clear()
                }.wip,
                result.getEditableContent(),
            )
            verify(exactly = 0) { exerciseRepository.findLatestByVersionBucket(any()) }
        }

        @ParameterizedTest
        @ValueSource(strings = ["New", "Uncommitted", "Committed"])
        fun `should reject another attached version from the same chain in every state`(state: String) {
            val otherId = ExerciseId(112)
            every { taskRepository.findById(taskId) } returns taskForDetach(state)
            every { exerciseRepository.findById(otherId) } returns exercise {
                this.id = otherId.value
                createdAt = Instant.ofEpochSecond(1)
                data = resource.data
            }

            assertRaises(ResourceVersionNotAttachedError(taskId = taskId, versionId = otherId)) {
                developerOperations.detachExercise(developer, taskId, otherId)
            }
            verify(exactly = 0) { taskRepository.update(any<Task>()) }
        }

        @Test
        fun `should reject a version present only in the committed revision when WIP exists`() {
            val original = taskForDetach("Uncommitted").withData {
                content.uncommitted(
                    wipBuilder = { exercises(listOf(99)) },
                    lastCommittedBuilder = {},
                )
            }
            val originalContent = original.data.content
            every { taskRepository.findById(taskId) } returns original

            assertRaises(ResourceVersionNotAttachedError(taskId = taskId, versionId = resourceId)) {
                developerOperations.detachExercise(developer, taskId, resourceId)
            }
            Assertions.assertSame(originalContent, original.data.content)
            verify(exactly = 0) { taskRepository.update(any<Task>()) }
        }

        @Test
        fun `should reject an unattached version in a New task`() {
            every { taskRepository.findById(taskId) } returns taskForDetach("New").withData {
                content.new { exercises.clear() }
            }

            assertRaises(ResourceVersionNotAttachedError(taskId = taskId, versionId = resourceId)) {
                developerOperations.detachExercise(developer, taskId, resourceId)
            }
            verify(exactly = 0) { taskRepository.update(any<Task>()) }
        }

        @Test
        fun `should reject repeated detachment without updating the task again`() {
            val detached = developerOperations.detachExercise(developer, taskId, resourceId).getOrThrow()
            every { taskRepository.findById(taskId) } returns detached

            assertRaises(ResourceVersionNotAttachedError(taskId = taskId, versionId = resourceId)) {
                developerOperations.detachExercise(developer, taskId, resourceId)
            }
            verify(exactly = 1) { taskRepository.update(any<Task>()) }
        }

        @ParameterizedTest
        @ValueSource(strings = ["New", "Uncommitted", "Committed"])
        fun `should detach the last exercise in every state`(state: String) {
            val original = taskForDetach(state)
            every { taskRepository.findById(taskId) } returns original

            val result = developerOperations.detachExercise(developer, taskId, resourceId).getOrThrow()

            val expected = taskContentNew {
                detachResources()
                exercises.clear()
            }.wip
            assertDetachedTask(original = original, result = result, expected = expected)
        }
    }

    @Nested
    inner class DetachTestTests {
        private val taskId = TaskId(0)
        private val resourceId = TestId(14)
        private val bucket = VersionBucket(UUID(0, 3))
        private val resource = test {
            this.id = 14
            createdAt = Instant.EPOCH
            data = viewPolygon().data
        }.withData { versionBucket = bucket }

        @BeforeEach
        fun prepareDetach() {
            every { taskRepository.findById(taskId) } returns taskForDetach("New")
            every { testRepository.findById(resourceId) } returns resource
            every { taskRepository.update(any<Task>()) } answers { testSavedTask(firstArg()) }
        }

        @Test
        fun `should reject a user without the Developer role`() {
            assertRaises(MissedDeveloperRoleError) {
                developerOperations.detachTest(testAdministrator {}, taskId, resourceId)
            }
            verify(exactly = 0) { taskRepository.update(any<Task>()) }
        }

        @Test
        fun `should reject a missing task`() {
            every { taskRepository.findById(taskId) } returns null

            assertRaises(TaskNotExistsError(taskId)) {
                developerOperations.detachTest(developer, taskId, resourceId)
            }
            verify(exactly = 0) { taskRepository.update(any<Task>()) }
        }

        @Test
        fun `should reject a missing polygon version`() {
            every { testRepository.findById(resourceId) } returns null

            assertRaises(TestNotExistsError(resourceId)) {
                developerOperations.detachTest(developer, taskId, resourceId)
            }
            verify(exactly = 0) { taskRepository.update(any<Task>()) }
        }

        @Test
        fun `should reject a task owned by another developer`() {
            every { taskRepository.findById(taskId) } returns taskForDetach("New").withData { owner(9) }

            assertRaises(TaskAccessDeniedError(taskId)) {
                developerOperations.detachTest(developer, taskId, resourceId)
            }
            verify(exactly = 0) { taskRepository.update(any<Task>()) }
        }

        @Test
        fun `should reject a chain not uploaded to the target task`() {
            every { taskRepository.findById(taskId) } returns taskForDetach("New").withData { uploadedResources.remove(bucket) }

            assertRaises(TestNotUploadedToTaskError(taskId = taskId, testId = resourceId)) {
                developerOperations.detachTest(developer, taskId, resourceId)
            }
            verify(exactly = 0) { taskRepository.update(any<Task>()) }
        }

        @ParameterizedTest
        @ValueSource(strings = ["New", "Uncommitted", "Committed"])
        fun `should detach the exact version while preserving other data in every state`(state: String) {
            val original = taskForDetach(state)
            val originalContent = original.getEditableContent()
            val originalVersion = original.version
            val saved = slot<Task>()
            every { taskRepository.findById(taskId) } returns original
            every { taskRepository.update(capture(saved)) } answers { testSavedTask(firstArg()) }

            val result = developerOperations.detachTest(developer, taskId, resourceId).getOrThrow()

            val expected = taskContentNew {
                detachResources()
                tests(listOf(13, 15))
            }.wip
            assertDetachedTask(original = original, result = result, expected = expected)
            Assertions.assertEquals(originalVersion, saved.captured.version)
            assertEditableContentEquals(expected, saved.captured.getEditableContent())
            assertEditableContentEquals(originalContent, original.getEditableContent())
            Assertions.assertEquals(originalVersion, original.version)
            Assertions.assertNotSame(original, saved.captured)
            verify(exactly = 1) { taskRepository.update(any<Task>()) }
        }

        @Test
        fun `should detach an older attached version when a newer version exists`() {
            val newer = test {
                this.id = 114
                createdAt = Instant.ofEpochSecond(1)
                data = resource.data
            }
            every { testRepository.findLatestByVersionBucket(bucket) } returns newer

            val result = developerOperations.detachTest(developer, taskId, resourceId).getOrThrow()

            assertEditableContentEquals(
                taskContentNew {
                    detachResources()
                    tests(listOf(13, 15))
                }.wip,
                result.getEditableContent(),
            )
            verify(exactly = 0) { testRepository.findLatestByVersionBucket(any()) }
        }

        @ParameterizedTest
        @ValueSource(strings = ["New", "Uncommitted", "Committed"])
        fun `should reject another attached version from the same chain in every state`(state: String) {
            val otherId = TestId(114)
            every { taskRepository.findById(taskId) } returns taskForDetach(state)
            every { testRepository.findById(otherId) } returns test {
                this.id = otherId.value
                createdAt = Instant.ofEpochSecond(1)
                data = resource.data
            }

            assertRaises(ResourceVersionNotAttachedError(taskId = taskId, versionId = otherId)) {
                developerOperations.detachTest(developer, taskId, otherId)
            }
            verify(exactly = 0) { taskRepository.update(any<Task>()) }
        }

        @Test
        fun `should reject a version present only in the committed revision when WIP exists`() {
            val original = taskForDetach("Uncommitted").withData {
                content.uncommitted(
                    wipBuilder = { tests(listOf(99)) },
                    lastCommittedBuilder = {},
                )
            }
            val originalContent = original.data.content
            every { taskRepository.findById(taskId) } returns original

            assertRaises(ResourceVersionNotAttachedError(taskId = taskId, versionId = resourceId)) {
                developerOperations.detachTest(developer, taskId, resourceId)
            }
            Assertions.assertSame(originalContent, original.data.content)
            verify(exactly = 0) { taskRepository.update(any<Task>()) }
        }

        @Test
        fun `should reject an unattached version in a New task`() {
            every { taskRepository.findById(taskId) } returns taskForDetach("New").withData {
                content.new { tests = mutableListOf() }
            }

            assertRaises(ResourceVersionNotAttachedError(taskId = taskId, versionId = resourceId)) {
                developerOperations.detachTest(developer, taskId, resourceId)
            }
            verify(exactly = 0) { taskRepository.update(any<Task>()) }
        }

        @Test
        fun `should reject repeated detachment without updating the task again`() {
            val detached = developerOperations.detachTest(developer, taskId, resourceId).getOrThrow()
            every { taskRepository.findById(taskId) } returns detached

            assertRaises(ResourceVersionNotAttachedError(taskId = taskId, versionId = resourceId)) {
                developerOperations.detachTest(developer, taskId, resourceId)
            }
            verify(exactly = 1) { taskRepository.update(any<Task>()) }
        }

        @ParameterizedTest
        @ValueSource(strings = ["New", "Uncommitted", "Committed"])
        fun `should detach the last polygon in every state`(state: String) {
            val original = taskForDetach(state) { tests(listOf(14)) }
            every { taskRepository.findById(taskId) } returns original

            val result = developerOperations.detachTest(developer, taskId, resourceId).getOrThrow()

            val expected = taskContentNew {
                detachResources()
                tests = mutableListOf()
            }.wip
            assertDetachedTask(original = original, result = result, expected = expected)
        }
    }

    @Nested
    inner class DetachDeveloperSolutionTests {
        private val taskId = TaskId(0)
        private val resourceId = DeveloperSolutionId(17)
        private val bucket = VersionBucket(UUID(0, 4))
        private val resource = developerSolution {
            this.id = 17
            createdAt = Instant.EPOCH
            data = viewDeveloperSolution().data
        }.withData { versionBucket = bucket }

        @BeforeEach
        fun prepareDetach() {
            every { taskRepository.findById(taskId) } returns taskForDetach("New")
            every { developerSolutionRepository.findById(resourceId) } returns resource
            every { taskRepository.update(any<Task>()) } answers { testSavedTask(firstArg()) }
        }

        @Test
        fun `should reject a user without the Developer role`() {
            assertRaises(MissedDeveloperRoleError) {
                developerOperations.detachDeveloperSolution(testAdministrator {}, taskId, resourceId)
            }
            verify(exactly = 0) { taskRepository.update(any<Task>()) }
        }

        @Test
        fun `should reject a missing task`() {
            every { taskRepository.findById(taskId) } returns null

            assertRaises(TaskNotExistsError(taskId)) {
                developerOperations.detachDeveloperSolution(developer, taskId, resourceId)
            }
            verify(exactly = 0) { taskRepository.update(any<Task>()) }
        }

        @Test
        fun `should reject a missing developer solution version`() {
            every { developerSolutionRepository.findById(resourceId) } returns null

            assertRaises(DeveloperSolutionNotExistsError(resourceId)) {
                developerOperations.detachDeveloperSolution(developer, taskId, resourceId)
            }
            verify(exactly = 0) { taskRepository.update(any<Task>()) }
        }

        @Test
        fun `should reject a task owned by another developer`() {
            every { taskRepository.findById(taskId) } returns taskForDetach("New").withData { owner(9) }

            assertRaises(TaskAccessDeniedError(taskId)) {
                developerOperations.detachDeveloperSolution(developer, taskId, resourceId)
            }
            verify(exactly = 0) { taskRepository.update(any<Task>()) }
        }

        @Test
        fun `should reject a chain not uploaded to the target task`() {
            every { taskRepository.findById(taskId) } returns taskForDetach("New").withData { uploadedResources.remove(bucket) }

            assertRaises(DeveloperSolutionNotUploadedToTaskError(taskId = taskId, developerSolutionId = resourceId)) {
                developerOperations.detachDeveloperSolution(developer, taskId, resourceId)
            }
            verify(exactly = 0) { taskRepository.update(any<Task>()) }
        }

        @ParameterizedTest
        @ValueSource(strings = ["New", "Uncommitted", "Committed"])
        fun `should detach the exact version while preserving other data in every state`(state: String) {
            val original = taskForDetach(state)
            val originalContent = original.getEditableContent()
            val originalVersion = original.version
            val saved = slot<Task>()
            every { taskRepository.findById(taskId) } returns original
            every { taskRepository.update(capture(saved)) } answers { testSavedTask(firstArg()) }

            val result = developerOperations.detachDeveloperSolution(developer, taskId, resourceId).getOrThrow()

            val expected = taskContentNew {
                detachResources()
                developerSolutions(listOf(16, 18))
            }.wip
            assertDetachedTask(original = original, result = result, expected = expected)
            Assertions.assertEquals(originalVersion, saved.captured.version)
            assertEditableContentEquals(expected, saved.captured.getEditableContent())
            assertEditableContentEquals(originalContent, original.getEditableContent())
            Assertions.assertEquals(originalVersion, original.version)
            Assertions.assertNotSame(original, saved.captured)
            verify(exactly = 1) { taskRepository.update(any<Task>()) }
        }

        @Test
        fun `should detach an older attached version when a newer version exists`() {
            val newer = developerSolution {
                this.id = 117
                createdAt = Instant.ofEpochSecond(1)
                data = resource.data
            }
            every { developerSolutionRepository.findLatestByVersionBucket(bucket) } returns newer

            val result = developerOperations.detachDeveloperSolution(developer, taskId, resourceId).getOrThrow()

            assertEditableContentEquals(
                taskContentNew {
                    detachResources()
                    developerSolutions(listOf(16, 18))
                }.wip,
                result.getEditableContent(),
            )
            verify(exactly = 0) { developerSolutionRepository.findLatestByVersionBucket(any()) }
        }

        @ParameterizedTest
        @ValueSource(strings = ["New", "Uncommitted", "Committed"])
        fun `should reject another attached version from the same chain in every state`(state: String) {
            val otherId = DeveloperSolutionId(117)
            every { taskRepository.findById(taskId) } returns taskForDetach(state)
            every { developerSolutionRepository.findById(otherId) } returns developerSolution {
                this.id = otherId.value
                createdAt = Instant.ofEpochSecond(1)
                data = resource.data
            }

            assertRaises(ResourceVersionNotAttachedError(taskId = taskId, versionId = otherId)) {
                developerOperations.detachDeveloperSolution(developer, taskId, otherId)
            }
            verify(exactly = 0) { taskRepository.update(any<Task>()) }
        }

        @Test
        fun `should reject a version present only in the committed revision when WIP exists`() {
            val original = taskForDetach("Uncommitted").withData {
                content.uncommitted(
                    wipBuilder = { developerSolutions(listOf(99)) },
                    lastCommittedBuilder = {},
                )
            }
            val originalContent = original.data.content
            every { taskRepository.findById(taskId) } returns original

            assertRaises(ResourceVersionNotAttachedError(taskId = taskId, versionId = resourceId)) {
                developerOperations.detachDeveloperSolution(developer, taskId, resourceId)
            }
            Assertions.assertSame(originalContent, original.data.content)
            verify(exactly = 0) { taskRepository.update(any<Task>()) }
        }

        @Test
        fun `should reject an unattached version in a New task`() {
            every { taskRepository.findById(taskId) } returns taskForDetach("New").withData {
                content.new { developerSolutions = mutableListOf() }
            }

            assertRaises(ResourceVersionNotAttachedError(taskId = taskId, versionId = resourceId)) {
                developerOperations.detachDeveloperSolution(developer, taskId, resourceId)
            }
            verify(exactly = 0) { taskRepository.update(any<Task>()) }
        }

        @Test
        fun `should reject repeated detachment without updating the task again`() {
            val detached = developerOperations.detachDeveloperSolution(developer, taskId, resourceId).getOrThrow()
            every { taskRepository.findById(taskId) } returns detached

            assertRaises(ResourceVersionNotAttachedError(taskId = taskId, versionId = resourceId)) {
                developerOperations.detachDeveloperSolution(developer, taskId, resourceId)
            }
            verify(exactly = 1) { taskRepository.update(any<Task>()) }
        }

        @ParameterizedTest
        @ValueSource(strings = ["New", "Uncommitted", "Committed"])
        fun `should detach the last developer solution in every state`(state: String) {
            val original = taskForDetach(state) { developerSolutions(listOf(17)) }
            every { taskRepository.findById(taskId) } returns original

            val result = developerOperations.detachDeveloperSolution(developer, taskId, resourceId).getOrThrow()

            val expected = taskContentNew {
                detachResources()
                developerSolutions = mutableListOf()
            }.wip
            assertDetachedTask(original = original, result = result, expected = expected)
        }
    }

    private fun taskForDetach(state: String, customize: TaskContentBuilder<*>.() -> Unit = {}): Task {
        val original = taskInState(state)
        return original.withData {
            uploadedResources = mutableSetOf(
                VersionBucket(UUID(0, 0)),
                VersionBucket(UUID(0, 1)),
                VersionBucket(UUID(0, 2)),
                VersionBucket(UUID(0, 3)),
                VersionBucket(UUID(0, 4)),
            )
            when (original.data.content) {
                is TaskContent.New -> content.new {
                    detachResources()
                    customize()
                }
                is TaskContent.Uncommitted -> content.uncommitted(
                    wipBuilder = {
                        detachResources()
                        customize()
                    },
                    lastCommittedBuilder = {
                        detachResources()
                        customize()
                    },
                )
                is TaskContent.Committed -> content.committed {
                    detachResources()
                    customize()
                }
            }
        }
    }

    private fun TaskContentBuilder<*>.detachResources() {
        statement(11)
        exercises(listOf(12))
        tests(listOf(13, 14, 15))
        developerSolutions(listOf(16, 17, 18))
        supportedTrikStudioVersions(listOf("3.0.0", "4.0.0"))
    }

    private fun assertDetachedTask(original: Task, result: Task, expected: WipTaskContent) {
        assertEditableContentEquals(expected, result.getEditableContent())
        Assertions.assertEquals(original.id, result.id)
        Assertions.assertEquals(original.createdAt, result.createdAt)
        Assertions.assertEquals(original.data.owner.id, result.data.owner.id)
        Assertions.assertEquals(original.data.name, result.data.name)
        Assertions.assertEquals(original.data.description, result.data.description)
        Assertions.assertEquals(original.data.sharedTo.ids, result.data.sharedTo.ids)
        Assertions.assertEquals(original.data.uploadedResources, result.data.uploadedResources)
        Assertions.assertEquals(savedTaskVersion.value, result.version?.value)
        when (val content = original.data.content) {
            is TaskContent.New -> Assertions.assertInstanceOf(TaskContent.New::class.java, result.data.content)
            is TaskContent.Uncommitted -> assertCommittedUnchanged(content.lastCommitted, result)
            is TaskContent.Committed -> assertCommittedUnchanged(content.lastCommitted, result)
        }
    }

    private fun assertEditableContentEquals(expected: WipTaskContent, actual: WipTaskContent) {
        Assertions.assertEquals(expected.statement?.id, actual.statement?.id)
        Assertions.assertEquals(expected.exercises.ids, actual.exercises.ids)
        Assertions.assertEquals(expected.tests.ids, actual.tests.ids)
        Assertions.assertEquals(expected.developerSolutions.ids, actual.developerSolutions.ids)
        Assertions.assertEquals(expected.supportedTrikStudioVersions, actual.supportedTrikStudioVersions)
    }
}
