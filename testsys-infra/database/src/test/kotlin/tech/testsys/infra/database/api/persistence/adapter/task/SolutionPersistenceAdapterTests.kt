package tech.testsys.infra.database.api.persistence.adapter.task

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import tech.testsys.domain.builder.api.solutionData
import tech.testsys.domain.builder.api.withData
import tech.testsys.domain.contract.persistence.repository.SolutionRepository
import tech.testsys.domain.model.task.Solution
import tech.testsys.domain.model.task.SolutionData
import tech.testsys.domain.model.task.SolutionId
import tech.testsys.domain.model.task.TrikSupportedLanguage
import tech.testsys.infra.database.DatabaseFixtures.Companion.chose
import tech.testsys.infra.database.api.persistence.adapter.PersistenceAdapterContractTests
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.repository.task.FileDataJpaEntityRepository
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

@OptIn(InternalDatabaseApi::class)
class SolutionPersistenceAdapterTests : PersistenceAdapterContractTests<SolutionData, SolutionId, Solution>() {

    @Autowired
    override lateinit var repository: SolutionRepository

    @Autowired
    private lateinit var fileDataJpaEntityRepository: FileDataJpaEntityRepository

    override fun newData() = solutionData {
        file(fixtures.unique("solution") + ".py", "print('solution')".toByteArray())
        language.python()
    }

    override fun idOf(value: Long) = SolutionId(value)

    override fun assertSameData(expected: Solution, actual: Solution) {
        assertEquals(expected.data.file.uploadedFilename, actual.data.file.uploadedFilename)
        assertContentEquals(expected.data.file.content, actual.data.file.content)
        assertEquals(expected.data.language, actual.data.language)
    }

    @Test
    fun `should keep every language through a round trip`() {
        val languages =
            listOf(TrikSupportedLanguage.Python, TrikSupportedLanguage.JavaScript, TrikSupportedLanguage.VisualLanguage)

        val saved = languages.map { language ->
            repository.save(
                solutionData {
                    file(fixtures.unique("solution"), byteArrayOf(1, 2, 3))
                    this.language.chose(language)
                },
            )
        }

        assertEquals(languages, saved.map { assertNotNull(repository.findById(it.id)).data.language })
    }

    @Test
    fun `should fail to update a solution and keep the stored file and language`() {
        val saved = repository.save(newData())
        val modified = saved.withData {
            file(fixtures.unique("solution") + ".js", "console.log('solution')".toByteArray())
            language.javaScript()
        }

        assertFailsWith<UnsupportedOperationException> { repository.update(modified) }

        assertSameData(saved, assertNotNull(repository.findById(saved.id)))
        assertEquals(1, fileDataJpaEntityRepository.count())
    }
}
