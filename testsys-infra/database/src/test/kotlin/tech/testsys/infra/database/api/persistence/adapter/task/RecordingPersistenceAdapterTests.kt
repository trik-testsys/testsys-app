package tech.testsys.infra.database.api.persistence.adapter.task

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import tech.testsys.domain.builder.api.recordingData
import tech.testsys.domain.builder.api.withData
import tech.testsys.domain.contract.persistence.repository.RecordingRepository
import tech.testsys.domain.model.task.Recording
import tech.testsys.domain.model.task.RecordingData
import tech.testsys.domain.model.task.RecordingId
import tech.testsys.infra.database.api.persistence.adapter.PersistenceAdapterContractTests
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.repository.task.FileDataJpaEntityRepository
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

@OptIn(InternalDatabaseApi::class)
class RecordingPersistenceAdapterTests : PersistenceAdapterContractTests<RecordingData, RecordingId, Recording>() {

    @Autowired
    override lateinit var repository: RecordingRepository

    @Autowired
    private lateinit var fileDataJpaEntityRepository: FileDataJpaEntityRepository

    override fun newData() = recordingData { file(fixtures.unique("recording") + ".mp4", byteArrayOf(1, 2, 3)) }

    override fun idOf(value: Long) = RecordingId(value)

    override fun assertSameData(expected: Recording, actual: Recording) {
        assertEquals(expected.data.file.uploadedFilename, actual.data.file.uploadedFilename)
        assertContentEquals(expected.data.file.content, actual.data.file.content)
    }

    @Test
    fun `should fail to update a recording and keep the stored file`() {
        val saved = repository.save(newData())
        val modified = saved.withData { file(fixtures.unique("recording") + ".mp4", byteArrayOf(4, 5, 6)) }

        assertFailsWith<UnsupportedOperationException> { repository.update(modified) }

        assertSameData(saved, assertNotNull(repository.findById(saved.id)))
        assertEquals(1, fileDataJpaEntityRepository.count())
    }
}
