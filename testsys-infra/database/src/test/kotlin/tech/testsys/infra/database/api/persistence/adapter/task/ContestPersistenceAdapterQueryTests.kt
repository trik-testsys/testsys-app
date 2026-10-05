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
import tech.testsys.infra.database.internal.jpa.entity.task.CommunityToContestId
import tech.testsys.infra.database.internal.jpa.entity.task.CommunityToContestJpaEntity
import tech.testsys.infra.database.internal.jpa.repository.task.CommunityToContestJpaEntityRepository
import tech.testsys.infra.database.internal.jpa.repository.task.ContestJpaEntityRepository

@OptIn(InternalDatabaseApi::class)
class ContestPersistenceAdapterQueryTests {

    private val jpaEntityRepository = mockk<ContestJpaEntityRepository>()
    private val communityToContestJpaEntityRepository = mockk<CommunityToContestJpaEntityRepository>()
    private val adapter = ContestPersistenceAdapter(
        jpaEntityRepository = jpaEntityRepository,
        taskToContestJpaEntityRepository = mockk(),
        communityToContestJpaEntityRepository = communityToContestJpaEntityRepository,
        trikStudioVersionJpaEntityRepository = mockk(),
    )

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun `should propagate storage exceptions when finding owned contests`(withCommunities: Boolean) {
        val failure = DataAccessResourceFailureException("Contest storage unavailable")
        val communities = communityIds(withCommunities)
        every { jpaEntityRepository.findAllByOwnerId(1) } throws failure

        val actual = Assertions.assertThrows(DataAccessResourceFailureException::class.java) {
            adapter.findAvailableToDeveloper(ownerId = MultipleRoleUserId(1), communityIds = communities)
        }

        Assertions.assertSame(failure, actual)
    }

    @Test
    fun `should propagate storage exceptions when finding shared associations`() {
        val failure = DataAccessResourceFailureException("Contest associations unavailable")
        every { jpaEntityRepository.findAllByOwnerId(1) } returns emptyList()
        every { communityToContestJpaEntityRepository.findAllByIdCommunityIdIn(setOf(2L)) } throws failure

        val actual = Assertions.assertThrows(DataAccessResourceFailureException::class.java) {
            adapter.findAvailableToDeveloper(ownerId = MultipleRoleUserId(1), communityIds = setOf(CommunityId(2)))
        }

        Assertions.assertSame(failure, actual)
    }

    @Test
    fun `should propagate storage exceptions when loading shared contests`() {
        val failure = DataAccessResourceFailureException("Shared contests unavailable")
        every { jpaEntityRepository.findAllByOwnerId(1) } returns emptyList()
        every { communityToContestJpaEntityRepository.findAllByIdCommunityIdIn(setOf(2L)) } returns listOf(
            CommunityToContestJpaEntity(id = CommunityToContestId(communityId = 2, contestId = 3)),
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
        verify(exactly = 0) { communityToContestJpaEntityRepository.findAllByIdCommunityIdIn(any()) }
        verify(exactly = 0) { jpaEntityRepository.findAllById(any()) }
    }

    @Test
    fun `should query all communities together and skip loading contests when no associations exist`() {
        every { jpaEntityRepository.findAllByOwnerId(1) } returns emptyList()
        every { communityToContestJpaEntityRepository.findAllByIdCommunityIdIn(setOf(2L, 3L)) } returns emptyList()

        val result = adapter.findAvailableToDeveloper(ownerId = MultipleRoleUserId(1), communityIds = setOf(CommunityId(2), CommunityId(3)))

        Assertions.assertEquals(emptyList<Any>(), result)
        verify(exactly = 1) { communityToContestJpaEntityRepository.findAllByIdCommunityIdIn(setOf(2L, 3L)) }
        verify(exactly = 0) { jpaEntityRepository.findAllById(any()) }
    }

    private fun communityIds(withCommunities: Boolean): Set<CommunityId> = if (withCommunities) setOf(CommunityId(2)) else emptySet()
}
