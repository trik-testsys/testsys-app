package tech.testsys.infra.database.api.persistence.adapter.task

import org.springframework.beans.factory.annotation.Autowired
import tech.testsys.domain.builder.api.judgmentOrderData
import tech.testsys.domain.builder.api.withData
import tech.testsys.domain.contract.persistence.repository.JudgmentOrderRepository
import tech.testsys.domain.model.task.JudgmentOrder
import tech.testsys.domain.model.task.JudgmentOrderData
import tech.testsys.domain.model.task.JudgmentOrderId
import tech.testsys.infra.database.api.persistence.adapter.PersistenceAdapterContractTest
import kotlin.test.assertEquals

class JudgmentOrderPersistenceAdapterTest : PersistenceAdapterContractTest<JudgmentOrderData, JudgmentOrderId, JudgmentOrder>() {

    @Autowired
    override lateinit var repository: JudgmentOrderRepository

    override fun newData(): JudgmentOrderData {
        val judgeId = fixtures.judge().id.value
        val verdictId = fixtures.verdict().id.value
        return judgmentOrderData {
            judge(judgeId)
            verdict(verdictId)
            reason = "Manual review"
        }
    }

    override fun modified(entity: JudgmentOrder): JudgmentOrder {
        val newJudgeId = fixtures.judge().id.value
        return entity.withData {
            judge(newJudgeId)
            reason = "Reassigned to another judge"
        }
    }

    override fun idOf(value: Long) = JudgmentOrderId(value)

    override fun assertSameData(expected: JudgmentOrder, actual: JudgmentOrder) {
        assertEquals(expected.data.judge.id, actual.data.judge.id)
        assertEquals(expected.data.verdict.id, actual.data.verdict.id)
        assertEquals(expected.data.reason, actual.data.reason)
    }
}
