package tech.testsys.infra.database.api.persistence.adapter.task

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import tech.testsys.domain.builder.api.taskData
import tech.testsys.domain.contract.persistence.Pagination
import tech.testsys.domain.contract.persistence.Sort
import tech.testsys.domain.contract.persistence.TaskFilter
import tech.testsys.domain.contract.persistence.repository.TaskRepository
import tech.testsys.domain.model.group.CommunityId
import tech.testsys.domain.model.task.Task
import tech.testsys.domain.model.user.MultipleRoleUserId
import tech.testsys.infra.database.DatabaseIntegrationTests

class TaskPaginationQueryTests : DatabaseIntegrationTests() {

    @Autowired
    private lateinit var repository: TaskRepository

    @ParameterizedTest
    @ValueSource(strings = ["alpha", "ALPHA", "%", "_", "\\", "  "])
    fun `should match literal case insensitive substrings and preserve spaces`(substring: String) {
        val owner = fixtures.developer().id
        val expected = saveTask(ownerId = owner, name = "  Alpha%_\\Beta  ")
        saveTask(ownerId = owner, name = "Other plain name")

        val page = repository.findAvailableToDeveloper(
            ownerId = owner,
            communityIds = emptySet(),
            pagination = Pagination(page = 0, size = 1),
            filter = TaskFilter(name = substring),
        )

        assertEquals(listOf(expected.id), page.content.map { entity -> entity.id })
        assertEquals(1L, page.totalElements)
    }

    @ParameterizedTest
    @CsvSource(
        "alpha,true,NEW,match",
        "alpha,true,,match|otherState",
        "alpha,false,NEW,match|otherOwner",
        "alpha,false,,match|otherOwner|otherState",
        ",true,NEW,match|otherName",
        ",true,,match|otherName|otherState",
        ",false,NEW,match|otherName|otherOwner",
        ",false,,match|otherName|otherOwner|otherState",
    )
    fun `should combine all specified filters before paging and counting`(
        name: String?,
        byOwner: Boolean,
        state: TaskFilter.State?,
        expected: String,
    ) {
        val owner = fixtures.developer().id
        val community = fixtures.community().id
        val otherOwner = fixtures.developer().id
        val saved = mapOf(
            "match" to saveTask(ownerId = owner, name = "Alpha"),
            "otherName" to saveTask(ownerId = owner, name = "Beta"),
            "otherOwner" to saveTask(ownerId = otherOwner, name = "Alpha", communityIds = listOf(community)),
            "otherState" to saveTask(ownerId = owner, name = "Alpha", state = TaskFilter.State.COMMITTED),
        )
        saveTask(ownerId = otherOwner, name = "Alpha")

        val page = repository.findAvailableToDeveloper(
            ownerId = owner,
            communityIds = setOf(community),
            pagination = Pagination(page = 0, size = 10),
            filter = TaskFilter(name = name, ownerId = filterOwner(byOwner, owner), state = state),
        )

        assertEquals(expected.split("|").map { key -> saved.getValue(key).id }, page.content.map { entity -> entity.id })
        assertEquals(expected.split("|").size.toLong(), page.totalElements)
    }

    @Test
    fun `should apply an owner filter only within authorized entities`() {
        val owner = fixtures.developer().id
        val otherOwner = fixtures.developer().id
        val community = fixtures.community().id
        saveTask(ownerId = owner, name = "Alpha")
        val shared = saveTask(ownerId = otherOwner, name = "Alpha", communityIds = listOf(community))
        saveTask(ownerId = otherOwner, name = "Alpha")

        val page = repository.findAvailableToDeveloper(
            ownerId = owner,
            communityIds = setOf(community),
            pagination = Pagination(page = 0, size = 1),
            filter = TaskFilter(ownerId = otherOwner),
        )

        assertEquals(listOf(shared.id), page.content.map { entity -> entity.id })
        assertEquals(1L, page.totalElements)
    }

    @Test
    fun `should return an empty filtered page for a missing owner`() {
        val owner = fixtures.developer().id
        saveTask(ownerId = owner, name = "Alpha")

        val page = repository.findAvailableToDeveloper(
            ownerId = owner,
            communityIds = emptySet(),
            pagination = Pagination(page = 0, size = 1),
            filter = TaskFilter(ownerId = MultipleRoleUserId(-1)),
        )

        assertEquals(emptyList<Task>(), page.content)
        assertEquals(0L, page.totalElements)
        assertEquals(0, page.totalPages)
    }

    @ParameterizedTest
    @CsvSource("0,2,true", "1,1,false", "2,0,false")
    fun `should count distinct access matches and return partial or out of range pages`(index: Int, size: Int, hasNext: Boolean) {
        val owner = fixtures.developer().id
        val otherOwner = fixtures.developer().id
        val communities = listOf(fixtures.community().id, fixtures.community().id)
        val first = saveTask(ownerId = owner, name = "Alpha", communityIds = communities)
        val second = saveTask(ownerId = otherOwner, name = "Alpha", communityIds = communities)
        val third = saveTask(ownerId = owner, name = "Alpha")
        saveTask(ownerId = otherOwner, name = "Alpha")
        saveTask(ownerId = owner, name = "Excluded")
        val expected = listOf(first.id, second.id, third.id).drop(index * 2).take(2)
        val request = Pagination(page = index, size = 2)

        val page = repository.findAvailableToDeveloper(
            ownerId = owner,
            communityIds = communities.toSet(),
            pagination = request,
            filter = TaskFilter(name = "Alpha"),
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
        val first = saveTask(ownerId = owner, name = "Equal")
        val second = saveTask(ownerId = owner, name = "Equal")
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
        saveTask(ownerId = owner, name = "First")
        val last = saveTask(ownerId = owner, name = "Last")
        val request = Pagination(page = 0, size = 1, sort = Sort(listOf(Sort.Order("id", Sort.Direction.DESC))))

        val page = repository.findAvailableToDeveloper(ownerId = owner, communityIds = emptySet(), pagination = request)

        assertEquals(listOf(last.id), page.content.map { entity -> entity.id })
        assertEquals(2L, page.totalElements)
        assertSame(request, page.pagination)
    }

    @Test
    fun `should match all authorized names with an empty substring`() {
        val owner = fixtures.developer().id
        val first = saveTask(ownerId = owner, name = "First")
        val second = saveTask(ownerId = owner, name = "Second")

        val page = repository.findAvailableToDeveloper(
            ownerId = owner,
            communityIds = emptySet(),
            pagination = Pagination(page = 0, size = 2),
            filter = TaskFilter(name = ""),
        )

        assertEquals(listOf(first.id, second.id), page.content.map { entity -> entity.id })
        assertEquals(2L, page.totalElements)
        assertFalse(page.hasNext)
    }

    @Test
    fun `should return an empty page when a name filter matches no authorized entity`() {
        val owner = fixtures.developer().id
        saveTask(ownerId = owner, name = "First")

        val page = repository.findAvailableToDeveloper(
            ownerId = owner,
            communityIds = emptySet(),
            pagination = Pagination(page = 0, size = 1),
            filter = TaskFilter(name = "absent"),
        )

        assertTrue(page.content.isEmpty())
        assertEquals(0L, page.totalElements)
    }

    @ParameterizedTest
    @EnumSource(TaskFilter.State::class)
    fun `should select each lifecycle state while excluding other states`(state: TaskFilter.State) {
        val owner = fixtures.developer().id
        val saved = mapOf(
            TaskFilter.State.NEW to saveTask(ownerId = owner, name = "New", state = TaskFilter.State.NEW),
            TaskFilter.State.UNCOMMITTED to saveTask(ownerId = owner, name = "Uncommitted", state = TaskFilter.State.UNCOMMITTED),
            TaskFilter.State.COMMITTED to saveTask(ownerId = owner, name = "Committed", state = TaskFilter.State.COMMITTED),
        )
        val original = saved.getValue(state)
        val request = Pagination(page = 0, size = 1)

        val page = repository.findAvailableToDeveloper(
            ownerId = owner,
            communityIds = emptySet(),
            pagination = request,
            filter = TaskFilter(state = state),
        )

        assertEquals(listOf(original.id), page.content.map { task -> task.id })
        assertEquals(original.data.content.javaClass, page.content.single().data.content.javaClass)
        assertEquals(original.version, page.content.single().version)
        assertEquals(1L, page.totalElements)
        assertSame(request, page.pagination)
    }

    private fun filterOwner(byOwner: Boolean, owner: MultipleRoleUserId): MultipleRoleUserId? = if (byOwner) owner else null

    private fun saveTask(
        ownerId: MultipleRoleUserId,
        name: String,
        communityIds: List<CommunityId> = emptyList(),
        state: TaskFilter.State = TaskFilter.State.NEW,
    ): Task = repository.save(
        taskData {
            owner = ownerId
            this.name = name
            description = "Pagination query test"
            sharedTo = communityIds.toMutableList()
            when (state) {
                TaskFilter.State.NEW -> content.new {}
                TaskFilter.State.COMMITTED -> content.committed {
                    exercises = mutableListOf(fixtures.exercise().id)
                    statement = fixtures.statement().id
                }
                TaskFilter.State.UNCOMMITTED -> content.uncommitted(
                    wipBuilder = {},
                    lastCommittedBuilder = {
                        exercises = mutableListOf(fixtures.exercise().id)
                        statement = fixtures.statement().id
                    },
                )
            }
        },
    )
}
