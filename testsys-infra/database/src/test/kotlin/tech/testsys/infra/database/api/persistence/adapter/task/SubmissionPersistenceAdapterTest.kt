package tech.testsys.infra.database.api.persistence.adapter.task

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import tech.testsys.domain.builder.api.submissionData
import tech.testsys.domain.builder.api.withData
import tech.testsys.domain.contract.persistence.repository.SubmissionRepository
import tech.testsys.domain.model.task.GradingResult
import tech.testsys.domain.model.task.Submission
import tech.testsys.domain.model.task.SubmissionData
import tech.testsys.domain.model.task.SubmissionId
import tech.testsys.domain.model.task.SubmissionKind
import tech.testsys.domain.model.task.SubmissionStatus
import tech.testsys.infra.database.api.persistence.adapter.PersistenceAdapterContractTest
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull

class SubmissionPersistenceAdapterTest : PersistenceAdapterContractTest<SubmissionData, SubmissionId, Submission>() {

    @Autowired
    override lateinit var repository: SubmissionRepository

    override fun newData(): SubmissionData {
        val author = fixtures.developer()
        val authorId = author.id
        val taskId = fixtures.task(author).id.value
        val solutionId = fixtures.solution().id.value
        return submissionData {
            this.author = authorId
            solution(solutionId)
            task(taskId)
            status.queued()
            kind.developerSolutionTest()
        }
    }

    override fun modified(entity: Submission) = entity.withData { status.inProgress() }

    override fun idOf(value: Long) = SubmissionId(value)

    override fun assertSameData(expected: Submission, actual: Submission) {
        assertEquals(expected.data.author.id, actual.data.author.id)
        assertEquals(expected.data.solution.id, actual.data.solution.id)
        assertEquals(expected.data.task.id, actual.data.task.id)
        assertSameStatus(expected.data.status, actual.data.status)
        assertSameKind(expected.data.kind, actual.data.kind)
        assertEquals(expected.data.judgmentOrders.ids.toSet(), actual.data.judgmentOrders.ids.toSet())
    }

    private fun assertSameStatus(expected: SubmissionStatus, actual: SubmissionStatus) {
        when (expected) {
            SubmissionStatus.Queued, SubmissionStatus.InProgress -> assertEquals(expected, actual)
            is SubmissionStatus.Graded -> {
                val actualGrade = assertIs<SubmissionStatus.Graded>(actual).grade
                when (val grade = expected.grade) {
                    is GradingResult.Success -> assertEquals(grade.verdict.id, assertIs<GradingResult.Success>(actualGrade).verdict.id)
                    is GradingResult.GradingError, GradingResult.Timeout -> assertEquals(grade, actualGrade)
                }
            }
        }
    }

    private fun assertSameKind(expected: SubmissionKind, actual: SubmissionKind) {
        when (expected) {
            SubmissionKind.DeveloperSolutionTest -> assertEquals(expected, actual)
            is SubmissionKind.Grading -> assertEquals(expected.contest.id, assertIs<SubmissionKind.Grading>(actual).contest.id)
        }
    }

    private fun gradingSubmissionData(contestId: Long): SubmissionData {
        val author = fixtures.student()
        val authorId = author.id
        val taskId = fixtures.task().id.value
        val solutionId = fixtures.solution().id.value
        return submissionData {
            this.author = authorId
            solution(solutionId)
            task(taskId)
            status.queued()
            kind.grading { contest(contestId) }
        }
    }

    @Test
    fun `a contest submission is queued, picked up and graded with a verdict`() {
        val contestId = fixtures.contest().id.value
        val queued = repository.save(gradingSubmissionData(contestId))

        val inProgress = repository.update(queued.withData { status.inProgress() })
        val verdictId = fixtures.verdict(inProgress).id.value
        val graded = repository.update(inProgress.withData { status.graded { status.success { verdict(verdictId) } } })

        val found = assertNotNull(repository.findById(queued.id))
        assertSameEntity(graded, found)
        assertEquals(SubmissionStatus.InProgress, inProgress.data.status)
        assertEquals(verdictId, assertIs<GradingResult.Success>(assertIs<SubmissionStatus.Graded>(found.data.status).grade).verdict.id.value)
        assertEquals(contestId, assertIs<SubmissionKind.Grading>(found.data.kind).contest.id.value)
    }

    @Test
    fun `a grading error survives a round trip`() {
        val data = newData()

        val saved = repository.save(data)
        val failed = repository.update(saved.withData { status.graded { status.error { description = "Grader crashed" } } })

        val found = assertNotNull(repository.findById(saved.id))
        assertSameEntity(failed, found)
        assertEquals(GradingResult.GradingError("Grader crashed"), assertIs<SubmissionStatus.Graded>(found.data.status).grade)
    }

    @Test
    fun `a grading timeout survives a round trip`() {
        val saved = repository.save(newData())

        val timedOut = repository.update(saved.withData { status.graded { status.timeout() } })

        val found = assertNotNull(repository.findById(saved.id))
        assertSameEntity(timedOut, found)
        assertEquals(GradingResult.Timeout, assertIs<SubmissionStatus.Graded>(found.data.status).grade)
    }

    @Test
    fun `judgment orders are projected from the orders issued for the verdicts of the submission`() {
        val saved = repository.save(newData())
        val verdict = fixtures.verdict(saved)
        val order = fixtures.judgmentOrder(verdict = verdict)
        fixtures.judgmentOrder()

        val found = assertNotNull(repository.findById(saved.id))

        assertEquals(listOf(order.id), found.data.judgmentOrders.ids)
    }

    @Test
    fun `judgment orders given on save are ignored`() {
        val author = fixtures.developer()
        val authorId = author.id
        val taskId = fixtures.task(author).id.value
        val solutionId = fixtures.solution().id.value

        val saved = repository.save(
            submissionData {
                this.author = authorId
                solution(solutionId)
                task(taskId)
                status.queued()
                kind.developerSolutionTest()
                judgmentOrders(listOf(UNKNOWN_ID))
            },
        )

        assertEquals(emptyList(), saved.data.judgmentOrders.ids)
        assertEquals(emptyList(), assertNotNull(repository.findById(saved.id)).data.judgmentOrders.ids)
    }
}
