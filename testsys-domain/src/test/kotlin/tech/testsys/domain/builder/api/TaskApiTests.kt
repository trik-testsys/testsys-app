package tech.testsys.domain.builder.api

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import tech.testsys.domain.model.EntityVersion
import tech.testsys.domain.model.LazyEntity
import tech.testsys.domain.model.LazyEntityList
import tech.testsys.domain.model.group.CommunityId
import tech.testsys.domain.model.task.CommittedTaskContent
import tech.testsys.domain.model.task.ContestData
import tech.testsys.domain.model.task.DeveloperSolutionData
import tech.testsys.domain.model.task.DeveloperSolutionId
import tech.testsys.domain.model.task.ExerciseData
import tech.testsys.domain.model.task.ExerciseId
import tech.testsys.domain.model.task.FileData
import tech.testsys.domain.model.task.JudgmentOrderData
import tech.testsys.domain.model.task.JudgmentOrderId
import tech.testsys.domain.model.task.LogsData
import tech.testsys.domain.model.task.LogsId
import tech.testsys.domain.model.task.RecordingData
import tech.testsys.domain.model.task.RecordingId
import tech.testsys.domain.model.task.Score
import tech.testsys.domain.model.task.SolutionData
import tech.testsys.domain.model.task.SolutionId
import tech.testsys.domain.model.task.StatementData
import tech.testsys.domain.model.task.StatementId
import tech.testsys.domain.model.task.SubmissionData
import tech.testsys.domain.model.task.SubmissionId
import tech.testsys.domain.model.task.SubmissionKind
import tech.testsys.domain.model.task.SubmissionStatus
import tech.testsys.domain.model.task.TaskContent
import tech.testsys.domain.model.task.TaskData
import tech.testsys.domain.model.task.TaskId
import tech.testsys.domain.model.task.TestData
import tech.testsys.domain.model.task.TestId
import tech.testsys.domain.model.task.TrikStudioVersion
import tech.testsys.domain.model.task.TrikSupportedLanguage
import tech.testsys.domain.model.task.VerdictData
import tech.testsys.domain.model.task.VerdictId
import tech.testsys.domain.model.task.WipTaskContent
import tech.testsys.domain.model.user.MultipleRoleUserId
import java.time.Duration
import java.time.Instant
import java.util.UUID

class TaskApiTests {

    @Nested
    inner class SubmissionTests {

        private val origin = submission {
            id = 1
            createdAt = Instant.ofEpochSecond(1)
            version = EntityVersion(7)
            data = SubmissionData(
                author = LazyEntity(MultipleRoleUserId(10)),
                solution = LazyEntity(SolutionId(15)),
                task = LazyEntity(TaskId(9)),
                status = SubmissionStatus.InProgress,
                kind = SubmissionKind.DeveloperSolutionTest,
                judgmentOrders = LazyEntityList(listOf(JudgmentOrderId(32)))
            )
        }

        @Test
        fun `should keep all fields if withData changes nothing`() {
            val copy = origin.withData {  }

            Assertions.assertEquals(origin.id,  copy.id)
            Assertions.assertEquals(origin.createdAt, copy.createdAt)
            Assertions.assertEquals(origin.version, copy.version)
            Assertions.assertEquals(origin.data.author.id, copy.data.author.id)
            Assertions.assertEquals(origin.data.solution.id, copy.data.solution.id)
            Assertions.assertEquals(origin.data.task.id, copy.data.task.id)
            Assertions.assertEquals(origin.data.status, copy.data.status)
            Assertions.assertEquals(origin.data.kind, copy.data.kind)
            Assertions.assertEquals(origin.data.judgmentOrders.ids, copy.data.judgmentOrders.ids)
        }

        @Test
        fun `should change author if withData sets author`() {
            val copy = origin.withData { author(45) }

            Assertions.assertEquals(45L, copy.data.author.id.value)
            Assertions.assertEquals(origin.data.status, copy.data.status)
            Assertions.assertEquals(origin.data.kind, copy.data.kind)
        }
    }

    @Nested
    inner class ContestTests {

        private val origin = contest {
            id = 1
            createdAt = Instant.ofEpochSecond(1)
            version = EntityVersion(7)
            data = ContestData(
                owner = LazyEntity(MultipleRoleUserId(10)),
                name = "Original Contest",
                description = "Original description",
                tasks = LazyEntityList(listOf(TaskId(20))),
                startsAt = Instant.ofEpochSecond(5000),
                contestDuration = Duration.ofHours(2),
                attemptDuration = Duration.ofMinutes(30),
                trikStudioVersion = TrikStudioVersion("3.0.0"),
                sharedTo = LazyEntityList(listOf(CommunityId(50)))
            )
        }

        @Test
        fun `should keep all fields if withData changes nothing`() {
            val copy = origin.withData { }

            Assertions.assertEquals(origin.id, copy.id)
            Assertions.assertEquals(origin.createdAt, copy.createdAt)
            Assertions.assertEquals(origin.version, copy.version)
            Assertions.assertEquals(origin.data.owner.id, copy.data.owner.id)
            Assertions.assertEquals(origin.data.name, copy.data.name)
            Assertions.assertEquals(origin.data.description, copy.data.description)
            Assertions.assertEquals(origin.data.tasks.ids, copy.data.tasks.ids)
            Assertions.assertEquals(origin.data.startsAt, copy.data.startsAt)
            Assertions.assertEquals(origin.data.contestDuration, copy.data.contestDuration)
            Assertions.assertEquals(origin.data.attemptDuration, copy.data.attemptDuration)
            Assertions.assertEquals(origin.data.trikStudioVersion, copy.data.trikStudioVersion)
            Assertions.assertEquals(origin.data.sharedTo.ids, copy.data.sharedTo.ids)
        }

        @Test
        fun `should change name if withData sets name`() {
            val copy = origin.withData { name = "Updated Contest" }

            Assertions.assertEquals("Updated Contest", copy.data.name)
        }
    }

    @Nested
    inner class DeveloperSolutionTests {

        private val versionBucket = UUID.randomUUID()
        private val origin = developerSolution {
            id = 1
            createdAt = Instant.ofEpochSecond(1)
            version = EntityVersion(7)
            data = DeveloperSolutionData(
                name = "Reference",
                description = "Reference solution",
                solution = LazyEntity(SolutionId(10)),
                expectedScore = Score(100),
                versionBucket = versionBucket,
            )
        }

        @Test
        fun `should keep all fields if withData changes nothing`() {
            val copy = origin.withData { }

            Assertions.assertEquals(origin.id, copy.id)
            Assertions.assertEquals(origin.createdAt, copy.createdAt)
            Assertions.assertEquals(origin.version, copy.version)
            Assertions.assertEquals(origin.data.name, copy.data.name)
            Assertions.assertEquals(origin.data.description, copy.data.description)
            Assertions.assertEquals(origin.data.solution.id, copy.data.solution.id)
            Assertions.assertEquals(origin.data.expectedScore, copy.data.expectedScore)
            Assertions.assertEquals(origin.data.versionBucket, copy.data.versionBucket)
        }

        @Test
        fun `should change solution if withData sets solution`() {
            val copy = origin.withData { solution(99) }

            Assertions.assertEquals(99L, copy.data.solution.id.value)
        }
    }

    @Nested
    inner class ExerciseTests {

        private val versionBucket = UUID.randomUUID()
        private val origin = exercise {
            id = 1
            createdAt = Instant.ofEpochSecond(1)
            version = EntityVersion(7)
            data = ExerciseData(
                name = "Exercise",
                description = "Exercise description",
                file = FileData("exercise.qrs", byteArrayOf(1, 2, 3)),
                language = TrikSupportedLanguage.Python,
                versionBucket = versionBucket,
            )
        }

        @Test
        fun `should keep all fields if withData changes nothing`() {
            val copy = origin.withData { }

            Assertions.assertEquals(origin.id, copy.id)
            Assertions.assertEquals(origin.createdAt, copy.createdAt)
            Assertions.assertEquals(origin.version, copy.version)
            Assertions.assertEquals(origin.data.name, copy.data.name)
            Assertions.assertEquals(origin.data.description, copy.data.description)
            Assertions.assertEquals(origin.data.file.uploadedFilename, copy.data.file.uploadedFilename)
            Assertions.assertArrayEquals(origin.data.file.content, copy.data.file.content)
            Assertions.assertEquals(origin.data.language, copy.data.language)
            Assertions.assertEquals(origin.data.versionBucket, copy.data.versionBucket)
        }

        @Test
        fun `should change language if withData sets language`() {
            val copy = origin.withData { language.javaScript() }

            Assertions.assertEquals(TrikSupportedLanguage.JavaScript, copy.data.language)
            Assertions.assertEquals(origin.data.file.uploadedFilename, copy.data.file.uploadedFilename)
        }
    }

    @Nested
    inner class JudgmentOrderTests {

        private val origin = judgmentOrder {
            id = 1
            createdAt = Instant.ofEpochSecond(1)
            version = EntityVersion(7)
            data = JudgmentOrderData(
                judge = LazyEntity(MultipleRoleUserId(10)),
                verdict = LazyEntity(VerdictId(20)),
                reason = "auto-grade",
            )
        }

        @Test
        fun `should keep all fields if withData changes nothing`() {
            val copy = origin.withData { }

            Assertions.assertEquals(origin.id, copy.id)
            Assertions.assertEquals(origin.createdAt, copy.createdAt)
            Assertions.assertEquals(origin.version, copy.version)
            Assertions.assertEquals(origin.data.judge.id, copy.data.judge.id)
            Assertions.assertEquals(origin.data.verdict.id, copy.data.verdict.id)
            Assertions.assertEquals(origin.data.reason, copy.data.reason)
        }

        @Test
        fun `should change judge if withData sets judge`() {
            val copy = origin.withData { judge(99) }

            Assertions.assertEquals(99L, copy.data.judge.id.value)
        }
    }

    @Nested
    inner class LogsTests {

        private val origin = logs {
            id = 1
            createdAt = Instant.ofEpochSecond(1)
            version = EntityVersion(7)
            data = LogsData(
                file = FileData("grading.log", byteArrayOf(1, 2)),
            )
        }

        @Test
        fun `should keep all fields if withData changes nothing`() {
            val copy = origin.withData { }

            Assertions.assertEquals(origin.id, copy.id)
            Assertions.assertEquals(origin.createdAt, copy.createdAt)
            Assertions.assertEquals(origin.version, copy.version)
            Assertions.assertEquals(origin.data.file.uploadedFilename, copy.data.file.uploadedFilename)
            Assertions.assertArrayEquals(origin.data.file.content, copy.data.file.content)
        }

        @Test
        fun `should change file if withData sets file`() {
            val copy = origin.withData { file("updated.log", byteArrayOf(3, 4)) }

            Assertions.assertEquals("updated.log", copy.data.file.uploadedFilename)
            Assertions.assertArrayEquals(byteArrayOf(3, 4), copy.data.file.content)
        }
    }

    @Nested
    inner class RecordingTests {

        private val origin = recording {
            id = 1
            createdAt = Instant.ofEpochSecond(1)
            version = EntityVersion(7)
            data = RecordingData(
                file = FileData("grading.mp4", byteArrayOf(5, 6)),
            )
        }

        @Test
        fun `should keep all fields if withData changes nothing`() {
            val copy = origin.withData { }

            Assertions.assertEquals(origin.id, copy.id)
            Assertions.assertEquals(origin.createdAt, copy.createdAt)
            Assertions.assertEquals(origin.version, copy.version)
            Assertions.assertEquals(origin.data.file.uploadedFilename, copy.data.file.uploadedFilename)
            Assertions.assertArrayEquals(origin.data.file.content, copy.data.file.content)
        }

        @Test
        fun `should change file if withData sets file`() {
            val copy = origin.withData { file("updated.mp4", byteArrayOf(7, 8)) }

            Assertions.assertEquals("updated.mp4", copy.data.file.uploadedFilename)
            Assertions.assertArrayEquals(byteArrayOf(7, 8), copy.data.file.content)
        }
    }

    @Nested
    inner class SolutionTests {

        private val versionBucket = UUID.randomUUID()
        private val origin = solution {
            id = 1
            createdAt = Instant.ofEpochSecond(1)
            version = EntityVersion(7)
            data = SolutionData(
                file = FileData("solution.py", byteArrayOf(4, 5, 6)),
                language = TrikSupportedLanguage.Python,
                versionBucket = versionBucket,
            )
        }

        @Test
        fun `should keep all fields if withData changes nothing`() {
            val copy = origin.withData { }

            Assertions.assertEquals(origin.id, copy.id)
            Assertions.assertEquals(origin.createdAt, copy.createdAt)
            Assertions.assertEquals(origin.version, copy.version)
            Assertions.assertEquals(origin.data.file.uploadedFilename, copy.data.file.uploadedFilename)
            Assertions.assertArrayEquals(origin.data.file.content, copy.data.file.content)
            Assertions.assertEquals(origin.data.language, copy.data.language)
            Assertions.assertEquals(origin.data.versionBucket, copy.data.versionBucket)
        }

        @Test
        fun `should change language if withData sets language`() {
            val copy = origin.withData { language.visualLanguage() }

            Assertions.assertEquals(TrikSupportedLanguage.VisualLanguage, copy.data.language)
            Assertions.assertEquals(origin.data.file.uploadedFilename, copy.data.file.uploadedFilename)
        }
    }

    @Nested
    inner class StatementTests {

        private val versionBucket = UUID.randomUUID()
        private val origin = statement {
            id = 1
            createdAt = Instant.ofEpochSecond(1)
            version = EntityVersion(7)
            data = StatementData(
                file = FileData("statement.pdf", byteArrayOf(7, 8, 9)),
                name = "Statement",
                description = "Statement description",
                versionBucket = versionBucket,
            )
        }

        @Test
        fun `should keep all fields if withData changes nothing`() {
            val copy = origin.withData { }

            Assertions.assertEquals(origin.id, copy.id)
            Assertions.assertEquals(origin.createdAt, copy.createdAt)
            Assertions.assertEquals(origin.version, copy.version)
            Assertions.assertEquals(origin.data.file.uploadedFilename, copy.data.file.uploadedFilename)
            Assertions.assertArrayEquals(origin.data.file.content, copy.data.file.content)
            Assertions.assertEquals(origin.data.name, copy.data.name)
            Assertions.assertEquals(origin.data.description, copy.data.description)
            Assertions.assertEquals(origin.data.versionBucket, copy.data.versionBucket)
        }

        @Test
        fun `should change file if withData sets file`() {
            val copy = origin.withData { file("updated.pdf", byteArrayOf(10, 11)) }

            Assertions.assertEquals("updated.pdf", copy.data.file.uploadedFilename)
        }
    }

    @Nested
    inner class TestTests {

        private val versionBucket = UUID.randomUUID()
        private val origin = test {
            id = 1
            createdAt = Instant.ofEpochSecond(1)
            version = EntityVersion(7)
            data = TestData(
                file = FileData("test.xml", byteArrayOf(1, 2)),
                name = "Polygon",
                description = "Polygon description",
                versionBucket = versionBucket,
            )
        }

        @Test
        fun `should keep all fields if withData changes nothing`() {
            val copy = origin.withData { }

            Assertions.assertEquals(origin.id, copy.id)
            Assertions.assertEquals(origin.createdAt, copy.createdAt)
            Assertions.assertEquals(origin.version, copy.version)
            Assertions.assertEquals(origin.data.file.uploadedFilename, copy.data.file.uploadedFilename)
            Assertions.assertArrayEquals(origin.data.file.content, copy.data.file.content)
            Assertions.assertEquals(origin.data.name, copy.data.name)
            Assertions.assertEquals(origin.data.description, copy.data.description)
            Assertions.assertEquals(origin.data.versionBucket, copy.data.versionBucket)
        }

        @Test
        fun `should change file if withData sets file`() {
            val copy = origin.withData { file("updated.xml", byteArrayOf(3, 4)) }

            Assertions.assertEquals("updated.xml", copy.data.file.uploadedFilename)
        }
    }

    @Nested
    inner class VerdictTests {

        private val origin = verdict {
            id = 1
            createdAt = Instant.ofEpochSecond(1)
            version = EntityVersion(7)
            data = VerdictData(
                score = Score(85),
                task = LazyEntity(TaskId(10)),
                submission = LazyEntity(SubmissionId(20)),
                logs = LazyEntity(LogsId(30)),
                recording = LazyEntity(RecordingId(40)),
            )
        }

        @Test
        fun `should keep all fields if withData changes nothing`() {
            val copy = origin.withData { }

            Assertions.assertEquals(origin.id, copy.id)
            Assertions.assertEquals(origin.createdAt, copy.createdAt)
            Assertions.assertEquals(origin.version, copy.version)
            Assertions.assertEquals(origin.data.score, copy.data.score)
            Assertions.assertEquals(origin.data.task.id, copy.data.task.id)
            Assertions.assertEquals(origin.data.submission.id, copy.data.submission.id)
            Assertions.assertEquals(origin.data.logs?.id, copy.data.logs?.id)
            Assertions.assertEquals(origin.data.recording?.id, copy.data.recording?.id)
        }

        @Test
        fun `should change score if withData sets score`() {
            val copy = origin.withData { score = 50 }

            Assertions.assertEquals(Score(50), copy.data.score)
        }
    }

    @Nested
    inner class TaskNewTests {

        private val origin = task {
            id = 1
            createdAt = Instant.ofEpochSecond(1)
            version = EntityVersion(7)
            data = TaskData(
                owner = LazyEntity(MultipleRoleUserId(10)),
                name = "Task Name",
                description = "Task Description",
                sharedTo = LazyEntityList(listOf(CommunityId(60))),
                content = TaskContent.New(
                    wip = WipTaskContent(
                        tests = LazyEntityList(listOf(TestId(20))),
                        exercise = LazyEntity(ExerciseId(30)),
                        statement = LazyEntity(StatementId(40)),
                        developerSolutions = LazyEntityList(listOf(DeveloperSolutionId(50))),
                        supportedTrikStudioVersions = listOf(TrikStudioVersion("3.0.0")),
                    )
                ),
            )
        }

        @Test
        fun `should keep all fields of a New task if withData changes nothing`() {
            val copy = origin.withData { }

            Assertions.assertEquals(origin.id, copy.id)
            Assertions.assertEquals(origin.createdAt, copy.createdAt)
            Assertions.assertEquals(origin.version, copy.version)
            Assertions.assertEquals(origin.data.owner.id, copy.data.owner.id)
            Assertions.assertEquals(origin.data.name, copy.data.name)
            Assertions.assertEquals(origin.data.description, copy.data.description)
            Assertions.assertEquals(origin.data.sharedTo.ids, copy.data.sharedTo.ids)

            Assertions.assertInstanceOf(TaskContent.New::class.java, copy.data.content)
            val originWip = (origin.data.content as TaskContent.New).wip
            val copyWip = (copy.data.content as TaskContent.New).wip
            Assertions.assertEquals(originWip.tests.ids, copyWip.tests.ids)
            Assertions.assertEquals(originWip.exercise?.id, copyWip.exercise?.id)
            Assertions.assertEquals(originWip.statement?.id, copyWip.statement?.id)
            Assertions.assertEquals(originWip.developerSolutions.ids, copyWip.developerSolutions.ids)
            Assertions.assertEquals(originWip.supportedTrikStudioVersions, copyWip.supportedTrikStudioVersions)
        }

        @Test
        fun `should change name of a New task if withData sets name`() {
            val copy = origin.withData {
                name = "Updated Name"
            }

            Assertions.assertEquals("Updated Name", copy.data.name)
            Assertions.assertEquals(10L, copy.data.owner.id.value)
            Assertions.assertEquals("Task Description", copy.data.description)
        }

        @Test
        fun `should change content from New to Committed if withData commits it`() {
            val copy = origin.withData {
                content.committed {
                    exercise(30)
                    statement(40)
                }
            }

            Assertions.assertInstanceOf(TaskContent.Committed::class.java, copy.data.content)
            val copyCommitted = (copy.data.content as TaskContent.Committed).lastCommitted
            Assertions.assertEquals(30L, copyCommitted.exercise.id.value)
            Assertions.assertEquals(40L, copyCommitted.statement.id.value)
        }
    }

    @Nested
    inner class TaskCommittedTests {

        private val origin = task {
            id = 2
            createdAt = Instant.ofEpochSecond(1)
            version = EntityVersion(7)
            data = TaskData(
                owner = LazyEntity(MultipleRoleUserId(10)),
                name = "Committed Task",
                description = "Committed Description",
                sharedTo = LazyEntityList(listOf(CommunityId(60))),
                content = TaskContent.Committed(
                    lastCommitted = CommittedTaskContent(
                        tests = LazyEntityList(listOf(TestId(20))),
                        exercise = LazyEntity(ExerciseId(30)),
                        statement = LazyEntity(StatementId(40)),
                        developerSolutions = LazyEntityList(listOf(DeveloperSolutionId(50))),
                        supportedTrikStudioVersions = listOf(TrikStudioVersion("3.0.0")),
                    )
                ),
            )
        }

        @Test
        fun `should keep all fields of a Committed task if withData changes nothing`() {
            val copy = origin.withData { }

            Assertions.assertEquals(origin.id, copy.id)
            Assertions.assertEquals(origin.createdAt, copy.createdAt)
            Assertions.assertEquals(origin.version, copy.version)
            Assertions.assertEquals(origin.data.owner.id, copy.data.owner.id)
            Assertions.assertEquals(origin.data.name, copy.data.name)
            Assertions.assertEquals(origin.data.description, copy.data.description)
            Assertions.assertEquals(origin.data.sharedTo.ids, copy.data.sharedTo.ids)

            Assertions.assertInstanceOf(TaskContent.Committed::class.java, copy.data.content)
            val originCommitted = (origin.data.content as TaskContent.Committed).lastCommitted
            val copyCommitted = (copy.data.content as TaskContent.Committed).lastCommitted
            Assertions.assertEquals(originCommitted.tests.ids, copyCommitted.tests.ids)
            Assertions.assertEquals(originCommitted.exercise.id, copyCommitted.exercise.id)
            Assertions.assertEquals(originCommitted.statement.id, copyCommitted.statement.id)
            Assertions.assertEquals(originCommitted.developerSolutions.ids, copyCommitted.developerSolutions.ids)
            Assertions.assertEquals(originCommitted.supportedTrikStudioVersions, copyCommitted.supportedTrikStudioVersions)
        }

        @Test
        fun `should change name of a Committed task if withData sets name`() {
            val copy = origin.withData { name = "Updated Name" }

            Assertions.assertEquals("Updated Name", copy.data.name)
            Assertions.assertEquals(10L, copy.data.owner.id.value)
            Assertions.assertEquals("Committed Description", copy.data.description)
        }

        @Test
        fun `should change committed content if withData sets it`() {
            val copy = origin.withData {
                content.committed { exercise(99) }
            }

            Assertions.assertInstanceOf(TaskContent.Committed::class.java, copy.data.content)
            val copyCommitted = (copy.data.content as TaskContent.Committed).lastCommitted
            Assertions.assertEquals(99L, copyCommitted.exercise.id.value)
            Assertions.assertEquals(40L, copyCommitted.statement.id.value)
        }
    }

    @Nested
    inner class TaskUncommittedTests {

        private val origin = task {
            id = 3
            createdAt = Instant.ofEpochSecond(1)
            version = EntityVersion(7)
            data = TaskData(
                owner = LazyEntity(MultipleRoleUserId(10)),
                name = "Uncommitted Task",
                description = "Uncommitted Description",
                sharedTo = LazyEntityList(emptyList()),
                content = TaskContent.Uncommitted(
                    wip = WipTaskContent(
                        tests = LazyEntityList(listOf(TestId(20))),
                        exercise = LazyEntity(ExerciseId(30)),
                        statement = LazyEntity(StatementId(40)),
                        developerSolutions = LazyEntityList(emptyList()),
                        supportedTrikStudioVersions = listOf(TrikStudioVersion("3.0.0")),
                    ),
                    lastCommitted = CommittedTaskContent(
                        tests = LazyEntityList(listOf(TestId(21))),
                        exercise = LazyEntity(ExerciseId(31)),
                        statement = LazyEntity(StatementId(41)),
                        developerSolutions = LazyEntityList(listOf(DeveloperSolutionId(51))),
                        supportedTrikStudioVersions = listOf(TrikStudioVersion("2.0.0")),
                    ),
                ),
            )
        }

        @Test
        fun `should keep all fields of an Uncommitted task if withData changes nothing`() {
            val copy = origin.withData { }

            Assertions.assertEquals(origin.id, copy.id)
            Assertions.assertEquals(origin.createdAt, copy.createdAt)
            Assertions.assertEquals(origin.version, copy.version)
            Assertions.assertEquals(origin.data.owner.id, copy.data.owner.id)
            Assertions.assertEquals(origin.data.name, copy.data.name)
            Assertions.assertEquals(origin.data.description, copy.data.description)

            Assertions.assertInstanceOf(TaskContent.Uncommitted::class.java, copy.data.content)
            val originContent = origin.data.content as TaskContent.Uncommitted
            val copyContent = copy.data.content as TaskContent.Uncommitted

            Assertions.assertEquals(originContent.wip.tests.ids, copyContent.wip.tests.ids)
            Assertions.assertEquals(originContent.wip.exercise?.id, copyContent.wip.exercise?.id)
            Assertions.assertEquals(originContent.wip.statement?.id, copyContent.wip.statement?.id)

            Assertions.assertEquals(originContent.lastCommitted.tests.ids, copyContent.lastCommitted.tests.ids)
            Assertions.assertEquals(originContent.lastCommitted.exercise.id, copyContent.lastCommitted.exercise.id)
            Assertions.assertEquals(originContent.lastCommitted.statement.id, copyContent.lastCommitted.statement.id)
        }

        @Test
        fun `should change uncommitted content if withData sets it`() {
            val copy = origin.withData {
                content.uncommitted(
                    wipBuilder = { exercise(99) },
                    lastCommittedBuilder = { exercise(98) },
                )
            }

            val copyContent = copy.data.content as TaskContent.Uncommitted
            Assertions.assertEquals(99L, copyContent.wip.exercise?.id?.value)
            Assertions.assertEquals(98L, copyContent.lastCommitted.exercise.id.value)
        }

        @Test
        fun `should change content from Uncommitted to New if withData resets it`() {
            val copy = origin.withData {
                content.new { }
            }

            Assertions.assertInstanceOf(TaskContent.New::class.java, copy.data.content)
        }
    }
}