package tech.testsys.infra.database.api.persistence.adapter.task

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import tech.testsys.domain.builder.api.verdict
import tech.testsys.domain.builder.api.verdictData
import tech.testsys.domain.builder.api.withData
import tech.testsys.domain.contract.persistence.repository.VerdictRepository
import tech.testsys.domain.model.task.Verdict
import tech.testsys.domain.model.task.VerdictData
import tech.testsys.domain.model.task.VerdictId
import tech.testsys.infra.database.api.persistence.adapter.PersistenceAdapterContractTests
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class VerdictPersistenceAdapterTests : PersistenceAdapterContractTests<VerdictData, VerdictId, Verdict>() {

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

    override fun modified(entity: Verdict): Verdict {
        val newLogsId = fixtures.logs().id
        val newRecordingId = fixtures.recording().id
        return entity.withData {
            score = 42
            logs = newLogsId
            recording = newRecordingId
        }
    }

    override fun detached(entity: Verdict) = verdict {
        id = entity.id.value
        createdAt = entity.createdAt
        data = entity.data
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
    fun `should save a verdict without logs and recording`() {
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

    @Test
    fun `should keep logs and recording when the score is updated`() {
        val saved = repository.save(newData())

        repository.update(saved.withData { score = 42 })

        val found = assertNotNull(repository.findById(saved.id))
        assertEquals(saved.data.logs?.id, found.data.logs?.id)
        assertEquals(saved.data.recording?.id, found.data.recording?.id)
    }

    @Test
    fun `should detach logs and recording if they are cleared on update`() {
        val saved = repository.save(newData())

        repository.update(
            saved.withData {
                logs = null
                recording = null
            },
        )

        val found = assertNotNull(repository.findById(saved.id))
        assertNull(found.data.logs)
        assertNull(found.data.recording)
    }

    @Test
    fun `should keep the task and submission if other ones are passed on update`() {
        val saved = repository.save(newData())
        val otherSubmission = fixtures.submission()

        val updated = repository.update(
            saved.withData {
                task = otherSubmission.data.task.id
                submission = otherSubmission.id
            },
        )

        val found = assertNotNull(repository.findById(saved.id))
        assertEquals(saved.data.task.id, updated.data.task.id)
        assertEquals(saved.data.submission.id, updated.data.submission.id)
        assertEquals(saved.data.task.id, found.data.task.id)
        assertEquals(saved.data.submission.id, found.data.submission.id)
    }
}
