package tech.testsys.infra.database.api.persistence.adapter.task

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import tech.testsys.domain.builder.api.testData
import tech.testsys.domain.builder.api.withData
import tech.testsys.domain.contract.persistence.repository.TestRepository
import tech.testsys.domain.model.task.TestData
import tech.testsys.domain.model.task.TestId
import tech.testsys.infra.database.api.persistence.adapter.PersistenceAdapterContractTest
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.repository.task.FileDataJpaEntityRepository
import java.util.UUID
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import tech.testsys.domain.model.task.Test as Polygon

@OptIn(InternalDatabaseApi::class)
class TestPersistenceAdapterTest : PersistenceAdapterContractTest<TestData, TestId, Polygon>() {

    @Autowired
    override lateinit var repository: TestRepository

    @Autowired
    private lateinit var fileDataJpaEntityRepository: FileDataJpaEntityRepository

    override fun newData() = testData {
        name = fixtures.unique("Polygon")
        description = "Polygon description"
        file(fixtures.unique("polygon") + ".xml", "<field/>".toByteArray())
        versionBucket = UUID.randomUUID()
    }

    override fun modified(entity: Polygon) = entity.withData {
        name = fixtures.unique("Renamed polygon")
        description = "Updated description"
        file(fixtures.unique("polygon") + ".xml", "<field><wall/></field>".toByteArray())
    }

    override fun idOf(value: Long) = TestId(value)

    override fun assertSameData(expected: Polygon, actual: Polygon) {
        assertEquals(expected.data.name, actual.data.name)
        assertEquals(expected.data.description, actual.data.description)
        assertEquals(expected.data.file.uploadedFilename, actual.data.file.uploadedFilename)
        assertContentEquals(expected.data.file.content, actual.data.file.content)
        assertEquals(expected.data.versionBucket, actual.data.versionBucket)
    }

    @Test
    fun `update with a changed file stores a new file version in the same bucket`() {
        val saved = repository.save(newData())

        repository.update(saved.withData { file(saved.data.file.uploadedFilename, "changed".toByteArray()) })

        val found = assertNotNull(repository.findById(saved.id))
        assertContentEquals("changed".toByteArray(), found.data.file.content)
        assertEquals(saved.data.versionBucket, found.data.versionBucket)
        assertEquals(2, fileDataJpaEntityRepository.count())
        assertEquals(setOf(saved.data.versionBucket), fileDataJpaEntityRepository.findAll().map { it.versionBucket }.toSet())
    }

    @Test
    fun `update with an unchanged file keeps the stored file`() {
        val saved = repository.save(newData())

        val updated = repository.update(saved.withData { description = "Only the description changed" })

        assertSameEntity(updated, assertNotNull(repository.findById(saved.id)))
        assertEquals(1, fileDataJpaEntityRepository.count())
    }
}
