package tech.testsys.infra.database.api.persistence.adapter.task

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import tech.testsys.domain.builder.api.developerSolution
import tech.testsys.domain.builder.api.developerSolutionData
import tech.testsys.domain.builder.api.withData
import tech.testsys.domain.contract.persistence.repository.DeveloperSolutionRepository
import tech.testsys.domain.model.task.DeveloperSolution
import tech.testsys.domain.model.task.DeveloperSolutionData
import tech.testsys.domain.model.task.DeveloperSolutionId
import tech.testsys.domain.model.task.VersionBucket
import tech.testsys.infra.database.api.persistence.adapter.UpdatablePersistenceAdapterContractTests
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.repository.task.DeveloperSolutionJpaEntityRepository
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

@OptIn(InternalDatabaseApi::class)
class DeveloperSolutionPersistenceAdapterTests :
    UpdatablePersistenceAdapterContractTests<DeveloperSolutionData, DeveloperSolutionId, DeveloperSolution>() {

    @Autowired
    override lateinit var repository: DeveloperSolutionRepository

    @Autowired
    private lateinit var jpaEntityRepository: DeveloperSolutionJpaEntityRepository

    override fun newData(): DeveloperSolutionData {
        val solutionId = fixtures.solution().id.value
        return developerSolutionData {
            name = fixtures.unique("Developer solution")
            description = "Developer solution description"
            solution(solutionId)
            expectedScore(100)
            versionBucket = VersionBucket(UUID.randomUUID())
        }
    }

    override fun modified(entity: DeveloperSolution) = entity.withData {
        name = fixtures.unique("Renamed developer solution")
        description = "Updated description"
    }

    override fun detached(entity: DeveloperSolution) = developerSolution {
        id = entity.id.value
        createdAt = entity.createdAt
        data = entity.data
    }

    override fun idOf(value: Long) = DeveloperSolutionId(value)

    override fun assertSameData(expected: DeveloperSolution, actual: DeveloperSolution) {
        assertEquals(expected.data.name, actual.data.name)
        assertEquals(expected.data.description, actual.data.description)
        assertEquals(expected.data.solution.id, actual.data.solution.id)
        assertEquals(expected.data.expectedScore, actual.data.expectedScore)
        assertEquals(expected.data.versionBucket, actual.data.versionBucket)
    }

    @Test
    fun `should keep the version bucket if another bucket is passed on update`() {
        val saved = repository.save(newData())

        val updated = repository.update(saved.withData { versionBucket = VersionBucket(UUID.randomUUID()) })

        assertEquals(saved.data.versionBucket, updated.data.versionBucket)
        assertEquals(saved.data.versionBucket, assertNotNull(repository.findById(saved.id)).data.versionBucket)
    }

    @Test
    fun `should fail to update a developer solution if the solution reference changed`() {
        val saved = repository.save(newData())
        val anotherSolutionId = fixtures.solution().id.value

        assertFailsWith<UnsupportedOperationException> { repository.update(saved.withData { solution(anotherSolutionId) }) }

        assertSameEntity(saved, assertNotNull(repository.findById(saved.id)))
    }

    @Test
    fun `should fail to update a developer solution if the expected score changed`() {
        val saved = repository.save(newData())

        assertFailsWith<UnsupportedOperationException> { repository.update(saved.withData { expectedScore(50) }) }

        assertSameEntity(saved, assertNotNull(repository.findById(saved.id)))
    }

    @Test
    fun `should keep the solution of the previous version when a new version is saved in the same bucket`() {
        val previous = repository.save(newData())
        val nextSolutionId = fixtures.solution().id.value

        val next = repository.save(
            developerSolutionData {
                name = previous.data.name
                description = previous.data.description
                solution(nextSolutionId)
                expectedScore(previous.data.expectedScore.value)
                versionBucket = previous.data.versionBucket
            },
        )

        assertSameEntity(previous, assertNotNull(repository.findById(previous.id)))
        assertSameEntity(next, assertNotNull(repository.findById(next.id)))
        assertEquals(previous.data.versionBucket, next.data.versionBucket)
    }

    @Test
    fun `should keep the expected score of the previous version when a new version shares its solution`() {
        val previous = repository.save(newData())

        val next = repository.save(
            developerSolutionData {
                name = previous.data.name
                description = previous.data.description
                solution(previous.data.solution.id.value)
                expectedScore(50)
                versionBucket = previous.data.versionBucket
            },
        )

        assertSameEntity(previous, assertNotNull(repository.findById(previous.id)))
        assertSameEntity(next, assertNotNull(repository.findById(next.id)))
        assertEquals(previous.data.solution.id, next.data.solution.id)
    }

    @Test
    fun `should find the latest version by creation time even with a lower id`() {
        val first = repository.save(newData())
        val second = repository.save(first.withData { name = "Another version" }.data)
        setCreatedAt(id = first.id, createdAt = Instant.ofEpochSecond(20))
        setCreatedAt(id = second.id, createdAt = Instant.ofEpochSecond(10))

        val latest = repository.findLatestByVersionBucket(first.data.versionBucket)

        assertEquals(first.id, latest?.id)
    }

    @Test
    fun `should choose the higher id when creation times are equal`() {
        val first = repository.save(newData())
        val second = repository.save(first.withData { name = "Another version" }.data)
        setCreatedAt(id = first.id, createdAt = Instant.EPOCH)
        setCreatedAt(id = second.id, createdAt = Instant.EPOCH)

        val latest = repository.findLatestByVersionBucket(first.data.versionBucket)

        assertEquals(second.id, latest?.id)
    }

    @Test
    fun `should keep the latest version after an older version is renamed`() {
        val first = repository.save(newData())
        val second = repository.save(first.withData { name = "Another version" }.data)
        setCreatedAt(id = first.id, createdAt = Instant.ofEpochSecond(10))
        setCreatedAt(id = second.id, createdAt = Instant.ofEpochSecond(20))
        val loadedFirst = assertNotNull(repository.findById(first.id))
        repository.update(loadedFirst.withData { name = "Renamed older version" })

        val latest = repository.findLatestByVersionBucket(first.data.versionBucket)

        assertEquals(second.id, latest?.id)
    }

    @Test
    fun `should isolate version chains and return null for a missing chain`() {
        val first = repository.save(newData())
        repository.save(newData())

        val latest = repository.findLatestByVersionBucket(first.data.versionBucket)
        val missing = repository.findLatestByVersionBucket(VersionBucket(UUID(0, 99)))

        assertEquals(first.id, latest?.id)
        assertEquals(null, missing)
    }

    private fun setCreatedAt(id: DeveloperSolutionId, createdAt: Instant) {
        val entity = jpaEntityRepository.findById(id.value).orElseThrow()
        entity.createdAt = createdAt
        jpaEntityRepository.saveAndFlush(entity)
    }
}
