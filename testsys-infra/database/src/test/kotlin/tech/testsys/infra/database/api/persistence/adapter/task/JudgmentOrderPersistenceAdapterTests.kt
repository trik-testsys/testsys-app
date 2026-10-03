package tech.testsys.infra.database.api.persistence.adapter.task

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import tech.testsys.domain.builder.api.judgmentOrder
import tech.testsys.domain.builder.api.judgmentOrderData
import tech.testsys.domain.builder.api.withData
import tech.testsys.domain.contract.persistence.repository.JudgmentOrderRepository
import tech.testsys.domain.model.task.JudgmentOrder
import tech.testsys.domain.model.task.JudgmentOrderData
import tech.testsys.domain.model.task.JudgmentOrderId
import tech.testsys.infra.database.api.persistence.adapter.UpdatablePersistenceAdapterContractTests
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class JudgmentOrderPersistenceAdapterTests : UpdatablePersistenceAdapterContractTests<JudgmentOrderData, JudgmentOrderId, JudgmentOrder>() {

    @Autowired
    override lateinit var repository: JudgmentOrderRepository

    override fun newData(): JudgmentOrderData {
        val judgeId = fixtures.judge().id.value
        val submissionId = fixtures.submission().id.value
        return judgmentOrderData {
            judge(judgeId)
            submission(submissionId)
            score = 50
            reason = "Manual review"
        }
    }

    override fun modified(entity: JudgmentOrder) = entity.withData {
        score = 75
        reason = "Clarified after an appeal"
    }

    override fun detached(entity: JudgmentOrder) = judgmentOrder {
        id = entity.id.value
        createdAt = entity.createdAt
        data = entity.data
    }

    override fun idOf(value: Long) = JudgmentOrderId(value)

    override fun assertSameData(expected: JudgmentOrder, actual: JudgmentOrder) {
        assertEquals(expected.data.judge.id, actual.data.judge.id)
        assertEquals(expected.data.submission.id, actual.data.submission.id)
        assertEquals(expected.data.score, actual.data.score)
        assertEquals(expected.data.reason, actual.data.reason)
    }

    @Test
    fun `should keep the judge and submission if other ones are passed on update`() {
        val saved = repository.save(newData())
        val otherJudgeId = fixtures.judge().id.value
        val otherSubmissionId = fixtures.submission().id.value

        val updated = repository.update(
            saved.withData {
                judge(otherJudgeId)
                submission(otherSubmissionId)
            },
        )

        val found = assertNotNull(repository.findById(saved.id))
        assertEquals(saved.data.judge.id, updated.data.judge.id)
        assertEquals(saved.data.submission.id, updated.data.submission.id)
        assertEquals(saved.data.judge.id, found.data.judge.id)
        assertEquals(saved.data.submission.id, found.data.submission.id)
    }
}
