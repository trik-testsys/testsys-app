package tech.testsys.infra.database.api.persistence.adapter.task

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import tech.testsys.domain.builder.api.developerData
import tech.testsys.domain.builder.api.judgmentOrderData
import tech.testsys.domain.builder.api.studentData
import tech.testsys.domain.builder.api.submissionData
import tech.testsys.domain.builder.api.verdictData
import tech.testsys.domain.builder.api.withData
import tech.testsys.domain.contract.FileBlobStorage
import tech.testsys.domain.contract.StoredBlobRef
import tech.testsys.domain.contract.persistence.ContestTaskResult
import tech.testsys.domain.contract.persistence.JudgeSubmissionFilter
import tech.testsys.domain.contract.persistence.Pagination
import tech.testsys.domain.contract.persistence.Sort
import tech.testsys.domain.contract.persistence.SubmissionCount
import tech.testsys.domain.contract.persistence.repository.ClassRepository
import tech.testsys.domain.contract.persistence.repository.CompetitionRepository
import tech.testsys.domain.contract.persistence.repository.JudgmentOrderRepository
import tech.testsys.domain.contract.persistence.repository.MultipleRoleUserRepository
import tech.testsys.domain.contract.persistence.repository.SubmissionRepository
import tech.testsys.domain.contract.persistence.repository.VerdictRepository
import tech.testsys.domain.model.group.ClassId
import tech.testsys.domain.model.group.Competition
import tech.testsys.domain.model.group.CompetitionId
import tech.testsys.domain.model.task.Contest
import tech.testsys.domain.model.task.GradingResult
import tech.testsys.domain.model.task.Score
import tech.testsys.domain.model.task.Submission
import tech.testsys.domain.model.task.SubmissionId
import tech.testsys.domain.model.task.SubmissionStatus
import tech.testsys.domain.model.task.Task
import tech.testsys.domain.model.task.TaskId
import tech.testsys.domain.model.user.MultipleRoleUserId
import tech.testsys.domain.model.user.UserId
import tech.testsys.infra.database.DatabaseIntegrationTests
import java.nio.file.Path
import java.sql.Timestamp
import java.time.Instant

class SubmissionPersistenceAdapterQueryTests : DatabaseIntegrationTests() {

    @Autowired
    private lateinit var repository: SubmissionRepository

    @Autowired
    private lateinit var users: MultipleRoleUserRepository

    @Autowired
    private lateinit var classes: ClassRepository

    @Autowired
    private lateinit var competitions: CompetitionRepository

    @Autowired
    private lateinit var blobStorage: FileBlobStorage

    @Autowired
    private lateinit var verdicts: VerdictRepository

    @Autowired
    private lateinit var judgmentOrders: JudgmentOrderRepository

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

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

    @Test
    fun `should count grading submissions of the contest per task in every state without author tests or other contests`() {
        val contest = fixtures.contest()
        val task = fixtures.task()
        val other = fixtures.task()
        val author = fixtures.student()
        gradingSubmission(author.id, task, contest)
        graded(gradingSubmission(fixtures.participant().id, task, contest), 10)
        withStatus(gradingSubmission(author.id, other, contest), "TIMEOUT")
        gradingSubmission(author.id, task, fixtures.contest())
        fixtures.submission(author = author, task = task)

        val actual = repository.countGradingByTask(contest.id)

        assertEquals(mapOf(task.id to 2L, other.id to 1L), actual)
    }

    @Test
    fun `should count no tasks of a contest without submissions`() {
        assertEquals(emptyMap<TaskId, Long>(), repository.countGradingByTask(fixtures.contest().id))
    }

    @Test
    fun `should count grading submissions and distinct authors of the given authors in the given contests`() {
        val contest = fixtures.contest()
        val otherContest = fixtures.contest()
        val task = fixtures.task()
        val student = fixtures.student()
        val participant = fixtures.participant()
        gradingSubmission(student.id, task, contest)
        withStatus(gradingSubmission(student.id, task, otherContest), "IN_PROGRESS")
        graded(gradingSubmission(participant.id, task, contest), 10)
        gradingSubmission(fixtures.student().id, task, contest)
        gradingSubmission(student.id, task, fixtures.contest())
        fixtures.submission(author = student, task = task)

        val actual = repository.countGrading(setOf(student.id, participant.id), setOf(contest.id, otherContest.id))

        assertEquals(SubmissionCount(submissions = 3, authors = 2), actual)
    }

    @Test
    fun `should count nothing without querying storage if authors or contests are empty`() {
        val contest = fixtures.contest()
        val authorId = fixtures.student().id
        gradingSubmission(authorId, fixtures.task(), contest)

        val (actual, statementCount) = withStatementCount {
            listOf(repository.countGrading(emptySet(), setOf(contest.id)), repository.countGrading(setOf(authorId), emptySet()))
        }

        assertEquals(listOf(SubmissionCount(submissions = 0, authors = 0), SubmissionCount(submissions = 0, authors = 0)), actual)
        assertEquals(0, statementCount)
    }

    @Test
    fun `should find only the given authors with grading submissions in any state in the given contests`() {
        val contest = fixtures.contest()
        val otherContest = fixtures.contest()
        val task = fixtures.task()
        val studentId = fixtures.student().id
        val participantId = fixtures.participant().id
        val idleStudentId = fixtures.student().id
        val elsewhereStudentId = fixtures.student().id
        val testingStudent = fixtures.student()
        withStatus(gradingSubmission(studentId, task, otherContest), "IN_PROGRESS")
        graded(gradingSubmission(participantId, task, contest), 10)
        gradingSubmission(elsewhereStudentId, task, fixtures.contest())
        gradingSubmission(fixtures.student().id, task, contest)
        fixtures.submission(author = testingStudent, task = task)

        val actual = repository.findGradingAuthorIds(
            authorIds = setOf(studentId, participantId, idleStudentId, elsewhereStudentId, testingStudent.id),
            contestIds = setOf(contest.id, otherContest.id),
        )

        assertEquals(setOf(studentId, participantId), actual)
    }

    @Test
    fun `should find no authors without querying storage if authors or contests are empty`() {
        val contest = fixtures.contest()
        val authorId = fixtures.student().id
        gradingSubmission(authorId, fixtures.task(), contest)

        val (actual, statementCount) = withStatementCount {
            listOf(
                repository.findGradingAuthorIds(authorIds = emptySet(), contestIds = setOf(contest.id)),
                repository.findGradingAuthorIds(authorIds = setOf(authorId), contestIds = emptySet()),
            )
        }

        assertEquals(listOf(emptySet<UserId>(), emptySet<UserId>()), actual)
        assertEquals(0, statementCount)
    }

    @Test
    fun `should give an invalid submission the result 0 and replace it with the latest judgment order`() {
        val contest = fixtures.contest()
        val task = fixtures.task()
        val otherTask = fixtures.task()
        val authorId = fixtures.student().id
        withStatus(gradingSubmission(authorId, task, contest), "INVALID_SUBMISSION")
        withStatus(gradingSubmission(authorId, task, contest), "TIMEOUT")
        val judged = withStatus(gradingSubmission(authorId, otherTask, contest), "INVALID_SUBMISSION")
        judgmentOrder(judged, orderScore = 25, issuedAt = "2026-01-01T00:00:00Z")

        val actual = repository.findContestResults(contest.id, setOf(authorId), setOf(task.id, otherTask.id))

        assertEquals(
            listOf(
                ContestTaskResult(authorId = authorId, taskId = task.id, bestScore = Score(0), submissionCount = 2),
                ContestTaskResult(authorId = authorId, taskId = otherTask.id, bestScore = Score(25), submissionCount = 1),
            ),
            actual,
        )
    }

    @Test
    fun `should return student and participant graded submissions available to judges`() {
        val student = successful(fixtures.student().id)
        val participant = successful(fixtures.participant().id)
        val invalid = withStatus(fixtures.gradingSubmission(fixtures.student().id), "INVALID_SUBMISSION").id
        successful(fixtures.developer().id)
        fixtures.successfulGradingVerdict(fixtures.submission(author = fixtures.student()))

        val page = repository.findAvailableToJudge(Pagination(page = 0, size = 10))

        assertEquals(setOf(student, participant, invalid), page.content.map { it.id }.toSet())
        assertEquals(3, page.totalElements)
        assertEquals(1, page.totalPages)
        assertFalse(page.hasNext)
    }

    @ParameterizedTest
    @ValueSource(strings = ["QUEUED", "IN_PROGRESS", "GRADING_ERROR", "TIMEOUT"])
    fun `should exclude submissions to judges without a successful verdict or an invalid submission result`(state: String) {
        val submission = fixtures.gradingSubmission()
        fixtures.verdict(submission)
        withStatus(submission, state)

        val page = repository.findAvailableToJudge(Pagination(page = 0, size = 1))

        assertTrue(page.content.isEmpty())
        assertEquals(0, page.totalElements)
    }

    @Test
    fun `should return a regraded submission to judges once`() {
        val submission = fixtures.gradingSubmission()
        fixtures.successfulGradingVerdict(submission)
        fixtures.successfulGradingVerdict(requireNotNull(repository.findById(submission.id)))

        val page = repository.findAvailableToJudge(Pagination(page = 0, size = 1))

        assertEquals(listOf(submission.id), page.content.map { it.id })
        assertEquals(1, page.totalElements)
    }

    @Test
    fun `should remove submissions from judges when the author no longer holds the student role`() {
        val student = fixtures.student()
        successful(student.id)
        users.update(student.withData { roles { clear() } })

        val page = repository.findAvailableToJudge(
            pagination = Pagination(page = 0, size = 1),
            filter = JudgeSubmissionFilter(authorId = student.id),
        )

        assertTrue(page.content.isEmpty())
        assertEquals(0, page.totalElements)
    }

    @Test
    fun `should return one submission to judges for an author with several roles and memberships`() {
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
        val submission = successful(author.id)

        val page = repository.findAvailableToJudge(Pagination(page = 0, size = 1))

        assertEquals(listOf(submission), page.content.map { it.id })
        assertEquals(1, page.totalElements)
    }

    @ParameterizedTest
    @ValueSource(strings = ["student", "participant"])
    fun `should apply the author filter for judges to the query and count before pagination`(role: String) {
        val authorId = author(role)
        val first = successful(authorId)
        val second = successful(authorId)
        val third = successful(authorId)
        successful(fixtures.student().id)
        successful(fixtures.participant().id)
        val pagination = Pagination(page = 1, size = 1, sort = Sort(listOf(Sort.Order("id"))))

        val page = repository.findAvailableToJudge(pagination = pagination, filter = JudgeSubmissionFilter(authorId = authorId))

        assertEquals(listOf(second), page.content.map { it.id })
        assertTrue(first.value < second.value && second.value < third.value)
        assertEquals(pagination, page.pagination)
        assertEquals(3, page.totalElements)
        assertEquals(3, page.totalPages)
        assertTrue(page.hasNext)
    }

    @Test
    fun `should return an empty page to judges with a filtered total beyond the last page`() {
        val authorId = fixtures.student().id
        successful(authorId)
        successful(fixtures.participant().id)

        val page = repository.findAvailableToJudge(
            pagination = Pagination(page = 2, size = 1),
            filter = JudgeSubmissionFilter(authorId = authorId),
        )

        assertTrue(page.content.isEmpty())
        assertEquals(1, page.totalElements)
        assertEquals(1, page.totalPages)
        assertFalse(page.hasNext)
    }

    @Test
    fun `should return an empty page to judges for an unknown author or an author without an eligible role`() {
        successful(fixtures.student().id)
        val developerId = fixtures.developer().id
        successful(developerId)

        val pages = listOf(MultipleRoleUserId(-1), developerId).map { authorId ->
            repository.findAvailableToJudge(
                pagination = Pagination(page = 0, size = 1),
                filter = JudgeSubmissionFilter(authorId = authorId),
            )
        }

        assertTrue(pages.all { page -> page.content.isEmpty() && page.totalElements == 0L })
    }

    @Test
    fun `should order submissions for judges by caller descending submission time and id across pages`() {
        val first = successful(fixtures.student().id)
        val second = withStatus(fixtures.gradingSubmission(fixtures.student().id), "INVALID_SUBMISSION").id
        val third = successful(fixtures.student().id)
        setCreatedAt(first, "2026-01-02T00:00:00Z")
        setCreatedAt(second, "2026-01-01T00:00:00Z")
        setCreatedAt(third, "2026-01-02T00:00:00Z")
        val sort = Sort(listOf(Sort.Order("createdAt", Sort.Direction.DESC), Sort.Order("id", Sort.Direction.DESC)))
        val requests = (0..2).map { page -> Pagination(page = page, size = 1, sort = sort) }

        val pages = requests.map { pagination -> repository.findAvailableToJudge(pagination) }

        assertEquals(listOf(third, first, second), pages.flatMap { it.content }.map { it.id })
        pages.forEachIndexed { index, page ->
            assertEquals(3, page.totalElements)
            assertSame(requests[index], page.pagination)
        }
        assertFalse(pages.last().hasNext)
    }

    @Test
    fun `should order submissions for judges by caller ascending submission time and id`() {
        val first = successful(fixtures.student().id)
        val second = successful(fixtures.student().id)
        val third = successful(fixtures.student().id)
        setCreatedAt(first, "2026-01-02T00:00:00Z")
        setCreatedAt(second, "2026-01-01T00:00:00Z")
        setCreatedAt(third, "2026-01-02T00:00:00Z")
        val pagination = Pagination(page = 0, size = 10, sort = Sort(listOf(Sort.Order("createdAt"), Sort.Order("id"))))

        val page = repository.findAvailableToJudge(pagination)

        assertEquals(listOf(second, first, third), page.content.map { it.id })
        assertSame(pagination, page.pagination)
    }

    @Test
    fun `should retain unsorted pagination for judges without adding an order`() {
        val first = successful(fixtures.student().id)
        val second = successful(fixtures.student().id)
        val pagination = Pagination(page = 0, size = 10)

        val page = repository.findAvailableToJudge(pagination)

        assertEquals(setOf(first, second), page.content.map { it.id }.toSet())
        assertSame(pagination, page.pagination)
        assertEquals(Sort.UNSORTED, page.pagination.sort)
    }

    @Test
    fun `should return submissions to judges with lazy references without reading missing blob contents`() {
        val submission = fixtures.gradingSubmission()
        val verdict = fixtures.successfulGradingVerdict(submission)
        jdbcTemplate.queryForList("select stored_file_name from ts_file_data", String::class.java).filterNotNull()
            .forEach { key ->
                listOf(STATEMENT_PATH, EXERCISE_PATH, TEST_PATH, SOLUTION_PATH, RECORDING_PATH, LOGS_PATH)
                    .forEach { path -> blobStorage.delete(StoredBlobRef(key), Path.of(path)) }
            }

        val page = repository.findAvailableToJudge(Pagination(page = 0, size = 1))

        val status = assertInstanceOf(SubmissionStatus.Graded::class.java, page.content.single().data.status)
        assertEquals(verdict.id, assertInstanceOf(GradingResult.Success::class.java, status.grade).verdict.id)
    }

    // https://github.com/trik-testsys/testsys-app/pull/17
    @Test
    @Tag("regression")
    fun `should keep the statement count constant as the page of submissions for judges grows`() {
        val authorId = fixtures.student().id
        val graded = List(5) { successful(authorId) }
        val sort = Sort(listOf(Sort.Order("id")))

        val (onePage, oneSubmissionStatements) =
            withStatementCount { repository.findAvailableToJudge(Pagination(page = 0, size = 1, sort = sort)) }
        val (fourPage, fourSubmissionsStatements) =
            withStatementCount { repository.findAvailableToJudge(Pagination(page = 0, size = 4, sort = sort)) }

        assertEquals(graded.take(1), onePage.content.map { it.id })
        assertEquals(graded.take(4), fourPage.content.map { it.id })
        assertEquals(oneSubmissionStatements, fourSubmissionsStatements)
    }

    @Test
    fun `should filter submissions for judges by one submission without broadening author access`() {
        val author = fixtures.student().id
        val selected = successful(author)
        successful(author)
        val developerSubmission = successful(fixtures.developer().id)

        val pages = listOf(
            JudgeSubmissionFilter(authorId = author, submissionId = selected),
            JudgeSubmissionFilter(submissionId = developerSubmission),
        ).map { filter -> repository.findAvailableToJudge(pagination = Pagination(page = 0, size = 1), filter = filter) }

        assertEquals(listOf(listOf(selected), emptyList()), pages.map { page -> page.content.map { it.id } })
        assertEquals(listOf(1L, 0L), pages.map { page -> page.totalElements })
    }

    @Test
    fun `should require both student membership and contest assignment before class paging for judges`() {
        val author = fixtures.student()
        val outsider = fixtures.student()
        val contest = fixtures.contest()
        val selected = fixtures.studentClass(students = listOf(author))
        val neighbor = fixtures.studentClass(students = listOf(author, outsider))
        classes.update(selected.withData { contests = mutableListOf(contest.id) })
        classes.update(neighbor.withData { contests = mutableListOf(contest.id) })
        val matches = (0..2).map { successful(author.id, contest) }
        successful(outsider.id, contest)
        successful(author.id)
        val pagination = Pagination(page = 1, size = 1, sort = Sort(listOf(Sort.Order("id"))))

        val page = repository.findAvailableToJudge(pagination = pagination, filter = JudgeSubmissionFilter(classId = selected.id))

        assertEquals(listOf(matches[1]), page.content.map { it.id })
        assertEquals(3L, page.totalElements)
        assertTrue(page.hasNext)
        assertSame(pagination, page.pagination)
    }

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun `should reflect current class membership and contest assignment for judges`(removeMember: Boolean) {
        val author = fixtures.student()
        val contest = fixtures.contest()
        val selected = fixtures.studentClass(students = listOf(author))
        val assigned = classes.update(selected.withData { contests = mutableListOf(contest.id) })
        successful(author.id, contest)
        classes.update(assigned.withData { if (removeMember) students.clear() else contests.clear() })

        val page = repository.findAvailableToJudge(
            pagination = Pagination(page = 0, size = 1),
            filter = JudgeSubmissionFilter(classId = selected.id),
        )

        assertTrue(page.content.isEmpty())
        assertEquals(0L, page.totalElements)
    }

    @Test
    fun `should require participant membership and contest assignment before competition paging for judges`() {
        val selected = fixtures.competition()
        val neighbor = fixtures.competition()
        val author = fixtures.participant(selected)
        val outsider = fixtures.participant(neighbor)
        val contest = fixtures.contest()
        competitions.update(reloaded(selected).withData { contests = mutableListOf(contest.id) })
        competitions.update(reloaded(neighbor).withData { contests = mutableListOf(contest.id) })
        val matches = (0..2).map { successful(author.id, contest) }
        successful(outsider.id, contest)
        successful(fixtures.student().id, contest)
        successful(author.id)
        val pagination = Pagination(page = 1, size = 1, sort = Sort(listOf(Sort.Order("id"))))

        val page = repository.findAvailableToJudge(pagination = pagination, filter = JudgeSubmissionFilter(competitionId = selected.id))

        assertEquals(listOf(matches[1]), page.content.map { it.id })
        assertEquals(3L, page.totalElements)
        assertTrue(page.hasNext)
    }

    @ParameterizedTest
    @ValueSource(strings = ["submission", "class", "competition", "class and competition"])
    fun `should return no submissions to judges for unknown or disjoint filters`(kind: String) {
        val student = fixtures.student()
        val selectedClass = fixtures.studentClass(students = listOf(student))
        val selectedCompetition = fixtures.competition()
        val participant = fixtures.participant(selectedCompetition)
        val contest = fixtures.contest()
        classes.update(selectedClass.withData { contests = mutableListOf(contest.id) })
        competitions.update(reloaded(selectedCompetition).withData { contests = mutableListOf(contest.id) })
        successful(student.id, contest)
        successful(participant.id, contest)
        val filter = when (kind) {
            "submission" -> JudgeSubmissionFilter(submissionId = SubmissionId(-1))
            "class" -> JudgeSubmissionFilter(classId = ClassId(-1))
            "competition" -> JudgeSubmissionFilter(competitionId = CompetitionId(-1))
            "class and competition" -> JudgeSubmissionFilter(classId = selectedClass.id, competitionId = selectedCompetition.id)
            else -> error("Unexpected filter $kind")
        }

        val page = repository.findAvailableToJudge(pagination = Pagination(page = 0, size = 1), filter = filter)

        assertTrue(page.content.isEmpty())
        assertEquals(0L, page.totalElements)
    }

    /** Reads [competition] again: saving a participant increments the competition version. */
    private fun reloaded(competition: Competition): Competition = requireNotNull(competitions.findById(competition.id))

    private fun author(role: String): UserId = when (role) {
        "student" -> fixtures.student().id
        "participant" -> fixtures.participant().id
        else -> error("Unexpected author role $role")
    }

    private fun successful(authorId: UserId, contest: Contest = fixtures.contest()): SubmissionId {
        val submission = fixtures.gradingSubmission(authorId = authorId, contest = contest)
        fixtures.successfulGradingVerdict(submission)
        return submission.id
    }

    private fun setCreatedAt(submissionId: SubmissionId, value: String) {
        jdbcTemplate.update(
            "update ts_submission set created_at = ? where id = ?",
            Timestamp.from(Instant.parse(value)),
            submissionId.value,
        )
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
                "QUEUED" -> status.queued()
                "IN_PROGRESS" -> status.inProgress()
                "GRADING_ERROR" -> status.graded { status.error { description = "Grader failed" } }
                "TIMEOUT" -> status.graded { status.timeout() }
                "INVALID_SUBMISSION" -> status.graded { status.invalidSubmission { description = "Does not start" } }
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
}
