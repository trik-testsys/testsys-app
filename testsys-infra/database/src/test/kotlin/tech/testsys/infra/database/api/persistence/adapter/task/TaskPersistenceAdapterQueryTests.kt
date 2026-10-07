package tech.testsys.infra.database.api.persistence.adapter.task

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.springframework.dao.DataAccessResourceFailureException
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.domain.Specification
import tech.testsys.domain.contract.persistence.Pagination
import tech.testsys.domain.contract.persistence.Sort
import tech.testsys.domain.model.group.CommunityId
import tech.testsys.domain.model.user.MultipleRoleUserId
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.task.TaskJpaEntity
import tech.testsys.infra.database.internal.jpa.repository.task.TaskJpaEntityRepository

@OptIn(InternalDatabaseApi::class)
class TaskPersistenceAdapterQueryTests {

    private val jpaEntityRepository = mockk<TaskJpaEntityRepository>()
    private val adapter = TaskPersistenceAdapter(
        jpaEntityRepository = jpaEntityRepository,
        taskContentJpaEntityRepository = mockk(),
        communityToTaskJpaEntityRepository = mockk(),
        versionBucketToTaskJpaEntityRepository = mockk(),
        exerciseToTaskContentJpaEntityRepository = mockk(),
        testToTaskContentJpaEntityRepository = mockk(),
        developerSolutionToTaskContentJpaEntityRepository = mockk(),
        trikStudioVersionToTaskContentJpaEntityRepository = mockk(),
        trikStudioVersionJpaEntityRepository = mockk(),
    )

    @Test
    fun `should propagate storage exceptions when finding a page`() {
        val failure = DataAccessResourceFailureException("Task storage unavailable")
        every { jpaEntityRepository.findAll(any<Specification<TaskJpaEntity>>(), any<Pageable>()) } throws failure

        val actual = assertThrows(DataAccessResourceFailureException::class.java) {
            adapter.findAvailableToDeveloper(
                ownerId = MultipleRoleUserId(1),
                communityIds = setOf(CommunityId(2)),
                pagination = Pagination(page = 0, size = 1),
            )
        }

        assertSame(failure, actual)
    }

    @ParameterizedTest
    @CsvSource("name,DESC,name:DESC id:ASC", "id,DESC,id:DESC")
    fun `should retain explicit order and add id only when not explicitly sorted`(field: String, direction: String, expected: String) {
        val requested =
            Pagination(page = 5, size = 2, sort = Sort(listOf(Sort.Order(field, Sort.Direction.valueOf(direction)))))
        val pageable = slot<Pageable>()
        every {
            jpaEntityRepository.findAll(any<Specification<TaskJpaEntity>>(), capture(pageable))
        } returns PageImpl(emptyList(), PageRequest.of(5, 2), 7)

        val page = adapter.findAvailableToDeveloper(
            ownerId = MultipleRoleUserId(1),
            communityIds = emptySet(),
            pagination = requested,
        )

        assertEquals(expected, pageable.captured.sort.joinToString(" ") { order -> "${order.property}:${order.direction}" })
        assertEquals(5, pageable.captured.pageNumber)
        assertEquals(2, pageable.captured.pageSize)
        assertEquals(emptyList<Any>(), page.content)
        assertEquals(7L, page.totalElements)
        assertSame(requested, page.pagination)
    }

    @Test
    fun `should default to id ascending without altering requested metadata`() {
        val requested = Pagination(page = 0, size = 2)
        val pageable = slot<Pageable>()
        every {
            jpaEntityRepository.findAll(any<Specification<TaskJpaEntity>>(), capture(pageable))
        } returns PageImpl(emptyList())

        val page = adapter.findAvailableToDeveloper(
            ownerId = MultipleRoleUserId(1),
            communityIds = emptySet(),
            pagination = requested,
        )

        assertEquals("id:ASC", pageable.captured.sort.joinToString(" ") { order -> "${order.property}:${order.direction}" })
        assertSame(requested, page.pagination)
        assertEquals(0L, page.totalElements)
        assertEquals(emptyList<Any>(), page.content)
    }
}
