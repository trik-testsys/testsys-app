package tech.testsys.infra.database.api.persistence.adapter.task

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import tech.testsys.domain.builder.api.contestData
import tech.testsys.domain.contract.persistence.ContestFilter
import tech.testsys.domain.contract.persistence.Pagination
import tech.testsys.domain.contract.persistence.Sort
import tech.testsys.domain.contract.persistence.repository.ContestRepository
import tech.testsys.domain.model.group.CommunityId
import tech.testsys.domain.model.task.Contest
import tech.testsys.domain.model.user.MultipleRoleUserId
import tech.testsys.infra.database.DatabaseIntegrationTests

class ContestPaginationQueryTests : DatabaseIntegrationTests() {

    @Autowired
    private lateinit var repository: ContestRepository

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
            repository.findAvailableToDeveloper(
                ownerId = owner,
                communityIds = setOf(community),
                pagination = Pagination(page = 0, size = 1),
            )
        }
        val (twenty, twentyStatements) = withStatementCount {
            repository.findAvailableToDeveloper(
                ownerId = owner,
                communityIds = setOf(community),
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
        val owner = fixtures.developer().id
        val expected = saveContest(ownerId = owner, name = "  Alpha%_\\Beta  ")
        saveContest(ownerId = owner, name = "Other plain name")

        val page = repository.findAvailableToDeveloper(
            ownerId = owner,
            communityIds = emptySet(),
            pagination = Pagination(page = 0, size = 1),
            filter = ContestFilter(name = substring),
        )

        assertEquals(listOf(expected.id), page.content.map { entity -> entity.id })
        assertEquals(1L, page.totalElements)
    }

    @ParameterizedTest
    @CsvSource(
        "alpha,true,match",
        "alpha,false,match|otherOwner",
        ",true,match|otherName",
        ",false,match|otherName|otherOwner",
    )
    fun `should combine all specified filters before paging and counting`(name: String?, byOwner: Boolean, expected: String) {
        val owner = fixtures.developer().id
        val community = fixtures.community().id
        val otherOwner = fixtures.developer().id
        val saved = mapOf(
            "match" to saveContest(ownerId = owner, name = "Alpha"),
            "otherName" to saveContest(ownerId = owner, name = "Beta"),
            "otherOwner" to saveContest(ownerId = otherOwner, name = "Alpha", communityIds = listOf(community)),
        )
        saveContest(ownerId = otherOwner, name = "Alpha")

        val page = repository.findAvailableToDeveloper(
            ownerId = owner,
            communityIds = setOf(community),
            pagination = Pagination(page = 0, size = 10),
            filter = ContestFilter(name = name, ownerId = filterOwner(byOwner, owner)),
        )

        assertEquals(expected.split("|").map { key -> saved.getValue(key).id }, page.content.map { entity -> entity.id })
        assertEquals(expected.split("|").size.toLong(), page.totalElements)
    }

    @Test
    fun `should apply an owner filter only within authorized entities`() {
        val owner = fixtures.developer().id
        val otherOwner = fixtures.developer().id
        val community = fixtures.community().id
        saveContest(ownerId = owner, name = "Alpha")
        val shared = saveContest(ownerId = otherOwner, name = "Alpha", communityIds = listOf(community))
        saveContest(ownerId = otherOwner, name = "Alpha")

        val page = repository.findAvailableToDeveloper(
            ownerId = owner,
            communityIds = setOf(community),
            pagination = Pagination(page = 0, size = 1),
            filter = ContestFilter(ownerId = otherOwner),
        )

        assertEquals(listOf(shared.id), page.content.map { entity -> entity.id })
        assertEquals(1L, page.totalElements)
    }

    @Test
    fun `should return an empty filtered page for a missing owner`() {
        val owner = fixtures.developer().id
        saveContest(ownerId = owner, name = "Alpha")

        val page = repository.findAvailableToDeveloper(
            ownerId = owner,
            communityIds = emptySet(),
            pagination = Pagination(page = 0, size = 1),
            filter = ContestFilter(ownerId = MultipleRoleUserId(-1)),
        )

        assertEquals(emptyList<Contest>(), page.content)
        assertEquals(0L, page.totalElements)
        assertEquals(0, page.totalPages)
    }

    @ParameterizedTest
    @CsvSource("0,2,true", "1,1,false", "2,0,false")
    fun `should count distinct access matches and return partial or out of range pages`(index: Int, size: Int, hasNext: Boolean) {
        val owner = fixtures.developer().id
        val otherOwner = fixtures.developer().id
        val communities = listOf(fixtures.community().id, fixtures.community().id)
        val first = saveContest(ownerId = owner, name = "Alpha", communityIds = communities)
        val second = saveContest(ownerId = otherOwner, name = "Alpha", communityIds = communities)
        val third = saveContest(ownerId = owner, name = "Alpha")
        saveContest(ownerId = otherOwner, name = "Alpha")
        saveContest(ownerId = owner, name = "Excluded")
        val expected = listOf(first.id, second.id, third.id).drop(index * 2).take(2)
        val request = Pagination(page = index, size = 2)

        val page = repository.findAvailableToDeveloper(
            ownerId = owner,
            communityIds = communities.toSet(),
            pagination = request,
            filter = ContestFilter(name = "Alpha"),
        )

        assertEquals(expected, page.content.map { entity -> entity.id })
        assertEquals(size, page.content.size)
        assertEquals(3L, page.totalElements)
        assertEquals(2, page.totalPages)
        assertEquals(hasNext, page.hasNext)
        assertSame(request, page.pagination)
    }

    @ParameterizedTest
    @CsvSource("0,first", "1,second")
    fun `should break equal custom sort values by ascending id across pages`(index: Int, expected: String) {
        val owner = fixtures.developer().id
        val first = saveContest(ownerId = owner, name = "Equal")
        val second = saveContest(ownerId = owner, name = "Equal")
        val saved = mapOf("first" to first.id, "second" to second.id)
        val request = Pagination(page = index, size = 1, sort = Sort(listOf(Sort.Order("name", Sort.Direction.DESC))))

        val page = repository.findAvailableToDeveloper(ownerId = owner, communityIds = emptySet(), pagination = request)

        assertEquals(listOf(saved.getValue(expected)), page.content.map { entity -> entity.id })
        assertEquals(2L, page.totalElements)
        assertSame(request, page.pagination)
    }

    @Test
    fun `should preserve explicitly descending id ordering`() {
        val owner = fixtures.developer().id
        saveContest(ownerId = owner, name = "First")
        val last = saveContest(ownerId = owner, name = "Last")
        val request = Pagination(page = 0, size = 1, sort = Sort(listOf(Sort.Order("id", Sort.Direction.DESC))))

        val page = repository.findAvailableToDeveloper(ownerId = owner, communityIds = emptySet(), pagination = request)

        assertEquals(listOf(last.id), page.content.map { entity -> entity.id })
        assertEquals(2L, page.totalElements)
        assertSame(request, page.pagination)
    }

    @Test
    fun `should match all authorized names with an empty substring`() {
        val owner = fixtures.developer().id
        val first = saveContest(ownerId = owner, name = "First")
        val second = saveContest(ownerId = owner, name = "Second")

        val page = repository.findAvailableToDeveloper(
            ownerId = owner,
            communityIds = emptySet(),
            pagination = Pagination(page = 0, size = 2),
            filter = ContestFilter(name = ""),
        )

        assertEquals(listOf(first.id, second.id), page.content.map { entity -> entity.id })
        assertEquals(2L, page.totalElements)
        assertFalse(page.hasNext)
    }

    @Test
    fun `should return an empty page when a name filter matches no authorized entity`() {
        val owner = fixtures.developer().id
        saveContest(ownerId = owner, name = "First")

        val page = repository.findAvailableToDeveloper(
            ownerId = owner,
            communityIds = emptySet(),
            pagination = Pagination(page = 0, size = 1),
            filter = ContestFilter(name = "absent"),
        )

        assertTrue(page.content.isEmpty())
        assertEquals(0L, page.totalElements)
    }

    @Test
    fun `should filter shared communities before paging without expanding access`() {
        val owner = fixtures.developer().id
        val otherOwner = fixtures.developer().id
        val selected = fixtures.community().id
        val access = fixtures.community().id
        saveContest(ownerId = owner, name = "Alpha")
        val first = saveContest(ownerId = owner, name = "Alpha", communityIds = listOf(selected, access))
        val second = saveContest(ownerId = otherOwner, name = "Alpha", communityIds = listOf(selected, access))
        saveContest(ownerId = otherOwner, name = "Alpha", communityIds = listOf(selected))
        saveContest(ownerId = otherOwner, name = "Alpha", communityIds = listOf(access))
        saveContest(ownerId = owner, name = "Other", communityIds = listOf(selected))
        val filter = ContestFilter(name = "Alpha", communityId = selected)

        val page = repository.findAvailableToDeveloper(
            ownerId = owner,
            communityIds = setOf(access),
            pagination = Pagination(page = 1, size = 1),
            filter = filter,
        )

        assertEquals(listOf(second.id), page.content.map { entity -> entity.id })
        assertTrue(first.id.value < second.id.value)
        assertEquals(2L, page.totalElements)
        assertEquals(2, page.totalPages)
        assertFalse(page.hasNext)
    }

    @Test
    fun `should filter owned entities by community without requiring membership`() {
        val owner = fixtures.developer().id
        val selected = fixtures.community().id
        val expected = saveContest(ownerId = owner, name = "Alpha", communityIds = listOf(selected))
        saveContest(ownerId = owner, name = "Alpha")

        val page = repository.findAvailableToDeveloper(
            ownerId = owner,
            communityIds = emptySet(),
            pagination = Pagination(page = 0, size = 1),
            filter = ContestFilter(ownerId = owner, communityId = selected),
        )

        assertEquals(listOf(expected.id), page.content.map { entity -> entity.id })
        assertEquals(1L, page.totalElements)
    }

    @Test
    fun `should retain the community filtered total beyond the last page`() {
        val owner = fixtures.developer().id
        val selected = fixtures.community().id
        saveContest(ownerId = owner, name = "Alpha", communityIds = listOf(selected))

        val page = repository.findAvailableToDeveloper(
            ownerId = owner,
            communityIds = emptySet(),
            pagination = Pagination(page = 3, size = 1),
            filter = ContestFilter(communityId = selected),
        )

        assertTrue(page.content.isEmpty())
        assertEquals(1L, page.totalElements)
    }

    @Test
    fun `should return no entities for an unknown community`() {
        val owner = fixtures.developer().id
        saveContest(ownerId = owner, name = "Alpha", communityIds = listOf(fixtures.community().id))

        val page = repository.findAvailableToDeveloper(
            ownerId = owner,
            communityIds = emptySet(),
            pagination = Pagination(page = 0, size = 1),
            filter = ContestFilter(communityId = CommunityId(-1)),
        )

        assertTrue(page.content.isEmpty())
        assertEquals(0L, page.totalElements)
    }

    private fun filterOwner(byOwner: Boolean, owner: MultipleRoleUserId): MultipleRoleUserId? = if (byOwner) owner else null

    private fun saveContest(ownerId: MultipleRoleUserId, name: String, communityIds: List<CommunityId> = emptyList()): Contest =
        repository.save(
            contestData {
                owner = ownerId
                this.name = name
                description = "Pagination query test"
                sharedTo = communityIds.toMutableList()
                trikStudioVersion = fixtures.trikStudioVersion()
            },
        )
}
