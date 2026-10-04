package tech.testsys.infra.database.api.persistence.adapter.task

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import tech.testsys.domain.builder.api.exercise
import tech.testsys.domain.builder.api.exerciseData
import tech.testsys.domain.builder.api.withData
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

    private fun setCreatedAt(id: ExerciseId, createdAt: Instant) {
        val entity = jpaEntityRepository.findById(id.value).orElseThrow()
        entity.createdAt = createdAt
        jpaEntityRepository.saveAndFlush(entity)
    }
}
