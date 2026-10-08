package tech.testsys.operation.user

import io.mockk.Called
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import tech.testsys.domain.builder.api.supervisor
import tech.testsys.domain.builder.api.supervisorData
import tech.testsys.domain.contract.persistence.ObserverContestFilter
import tech.testsys.domain.contract.persistence.Page
import tech.testsys.domain.contract.persistence.Pagination
import tech.testsys.domain.contract.persistence.Sort
import tech.testsys.domain.contract.persistence.repository.ContestRepository
import tech.testsys.domain.model.group.CompetitionId
import tech.testsys.domain.model.task.Contest
import tech.testsys.domain.model.task.ContestData
import tech.testsys.domain.model.task.ContestId
import tech.testsys.domain.model.user.HashAlgorithm
import tech.testsys.operation.error.MissedObserverRoleError
import tech.testsys.operation.error.getOrThrow
import tech.testsys.operation.util.assertRaises
import tech.testsys.operation.util.testContest
import tech.testsys.operation.util.testObserver
import tech.testsys.operation.util.testParticipant
import java.time.Duration
import java.time.Instant

class ObserverOperationsTests {

    private val contests = mockk<ContestRepository>()
    private val operations = ObserverOperations(contestRepository = contests)

    @Nested
    inner class ViewContestsTests {

        @Test
        fun `should reject a participant before reading contests`() {
            val user = testParticipant()

            assertRaises(MissedObserverRoleError) {
                operations.viewContests(user = user, pagination = Pagination(page = 0, size = 10))
            }

            verify { contests wasNot Called }
        }

        @Test
        fun `should reject a supervisor before reading contests`() {
            val user = supervisor {
                id = 29
                createdAt = Instant.EPOCH
                data = supervisorData {
                    name = "Supervisor"
                    accessToken("supervisor", algorithm = HashAlgorithm.Identity)
                }
            }

            assertRaises(MissedObserverRoleError) {
                operations.viewContests(user = user, pagination = Pagination(page = 0, size = 10))
            }

            verify { contests wasNot Called }
        }

        @Test
        fun `should pass assigned competition ids and both filters with requested pagination`() {
            val user = testObserver { competitions(listOf(23, 24, 23)) }
            val pagination = Pagination(
                page = 0,
                size = 2,
                sort = Sort(listOf(Sort.Order("name", Sort.Direction.DESC))),
            )
            val filter = ObserverContestFilter(name = " Alpha%_\\ ", contestId = ContestId(19))
            val expected = Page(content = listOf(testContest()), pagination = pagination, totalElements = 1)
            every {
                contests.findAvailableToObserver(
                    competitionIds = setOf(CompetitionId(23), CompetitionId(24)),
                    pagination = pagination,
                    filter = filter,
                )
            } returns expected

            val result = operations.viewContests(user = user, pagination = pagination, filter = filter).getOrThrow()

            assertSame(expected, result)
            assertEquals(listOf(CompetitionId(23), CompetitionId(24), CompetitionId(23)), user.data.competitions.ids)
        }

        @ParameterizedTest
        @CsvSource("0,0", "4,7")
        fun `should retain empty repository pages and their totals`(pageIndex: Int, total: Long) {
            val user = testObserver { competitions(listOf(23)) }
            val pagination = Pagination(page = pageIndex, size = 2)
            val expected = Page<Contest>(content = emptyList(), pagination = pagination, totalElements = total)
            every {
                contests.findAvailableToObserver(setOf(CompetitionId(23)), pagination, ObserverContestFilter())
            } returns expected

            val result = operations.viewContests(user = user, pagination = pagination).getOrThrow()

            assertSame(expected, result)
        }

        @Test
        fun `should return an empty page when no competitions are assigned`() {
            val user = testObserver()
            val pagination = Pagination(page = 0, size = 10)
            val expected = Page<Contest>(content = emptyList(), pagination = pagination, totalElements = 0)
            every { contests.findAvailableToObserver(emptySet(), pagination, ObserverContestFilter()) } returns expected

            val result = operations.viewContests(user = user, pagination = pagination).getOrThrow()

            assertSame(expected, result)
        }

        @Test
        fun `should return temporal fields unchanged without writing contests`() {
            val user = testObserver { competitions(listOf(23)) }
            val pagination = Pagination(page = 0, size = 10)
            val contest = testContest {
                startsAt = Instant.parse("2040-01-01T00:00:00Z")
                contestDuration = Duration.ofHours(2)
                attemptDuration = Duration.ofMinutes(30)
            }
            val expected = Page(content = listOf(contest), pagination = pagination, totalElements = 1)
            every {
                contests.findAvailableToObserver(setOf(CompetitionId(23)), pagination, ObserverContestFilter())
            } returns expected

            val result = operations.viewContests(user = user, pagination = pagination).getOrThrow()

            assertSame(contest, result.content.single())
            assertEquals(Instant.parse("2040-01-01T02:00:00Z"), result.content.single().data.endsAt)
            verify(exactly = 0) { contests.save(any<ContestData>()) }
            verify(exactly = 0) { contests.update(any<Contest>()) }
            verify(exactly = 0) { contests.removeById(any()) }
            verify(exactly = 0) { contests.removeByIds(any()) }
        }

        @Test
        fun `should propagate technical repository exceptions`() {
            val user = testObserver { competitions(listOf(23)) }
            val pagination = Pagination(page = 0, size = 10)
            val failure = IllegalStateException("Storage unavailable")
            every {
                contests.findAvailableToObserver(setOf(CompetitionId(23)), pagination, ObserverContestFilter())
            } throws failure

            val result = assertThrows(IllegalStateException::class.java) {
                operations.viewContests(user = user, pagination = pagination)
            }

            assertSame(failure, result)
        }
    }
}
