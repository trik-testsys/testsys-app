package tech.testsys.infra.database.api.persistence.adapter

import org.junit.jupiter.api.Test
import org.springframework.dao.OptimisticLockingFailureException
import tech.testsys.domain.model.DomainEntity
import tech.testsys.domain.model.DomainId
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * [PersistenceAdapterContractTests] of an adapter that supports `update`: updating with optimistic locking and
 * rejecting an entity that was not obtained from persistence. Subclasses also supply a way to modify an entity
 * and a detached copy of an entity.
 *
 * @param Data the data type a new entity is created from.
 * @param Id the id type of the entity.
 * @param Entity the domain entity type.
 */
abstract class UpdatablePersistenceAdapterContractTests<Data, Id : DomainId, Entity : DomainEntity<Id>> :
    PersistenceAdapterContractTests<Data, Id, Entity>() {

    /**
     * A copy of [entity] with different data but the same id and version.
     */
    protected abstract fun modified(entity: Entity): Entity

    /**
     * A copy of [entity] without the `version` token, as if the entity had never come from persistence.
     */
    protected abstract fun detached(entity: Entity): Entity

    @Test
    fun `should store the modified data, keep the creation time and bump the version on update`() {
        val saved = repository.save(newData())
        val storedCreatedAt = assertNotNull(repository.findById(saved.id)).createdAt
        val modified = modified(saved)

        val updated = repository.update(modified)

        assertEquals(saved.id, updated.id)
        assertTrue(
            assertNotNull(updated.version).value > assertNotNull(saved.version).value,
            "version ${updated.version} should be bumped",
        )
        assertEquals(storedCreatedAt, updated.createdAt)
        assertSameData(modified, updated)
        assertSameEntity(updated, assertNotNull(repository.findById(saved.id)))
    }

    @Test
    fun `should fail to update an entity with a stale version`() {
        val saved = repository.save(newData())
        repository.update(modified(saved))

        assertFailsWith<OptimisticLockingFailureException> { repository.update(modified(saved)) }
    }

    @Test
    fun `should fail to update an entity that was not obtained from persistence`() {
        val saved = repository.save(newData())

        val error = assertFailsWith<IllegalArgumentException> { repository.update(detached(saved)) }

        assertContains(error.message.orEmpty(), "entity was not obtained from persistence")
    }

    @Test
    fun `should fail to remove an entity with a stale version and keep it`() {
        val saved = repository.save(newData())
        val updated = repository.update(modified(saved))

        assertFailsWith<OptimisticLockingFailureException> { repository.remove(saved) }

        assertSameEntity(updated, assertNotNull(repository.findById(saved.id)))
    }

    @Test
    fun `should fail to remove an entity that was not obtained from persistence`() {
        val saved = repository.save(newData())

        assertFailsWith<IllegalArgumentException> { repository.remove(detached(saved)) }

        assertNotNull(repository.findById(saved.id))
    }

    @Test
    fun `should store every item of a list on update`() {
        val saved = repository.save(listOf(newData(), newData()))

        val updated = repository.update(saved.map { modified(it) })

        assertEquals(saved.map { it.id }, updated.map { it.id })
        updated.forEach { assertSameEntity(it, assertNotNull(repository.findById(it.id))) }
    }
}
