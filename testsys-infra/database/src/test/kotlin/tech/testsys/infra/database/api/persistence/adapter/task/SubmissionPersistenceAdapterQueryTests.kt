package tech.testsys.infra.database.api.persistence.adapter.task

import jakarta.persistence.EntityManagerFactory
import org.hibernate.SessionFactory
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import tech.testsys.domain.builder.api.judgmentOrderData
import tech.testsys.domain.builder.api.submissionData
import tech.testsys.domain.builder.api.verdictData
import tech.testsys.domain.builder.api.withData
import tech.testsys.domain.contract.persistence.ContestTaskResult
import tech.testsys.domain.contract.persistence.repository.JudgmentOrderRepository
import tech.testsys.domain.contract.persistence.repository.SubmissionRepository
import tech.testsys.domain.contract.persistence.repository.VerdictRepository
import tech.testsys.domain.model.task.Contest
import tech.testsys.domain.model.task.Score
import tech.testsys.domain.model.task.Submission
import tech.testsys.domain.model.task.Task
import tech.testsys.domain.model.user.UserId
import tech.testsys.infra.database.DatabaseIntegrationTests
import java.sql.Timestamp
import java.time.Instant

class SubmissionPersistenceAdapterQueryTests : DatabaseIntegrationTests() {

    @Autowired
    private lateinit var repository: SubmissionRepository

    @Autowired
    private lateinit var verdicts: VerdictRepository

    @Autowired
    private lateinit var judgmentOrders: JudgmentOrderRepository

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @Autowired
    private lateinit var entityManagerFactory: EntityManagerFactory

    @Test
    fun `should count submissions in every grading state and take the best polygon total`() {
        val contest = fixtures.contest()
        val task = fixtures.task()
        val authorId = fixtures.student().id
        gradingSubmission(authorId, task, contest)
        withStatus(gradingSubmission(authorId, task, contest), "IN_PROGRESS")
        withStatus(gradingSubmission(authorId, task, contest), "GRADING_ERROR")
        withStatus(gradingSubmission(authorId, task, contest), "TIMEOUT")
        graded(gradingSubmission(authorId, task, contest), 30, 40)
        graded(gradingSubmission(authorId, task, contest), 50)

        val actual = repository.findContestResults(contest.id, setOf(authorId), setOf(task.id))

        assertEquals(
            listOf(ContestTaskResult(authorId = authorId, taskId = task.id, bestScore = Score(70), submissionCount = 6)),
            actual,
        )
    }

    @Test
    fun `should replace the verdict total with the latest judgment order even if it is lower`() {
        val contest = fixtures.contest()
        val task = fixtures.task()
        val authorId = fixtures.student().id
        val submission = graded(gradingSubmission(authorId, task, contest), 90)
        judgmentOrder(submission, orderScore = 100, issuedAt = "2026-01-01T00:00:00Z")
        judgmentOrder(submission, orderScore = 20, issuedAt = "2026-01-02T00:00:00Z")
        judgmentOrder(submission, orderScore = 60, issuedAt = "2025-12-31T00:00:00Z")

        val actual = repository.findContestResults(contest.id, setOf(authorId), setOf(task.id))

        assertEquals(
            listOf(ContestTaskResult(authorId = authorId, taskId = task.id, bestScore = Score(20), submissionCount = 1)),
            actual,
        )
    }

    @Test
    fun `should take the best result among judgment orders and verdict totals of successful submissions`() {
        val contest = fixtures.contest()
        val task = fixtures.task()
        val authorId = fixtures.student().id
        val judged = graded(gradingSubmission(authorId, task, contest), 90)
        judgmentOrder(judged, orderScore = 10, issuedAt = "2026-01-01T00:00:00Z")
        graded(gradingSubmission(authorId, task, contest), 40)

        val actual = repository.findContestResults(contest.id, setOf(authorId), setOf(task.id))

        assertEquals(
            listOf(ContestTaskResult(authorId = authorId, taskId = task.id, bestScore = Score(40), submissionCount = 2)),
            actual,
        )
    }

    @Test
    fun `should return no best score with a positive count if no submission is graded successfully`() {
        val contest = fixtures.contest()
        val task = fixtures.task()
        val authorId = fixtures.participant().id
        gradingSubmission(authorId, task, contest)
        withStatus(gradingSubmission(authorId, task, contest), "TIMEOUT")

        val actual = repository.findContestResults(contest.id, setOf(authorId), setOf(task.id))

        assertEquals(
            listOf(ContestTaskResult(authorId = authorId, taskId = task.id, bestScore = null, submissionCount = 2)),
            actual,
        )
    }

    @Test
    fun `should ignore judgment orders of submissions without a successful verdict`() {
        val contest = fixtures.contest()
        val task = fixtures.task()
        val authorId = fixtures.student().id
        val submission = withStatus(gradingSubmission(authorId, task, contest), "GRADING_ERROR")
        judgmentOrder(submission, orderScore = 100, issuedAt = "2026-01-01T00:00:00Z")

        val actual = repository.findContestResults(contest.id, setOf(authorId), setOf(task.id))

        assertEquals(
            listOf(ContestTaskResult(authorId = authorId, taskId = task.id, bestScore = null, submissionCount = 1)),
            actual,
        )
    }

    @Test
    fun `should exclude developer solution tests other contests and authors or tasks outside the sets`() {
        val contest = fixtures.contest()
        val task = fixtures.task()
        val author = fixtures.student()
        graded(gradingSubmission(author.id, task, contest), 10)
        fixtures.submission(author = author, task = task)
        graded(gradingSubmission(author.id, task, fixtures.contest()), 90)
        graded(gradingSubmission(fixtures.student().id, task, contest), 90)
        graded(gradingSubmission(author.id, fixtures.task(), contest), 90)

        val actual = repository.findContestResults(contest.id, setOf(author.id), setOf(task.id))

        assertEquals(
            listOf(ContestTaskResult(authorId = author.id, taskId = task.id, bestScore = Score(10), submissionCount = 1)),
            actual,
        )
    }

    @Test
    fun `should order students and participants by author id and then task id omitting pairs without submissions`() {
        val contest = fixtures.contest()
        val firstTask = fixtures.task()
        val secondTask = fixtures.task()
        val idleTask = fixtures.task()
        val studentId = fixtures.student().id
        val participantId = fixtures.participant().id
        val idleStudentId = fixtures.student().id
        gradingSubmission(participantId, secondTask, contest)
        gradingSubmission(participantId, firstTask, contest)
        gradingSubmission(studentId, secondTask, contest)
        gradingSubmission(studentId, firstTask, contest)

        val actual = repository.findContestResults(
            contestId = contest.id,
            authorIds = setOf(participantId, idleStudentId, studentId),
            taskIds = setOf(idleTask.id, secondTask.id, firstTask.id),
        )

        assertTrue(studentId.value < participantId.value && firstTask.id.value < secondTask.id.value)
        assertEquals(
            listOf(
                ContestTaskResult(authorId = studentId, taskId = firstTask.id, bestScore = null, submissionCount = 1),
                ContestTaskResult(authorId = studentId, taskId = secondTask.id, bestScore = null, submissionCount = 1),
                ContestTaskResult(authorId = participantId, taskId = firstTask.id, bestScore = null, submissionCount = 1),
                ContestTaskResult(authorId = participantId, taskId = secondTask.id, bestScore = null, submissionCount = 1),
            ),
            actual,
        )
    }

    @Test
    fun `should return an empty list without querying storage if authors are empty`() {
        val contest = fixtures.contest()
        val task = fixtures.task()
        graded(gradingSubmission(fixtures.student().id, task, contest), 10)

        val (actual, statementCount) = withStatementCount { repository.findContestResults(contest.id, emptySet(), setOf(task.id)) }

        assertEquals(emptyList<ContestTaskResult>(), actual)
        assertEquals(0, statementCount)
    }

    @Test
    fun `should return an empty list without querying storage if tasks are empty`() {
        val contest = fixtures.contest()
        val authorId = fixtures.student().id
        graded(gradingSubmission(authorId, fixtures.task(), contest), 10)

        val (actual, statementCount) = withStatementCount { repository.findContestResults(contest.id, setOf(authorId), emptySet()) }

        assertEquals(emptyList<ContestTaskResult>(), actual)
        assertEquals(0, statementCount)
    }

    @Test
    fun `should read submissions judgment orders and polygon outcomes in three statements`() {
        val contest = fixtures.contest()
        val tasks = listOf(fixtures.task(), fixtures.task())
        val authorIds = listOf(fixtures.student().id, fixtures.participant().id)
        val judged = graded(gradingSubmission(authorIds[0], tasks[0], contest), 10)
        judgmentOrder(judged, orderScore = 70, issuedAt = "2026-01-01T00:00:00Z")
        graded(gradingSubmission(authorIds[0], tasks[1], contest), 20)
        graded(gradingSubmission(authorIds[1], tasks[0], contest), 30, 5)
        graded(gradingSubmission(authorIds[1], tasks[1], contest), 40)

        val (actual, statementCount) = withStatementCount {
            repository.findContestResults(contest.id, authorIds.toSet(), tasks.map { it.id }.toSet())
        }

        assertEquals(listOf(70, 20, 35, 40), actual.map { result -> result.bestScore?.value })
        assertEquals(3, statementCount)
    }

    private fun gradingSubmission(authorId: UserId, task: Task, contest: Contest): Submission {
        val taskId = task.id.value
        val solutionId = fixtures.solution().id.value
        return repository.save(
            submissionData {
                author = authorId
                task(taskId)
                solution(solutionId)
                status.queued()
                kind.grading { this.contest = contest.id }
            },
        )
    }

    private fun withStatus(submission: Submission, state: String): Submission = repository.update(
        submission.withData {
            when (state) {
                "IN_PROGRESS" -> status.inProgress()
                "GRADING_ERROR" -> status.graded { status.error { description = "Grader failed" } }
                "TIMEOUT" -> status.graded { status.timeout() }
                else -> error("Unexpected test state $state")
            }
        },
    )

    private fun graded(submission: Submission, vararg polygonScores: Int): Submission {
        val outcomes = polygonScores.map { polygonScore -> Triple(polygonScore, fixtures.polygon().id.value, fixtures.logs().id.value) }
        val verdict = verdicts.save(
            verdictData {
                task = submission.data.task.id
                this.submission = submission.id
                outcomes.forEach { (polygonScore, polygonId, logsId) ->
                    testVerdict {
                        score = polygonScore
                        test(polygonId)
                        logs(logsId)
                    }
                }
            },
        )
        return repository.update(submission.withData { status.graded { status.success { this.verdict = verdict.id } } })
    }

    private fun judgmentOrder(submission: Submission, orderScore: Int, issuedAt: String) {
        val judgeId = fixtures.judge().id.value
        val submissionId = submission.id.value
        val order = judgmentOrders.save(
            judgmentOrderData {
                judge(judgeId)
                submission(submissionId)
                score = orderScore
                reason = "Manual review"
            },
        )
        jdbcTemplate.update(
            "update ts_judgment_order set created_at = ? where id = ?",
            Timestamp.from(Instant.parse(issuedAt)),
            order.id.value,
        )
    }

    private fun <T> withStatementCount(block: () -> T): Pair<T, Long> {
        val statistics = entityManagerFactory.unwrap(SessionFactory::class.java).statistics
        val wasEnabled = statistics.isStatisticsEnabled
        statistics.isStatisticsEnabled = true
        statistics.clear()
        return try {
            val result = block()
            result to statistics.prepareStatementCount
        } finally {
            statistics.isStatisticsEnabled = wasEnabled
        }
    }
}
