package tech.testsys.infra.database.api.persistence.adapter.task

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DataIntegrityViolationException
import tech.testsys.domain.builder.api.task
import tech.testsys.domain.builder.api.taskData
import tech.testsys.domain.builder.api.withData
import tech.testsys.domain.contract.persistence.repository.ExerciseRepository
import tech.testsys.domain.contract.persistence.repository.StatementRepository
import tech.testsys.domain.contract.persistence.repository.TaskRepository
import tech.testsys.domain.model.group.CommunityId
import tech.testsys.domain.model.task.CommittedTaskContent
import tech.testsys.domain.model.task.Task
import tech.testsys.domain.model.task.TaskContent
import tech.testsys.domain.model.task.TaskData
import tech.testsys.domain.model.task.TaskId
import tech.testsys.domain.model.task.TrikStudioVersion
import tech.testsys.domain.model.task.TrikSupportedLanguage
import tech.testsys.domain.model.task.WipTaskContent
import tech.testsys.domain.model.user.MultipleRoleUserId
import tech.testsys.infra.database.api.persistence.adapter.UpdatablePersistenceAdapterContractTests
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.repository.task.CommunityToTaskJpaEntityRepository
import tech.testsys.infra.database.internal.jpa.repository.task.ExerciseToTaskContentJpaEntityRepository
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

    @Autowired
    private lateinit var exerciseToTaskContentJpaEntityRepository: ExerciseToTaskContentJpaEntityRepository

    @Autowired
    private lateinit var exerciseRepository: ExerciseRepository

    override fun newData(): TaskData {
        val ownerId = fixtures.developer().id.value
        val communityId = fixtures.community().id.value
        val exerciseIds = listOf(
            fixtures.exercise().id.value,
            fixtures.exercise(TrikSupportedLanguage.JavaScript).id.value,
        )
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
                exercises(exerciseIds)
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

    @Nested
    inner class FindAvailableToDeveloperTests {

        @Test
        fun `should return owned and shared tasks while excluding inaccessible tasks`() {
            val owner = fixtures.developer().id
            val otherOwner = fixtures.developer().id
            val community = fixtures.community().id
            val unrelatedCommunity = fixtures.community().id
            val owned = saveTask(ownerId = owner, communityIds = emptyList())
            val shared = saveTask(ownerId = otherOwner, communityIds = listOf(community))
            saveTask(ownerId = otherOwner, communityIds = emptyList())
            saveTask(ownerId = otherOwner, communityIds = listOf(unrelatedCommunity))

            val result = repository.findAvailableToDeveloper(ownerId = owner, communityIds = setOf(community))

            assertEquals(setOf(owned.id, shared.id), result.map { it.id }.toSet())
            assertUnchangedTask(owned, result.single { it.id == owned.id })
            assertUnchangedTask(shared, result.single { it.id == shared.id })
        }

        @Test
        fun `should return each task once when ownership and several shared communities overlap`() {
            val owner = fixtures.developer().id
            val communities = listOf(fixtures.community().id, fixtures.community().id)
            val owned = saveTask(ownerId = owner, communityIds = communities)
            val shared = saveTask(ownerId = fixtures.developer().id, communityIds = communities)

            val result = repository.findAvailableToDeveloper(ownerId = owner, communityIds = communities.toSet())

            assertEquals(2, result.size)
            assertEquals(setOf(owned.id, shared.id), result.map { it.id }.toSet())
        }

        @Test
        fun `should return only owned tasks when the community set is empty`() {
            val owner = fixtures.developer().id
            val community = fixtures.community().id
            val owned = saveTask(ownerId = owner, communityIds = listOf(community))
            saveTask(ownerId = fixtures.developer().id, communityIds = listOf(community))

            val result = repository.findAvailableToDeveloper(ownerId = owner, communityIds = emptySet())

            assertEquals(listOf(owned.id), result.map { it.id })
        }

        @ParameterizedTest
        @ValueSource(booleans = [false, true])
        fun `should return an empty list when no task is available`(withCommunities: Boolean) {
            val owner = fixtures.developer().id
            val community = fixtures.community().id
            saveTask(ownerId = fixtures.developer().id, communityIds = emptyList())
            val communities = communityIds(withCommunities, community)

            val result = repository.findAvailableToDeveloper(ownerId = owner, communityIds = communities)

            assertEquals(emptyList(), result)
        }

        @ParameterizedTest
        @ValueSource(strings = ["New", "Uncommitted", "Committed"])
        fun `should retain all task data and revisions without changing stored state`(state: String) {
            val saved = repository.save(newData())
            val committed = assertIs<TaskContent.Committed>(saved.data.content).lastCommitted
            val expected = repository.update(taskInState(saved, committed, state))
            val unrelatedOwner = fixtures.developer().id

            val result = repository.findAvailableToDeveloper(
                ownerId = unrelatedOwner,
                communityIds = expected.data.sharedTo.ids.toSet(),
            ).single()

            assertEquals(expected.id, result.id)
            assertSameData(expected, result)
            assertEquals(expected.version, result.version)
            val retained = assertNotNull(repository.findById(expected.id))
            assertSameData(expected, retained)
            assertEquals(expected.version, retained.version)
        }

        private fun saveTask(ownerId: MultipleRoleUserId, communityIds: List<CommunityId>): Task = repository.save(
            taskData {
                owner = ownerId
                name = fixtures.unique("Available task")
                description = "Task description"
                sharedTo = communityIds.toMutableList()
                content.new {}
            },
        )

        private fun communityIds(withCommunities: Boolean, community: CommunityId): Set<CommunityId> =
            if (withCommunities) setOf(community) else emptySet()

        private fun assertUnchangedTask(expected: Task, actual: Task) {
            assertSameData(expected, actual)
            assertEquals(expected.createdAt, actual.createdAt)
            assertEquals(expected.version, actual.version)
        }

        private fun taskInState(saved: Task, committed: CommittedTaskContent, state: String): Task = saved.withData {
            uploadedResources.add(fixtures.statement().data.versionBucket)
            when (state) {
                "New" -> content.new { tests = committed.tests.ids.toMutableList() }
                "Uncommitted" -> content.uncommitted(
                    wipBuilder = { tests = committed.tests.ids.toMutableList() },
                    lastCommittedBuilder = {
                        exercises = committed.exercises.ids.toMutableList()
                        statement = committed.statement.id
                        tests = committed.tests.ids.toMutableList()
                    },
                )
                "Committed" -> Unit
                else -> error("Unsupported test state: $state")
            }
        }
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
        assertEquals(expected.exercises.ids.toSet(), actual.exercises.ids.toSet())
        assertEquals(expected.statement?.id, actual.statement?.id)
        assertEquals(expected.developerSolutions.ids.toSet(), actual.developerSolutions.ids.toSet())
        assertEquals(expected.supportedTrikStudioVersions.toSet(), actual.supportedTrikStudioVersions.toSet())
    }

    private fun assertSameCommitted(expected: CommittedTaskContent, actual: CommittedTaskContent) {
        assertEquals(expected.tests.ids.toSet(), actual.tests.ids.toSet())
        assertEquals(expected.exercises.ids.toSet(), actual.exercises.ids.toSet())
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
        val wipExerciseIds = listOf(
            fixtures.exercise().id.value,
            fixtures.exercise(TrikSupportedLanguage.JavaScript).id.value,
        )
        val committedPolygonId = fixtures.polygon().id.value
        val wipPolygonId = fixtures.polygon().id.value
        val data = taskData {
            owner(ownerId)
            name = fixtures.unique("Uncommitted task")
            description = "Edited after a commit"
            content.uncommitted(
                wipBuilder = {
                    exercises(wipExerciseIds)
                    tests(listOf(committedPolygonId, wipPolygonId))
                },
                lastCommittedBuilder = {
                    exercises(listOf(committedExerciseId))
                    statement(committedStatementId)
                    tests(listOf(committedPolygonId))
                },
            )
        }

        val saved = repository.save(data)
        val found = assertNotNull(repository.findById(saved.id))

        assertSameContent(data.content, found.data.content)
        assertEquals(2, taskContentJpaEntityRepository.count())
        assertEquals(3, exerciseToTaskContentJpaEntityRepository.count())
        assertEquals(3, testToTaskContentJpaEntityRepository.count())
    }

    @Test
    fun `should replace exercise associations without deleting resource versions or files`() {
        val saved = repository.save(newData())
        val originalIds = assertIs<TaskContent.Committed>(saved.data.content).lastCommitted.exercises.ids
        val originalResources = exerciseRepository.findByIds(originalIds)
        val replacement = fixtures.exercise()

        repository.update(saved.withData { content.committed { exercises = mutableListOf(replacement.id) } })

        val found = assertNotNull(repository.findById(saved.id))
        val content = assertIs<TaskContent.Committed>(found.data.content).lastCommitted
        assertEquals(listOf(replacement.id), content.exercises.ids)
        assertEquals(1, exerciseToTaskContentJpaEntityRepository.count())
        assertEquals(originalIds.toSet(), exerciseRepository.findByIds(originalIds).map { it.id }.toSet())
        assertEquals(
            originalResources.sortedBy { it.id.value }.map { it.data.file.content.toList() },
            exerciseRepository.findByIds(originalIds).sortedBy { it.id.value }.map { it.data.file.content.toList() },
        )
    }

    @Test
    fun `should remove all exercise associations while retaining every exercise resource`() {
        val saved = repository.save(newData())
        val exerciseIds = assertIs<TaskContent.Committed>(saved.data.content).lastCommitted.exercises.ids
        val original = exerciseRepository.findByIds(exerciseIds)

        repository.removeById(saved.id)

        assertEquals(0, exerciseToTaskContentJpaEntityRepository.count())
        assertEquals(0, taskContentJpaEntityRepository.count())
        val retained = exerciseRepository.findByIds(exerciseIds)
        assertEquals(exerciseIds.toSet(), retained.map { it.id }.toSet())
        assertEquals(
            original.sortedBy { it.id.value }.map { it.data.file.content.toList() },
            retained.sortedBy { it.id.value }.map { it.data.file.content.toList() },
        )
    }

    @Test
    fun `should replace the content rows when a task is committed`() {
        val saved = repository.save(newTaskData())
        val exerciseId = fixtures.exercise().id.value
        val statementId = fixtures.statement().id.value
        val polygonIds = listOf(fixtures.polygon().id.value, fixtures.polygon().id.value)

        val committed = saved.withData {
            content.committed {
                exercises(listOf(exerciseId))
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
                    exercises = committed.exercises.ids.toMutableList()
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
                exercises(listOf(exerciseId))
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
    fun `should replace committed content with editable content having registered versions before deleting old rows`() {
        val saved = repository.save(newData())
        val version = fixtures.trikStudioVersion()
        val polygon = fixtures.polygon()
        val before = assertIs<TaskContent.Committed>(saved.data.content).lastCommitted

        val updated = repository.update(
            saved.withData {
                content.uncommitted(
                    wipBuilder = {
                        tests = mutableListOf(polygon.id)
                        supportedTrikStudioVersions = mutableListOf(version)
                    },
                    lastCommittedBuilder = {
                        statement = before.statement.id
                        exercises = before.exercises.ids.toMutableList()
                        tests = before.tests.ids.toMutableList()
                        developerSolutions = before.developerSolutions.ids.toMutableList()
                        supportedTrikStudioVersions = before.supportedTrikStudioVersions.toMutableList()
                    },
                )
            },
        )

        val content = assertIs<TaskContent.Uncommitted>(updated.data.content)
        assertEquals(listOf(version), content.wip.supportedTrikStudioVersions)
        assertEquals(listOf(polygon.id), content.wip.tests.ids)
        assertSameContent(updated.data.content, assertNotNull(repository.findById(saved.id)).data.content)
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
