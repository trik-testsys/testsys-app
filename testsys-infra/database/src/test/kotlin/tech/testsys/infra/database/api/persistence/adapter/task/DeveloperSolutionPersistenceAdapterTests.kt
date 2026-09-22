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
import tech.testsys.infra.database.api.persistence.adapter.PersistenceAdapterContractTests
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.repository.task.DeveloperSolutionJpaEntityRepository
import tech.testsys.infra.database.internal.jpa.repository.task.FileDataJpaEntityRepository
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

@OptIn(InternalDatabaseApi::class)
class DeveloperSolutionPersistenceAdapterTests :
    PersistenceAdapterContractTests<DeveloperSolutionData, DeveloperSolutionId, DeveloperSolution>() {

    @Autowired
    override lateinit var repository: DeveloperSolutionRepository

    @Autowired
    private lateinit var developerSolutionJpaEntityRepository: DeveloperSolutionJpaEntityRepository

    @Autowired
    private lateinit var fileDataJpaEntityRepository: FileDataJpaEntityRepository

    override fun newData(): DeveloperSolutionData {
        val solution = fixtures.solution()
        return developerSolutionData {
            name = fixtures.unique("Developer solution")
            description = "Developer solution description"
            solution(solution.id.value)
            expectedScore(100)
            versionBucket = solution.data.versionBucket
        }
    }

    override fun modified(entity: DeveloperSolution): DeveloperSolution {
        val newSolutionId = fixtures.solution(versionBucket = entity.data.versionBucket).id.value
        return entity.withData {
            name = fixtures.unique("Renamed developer solution")
            description = "Updated description"
            solution(newSolutionId)
            expectedScore(50)
        }
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

        val updated = repository.update(saved.withData { versionBucket = UUID.randomUUID() })

        assertEquals(saved.data.versionBucket, updated.data.versionBucket)
        assertEquals(saved.data.versionBucket, assertNotNull(repository.findById(saved.id)).data.versionBucket)
    }

    @Test
    fun `should keep all solution files of a developer solution in one bucket`() {
        val saved = repository.save(newData())

        repository.update(modified(saved))

        assertEquals(2, fileDataJpaEntityRepository.count())
        assertEquals(
            setOf(saved.data.versionBucket),
            fileDataJpaEntityRepository.findAll().map { it.versionBucket }.toSet(),
        )
    }

    @Test
    fun `should fail to save a developer solution if the solution belongs to another bucket`() {
        val foreignSolutionId = fixtures.solution().id.value
        val data = developerSolutionData {
            name = fixtures.unique("Developer solution")
            description = "Developer solution description"
            solution(foreignSolutionId)
            expectedScore(100)
            versionBucket = UUID.randomUUID()
        }

        assertFailsWith<IllegalArgumentException> { repository.save(data) }

        assertEquals(0, developerSolutionJpaEntityRepository.count())
    }

    @Test
    fun `should fail to update a developer solution if the solution belongs to another bucket`() {
        val saved = repository.save(newData())
        val foreignSolutionId = fixtures.solution().id.value

        assertFailsWith<IllegalArgumentException> { repository.update(saved.withData { solution(foreignSolutionId) }) }

        assertSameData(saved, assertNotNull(repository.findById(saved.id)))
    }
}
