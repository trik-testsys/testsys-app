package tech.testsys.infra.database.api.persistence.adapter.group

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import tech.testsys.domain.builder.api.competitionData
import tech.testsys.domain.contract.persistence.CompetitionFilter
import tech.testsys.domain.contract.persistence.Pagination
import tech.testsys.domain.contract.persistence.Sort
import tech.testsys.domain.contract.persistence.repository.CompetitionRepository
import tech.testsys.domain.model.group.Competition
import tech.testsys.domain.model.group.CompetitionId
import tech.testsys.domain.model.user.MultipleRoleUserId
import tech.testsys.infra.database.DatabaseIntegrationTests
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.repository.group.CompetitionJpaEntityRepository
import java.time.Instant

@OptIn(InternalDatabaseApi::class)
class CompetitionPaginationQueryTests : DatabaseIntegrationTests() {

    @Autowired
    private lateinit var repository: CompetitionRepository

    @Autowired
    private lateinit var jpaEntityRepository: CompetitionJpaEntityRepository

    @ParameterizedTest
    @ValueSource(strings = ["alpha", "ALPHA", "%", "_", "\\", "  "])
    fun `should match literal case insensitive substrings and preserve spaces`(substring: String) {
        val owner = fixtures.manager().id
        val expected = saveCompetition(ownerId = owner, name = "  Alpha%_\\Beta  ")
        saveCompetition(ownerId = owner, name = "Other plain name")
        saveCompetition(ownerId = fixtures.manager().id, name = "  Alpha%_\\Beta  ")

        val page = repository.findAvailableToManager(
            ownerId = owner,
            pagination = Pagination(page = 0, size = 1),
            filter = CompetitionFilter(name = substring),
        )

        assertEquals(listOf(expected.id), page.content.map { entity -> entity.id })
        assertEquals(1L, page.totalElements)
    }

    @ParameterizedTest
    @CsvSource("0,2,true", "1,1,false", "2,0,false")
    fun `should exclude foreign competitions before paging and counting`(index: Int, size: Int, hasNext: Boolean) {
        val owner = fixtures.manager().id
        val otherOwner = fixtures.manager().id
        saveCompetition(ownerId = otherOwner, name = "Alpha")
        val first = saveCompetition(ownerId = owner, name = "Alpha")
        val second = saveCompetition(ownerId = owner, name = "Alpha")
        val third = saveCompetition(ownerId = owner, name = "Alpha")
        saveCompetition(ownerId = owner, name = "Excluded")
        saveCompetition(ownerId = otherOwner, name = "Alpha")
        val expected = listOf(first.id, second.id, third.id).drop(index * 2).take(2)
        val request = Pagination(page = index, size = 2)

        val page = repository.findAvailableToManager(
            ownerId = owner,
            pagination = request,
            filter = CompetitionFilter(name = "Alpha"),
        )

        assertEquals(expected, page.content.map { entity -> entity.id })
        assertEquals(size, page.content.size)
        assertEquals(3L, page.totalElements)
        assertEquals(2, page.totalPages)
        assertEquals(hasNext, page.hasNext)
        assertSame(request, page.pagination)
        assertTrue(page.content.all { entity -> entity.data.owner.id == owner })
    }

    @Test
    fun `should assemble competition details and participant count without changing participants`() {
        val owner = fixtures.manager()
        val contest = fixtures.contest().id
        val saved = repository.save(
            competitionData {
                this.owner = owner.id
                name = "Viewed competition"
                description = "Competition description"
                contests = mutableListOf(contest)
            },
        )
        val participants = listOf(fixtures.participant(saved).id, fixtures.participant(saved).id)
        fixtures.participant(fixtures.competition(owner = fixtures.manager()))

        val page = repository.findAvailableToManager(ownerId = owner.id, pagination = Pagination(page = 0, size = 1))

        val actual = page.content.single()
        assertEquals(saved.id, actual.id)
        assertEquals("Viewed competition", actual.data.name)
        assertEquals("Competition description", actual.data.description)
        assertEquals(owner.id, actual.data.owner.id)
        assertEquals(participants.toSet(), actual.data.participants.ids.toSet())
        assertEquals(2, actual.data.participants.ids.size)
        assertEquals(listOf(contest), actual.data.contests.ids)
        assertEquals(saved.version, actual.version)
        assertEquals(participants.toSet(), repository.findById(saved.id)?.data?.participants?.ids?.toSet())
    }

    @ParameterizedTest
    @CsvSource("20,,middle|last", ",20,first|middle", "20,30,middle|last", "20,20,middle")
    fun `should include each creation bound and leave unspecified bounds unrestricted`(from: Long?, to: Long?, expected: String) {
        val owner = fixtures.manager().id
        val saved = mapOf(
            "first" to saveCompetition(ownerId = owner, name = "Alpha", createdAt = Instant.ofEpochSecond(10)),
            "middle" to saveCompetition(ownerId = owner, name = "Alpha", createdAt = Instant.ofEpochSecond(20)),
            "last" to saveCompetition(ownerId = owner, name = "Alpha", createdAt = Instant.ofEpochSecond(30)),
        )
        saveCompetition(ownerId = fixtures.manager().id, name = "Alpha", createdAt = Instant.ofEpochSecond(20))
        val filter = CompetitionFilter(
            createdFrom = from?.let { Instant.ofEpochSecond(it) },
            createdTo = to?.let { Instant.ofEpochSecond(it) },
        )

        val page =
            repository.findAvailableToManager(ownerId = owner, pagination = Pagination(page = 0, size = 10), filter = filter)

        assertEquals(expected.split("|").map { key -> saved.getValue(key).id }, page.content.map { entity -> entity.id })
        assertEquals(expected.split("|").size.toLong(), page.totalElements)
    }

    @Test
    fun `should combine name and creation filters before paging and counting`() {
        val owner = fixtures.manager().id
        saveCompetition(ownerId = owner, name = "Alpha", createdAt = Instant.ofEpochSecond(9))
        saveCompetition(ownerId = owner, name = "Alpha", createdAt = Instant.ofEpochSecond(10))
        val expected = saveCompetition(ownerId = owner, name = "Alpha", createdAt = Instant.ofEpochSecond(20))
        saveCompetition(ownerId = owner, name = "Beta", createdAt = Instant.ofEpochSecond(15))
        saveCompetition(ownerId = owner, name = "Alpha", createdAt = Instant.ofEpochSecond(21))
        saveCompetition(ownerId = fixtures.manager().id, name = "Alpha", createdAt = Instant.ofEpochSecond(15))
        val filter =
            CompetitionFilter(name = "ALPHA", createdFrom = Instant.ofEpochSecond(10), createdTo = Instant.ofEpochSecond(20))

        val page =
            repository.findAvailableToManager(ownerId = owner, pagination = Pagination(page = 1, size = 1), filter = filter)

        assertEquals(listOf(expected.id), page.content.map { entity -> entity.id })
        assertEquals(2L, page.totalElements)
        assertFalse(page.hasNext)
    }

    @ParameterizedTest
    @CsvSource("0,first", "1,second")
    fun `should break equal custom sort values by ascending id across pages`(index: Int, expected: String) {
        val owner = fixtures.manager().id
        val first = saveCompetition(ownerId = owner, name = "Equal")
        val second = saveCompetition(ownerId = owner, name = "Equal")
        val saved = mapOf("first" to first.id, "second" to second.id)
        val request =
            Pagination(page = index, size = 1, sort = Sort(listOf(Sort.Order(field = "name", direction = Sort.Direction.DESC))))

        val page = repository.findAvailableToManager(ownerId = owner, pagination = request)

        assertEquals(listOf(saved.getValue(expected)), page.content.map { entity -> entity.id })
        assertEquals(2L, page.totalElements)
        assertSame(request, page.pagination)
    }

    @ParameterizedTest
    @ValueSource(strings = ["id", "createdAt", "name"])
    fun `should preserve explicitly descending ordering`(field: String) {
        val owner = fixtures.manager().id
        saveCompetition(ownerId = owner, name = "First", createdAt = Instant.ofEpochSecond(10))
        val last = saveCompetition(ownerId = owner, name = "Last", createdAt = Instant.ofEpochSecond(20))
        val request =
            Pagination(page = 0, size = 1, sort = Sort(listOf(Sort.Order(field = field, direction = Sort.Direction.DESC))))

        val page = repository.findAvailableToManager(ownerId = owner, pagination = request)

        assertEquals(listOf(last.id), page.content.map { entity -> entity.id })
        assertEquals(2L, page.totalElements)
        assertSame(request, page.pagination)
    }

    @Test
    fun `should match all owned names with an empty substring`() {
        val owner = fixtures.manager().id
        val first = saveCompetition(ownerId = owner, name = "First")
        val second = saveCompetition(ownerId = owner, name = "Second")
        saveCompetition(ownerId = fixtures.manager().id, name = "Foreign")

        val page = repository.findAvailableToManager(
            ownerId = owner,
            pagination = Pagination(page = 0, size = 2),
            filter = CompetitionFilter(name = ""),
        )

        assertEquals(listOf(first.id, second.id), page.content.map { entity -> entity.id })
        assertEquals(2L, page.totalElements)
        assertFalse(page.hasNext)
    }

    @ParameterizedTest
    @ValueSource(strings = ["absent", "Foreign"])
    fun `should return an empty page if no owned competition matches`(name: String) {
        val owner = fixtures.manager().id
        saveCompetition(ownerId = owner, name = "First")
        saveCompetition(ownerId = fixtures.manager().id, name = "Foreign")

        val page = repository.findAvailableToManager(
            ownerId = owner,
            pagination = Pagination(page = 0, size = 1),
            filter = CompetitionFilter(name = name),
        )

        assertEquals(emptyList<Competition>(), page.content)
        assertEquals(0L, page.totalElements)
        assertEquals(0, page.totalPages)
        assertFalse(page.hasNext)
    }

    @Test
    fun `should return an empty page when manager owns no competitions`() {
        val owner = fixtures.manager().id
        saveCompetition(ownerId = fixtures.manager().id, name = "Foreign")

        val page = repository.findAvailableToManager(ownerId = owner, pagination = Pagination(page = 0, size = 1))

        assertEquals(emptyList<Competition>(), page.content)
        assertEquals(0L, page.totalElements)
    }

    private fun saveCompetition(ownerId: MultipleRoleUserId, name: String, createdAt: Instant = Instant.EPOCH): Competition {
        val saved = repository.save(
            competitionData {
                owner = ownerId
                this.name = name
                description = "Pagination query test"
            },
        )
        setCreatedAt(id = saved.id, createdAt = createdAt)
        return saved
    }

    private fun setCreatedAt(id: CompetitionId, createdAt: Instant) {
        val entity = jpaEntityRepository.findById(id.value).orElseThrow()
        entity.createdAt = createdAt
        jpaEntityRepository.saveAndFlush(entity)
    }
}
