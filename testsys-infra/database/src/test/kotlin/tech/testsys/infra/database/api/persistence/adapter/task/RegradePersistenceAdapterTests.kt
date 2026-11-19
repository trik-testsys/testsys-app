package tech.testsys.infra.database.api.persistence.adapter.task

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.transaction.support.TransactionOperations
import tech.testsys.domain.builder.api.regradeData
import tech.testsys.domain.builder.api.withData
import tech.testsys.domain.contract.persistence.repository.RegradeRepository
import tech.testsys.domain.contract.persistence.repository.SubmissionRepository
import tech.testsys.domain.contract.persistence.repository.TaskRepository
import tech.testsys.domain.model.task.Regrade
import tech.testsys.domain.model.task.RegradeData
import tech.testsys.domain.model.task.RegradeId
import tech.testsys.infra.database.api.persistence.adapter.PersistenceAdapterContractTests
import java.sql.Timestamp
import java.time.Instant
import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

class RegradePersistenceAdapterTests : PersistenceAdapterContractTests<RegradeData, RegradeId, Regrade>() {

    @Autowired
    override lateinit var repository: RegradeRepository

    @Autowired
    private lateinit var jdbc: JdbcTemplate

    @Autowired
    private lateinit var submissions: SubmissionRepository

    @Autowired
    private lateinit var taskRepository: TaskRepository

    @Autowired
    private lateinit var transactions: TransactionOperations

    override fun newData(): RegradeData {
        val task = fixtures.task()
        val taskId = task.id.value
        val polygonIds = listOf(fixtures.polygon().id.value, fixtures.polygon().id.value)
        val submissionIds = List(2) { fixtures.gradingSubmission(task = task).id.value }
        return regradeData {
            task(taskId)
            tests(polygonIds)
            submissions(submissionIds)
        }
    }

    override fun idOf(value: Long) = RegradeId(value)

    override fun assertSameData(expected: Regrade, actual: Regrade) {
        assertEquals(expected.data.task.id, actual.data.task.id)
        assertEquals(expected.data.tests.ids.toSet(), actual.data.tests.ids.toSet())
        assertEquals(expected.data.submissions.ids.toSet(), actual.data.submissions.ids.toSet())
        assertEquals(expected.data.stoppedAt, actual.data.stoppedAt)
    }

    @Test
    fun `should fail to update a regrade and keep the stored submissions`() {
        val saved = repository.save(newData())
        val modified = saved.withData { submissions = mutableListOf() }

        assertFailsWith<UnsupportedOperationException> { repository.update(modified) }

        assertSameData(saved, assertNotNull(repository.findById(saved.id)))
    }

    @Test
    fun `should increment the task version when a regrade is saved`() {
        val data = newData()
        val task = taskRepository.load(data.task)

        repository.save(data)

        assertEquals(assertNotNull(task.version).value + 1, assertNotNull(taskRepository.findById(task.id)?.version).value)
    }

    @Test
    fun `should increment the task version when a regrade is removed`() {
        val regrade = repository.save(newData())
        val task = taskRepository.load(regrade.data.task)

        repository.removeById(regrade.id)

        assertEquals(null, repository.findById(regrade.id))
        assertEquals(assertNotNull(task.version).value + 1, assertNotNull(taskRepository.findById(task.id)?.version).value)
    }

    @Test
    fun `should find regrades by ids with the same statement count for one and twenty ids`() {
        val data = newData()
        val ids = List(20) { repository.save(data).id }

        val (one, oneIdStatements) = withStatementCount { repository.findByIds(ids.take(1)) }
        val (twenty, twentyIdsStatements) = withStatementCount { repository.findByIds(ids) }

        assertEquals(ids.take(1), one.map { regrade -> regrade.id })
        assertEquals(List(20) { data.submissions.ids.toSet() }, twenty.map { regrade -> regrade.data.submissions.ids.toSet() })
        assertEquals(List(20) { data.tests.ids.toSet() }, twenty.map { regrade -> regrade.data.tests.ids.toSet() })
        assertEquals(oneIdStatements, twentyIdsStatements)
    }

    @Nested
    inner class FindByTaskIdTests {

        @Test
        fun `should find the regrades of a task in creation order without regrades of another task`() {
            val task = fixtures.task()
            val first = fixtures.regrade(task = task)
            val second = fixtures.regrade(task = task)
            fixtures.regrade()

            val found = repository.findByTaskId(task.id)

            assertEquals(listOf(first.id, second.id), found.map { regrade -> regrade.id })
        }

        @Test
        fun `should find regrades of a task with the same statement count for one and twenty regrades`() {
            val oneRegradeTask = fixtures.task()
            val twentyRegradesTask = fixtures.task()
            fixtures.regrade(task = oneRegradeTask)
            repeat(20) { fixtures.regrade(task = twentyRegradesTask) }

            val (one, oneRegradeStatements) = withStatementCount { repository.findByTaskId(oneRegradeTask.id) }
            val (twenty, twentyRegradesStatements) = withStatementCount { repository.findByTaskId(twentyRegradesTask.id) }

            assertEquals(1, one.size)
            assertEquals(20, twenty.size)
            assertEquals(oneRegradeStatements, twentyRegradesStatements)
        }
    }

    @Nested
    inner class CountGradedTests {

        @Test
        fun `should count a submission graded after the regrade once even with two verdicts`() {
            val task = fixtures.task()
            val polygons = listOf(fixtures.polygon())
            val submission = fixtures.gradingSubmission(task = task)
            val regraded = listOf(submission, fixtures.gradingSubmission(task = task))
            val regrade = fixtures.regrade(task = task, polygons = polygons, regraded = regraded)
            fixtures.verdict(submission, polygons)
            fixtures.verdict(submission, polygons)

            val graded = repository.countGraded(setOf(regrade.id))

            assertEquals(mapOf(regrade.id to 1), graded)
        }

        @Test
        fun `should not count a verdict created before the regrade`() {
            val task = fixtures.task()
            val polygons = listOf(fixtures.polygon())
            val submission = fixtures.gradingSubmission(task = task)
            fixtures.verdict(submission, polygons)
            val regrade = fixtures.regrade(task = task, polygons = polygons, regraded = listOf(submission))

            val graded = repository.countGraded(setOf(regrade.id))

            assertEquals(mapOf(regrade.id to 0), graded)
        }

        @Test
        fun `should count a verdict whose test verdicts all ran on the regrade polygons`() {
            val task = fixtures.task()
            val polygons = listOf(fixtures.polygon(), fixtures.polygon())
            val submission = fixtures.gradingSubmission(task = task)
            val regrade = fixtures.regrade(task = task, polygons = polygons, regraded = listOf(submission))
            fixtures.verdict(submission, polygons)

            val graded = repository.countGraded(setOf(regrade.id))

            assertEquals(mapOf(regrade.id to 1), graded)
        }

        @Test
        fun `should not count a verdict with a test verdict on a polygon outside the regrade`() {
            val task = fixtures.task()
            val polygon = fixtures.polygon()
            val submission = fixtures.gradingSubmission(task = task)
            val regrade = fixtures.regrade(task = task, polygons = listOf(polygon), regraded = listOf(submission))
            fixtures.verdict(submission, listOf(polygon, fixtures.polygon()))

            val graded = repository.countGraded(setOf(regrade.id))

            assertEquals(mapOf(regrade.id to 0), graded)
        }

        @Test
        fun `should not count a verdict without a result on one of the regrade polygons`() {
            val task = fixtures.task()
            val polygon = fixtures.polygon()
            val polygons = listOf(polygon, fixtures.polygon())
            val submission = fixtures.gradingSubmission(task = task)
            val regrade = fixtures.regrade(task = task, polygons = polygons, regraded = listOf(submission))
            fixtures.verdict(submission, listOf(polygon))

            val graded = repository.countGraded(setOf(regrade.id))

            assertEquals(mapOf(regrade.id to 0), graded)
        }

        @Test
        fun `should not count a verdict of a submission outside the regrade`() {
            val task = fixtures.task()
            val polygons = listOf(fixtures.polygon())
            val regrade = fixtures.regrade(task = task, polygons = polygons, regraded = listOf(fixtures.gradingSubmission(task = task)))
            val outside = fixtures.gradingSubmission(task = task)
            fixtures.verdict(outside, polygons)

            val graded = repository.countGraded(setOf(regrade.id))

            assertEquals(mapOf(regrade.id to 0), graded)
        }

        @Test
        fun `should count each regrade separately and skip unknown ids`() {
            val task = fixtures.task()
            val polygons = listOf(fixtures.polygon())
            val submission = fixtures.gradingSubmission(task = task)
            val earlier = fixtures.regrade(task = task, polygons = polygons, regraded = listOf(submission))
            fixtures.verdict(submission, polygons)
            val later = fixtures.regrade(task = task, polygons = polygons, regraded = listOf(submission))

            val graded = repository.countGraded(setOf(earlier.id, later.id, RegradeId(UNKNOWN_ID)))

            assertEquals(mapOf(earlier.id to 1, later.id to 0), graded)
        }

        @Test
        fun `should count a verdict created after the next regrade only in the next regrade`() {
            val task = fixtures.task()
            val polygons = listOf(fixtures.polygon())
            val submission = fixtures.gradingSubmission(task = task)
            val earlier = fixtures.regrade(task = task, polygons = polygons, regraded = listOf(submission))
            val later = fixtures.regrade(task = task, polygons = polygons, regraded = listOf(submission))
            fixtures.verdict(submission, polygons)

            val graded = repository.countGraded(setOf(earlier.id, later.id))

            assertEquals(mapOf(earlier.id to 0, later.id to 1), graded)
        }

        @Test
        fun `should count a verdict created at the time of two regrades only in the regrade with the greater id`() {
            val task = fixtures.task()
            val polygons = listOf(fixtures.polygon())
            val submission = fixtures.gradingSubmission(task = task)
            val (lower, higher) = List(2) { fixtures.regrade(task = task, polygons = polygons, regraded = listOf(submission)) }
                .sortedBy { regrade -> regrade.id.value }
            val verdict = fixtures.verdict(submission, polygons)
            val createdAt = Timestamp.from(Instant.ofEpochSecond(1_700_000_000))
            jdbc.update("update ts_regrade set created_at = ? where task_id = ?", createdAt, task.id.value)
            jdbc.update("update ts_verdict set created_at = ? where id = ?", createdAt, verdict.id.value)

            val graded = repository.countGraded(setOf(lower.id, higher.id))

            assertEquals(mapOf(lower.id to 0, higher.id to 1), graded)
        }

        @Test
        fun `should not bound the progress by a regrade of another task`() {
            val task = fixtures.task()
            val polygons = listOf(fixtures.polygon())
            val submission = fixtures.gradingSubmission(task = task)
            val regrade = fixtures.regrade(task = task, polygons = polygons, regraded = listOf(submission))
            fixtures.regrade()
            fixtures.verdict(submission, polygons)

            val graded = repository.countGraded(setOf(regrade.id))

            assertEquals(mapOf(regrade.id to 1), graded)
        }

        @Test
        fun `should return an empty map without querying for no regrades`() {
            val (graded, statements) = withStatementCount { repository.countGraded(emptySet()) }

            assertEquals(emptyMap(), graded)
            assertEquals(0, statements)
        }
    }

    @Nested
    inner class StopPreviousTests {

        @Test
        fun `should stop the previous regrade with a queued submission at the time of the new one`() {
            val task = fixtures.task()
            val submission = fixtures.gradingSubmission(task = task)
            val previous = fixtures.regrade(task = task, regraded = listOf(submission))
            val started = submissions.update(submission.withData { status.inProgress() })
            submissions.update(started.withData { status.queued() })
            val next = fixtures.regrade(task = task, regraded = listOf(submission))

            repository.stopPrevious(next)

            val stored = assertNotNull(repository.findById(previous.id))
            assertEquals(next.createdAt, stored.data.stoppedAt)
            assertEquals(assertNotNull(previous.version).value + 1, assertNotNull(stored.version).value)
        }

        @Test
        fun `should stop the previous regrade with a submission in progress after it was created`() {
            val task = fixtures.task()
            val submission = fixtures.gradingSubmission(task = task)
            val previous = fixtures.regrade(task = task, regraded = listOf(submission))
            submissions.update(submission.withData { status.inProgress() })
            val next = fixtures.regrade(task = task, regraded = listOf(submission))

            repository.stopPrevious(next)

            assertEquals(next.createdAt, assertNotNull(repository.findById(previous.id)).data.stoppedAt)
        }

        @Test
        fun `should stop a regrade saved by a concurrent transaction when the next regrade of the task is saved`() {
            val task = fixtures.task()
            val taskId = task.id.value
            val submissionIds = listOf(fixtures.gradingSubmission(task = task).id.value)
            val polygonIds = listOf(fixtures.polygon().id.value)
            val data = regradeData {
                task(taskId)
                tests(polygonIds)
                submissions(submissionIds)
            }
            val firstSaved = CountDownLatch(1)
            val secondRead = CountDownLatch(1)

            Executors.newFixedThreadPool(2).use { pool ->
                val first = pool.submit(
                    Callable {
                        transactions.execute {
                            repository.stopPrevious(repository.save(data))
                            firstSaved.countDown()
                            check(secondRead.await(10, TimeUnit.SECONDS))
                        }
                    },
                )
                val second = pool.submit(
                    Callable {
                        transactions.execute {
                            check(firstSaved.await(10, TimeUnit.SECONDS))
                            repository.findByTaskId(task.id)
                            secondRead.countDown()
                            repository.stopPrevious(repository.save(data))
                        }
                    },
                )
                first.get()
                second.get()
            }

            val (earlier, later) = repository.findByTaskId(task.id)
            assertEquals(later.createdAt, earlier.data.stoppedAt)
        }

        @Test
        fun `should not stop the previous regrade whose submissions all got a result after it`() {
            val task = fixtures.task()
            val submission = fixtures.gradingSubmission(task = task)
            val previous = fixtures.regrade(task = task, regraded = listOf(submission))
            fixtures.successfulGradingVerdict(submission)
            val next = fixtures.regrade(task = task, regraded = listOf(submission))

            repository.stopPrevious(next)

            val stored = assertNotNull(repository.findById(previous.id))
            assertEquals(null, stored.data.stoppedAt)
            assertEquals(previous.version, stored.version)
        }

        @Test
        fun `should stop the previous regrade with a graded submission not changed since it was created`() {
            val task = fixtures.task()
            val submission = fixtures.gradingSubmission(task = task)
            fixtures.successfulGradingVerdict(submission)
            jdbc.update(
                "update ts_submission set updated_at = ? where id = ?",
                Timestamp.from(Instant.ofEpochSecond(1_700_000_000)),
                submission.id.value,
            )
            val previous = fixtures.regrade(task = task, regraded = listOf(submission))
            val next = fixtures.regrade(task = task, regraded = listOf(submission))

            repository.stopPrevious(next)

            assertEquals(next.createdAt, assertNotNull(repository.findById(previous.id)).data.stoppedAt)
        }

        @Test
        fun `should not stop an earlier regrade before the immediately previous one`() {
            val task = fixtures.task()
            val queued = fixtures.gradingSubmission(task = task)
            val earlier = fixtures.regrade(task = task, regraded = listOf(queued))
            val graded = fixtures.gradingSubmission(task = task)
            val previous = fixtures.regrade(task = task, regraded = listOf(graded))
            fixtures.successfulGradingVerdict(graded)
            val next = fixtures.regrade(task = task, regraded = listOf(queued, graded))

            repository.stopPrevious(next)

            assertEquals(null, assertNotNull(repository.findById(earlier.id)).data.stoppedAt)
            assertEquals(null, assertNotNull(repository.findById(previous.id)).data.stoppedAt)
        }

        @Test
        fun `should not stop a regrade of another task`() {
            val other = fixtures.regrade()
            val next = fixtures.regrade()

            repository.stopPrevious(next)

            val stored = assertNotNull(repository.findById(other.id))
            assertEquals(null, stored.data.stoppedAt)
            assertEquals(other.version, stored.version)
        }

        @Test
        fun `should change nothing for the first regrade of a task`() {
            val first = fixtures.regrade()

            repository.stopPrevious(first)

            val stored = assertNotNull(repository.findById(first.id))
            assertEquals(null, stored.data.stoppedAt)
            assertEquals(first.version, stored.version)
        }
    }
}
