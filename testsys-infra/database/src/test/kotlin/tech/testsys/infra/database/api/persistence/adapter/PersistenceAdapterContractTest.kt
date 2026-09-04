package tech.testsys.infra.database.api.persistence.adapter

import org.junit.jupiter.api.Test
import org.springframework.dao.OptimisticLockingFailureException
import tech.testsys.domain.contract.persistence.repository.EntityRepository
import tech.testsys.domain.model.DomainEntity
import tech.testsys.domain.model.DomainId
import tech.testsys.domain.model.EntityVersion
import tech.testsys.domain.model.LazyEntity
import tech.testsys.domain.model.LazyEntityList
import tech.testsys.infra.database.DatabaseIntegrationTest
import java.time.Duration
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Contract every persistence adapter fulfils as an [EntityRepository]: ids, versions, creation time, finding,
 * loading, updating with optimistic locking and removing. Subclasses supply the adapter, fresh data, a way to
 * modify an entity and a field-level comparison; entity-specific behaviour gets its own tests there.
 *
 * @param Data the data type a new entity is created from.
 * @param Id the id type of the entity.
 * @param Entity the domain entity type.
 */
abstract class PersistenceAdapterContractTest<Data, Id : DomainId, Entity : DomainEntity<Id>> : DatabaseIntegrationTest() {

    protected abstract val repository: EntityRepository<Data, Id, Entity>

    /**
     * Fresh data with unique values; prerequisites are created through [fixtures].
     */
    protected abstract fun newData(): Data

    /**
     * A copy of [entity] with different data but the same id and version.
     */
    protected abstract fun modified(entity: Entity): Entity

    protected abstract fun idOf(value: Long): Id

    /**
     * Asserts that [actual] carries the same data as [expected]; ids and versions are compared by the contract itself.
     */
    protected abstract fun assertSameData(expected: Entity, actual: Entity)

    protected fun assertSameEntity(expected: Entity, actual: Entity) {
        assertEquals(expected.id, actual.id)
        assertEquals(expected.version, actual.version)
        assertSameInstant(expected.createdAt, actual.createdAt)
        assertSameData(expected, actual)
    }

    /**
     * Instants survive a round trip only up to the precision of the timestamp column (the database rounds them),
     * so an in-memory instant is compared with its stored counterpart with a small tolerance.
     */
    protected fun assertSameInstant(expected: Instant, actual: Instant) {
        val difference = Duration.between(expected, actual).abs()
        assertTrue(difference <= INSTANT_TOLERANCE, "expected $expected within $INSTANT_TOLERANCE of $actual")
    }

    @Test
    fun `save assigns an id, the initial version and the creation time`() {
        val before = Instant.now()

        val saved = repository.save(newData())

        assertTrue(saved.id.value > 0)
        assertEquals(EntityVersion(0), saved.version)
        assertTrue(!saved.createdAt.isBefore(before.minus(TOLERANCE)) && !saved.createdAt.isAfter(Instant.now().plus(TOLERANCE)))
    }

    @Test
    fun `save stores every item of a list`() {
        val saved = repository.save(listOf(newData(), newData()))

        assertEquals(2, saved.map { it.id }.distinct().size)
        saved.forEach { assertSameEntity(it, assertNotNull(repository.findById(it.id))) }
    }

    @Test
    fun `findById returns the saved entity`() {
        val saved = repository.save(newData())

        val found = assertNotNull(repository.findById(saved.id))

        assertSameEntity(saved, found)
    }

    @Test
    fun `findById returns null for an unknown id`() {
        assertNull(repository.findById(idOf(UNKNOWN_ID)))
    }

    @Test
    fun `findByIds returns the found entities and skips unknown ids`() {
        val first = repository.save(newData())
        val second = repository.save(newData())

        val found = repository.findByIds(listOf(first.id, idOf(UNKNOWN_ID), second.id))

        assertEquals(setOf(first.id, second.id), found.map { it.id }.toSet())
        assertSameEntity(first, found.single { it.id == first.id })
    }

    @Test
    fun `load resolves a lazy reference`() {
        val saved = repository.save(newData())

        val loaded = repository.load(LazyEntity(saved.id))

        assertSameEntity(saved, loaded)
    }

    @Test
    fun `load fails for a missing reference`() {
        assertFailsWith<IllegalArgumentException> { repository.load(LazyEntity(idOf(UNKNOWN_ID))) }
    }

    @Test
    fun `load resolves a lazy list`() {
        val first = repository.save(newData())
        val second = repository.save(newData())

        val loaded = repository.load(LazyEntityList(listOf(first.id, second.id)))

        assertEquals(setOf(first.id, second.id), loaded.map { it.id }.toSet())
    }

    @Test
    fun `load fails when a lazy list references a missing entity`() {
        val saved = repository.save(newData())

        assertFailsWith<IllegalArgumentException> { repository.load(LazyEntityList(listOf(saved.id, idOf(UNKNOWN_ID)))) }
    }

    @Test
    fun `update stores the modified data, keeps the creation time and bumps the version`() {
        val saved = repository.save(newData())
        val storedCreatedAt = assertNotNull(repository.findById(saved.id)).createdAt
        val modified = modified(saved)

        val updated = repository.update(modified)

        assertEquals(saved.id, updated.id)
        assertTrue(updated.version.value > saved.version.value, "version ${updated.version} should be bumped")
        assertEquals(storedCreatedAt, updated.createdAt)
        assertSameData(modified, updated)
        assertSameEntity(updated, assertNotNull(repository.findById(saved.id)))
    }

    @Test
    fun `update with a stale version fails`() {
        val saved = repository.save(newData())
        repository.update(modified(saved))

        assertFailsWith<OptimisticLockingFailureException> { repository.update(modified(saved)) }
    }

    @Test
    fun `update stores every item of a list`() {
        val saved = repository.save(listOf(newData(), newData()))

        val updated = repository.update(saved.map { modified(it) })

        assertEquals(saved.map { it.id }, updated.map { it.id })
        updated.forEach { assertSameEntity(it, assertNotNull(repository.findById(it.id))) }
    }

    @Test
    fun `removeById deletes the entity`() {
        val saved = repository.save(newData())

        repository.removeById(saved.id)

        assertNull(repository.findById(saved.id))
    }

    @Test
    fun `removeByIds deletes every entity`() {
        val first = repository.save(newData())
        val second = repository.save(newData())

        repository.removeByIds(listOf(first.id, second.id))

        assertTrue(repository.findByIds(listOf(first.id, second.id)).isEmpty())
    }

    @Test
    fun `remove deletes the entity`() {
        val saved = repository.save(newData())

        repository.remove(saved)

        assertNull(repository.findById(saved.id))
    }

    @Test
    fun `remove deletes every entity of a list`() {
        val saved = repository.save(listOf(newData(), newData()))

        repository.remove(saved)

        assertTrue(repository.findByIds(saved.map { it.id }).isEmpty())
    }

    protected companion object {

        const val UNKNOWN_ID = -1L
        val TOLERANCE: Duration = Duration.ofSeconds(2)
        val INSTANT_TOLERANCE: Duration = Duration.ofMillis(10)
    }
}
