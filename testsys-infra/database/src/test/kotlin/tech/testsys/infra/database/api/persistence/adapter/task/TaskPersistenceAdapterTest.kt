package tech.testsys.infra.database.api.persistence.adapter.task

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import tech.testsys.domain.builder.api.taskData
import tech.testsys.domain.builder.api.withData
import tech.testsys.domain.contract.persistence.repository.TaskRepository
import tech.testsys.domain.model.task.CommittedTaskContent
import tech.testsys.domain.model.task.Task
import tech.testsys.domain.model.task.TaskContent
import tech.testsys.domain.model.task.TaskData
import tech.testsys.domain.model.task.TaskId
import tech.testsys.domain.model.task.TrikStudioVersion
import tech.testsys.domain.model.task.WipTaskContent
import tech.testsys.infra.database.api.persistence.adapter.PersistenceAdapterContractTest
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.repository.task.CommunityToTaskJpaEntityRepository
import tech.testsys.infra.database.internal.jpa.repository.task.TaskContentJpaEntityRepository
import tech.testsys.infra.database.internal.jpa.repository.task.TestToTaskContentJpaEntityRepository
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNotNull

@OptIn(InternalDatabaseApi::class)
class TaskPersistenceAdapterTest : PersistenceAdapterContractTest<TaskData, TaskId, Task>() {

    @Autowired
    override lateinit var repository: TaskRepository

    @Autowired
    private lateinit var taskContentJpaEntityRepository: TaskContentJpaEntityRepository

    @Autowired
    private lateinit var testToTaskContentJpaEntityRepository: TestToTaskContentJpaEntityRepository

    @Autowired
    private lateinit var communityToTaskJpaEntityRepository: CommunityToTaskJpaEntityRepository

    override fun newData(): TaskData {
        val ownerId = fixtures.developer().id.value
        val communityId = fixtures.community().id.value
        val exerciseId = fixtures.exercise().id.value
        val statementId = fixtures.statement().id.value
        val polygonIds = listOf(fixtures.polygon().id.value, fixtures.polygon().id.value)
        val developerSolutionIds = listOf(fixtures.developerSolution().id.value)
        val version = fixtures.trikStudioVersion()
        return taskData {
            owner(ownerId)
            name = fixtures.unique("Task")
            description = "Task description"
            sharedTo(listOf(communityId))
            content.committed {
                exercise(exerciseId)
                statement(statementId)
                tests(polygonIds)
                developerSolutions(developerSolutionIds)
                supportedTrikStudioVersions = mutableListOf(version)
            }
        }
    }

    override fun modified(entity: Task): Task {
        val communityId = fixtures.community().id
        return entity.withData {
            name = fixtures.unique("Renamed task")
            description = "Updated description"
            sharedTo = mutableListOf(communityId)
        }
    }

    override fun idOf(value: Long) = TaskId(value)

    override fun assertSameData(expected: Task, actual: Task) {
        assertEquals(expected.data.owner.id, actual.data.owner.id)
        assertEquals(expected.data.name, actual.data.name)
        assertEquals(expected.data.description, actual.data.description)
        assertEquals(expected.data.sharedTo.ids.toSet(), actual.data.sharedTo.ids.toSet())
        assertSameContent(expected.data.content, actual.data.content)
    }

    private fun assertSameContent(expected: TaskContent, actual: TaskContent) {
        when (expected) {
            is TaskContent.New -> assertSameWip(expected.wip, assertIs<TaskContent.New>(actual).wip)
            is TaskContent.Uncommitted -> {
                val actualUncommitted = assertIs<TaskContent.Uncommitted>(actual)
                assertSameWip(expected.wip, actualUncommitted.wip)
                assertSameCommitted(expected.lastCommitted, actualUncommitted.lastCommitted)
            }
            is TaskContent.Committed -> assertSameCommitted(expected.lastCommitted, assertIs<TaskContent.Committed>(actual).lastCommitted)
        }
    }

    private fun assertSameWip(expected: WipTaskContent, actual: WipTaskContent) {
        assertEquals(expected.tests.ids.toSet(), actual.tests.ids.toSet())
        assertEquals(expected.exercise?.id, actual.exercise?.id)
        assertEquals(expected.statement?.id, actual.statement?.id)
        assertEquals(expected.developerSolutions.ids.toSet(), actual.developerSolutions.ids.toSet())
        assertEquals(expected.supportedTrikStudioVersions.toSet(), actual.supportedTrikStudioVersions.toSet())
    }

    private fun assertSameCommitted(expected: CommittedTaskContent, actual: CommittedTaskContent) {
        assertEquals(expected.tests.ids.toSet(), actual.tests.ids.toSet())
        assertEquals(expected.exercise.id, actual.exercise.id)
        assertEquals(expected.statement.id, actual.statement.id)
        assertEquals(expected.developerSolutions.ids.toSet(), actual.developerSolutions.ids.toSet())
        assertEquals(expected.supportedTrikStudioVersions.toSet(), actual.supportedTrikStudioVersions.toSet())
    }

    private fun newTaskData(): TaskData {
        val ownerId = fixtures.developer().id.value
        val polygonId = fixtures.polygon().id.value
        val developerSolutionId = fixtures.developerSolution().id.value
        val version = fixtures.trikStudioVersion()
        return taskData {
            owner(ownerId)
            name = fixtures.unique("New task")
            description = "Nothing committed yet"
            content.new {
                tests(listOf(polygonId))
                developerSolutions(listOf(developerSolutionId))
                supportedTrikStudioVersions = mutableListOf(version)
            }
        }
    }

    @Test
    fun `new content without exercise and statement survives a round trip`() {
        val data = newTaskData()

        val saved = repository.save(data)
        val found = assertNotNull(repository.findById(saved.id))

        assertSameContent(data.content, saved.data.content)
        assertSameContent(data.content, found.data.content)
        assertEquals(1, taskContentJpaEntityRepository.count())
    }

    @Test
    fun `uncommitted content keeps the wip and the last committed revisions apart`() {
        val ownerId = fixtures.developer().id.value
        val committedExerciseId = fixtures.exercise().id.value
        val committedStatementId = fixtures.statement().id.value
        val wipExerciseId = fixtures.exercise().id.value
        val committedPolygonId = fixtures.polygon().id.value
        val wipPolygonId = fixtures.polygon().id.value
        val data = taskData {
            owner(ownerId)
            name = fixtures.unique("Uncommitted task")
            description = "Edited after a commit"
            content.uncommitted(
                wipBuilder = {
                    exercise(wipExerciseId)
                    tests(listOf(committedPolygonId, wipPolygonId))
                },
                lastCommittedBuilder = {
                    exercise(committedExerciseId)
                    statement(committedStatementId)
                    tests(listOf(committedPolygonId))
                },
            )
        }

        val saved = repository.save(data)
        val found = assertNotNull(repository.findById(saved.id))

        assertSameContent(data.content, found.data.content)
        assertEquals(2, taskContentJpaEntityRepository.count())
        assertEquals(3, testToTaskContentJpaEntityRepository.count())
    }

    @Test
    fun `committing a task replaces its content rows`() {
        val saved = repository.save(newTaskData())
        val exerciseId = fixtures.exercise().id.value
        val statementId = fixtures.statement().id.value
        val polygonIds = listOf(fixtures.polygon().id.value, fixtures.polygon().id.value)

        val committed = saved.withData {
            content.committed {
                exercise(exerciseId)
                statement(statementId)
                tests(polygonIds)
            }
        }
        repository.update(committed)

        val found = assertNotNull(repository.findById(saved.id))
        assertSameContent(committed.data.content, found.data.content)
        assertEquals(1, taskContentJpaEntityRepository.count())
        assertEquals(2, testToTaskContentJpaEntityRepository.count())
    }

    @Test
    fun `editing a committed task adds a wip revision beside the committed one`() {
        val saved = repository.save(newData())
        val committed = assertIs<TaskContent.Committed>(saved.data.content).lastCommitted
        val wipPolygonId = fixtures.polygon().id.value

        val edited = saved.withData {
            content.uncommitted(
                wipBuilder = { tests(listOf(wipPolygonId)) },
                lastCommittedBuilder = {
                    exercise = committed.exercise.id
                    statement = committed.statement.id
                    tests = committed.tests.ids.toMutableList()
                    developerSolutions = committed.developerSolutions.ids.toMutableList()
                    supportedTrikStudioVersions = committed.supportedTrikStudioVersions.toMutableList()
                },
            )
        }
        repository.update(edited)

        val found = assertNotNull(repository.findById(saved.id))
        val foundContent = assertIs<TaskContent.Uncommitted>(found.data.content)
        assertSameCommitted(committed, foundContent.lastCommitted)
        assertEquals(setOf(wipPolygonId), foundContent.wip.tests.ids.map { it.value }.toSet())
        assertEquals(2, taskContentJpaEntityRepository.count())
    }

    @Test
    fun `update reconciles the shared community rows`() {
        val saved = repository.save(newData())
        val kept = saved.data.sharedTo.ids.single()
        val added = fixtures.community().id

        repository.update(saved.withData { sharedTo = mutableListOf(kept, added) })
        repository.update(assertNotNull(repository.findById(saved.id)).withData { sharedTo = mutableListOf(added) })

        val found = assertNotNull(repository.findById(saved.id))
        assertEquals(listOf(added), found.data.sharedTo.ids)
        assertEquals(1, communityToTaskJpaEntityRepository.findAllByTaskId(saved.id.value).size)
    }

    @Test
    fun `save fails for an unregistered TRIK Studio version`() {
        val ownerId = fixtures.developer().id.value
        val exerciseId = fixtures.exercise().id.value
        val statementId = fixtures.statement().id.value
        val data = taskData {
            owner(ownerId)
            name = fixtures.unique("Task")
            description = "Task description"
            content.committed {
                exercise(exerciseId)
                statement(statementId)
                supportedTrikStudioVersions = mutableListOf(TrikStudioVersion(fixtures.unique("unregistered")))
            }
        }

        assertFailsWith<IllegalArgumentException> { repository.save(data) }
    }
}
