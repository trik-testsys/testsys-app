package tech.testsys.infra.database.api.persistence.adapter.task

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import tech.testsys.domain.builder.api.logsData
import tech.testsys.domain.builder.api.withData
import tech.testsys.domain.contract.FileBlobStorage
import tech.testsys.domain.contract.FileContentReader
import tech.testsys.domain.contract.StoredBlobRef
import tech.testsys.domain.contract.persistence.repository.LogsRepository
import tech.testsys.domain.model.task.Logs
import tech.testsys.domain.model.task.LogsData
import tech.testsys.domain.model.task.LogsId
import tech.testsys.infra.database.api.persistence.adapter.PersistenceAdapterContractTests
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.repository.task.FileDataJpaEntityRepository
import java.nio.file.Path
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

@OptIn(InternalDatabaseApi::class)
class LogsPersistenceAdapterTests : PersistenceAdapterContractTests<LogsData, LogsId, Logs>() {

    @Autowired
    private lateinit var fileContentReader: FileContentReader

    @Autowired
    override lateinit var repository: LogsRepository

    @Autowired
    private lateinit var fileDataJpaEntityRepository: FileDataJpaEntityRepository

    @Autowired
    private lateinit var fileBlobStorage: FileBlobStorage

    override fun newData() = logsData { file(fixtures.unique("logs") + ".txt", "grader output".toByteArray()) }

    override fun idOf(value: Long) = LogsId(value)

    override fun assertSameData(expected: Logs, actual: Logs) {
        assertEquals(expected.data.file.uploadedFilename, actual.data.file.uploadedFilename)
        assertContentEquals(fileContentReader.read(expected.data.file), fileContentReader.read(actual.data.file))
    }

    @Test
    fun `should fail to update logs and keep the stored file`() {
        val saved = repository.save(newData())
        val modified = saved.withData { file(fixtures.unique("logs") + ".txt", "more grader output".toByteArray()) }

        assertFailsWith<UnsupportedOperationException> { repository.update(modified) }

        assertSameData(saved, assertNotNull(repository.findById(saved.id)))
        assertEquals(1, fileDataJpaEntityRepository.count())
    }

    @Test
    fun `should store the logs file under the logs path`() {
        val saved = repository.save(newData())

        val ref = StoredBlobRef(fileDataJpaEntityRepository.findAll().single().storedFileName)
        assertContentEquals(fileContentReader.read(saved.data.file), fileBlobStorage.load(ref, Path.of(LOGS_PATH)))
    }
}
