package tech.testsys.operation.user

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.extension.ExtendWith
import tech.testsys.domain.builder.api.*
import tech.testsys.domain.contract.persistence.repository.StatementRepository
import tech.testsys.domain.contract.persistence.repository.TaskRepository
import tech.testsys.domain.model.EntityVersion
import tech.testsys.domain.model.task.StatementId
import tech.testsys.domain.model.task.Task
import tech.testsys.domain.model.task.TaskContent
import tech.testsys.domain.model.task.TaskData
import tech.testsys.domain.model.task.TaskId
import tech.testsys.domain.model.user.MultipleRoleUser
import tech.testsys.domain.model.user.MultipleRoleUserId
import tech.testsys.domain.model.user.UserId
import tech.testsys.operation.error.AuthorizationError
import tech.testsys.operation.error.CreateTaskError
import tech.testsys.operation.error.StatementNotExistsError
import tech.testsys.operation.error.TaskAlreadyHasStatementError
import tech.testsys.operation.error.TaskNotExistsError
import tech.testsys.operation.error.getOrThrow
import tech.testsys.operation.util.assertRaises
import tech.testsys.operation.util.testAdministrator
import tech.testsys.operation.util.testDeveloper
import tech.testsys.operation.util.testStatement
import tech.testsys.operation.util.testTask
import tech.testsys.operation.util.testUncommittedTask
import kotlin.test.Test
import java.time.Instant

@ExtendWith(MockKExtension::class)
@MockKExtension.ConfirmVerification
class DeveloperOperationsTests {

    @MockK
    lateinit var taskRepository: TaskRepository

    @MockK
    lateinit var statementRepository: StatementRepository

    @InjectMockKs
    lateinit var developerOperations: DeveloperOperations

    lateinit var developer: MultipleRoleUser

    @BeforeEach
    fun beforeEach() {
        developer = testDeveloper { data = developerData { }}
    }

    @Nested
    inner class CreateTaskTests {

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

        private val taskName = "testTask"
        private val taskDescription = "testTaskDescription"

        @Test
        fun `should raise AuthorizationError if user is not a Developer`() {
            val nonDeveloper = testAdministrator { }

            assertRaises(AuthorizationError) {
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
            val taskContent = taskData.content
            Assertions.assertTrue { taskData.sharedTo.ids.isEmpty() }
            when (taskContent) {
                is TaskContent.New -> {
                    Assertions.assertTrue { taskContent.wip.tests.ids.isEmpty() }
                    Assertions.assertTrue { taskContent.wip.exercise == null }
                    Assertions.assertTrue { taskContent.wip.statement == null }
                    Assertions.assertTrue { taskContent.wip.developerSolutions.ids.isEmpty() }
                    Assertions.assertTrue { taskContent.wip.supportedTrikStudioVersions.isEmpty() }
                }
                else -> Assertions.fail("data should be TaskData.New")
            }
        }

        @Test
        fun `should save task`() {
            developerOperations.createTask(developer, taskName, taskDescription).getOrThrow()

            verify(exactly = 1) { taskRepository.save(any<TaskData>()) }
        }
    }

    @Nested
    inner class AttachStatementTests  {
        val taskId = TaskId(1L)
        val statementId = StatementId(1)

        @Test
        fun `should raise AuthorizationError if user is not a Developer`() {
            val nonDeveloper = testAdministrator { }

            assertRaises(AuthorizationError) {
                developerOperations.attachStatement(nonDeveloper, taskId, statementId)
            }
        }

        @Test
        fun `should raise TaskNotExistsError if task not exists`() {
            every { statementRepository.findById(eq(statementId)) } answers { testStatement() }
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
        fun `should raise TaskAlreadyHasStatementError if task has statement`() {
            every { statementRepository.findById(eq(statementId)) } answers { testStatement() }
            every { taskRepository.findById(eq(taskId)) } answers {
                testUncommittedTask().withData {
                    content.uncommitted(
                        wipBuilder = { statement = statementId },
                        lastCommittedBuilder = {}
                    )
                }
            }

            assertRaises(TaskAlreadyHasStatementError) {
                developerOperations.attachStatement(developer, taskId, statementId)
            }
        }

        @Test
        fun `should return new or uncommitted task`() {
            every { statementRepository.findById(eq(statementId)) } answers { testStatement() }
            every { taskRepository.findById(eq(taskId)) } answers { testUncommittedTask() }
            every { taskRepository.update(any<Task>()) } answers { arg(0) }

            val result = developerOperations.attachStatement(developer, taskId, statementId)
                .getOrThrow()

            when (result.data.content) {
                is TaskContent.New -> {}
                is TaskContent.Uncommitted -> {}
                else -> Assertions.fail("data should be TaskData.New")
            }
        }

        @Test
        fun `should change statement`() {
            every { statementRepository.findById(eq(statementId)) } answers { testStatement() }
            every { taskRepository.findById(eq(taskId)) } answers { testUncommittedTask() }
            every { taskRepository.update(any<Task>()) } answers { arg(0) }

            val result = developerOperations.attachStatement(developer, taskId, statementId)
                .getOrThrow()

            val actualStatementId = when (val content = result.data.content) {
                is TaskContent.New -> content.wip.statement
                is TaskContent.Uncommitted -> content.wip.statement
                else -> Assertions.fail("data should be TaskData.New")
            }
            Assertions.assertEquals(statementId.value, actualStatementId?.id?.value)
        }

        @Test
        fun `should update task`() {
            every { statementRepository.findById(eq(statementId)) } answers { testStatement() }
            every { taskRepository.findById(eq(taskId)) } answers { testUncommittedTask() }
            every { taskRepository.update(any<Task>()) } answers { arg(0) }

            developerOperations.attachStatement(developer, taskId, statementId)
                .getOrThrow()

            verify(exactly = 1) { taskRepository.update(any<Task>()) }
        }
    }
}