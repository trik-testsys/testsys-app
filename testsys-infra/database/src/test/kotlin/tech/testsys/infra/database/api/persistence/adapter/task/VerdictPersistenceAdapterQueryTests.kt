package tech.testsys.infra.database.api.persistence.adapter.task

import jakarta.persistence.EntityManagerFactory
import org.hibernate.SessionFactory
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import tech.testsys.domain.builder.api.developerData
import tech.testsys.domain.builder.api.studentData
import tech.testsys.domain.builder.api.verdictData
import tech.testsys.domain.builder.api.withData
import tech.testsys.domain.contract.FileBlobStorage
import tech.testsys.domain.contract.StoredBlobRef
import tech.testsys.domain.contract.persistence.Pagination
import tech.testsys.domain.contract.persistence.Sort
import tech.testsys.domain.contract.persistence.repository.MultipleRoleUserRepository
import tech.testsys.domain.contract.persistence.repository.SubmissionRepository
import tech.testsys.domain.contract.persistence.repository.VerdictRepository
import tech.testsys.domain.model.task.Verdict
import tech.testsys.domain.model.user.MultipleRoleUserId
import tech.testsys.domain.model.user.UserId
import tech.testsys.infra.database.DatabaseIntegrationTests
import java.sql.Timestamp
import java.time.Instant

class VerdictPersistenceAdapterQueryTests : DatabaseIntegrationTests() {

    @Autowired
    private lateinit var repository: VerdictRepository

    @Autowired
    private lateinit var submissions: SubmissionRepository

    @Autowired
    private lateinit var users: MultipleRoleUserRepository

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @Autowired
    private lateinit var blobStorage: FileBlobStorage

    @Autowired
    private lateinit var entityManagerFactory: EntityManagerFactory

    @Test
    fun `should return student and participant grading verdicts without community restrictions`() {
        val studentVerdict = successfulVerdict(fixtures.student().id)
        val participantVerdict = successfulVerdict(fixtures.participant().id)
        successfulVerdict(fixtures.developer().id)
        fixtures.successfulGradingVerdict(fixtures.submission(author = fixtures.student()))

        val page = repository.findAvailableToJudge(Pagination(page = 0, size = 10))

        assertEquals(setOf(studentVerdict.id, participantVerdict.id), page.content.map { it.id }.toSet())
        assertEquals(2, page.totalElements)
        assertEquals(1, page.totalPages)
        assertFalse(page.hasNext)
    }

    @ParameterizedTest
    @ValueSource(strings = ["QUEUED", "IN_PROGRESS", "GRADING_ERROR", "TIMEOUT"])
    fun `should exclude verdicts if submission has no current successful grading`(state: String) {
        val submission = fixtures.gradingSubmission()
        fixtures.verdict(submission)
        val changed = submission.withData {
            when (state) {
                "QUEUED" -> status.queued()
                "IN_PROGRESS" -> status.inProgress()
                "GRADING_ERROR" -> status.graded { status.error { description = "Grader failed" } }
                "TIMEOUT" -> status.graded { status.timeout() }
                else -> error("Unexpected test state $state")
            }
        }
        submissions.update(changed)

        val page = repository.findAvailableToJudge(Pagination(page = 0, size = 1))

        assertTrue(page.content.isEmpty())
        assertEquals(0, page.totalElements)
    }

    @Test
    fun `should return only the current verdict after regrading`() {
        val submission = fixtures.gradingSubmission()
        fixtures.successfulGradingVerdict(submission)
        val currentSubmission = requireNotNull(submissions.findById(submission.id))
        val currentVerdict = fixtures.successfulGradingVerdict(currentSubmission)

        val page = repository.findAvailableToJudge(Pagination(page = 0, size = 1))

        assertEquals(listOf(currentVerdict.id), page.content.map { it.id })
        assertEquals(1, page.totalElements)
    }

    @Test
    fun `should remove results when the author no longer holds the student role`() {
        val student = fixtures.student()
        successfulVerdict(student.id)
        users.update(student.withData { roles { clear() } })

        val page = repository.findAvailableToJudge(pagination = Pagination(page = 0, size = 1), authorId = student.id)

        assertTrue(page.content.isEmpty())
        assertEquals(0, page.totalElements)
    }

    @Test
    fun `should return one verdict for an author with several roles and memberships`() {
        val communityIds = listOf(fixtures.community().id.value, fixtures.community().id.value)
        val author = fixtures.multipleRoleUser {
            roles {
                student {
                    memberOf(communityIds)
                    data = studentData {}
                }
                developer {
                    memberOf(communityIds)
                    data = developerData {}
                }
            }
        }
        val verdict = successfulVerdict(author.id)

        val page = repository.findAvailableToJudge(Pagination(page = 0, size = 1))

        assertEquals(listOf(verdict.id), page.content.map { it.id })
        assertEquals(1, page.totalElements)
    }

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun `should apply the author user id to the query and count before pagination`(participant: Boolean) {
        val authorId = if (participant) fixtures.participant().id else fixtures.student().id
        val first = successfulVerdict(authorId)
        val second = successfulVerdict(authorId)
        val third = successfulVerdict(authorId)
        successfulVerdict(fixtures.student().id)
        successfulVerdict(fixtures.participant().id)
        val pagination = Pagination(page = 1, size = 1, sort = Sort(listOf(Sort.Order("id"))))

        val page = repository.findAvailableToJudge(pagination = pagination, authorId = authorId)

        assertEquals(listOf(second.id), page.content.map { it.id })
        assertTrue(first.id.value < second.id.value && second.id.value < third.id.value)
        assertEquals(pagination, page.pagination)
        assertEquals(3, page.totalElements)
        assertEquals(3, page.totalPages)
        assertTrue(page.hasNext)
    }

    @Test
    fun `should return an empty page with a filtered total beyond the last page`() {
        val authorId = fixtures.student().id
        successfulVerdict(authorId)
        successfulVerdict(fixtures.participant().id)

        val page = repository.findAvailableToJudge(pagination = Pagination(page = 2, size = 1), authorId = authorId)

        assertTrue(page.content.isEmpty())
        assertEquals(1, page.totalElements)
        assertEquals(1, page.totalPages)
        assertFalse(page.hasNext)
    }

    @Test
    fun `should return an empty page for an unknown author user id`() {
        successfulVerdict(fixtures.student().id)

        val page = repository.findAvailableToJudge(pagination = Pagination(page = 0, size = 1), authorId = MultipleRoleUserId(-1))

        assertTrue(page.content.isEmpty())
        assertEquals(0, page.totalElements)
        assertEquals(0, page.totalPages)
    }

    @Test
    fun `should return an empty page for a filtered author without an eligible role`() {
        val authorId = fixtures.developer().id
        successfulVerdict(authorId)

        val page = repository.findAvailableToJudge(pagination = Pagination(page = 0, size = 1), authorId = authorId)

        assertTrue(page.content.isEmpty())
        assertEquals(0, page.totalElements)
    }

    @Test
    fun `should apply caller descending creation time and id order across pages`() {
        val first = successfulVerdict(fixtures.student().id)
        val second = successfulVerdict(fixtures.student().id)
        val third = successfulVerdict(fixtures.student().id)
        setCreatedAt(first, "2026-01-02T00:00:00Z")
        setCreatedAt(second, "2026-01-01T00:00:00Z")
        setCreatedAt(third, "2026-01-02T00:00:00Z")

        val sort = Sort(listOf(Sort.Order("createdAt", Sort.Direction.DESC), Sort.Order("id", Sort.Direction.DESC)))
        val requests = (0..2).map { page -> Pagination(page = page, size = 1, sort = sort) }

        val pages = requests.map { pagination -> repository.findAvailableToJudge(pagination) }

        assertEquals(listOf(third.id, first.id, second.id), pages.flatMap { it.content }.map { it.id })
        pages.forEachIndexed { index, page ->
            assertEquals(3, page.totalElements)
            assertSame(requests[index], page.pagination)
        }
        assertFalse(pages.last().hasNext)
    }

    @Test
    fun `should apply caller ascending creation time and id order`() {
        val first = successfulVerdict(fixtures.student().id)
        val second = successfulVerdict(fixtures.student().id)
        val third = successfulVerdict(fixtures.student().id)
        setCreatedAt(first, "2026-01-02T00:00:00Z")
        setCreatedAt(second, "2026-01-01T00:00:00Z")
        setCreatedAt(third, "2026-01-02T00:00:00Z")
        val pagination = Pagination(page = 0, size = 10, sort = Sort(listOf(Sort.Order("createdAt"), Sort.Order("id"))))

        val page = repository.findAvailableToJudge(pagination)

        assertEquals(listOf(second.id, first.id, third.id), page.content.map { it.id })
        assertSame(pagination, page.pagination)
    }

    @Test
    fun `should preserve scores and file references without reading missing blob contents`() {
        val submission = fixtures.gradingSubmission()
        val polygonId = fixtures.polygon().id.value
        val logsId = fixtures.logs().id.value
        val recordingId = fixtures.recording().id.value
        val verdict = repository.save(
            verdictData {
                task = submission.data.task.id
                this.submission = submission.id
                testVerdict {
                    score = 42
                    test(polygonId)
                    logs(logsId)
                    recording(recordingId)
                }
            },
        )
        submissions.update(submission.withData { status.graded { status.success { this.verdict = verdict.id } } })
        jdbcTemplate.queryForList("select stored_file_name from ts_file_data", String::class.java)
            .forEach { key -> blobStorage.delete(StoredBlobRef(key)) }

        val page = repository.findAvailableToJudge(Pagination(page = 0, size = 1))

        assertEquals(verdict.id, page.content.single().id)
        val outcome = page.content.single().data.testVerdicts.single()
        assertEquals(42, outcome.score.value)
        assertEquals(logsId, outcome.logs.id.value)
        assertEquals(recordingId, outcome.recording?.id?.value)
    }

    @Test
    fun `should apply caller sorting by submission id without restricting fields`() {
        val firstSubmission = fixtures.gradingSubmission()
        val secondSubmission = fixtures.gradingSubmission()
        val secondVerdict = fixtures.successfulGradingVerdict(secondSubmission)
        val firstVerdict = fixtures.successfulGradingVerdict(firstSubmission)
        val pagination = Pagination(page = 0, size = 2, sort = Sort(listOf(Sort.Order("submissionId"))))

        val page = repository.findAvailableToJudge(pagination)

        assertEquals(listOf(firstVerdict.id, secondVerdict.id), page.content.map { it.id })
        assertSame(pagination, page.pagination)
    }

    @Test
    fun `should retain unsorted pagination without adding an order`() {
        val first = successfulVerdict(fixtures.student().id)
        val second = successfulVerdict(fixtures.student().id)
        val pagination = Pagination(page = 0, size = 10)

        val page = repository.findAvailableToJudge(pagination)

        assertEquals(setOf(first.id, second.id), page.content.map { it.id }.toSet())
        assertSame(pagination, page.pagination)
        assertEquals(Sort.UNSORTED, page.pagination.sort)
    }

    @ParameterizedTest
    @ValueSource(ints = [1, 4])
    @Tag("regression")
    fun `should keep the statement count constant as the verdict page grows`(pageSize: Int) {
        val authorId = fixtures.student().id
        val verdicts = List(5) { successfulVerdict(authorId) }
        val pagination = Pagination(page = 0, size = pageSize, sort = Sort(listOf(Sort.Order("id"))))

        val (page, statementCount) = withStatementCount { repository.findAvailableToJudge(pagination) }

        assertEquals(verdicts.take(pageSize).map { it.id }, page.content.map { it.id })
        assertEquals(5, page.totalElements)
        assertEquals(3, statementCount)
    }

    @Test
    fun `should skip loading polygon outcomes for an empty page`() {
        successfulVerdict(fixtures.student().id)
        val pagination = Pagination(page = 5, size = 1)

        val (page, statementCount) = withStatementCount { repository.findAvailableToJudge(pagination) }

        assertTrue(page.content.isEmpty())
        assertEquals(1, page.totalElements)
        assertEquals(2, statementCount)
    }

    @Test
    fun `should group polygon outcomes by verdict and retain caller page order`() {
        val authorId = fixtures.student().id
        val firstPolygonId = fixtures.polygon().id.value
        val secondPolygonId = fixtures.polygon().id.value
        val first = successfulVerdictWithOutcomes(
            authorId = authorId,
            testIds = listOf(secondPolygonId, firstPolygonId),
            scores = listOf(20, 10),
        )
        val second = successfulVerdictWithOutcomes(
            authorId = authorId,
            testIds = listOf(firstPolygonId, secondPolygonId),
            scores = listOf(30, 40),
        )
        val pagination = Pagination(page = 0, size = 2, sort = Sort(listOf(Sort.Order("id", Sort.Direction.DESC))))

        val page = repository.findAvailableToJudge(pagination)

        assertEquals(listOf(second.id, first.id), page.content.map { it.id })
        assertEquals(
            listOf(listOf(30, 40), listOf(10, 20)),
            page.content.map { verdict -> verdict.data.testVerdicts.map { it.score.value } },
        )
        assertEquals(
            listOf(listOf(firstPolygonId, secondPolygonId), listOf(firstPolygonId, secondPolygonId)),
            page.content.map { verdict -> verdict.data.testVerdicts.map { it.test.id.value } },
        )
        assertSame(pagination, page.pagination)
    }

    @Test
    fun `should preserve the failure for a current verdict without polygon outcomes`() {
        val verdict = successfulVerdict(fixtures.student().id)
        jdbcTemplate.update("delete from ts_test_verdict where verdict_id = ?", verdict.id.value)

        val failure = assertThrows(IllegalArgumentException::class.java) {
            repository.findAvailableToJudge(Pagination(page = 0, size = 1))
        }

        assertTrue(requireNotNull(failure.message).contains(verdict.data.submission.id.value.toString()))
    }

    private fun successfulVerdict(authorId: UserId): Verdict = fixtures.successfulGradingVerdict(fixtures.gradingSubmission(authorId))

    private fun successfulVerdictWithOutcomes(authorId: UserId, testIds: List<Long>, scores: List<Int>): Verdict {
        val submission = fixtures.gradingSubmission(authorId)
        val firstLogsId = fixtures.logs().id.value
        val secondLogsId = fixtures.logs().id.value
        val verdict = repository.save(
            verdictData {
                task = submission.data.task.id
                this.submission = submission.id
                testVerdict {
                    test(testIds[0])
                    score = scores[0]
                    logs(firstLogsId)
                }
                testVerdict {
                    test(testIds[1])
                    score = scores[1]
                    logs(secondLogsId)
                }
            },
        )
        submissions.update(submission.withData { status.graded { status.success { this.verdict = verdict.id } } })
        return verdict
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

    private fun setCreatedAt(verdict: Verdict, value: String) {
        jdbcTemplate.update("update ts_verdict set created_at = ? where id = ?", Timestamp.from(Instant.parse(value)), verdict.id.value)
    }
}
