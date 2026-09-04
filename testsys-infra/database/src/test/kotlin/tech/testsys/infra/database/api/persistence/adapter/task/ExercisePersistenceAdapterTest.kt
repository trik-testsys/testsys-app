package tech.testsys.infra.database.api.persistence.adapter.task

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import tech.testsys.domain.builder.api.exerciseData
import tech.testsys.domain.builder.api.withData
import tech.testsys.domain.contract.persistence.repository.ExerciseRepository
import tech.testsys.domain.model.task.Exercise
import tech.testsys.domain.model.task.ExerciseData
import tech.testsys.domain.model.task.ExerciseId
import tech.testsys.domain.model.task.TrikSupportedLanguage
import tech.testsys.infra.database.DatabaseFixtures.Companion.chose
import tech.testsys.infra.database.api.persistence.adapter.PersistenceAdapterContractTest
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.repository.task.FileDataJpaEntityRepository
import java.util.UUID
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@OptIn(InternalDatabaseApi::class)
class ExercisePersistenceAdapterTest : PersistenceAdapterContractTest<ExerciseData, ExerciseId, Exercise>() {

    @Autowired
    override lateinit var repository: ExerciseRepository

    @Autowired
    private lateinit var fileDataJpaEntityRepository: FileDataJpaEntityRepository

    override fun newData() = exerciseData {
        name = fixtures.unique("Exercise")
        description = "Exercise description"
        file(fixtures.unique("exercise") + ".qrs", "exercise".toByteArray())
        language.python()
        versionBucket = UUID.randomUUID()
    }

    override fun modified(entity: Exercise) = entity.withData {
        name = fixtures.unique("Renamed exercise")
        description = "Updated description"
        file(fixtures.unique("exercise") + ".qrs", "changed exercise".toByteArray())
        language.visualLanguage()
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
    fun `every language survives a round trip`() {
        val languages = listOf(TrikSupportedLanguage.Python, TrikSupportedLanguage.JavaScript, TrikSupportedLanguage.VisualLanguage)

        val saved = languages.map { language ->
            repository.save(
                exerciseData {
                    name = fixtures.unique("Exercise")
                    description = "Exercise description"
                    file(fixtures.unique("exercise"), byteArrayOf(1, 2, 3))
                    this.language.chose(language)
                    versionBucket = UUID.randomUUID()
                },
            )
        }

        assertEquals(languages, saved.map { assertNotNull(repository.findById(it.id)).data.language })
    }

    @Test
    fun `update with a changed file stores a new file version`() {
        val saved = repository.save(newData())

        repository.update(saved.withData { file(saved.data.file.uploadedFilename, "changed".toByteArray()) })

        assertContentEquals("changed".toByteArray(), assertNotNull(repository.findById(saved.id)).data.file.content)
        assertEquals(2, fileDataJpaEntityRepository.count())
    }

    @Test
    fun `update with an unchanged file keeps the stored file`() {
        val saved = repository.save(newData())

        repository.update(saved.withData { language.javaScript() })

        assertEquals(1, fileDataJpaEntityRepository.count())
    }
}
