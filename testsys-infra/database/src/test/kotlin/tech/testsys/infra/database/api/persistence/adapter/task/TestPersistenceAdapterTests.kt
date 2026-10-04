package tech.testsys.infra.database.api.persistence.adapter.task

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import tech.testsys.domain.builder.api.test
import tech.testsys.domain.builder.api.testData
import tech.testsys.domain.builder.api.withData
import tech.testsys.domain.contract.persistence.repository.TestRepository
import tech.testsys.domain.model.task.TestData
import tech.testsys.domain.model.task.TestId
import tech.testsys.domain.model.task.VersionBucket
import tech.testsys.infra.database.api.persistence.adapter.UpdatablePersistenceAdapterContractTests
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.repository.task.FileDataJpaEntityRepository
import tech.testsys.infra.database.internal.jpa.repository.task.TestJpaEntityRepository
import java.time.Instant
import java.util.UUID
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import tech.testsys.domain.model.task.Test as Polygon

@OptIn(InternalDatabaseApi::class)
class TestPersistenceAdapterTests : UpdatablePersistenceAdapterContractTests<TestData, TestId, Polygon>() {

    @Autowired
    override lateinit var repository: TestRepository

    @Autowired
    private lateinit var fileDataJpaEntityRepository: FileDataJpaEntityRepository

    @Autowired
    private lateinit var jpaEntityRepository: TestJpaEntityRepository

    override fun newData() = testData {
        name = fixtures.unique("Polygon")
        description = "Polygon description"
        file(fixtures.unique("polygon") + ".xml", "<field/>".toByteArray())
        versionBucket = VersionBucket(UUID.randomUUID())
    }

    override fun modified(entity: Polygon) = entity.withData {
        name = fixtures.unique("Renamed polygon")
        description = "Updated description"
    }

    override fun detached(entity: Polygon) = test {
        id = entity.id.value
        createdAt = entity.createdAt
        data = entity.data
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
    fun `should fail to update a test if the file content changed`() {
        val saved = repository.save(newData())

        assertFailsWith<UnsupportedOperationException> {
            repository.update(saved.withData { file(saved.data.file.uploadedFilename, "<field><wall/></field>".toByteArray()) })
        }

        assertSameEntity(saved, assertNotNull(repository.findById(saved.id)))
        assertEquals(1, fileDataJpaEntityRepository.count())
    }

    @Test
    fun `should fail to update a test if the file was renamed`() {
        val saved = repository.save(newData())

        assertFailsWith<UnsupportedOperationException> {
            repository.update(saved.withData { file("renamed.xml", saved.data.file.content) })
        }

        assertSameEntity(saved, assertNotNull(repository.findById(saved.id)))
        assertEquals(1, fileDataJpaEntityRepository.count())
    }

    @Test
    fun `should keep the file of the previous version when a new version is saved in the same bucket`() {
        val previous = repository.save(newData())

        val next = repository.save(
            testData {
                name = previous.data.name
                description = previous.data.description
                file(fixtures.unique("polygon") + ".xml", "<field><wall/></field>".toByteArray())
                versionBucket = previous.data.versionBucket
            },
        )

        assertSameEntity(previous, assertNotNull(repository.findById(previous.id)))
        assertSameEntity(next, assertNotNull(repository.findById(next.id)))
        assertEquals(previous.data.versionBucket, next.data.versionBucket)
        assertEquals(2, fileDataJpaEntityRepository.count())
    }

    @Test
    fun `should keep the stored file on update with an unchanged file`() {
        val saved = repository.save(newData())

        val updated = repository.update(saved.withData { description = "Only the description changed" })

        assertSameEntity(updated, assertNotNull(repository.findById(saved.id)))
        assertEquals(1, fileDataJpaEntityRepository.count())
    }

    @Test
    fun `should keep the version bucket if another bucket is passed on update`() {
        val saved = repository.save(newData())

        val updated = repository.update(saved.withData { versionBucket = VersionBucket(UUID.randomUUID()) })

        assertEquals(saved.data.versionBucket, updated.data.versionBucket)
        assertEquals(saved.data.versionBucket, assertNotNull(repository.findById(saved.id)).data.versionBucket)
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

    private fun setCreatedAt(id: TestId, createdAt: Instant) {
        val entity = jpaEntityRepository.findById(id.value).orElseThrow()
        entity.createdAt = createdAt
        jpaEntityRepository.saveAndFlush(entity)
    }
}
