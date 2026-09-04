package tech.testsys.infra.database.api.persistence.adapter.task

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import tech.testsys.domain.builder.api.verdictData
import tech.testsys.domain.builder.api.withData
import tech.testsys.domain.contract.persistence.repository.VerdictRepository
import tech.testsys.domain.model.task.Verdict
import tech.testsys.domain.model.task.VerdictData
import tech.testsys.domain.model.task.VerdictId
import tech.testsys.infra.database.api.persistence.adapter.PersistenceAdapterContractTest
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class VerdictPersistenceAdapterTest : PersistenceAdapterContractTest<VerdictData, VerdictId, Verdict>() {

    @Autowired
    override lateinit var repository: VerdictRepository

    override fun newData(): VerdictData {
        val submission = fixtures.submission()
        val submissionId = submission.id.value
        val taskId = submission.data.task.id.value
        val logsId = fixtures.logs().id.value
        val recordingId = fixtures.recording().id.value
        return verdictData {
            score = 100
            task(taskId)
            submission(submissionId)
            logs(logsId)
            recording(recordingId)
        }
    }

    override fun modified(entity: Verdict) = entity.withData {
        score = 42
        logs = null
        recording = null
    }

    override fun idOf(value: Long) = VerdictId(value)

    override fun assertSameData(expected: Verdict, actual: Verdict) {
        assertEquals(expected.data.score, actual.data.score)
        assertEquals(expected.data.task.id, actual.data.task.id)
        assertEquals(expected.data.submission.id, actual.data.submission.id)
        assertEquals(expected.data.logs?.id, actual.data.logs?.id)
        assertEquals(expected.data.recording?.id, actual.data.recording?.id)
    }

    @Test
    fun `logs and recording are optional`() {
        val submission = fixtures.submission()
        val submissionId = submission.id.value
        val taskId = submission.data.task.id.value

        val saved = repository.save(
            verdictData {
                score = 0
                task(taskId)
                submission(submissionId)
            },
        )

        val found = assertNotNull(repository.findById(saved.id))
        assertNull(found.data.logs)
        assertNull(found.data.recording)
        assertEquals(0, found.data.score.value)
    }
}
