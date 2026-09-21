package tech.testsys.infra.database.api.persistence.adapter

import org.junit.jupiter.api.Test
import org.springframework.dao.OptimisticLockingFailureException
import tech.testsys.domain.contract.persistence.repository.EntityRepository
import tech.testsys.domain.model.DomainEntity
import tech.testsys.domain.model.DomainId
import tech.testsys.domain.model.EntityVersion
import tech.testsys.domain.model.LazyEntity
import tech.testsys.domain.model.LazyEntityList
import tech.testsys.infra.database.DatabaseIntegrationTests
import java.time.Duration
import java.time.Instant
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Contract every persistence adapter fulfils as an [EntityRepository]: ids, versions, creation time, finding,
 * loading, updating with optimistic locking and removing. Subclasses supply the adapter, fresh data, a way to
 * modify an entity, a detached copy of an entity and a field-level comparison; entity-specific behaviour gets
 * its own tests there.
 *
 * @param Data the data type a new entity is created from.
 * @param Id the id type of the entity.
 * @param Entity the domain entity type.
 */
abstract class PersistenceAdapterContractTests<Data, Id : DomainId, Entity : DomainEntity<Id>> : DatabaseIntegrationTests() {

    protected abstract val repository: EntityRepository<Data, Id, Entity>

    /**
     * Fresh data with unique values; prerequisites are created through [fixtures].
     */
    protected abstract fun newData(): Data

    /**
     * A copy of [entity] with different data but the same id and version.
     */
    protected abstract fun modified(entity: Entity): Entity

    /**
     * A copy of [entity] without the `version` token, as if the entity had never come from persistence.
     */
    protected abstract fun detached(entity: Entity): Entity

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
    fun `should assign an id, the initial version and the creation time on save`() {
        val before = Instant.now()

        val saved = repository.save(newData())

        assertTrue(saved.id.value > 0)
        assertEquals(EntityVersion(0), saved.version)
        assertTrue(!saved.createdAt.isBefore(before.minus(TOLERANCE)) && !saved.createdAt.isAfter(Instant.now().plus(TOLERANCE)))
    }

    @Test
    fun `should store every item of a list on save`() {
        val saved = repository.save(listOf(newData(), newData()))

        assertEquals(2, saved.map { it.id }.distinct().size)
        saved.forEach { assertSameEntity(it, assertNotNull(repository.findById(it.id))) }
    }

    @Test
    fun `should return the saved entity by id`() {
        val saved = repository.save(newData())

        val found = assertNotNull(repository.findById(saved.id))

        assertSameEntity(saved, found)
    }

    @Test
    fun `should return null if the id is unknown`() {
        assertNull(repository.findById(idOf(UNKNOWN_ID)))
    }

    @Test
    fun `should return the found entities and skip unknown ids when searching by ids`() {
        val first = repository.save(newData())
        val second = repository.save(newData())

        val found = repository.findByIds(listOf(first.id, idOf(UNKNOWN_ID), second.id))

        assertEquals(setOf(first.id, second.id), found.map { it.id }.toSet())
        assertSameEntity(first, found.single { it.id == first.id })
    }

    @Test
    fun `should resolve a lazy reference on load`() {
        val saved = repository.save(newData())

        val loaded = repository.load(LazyEntity(saved.id))

        assertSameEntity(saved, loaded)
    }

    @Test
    fun `should fail to load a missing reference`() {
        assertFailsWith<IllegalArgumentException> { repository.load(LazyEntity(idOf(UNKNOWN_ID))) }
    }

    @Test
    fun `should resolve a lazy list on load`() {
        val first = repository.save(newData())
        val second = repository.save(newData())

        val loaded = repository.load(LazyEntityList(listOf(first.id, second.id)))

        assertEquals(setOf(first.id, second.id), loaded.map { it.id }.toSet())
    }

    @Test
    fun `should fail to load a lazy list if it references a missing entity`() {
        val saved = repository.save(newData())

        assertFailsWith<IllegalArgumentException> { repository.load(LazyEntityList(listOf(saved.id, idOf(UNKNOWN_ID)))) }
    }

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
    fun `should store every item of a list on update`() {
        val saved = repository.save(listOf(newData(), newData()))

        val updated = repository.update(saved.map { modified(it) })

        assertEquals(saved.map { it.id }, updated.map { it.id })
        updated.forEach { assertSameEntity(it, assertNotNull(repository.findById(it.id))) }
    }

    @Test
    fun `should delete the entity by id`() {
        val saved = repository.save(newData())

        repository.removeById(saved.id)

        assertNull(repository.findById(saved.id))
    }

    @Test
    fun `should delete every entity by ids`() {
        val first = repository.save(newData())
        val second = repository.save(newData())

        repository.removeByIds(listOf(first.id, second.id))

        assertTrue(repository.findByIds(listOf(first.id, second.id)).isEmpty())
    }

    @Test
    fun `should delete the given entity`() {
        val saved = repository.save(newData())

        repository.remove(saved)

        assertNull(repository.findById(saved.id))
    }

    @Test
    fun `should delete every entity of a given list`() {
        val saved = repository.save(listOf(newData(), newData()))

        repository.remove(saved)

        assertTrue(repository.findByIds(saved.map { it.id }).isEmpty())
    }

    protected companion object {

        const val UNKNOWN_ID = -1L
        val TOLERANCE: Duration = Duration.ofSeconds(2)
        val INSTANT_TOLERANCE: Duration = Duration.ofNanos(1_000)
    }
}
