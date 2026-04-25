package tech.testsys.domain.builder.api

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import tech.testsys.domain.model.LazyEntity
import tech.testsys.domain.model.LazyEntityList
import tech.testsys.domain.model.group.Class
import tech.testsys.domain.model.group.ClassData
import tech.testsys.domain.model.group.ClassId
import tech.testsys.domain.model.group.Community
import tech.testsys.domain.model.group.CommunityData
import tech.testsys.domain.model.group.CommunityId
import tech.testsys.domain.model.group.Competition
import tech.testsys.domain.model.group.CompetitionData
import tech.testsys.domain.model.group.CompetitionId
import tech.testsys.domain.model.task.CommitedTaskContent
import tech.testsys.domain.model.task.Contest
import tech.testsys.domain.model.task.ContestData
import tech.testsys.domain.model.task.ContestId
import tech.testsys.domain.model.task.DeveloperSolution
import tech.testsys.domain.model.task.DeveloperSolutionData
import tech.testsys.domain.model.task.DeveloperSolutionId
import tech.testsys.domain.model.task.Exercise
import tech.testsys.domain.model.task.ExerciseData
import tech.testsys.domain.model.task.ExerciseId
import tech.testsys.domain.model.task.FileData
import tech.testsys.domain.model.task.JudgmentOrder
import tech.testsys.domain.model.task.JudgmentOrderData
import tech.testsys.domain.model.task.JudgmentOrderId
import tech.testsys.domain.model.task.Score
import tech.testsys.domain.model.task.Solution
import tech.testsys.domain.model.task.SolutionData
import tech.testsys.domain.model.task.SolutionId
import tech.testsys.domain.model.task.Statement
import tech.testsys.domain.model.task.StatementData
import tech.testsys.domain.model.task.StatementId
import tech.testsys.domain.model.task.Submission
import tech.testsys.domain.model.task.SubmissionData
import tech.testsys.domain.model.task.SubmissionId
import tech.testsys.domain.model.task.SubmissionKind
import tech.testsys.domain.model.task.SubmissionStatus
import tech.testsys.domain.model.task.Task
import tech.testsys.domain.model.task.TaskContent
import tech.testsys.domain.model.task.TaskData
import tech.testsys.domain.model.task.TaskId
import tech.testsys.domain.model.task.TestData
import tech.testsys.domain.model.task.TestId
import tech.testsys.domain.model.task.TrikStudioVersion
import tech.testsys.domain.model.task.TrikSupportedLanguage
import tech.testsys.domain.model.task.Verdict
import tech.testsys.domain.model.task.VerdictData
import tech.testsys.domain.model.task.VerdictId
import tech.testsys.domain.model.task.WipTaskContent
import tech.testsys.domain.model.user.Administrator
import tech.testsys.domain.model.user.Developer
import tech.testsys.domain.model.user.DeveloperData
import tech.testsys.domain.model.user.MultipleRoleUser
import tech.testsys.domain.model.user.MultipleRoleUserData
import tech.testsys.domain.model.user.MultipleRoleUserId
import tech.testsys.domain.model.user.Observer
import tech.testsys.domain.model.user.ObserverData
import tech.testsys.domain.model.user.Participant
import tech.testsys.domain.model.user.ParticipantData
import tech.testsys.domain.model.user.SingleRoleUserId
import tech.testsys.domain.model.user.Supervisor
import tech.testsys.domain.model.user.SupervisorData
import java.time.Duration
import java.time.Instant
import java.util.UUID

class BuilderApiTests {

    @Nested
    inner class SubmissionTests {

        private val origin = Submission(
            id = SubmissionId(1),
            createdAt = Instant.now(),
            data = SubmissionData(
                author = LazyEntity(MultipleRoleUserId(10)),
                solution = LazyEntity(SolutionId(15)),
                task = LazyEntity(TaskId(9)),
                status = SubmissionStatus.InProgress,
                kind = SubmissionKind.DeveloperSolutionTest,
                judgmentOrders = LazyEntityList(listOf(JudgmentOrderId(32)))
            )
        )

        @Test
        fun `withData should keep all unmodified fields`() {
            val copy = origin.withData {  }

            Assertions.assertEquals(origin.id,  copy.id)
            Assertions.assertEquals(origin.createdAt, copy.createdAt)
            Assertions.assertEquals(origin.data.author.id, copy.data.author.id)
            Assertions.assertEquals(origin.data.solution.id, copy.data.solution.id)
            Assertions.assertEquals(origin.data.task.id, copy.data.task.id)
            Assertions.assertEquals(origin.data.status, copy.data.status)
            Assertions.assertEquals(origin.data.kind, copy.data.kind)
            Assertions.assertEquals(
                origin.data.judgmentOrders.ids.size,
                copy.data.judgmentOrders.ids.size
            )
            origin.data.judgmentOrders.ids.zip(copy.data.judgmentOrders.ids).forEach { (originId, copyId) ->
                Assertions.assertEquals(originId.value, copyId.value)
            }
        }

        @Test
        fun `withData should change modified fields`() {
            val copy = origin.withData { author(45) }

            Assertions.assertEquals(45L, copy.data.author.id.value)
            Assertions.assertEquals(origin.data.status, copy.data.status)
            Assertions.assertEquals(origin.data.kind, copy.data.kind)
        }
    }

    @Nested
    inner class ClassTests {

        private val origin = Class(
            id = ClassId(1),
            createdAt = Instant.now(),
            data = ClassData(
                owner = LazyEntity(MultipleRoleUserId(10)),
                name = "Original Class",
                description = "Original description",
                students = LazyEntityList(listOf(MultipleRoleUserId(20), MultipleRoleUserId(30))),
                contests = LazyEntityList(listOf(ContestId(40)))
            )
        )

        @Test
        fun `withData should keep all unmodified fields`() {
            val copy = origin.withData { }

            Assertions.assertEquals(origin.id, copy.id)
            Assertions.assertEquals(origin.createdAt, copy.createdAt)
            Assertions.assertEquals(origin.data.owner.id, copy.data.owner.id)
            Assertions.assertEquals(origin.data.name, copy.data.name)
            Assertions.assertEquals(origin.data.description, copy.data.description)
            Assertions.assertEquals(origin.data.students.ids.size, copy.data.students.ids.size)
            Assertions.assertEquals(origin.data.contests.ids.size, copy.data.contests.ids.size)
        }

        @Test
        fun `withData should change modified fields`() {
            val copy = origin.withData { owner(99) }

            Assertions.assertEquals(99L, copy.data.owner.id.value)
        }
    }

    @Nested
    inner class CommunityTests {

        private val origin = Community(
            id = CommunityId(1),
            createdAt = Instant.now(),
            data = CommunityData(
                owner = LazyEntity(MultipleRoleUserId(10)),
                name = "Original Community",
                description = "Original description",
            )
        )

        @Test
        fun `withData should keep all unmodified fields`() {
            val copy = origin.withData { }

            Assertions.assertEquals(origin.id, copy.id)
            Assertions.assertEquals(origin.createdAt, copy.createdAt)
            Assertions.assertEquals(origin.data.owner.id, copy.data.owner.id)
            Assertions.assertEquals(origin.data.name, copy.data.name)
            Assertions.assertEquals(origin.data.description, copy.data.description)
        }

        @Test
        fun `withData should change modified fields`() {
            val copy = origin.withData { owner(99) }

            Assertions.assertEquals(99L, copy.data.owner.id.value)
        }
    }

    @Nested
    inner class CompetitionTests {

        private val origin = Competition(
            id = CompetitionId(1),
            createdAt = Instant.now(),
            data = CompetitionData(
                owner = LazyEntity(MultipleRoleUserId(10)),
                name = "Original Competition",
                description = "Original description",
                participants = LazyEntityList(listOf(SingleRoleUserId(20))),
                contests = LazyEntityList(listOf(ContestId(30)))
            )
        )

        @Test
        fun `withData should keep all unmodified fields`() {
            val copy = origin.withData { }

            Assertions.assertEquals(origin.id, copy.id)
            Assertions.assertEquals(origin.createdAt, copy.createdAt)
            Assertions.assertEquals(origin.data.owner.id, copy.data.owner.id)
            Assertions.assertEquals(origin.data.name, copy.data.name)
            Assertions.assertEquals(origin.data.description, copy.data.description)
            Assertions.assertEquals(origin.data.participants.ids.size, copy.data.participants.ids.size)
            Assertions.assertEquals(origin.data.contests.ids.size, copy.data.contests.ids.size)
        }

        @Test
        fun `withData should change modified fields`() {
            val copy = origin.withData { owner(99) }

            Assertions.assertEquals(99L, copy.data.owner.id.value)
        }
    }

    @Nested
    inner class ContestTests {

        private val origin = Contest(
            id = ContestId(1),
            createdAt = Instant.now(),
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
        )

        @Test
        fun `withData should keep all unmodified fields`() {
            val copy = origin.withData { }

            Assertions.assertEquals(origin.id, copy.id)
            Assertions.assertEquals(origin.createdAt, copy.createdAt)
            Assertions.assertEquals(origin.data.owner.id, copy.data.owner.id)
            Assertions.assertEquals(origin.data.name, copy.data.name)
            Assertions.assertEquals(origin.data.description, copy.data.description)
            Assertions.assertEquals(origin.data.tasks.ids.size, copy.data.tasks.ids.size)
            Assertions.assertEquals(origin.data.startsAt, copy.data.startsAt)
            Assertions.assertEquals(origin.data.contestDuration, copy.data.contestDuration)
            Assertions.assertEquals(origin.data.attemptDuration, copy.data.attemptDuration)
            Assertions.assertEquals(origin.data.trikStudioVersion, copy.data.trikStudioVersion)
            Assertions.assertEquals(origin.data.sharedTo.ids.size, copy.data.sharedTo.ids.size)
        }

        @Test
        fun `withData should change modified fields`() {
            val copy = origin.withData { name = "Updated Contest" }

            Assertions.assertEquals("Updated Contest", copy.data.name)
        }
    }

    @Nested
    inner class DeveloperSolutionTests {

        private val versionBucket = UUID.randomUUID()
        private val origin = DeveloperSolution(
            id = DeveloperSolutionId(1),
            createdAt = Instant.now(),
            data = DeveloperSolutionData(
                name = "Reference",
                description = "Reference solution",
                solution = LazyEntity(SolutionId(10)),
                expectedScore = Score(100),
                versionBucket = versionBucket,
            )
        )

        @Test
        fun `withData should keep all unmodified fields`() {
            val copy = origin.withData { }

            Assertions.assertEquals(origin.id, copy.id)
            Assertions.assertEquals(origin.createdAt, copy.createdAt)
            Assertions.assertEquals(origin.data.name, copy.data.name)
            Assertions.assertEquals(origin.data.description, copy.data.description)
            Assertions.assertEquals(origin.data.solution.id, copy.data.solution.id)
            Assertions.assertEquals(origin.data.expectedScore, copy.data.expectedScore)
            Assertions.assertEquals(origin.data.versionBucket, copy.data.versionBucket)
        }

        @Test
        fun `withData should change modified fields`() {
            val copy = origin.withData { solution(99) }

            Assertions.assertEquals(99L, copy.data.solution.id.value)
        }
    }

    @Nested
    inner class ExerciseTests {

        private val versionBucket = UUID.randomUUID()
        private val origin = Exercise(
            id = ExerciseId(1),
            createdAt = Instant.now(),
            data = ExerciseData(
                name = "Exercise",
                description = "Exercise description",
                file = FileData("exercise.qrs", byteArrayOf(1, 2, 3)),
                language = TrikSupportedLanguage.Python,
                versionBucket = versionBucket,
            )
        )

        @Test
        fun `withData should keep all unmodified fields`() {
            val copy = origin.withData { }

            Assertions.assertEquals(origin.id, copy.id)
            Assertions.assertEquals(origin.createdAt, copy.createdAt)
            Assertions.assertEquals(origin.data.name, copy.data.name)
            Assertions.assertEquals(origin.data.description, copy.data.description)
            Assertions.assertEquals(origin.data.file.uploadedFilename, copy.data.file.uploadedFilename)
            Assertions.assertArrayEquals(origin.data.file.content, copy.data.file.content)
            Assertions.assertEquals(origin.data.language, copy.data.language)
            Assertions.assertEquals(origin.data.versionBucket, copy.data.versionBucket)
        }

        @Test
        fun `withData should change modified fields`() {
            val copy = origin.withData { language.javaScript() }

            Assertions.assertEquals(TrikSupportedLanguage.JavaScript, copy.data.language)
            Assertions.assertEquals(origin.data.file.uploadedFilename, copy.data.file.uploadedFilename)
        }
    }

    @Nested
    inner class JudgmentOrderTests {

        private val origin = JudgmentOrder(
            id = JudgmentOrderId(1),
            createdAt = Instant.now(),
            data = JudgmentOrderData(
                judge = LazyEntity(MultipleRoleUserId(10)),
                verdict = LazyEntity(VerdictId(20)),
                reason = "auto-grade",
            )
        )

        @Test
        fun `withData should keep all unmodified fields`() {
            val copy = origin.withData { }

            Assertions.assertEquals(origin.id, copy.id)
            Assertions.assertEquals(origin.createdAt, copy.createdAt)
            Assertions.assertEquals(origin.data.judge.id, copy.data.judge.id)
            Assertions.assertEquals(origin.data.verdict.id, copy.data.verdict.id)
            Assertions.assertEquals(origin.data.reason, copy.data.reason)
        }

        @Test
        fun `withData should change modified fields`() {
            val copy = origin.withData { judge(99) }

            Assertions.assertEquals(99L, copy.data.judge.id.value)
        }
    }

    @Nested
    inner class SolutionTests {

        private val versionBucket = UUID.randomUUID()
        private val origin = Solution(
            id = SolutionId(1),
            createdAt = Instant.now(),
            data = SolutionData(
                file = FileData("solution.py", byteArrayOf(4, 5, 6)),
                language = TrikSupportedLanguage.Python,
                name = "Solution",
                description = "Solution description",
                versionBucket = versionBucket,
            )
        )

        @Test
        fun `withData should keep all unmodified fields`() {
            val copy = origin.withData { }

            Assertions.assertEquals(origin.id, copy.id)
            Assertions.assertEquals(origin.createdAt, copy.createdAt)
            Assertions.assertEquals(origin.data.file.uploadedFilename, copy.data.file.uploadedFilename)
            Assertions.assertArrayEquals(origin.data.file.content, copy.data.file.content)
            Assertions.assertEquals(origin.data.language, copy.data.language)
            Assertions.assertEquals(origin.data.name, copy.data.name)
            Assertions.assertEquals(origin.data.description, copy.data.description)
            Assertions.assertEquals(origin.data.versionBucket, copy.data.versionBucket)
        }

        @Test
        fun `withData should change modified fields`() {
            val copy = origin.withData { language.visualLanguage() }

            Assertions.assertEquals(TrikSupportedLanguage.VisualLanguage, copy.data.language)
            Assertions.assertEquals(origin.data.file.uploadedFilename, copy.data.file.uploadedFilename)
        }
    }

    @Nested
    inner class StatementTests {

        private val versionBucket = UUID.randomUUID()
        private val origin = Statement(
            id = StatementId(1),
            createdAt = Instant.now(),
            data = StatementData(
                file = FileData("statement.pdf", byteArrayOf(7, 8, 9)),
                name = "Statement",
                description = "Statement description",
                versionBucket = versionBucket,
            )
        )

        @Test
        fun `withData should keep all unmodified fields`() {
            val copy = origin.withData { }

            Assertions.assertEquals(origin.id, copy.id)
            Assertions.assertEquals(origin.createdAt, copy.createdAt)
            Assertions.assertEquals(origin.data.file.uploadedFilename, copy.data.file.uploadedFilename)
            Assertions.assertArrayEquals(origin.data.file.content, copy.data.file.content)
            Assertions.assertEquals(origin.data.name, copy.data.name)
            Assertions.assertEquals(origin.data.description, copy.data.description)
            Assertions.assertEquals(origin.data.versionBucket, copy.data.versionBucket)
        }

        @Test
        fun `withData should change modified fields`() {
            val copy = origin.withData { file("updated.pdf", byteArrayOf(10, 11)) }

            Assertions.assertEquals("updated.pdf", copy.data.file.uploadedFilename)
        }
    }

    @Nested
    inner class TestTests {

        private val versionBucket = UUID.randomUUID()
        private val origin = tech.testsys.domain.model.task.Test(
            id = TestId(1),
            createdAt = Instant.now(),
            data = TestData(
                file = FileData("test.xml", byteArrayOf(1, 2)),
                name = "Polygon",
                description = "Polygon description",
                versionBucket = versionBucket,
            )
        )

        @Test
        fun `withData should keep all unmodified fields`() {
            val copy = origin.withData { }

            Assertions.assertEquals(origin.id, copy.id)
            Assertions.assertEquals(origin.createdAt, copy.createdAt)
            Assertions.assertEquals(origin.data.file.uploadedFilename, copy.data.file.uploadedFilename)
            Assertions.assertArrayEquals(origin.data.file.content, copy.data.file.content)
            Assertions.assertEquals(origin.data.name, copy.data.name)
            Assertions.assertEquals(origin.data.description, copy.data.description)
            Assertions.assertEquals(origin.data.versionBucket, copy.data.versionBucket)
        }

        @Test
        fun `withData should change modified fields`() {
            val copy = origin.withData { file("updated.xml", byteArrayOf(3, 4)) }

            Assertions.assertEquals("updated.xml", copy.data.file.uploadedFilename)
        }
    }

    @Nested
    inner class VerdictTests {

        private val origin = Verdict(
            id = VerdictId(1),
            createdAt = Instant.now(),
            data = VerdictData(
                score = Score(85),
                task = LazyEntity(TaskId(10)),
                submission = LazyEntity(SubmissionId(20))
            )
        )

        @Test
        fun `withData should keep all unmodified fields`() {
            val copy = origin.withData { }

            Assertions.assertEquals(origin.id, copy.id)
            Assertions.assertEquals(origin.createdAt, copy.createdAt)
            Assertions.assertEquals(origin.data.score, copy.data.score)
            Assertions.assertEquals(origin.data.task.id, copy.data.task.id)
            Assertions.assertEquals(origin.data.submission.id, copy.data.submission.id)
        }

        @Test
        fun `withData should change modified fields`() {
            val copy = origin.withData { score = 50 }

            Assertions.assertEquals(Score(50), copy.data.score)
        }
    }

    @Nested
    inner class TaskNewTests {

        private val origin = Task(
            id = TaskId(1),
            createdAt = Instant.now(),
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
        )

        @Test
        fun `withData should keep all unmodified fields for New`() {
            val copy = origin.withData { }

            Assertions.assertEquals(origin.id, copy.id)
            Assertions.assertEquals(origin.createdAt, copy.createdAt)
            Assertions.assertEquals(origin.data.owner.id, copy.data.owner.id)
            Assertions.assertEquals(origin.data.name, copy.data.name)
            Assertions.assertEquals(origin.data.description, copy.data.description)
            Assertions.assertEquals(origin.data.sharedTo.ids.size, copy.data.sharedTo.ids.size)

            Assertions.assertInstanceOf(TaskContent.New::class.java, copy.data.content)
            val originWip = (origin.data.content as TaskContent.New).wip
            val copyWip = (copy.data.content as TaskContent.New).wip
            Assertions.assertEquals(originWip.tests.ids.size, copyWip.tests.ids.size)
            Assertions.assertEquals(originWip.exercise?.id, copyWip.exercise?.id)
            Assertions.assertEquals(originWip.statement?.id, copyWip.statement?.id)
            Assertions.assertEquals(originWip.developerSolutions.ids.size, copyWip.developerSolutions.ids.size)
            Assertions.assertEquals(originWip.supportedTrikStudioVersions, copyWip.supportedTrikStudioVersions)
        }

        @Test
        fun `withData should change modified identity fields for New`() {
            val copy = origin.withData {
                name = "Updated Name"
            }

            Assertions.assertEquals("Updated Name", copy.data.name)
            Assertions.assertEquals(10L, copy.data.owner.id.value)
            Assertions.assertEquals("Task Description", copy.data.description)
        }

        @Test
        fun `withData should change variant from New to Committed`() {
            val copy = origin.withData {
                content.committed {
                    exercise(30)
                    statement(40)
                }
            }

            Assertions.assertInstanceOf(TaskContent.Committed::class.java, copy.data.content)
            val copyCommitted = (copy.data.content as TaskContent.Committed).lastCommited
            Assertions.assertEquals(30L, copyCommitted.exercise.id.value)
            Assertions.assertEquals(40L, copyCommitted.statement.id.value)
        }
    }

    @Nested
    inner class TaskCommittedTests {

        private val origin = Task(
            id = TaskId(2),
            createdAt = Instant.now(),
            data = TaskData(
                owner = LazyEntity(MultipleRoleUserId(10)),
                name = "Committed Task",
                description = "Committed Description",
                sharedTo = LazyEntityList(listOf(CommunityId(60))),
                content = TaskContent.Committed(
                    lastCommited = CommitedTaskContent(
                        tests = LazyEntityList(listOf(TestId(20))),
                        exercise = LazyEntity(ExerciseId(30)),
                        statement = LazyEntity(StatementId(40)),
                        developerSolutions = LazyEntityList(listOf(DeveloperSolutionId(50))),
                        supportedTrikStudioVersions = listOf(TrikStudioVersion("3.0.0")),
                    )
                ),
            )
        )

        @Test
        fun `withData should keep all unmodified fields for Committed`() {
            val copy = origin.withData { }

            Assertions.assertEquals(origin.id, copy.id)
            Assertions.assertEquals(origin.createdAt, copy.createdAt)
            Assertions.assertEquals(origin.data.owner.id, copy.data.owner.id)
            Assertions.assertEquals(origin.data.name, copy.data.name)
            Assertions.assertEquals(origin.data.description, copy.data.description)
            Assertions.assertEquals(origin.data.sharedTo.ids.size, copy.data.sharedTo.ids.size)

            Assertions.assertInstanceOf(TaskContent.Committed::class.java, copy.data.content)
            val originCommitted = (origin.data.content as TaskContent.Committed).lastCommited
            val copyCommitted = (copy.data.content as TaskContent.Committed).lastCommited
            Assertions.assertEquals(originCommitted.tests.ids.size, copyCommitted.tests.ids.size)
            Assertions.assertEquals(originCommitted.exercise.id, copyCommitted.exercise.id)
            Assertions.assertEquals(originCommitted.statement.id, copyCommitted.statement.id)
            Assertions.assertEquals(originCommitted.developerSolutions.ids.size, copyCommitted.developerSolutions.ids.size)
            Assertions.assertEquals(originCommitted.supportedTrikStudioVersions, copyCommitted.supportedTrikStudioVersions)
        }

        @Test
        fun `withData should change modified identity fields for Committed`() {
            val copy = origin.withData { name = "Updated Name" }

            Assertions.assertEquals("Updated Name", copy.data.name)
            Assertions.assertEquals(10L, copy.data.owner.id.value)
            Assertions.assertEquals("Committed Description", copy.data.description)
        }

        @Test
        fun `withData should change committed content`() {
            val copy = origin.withData {
                content.committed { exercise(99) }
            }

            Assertions.assertInstanceOf(TaskContent.Committed::class.java, copy.data.content)
            val copyCommitted = (copy.data.content as TaskContent.Committed).lastCommited
            Assertions.assertEquals(99L, copyCommitted.exercise.id.value)
            Assertions.assertEquals(40L, copyCommitted.statement.id.value)
        }
    }

    @Nested
    inner class TaskUncommitedTests {

        private val origin = Task(
            id = TaskId(3),
            createdAt = Instant.now(),
            data = TaskData(
                owner = LazyEntity(MultipleRoleUserId(10)),
                name = "Uncommited Task",
                description = "Uncommited Description",
                sharedTo = LazyEntityList(emptyList()),
                content = TaskContent.Uncommited(
                    wip = WipTaskContent(
                        tests = LazyEntityList(listOf(TestId(20))),
                        exercise = LazyEntity(ExerciseId(30)),
                        statement = LazyEntity(StatementId(40)),
                        developerSolutions = LazyEntityList(emptyList()),
                        supportedTrikStudioVersions = listOf(TrikStudioVersion("3.0.0")),
                    ),
                    lastCommited = CommitedTaskContent(
                        tests = LazyEntityList(listOf(TestId(21))),
                        exercise = LazyEntity(ExerciseId(31)),
                        statement = LazyEntity(StatementId(41)),
                        developerSolutions = LazyEntityList(listOf(DeveloperSolutionId(51))),
                        supportedTrikStudioVersions = listOf(TrikStudioVersion("2.0.0")),
                    ),
                ),
            )
        )

        @Test
        fun `withData should keep all unmodified fields for Uncommited`() {
            val copy = origin.withData { }

            Assertions.assertEquals(origin.id, copy.id)
            Assertions.assertEquals(origin.createdAt, copy.createdAt)
            Assertions.assertEquals(origin.data.owner.id, copy.data.owner.id)
            Assertions.assertEquals(origin.data.name, copy.data.name)
            Assertions.assertEquals(origin.data.description, copy.data.description)

            Assertions.assertInstanceOf(TaskContent.Uncommited::class.java, copy.data.content)
            val originContent = origin.data.content as TaskContent.Uncommited
            val copyContent = copy.data.content as TaskContent.Uncommited

            Assertions.assertEquals(originContent.wip.tests.ids.size, copyContent.wip.tests.ids.size)
            Assertions.assertEquals(originContent.wip.exercise?.id, copyContent.wip.exercise?.id)
            Assertions.assertEquals(originContent.wip.statement?.id, copyContent.wip.statement?.id)

            Assertions.assertEquals(originContent.lastCommited.tests.ids.size, copyContent.lastCommited.tests.ids.size)
            Assertions.assertEquals(originContent.lastCommited.exercise.id, copyContent.lastCommited.exercise.id)
            Assertions.assertEquals(originContent.lastCommited.statement.id, copyContent.lastCommited.statement.id)
        }

        @Test
        fun `withData should change uncommited content`() {
            val copy = origin.withData {
                content.uncommited(
                    wipBuilder = { exercise(99) },
                    lastCommitedBuilder = { exercise(98) },
                )
            }

            val copyContent = copy.data.content as TaskContent.Uncommited
            Assertions.assertEquals(99L, copyContent.wip.exercise?.id?.value)
            Assertions.assertEquals(98L, copyContent.lastCommited.exercise.id.value)
        }

        @Test
        fun `withData should change variant from Uncommited to New`() {
            val copy = origin.withData {
                content.new { }
            }

            Assertions.assertInstanceOf(TaskContent.New::class.java, copy.data.content)
        }
    }

    @Nested
    inner class ParticipantTests {

        private val origin = Participant(
            id = SingleRoleUserId(1),
            createdAt = Instant.now(),
            data = ParticipantData(
                competition = LazyEntity(CompetitionId(10)),
                accessToken = "participant-token",
                name = "Participant",
            )
        )

        @Test
        fun `withData should keep all unmodified fields`() {
            val copy = origin.withData { }

            Assertions.assertEquals(origin.id, copy.id)
            Assertions.assertEquals(origin.createdAt, copy.createdAt)
            Assertions.assertEquals(origin.data.competition.id, copy.data.competition.id)
            Assertions.assertEquals(origin.data.accessToken, copy.data.accessToken)
            Assertions.assertEquals(origin.data.name, copy.data.name)
        }

        @Test
        fun `withData should change modified fields`() {
            val copy = origin.withData { competition(99) }

            Assertions.assertEquals(99L, copy.data.competition.id.value)
        }
    }

    @Nested
    inner class ObserverTests {

        private val origin = Observer(
            id = SingleRoleUserId(1),
            createdAt = Instant.now(),
            data = ObserverData(
                community = LazyEntity(CommunityId(7)),
                competitions = LazyEntityList(listOf(CompetitionId(10), CompetitionId(20))),
                accessToken = "observer-token",
                name = "Observer",
            )
        )

        @Test
        fun `withData should keep all unmodified fields`() {
            val copy = origin.withData { }

            Assertions.assertEquals(origin.id, copy.id)
            Assertions.assertEquals(origin.createdAt, copy.createdAt)
            Assertions.assertEquals(origin.data.community.id, copy.data.community.id)
            Assertions.assertEquals(origin.data.accessToken, copy.data.accessToken)
            Assertions.assertEquals(origin.data.name, copy.data.name)
            Assertions.assertEquals(origin.data.competitions.ids.size, copy.data.competitions.ids.size)
        }

        @Test
        fun `withData should change modified fields`() {
            val copy = origin.withData { accessToken = "new-token" }

            Assertions.assertEquals("new-token", copy.data.accessToken)
        }
    }

    @Nested
    inner class MultipleRoleUserTests {

        private val origin = MultipleRoleUser(
            id = MultipleRoleUserId(1),
            createdAt = Instant.now(),
            data = MultipleRoleUserData(
                accessToken = "user-token",
                name = "Alice",
                email = "alice@example.com",
                roles = listOf(
                    Developer(
                        memberOf = LazyEntityList(listOf(CommunityId(10))),
                        data = DeveloperData(
                            tasks = LazyEntityList(listOf(TaskId(1))),
                            contests = LazyEntityList(emptyList()),
                            polygons = LazyEntityList(emptyList()),
                            solutions = LazyEntityList(emptyList()),
                            exercises = LazyEntityList(emptyList())
                        )
                    ),
                    Administrator(
                        memberOf = LazyEntityList(emptyList())
                    )
                )
            )
        )

        @Test
        fun `withData should keep all unmodified fields`() {
            val copy = origin.withData { }

            Assertions.assertEquals(origin.id, copy.id)
            Assertions.assertEquals(origin.createdAt, copy.createdAt)
            Assertions.assertEquals(origin.data.accessToken, copy.data.accessToken)
            Assertions.assertEquals(origin.data.name, copy.data.name)
            Assertions.assertEquals(origin.data.email, copy.data.email)
            Assertions.assertEquals(origin.data.roles.size, copy.data.roles.size)
        }

        @Test
        fun `withData should change modified fields`() {
            val copy = origin.withData { accessToken = "new-token" }

            Assertions.assertEquals("new-token", copy.data.accessToken)
        }
    }

    @Nested
    inner class SupervisorTests {

        private val origin = Supervisor(
            id = SingleRoleUserId(1),
            createdAt = Instant.now(),
            data = SupervisorData(
                accessToken = "supervisor-token",
                name = "Supervisor",
            )
        )

        @Test
        fun `withData should keep all unmodified fields`() {
            val copy = origin.withData { }

            Assertions.assertEquals(origin.id, copy.id)
            Assertions.assertEquals(origin.createdAt, copy.createdAt)
            Assertions.assertEquals(origin.data.accessToken, copy.data.accessToken)
            Assertions.assertEquals(origin.data.name, copy.data.name)
        }

        @Test
        fun `withData should change modified fields`() {
            val copy = origin.withData { accessToken = "new-token" }

            Assertions.assertEquals("new-token", copy.data.accessToken)
        }
    }

}
