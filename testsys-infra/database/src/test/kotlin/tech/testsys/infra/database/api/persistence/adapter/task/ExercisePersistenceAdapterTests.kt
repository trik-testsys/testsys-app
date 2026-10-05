package tech.testsys.infra.database.api.persistence.adapter.task

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import tech.testsys.domain.builder.api.exercise
import tech.testsys.domain.builder.api.exerciseData
import tech.testsys.domain.builder.api.withData
import tech.testsys.domain.contract.FileBlobStorage
import tech.testsys.domain.contract.StoredBlobRef
import tech.testsys.domain.contract.persistence.repository.ExerciseRepository
import tech.testsys.domain.model.task.Exercise
import tech.testsys.domain.model.task.ExerciseData
import tech.testsys.domain.model.task.ExerciseId
import tech.testsys.domain.model.task.TrikSupportedLanguage
import tech.testsys.domain.model.task.VersionBucket
import tech.testsys.infra.database.DatabaseFixtures.Companion.chose
import tech.testsys.infra.database.api.persistence.adapter.UpdatablePersistenceAdapterContractTests
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.repository.task.ExerciseJpaEntityRepository
import tech.testsys.infra.database.internal.jpa.repository.task.FileDataJpaEntityRepository
import java.time.Instant
import java.util.UUID
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

@OptIn(InternalDatabaseApi::class)
class ExercisePersistenceAdapterTests : UpdatablePersistenceAdapterContractTests<ExerciseData, ExerciseId, Exercise>() {

    @Autowired
    override lateinit var repository: ExerciseRepository

    @Autowired
    private lateinit var fileDataJpaEntityRepository: FileDataJpaEntityRepository

    @Autowired
    private lateinit var jpaEntityRepository: ExerciseJpaEntityRepository

    @Autowired
    private lateinit var fileBlobStorage: FileBlobStorage

    override fun newData() = exerciseData {
        name = fixtures.unique("Exercise")
        description = "Exercise description"
        file(fixtures.unique("exercise") + ".qrs", "exercise".toByteArray())
        language.python()
        versionBucket = VersionBucket(UUID.randomUUID())
    }

    override fun modified(entity: Exercise) = entity.withData {
        name = fixtures.unique("Renamed exercise")
        description = "Updated description"
    }

    override fun detached(entity: Exercise) = exercise {
        id = entity.id.value
        createdAt = entity.createdAt
        data = entity.data
    }

    override fun idOf(value: Long) = ExerciseId(value)

    override fun assertSameData(expected: Exercise, actual: Exercise) {
        assertEquals(expected.data.name, actual.data.name)
        assertEquals(expected.data.description, actual.data.description)
        assertEquals(expected.data.file.uploadedFilename, actual.data.file.uploadedFilename)
        assertContentEquals(expected.data.file.content, actual.data.file.content)
        assertEquals(expected.data.language, actual.data.language)
        assertEquals(expected.data.versionBucket, actual.data.versionBucket)
    }

    @Test
    fun `should keep every language through a round trip`() {
        val languages =
            listOf(TrikSupportedLanguage.Python, TrikSupportedLanguage.JavaScript, TrikSupportedLanguage.VisualLanguage)

        val saved = languages.map { language ->
            repository.save(
                exerciseData {
                    name = fixtures.unique("Exercise")
                    description = "Exercise description"
                    file(fixtures.unique("exercise"), byteArrayOf(1, 2, 3))
                    this.language.chose(language)
                    versionBucket = VersionBucket(UUID.randomUUID())
                },
            )
        }

        assertEquals(languages, saved.map { assertNotNull(repository.findById(it.id)).data.language })
    }

    @Test
    fun `should fail to update an exercise if the file content changed`() {
        val saved = repository.save(newData())

        assertFailsWith<UnsupportedOperationException> {
            repository.update(saved.withData { file(saved.data.file.uploadedFilename, "changed exercise".toByteArray()) })
        }

        assertSameEntity(saved, assertNotNull(repository.findById(saved.id)))
        assertEquals(1, fileDataJpaEntityRepository.count())
    }

    @Test
    fun `should fail to update an exercise if the file was renamed`() {
        val saved = repository.save(newData())

        assertFailsWith<UnsupportedOperationException> {
            repository.update(saved.withData { file("renamed.qrs", saved.data.file.content) })
        }

        assertSameEntity(saved, assertNotNull(repository.findById(saved.id)))
        assertEquals(1, fileDataJpaEntityRepository.count())
    }

    @Test
    fun `should fail to update an exercise if the language changed`() {
        val saved = repository.save(newData())

        assertFailsWith<UnsupportedOperationException> { repository.update(saved.withData { language.javaScript() }) }

        assertSameEntity(saved, assertNotNull(repository.findById(saved.id)))
    }

    @Test
    fun `should keep the file of the previous version when a new version is saved in the same bucket`() {
        val previous = repository.save(newData())

        val next = repository.save(
            exerciseData {
                name = previous.data.name
                description = previous.data.description
                file(fixtures.unique("exercise") + ".qrs", "changed exercise".toByteArray())
                language.visualLanguage()
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

        repository.update(saved.withData { description = "Only the description changed" })

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

    @Test
    fun `should return history including older file versions`() {
        val first = repository.save(newData())
        val second = repository.save(first.withData { name = "Another version" }.data)

        val history = repository.findVersionsByVersionBucket(first.data.versionBucket)

        assertEquals(setOf(first.id, second.id), history.map { it.id }.toSet())
        assertSameData(first, history.single { it.id == first.id })
        assertSameData(second, history.single { it.id == second.id })
    }

    @Test
    fun `should return current version name without changing creation time after rename`() {
        val saved = repository.save(newData())
        setCreatedAt(id = saved.id, createdAt = Instant.EPOCH)
        val loaded = assertNotNull(repository.findById(saved.id))
        repository.update(loaded.withData { name = "Renamed version" })

        val history = repository.findVersionsByVersionBucket(saved.data.versionBucket)

        assertEquals("Renamed version", history.single().data.name)
        assertEquals(Instant.EPOCH, history.single().createdAt)
        assertEquals(saved.id, history.single().id)
    }

    @Test
    fun `should return an empty history for an absent chain`() {
        repository.save(newData())

        val history = repository.findVersionsByVersionBucket(VersionBucket(UUID(0, 99)))

        assertEquals(emptyList(), history)
    }

    @Test
    fun `should report that a missing chain has no versions`() {
        val hasVersions = repository.existsByVersionBucket(VersionBucket(UUID(0, 99)))

        assertFalse(hasVersions)
    }

    @Test
    fun `should return an old version file reference and check existence without reading its missing blob`() {
        val old = repository.save(newData())
        repository.save(old.withData { name = "Another version" }.data)
        val row = jpaEntityRepository.findById(old.id.value).orElseThrow()
        val fileId = row.fileDataId
        val file = fileDataJpaEntityRepository.findById(fileId).orElseThrow()
        val expected = StoredBlobRef(file.storedFileName)
        fileBlobStorage.delete(expected)

        val reference = repository.findFileRef(old.data.versionBucket, old.id)
        val hasVersions = repository.existsByVersionBucket(old.data.versionBucket)

        assertEquals(expected, reference)
        assertTrue(hasVersions)
    }

    @Test
    fun `should reject a file reference for a version outside the requested chain`() {
        val saved = repository.save(newData())

        val reference = repository.findFileRef(VersionBucket(UUID(0, 99)), saved.id)

        assertNull(reference)
    }

    @Test
    fun `should return null for a missing version file reference`() {
        val reference = repository.findFileRef(VersionBucket(UUID(0, 99)), ExerciseId(99))

        assertNull(reference)
    }

    private fun setCreatedAt(id: ExerciseId, createdAt: Instant) {
        val entity = jpaEntityRepository.findById(id.value).orElseThrow()
        entity.createdAt = createdAt
        jpaEntityRepository.saveAndFlush(entity)
    }
}
