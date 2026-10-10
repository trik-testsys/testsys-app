package tech.testsys.infra.database.internal.jpa.entity

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import tech.testsys.infra.database.DatabaseIntegrationTests
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.task.CommunityToTaskJpaEntity
import tech.testsys.infra.database.internal.jpa.repository.task.CommunityToTaskJpaEntityRepository
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(InternalDatabaseApi::class)
class CompositeJpaEntityTests : DatabaseIntegrationTests() {

    @Autowired
    private lateinit var communityToTaskJpaEntityRepository: CommunityToTaskJpaEntityRepository

    @Test
    fun `should treat a created row as new until it is saved`() {
        val communityId = fixtures.community().id.value
        val row = CommunityToTaskJpaEntity(communityId = communityId, taskId = fixtures.task().id.value)
        val isNewBeforeSave = row.isNew

        communityToTaskJpaEntityRepository.save(row)

        assertTrue(isNewBeforeSave)
        assertFalse(row.isNew)
    }

    @Test
    fun `should treat a loaded row as stored`() {
        val taskId = fixtures.task().id.value
        communityToTaskJpaEntityRepository.save(CommunityToTaskJpaEntity(communityId = fixtures.community().id.value, taskId = taskId))

        val loaded = communityToTaskJpaEntityRepository.findAllByTaskId(taskId).single()

        assertFalse(loaded.isNew)
    }

    @Test
    fun `should insert one and twenty new rows with the same statement count`() {
        val communityIds = List(20) { fixtures.community().id.value }
        val oneRowTaskId = fixtures.task().id.value
        val twentyRowsTaskId = fixtures.task().id.value

        val oneRow = listOf(CommunityToTaskJpaEntity(communityId = communityIds.first(), taskId = oneRowTaskId))
        val twentyRows = communityIds.map { communityId -> CommunityToTaskJpaEntity(communityId = communityId, taskId = twentyRowsTaskId) }

        val (_, oneRowStatements) = withStatementCount { communityToTaskJpaEntityRepository.saveAll(oneRow) }
        val (_, twentyRowsStatements) = withStatementCount { communityToTaskJpaEntityRepository.saveAll(twentyRows) }

        val storedCommunityIds = communityToTaskJpaEntityRepository.findAllByTaskId(twentyRowsTaskId).map { row -> row.id.communityId }
        assertEquals(communityIds.toSet(), storedCommunityIds.toSet())
        assertEquals(oneRowStatements, twentyRowsStatements)
    }

    @Test
    fun `should delete a loaded row`() {
        val taskId = fixtures.task().id.value
        communityToTaskJpaEntityRepository.save(CommunityToTaskJpaEntity(communityId = fixtures.community().id.value, taskId = taskId))
        val loaded = communityToTaskJpaEntityRepository.findAllByTaskId(taskId).single()

        communityToTaskJpaEntityRepository.delete(loaded)

        assertEquals(emptyList(), communityToTaskJpaEntityRepository.findAllByTaskId(taskId))
    }
}
