package tech.testsys.operation.user

import io.mockk.Called
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import tech.testsys.domain.builder.api.competition
import tech.testsys.domain.builder.api.withData
import tech.testsys.domain.contract.persistence.ObserverContestFilter
import tech.testsys.domain.contract.persistence.Page
import tech.testsys.domain.contract.persistence.Pagination
import tech.testsys.domain.contract.persistence.Sort
import tech.testsys.domain.contract.persistence.repository.CompetitionRepository
import tech.testsys.domain.contract.persistence.repository.ContestRepository
import tech.testsys.domain.model.group.CompetitionId
import tech.testsys.domain.model.task.Contest
import tech.testsys.domain.model.task.ContestData
import tech.testsys.domain.model.task.ContestId
import tech.testsys.operation.error.CompetitionAccessDeniedError
import tech.testsys.operation.error.CompetitionNotExistsError
import tech.testsys.operation.error.MissedObserverRoleError
import tech.testsys.operation.error.getOrThrow
import tech.testsys.operation.util.assertRaises
import tech.testsys.operation.util.testCompetition
import tech.testsys.operation.util.testContest
import tech.testsys.operation.util.testObserver
import tech.testsys.operation.util.testParticipant
import tech.testsys.operation.util.testSupervisor
import java.time.Duration
import java.time.Instant

class ObserverOperationsTests {

    private val contests = mockk<ContestRepository>()
    private val competitions = mockk<CompetitionRepository>()
    private val operations = ObserverOperations(contestRepository = contests, competitionRepository = competitions)

    @Nested
    inner class DownloadResultTests {

        @Nested
        inner class HappyPathTests {

            @Test
            fun `should return an empty CSV named after the competition with an assigned contest`() {
                val user = testObserver { contests(listOf(24, 23)) }
                every { competitions.findById(CompetitionId(23)) } returns testCompetition {
                    name = "Different title"
                    contests(listOf(23, 99))
                }

                val result = operations.downloadResult(user = user, competitionId = CompetitionId(23)).getOrThrow()

                assertEquals("competition-23-results.csv", result.uploadedFilename)
                assertArrayEquals(byteArrayOf(), result.content)
            }

            @Test
            fun `should download results when an assigned contest occurs more than once`() {
                val user = testObserver { contests(listOf(23, 23)) }
                every { competitions.findById(CompetitionId(23)) } returns testCompetition { contests(listOf(23)) }

                val result = operations.downloadResult(user = user, competitionId = CompetitionId(23)).getOrThrow()

                assertEquals("competition-23-results.csv", result.uploadedFilename)
                assertArrayEquals(byteArrayOf(), result.content)
            }

            @Test
            fun `should download results of a second competition that shares an assigned contest with another competition`() {
                val user = testObserver { contests(listOf(19, 20)) }
                val second = competition {
                    id = 24
                    createdAt = Instant.EPOCH
                    data = testCompetition { contests(listOf(19, 99)) }.data
                }
                every { competitions.findById(second.id) } returns second

                val result = operations.downloadResult(user, second.id).getOrThrow()

                assertEquals("competition-24-results.csv", result.uploadedFilename)
            }
        }

        @Nested
        inner class RefusalTests {

            @Test
            fun `should raise MissedObserverRoleError before reading competitions if user is a Participant`() {
                val user = testParticipant()

                assertRaises(MissedObserverRoleError) {
                    operations.downloadResult(user = user, competitionId = CompetitionId(23))
                }

                verify { competitions wasNot Called }
            }

            @Test
            fun `should raise MissedObserverRoleError before reading competitions if user is a Supervisor`() {
                val user = testSupervisor()

                assertRaises(MissedObserverRoleError) {
                    operations.downloadResult(user = user, competitionId = CompetitionId(23))
                }

                verify { competitions wasNot Called }
            }

            @Test
            fun `should raise CompetitionNotExistsError before checking assignment if the competition does not exist`() {
                val user = testObserver()
                every { competitions.findById(CompetitionId(23)) } returns null

                assertRaises(CompetitionNotExistsError(CompetitionId(23))) {
                    operations.downloadResult(user = user, competitionId = CompetitionId(23))
                }
            }

            @Test
            fun `should raise CompetitionAccessDeniedError when no assigned contest belongs to the competition`() {
                val user = testObserver { contests(listOf(24)) }
                every { competitions.findById(CompetitionId(23)) } returns testCompetition()

                assertRaises(CompetitionAccessDeniedError(CompetitionId(23))) {
                    operations.downloadResult(user = user, competitionId = CompetitionId(23))
                }
            }

            @Test
            fun `should raise CompetitionAccessDeniedError when no contests are assigned`() {
                val user = testObserver()
                every { competitions.findById(CompetitionId(23)) } returns testCompetition()

                assertRaises(CompetitionAccessDeniedError(CompetitionId(23))) {
                    operations.downloadResult(user = user, competitionId = CompetitionId(23))
                }
            }
        }

        @Nested
        inner class InvariantTests {

            @Test
            fun `should read only the competition and not access contests when downloading results`() {
                val user = testObserver { contests(listOf(24, 19, 19)) }
                every { competitions.findById(CompetitionId(23)) } returns testCompetition { contests(listOf(19)) }

                operations.downloadResult(user = user, competitionId = CompetitionId(23)).getOrThrow()

                verify(exactly = 1) { competitions.findById(CompetitionId(23)) }
                confirmVerified(competitions)
                verify { contests wasNot Called }
            }

            @Test
            fun `should raise CompetitionAccessDeniedError if the assigned contest is no longer in the current competition`() {
                val user = testObserver { contests(listOf(19)) }
                val original = testCompetition { contests(listOf(19)) }
                every { competitions.findById(original.id) } returns original.withData { contests(listOf(20)) }

                assertRaises(CompetitionAccessDeniedError(original.id)) { operations.downloadResult(user, original.id) }
            }

            @Test
            fun `should download results if an assigned contest was added to the current competition`() {
                val user = testObserver { contests(listOf(19)) }
                val original = testCompetition { contests(listOf(20)) }
                every { competitions.findById(original.id) } returns original.withData { contests(listOf(20, 19)) }

                val result = operations.downloadResult(user, original.id).getOrThrow()

                assertEquals("competition-23-results.csv", result.uploadedFilename)
            }
        }

        @Nested
        inner class ModuleRuleTests {

            @Test
            fun `should propagate technical competition repository exceptions`() {
                val user = testObserver { contests(listOf(23)) }
                val failure = IllegalStateException("Storage unavailable")
                every { competitions.findById(CompetitionId(23)) } throws failure

                val result = assertThrows(IllegalStateException::class.java) {
                    operations.downloadResult(user = user, competitionId = CompetitionId(23))
                }

                assertSame(failure, result)
            }
        }
    }

    @Nested
    inner class ViewContestsTests {

        @Nested
        inner class HappyPathTests {

            @Test
            fun `should pass unique assigned contest ids and both filters with requested pagination`() {
                val user = testObserver { contests(listOf(23, 24, 23)) }
                val pagination = Pagination(
                    page = 0,
                    size = 2,
                    sort = Sort(listOf(Sort.Order("name", Sort.Direction.DESC))),
                )
                val filter = ObserverContestFilter(name = " Alpha%_\\ ", contestId = ContestId(19))
                val expected = Page(content = listOf(testContest()), pagination = pagination, totalElements = 1)
                every {
                    contests.findAvailableToObserver(
                        contestIds = setOf(ContestId(23), ContestId(24)),
                        pagination = pagination,
                        filter = filter,
                    )
                } returns expected

                val result = operations.viewContests(user = user, pagination = pagination, filter = filter).getOrThrow()

                assertSame(expected, result)
            }

            @ParameterizedTest
            @CsvSource("0,0", "4,7")
            fun `should retain empty repository pages and their totals`(pageIndex: Int, total: Long) {
                val user = testObserver { contests(listOf(23)) }
                val pagination = Pagination(page = pageIndex, size = 2)
                val expected = Page<Contest>(content = emptyList(), pagination = pagination, totalElements = total)
                every {
                    contests.findAvailableToObserver(setOf(ContestId(23)), pagination, ObserverContestFilter())
                } returns expected

                val result = operations.viewContests(user = user, pagination = pagination).getOrThrow()

                assertSame(expected, result)
            }

            @Test
            fun `should return an empty page when no contests are assigned`() {
                val user = testObserver()
                val pagination = Pagination(page = 0, size = 10)
                val expected = Page<Contest>(content = emptyList(), pagination = pagination, totalElements = 0)
                every { contests.findAvailableToObserver(emptySet(), pagination, ObserverContestFilter()) } returns expected

                val result = operations.viewContests(user = user, pagination = pagination).getOrThrow()

                assertSame(expected, result)
            }
        }

        @Nested
        inner class RefusalTests {

            @Test
            fun `should raise MissedObserverRoleError before reading contests if user is a Participant`() {
                val user = testParticipant()

                assertRaises(MissedObserverRoleError) {
                    operations.viewContests(user = user, pagination = Pagination(page = 0, size = 10))
                }

                verify { contests wasNot Called }
            }

            @Test
            fun `should raise MissedObserverRoleError before reading contests if user is a Supervisor`() {
                val user = testSupervisor()

                assertRaises(MissedObserverRoleError) {
                    operations.viewContests(user = user, pagination = Pagination(page = 0, size = 10))
                }

                verify { contests wasNot Called }
            }
        }

        @Nested
        inner class InvariantTests {

            @Test
            fun `should return a future contest with its time limits`() {
                val user = testObserver { contests(listOf(23)) }
                val pagination = Pagination(page = 0, size = 10)
                val contest = testContest {
                    startsAt = Instant.parse("2040-01-01T00:00:00Z")
                    contestDuration = Duration.ofHours(2)
                    attemptDuration = Duration.ofMinutes(30)
                }
                val expected = Page(content = listOf(contest), pagination = pagination, totalElements = 1)
                every {
                    contests.findAvailableToObserver(setOf(ContestId(23)), pagination, ObserverContestFilter())
                } returns expected

                val result = operations.viewContests(user = user, pagination = pagination).getOrThrow()

                assertSame(contest, result.content.single())
            }

            @Test
            fun `should not write contests when viewing them`() {
                val user = testObserver { contests(listOf(23)) }
                val pagination = Pagination(page = 0, size = 10)
                val expected = Page(content = listOf(testContest()), pagination = pagination, totalElements = 1)
                every {
                    contests.findAvailableToObserver(setOf(ContestId(23)), pagination, ObserverContestFilter())
                } returns expected

                operations.viewContests(user = user, pagination = pagination).getOrThrow()

                verify(exactly = 0) { contests.save(any<ContestData>()) }
                verify(exactly = 0) { contests.update(any<Contest>()) }
                verify(exactly = 0) { contests.removeById(any()) }
                verify(exactly = 0) { contests.removeByIds(any()) }
            }
        }

        @Nested
        inner class ModuleRuleTests {

            @Test
            fun `should propagate technical repository exceptions`() {
                val user = testObserver { contests(listOf(23)) }
                val pagination = Pagination(page = 0, size = 10)
                val failure = IllegalStateException("Storage unavailable")
                every {
                    contests.findAvailableToObserver(setOf(ContestId(23)), pagination, ObserverContestFilter())
                } throws failure

                val result = assertThrows(IllegalStateException::class.java) {
                    operations.viewContests(user = user, pagination = pagination)
                }

                assertSame(failure, result)
            }
        }
    }
}
