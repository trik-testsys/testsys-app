package tech.testsys.infra.database.api.persistence.adapter.task

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import tech.testsys.domain.builder.api.regradeData
import tech.testsys.domain.builder.api.withData
import tech.testsys.domain.contract.persistence.repository.RegradeRepository
import tech.testsys.domain.model.task.Regrade
import tech.testsys.domain.model.task.RegradeData
import tech.testsys.domain.model.task.RegradeId
import tech.testsys.infra.database.api.persistence.adapter.PersistenceAdapterContractTests
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

class RegradePersistenceAdapterTests : PersistenceAdapterContractTests<RegradeData, RegradeId, Regrade>() {

    @Autowired
    override lateinit var repository: RegradeRepository

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
    }

    @Test
    fun `should fail to update a regrade and keep the stored submissions`() {
        val saved = repository.save(newData())
        val modified = saved.withData { submissions = mutableListOf() }

        assertFailsWith<UnsupportedOperationException> { repository.update(modified) }

        assertSameData(saved, assertNotNull(repository.findById(saved.id)))
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
        fun `should return an empty map without querying for no regrades`() {
            val (graded, statements) = withStatementCount { repository.countGraded(emptySet()) }

            assertEquals(emptyMap(), graded)
            assertEquals(0, statements)
        }
    }
}
