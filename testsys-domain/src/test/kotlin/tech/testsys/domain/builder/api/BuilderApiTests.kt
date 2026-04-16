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
import tech.testsys.domain.model.task.TaskData
import tech.testsys.domain.model.task.TaskId
import tech.testsys.domain.model.task.TestData
import tech.testsys.domain.model.task.TestId
import tech.testsys.domain.model.task.TrikStudioVersion
import tech.testsys.domain.model.task.TrikSupportedLanguage
import tech.testsys.domain.model.task.Verdict
import tech.testsys.domain.model.task.VerdictData
import tech.testsys.domain.model.task.VerdictId
import tech.testsys.domain.model.task.VersionData
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
                owner = LazyEntity(MultipleRoleUserId(10))
            )
        )

        @Test
        fun `withData should keep all unmodified fields`() {
            val copy = origin.withData { }

            Assertions.assertEquals(origin.id, copy.id)
            Assertions.assertEquals(origin.createdAt, copy.createdAt)
            Assertions.assertEquals(origin.data.owner.id, copy.data.owner.id)
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

        private val origin = DeveloperSolution(
            id = DeveloperSolutionId(1),
            createdAt = Instant.now(),
            data = DeveloperSolutionData(
                solution = LazyEntity(SolutionId(10)),
                expectedScore = Score(100)
            )
        )

        @Test
        fun `withData should keep all unmodified fields`() {
            val copy = origin.withData { }

            Assertions.assertEquals(origin.id, copy.id)
            Assertions.assertEquals(origin.createdAt, copy.createdAt)
            Assertions.assertEquals(origin.data.solution.id, copy.data.solution.id)
            Assertions.assertEquals(origin.data.expectedScore, copy.data.expectedScore)
        }

        @Test
        fun `withData should change modified fields`() {
            val copy = origin.withData { solution(99) }

            Assertions.assertEquals(99L, copy.data.solution.id.value)
        }
    }

    @Nested
    inner class ExerciseTests {

        private val origin = Exercise(
            id = ExerciseId(1),
            createdAt = Instant.now(),
            versionData = VersionData(LazyEntity(ExerciseId(1)), 0),
            data = ExerciseData(
                file = FileData("exercise.qrs", byteArrayOf(1, 2, 3)),
                language = TrikSupportedLanguage.Python
            )
        )

        @Test
        fun `withData should keep all unmodified fields`() {
            val copy = origin.withData { }

            Assertions.assertEquals(origin.id, copy.id)
            Assertions.assertEquals(origin.createdAt, copy.createdAt)
            Assertions.assertEquals(origin.versionData.root.id, copy.versionData.root.id)
            Assertions.assertEquals(origin.versionData.index, copy.versionData.index)
            Assertions.assertEquals(origin.data.file.uploadedFilename, copy.data.file.uploadedFilename)
            Assertions.assertArrayEquals(origin.data.file.content, copy.data.file.content)
            Assertions.assertEquals(origin.data.language, copy.data.language)
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
                verdict = LazyEntity(VerdictId(20))
            )
        )

        @Test
        fun `withData should keep all unmodified fields`() {
            val copy = origin.withData { }

            Assertions.assertEquals(origin.id, copy.id)
            Assertions.assertEquals(origin.createdAt, copy.createdAt)
            Assertions.assertEquals(origin.data.judge.id, copy.data.judge.id)
            Assertions.assertEquals(origin.data.verdict.id, copy.data.verdict.id)
        }

        @Test
        fun `withData should change modified fields`() {
            val copy = origin.withData { judge(99) }

            Assertions.assertEquals(99L, copy.data.judge.id.value)
        }
    }

    @Nested
    inner class SolutionTests {

        private val origin = Solution(
            id = SolutionId(1),
            createdAt = Instant.now(),
            data = SolutionData(
                file = FileData("solution.py", byteArrayOf(4, 5, 6)),
                language = TrikSupportedLanguage.Python
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

        private val origin = Statement(
            id = StatementId(1),
            createdAt = Instant.now(),
            versionData = VersionData(LazyEntity(StatementId(1)), 0),
            data = StatementData(
                file = FileData("statement.pdf", byteArrayOf(7, 8, 9))
            )
        )

        @Test
        fun `withData should keep all unmodified fields`() {
            val copy = origin.withData { }

            Assertions.assertEquals(origin.id, copy.id)
            Assertions.assertEquals(origin.createdAt, copy.createdAt)
            Assertions.assertEquals(origin.versionData.root.id, copy.versionData.root.id)
            Assertions.assertEquals(origin.versionData.index, copy.versionData.index)
            Assertions.assertEquals(origin.data.file.uploadedFilename, copy.data.file.uploadedFilename)
            Assertions.assertArrayEquals(origin.data.file.content, copy.data.file.content)
        }

        @Test
        fun `withData should change modified fields`() {
            val copy = origin.withData { file("updated.pdf", byteArrayOf(10, 11)) }

            Assertions.assertEquals("updated.pdf", copy.data.file.uploadedFilename)
        }
    }

    @Nested
    inner class TestTests {

        private val origin = tech.testsys.domain.model.task.Test(
            id = TestId(1),
            createdAt = Instant.now(),
            data = TestData(
                file = FileData("test.xml", byteArrayOf(1, 2)),
                versionData = VersionData(LazyEntity(TestId(1)), 0)
            )
        )

        @Test
        fun `withData should keep all unmodified fields`() {
            val copy = origin.withData { }

            Assertions.assertEquals(origin.id, copy.id)
            Assertions.assertEquals(origin.createdAt, copy.createdAt)
            Assertions.assertEquals(origin.data.file.uploadedFilename, copy.data.file.uploadedFilename)
            Assertions.assertArrayEquals(origin.data.file.content, copy.data.file.content)
            Assertions.assertEquals(origin.data.versionData.root.id, copy.data.versionData.root.id)
            Assertions.assertEquals(origin.data.versionData.index, copy.data.versionData.index)
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
            data = TaskData.New(
                wip = WipTaskContent(
                    owner = LazyEntity(MultipleRoleUserId(10)),
                    name = "Task Name",
                    description = "Task Description",
                    tests = LazyEntityList(listOf(TestId(20))),
                    exercise = LazyEntity(ExerciseId(30)),
                    statement = LazyEntity(StatementId(40)),
                    developerSolutions = LazyEntityList(listOf(DeveloperSolutionId(50))),
                    supportedTrikStudioVersions = listOf(TrikStudioVersion("3.0.0")),
                    sharedTo = LazyEntityList(listOf(CommunityId(60)))
                )
            )
        )

        @Test
        fun `withData should keep all unmodified fields for New`() {
            val copy = origin.withData { }
            val originWip = (origin.data as TaskData.New).wip
            val copyWip = (copy.data as TaskData.New).wip

            Assertions.assertEquals(origin.id, copy.id)
            Assertions.assertEquals(origin.createdAt, copy.createdAt)
            Assertions.assertInstanceOf(TaskData.New::class.java, copy.data)
            Assertions.assertEquals(originWip.owner.id, copyWip.owner.id)
            Assertions.assertEquals(originWip.name, copyWip.name)
            Assertions.assertEquals(originWip.description, copyWip.description)
            Assertions.assertEquals(originWip.tests.ids.size, copyWip.tests.ids.size)
            Assertions.assertEquals(originWip.exercise?.id, copyWip.exercise?.id)
            Assertions.assertEquals(originWip.statement?.id, copyWip.statement?.id)
            Assertions.assertEquals(originWip.developerSolutions.ids.size, copyWip.developerSolutions.ids.size)
            Assertions.assertEquals(originWip.supportedTrikStudioVersions, copyWip.supportedTrikStudioVersions)
            Assertions.assertEquals(originWip.sharedTo.ids.size, copyWip.sharedTo.ids.size)
        }

        @Test
        fun `withData should change modified fields for New`() {
            val copy = origin.withData {
                new { name = "Updated Name" }
            }

            val copyWip = (copy.data as TaskData.New).wip
            Assertions.assertEquals("Updated Name", copyWip.name)
            Assertions.assertEquals(10L, copyWip.owner.id.value)
            Assertions.assertEquals("Task Description", copyWip.description)
        }

        @Test
        fun `withData should change variant from New to Committed`() {
            val copy = origin.withData {
                commited {
                    owner(10)
                    name = "Committed Name"
                    description = "Committed Description"
                    exercise(30)
                    statement(40)
                }
            }

            Assertions.assertInstanceOf(TaskData.Committed::class.java, copy.data)
            val copyCommitted = (copy.data as TaskData.Committed).lastCommited
            Assertions.assertEquals("Committed Name", copyCommitted.name)
        }
    }

    @Nested
    inner class TaskCommittedTests {

        private val origin = Task(
            id = TaskId(2),
            createdAt = Instant.now(),
            data = TaskData.Committed(
                lastCommited = CommitedTaskContent(
                    owner = LazyEntity(MultipleRoleUserId(10)),
                    name = "Committed Task",
                    description = "Committed Description",
                    tests = LazyEntityList(listOf(TestId(20))),
                    exercise = LazyEntity(ExerciseId(30)),
                    statement = LazyEntity(StatementId(40)),
                    developerSolutions = LazyEntityList(listOf(DeveloperSolutionId(50))),
                    supportedTrikStudioVersions = listOf(TrikStudioVersion("3.0.0")),
                    sharedTo = LazyEntityList(listOf(CommunityId(60)))
                )
            )
        )

        @Test
        fun `withData should keep all unmodified fields for Committed`() {
            val copy = origin.withData { }
            val originCommitted = (origin.data as TaskData.Committed).lastCommited
            val copyCommitted = (copy.data as TaskData.Committed).lastCommited

            Assertions.assertEquals(origin.id, copy.id)
            Assertions.assertEquals(origin.createdAt, copy.createdAt)
            Assertions.assertInstanceOf(TaskData.Committed::class.java, copy.data)
            Assertions.assertEquals(originCommitted.owner.id, copyCommitted.owner.id)
            Assertions.assertEquals(originCommitted.name, copyCommitted.name)
            Assertions.assertEquals(originCommitted.description, copyCommitted.description)
            Assertions.assertEquals(originCommitted.tests.ids.size, copyCommitted.tests.ids.size)
            Assertions.assertEquals(originCommitted.exercise.id, copyCommitted.exercise.id)
            Assertions.assertEquals(originCommitted.statement.id, copyCommitted.statement.id)
            Assertions.assertEquals(originCommitted.developerSolutions.ids.size, copyCommitted.developerSolutions.ids.size)
            Assertions.assertEquals(originCommitted.supportedTrikStudioVersions, copyCommitted.supportedTrikStudioVersions)
            Assertions.assertEquals(originCommitted.sharedTo.ids.size, copyCommitted.sharedTo.ids.size)
        }

        @Test
        fun `withData should change modified fields for Committed`() {
            val copy = origin.withData {
                commited { name = "Updated Name" }
            }

            val copyCommitted = (copy.data as TaskData.Committed).lastCommited
            Assertions.assertEquals("Updated Name", copyCommitted.name)
            Assertions.assertEquals(10L, copyCommitted.owner.id.value)
            Assertions.assertEquals("Committed Description", copyCommitted.description)
        }
    }

    @Nested
    inner class TaskUncommitedTests {

        private val origin = Task(
            id = TaskId(3),
            createdAt = Instant.now(),
            data = TaskData.Uncommited(
                wip = WipTaskContent(
                    owner = LazyEntity(MultipleRoleUserId(10)),
                    name = "WIP Name",
                    description = "WIP Description",
                    tests = LazyEntityList(listOf(TestId(20))),
                    exercise = LazyEntity(ExerciseId(30)),
                    statement = LazyEntity(StatementId(40)),
                    developerSolutions = LazyEntityList(emptyList()),
                    supportedTrikStudioVersions = listOf(TrikStudioVersion("3.0.0")),
                    sharedTo = LazyEntityList(emptyList())
                ),
                lastCommited = CommitedTaskContent(
                    owner = LazyEntity(MultipleRoleUserId(10)),
                    name = "Committed Name",
                    description = "Committed Description",
                    tests = LazyEntityList(listOf(TestId(21))),
                    exercise = LazyEntity(ExerciseId(31)),
                    statement = LazyEntity(StatementId(41)),
                    developerSolutions = LazyEntityList(listOf(DeveloperSolutionId(51))),
                    supportedTrikStudioVersions = listOf(TrikStudioVersion("2.0.0")),
                    sharedTo = LazyEntityList(listOf(CommunityId(61)))
                )
            )
        )

        @Test
        fun `withData should keep all unmodified fields for Uncommited`() {
            val copy = origin.withData { }
            val originData = origin.data as TaskData.Uncommited
            val copyData = copy.data as TaskData.Uncommited

            Assertions.assertEquals(origin.id, copy.id)
            Assertions.assertEquals(origin.createdAt, copy.createdAt)
            Assertions.assertInstanceOf(TaskData.Uncommited::class.java, copy.data)

            Assertions.assertEquals(originData.wip.owner.id, copyData.wip.owner.id)
            Assertions.assertEquals(originData.wip.name, copyData.wip.name)
            Assertions.assertEquals(originData.wip.description, copyData.wip.description)
            Assertions.assertEquals(originData.wip.tests.ids.size, copyData.wip.tests.ids.size)
            Assertions.assertEquals(originData.wip.exercise?.id, copyData.wip.exercise?.id)
            Assertions.assertEquals(originData.wip.statement?.id, copyData.wip.statement?.id)

            Assertions.assertEquals(originData.lastCommited.owner.id, copyData.lastCommited.owner.id)
            Assertions.assertEquals(originData.lastCommited.name, copyData.lastCommited.name)
            Assertions.assertEquals(originData.lastCommited.description, copyData.lastCommited.description)
            Assertions.assertEquals(originData.lastCommited.tests.ids.size, copyData.lastCommited.tests.ids.size)
            Assertions.assertEquals(originData.lastCommited.exercise.id, copyData.lastCommited.exercise.id)
            Assertions.assertEquals(originData.lastCommited.statement.id, copyData.lastCommited.statement.id)
        }

        @Test
        fun `withData should change modified fields for Uncommited`() {
            val copy = origin.withData {
                uncommited(
                    wipBuilder = { name = "Updated WIP" },
                    lastCommitedBuilder = { name = "Updated Committed" }
                )
            }

            val copyData = copy.data as TaskData.Uncommited
            Assertions.assertEquals("Updated WIP", copyData.wip.name)
            Assertions.assertEquals(10L, copyData.wip.owner.id.value)
            Assertions.assertEquals("Updated Committed", copyData.lastCommited.name)
            Assertions.assertEquals(10L, copyData.lastCommited.owner.id.value)
        }

        @Test
        fun `withData should change variant from Uncommited to New`() {
            val copy = origin.withData {
                new {
                    owner(10)
                    name = "New Name"
                }
            }

            Assertions.assertInstanceOf(TaskData.New::class.java, copy.data)
            val copyWip = (copy.data as TaskData.New).wip
            Assertions.assertEquals("New Name", copyWip.name)
        }
    }

    @Nested
    inner class ParticipantTests {

        private val origin = Participant(
            id = SingleRoleUserId(1),
            createdAt = Instant.now(),
            data = ParticipantData(
                competition = LazyEntity(CompetitionId(10)),
                accessToken = "participant-token"
            )
        )

        @Test
        fun `withData should keep all unmodified fields`() {
            val copy = origin.withData { }

            Assertions.assertEquals(origin.id, copy.id)
            Assertions.assertEquals(origin.createdAt, copy.createdAt)
            Assertions.assertEquals(origin.data.competition.id, copy.data.competition.id)
            Assertions.assertEquals(origin.data.accessToken, copy.data.accessToken)
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
                competitions = LazyEntityList(listOf(CompetitionId(10), CompetitionId(20))),
                accessToken = "observer-token"
            )
        )

        @Test
        fun `withData should keep all unmodified fields`() {
            val copy = origin.withData { }

            Assertions.assertEquals(origin.id, copy.id)
            Assertions.assertEquals(origin.createdAt, copy.createdAt)
            Assertions.assertEquals(origin.data.accessToken, copy.data.accessToken)
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
                accessToken = "supervisor-token"
            )
        )

        @Test
        fun `withData should keep all unmodified fields`() {
            val copy = origin.withData { }

            Assertions.assertEquals(origin.id, copy.id)
            Assertions.assertEquals(origin.createdAt, copy.createdAt)
            Assertions.assertEquals(origin.data.accessToken, copy.data.accessToken)
        }

        @Test
        fun `withData should change modified fields`() {
            val copy = origin.withData { accessToken = "new-token" }

            Assertions.assertEquals("new-token", copy.data.accessToken)
        }
    }

}
