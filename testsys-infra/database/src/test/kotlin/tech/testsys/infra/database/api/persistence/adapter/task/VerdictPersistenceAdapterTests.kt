package tech.testsys.infra.database.api.persistence.adapter.task

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import tech.testsys.domain.builder.api.verdict
import tech.testsys.domain.builder.api.verdictData
import tech.testsys.domain.builder.api.withData
import tech.testsys.domain.contract.persistence.repository.VerdictRepository
import tech.testsys.domain.model.task.TestVerdict
import tech.testsys.domain.model.task.Verdict
import tech.testsys.domain.model.task.VerdictData
import tech.testsys.domain.model.task.VerdictId
import tech.testsys.infra.database.api.persistence.adapter.UpdatablePersistenceAdapterContractTests
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class VerdictPersistenceAdapterTests : UpdatablePersistenceAdapterContractTests<VerdictData, VerdictId, Verdict>() {

    @Autowired
    override lateinit var repository: VerdictRepository

    override fun newData(): VerdictData {
        val submission = fixtures.submission()
        val submissionId = submission.id.value
        val taskId = submission.data.task.id.value
        val firstPolygonId = fixtures.polygon().id.value
        val secondPolygonId = fixtures.polygon().id.value
        val firstLogsId = fixtures.logs().id.value
        val secondLogsId = fixtures.logs().id.value
        val recordingId = fixtures.recording().id.value
        return verdictData {
            task(taskId)
            submission(submissionId)
            testVerdict {
                score = 100
                test(firstPolygonId)
                logs(firstLogsId)
                recording(recordingId)
            }
            testVerdict {
                score = 0
                test(secondPolygonId)
                logs(secondLogsId)
            }
        }
    }

    override val updatable = false

    override fun modified(entity: Verdict): Verdict {
        val polygonId = fixtures.polygon().id.value
        val logsId = fixtures.logs().id.value
        return entity.withData {
            testVerdicts.clear()
            testVerdict {
                score = 42
                test(polygonId)
                logs(logsId)
            }
        }
    }

    override fun detached(entity: Verdict) = verdict {
        id = entity.id.value
        createdAt = entity.createdAt
        data = entity.data
    }

    override fun idOf(value: Long) = VerdictId(value)

    override fun assertSameData(expected: Verdict, actual: Verdict) {
        assertEquals(expected.data.task.id, actual.data.task.id)
        assertEquals(expected.data.submission.id, actual.data.submission.id)
        assertEquals(expected.data.testVerdicts.size, actual.data.testVerdicts.size)
        expected.data.testVerdicts.sortedBy { it.test.id.value }
            .zip(actual.data.testVerdicts.sortedBy { it.test.id.value })
            .forEach { (expectedTestVerdict, actualTestVerdict) -> assertSameTestVerdict(expectedTestVerdict, actualTestVerdict) }
    }

    private fun assertSameTestVerdict(expected: TestVerdict, actual: TestVerdict) {
        assertEquals(expected.score, actual.score)
        assertEquals(expected.test.id, actual.test.id)
        assertEquals(expected.logs.id, actual.logs.id)
        assertEquals(expected.recording?.id, actual.recording?.id)
    }

    @Test
    fun `should reject a verdict without any test outcome before saving`() {
        val submission = fixtures.submission()
        val submissionId = submission.id.value
        val taskId = submission.data.task.id.value

        assertFailsWith<IllegalArgumentException> {
            repository.save(
                verdictData {
                    task(taskId)
                    submission(submissionId)
                },
            )
        }
    }

    @Test
    fun `should save a test outcome without recording`() {
        val submission = fixtures.submission()
        val submissionId = submission.id.value
        val taskId = submission.data.task.id.value
        val polygonId = fixtures.polygon().id.value
        val logsId = fixtures.logs().id.value

        val saved = repository.save(
            verdictData {
                task(taskId)
                submission(submissionId)
                testVerdict {
                    score = 0
                    test(polygonId)
                    logs(logsId)
                }
            },
        )

        val found = assertNotNull(repository.findById(saved.id))
        val testVerdict = found.data.testVerdicts.single()
        assertEquals(0, testVerdict.score.value)
        assertEquals(logsId, testVerdict.logs.id.value)
        assertNull(testVerdict.recording)
    }

    @Test
    fun `should fail to update a verdict and keep the stored test outcomes`() {
        val saved = repository.save(newData())

        assertFailsWith<UnsupportedOperationException> { repository.update(modified(saved)) }

        assertSameData(saved, assertNotNull(repository.findById(saved.id)))
    }
}
