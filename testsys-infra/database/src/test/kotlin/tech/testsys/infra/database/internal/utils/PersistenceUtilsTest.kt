package tech.testsys.infra.database.internal.utils

import org.junit.jupiter.api.Test
import tech.testsys.infra.database.internal.InternalDatabaseApi
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(InternalDatabaseApi::class)
class PersistenceUtilsTest {

    private data class Row(val key: Int)

    private val deleted = mutableListOf<List<Row>>()
    private val saved = mutableListOf<List<Row>>()

    private fun sync(existing: List<Row>, target: List<Int>) = syncJoinTable(
        existing = existing,
        targetKeys = target,
        keyOf = Row::key,
        buildAssociation = ::Row,
        deleteAll = { deleted += it },
        saveAll = { saved += it },
    )

    @Test
    fun `rows absent from the target are deleted`() {
        sync(existing = listOf(Row(1), Row(2), Row(3)), target = listOf(2))

        assertEquals(listOf(listOf(Row(1), Row(3))), deleted)
        assertTrue(saved.isEmpty())
    }

    @Test
    fun `target keys without a row are saved once`() {
        sync(existing = listOf(Row(1)), target = listOf(1, 2, 2, 3))

        assertTrue(deleted.isEmpty())
        assertEquals(1, saved.size)
        assertEquals(setOf(Row(2), Row(3)), saved.single().toSet())
    }

    @Test
    fun `nothing is called when rows already match the target`() {
        sync(existing = listOf(Row(1), Row(2)), target = listOf(2, 1))

        assertTrue(deleted.isEmpty())
        assertTrue(saved.isEmpty())
    }

    @Test
    fun `an empty target deletes every row`() {
        sync(existing = listOf(Row(1), Row(2)), target = emptyList())

        assertEquals(listOf(listOf(Row(1), Row(2))), deleted)
        assertTrue(saved.isEmpty())
    }

    @Test
    fun `rows are replaced when target and existing are disjoint`() {
        sync(existing = listOf(Row(1)), target = listOf(2))

        assertEquals(listOf(listOf(Row(1))), deleted)
        assertEquals(listOf(listOf(Row(2))), saved)
    }
}
