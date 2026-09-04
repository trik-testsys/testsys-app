package tech.testsys.infra.database.api.persistence.adapter.task

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import tech.testsys.domain.builder.api.logsData
import tech.testsys.domain.builder.api.withData
import tech.testsys.domain.contract.persistence.repository.LogsRepository
import tech.testsys.domain.model.task.Logs
import tech.testsys.domain.model.task.LogsData
import tech.testsys.domain.model.task.LogsId
import tech.testsys.infra.database.api.persistence.adapter.PersistenceAdapterContractTest
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.repository.task.FileDataJpaEntityRepository
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@OptIn(InternalDatabaseApi::class)
class LogsPersistenceAdapterTest : PersistenceAdapterContractTest<LogsData, LogsId, Logs>() {

    @Autowired
    override lateinit var repository: LogsRepository

    @Autowired
    private lateinit var fileDataJpaEntityRepository: FileDataJpaEntityRepository

    override fun newData() = logsData { file(fixtures.unique("logs") + ".txt", "grader output".toByteArray()) }

    override fun modified(entity: Logs) = entity.withData { file(fixtures.unique("logs") + ".txt", "more grader output".toByteArray()) }

    override fun idOf(value: Long) = LogsId(value)

    override fun assertSameData(expected: Logs, actual: Logs) {
        assertEquals(expected.data.file.uploadedFilename, actual.data.file.uploadedFilename)
        assertContentEquals(expected.data.file.content, actual.data.file.content)
    }

    @Test
    fun `update with an unchanged file keeps the stored file`() {
        val saved = repository.save(newData())

        val updated = repository.update(saved.withData { file(saved.data.file.uploadedFilename, saved.data.file.content) })

        assertSameEntity(updated, assertNotNull(repository.findById(saved.id)))
        assertEquals(1, fileDataJpaEntityRepository.count())
    }
}
