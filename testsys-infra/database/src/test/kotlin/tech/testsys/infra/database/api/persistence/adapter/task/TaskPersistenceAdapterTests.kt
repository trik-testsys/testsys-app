package tech.testsys.infra.database.api.persistence.adapter.task

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DataIntegrityViolationException
import tech.testsys.domain.builder.api.task
import tech.testsys.domain.builder.api.taskData
import tech.testsys.domain.builder.api.withData
import tech.testsys.domain.contract.persistence.repository.StatementRepository
import tech.testsys.domain.contract.persistence.repository.TaskRepository
import tech.testsys.domain.model.task.CommittedTaskContent
import tech.testsys.domain.model.task.Task
import tech.testsys.domain.model.task.TaskContent
import tech.testsys.domain.model.task.TaskData
import tech.testsys.domain.model.task.TaskId
import tech.testsys.domain.model.task.TrikStudioVersion
import tech.testsys.domain.model.task.WipTaskContent
import tech.testsys.infra.database.api.persistence.adapter.UpdatablePersistenceAdapterContractTests
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.repository.task.CommunityToTaskJpaEntityRepository
import tech.testsys.infra.database.internal.jpa.repository.task.TaskContentJpaEntityRepository
import tech.testsys.infra.database.internal.jpa.repository.task.TestToTaskContentJpaEntityRepository
import tech.testsys.infra.database.internal.jpa.repository.task.VersionBucketToTaskJpaEntityRepository
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNotNull

@OptIn(InternalDatabaseApi::class)
class TaskPersistenceAdapterTests : UpdatablePersistenceAdapterContractTests<TaskData, TaskId, Task>() {

    @Autowired
    override lateinit var repository: TaskRepository

    @Autowired
    private lateinit var taskContentJpaEntityRepository: TaskContentJpaEntityRepository

    @Autowired
    private lateinit var testToTaskContentJpaEntityRepository: TestToTaskContentJpaEntityRepository

    @Autowired
    private lateinit var communityToTaskJpaEntityRepository: CommunityToTaskJpaEntityRepository

    @Autowired
    private lateinit var versionBucketToTaskJpaEntityRepository: VersionBucketToTaskJpaEntityRepository

    @Autowired
    private lateinit var statementRepository: StatementRepository

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
            uploadedResources = mutableSetOf(fixtures.statement().data.versionBucket)
        }
    }

    override fun detached(entity: Task) = task {
        id = entity.id.value
        createdAt = entity.createdAt
        data = entity.data
    }

    override fun idOf(value: Long) = TaskId(value)

    override fun assertSameData(expected: Task, actual: Task) {
        assertEquals(expected.data.owner.id, actual.data.owner.id)
        assertEquals(expected.data.name, actual.data.name)
        assertEquals(expected.data.description, actual.data.description)
        assertEquals(expected.data.sharedTo.ids.toSet(), actual.data.sharedTo.ids.toSet())
        assertEquals(expected.data.uploadedResources, actual.data.uploadedResources)
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
    fun `should keep new content without exercise and statement through a round trip`() {
        val data = newTaskData()

        val saved = repository.save(data)
        val found = assertNotNull(repository.findById(saved.id))

        assertSameContent(data.content, saved.data.content)
        assertSameContent(data.content, found.data.content)
        assertEquals(1, taskContentJpaEntityRepository.count())
    }

    @Test
    fun `should keep the wip and the last committed revisions apart for uncommitted content`() {
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
    fun `should replace the content rows when a task is committed`() {
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
    fun `should add a wip revision beside the committed one when a committed task is edited`() {
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
    fun `should reconcile the shared community rows on update`() {
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
    fun `should keep a task without description as a task with an empty description`() {
        val ownerId = fixtures.developer().id.value

        val saved = repository.save(
            taskData {
                owner(ownerId)
                name = fixtures.unique("Task without description")
                description = ""
                content.new {}
            },
        )

        val found = assertNotNull(repository.findById(saved.id))
        assertEquals("", saved.data.description)
        assertEquals("", found.data.description)
    }

    @Test
    fun `should fail to save a task with an unregistered TRIK Studio version`() {
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

    @Test
    fun `should keep the owner if another owner is passed on update`() {
        val saved = repository.save(newData())
        val otherOwner = fixtures.developer().id

        val updated = repository.update(saved.withData { owner = otherOwner })

        assertEquals(saved.data.owner.id, updated.data.owner.id)
        assertEquals(saved.data.owner.id, assertNotNull(repository.findById(saved.id)).data.owner.id)
    }

    @Test
    fun `should keep unattached uploaded resource chains through a round trip`() {
        val statement = fixtures.statement()
        val data = newTaskData()
        val saved = repository.save(
            taskData {
                owner = data.owner.id
                name = data.name
                description = data.description
                content.new {}
                uploadedResources.add(statement.data.versionBucket)
            },
        )

        val found = assertNotNull(repository.findById(saved.id))

        assertEquals(setOf(statement.data.versionBucket), saved.data.uploadedResources)
        assertEquals(saved.data.uploadedResources, found.data.uploadedResources)
        assertSameContent(saved.data.content, found.data.content)
    }

    @Test
    fun `should add uploaded chains without changing committed content`() {
        val saved = repository.save(newData())
        val added = fixtures.statement().data.versionBucket

        val updated = repository.update(saved.withData { uploadedResources.add(added) })

        assertEquals(saved.data.uploadedResources + added, updated.data.uploadedResources)
        val found = assertNotNull(repository.findById(saved.id))
        assertEquals(updated.data.uploadedResources, found.data.uploadedResources)
        assertSameContent(saved.data.content, found.data.content)
    }

    @Test
    fun `should remove an uploaded chain used in content without changing content`() {
        val saved = fixtures.task()
        val removed = saved.data.uploadedResources.first()
        val expected = saved.data.uploadedResources - removed

        val updated = repository.update(saved.withData { uploadedResources.remove(removed) })

        assertEquals(expected, updated.data.uploadedResources)
        val found = assertNotNull(repository.findById(saved.id))
        assertEquals(expected, found.data.uploadedResources)
        assertSameContent(saved.data.content, found.data.content)
        assertEquals(1, versionBucketToTaskJpaEntityRepository.findAllByTaskId(saved.id.value).size)
    }

    @Test
    fun `should reject uploading the same chain to two tasks simultaneously`() {
        val first = fixtures.task()
        val second = fixtures.task()
        val bucket = first.data.uploadedResources.first()

        assertFailsWith<DataIntegrityViolationException> {
            repository.update(second.withData { uploadedResources.add(bucket) })
        }
        assertEquals(first.data.uploadedResources, assertNotNull(repository.findById(first.id)).data.uploadedResources)
        assertEquals(second.data.uploadedResources, assertNotNull(repository.findById(second.id)).data.uploadedResources)
    }

    @Test
    fun `should remove memberships while preserving resource versions when a task is removed`() {
        val saved = fixtures.task()
        val statementId = assertIs<TaskContent.Committed>(saved.data.content).lastCommitted.statement.id
        val statement = assertNotNull(statementRepository.findById(statementId))

        repository.removeById(saved.id)

        assertEquals(emptyList(), versionBucketToTaskJpaEntityRepository.findAllByTaskId(saved.id.value))
        val retained = assertNotNull(statementRepository.findById(statementId))
        assertEquals(statement.data.versionBucket, retained.data.versionBucket)
        assertEquals(statement.data.file.content.toList(), retained.data.file.content.toList())
    }
}
