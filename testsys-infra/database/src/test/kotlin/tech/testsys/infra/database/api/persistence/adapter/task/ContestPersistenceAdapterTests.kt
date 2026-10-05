package tech.testsys.infra.database.api.persistence.adapter.task

import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.springframework.beans.factory.annotation.Autowired
import tech.testsys.domain.builder.api.contest
import tech.testsys.domain.builder.api.contestData
import tech.testsys.domain.builder.api.withData
import tech.testsys.domain.contract.persistence.repository.ContestRepository
import tech.testsys.domain.model.task.Contest
import tech.testsys.domain.model.task.ContestData
import tech.testsys.domain.model.task.ContestId
import tech.testsys.domain.model.task.TrikStudioVersion
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

    private companion object {

        val STARTS_AT: Instant = Instant.parse("2026-09-04T10:00:00Z")
    }
}
