package tech.testsys.infra.database.api.persistence.adapter.task

import org.springframework.beans.factory.annotation.Autowired
import tech.testsys.domain.builder.api.developerSolutionData
import tech.testsys.domain.builder.api.withData
import tech.testsys.domain.contract.persistence.repository.DeveloperSolutionRepository
import tech.testsys.domain.model.task.DeveloperSolution
import tech.testsys.domain.model.task.DeveloperSolutionData
import tech.testsys.domain.model.task.DeveloperSolutionId
import tech.testsys.infra.database.api.persistence.adapter.PersistenceAdapterContractTest
import java.util.UUID
import kotlin.test.assertEquals

class DeveloperSolutionPersistenceAdapterTest :
    PersistenceAdapterContractTest<DeveloperSolutionData, DeveloperSolutionId, DeveloperSolution>() {

    @Autowired
    override lateinit var repository: DeveloperSolutionRepository

    override fun newData(): DeveloperSolutionData {
        val solutionId = fixtures.solution().id.value
        return developerSolutionData {
            name = fixtures.unique("Developer solution")
            description = "Developer solution description"
            solution(solutionId)
            expectedScore(100)
            versionBucket = UUID.randomUUID()
        }
    }

    override fun modified(entity: DeveloperSolution): DeveloperSolution {
        val newSolutionId = fixtures.solution().id.value
        return entity.withData {
            name = fixtures.unique("Renamed developer solution")
            description = "Updated description"
            solution(newSolutionId)
            expectedScore(50)
        }
    }

    override fun idOf(value: Long) = DeveloperSolutionId(value)

    override fun assertSameData(expected: DeveloperSolution, actual: DeveloperSolution) {
        assertEquals(expected.data.name, actual.data.name)
        assertEquals(expected.data.description, actual.data.description)
        assertEquals(expected.data.solution.id, actual.data.solution.id)
        assertEquals(expected.data.expectedScore, actual.data.expectedScore)
        assertEquals(expected.data.versionBucket, actual.data.versionBucket)
    }
}
