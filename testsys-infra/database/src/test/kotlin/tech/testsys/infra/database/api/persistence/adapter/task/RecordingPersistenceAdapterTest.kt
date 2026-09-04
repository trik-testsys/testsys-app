package tech.testsys.infra.database.api.persistence.adapter.task

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import tech.testsys.domain.builder.api.recordingData
import tech.testsys.domain.builder.api.withData
import tech.testsys.domain.contract.persistence.repository.RecordingRepository
import tech.testsys.domain.model.task.Recording
import tech.testsys.domain.model.task.RecordingData
import tech.testsys.domain.model.task.RecordingId
import tech.testsys.infra.database.api.persistence.adapter.PersistenceAdapterContractTest
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.repository.task.FileDataJpaEntityRepository
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@OptIn(InternalDatabaseApi::class)
class RecordingPersistenceAdapterTest : PersistenceAdapterContractTest<RecordingData, RecordingId, Recording>() {

    @Autowired
    override lateinit var repository: RecordingRepository

    @Autowired
    private lateinit var fileDataJpaEntityRepository: FileDataJpaEntityRepository

    override fun newData() = recordingData { file(fixtures.unique("recording") + ".mp4", byteArrayOf(1, 2, 3)) }

    override fun modified(entity: Recording) = entity.withData { file(fixtures.unique("recording") + ".mp4", byteArrayOf(4, 5, 6)) }

    override fun idOf(value: Long) = RecordingId(value)

    override fun assertSameData(expected: Recording, actual: Recording) {
        assertEquals(expected.data.file.uploadedFilename, actual.data.file.uploadedFilename)
        assertContentEquals(expected.data.file.content, actual.data.file.content)
    }

    @Test
    fun `update with a changed file stores a new file version`() {
        val saved = repository.save(newData())

        repository.update(saved.withData { file(saved.data.file.uploadedFilename, byteArrayOf(9)) })

        assertContentEquals(byteArrayOf(9), assertNotNull(repository.findById(saved.id)).data.file.content)
        assertEquals(2, fileDataJpaEntityRepository.count())
    }
}
