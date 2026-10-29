package tech.testsys.operation.user

import io.mockk.Called
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import tech.testsys.domain.builder.api.competition
import tech.testsys.domain.builder.api.contest
import tech.testsys.domain.builder.api.participant
import tech.testsys.domain.builder.api.task
import tech.testsys.domain.contract.persistence.ContestTaskResult
import tech.testsys.domain.contract.persistence.ObserverContestFilter
import tech.testsys.domain.contract.persistence.Page
import tech.testsys.domain.contract.persistence.Pagination
import tech.testsys.domain.contract.persistence.repository.CompetitionRepository
import tech.testsys.domain.contract.persistence.repository.ContestRepository
import tech.testsys.domain.contract.persistence.repository.ObserverContest
import tech.testsys.domain.contract.persistence.repository.ParticipantRepository
import tech.testsys.domain.contract.persistence.repository.SubmissionRepository
import tech.testsys.domain.contract.persistence.repository.TaskRepository
import tech.testsys.domain.model.group.CompetitionId
import tech.testsys.domain.model.task.ContestId
import tech.testsys.domain.model.task.Score
import tech.testsys.domain.model.task.StatementId
import tech.testsys.domain.model.task.TaskId
import tech.testsys.domain.model.user.UserId
import tech.testsys.operation.error.MissedObserverRoleError
import tech.testsys.operation.error.getOrThrow
import tech.testsys.operation.util.assertRaises
import tech.testsys.operation.util.testCompetition
import tech.testsys.operation.util.testContest
import tech.testsys.operation.util.testObserver
import tech.testsys.operation.util.testParticipant
import tech.testsys.operation.util.testTask
import java.time.Instant

class ObserverOperationsTests {
    private val contests = mockk<ContestRepository>()
    private val competitions = mockk<CompetitionRepository>()
    private val participants = mockk<ParticipantRepository>()
    private val tasks = mockk<TaskRepository>()
    private val submissions = mockk<SubmissionRepository>()
    private val operations = ObserverOperations(contests, competitions, participants, tasks, submissions)

    @Nested
    inner class ViewContestsTests {
        @Nested
        inner class HappyPathTests {
            @Test
            fun `should return pairs from the persistence port`() {
                val observer = testObserver { contests(listOf(19, 19)) }
                val pagination = Pagination(0, 2)
                val filter = ObserverContestFilter(name = "Tour", competitionId = CompetitionId(23))
                val pair = ObserverContest(testCompetition { contests(listOf(19)) }, testContest())
                val expected = Page(listOf(pair), pagination, 1)
                every { competitions.findAvailableToObserver(setOf(ContestId(19)), pagination, filter) } returns expected

                val actual = operations.viewContests(observer, pagination, filter).getOrThrow()

                assertSame(expected, actual)
                verify(exactly = 1) { competitions.findAvailableToObserver(setOf(ContestId(19)), pagination, filter) }
            }
        }

        @Nested
        inner class RefusalTests {
            @Test
            fun `should reject another role before reading repositories`() {
                assertRaises(MissedObserverRoleError) { operations.viewContests(testParticipant(), Pagination(0, 10)) }
                verify { competitions wasNot Called }
            }
        }
    }

    @Nested
    inner class DownloadResultTests {
        @Nested
        inner class HappyPathTests {
            @Test
            fun `should combine repeated participant ids and keep contest results separate`() {
                val observer = testObserver { contests(listOf(20, 19)) }
                val firstCompetition = testCompetition {
                    name = "First"
                    contests(listOf(19))
                    participants(listOf(17))
                }
                val secondCompetition = competition {
                    id = 24
                    createdAt = Instant.EPOCH
                    data = testCompetition {
                        name = "Second"
                        contests(listOf(20, 21))
                        participants(listOf(18, 17))
                    }.data
                }
                val firstContest = testContest { tasks(listOf(0)) }
                val secondContest = contest {
                    id = 20
                    createdAt = Instant.EPOCH
                    data = testContest { tasks(listOf(2)) }.data
                }
                val firstTask = testTask { new { statement = StatementId(1) } }
                val secondTask = task {
                    id = 2
                    createdAt = Instant.EPOCH
                    data = firstTask.data
                }
                val firstParticipant = testParticipant { name = "Same" }
                val secondParticipant = participant {
                    id = 18
                    createdAt = Instant.EPOCH
                    data = testParticipant {
                        name = "Same"
                        competition(24)
                    }.data
                }
                every { competitions.findByContestIds(setOf(ContestId(19), ContestId(20))) } returns
                    listOf(secondCompetition, firstCompetition)
                every { contests.findByIds(listOf(ContestId(19), ContestId(20))) } returns listOf(secondContest, firstContest)
                every { participants.findByIds(listOf(firstParticipant.id, secondParticipant.id)) } returns
                    listOf(secondParticipant, firstParticipant)
                every { tasks.findByIds(listOf(TaskId(0), TaskId(2))) } returns listOf(secondTask, firstTask)
                every {
                    submissions.findContestResults(ContestId(19), setOf(firstParticipant.id), setOf(TaskId(0)))
                } returns listOf(ContestTaskResult(firstParticipant.id, TaskId(0), Score(42), 3))
                every {
                    submissions.findContestResults(ContestId(20), setOf(firstParticipant.id, secondParticipant.id), setOf(TaskId(2)))
                } returns listOf(
                    ContestTaskResult(firstParticipant.id, TaskId(2), Score(73), 1),
                    ContestTaskResult(secondParticipant.id, TaskId(2), Score(27), 1),
                )

                val result = operations.downloadResult(observer).getOrThrow()

                assertEquals(
                    listOf(
                        ObserverResult.Column("Contest", ContestId(19), "name", TaskId(0)),
                        ObserverResult.Column("Contest", ContestId(20), "name", TaskId(2)),
                    ),
                    result.columns,
                )
                assertEquals(
                    listOf(
                        ObserverResult.Row(
                            participantId = firstParticipant.id,
                            scores = listOf(42, 73),
                        ),
                        ObserverResult.Row(
                            participantId = secondParticipant.id,
                            scores = listOf(null, 27),
                        ),
                    ),
                    result.rows,
                )
                verify(exactly = 1) { competitions.findByContestIds(setOf(ContestId(19), ContestId(20))) }
                verify(exactly = 1) { contests.findByIds(listOf(ContestId(19), ContestId(20))) }
                verify(exactly = 1) { participants.findByIds(listOf(firstParticipant.id, secondParticipant.id)) }
                verify(exactly = 1) { tasks.findByIds(listOf(TaskId(0), TaskId(2))) }
                verify(exactly = 0) { submissions.findContestResults(ContestId(21), any(), any()) }
            }

            @Test
            fun `should share one contest column across competitions`() {
                val observer = testObserver { contests(listOf(19)) }
                val firstCompetition = testCompetition {
                    contests(listOf(19))
                    participants(listOf(17))
                }
                val secondCompetition = competition {
                    id = 24
                    createdAt = Instant.EPOCH
                    data = testCompetition {
                        contests(listOf(19))
                        participants(listOf(17, 18))
                    }.data
                }
                val contest = testContest { tasks(listOf(0)) }
                val task = testTask { new { statement = StatementId(1) } }
                val firstParticipant = testParticipant()
                val secondParticipant = participant {
                    id = 18
                    createdAt = Instant.EPOCH
                    data = testParticipant { competition(24) }.data
                }
                every { competitions.findByContestIds(setOf(ContestId(19))) } returns
                    listOf(secondCompetition, firstCompetition)
                every { contests.findByIds(listOf(ContestId(19))) } returns listOf(contest)
                every { participants.findByIds(listOf(firstParticipant.id, secondParticipant.id)) } returns
                    listOf(secondParticipant, firstParticipant)
                every { tasks.findByIds(listOf(TaskId(0))) } returns listOf(task)
                every {
                    submissions.findContestResults(
                        ContestId(19),
                        setOf(firstParticipant.id, secondParticipant.id),
                        setOf(TaskId(0)),
                    )
                } returns listOf(
                    ContestTaskResult(firstParticipant.id, TaskId(0), Score(42), 1),
                    ContestTaskResult(secondParticipant.id, TaskId(0), Score(27), 1),
                )

                val result = operations.downloadResult(observer).getOrThrow()

                assertEquals(listOf(ObserverResult.Column("Contest", ContestId(19), "name", TaskId(0))), result.columns)
                assertEquals(
                    listOf(
                        ObserverResult.Row(firstParticipant.id, listOf(42)),
                        ObserverResult.Row(secondParticipant.id, listOf(27)),
                    ),
                    result.rows,
                )
                verify(exactly = 1) {
                    submissions.findContestResults(
                        ContestId(19),
                        setOf(firstParticipant.id, secondParticipant.id),
                        setOf(TaskId(0)),
                    )
                }
            }

            @Test
            fun `should return an empty table when no competition is accessible`() {
                every { competitions.findByContestIds(emptySet()) } returns emptyList()
                every { contests.findByIds(emptyList()) } returns emptyList()
                every { participants.findByIds(emptyList()) } returns emptyList()
                every { tasks.findByIds(emptyList()) } returns emptyList()

                val result = operations.downloadResult(testObserver()).getOrThrow()

                assertEquals(ObserverResult(emptyList(), emptyList()), result)
                verify { submissions wasNot Called }
            }

            @Test
            fun `should include every participant and only assigned contest scores`() {
                val observer = testObserver { contests(listOf(19)) }
                val competition = testCompetition {
                    contests(listOf(19, 20))
                    participants(listOf(17))
                }
                val contest = testContest { tasks(listOf(0)) }
                val participant = testParticipant { name = "A;\"B" }
                val task = testTask { new { statement = StatementId(1) } }
                every { competitions.findByContestIds(setOf(ContestId(19))) } returns listOf(competition)
                every { contests.findByIds(listOf(ContestId(19))) } returns listOf(contest)
                every { participants.findByIds(listOf(participant.id)) } returns listOf(participant)
                every { tasks.findByIds(listOf(TaskId(0))) } returns listOf(task)
                every {
                    submissions.findContestResults(ContestId(19), setOf<UserId>(participant.id), setOf(TaskId(0)))
                } returns listOf(ContestTaskResult(participant.id, TaskId(0), Score(42), 2))

                val result = operations.downloadResult(observer).getOrThrow()

                assertEquals(listOf(ObserverResult.Column("Contest", ContestId(19), "name", TaskId(0))), result.columns)
                assertEquals(
                    listOf(
                        ObserverResult.Row(
                            participantId = participant.id,
                            scores = listOf(42),
                        ),
                    ),
                    result.rows,
                )
                verify(exactly = 0) { submissions.findContestResults(ContestId(20), any(), any()) }
            }

            @Test
            fun `should leave score empty when the participant has no submission`() {
                val observer = testObserver { contests(listOf(19)) }
                val competition = testCompetition {
                    contests(listOf(19))
                    participants(listOf(17))
                }
                val contest = testContest { tasks(listOf(0)) }
                val participant = testParticipant()
                val task = testTask { new { statement = StatementId(1) } }
                every { competitions.findByContestIds(setOf(ContestId(19))) } returns listOf(competition)
                every { contests.findByIds(listOf(ContestId(19))) } returns listOf(contest)
                every { participants.findByIds(listOf(participant.id)) } returns listOf(participant)
                every { tasks.findByIds(listOf(TaskId(0))) } returns listOf(task)
                every { submissions.findContestResults(ContestId(19), any<Set<UserId>>(), setOf(TaskId(0))) } returns emptyList()

                val result = operations.downloadResult(observer).getOrThrow()

                assertEquals(
                    listOf(
                        ObserverResult.Row(
                            participantId = participant.id,
                            scores = listOf(null),
                        ),
                    ),
                    result.rows,
                )
            }

            @Test
            fun `should order assigned contest and task columns by identifiers`() {
                val observer = testObserver { contests(listOf(19, 20)) }
                val competition = testCompetition { contests(listOf(20, 21, 19)) }
                val first = testContest {
                    name = "Одинаковый"
                    tasks(listOf(2, 0))
                }
                val second = contest {
                    id = 20
                    createdAt = Instant.EPOCH
                    data = first.data
                }
                val firstTask = testTask { new { statement = StatementId(1) } }
                val secondTask = task {
                    id = 2
                    createdAt = Instant.EPOCH
                    data = firstTask.data
                }
                every { competitions.findByContestIds(setOf(ContestId(19), ContestId(20))) } returns listOf(competition)
                every { contests.findByIds(listOf(ContestId(19), ContestId(20))) } returns listOf(second, first)
                every { participants.findByIds(emptyList()) } returns emptyList()
                every { tasks.findByIds(listOf(TaskId(2), TaskId(0))) } returns listOf(secondTask, firstTask)
                every { submissions.findContestResults(any(), emptySet(), setOf(TaskId(0), TaskId(2))) } returns emptyList()

                val result = operations.downloadResult(observer).getOrThrow()

                assertEquals(
                    listOf(
                        ObserverResult.Column("Одинаковый", ContestId(19), "name", TaskId(0)),
                        ObserverResult.Column("Одинаковый", ContestId(19), "name", TaskId(2)),
                        ObserverResult.Column("Одинаковый", ContestId(20), "name", TaskId(0)),
                        ObserverResult.Column("Одинаковый", ContestId(20), "name", TaskId(2)),
                    ),
                    result.columns,
                )
                verify(exactly = 0) { submissions.findContestResults(ContestId(21), any(), any()) }
            }
        }

        @Nested
        inner class RefusalTests {
            @Test
            fun `should reject another role before reading repositories`() {
                assertRaises(MissedObserverRoleError) { operations.downloadResult(testParticipant()) }
                verify { competitions wasNot Called }
            }
        }
    }
}
