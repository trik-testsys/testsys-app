package tech.testsys.infra.database.api.persistence.adapter.task

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import tech.testsys.domain.builder.api.submission
import tech.testsys.domain.builder.api.submissionData
import tech.testsys.domain.builder.api.withData
import tech.testsys.domain.contract.persistence.repository.SubmissionRepository
import tech.testsys.domain.model.task.GradingResult
import tech.testsys.domain.model.task.Submission
import tech.testsys.domain.model.task.SubmissionData
import tech.testsys.domain.model.task.SubmissionId
import tech.testsys.domain.model.task.SubmissionKind
import tech.testsys.domain.model.task.SubmissionStatus
import tech.testsys.domain.model.task.TrikStudioVersion
import tech.testsys.domain.model.user.SingleRoleUserId
import tech.testsys.infra.database.api.persistence.adapter.UpdatablePersistenceAdapterContractTests
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.repository.task.SubmissionJpaEntityRepository
import tech.testsys.infra.database.internal.jpa.repository.task.TrikStudioVersionJpaEntityRepository
import tech.testsys.infra.database.internal.utils.findIdByTagOrError
import java.sql.Timestamp
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@OptIn(InternalDatabaseApi::class)
class SubmissionPersistenceAdapterTests : UpdatablePersistenceAdapterContractTests<SubmissionData, SubmissionId, Submission>() {

    @Autowired
    override lateinit var repository: SubmissionRepository

    @Autowired
    private lateinit var submissionJpaEntityRepository: SubmissionJpaEntityRepository

    @Autowired
    private lateinit var trikStudioVersionJpaEntityRepository: TrikStudioVersionJpaEntityRepository

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    override fun newData(): SubmissionData = developerSolutionTestData(fixtures.trikStudioVersion())

    override fun modified(entity: Submission) = entity.withData { status.inProgress() }

    override fun detached(entity: Submission) = submission {
        id = entity.id.value
        createdAt = entity.createdAt
        data = entity.data
    }

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
            is SubmissionKind.DeveloperSolutionTest -> assertEquals(
                expected.trikStudioVersion,
                assertIs<SubmissionKind.DeveloperSolutionTest>(actual).trikStudioVersion,
            )
            is SubmissionKind.Grading -> assertEquals(expected.contest.id, assertIs<SubmissionKind.Grading>(actual).contest.id)
        }
    }

    private fun gradingSubmissionData(contestId: Long, taskId: Long = fixtures.task().id.value): SubmissionData {
        val author = fixtures.student()
        val authorId = author.id
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
    fun `should queue, pick up and grade a contest submission with a verdict`() {
        val contestId = fixtures.contest().id.value
        val queued = repository.save(gradingSubmissionData(contestId))

        val inProgress = repository.update(queued.withData { status.inProgress() })
        val verdictId = fixtures.verdict(inProgress).id.value
        val graded = repository.update(inProgress.withData { status.graded { status.success { verdict(verdictId) } } })

        val found = assertNotNull(repository.findById(queued.id))
        assertSameEntity(graded, found)
        assertEquals(SubmissionStatus.InProgress, inProgress.data.status)
        assertEquals(
            verdictId,
            assertIs<GradingResult.Success>(assertIs<SubmissionStatus.Graded>(found.data.status).grade).verdict.id.value,
        )
        assertEquals(contestId, assertIs<SubmissionKind.Grading>(found.data.kind).contest.id.value)
    }

    @Test
    fun `should keep a grading error through a round trip`() {
        val data = newData()

        val saved = repository.save(data)
        val failed = repository.update(saved.withData { status.graded { status.error { description = "Grader crashed" } } })

        val found = assertNotNull(repository.findById(saved.id))
        assertSameEntity(failed, found)
        val grade = assertIs<GradingResult.GradingError>(assertIs<SubmissionStatus.Graded>(found.data.status).grade)
        assertEquals("Grader crashed", grade.description)
    }

    @Test
    fun `should keep a grading timeout through a round trip`() {
        val saved = repository.save(newData())

        val timedOut = repository.update(saved.withData { status.graded { status.timeout() } })

        val found = assertNotNull(repository.findById(saved.id))
        assertSameEntity(timedOut, found)
        assertEquals(GradingResult.Timeout, assertIs<SubmissionStatus.Graded>(found.data.status).grade)
    }

    @Test
    fun `should project judgment orders from the orders issued for the submission`() {
        val saved = repository.save(newData())
        val order = fixtures.judgmentOrder(submission = saved)
        fixtures.judgmentOrder()

        val found = assertNotNull(repository.findById(saved.id))

        assertEquals(listOf(order.id), found.data.judgmentOrders.ids)
    }

    @Test
    fun `should ignore judgment orders given on save`() {
        val author = fixtures.developer()
        val authorId = author.id
        val taskId = fixtures.task(author).id.value
        val solutionId = fixtures.solution().id.value
        val version = fixtures.trikStudioVersion()

        val saved = repository.save(
            submissionData {
                this.author = authorId
                solution(solutionId)
                task(taskId)
                status.queued()
                kind.developerSolutionTest { trikStudioVersion = version }
                judgmentOrders(listOf(UNKNOWN_ID))
            },
        )

        assertEquals(emptyList(), saved.data.judgmentOrders.ids)
        assertEquals(emptyList(), assertNotNull(repository.findById(saved.id)).data.judgmentOrders.ids)
    }

    @Test
    fun `should keep the fixed-role kind of the author through a round trip`() {
        val authorId = fixtures.participant().id
        val taskId = fixtures.task().id.value
        val solutionId = fixtures.solution().id.value
        val version = fixtures.trikStudioVersion()

        val saved = repository.save(
            submissionData {
                this.author = authorId
                solution(solutionId)
                task(taskId)
                status.queued()
                kind.developerSolutionTest { trikStudioVersion = version }
            },
        )

        val found = assertNotNull(repository.findById(saved.id))
        assertEquals(authorId, saved.data.author.id)
        assertEquals(authorId, found.data.author.id)
        assertIs<SingleRoleUserId>(found.data.author.id)
    }

    @Test
    fun `should keep the TRIK Studio version if an unregistered version is passed on update`() {
        val saved = repository.save(newData())
        val unknownVersion = TrikStudioVersion(fixtures.unique("unregistered"))
        val changed = saved.withData { kind.developerSolutionTest { trikStudioVersion = unknownVersion } }

        val updated = repository.update(changed)

        assertSameKind(saved.data.kind, updated.data.kind)
        assertSameKind(saved.data.kind, assertNotNull(repository.findById(saved.id)).data.kind)
    }

    @Test
    fun `should fail to save a submission with an unregistered TRIK Studio version`() {
        val unknownVersion = TrikStudioVersion(fixtures.unique("unregistered"))
        val data = developerSolutionTestData(unknownVersion)

        assertFailsWith<IllegalArgumentException> { repository.save(data) }
    }

    @Test
    fun `should keep the author, solution, task and kind if other ones are passed on update`() {
        val saved = repository.save(newData())
        val otherAuthor = fixtures.student().id
        val otherSolutionId = fixtures.solution().id.value
        val otherTaskId = fixtures.task().id.value
        val otherContestId = fixtures.contest().id.value

        repository.update(
            saved.withData {
                author = otherAuthor
                solution(otherSolutionId)
                task(otherTaskId)
                kind.grading { contest(otherContestId) }
            },
        )

        val found = assertNotNull(repository.findById(saved.id))
        assertEquals(saved.data.author.id, found.data.author.id)
        assertEquals(saved.data.solution.id, found.data.solution.id)
        assertEquals(saved.data.task.id, found.data.task.id)
        assertSameKind(saved.data.kind, found.data.kind)
    }

    @Test
    fun `should save grading without a separate TRIK Studio version`() {
        val contestId = fixtures.contest().id.value

        val saved = repository.save(gradingSubmissionData(contestId))

        val row = submissionJpaEntityRepository.findById(saved.id.value).orElseThrow()
        val found = assertNotNull(repository.findById(saved.id))
        assertNull(row.trikStudioVersionId)
        assertEquals(contestId, assertIs<SubmissionKind.Grading>(saved.data.kind).contest.id.value)
        assertSameEntity(saved, found)
    }

    @Test
    fun `should ignore a legacy version column when reading grading`() {
        val contestId = fixtures.contest().id.value
        val saved = repository.save(gradingSubmissionData(contestId))
        val version = fixtures.trikStudioVersion()
        val versionId = trikStudioVersionJpaEntityRepository.findIdByTagOrError(version.version)
        jdbcTemplate.update("update ts_submission set trik_studio_version_id = ? where id = ?", versionId, saved.id.value)

        val found = assertNotNull(repository.findById(saved.id))

        assertSameEntity(saved, found)
        assertEquals(contestId, assertIs<SubmissionKind.Grading>(found.data.kind).contest.id.value)
    }

    @Test
    fun `should keep grading contest if another contest is passed on update`() {
        val contestId = fixtures.contest().id.value
        val saved = repository.save(gradingSubmissionData(contestId))
        val otherContestId = fixtures.contest().id.value

        val updated = repository.update(saved.withData { kind.grading { contest(otherContestId) } })

        assertEquals(contestId, assertIs<SubmissionKind.Grading>(updated.data.kind).contest.id.value)
        assertSameEntity(updated, assertNotNull(repository.findById(saved.id)))
    }

    @Test
    fun `should keep grading kind if developer solution test with unknown version is passed on update`() {
        val contestId = fixtures.contest().id.value
        val saved = repository.save(gradingSubmissionData(contestId))
        val unknownVersion = TrikStudioVersion(fixtures.unique("unregistered"))

        val updated = repository.update(
            saved.withData { kind.developerSolutionTest { trikStudioVersion = unknownVersion } },
        )

        assertEquals(contestId, assertIs<SubmissionKind.Grading>(updated.data.kind).contest.id.value)
        assertSameEntity(updated, assertNotNull(repository.findById(saved.id)))
    }

    @Test
    fun `should find only contest submissions of the task ordered by id`() {
        val task = fixtures.task()
        val contestId = fixtures.contest().id.value
        val first = repository.save(gradingSubmissionData(contestId, task.id.value))
        repository.save(developerSolutionTestData(fixtures.trikStudioVersion(), task.id.value))
        repository.save(gradingSubmissionData(contestId))
        val second = repository.save(gradingSubmissionData(contestId, task.id.value))

        val found = repository.findGradingByTaskId(task.id)

        assertEquals(listOf(first.id, second.id), found.map { submission -> submission.id })
        assertEquals(
            listOf(contestId, contestId),
            found.map { submission -> assertIs<SubmissionKind.Grading>(submission.data.kind).contest.id.value },
        )
    }

    @Test
    fun `should find submissions by ids with the same statement count for one and twenty ids`() {
        val version = fixtures.trikStudioVersion()
        val judge = fixtures.judge()
        val saved = List(20) { repository.save(developerSolutionTestData(version)) }
        val orderIds = saved.map { submission -> fixtures.judgmentOrder(judge = judge, submission = submission).id }
        val ids = saved.map { submission -> submission.id }

        val (one, oneIdStatements) = withStatementCount { repository.findByIds(ids.take(1)) }
        val (twenty, twentyIdsStatements) = withStatementCount { repository.findByIds(ids) }

        assertEquals(ids.take(1), one.map { submission -> submission.id })
        assertEquals(ids.toSet(), twenty.map { submission -> submission.id }.toSet())
        assertEquals(
            ids.zip(orderIds).toMap(),
            twenty.associate { submission -> submission.id to submission.data.judgmentOrders.ids.single() },
        )
        assertEquals(
            List(20) { version },
            twenty.map { submission -> assertIs<SubmissionKind.DeveloperSolutionTest>(submission.data.kind).trikStudioVersion },
        )
        assertEquals(oneIdStatements, twentyIdsStatements)
    }

    @Test
    fun `should find contest submissions of a task with the same statement count for one and twenty submissions`() {
        val contestId = fixtures.contest().id.value
        val oneSubmissionTask = fixtures.task()
        val twentySubmissionsTask = fixtures.task()
        val single = repository.save(gradingSubmissionData(contestId, oneSubmissionTask.id.value))
        val twentyIds = List(20) {
            repository.save(gradingSubmissionData(contestId, twentySubmissionsTask.id.value)).id
        }

        val (one, oneSubmissionStatements) = withStatementCount { repository.findGradingByTaskId(oneSubmissionTask.id) }
        val (twenty, twentySubmissionsStatements) = withStatementCount { repository.findGradingByTaskId(twentySubmissionsTask.id) }

        assertEquals(listOf(single.id), one.map { submission -> submission.id })
        assertEquals(twentyIds, twenty.map { submission -> submission.id })
        assertEquals(oneSubmissionStatements, twentySubmissionsStatements)
    }

    @Test
    fun `should find no contest submissions of a task with only author solution tests`() {
        val task = fixtures.task()
        repository.save(developerSolutionTestData(fixtures.trikStudioVersion(), task.id.value))

        val found = repository.findGradingByTaskId(task.id)

        assertEquals(emptyList(), found)
    }

    @Test
    fun `should find no contest submissions of a task without submissions`() {
        val task = fixtures.task()
        repository.save(gradingSubmissionData(fixtures.contest().id.value))

        val found = repository.findGradingByTaskId(task.id)

        assertEquals(emptyList(), found)
    }

    @Nested
    inner class FindGradingByContextTests {

        @Test
        fun `should assemble contexts of one and twenty submissions with the same statement count`() {
            val author = fixtures.student()
            val contest = fixtures.contest()
            val oneTask = fixtures.task()
            val twentyTask = fixtures.task()
            val single = fixtures.gradingSubmission(authorId = author.id, contest = contest, task = oneTask)
            val saved = List(20) { fixtures.gradingSubmission(authorId = author.id, contest = contest, task = twentyTask) }
            val judge = fixtures.judge()
            val singleOrder = fixtures.judgmentOrder(judge = judge, submission = single).id
            val orders = saved.map { submission -> fixtures.judgmentOrder(judge = judge, submission = submission).id }

            val (one, oneStatements) = withStatementCount {
                repository.findGradingByContext(authorId = author.id, taskId = oneTask.id, contestId = contest.id)
            }
            val (twenty, twentyStatements) = withStatementCount {
                repository.findGradingByContext(authorId = author.id, taskId = twentyTask.id, contestId = contest.id)
            }

            assertEquals(listOf(single.id), one.map { submission -> submission.id })
            assertEquals(listOf(singleOrder), one.single().data.judgmentOrders.ids)
            assertEquals(saved.map { submission -> submission.id }, twenty.map { submission -> submission.id })
            assertEquals(orders, twenty.map { submission -> submission.data.judgmentOrders.ids.single() })
            assertEquals(List(20) { author.id }, twenty.map { submission -> submission.data.author.id })
            assertEquals(List(20) { twentyTask.id }, twenty.map { submission -> submission.data.task.id })
            assertEquals(
                List(20) { contest.id },
                twenty.map { submission -> assertIs<SubmissionKind.Grading>(submission.data.kind).contest.id },
            )
            assertEquals(oneStatements, twentyStatements)
        }

        @Test
        fun `should find grading submissions of the author for the task in the contest ordered by creation time`() {
            val author = fixtures.student()
            val task = fixtures.task()
            val contest = fixtures.contest()
            val later = fixtures.gradingSubmission(authorId = author.id, contest = contest, task = task)
            val earlier = fixtures.gradingSubmission(authorId = author.id, contest = contest, task = task)
            setCreatedAt(later, "2030-01-02T00:00:00Z")
            setCreatedAt(earlier, "2030-01-01T00:00:00Z")
            fixtures.gradingSubmission(authorId = fixtures.student().id, contest = contest, task = task)
            fixtures.gradingSubmission(authorId = author.id, contest = contest)
            fixtures.gradingSubmission(authorId = author.id, task = task)
            fixtures.submission(author = author, task = task)

            val found = repository.findGradingByContext(authorId = author.id, taskId = task.id, contestId = contest.id)

            assertEquals(listOf(earlier.id, later.id), found.map { it.id })
            assertEquals(contest.id, assertIs<SubmissionKind.Grading>(found.first().data.kind).contest.id)
        }

        @Test
        fun `should order grading submissions by id if they were created at the same time`() {
            val author = fixtures.student()
            val task = fixtures.task()
            val contest = fixtures.contest()
            val first = fixtures.gradingSubmission(authorId = author.id, contest = contest, task = task)
            val second = fixtures.gradingSubmission(authorId = author.id, contest = contest, task = task)
            setCreatedAt(first, "2030-01-01T00:00:00Z")
            setCreatedAt(second, "2030-01-01T00:00:00Z")

            val found = repository.findGradingByContext(authorId = author.id, taskId = task.id, contestId = contest.id)

            assertEquals(listOf(first.id, second.id), found.map { it.id })
        }

        @Test
        fun `should return an empty list if the author has no grading submissions in the context`() {
            val author = fixtures.student()
            val task = fixtures.task()
            val contest = fixtures.contest()
            fixtures.gradingSubmission(contest = contest, task = task)

            val found = repository.findGradingByContext(authorId = author.id, taskId = task.id, contestId = contest.id)

            assertEquals(emptyList(), found)
        }

        @Test
        fun `should keep the fixed-role kind of a participant author`() {
            val participantId = fixtures.participant().id
            val task = fixtures.task()
            val contest = fixtures.contest()
            val saved = fixtures.gradingSubmission(authorId = participantId, contest = contest, task = task)

            val found = repository.findGradingByContext(authorId = participantId, taskId = task.id, contestId = contest.id)

            assertEquals(listOf(saved.id), found.map { it.id })
            assertIs<SingleRoleUserId>(found.single().data.author.id)
        }

        private fun setCreatedAt(submission: Submission, value: String) {
            jdbcTemplate.update(
                "update ts_submission set created_at = ? where id = ?",
                Timestamp.from(Instant.parse(value)),
                submission.id.value,
            )
        }
    }

    private fun developerSolutionTestData(version: TrikStudioVersion, submittedTaskId: Long? = null): SubmissionData {
        val author = fixtures.developer()
        val authorId = author.id
        val taskId = submittedTaskId ?: fixtures.task(author).id.value
        val solutionId = fixtures.solution().id.value
        return submissionData {
            this.author = authorId
            solution(solutionId)
            task(taskId)
            status.queued()
            kind.developerSolutionTest { trikStudioVersion = version }
        }
    }
}
