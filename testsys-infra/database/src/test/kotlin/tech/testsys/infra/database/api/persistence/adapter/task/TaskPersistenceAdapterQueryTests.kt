package tech.testsys.infra.database.api.persistence.adapter.task

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.dao.DataAccessResourceFailureException
import tech.testsys.domain.model.group.CommunityId
import tech.testsys.domain.model.user.MultipleRoleUserId
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.task.CommunityToTaskId
import tech.testsys.infra.database.internal.jpa.entity.task.CommunityToTaskJpaEntity
import tech.testsys.infra.database.internal.jpa.repository.task.CommunityToTaskJpaEntityRepository
import tech.testsys.infra.database.internal.jpa.repository.task.TaskJpaEntityRepository

@OptIn(InternalDatabaseApi::class)
class TaskPersistenceAdapterQueryTests {

    private val jpaEntityRepository = mockk<TaskJpaEntityRepository>()
    private val communityToTaskJpaEntityRepository = mockk<CommunityToTaskJpaEntityRepository>()
    private val adapter = TaskPersistenceAdapter(
        jpaEntityRepository = jpaEntityRepository,
        taskContentJpaEntityRepository = mockk(),
        communityToTaskJpaEntityRepository = communityToTaskJpaEntityRepository,
        versionBucketToTaskJpaEntityRepository = mockk(),
        testToTaskContentJpaEntityRepository = mockk(),
        developerSolutionToTaskContentJpaEntityRepository = mockk(),
        trikStudioVersionToTaskContentJpaEntityRepository = mockk(),
        trikStudioVersionJpaEntityRepository = mockk(),
    )

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun `should propagate storage exceptions when finding owned tasks`(withCommunities: Boolean) {
        val failure = DataAccessResourceFailureException("Task storage unavailable")
        val communities = communityIds(withCommunities)
        every { jpaEntityRepository.findAllByOwnerId(1) } throws failure

        val actual = Assertions.assertThrows(DataAccessResourceFailureException::class.java) {
            adapter.findAvailableToDeveloper(ownerId = MultipleRoleUserId(1), communityIds = communities)
        }

        Assertions.assertSame(failure, actual)
    }

    @Test
    fun `should propagate storage exceptions when finding shared associations`() {
        val failure = DataAccessResourceFailureException("Task associations unavailable")
        every { jpaEntityRepository.findAllByOwnerId(1) } returns emptyList()
        every { communityToTaskJpaEntityRepository.findAllByIdCommunityIdIn(setOf(2L)) } throws failure

        val actual = Assertions.assertThrows(DataAccessResourceFailureException::class.java) {
            adapter.findAvailableToDeveloper(ownerId = MultipleRoleUserId(1), communityIds = setOf(CommunityId(2)))
        }

        Assertions.assertSame(failure, actual)
    }

    @Test
    fun `should propagate storage exceptions when loading shared tasks`() {
        val failure = DataAccessResourceFailureException("Shared tasks unavailable")
        every { jpaEntityRepository.findAllByOwnerId(1) } returns emptyList()
        every { communityToTaskJpaEntityRepository.findAllByIdCommunityIdIn(setOf(2L)) } returns listOf(
            CommunityToTaskJpaEntity(id = CommunityToTaskId(communityId = 2, taskId = 3)),
        )
        every { jpaEntityRepository.findAllById(setOf(3L)) } throws failure

        val actual = Assertions.assertThrows(DataAccessResourceFailureException::class.java) {
            adapter.findAvailableToDeveloper(ownerId = MultipleRoleUserId(1), communityIds = setOf(CommunityId(2)))
        }

        Assertions.assertSame(failure, actual)
    }

    @Test
    fun `should skip shared queries when community identifiers are empty`() {
        every { jpaEntityRepository.findAllByOwnerId(1) } returns emptyList()

        val result = adapter.findAvailableToDeveloper(ownerId = MultipleRoleUserId(1), communityIds = emptySet())

        Assertions.assertEquals(emptyList<Any>(), result)
        verify(exactly = 0) { communityToTaskJpaEntityRepository.findAllByIdCommunityIdIn(any()) }
        verify(exactly = 0) { jpaEntityRepository.findAllById(any()) }
    }

    @Test
    fun `should query all communities together and skip loading tasks when no associations exist`() {
        every { jpaEntityRepository.findAllByOwnerId(1) } returns emptyList()
        every { communityToTaskJpaEntityRepository.findAllByIdCommunityIdIn(setOf(2L, 3L)) } returns emptyList()

        val result = adapter.findAvailableToDeveloper(ownerId = MultipleRoleUserId(1), communityIds = setOf(CommunityId(2), CommunityId(3)))

        Assertions.assertEquals(emptyList<Any>(), result)
        verify(exactly = 1) { communityToTaskJpaEntityRepository.findAllByIdCommunityIdIn(setOf(2L, 3L)) }
        verify(exactly = 0) { jpaEntityRepository.findAllById(any()) }
    }

    private fun communityIds(withCommunities: Boolean): Set<CommunityId> = if (withCommunities) setOf(CommunityId(2)) else emptySet()
}
