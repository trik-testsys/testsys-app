package tech.testsys.operation.user

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import tech.testsys.domain.builder.api.*
import tech.testsys.domain.contract.persistence.repository.CommunityRepository
import tech.testsys.domain.contract.persistence.repository.DeveloperSolutionRepository
import tech.testsys.domain.contract.persistence.repository.ExerciseRepository
import tech.testsys.domain.contract.persistence.repository.StatementRepository
import tech.testsys.domain.contract.persistence.repository.TaskRepository
import tech.testsys.domain.contract.persistence.repository.TestRepository
import tech.testsys.domain.model.EntityVersion
import tech.testsys.domain.model.LazyEntity
import tech.testsys.domain.model.LazyEntityList
import tech.testsys.domain.model.group.CommunityId
import tech.testsys.domain.model.task.CommittedTaskContent
import tech.testsys.domain.model.task.DeveloperSolution
import tech.testsys.domain.model.task.DeveloperSolutionId
import tech.testsys.domain.model.task.Exercise
import tech.testsys.domain.model.task.ExerciseId
import tech.testsys.domain.model.task.Statement
import tech.testsys.domain.model.task.StatementId
import tech.testsys.domain.model.task.Task
import tech.testsys.domain.model.task.TaskContent
import tech.testsys.domain.model.task.TaskData
import tech.testsys.domain.model.task.TaskId
import tech.testsys.domain.model.task.TestId
import tech.testsys.domain.model.task.VersionBucket
import tech.testsys.domain.model.user.MultipleRoleUser
import tech.testsys.domain.model.user.MultipleRoleUserId
import tech.testsys.operation.annotation.InternalOperationsApi
import tech.testsys.operation.error.CommunityAccessDeniedError
import tech.testsys.operation.error.CommunityNotExistsError
import tech.testsys.operation.error.DeveloperSolutionNotExistsError
import tech.testsys.operation.error.DeveloperSolutionNotUploadedToTaskError
import tech.testsys.operation.error.DeveloperSolutionVersionNotLatestError
import tech.testsys.operation.error.ExerciseNotExistsError
import tech.testsys.operation.error.ExerciseNotUploadedToTaskError
import tech.testsys.operation.error.ExerciseVersionNotLatestError
import tech.testsys.operation.error.MissedDeveloperRoleError
import tech.testsys.operation.error.ResourceAlreadyAttachedError
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
import tech.testsys.operation.error.getOrThrow
import tech.testsys.operation.util.assertRaises
import tech.testsys.operation.util.getEditableContent
import tech.testsys.operation.util.savedTaskVersion
import tech.testsys.operation.util.testAdministrator
import tech.testsys.operation.util.testCommitedTask
import tech.testsys.operation.util.testCommunity
import tech.testsys.operation.util.testDeveloper
import tech.testsys.operation.util.testNewTask
import tech.testsys.operation.util.testSavedTask
import tech.testsys.operation.util.testStatement
import tech.testsys.operation.util.testUncommittedTask
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
    private val developerOperations = DeveloperOperations(
        taskRepository,
        statementRepository,
        communityRepository,
        exerciseRepository,
        testRepository,
        developerSolutionRepository,
    )

    private lateinit var developer: MultipleRoleUser

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

    private fun tech.testsys.domain.builder.task.TaskContentBuilder<*>.existingResources() {
        tests(listOf(7))
        developerSolutions(listOf(8))
        supportedTrikStudioVersions(listOf("3.0.0"))
    }

    private fun assertCommittedUnchanged(expected: CommittedTaskContent, result: Task) {
        val content = Assertions.assertInstanceOf(TaskContent.Uncommitted::class.java, result.data.content)
        Assertions.assertEquals(expected.tests.ids, content.lastCommitted.tests.ids)
        Assertions.assertEquals(expected.exercise.id, content.lastCommitted.exercise.id)
        Assertions.assertEquals(expected.statement.id, content.lastCommitted.statement.id)
        Assertions.assertEquals(expected.developerSolutions.ids, content.lastCommitted.developerSolutions.ids)
        Assertions.assertEquals(expected.supportedTrikStudioVersions, content.lastCommitted.supportedTrikStudioVersions)
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
            Assertions.assertTrue { taskContent.wip.exercise == null }
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
            Assertions.assertEquals(ExerciseId(1L), content.lastCommitted.exercise.id)
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
            Assertions.assertEquals(ExerciseId(1L), content.lastCommitted.exercise.id)
            Assertions.assertEquals(StatementId(1L), content.lastCommitted.statement.id)
            Assertions.assertNull(content.wip.statement)
        }

        @Test
        fun `should keep Committed task content when task is shared`() {
            every { taskRepository.findById(eq(taskId)) } answers { testCommitedTask() }

            val result = developerOperations.shareTask(developer, taskId, setOf(firstCommunityId)).getOrThrow()

            val content = Assertions.assertInstanceOf(TaskContent.Committed::class.java, result.data.content)
            Assertions.assertEquals(ExerciseId(1L), content.lastCommitted.exercise.id)
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
            every { exerciseRepository.load(any<LazyEntity<ExerciseId, Exercise>>()) } answers {
                val reference = firstArg<LazyEntity<ExerciseId, Exercise>>()
                resource(reference.id.value, VersionBucket(UUID(0, 3)))
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
                content.new { exercise(1) }
            }
            every { exerciseRepository.load(any<LazyEntity<ExerciseId, Exercise>>()) } returns resource(1)

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
            Assertions.assertEquals(attachedId, after.exercise?.id)
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
        fun `should reject an occupied exercise slot with the same language`() {
            every { taskRepository.findById(taskId) } returns testNewTask().withData {
                uploadedResources.add(bucket)
                content.new { exercise(1) }
            }
            every { exerciseRepository.load(any<LazyEntity<ExerciseId, Exercise>>()) } returns resource(1, VersionBucket(UUID(0, 3)))

            assertRaises(TaskAlreadyHasExerciseError) {
                developerOperations.attachExercise(developer, taskId, resourceId)
            }
            verify(exactly = 0) { taskRepository.update(any<Task>()) }
        }

        @Test
        fun `should reject an occupied exercise slot with a different language`() {
            val existing = resource(1, VersionBucket(UUID(0, 3))).withData { language.javaScript() }
            every { taskRepository.findById(taskId) } returns testNewTask().withData {
                uploadedResources.add(bucket)
                content.new { exercise(1) }
            }
            every { exerciseRepository.load(any<LazyEntity<ExerciseId, Exercise>>()) } returns existing

            assertRaises(TaskAlreadyHasExerciseError) {
                developerOperations.attachExercise(developer, taskId, resourceId)
            }
            verify(exactly = 0) { taskRepository.update(any<Task>()) }
        }

        @Test
        fun `should reject an occupied exercise slot in a Committed task`() {
            val original = taskInState("Committed").withData { uploadedResources.add(bucket) }
            every { taskRepository.findById(taskId) } returns original

            assertRaises(TaskAlreadyHasExerciseError) {
                developerOperations.attachExercise(developer, taskId, resourceId)
            }
            Assertions.assertInstanceOf(TaskContent.Committed::class.java, original.data.content)
            verify(exactly = 0) { taskRepository.update(any<Task>()) }
        }

        @Test
        fun `should reject an attached exercise chain in a Committed task`() {
            val original = testCommitedTask().withData { uploadedResources.add(bucket) }
            every { taskRepository.findById(taskId) } returns original
            every { exerciseRepository.load(any<LazyEntity<ExerciseId, Exercise>>()) } returns resource(1)

            assertRaises(ResourceAlreadyAttachedError(taskId, bucket)) {
                developerOperations.attachExercise(developer, taskId, resourceId)
            }
            Assertions.assertInstanceOf(TaskContent.Committed::class.java, original.data.content)
            verify(exactly = 0) { taskRepository.update(any<Task>()) }
        }

        @Test
        fun `should attach an archive as one exercise resource`() {
            val archive = resource().withData { file("exercises.zip", byteArrayOf(1, 2)) }
            every { exerciseRepository.findById(resourceId) } returns archive
            every { exerciseRepository.findLatestByVersionBucket(bucket) } returns archive

            val result = developerOperations.attachExercise(developer, taskId, resourceId).getOrThrow()

            Assertions.assertEquals(resourceId, result.getEditableContent().exercise?.id)
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
            Assertions.assertEquals(before.exercise?.id, after.exercise?.id)
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
            Assertions.assertEquals(before.exercise?.id, after.exercise?.id)
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
}
