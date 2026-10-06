package tech.testsys.operation.user

import io.mockk.Called
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import tech.testsys.domain.builder.api.judgeData
import tech.testsys.domain.builder.api.verdict
import tech.testsys.domain.builder.api.verdictData
import tech.testsys.domain.contract.persistence.Page
import tech.testsys.domain.contract.persistence.Pagination
import tech.testsys.domain.contract.persistence.Sort
import tech.testsys.domain.contract.persistence.repository.VerdictRepository
import tech.testsys.domain.model.task.Verdict
import tech.testsys.domain.model.user.MultipleRoleUserId
import tech.testsys.domain.model.user.SingleRoleUserId
import tech.testsys.operation.error.MissedJudgeRoleError
import tech.testsys.operation.error.getOrThrow
import tech.testsys.operation.util.assertRaises
import tech.testsys.operation.util.testJudge
import tech.testsys.operation.util.testMultipleRoleUser
import java.time.Instant

class JudgeOperationsTests {

    private val repository = mockk<VerdictRepository>()
    private val operations = JudgeOperations(repository)
    private val judge = testJudge { data = judgeData {} }

    @Nested
    inner class ViewResultsTests {

        @Test
        fun `should raise MissedJudgeRoleError before querying if user is not a judge`() {
            val user = testMultipleRoleUser {}

            assertRaises(MissedJudgeRoleError) { operations.viewResults(user = user, pagination = Pagination(page = 0, size = 10)) }

            verify { repository wasNot Called }
        }

        @Test
        fun `should return the page with scores and lazy references unchanged`() {
            val pagination = Pagination(page = 1, size = 2)
            val responsePagination = pagination.copy(sort = Sort(listOf(Sort.Order("storageOrder"))))
            val first = testVerdict(10)
            val second = testVerdict(20)
            val page = Page(content = listOf(first, second), pagination = responsePagination, totalElements = 5)
            every { repository.findAvailableToJudge(refEq(pagination), null) } returns page

            val result = operations.viewResults(user = judge, pagination = pagination).getOrThrow()

            assertSame(page, result)
            assertSame(first, result.content[0])
            assertEquals(listOf(75, 0), result.content[0].data.testVerdicts.map { it.score.value })
            assertEquals(listOf(30L, 31L), result.content[0].data.testVerdicts.map { it.logs.id.value })
            assertEquals(40L, result.content[0].data.testVerdicts[0].recording?.id?.value)
            assertEquals(null, result.content[0].data.testVerdicts[1].recording)
            assertEquals(3, result.totalPages)
            assertEquals(true, result.hasNext)
            verify(exactly = 1) { repository.findAvailableToJudge(refEq(pagination), null) }
        }

        @Test
        fun `should pass a student user id filter and retain the requested page`() {
            val authorId = MultipleRoleUserId(7)
            val pagination = Pagination(page = 3, size = 4)
            val page = Page<Verdict>(content = emptyList(), pagination = pagination, totalElements = 2)
            every { repository.findAvailableToJudge(refEq(pagination), authorId) } returns page

            val result = operations.viewResults(user = judge, pagination = pagination, authorId = authorId).getOrThrow()

            assertSame(page, result)
            assertFalse(result.hasNext)
            verify(exactly = 1) { repository.findAvailableToJudge(refEq(pagination), authorId) }
        }

        @Test
        fun `should pass a participant user id and preserve the requested pagination`() {
            val authorId = SingleRoleUserId(9)
            val pagination = Pagination(page = 0, size = 1)
            val page = Page(content = listOf(testVerdict(1)), pagination = pagination, totalElements = 1)
            every { repository.findAvailableToJudge(pagination, authorId) } returns page

            val result = operations.viewResults(user = judge, pagination = pagination, authorId = authorId).getOrThrow()

            assertSame(page, result)
            verify(exactly = 1) { repository.findAvailableToJudge(pagination, authorId) }
        }

        @Test
        fun `should return an empty page without a domain error`() {
            val pagination = Pagination(page = 0, size = 10)
            val page = Page<Verdict>(content = emptyList(), pagination = pagination, totalElements = 0)
            every { repository.findAvailableToJudge(page.pagination, null) } returns page

            val result = operations.viewResults(user = judge, pagination = pagination).getOrThrow()

            assertSame(page, result)
            assertEquals(0, result.totalPages)
            assertFalse(result.hasNext)
        }

        @Test
        fun `should forward arbitrary sorting metadata unchanged to the repository`() {
            val pagination =
                Pagination(page = 0, size = 10, sort = Sort(listOf(Sort.Order("storageField", Sort.Direction.DESC))))
            val page = Page<Verdict>(content = emptyList(), pagination = pagination, totalElements = 0)
            every { repository.findAvailableToJudge(refEq(pagination), null) } returns page

            val result = operations.viewResults(user = judge, pagination = pagination).getOrThrow()

            assertSame(page, result)
            verify(exactly = 1) { repository.findAvailableToJudge(refEq(pagination), null) }
        }

        @Test
        fun `should propagate the original technical exception`() {
            val pagination = Pagination(page = 0, size = 10)
            val failure = IllegalStateException("Storage unavailable")
            every { repository.findAvailableToJudge(refEq(pagination), null) } throws failure

            val thrown = assertThrows(IllegalStateException::class.java) {
                operations.viewResults(user = judge, pagination = pagination)
            }

            assertSame(failure, thrown)
        }
    }

    private fun testVerdict(verdictId: Long): Verdict = verdict {
        id = verdictId
        createdAt = Instant.parse("2026-01-01T00:00:00Z")
        data = verdictData {
            task(1)
            submission(2)
            testVerdict {
                score = 75
                test(10)
                logs(30)
                recording(40)
            }
            testVerdict {
                score = 0
                test(11)
                logs(31)
            }
        }
    }
}
