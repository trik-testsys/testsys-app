package tech.testsys.infra.database.api.persistence.adapter.task

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.springframework.beans.factory.annotation.Autowired
import tech.testsys.domain.builder.api.contest
import tech.testsys.domain.builder.api.contestData
import tech.testsys.domain.builder.api.withData
import tech.testsys.domain.contract.persistence.Pagination
import tech.testsys.domain.contract.persistence.repository.ContestRepository
import tech.testsys.domain.model.group.CommunityId
import tech.testsys.domain.model.task.Contest
import tech.testsys.domain.model.task.ContestData
import tech.testsys.domain.model.task.ContestId
import tech.testsys.domain.model.task.TaskId
import tech.testsys.domain.model.task.TrikStudioVersion
import tech.testsys.domain.model.user.MultipleRoleUserId
import tech.testsys.infra.database.api.persistence.adapter.UpdatablePersistenceAdapterContractTests
import java.time.Duration
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class ContestPersistenceAdapterTests : UpdatablePersistenceAdapterContractTests<ContestData, ContestId, Contest>() {

    @Autowired
    override lateinit var repository: ContestRepository

    @Nested
    inner class FindAvailableToDeveloperTests {

        @Test
        fun `should find owned and shared contests while excluding inaccessible contests`() {
            val owner = fixtures.developer().id
            val otherOwner = fixtures.developer().id
            val community = fixtures.community().id
            val unrelatedCommunity = fixtures.community().id
            val owned = saveContest(ownerId = owner, communityIds = emptyList())
            val shared = saveContest(ownerId = otherOwner, communityIds = listOf(community))
            saveContest(ownerId = otherOwner, communityIds = emptyList())
            saveContest(ownerId = otherOwner, communityIds = listOf(unrelatedCommunity))

            val result = repository.findAvailableToDeveloper(
                ownerId = owner,
                communityIds = setOf(community),
                pagination = Pagination(page = 0, size = 10),
            ).content

            assertEquals(setOf(owned.id, shared.id), result.map { it.id }.toSet())
            assertSameData(owned, result.single { it.id == owned.id })
            assertSameData(shared, result.single { it.id == shared.id })
            assertEquals(shared.version, result.single { it.id == shared.id }.version)
        }

        @Test
        fun `should return contests once when ownership and shared communities overlap`() {
            val owner = fixtures.developer().id
            val communities = listOf(fixtures.community().id, fixtures.community().id)
            val owned = saveContest(ownerId = owner, communityIds = communities)
            val shared = saveContest(ownerId = fixtures.developer().id, communityIds = communities)

            val result = repository.findAvailableToDeveloper(
                ownerId = owner,
                communityIds = communities.toSet(),
                pagination = Pagination(page = 0, size = 10),
            ).content

            assertEquals(2, result.size)
            assertEquals(setOf(owned.id, shared.id), result.map { it.id }.toSet())
        }

        @Test
        fun `should find only owned contests when no communities grant access`() {
            val owner = fixtures.developer().id
            val community = fixtures.community().id
            val owned = saveContest(ownerId = owner, communityIds = listOf(community))
            saveContest(ownerId = fixtures.developer().id, communityIds = listOf(community))

            val result = repository.findAvailableToDeveloper(
                ownerId = owner,
                communityIds = emptySet(),
                pagination = Pagination(page = 0, size = 10),
            ).content

            assertEquals(listOf(owned.id), result.map { it.id })
        }
    }

    override fun newData(): ContestData = newDataWithLimits(total = Duration.ofHours(2), attempt = Duration.ofMinutes(30))

    override fun modified(entity: Contest): Contest {
        val keptTask = entity.data.tasks.ids.first()
        val newTask = fixtures.task().id
        val newVersion = fixtures.trikStudioVersion()
        val newCommunity = fixtures.community().id
        return entity.withData {
            name = fixtures.unique("Renamed contest")
            description = "Updated description"
            tasks = mutableListOf(keptTask, newTask)
            startsAt = null
            contestDuration = Duration.ofHours(3)
            attemptDuration = Duration.ofHours(1)
            trikStudioVersion = newVersion
            sharedTo = mutableListOf(newCommunity)
        }
    }

    override fun detached(entity: Contest) = contest {
        id = entity.id.value
        createdAt = entity.createdAt
        data = entity.data
    }

    override fun idOf(value: Long) = ContestId(value)

    override fun assertSameData(expected: Contest, actual: Contest) {
        assertEquals(expected.data.owner.id, actual.data.owner.id)
        assertEquals(expected.data.name, actual.data.name)
        assertEquals(expected.data.description, actual.data.description)
        assertEquals(expected.data.tasks.ids.toSet(), actual.data.tasks.ids.toSet())
        assertEquals(expected.data.startsAt, actual.data.startsAt)
        assertEquals(expected.data.contestDuration, actual.data.contestDuration)
        assertEquals(expected.data.attemptDuration, actual.data.attemptDuration)
        assertEquals(expected.data.trikStudioVersion, actual.data.trikStudioVersion)
        assertEquals(expected.data.sharedTo.ids.toSet(), actual.data.sharedTo.ids.toSet())
    }

    @ParameterizedTest
    @CsvSource("false,false", "false,true", "true,false", "true,true")
    fun `should save and read each combination of absent and finite limits`(hasTotal: Boolean, hasAttempt: Boolean) {
        val total = Duration.ofMillis(7_200_001).takeIf { hasTotal }
        val attempt = Duration.ofMillis(1_800_001).takeIf { hasAttempt }
        val data = newDataWithLimits(total = total, attempt = attempt)

        val saved = repository.save(data)

        val found = assertNotNull(repository.findById(saved.id))
        assertEquals(total, saved.data.contestDuration)
        assertEquals(attempt, saved.data.attemptDuration)
        assertEquals(total, found.data.contestDuration)
        assertEquals(attempt, found.data.attemptDuration)
    }

    @ParameterizedTest
    @CsvSource("false,false", "false,true", "true,false", "true,true")
    fun `should update and read each combination of absent and finite limits`(hasTotal: Boolean, hasAttempt: Boolean) {
        val saved = repository.save(newData())
        val total = Duration.ofMillis(10_800_001).takeIf { hasTotal }
        val attempt = Duration.ofMillis(3_600_001).takeIf { hasAttempt }

        val updated = repository.update(
            saved.withData {
                contestDuration = total
                attemptDuration = attempt
            },
        )

        val found = assertNotNull(repository.findById(updated.id))
        assertEquals(total, updated.data.contestDuration)
        assertEquals(attempt, updated.data.attemptDuration)
        assertEquals(total, found.data.contestDuration)
        assertEquals(attempt, found.data.attemptDuration)
    }

    @Test
    fun `should leave a contest without a start moment without an end moment`() {
        val ownerId = fixtures.developer().id.value
        val version = fixtures.trikStudioVersion()

        val saved = repository.save(
            contestData {
                owner(ownerId)
                name = fixtures.unique("Unscheduled contest")
                description = "Not scheduled yet"
                contestDuration = Duration.ofHours(2)
                attemptDuration = Duration.ofMinutes(30)
                trikStudioVersion = version
            },
        )

        val found = assertNotNull(repository.findById(saved.id))
        assertNull(found.data.startsAt)
        assertNull(found.data.endsAt)
        assertEquals(emptyList(), found.data.tasks.ids)
        assertEquals(emptyList(), found.data.sharedTo.ids)
    }

    @Test
    fun `should fail to save a contest with an unregistered TRIK Studio version`() {
        val ownerId = fixtures.developer().id.value

        assertFailsWith<IllegalArgumentException> {
            repository.save(
                contestData {
                    owner(ownerId)
                    name = fixtures.unique("Contest")
                    description = "Contest description"
                    contestDuration = Duration.ofHours(2)
                    attemptDuration = Duration.ofMinutes(30)
                    trikStudioVersion = TrikStudioVersion(fixtures.unique("unregistered"))
                },
            )
        }
    }

    @Test
    fun `should fail to update a contest with an unregistered TRIK Studio version`() {
        val saved = repository.save(newData())

        assertFailsWith<IllegalArgumentException> {
            repository.update(saved.withData { trikStudioVersion = TrikStudioVersion(fixtures.unique("unregistered")) })
        }
        assertEquals(saved.data.trikStudioVersion, assertNotNull(repository.findById(saved.id)).data.trikStudioVersion)
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
    fun `should increment the contest version on an update of the tasks only`() {
        val saved = repository.save(newData())
        val task = fixtures.task().id

        val updated = repository.update(saved.withData { tasks = mutableListOf(task) })

        assertEquals(assertNotNull(saved.version).value + 1, assertNotNull(updated.version).value)
        assertEquals(updated.version, assertNotNull(repository.findById(saved.id)).version)
    }

    @Test
    fun `should find contests by ids with the same statement count for one and twenty ids`() {
        val data = newData()
        val ids = List(20) { repository.save(data).id }

        val (one, oneIdStatements) = withStatementCount { repository.findByIds(ids.take(1)) }
        val (twenty, twentyIdsStatements) = withStatementCount { repository.findByIds(ids) }

        assertEquals(ids.take(1), one.map { contest -> contest.id })
        assertEquals(ids.toSet(), twenty.map { contest -> contest.id }.toSet())
        assertEquals(List(20) { data.tasks.ids.toSet() }, twenty.map { contest -> contest.data.tasks.ids.toSet() })
        assertEquals(List(20) { data.trikStudioVersion }, twenty.map { contest -> contest.data.trikStudioVersion })
        assertEquals(oneIdStatements, twentyIdsStatements)
    }

    @Test
    fun `should find contests of a task with the same statement count for one and twenty contests`() {
        val oneContestTask = fixtures.task()
        val twentyContestsTask = fixtures.task()
        val community = fixtures.community().id
        val owner = fixtures.developer().id
        val single = saveContest(ownerId = owner, communityIds = listOf(community), taskIds = listOf(oneContestTask.id))
        val twentyIds = List(20) {
            saveContest(ownerId = owner, communityIds = listOf(community), taskIds = listOf(twentyContestsTask.id)).id
        }

        val (one, oneContestStatements) = withStatementCount { repository.findByTaskId(oneContestTask.id) }
        val (twenty, twentyContestsStatements) = withStatementCount { repository.findByTaskId(twentyContestsTask.id) }

        assertEquals(listOf(single.id), one.map { contest -> contest.id })
        assertEquals(twentyIds.sortedBy { id -> id.value }, twenty.map { contest -> contest.id })
        assertEquals(List(20) { listOf(community) }, twenty.map { contest -> contest.data.sharedTo.ids })
        assertEquals(oneContestStatements, twentyContestsStatements)
    }

    @Test
    fun `should save one task and community link per id when ids repeat`() {
        val taskId = fixtures.task().id
        val communityId = fixtures.community().id

        val saved = saveContest(
            ownerId = fixtures.developer().id,
            communityIds = listOf(communityId, communityId),
            taskIds = listOf(taskId, taskId),
        )

        val found = assertNotNull(repository.findById(saved.id))
        assertEquals(listOf(taskId), found.data.tasks.ids)
        assertEquals(listOf(communityId), found.data.sharedTo.ids)
    }

    private fun newDataWithLimits(total: Duration?, attempt: Duration?): ContestData {
        val ownerId = fixtures.developer().id.value
        val taskIds = listOf(fixtures.task().id, fixtures.task().id)
        val communityIds = listOf(fixtures.community().id.value)
        val version = fixtures.trikStudioVersion()
        return contestData {
            owner(ownerId)
            name = fixtures.unique("Contest")
            description = "Contest description"
            tasks = taskIds.toMutableList()
            startsAt = STARTS_AT
            contestDuration = total
            attemptDuration = attempt
            trikStudioVersion = version
            sharedTo(communityIds)
        }
    }

    private fun saveContest(
        ownerId: MultipleRoleUserId,
        communityIds: List<CommunityId>,
        taskIds: List<TaskId> = listOf(fixtures.task().id),
    ): Contest = repository.save(
        contestData {
            owner = ownerId
            name = fixtures.unique("Available contest")
            description = "Available contest description"
            trikStudioVersion = fixtures.trikStudioVersion()
            tasks = taskIds.toMutableList()
            sharedTo = communityIds.toMutableList()
        },
    )

    private companion object {

        val STARTS_AT: Instant = Instant.parse("2026-09-04T10:00:00Z")
    }
}
