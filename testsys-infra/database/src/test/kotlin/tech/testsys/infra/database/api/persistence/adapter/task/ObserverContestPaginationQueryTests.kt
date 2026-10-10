package tech.testsys.infra.database.api.persistence.adapter.task

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.NullSource
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import tech.testsys.domain.builder.api.contestData
import tech.testsys.domain.builder.api.withData
import tech.testsys.domain.contract.persistence.ObserverContestFilter
import tech.testsys.domain.contract.persistence.Pagination
import tech.testsys.domain.contract.persistence.Sort
import tech.testsys.domain.contract.persistence.repository.CompetitionRepository
import tech.testsys.domain.contract.persistence.repository.ContestRepository
import tech.testsys.domain.model.group.Competition
import tech.testsys.domain.model.task.Contest
import tech.testsys.domain.model.task.ContestId
import tech.testsys.infra.database.DatabaseIntegrationTests
import java.time.Duration
import java.time.Instant

class ObserverContestPaginationQueryTests : DatabaseIntegrationTests() {

    @Autowired
    private lateinit var repository: ContestRepository

    @Autowired
    private lateinit var competitions: CompetitionRepository

    @Test
    fun `should assemble pages of one and twenty contests with the same statement count`() {
        val owner = fixtures.developer().id
        val community = fixtures.community().id
        val task = fixtures.task().id
        val version = fixtures.trikStudioVersion()
        val ids = List(21) {
            repository.save(
                contestData {
                    this.owner = owner
                    name = fixtures.unique("Contest")
                    description = "Pagination query test"
                    sharedTo = mutableListOf(community)
                    tasks = mutableListOf(task)
                    trikStudioVersion = version
                },
            ).id
        }

        val (one, oneStatements) = withStatementCount {
            repository.findAvailableToObserver(
                contestIds = ids.toSet(),
                pagination = Pagination(page = 0, size = 1),
            )
        }
        val (twenty, twentyStatements) = withStatementCount {
            repository.findAvailableToObserver(
                contestIds = ids.toSet(),
                pagination = Pagination(page = 0, size = 20),
            )
        }

        assertEquals(ids.take(1), one.content.map { contest -> contest.id })
        assertEquals(ids.take(20), twenty.content.map { contest -> contest.id })
        assertEquals(21L, one.totalElements)
        assertEquals(21L, twenty.totalElements)
        assertEquals(List(20) { listOf(community) }, twenty.content.map { contest -> contest.data.sharedTo.ids })
        assertEquals(List(20) { listOf(task) }, twenty.content.map { contest -> contest.data.tasks.ids })
        assertEquals(List(20) { version }, twenty.content.map { contest -> contest.data.trikStudioVersion })
        assertEquals(oneStatements, twentyStatements)
    }

    @ParameterizedTest
    @ValueSource(strings = ["alpha", "ALPHA", "%", "_", "\\", "  "])
    fun `should match literal case insensitive substrings and preserve spaces`(substring: String) {
        val expected = saveContest("  Alpha%_\\Beta  ")
        val other = saveContest("Other plain name")
        val assigned = saveCompetition(expected, other)

        val page = repository.findAvailableToObserver(
            contestIds = assigned.data.contests.ids.toSet(),
            pagination = Pagination(page = 0, size = 1),
            filter = ObserverContestFilter(name = substring),
        )

        assertEquals(listOf(expected.id), page.content.map { contest -> contest.id })
        assertEquals(1L, page.totalElements)
    }

    @ParameterizedTest
    @CsvSource("0,first|second,true", "1,third,false", "2,,false")
    fun `should count shared contests once and preserve totals outside page bounds`(index: Int, expected: String?, hasNext: Boolean) {
        val first = saveContest("Alpha first")
        val second = saveContest("Alpha second")
        val third = saveContest("Alpha third")
        val excludedByName = saveContest("Beta")
        val excludedByAccess = saveContest("Alpha inaccessible")
        val assigned = saveCompetition(first, second, excludedByName)
        val anotherAssigned = saveCompetition(first, third)
        saveCompetition(excludedByAccess)
        saveContest("Alpha unattached")
        val saved = mapOf("first" to first.id, "second" to second.id, "third" to third.id)
        val request = Pagination(page = index, size = 2)

        val page = repository.findAvailableToObserver(
            contestIds = (assigned.data.contests.ids + anotherAssigned.data.contests.ids).toSet(),
            pagination = request,
            filter = ObserverContestFilter(name = "Alpha"),
        )

        assertEquals(expected?.split("|")?.map { key -> saved.getValue(key) }.orEmpty(), page.content.map { contest -> contest.id })
        assertEquals(3L, page.totalElements)
        assertEquals(2, page.totalPages)
        assertEquals(hasNext, page.hasNext)
        assertSame(request, page.pagination)
    }

    @Test
    fun `should exclude an unassigned neighbor in the same competition`() {
        val assigned = saveContest("Assigned")
        val neighbor = saveContest("Neighbor")
        saveCompetition(assigned, neighbor)

        val page = repository.findAvailableToObserver(
            contestIds = setOf(assigned.id),
            pagination = Pagination(page = 0, size = 10),
        )

        assertEquals(listOf(assigned.id), page.content.map { contest -> contest.id })
        assertEquals(1L, page.totalElements)
    }

    @Test
    fun `should return an assigned contest without any competition`() {
        val assigned = saveContest("Assigned")

        val page = repository.findAvailableToObserver(
            contestIds = setOf(assigned.id),
            pagination = Pagination(page = 0, size = 10),
        )

        assertEquals(listOf(assigned.id), page.content.map { contest -> contest.id })
        assertEquals(1L, page.totalElements)
    }

    @Test
    fun `should return no contests when no contests are assigned`() {
        saveCompetition(saveContest("Alpha"))
        val request = Pagination(page = 0, size = 10)

        val page = repository.findAvailableToObserver(contestIds = emptySet(), pagination = request)

        assertEquals(emptyList<Contest>(), page.content)
        assertEquals(0L, page.totalElements)
        assertEquals(0, page.totalPages)
        assertSame(request, page.pagination)
    }

    @Test
    fun `should return no contests for a nonexistent assigned contest`() {
        saveCompetition(saveContest("Alpha"))

        val page = repository.findAvailableToObserver(
            contestIds = setOf(ContestId(-1)),
            pagination = Pagination(page = 0, size = 10),
        )

        assertTrue(page.content.isEmpty())
        assertEquals(0L, page.totalElements)
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = ["", "alpha", "ALPHA"])
    fun `should select the exact accessible contest with an optional matching name`(name: String?) {
        val first = saveContest("Alpha first")
        val selected = saveContest("Alpha second")
        val assigned = saveCompetition(first, selected)

        val page = repository.findAvailableToObserver(
            contestIds = assigned.data.contests.ids.toSet(),
            pagination = Pagination(page = 0, size = 1),
            filter = ObserverContestFilter(name = name, contestId = selected.id),
        )

        assertEquals(listOf(selected.id), page.content.map { contest -> contest.id })
        assertEquals(1L, page.totalElements)
        assertFalse(page.hasNext)
    }

    @Test
    fun `should return no contests when the exact id matches but the name does not`() {
        val selected = saveContest("Alpha")
        val assigned = saveCompetition(selected, saveContest("Beta"))

        val page = repository.findAvailableToObserver(
            contestIds = assigned.data.contests.ids.toSet(),
            pagination = Pagination(page = 0, size = 1),
            filter = ObserverContestFilter(name = "Beta", contestId = selected.id),
        )

        assertTrue(page.content.isEmpty())
        assertEquals(0L, page.totalElements)
    }

    @Test
    fun `should not expand access when an exact id belongs to another competition`() {
        val assigned = saveCompetition(saveContest("Alpha"))
        val inaccessible = saveContest("Alpha")
        saveCompetition(inaccessible)

        val page = repository.findAvailableToObserver(
            contestIds = assigned.data.contests.ids.toSet(),
            pagination = Pagination(page = 0, size = 1),
            filter = ObserverContestFilter(name = "alpha", contestId = inaccessible.id),
        )

        assertTrue(page.content.isEmpty())
        assertEquals(0L, page.totalElements)
    }

    @Test
    fun `should return no contests for a nonexistent exact id`() {
        val assigned = saveCompetition(saveContest("Alpha"))

        val page = repository.findAvailableToObserver(
            contestIds = assigned.data.contests.ids.toSet(),
            pagination = Pagination(page = 0, size = 1),
            filter = ObserverContestFilter(contestId = ContestId(-1)),
        )

        assertTrue(page.content.isEmpty())
        assertEquals(0L, page.totalElements)
    }

    @Test
    fun `should match every accessible name when the substring is empty`() {
        val first = saveContest("First")
        val second = saveContest("Second")
        val assigned = saveCompetition(first, second)

        val page = repository.findAvailableToObserver(
            contestIds = assigned.data.contests.ids.toSet(),
            pagination = Pagination(page = 0, size = 10),
            filter = ObserverContestFilter(name = ""),
        )

        assertEquals(listOf(first.id, second.id), page.content.map { contest -> contest.id })
        assertEquals(2L, page.totalElements)
    }

    @Test
    fun `should return no contests when no accessible name matches`() {
        val assigned = saveCompetition(saveContest("Alpha"))

        val page = repository.findAvailableToObserver(
            contestIds = assigned.data.contests.ids.toSet(),
            pagination = Pagination(page = 0, size = 1),
            filter = ObserverContestFilter(name = "missing"),
        )

        assertTrue(page.content.isEmpty())
        assertEquals(0L, page.totalElements)
    }

    @ParameterizedTest
    @CsvSource("0,first", "1,second", "2,last")
    fun `should apply custom ordering and break ties by ascending id across pages`(index: Int, expected: String) {
        val last = saveContest("Alpha")
        val first = saveContest("Beta")
        val second = saveContest("Beta")
        val assigned = saveCompetition(last, first, second)
        val saved = mapOf("first" to first.id, "second" to second.id, "last" to last.id)
        val request = Pagination(page = index, size = 1, sort = Sort(listOf(Sort.Order("name", Sort.Direction.DESC))))

        val page = repository.findAvailableToObserver(contestIds = assigned.data.contests.ids.toSet(), pagination = request)

        assertEquals(listOf(saved.getValue(expected)), page.content.map { contest -> contest.id })
        assertEquals(3L, page.totalElements)
        assertSame(request, page.pagination)
    }

    @Test
    fun `should preserve explicitly descending id ordering`() {
        val first = saveContest("First")
        val last = saveContest("Last")
        val assigned = saveCompetition(first, last)
        val request = Pagination(page = 0, size = 1, sort = Sort(listOf(Sort.Order("id", Sort.Direction.DESC))))

        val page = repository.findAvailableToObserver(contestIds = assigned.data.contests.ids.toSet(), pagination = request)

        assertEquals(listOf(last.id), page.content.map { contest -> contest.id })
        assertEquals(2L, page.totalElements)
        assertSame(request, page.pagination)
    }

    @Test
    fun `should preserve assigned contests when competition membership changes`() {
        val removed = saveContest("Removed")
        val added = saveContest("Added")
        val assigned = saveCompetition(removed)
        competitions.update(assigned.withData { contests = mutableListOf(added.id) })

        val page = repository.findAvailableToObserver(
            contestIds = assigned.data.contests.ids.toSet(),
            pagination = Pagination(page = 0, size = 10),
        )

        assertEquals(listOf(removed.id), page.content.map { contest -> contest.id })
        assertEquals(1L, page.totalElements)
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = ["2000-01-01T00:00:00Z", "2040-01-01T00:00:00Z"])
    fun `should retain past future and absent start times with their limits`(start: String?) {
        val startsAt = start?.let(Instant::parse)
        val contest = repository.update(
            saveContest("Timed").withData {
                this.startsAt = startsAt
                contestDuration = Duration.ofHours(2)
                attemptDuration = Duration.ofMinutes(30)
            },
        )
        val assigned = saveCompetition(contest)

        val page = repository.findAvailableToObserver(
            contestIds = assigned.data.contests.ids.toSet(),
            pagination = Pagination(page = 0, size = 1),
        )

        val actual = page.content.single()
        assertEquals(contest.id, actual.id)
        assertEquals("Timed", actual.data.name)
        assertEquals(startsAt, actual.data.startsAt)
        assertEquals(startsAt?.plusSeconds(7200), actual.data.endsAt)
        assertEquals(Duration.ofHours(2), actual.data.contestDuration)
        assertEquals(Duration.ofMinutes(30), actual.data.attemptDuration)
        assertEquals(contest.version, actual.version)
    }

    @Test
    fun `should preserve absent time limits`() {
        val contest = saveContest("Unlimited")
        val assigned = saveCompetition(contest)

        val page = repository.findAvailableToObserver(
            contestIds = assigned.data.contests.ids.toSet(),
            pagination = Pagination(page = 0, size = 1),
        )

        val actual = page.content.single()
        assertEquals("Unlimited", actual.data.name)
        assertEquals(null, actual.data.startsAt)
        assertEquals(null, actual.data.endsAt)
        assertEquals(null, actual.data.contestDuration)
        assertEquals(null, actual.data.attemptDuration)
        assertEquals(contest.version, actual.version)
    }

    private fun saveContest(name: String): Contest = repository.save(
        contestData {
            owner = fixtures.developer().id
            this.name = name
            description = "Observer pagination query test"
            trikStudioVersion = fixtures.trikStudioVersion()
        },
    )

    private fun saveCompetition(vararg contests: Contest): Competition = competitions.update(
        fixtures.competition().withData { this.contests = contests.map { contest -> contest.id }.toMutableList() },
    )
}
