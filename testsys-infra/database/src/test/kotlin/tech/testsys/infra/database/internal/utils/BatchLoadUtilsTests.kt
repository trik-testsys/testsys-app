package tech.testsys.infra.database.internal.utils

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import tech.testsys.infra.database.internal.InternalDatabaseApi

@OptIn(InternalDatabaseApi::class)
class BatchLoadUtilsTests {

    private data class Link(val ownerId: Long, val linkedId: Long)

    private val requestedChunks = mutableListOf<List<Long>>()

    private fun findLinks(rows: List<Link>): (List<Long>) -> List<Link> = { ownerIds ->
        requestedChunks += ownerIds
        rows.filter { row -> row.ownerId in ownerIds }
    }

    @Nested
    inner class FindAllInChunksTests {

        @Test
        fun `should return no rows without calling find if ids are empty`() {
            val rows = findAllInChunks(ids = emptyList(), find = findLinks(listOf(Link(ownerId = 1, linkedId = 10))))

            assertEquals(emptyList<Link>(), rows)
            assertEquals(emptyList<List<Long>>(), requestedChunks)
        }

        @Test
        fun `should pass ids to find in chunks of at most the chunk size`() {
            findAllInChunks(ids = listOf(1L, 2L, 3L, 4L, 5L), chunkSize = 2, find = findLinks(emptyList()))

            assertEquals(listOf(listOf(1L, 2L), listOf(3L, 4L), listOf(5L)), requestedChunks)
        }

        @Test
        fun `should request a repeated id once`() {
            findAllInChunks(ids = listOf(1L, 2L, 1L), find = findLinks(emptyList()))

            assertEquals(listOf(listOf(1L, 2L)), requestedChunks)
        }

        @Test
        fun `should concatenate rows of all chunks in chunk order`() {
            val links = listOf(Link(ownerId = 3, linkedId = 30), Link(ownerId = 1, linkedId = 10))

            val rows = findAllInChunks(ids = listOf(1L, 3L), chunkSize = 1, find = findLinks(links))

            assertEquals(listOf(Link(ownerId = 1, linkedId = 10), Link(ownerId = 3, linkedId = 30)), rows)
        }

        @Test
        fun `should reject a chunk size that is not positive`() {
            assertThrows(IllegalArgumentException::class.java) {
                findAllInChunks(ids = listOf(1L), chunkSize = 0, find = findLinks(emptyList()))
            }
        }
    }

    @Nested
    inner class FindLinkedIdsTests {

        @Test
        fun `should group linked ids by owner in the order of the found rows`() {
            val links = listOf(
                Link(ownerId = 1, linkedId = 12),
                Link(ownerId = 2, linkedId = 20),
                Link(ownerId = 1, linkedId = 11),
            )

            val linkedIds = findLinkedIds(
                ownerIds = listOf(1L, 2L),
                find = findLinks(links),
                ownerIdOf = Link::ownerId,
                linkedIdOf = Link::linkedId,
            )

            assertEquals(mapOf(1L to listOf(12L, 11L), 2L to listOf(20L)), linkedIds)
        }

        @Test
        fun `should map an owner without rows to an empty list`() {
            val linkedIds = findLinkedIds(
                ownerIds = listOf(1L, 2L),
                find = findLinks(listOf(Link(ownerId = 1, linkedId = 10))),
                ownerIdOf = Link::ownerId,
                linkedIdOf = Link::linkedId,
            )

            assertEquals(mapOf(1L to listOf(10L), 2L to emptyList()), linkedIds)
        }

        @Test
        fun `should return an empty map without calling find if owner ids are empty`() {
            val linkedIds = findLinkedIds(
                ownerIds = emptyList(),
                find = findLinks(listOf(Link(ownerId = 1, linkedId = 10))),
                ownerIdOf = Link::ownerId,
                linkedIdOf = Link::linkedId,
            )

            assertEquals(emptyMap<Long, List<Long>>(), linkedIds)
            assertEquals(emptyList<List<Long>>(), requestedChunks)
        }

        @Test
        fun `should read linked ids of owners split into several chunks`() {
            val links = listOf(Link(ownerId = 1, linkedId = 10), Link(ownerId = 3, linkedId = 30))

            val linkedIds = findLinkedIds(
                ownerIds = listOf(1L, 2L, 3L),
                find = findLinks(links),
                ownerIdOf = Link::ownerId,
                linkedIdOf = Link::linkedId,
                chunkSize = 2,
            )

            assertEquals(mapOf(1L to listOf(10L), 2L to emptyList(), 3L to listOf(30L)), linkedIds)
            assertEquals(listOf(listOf(1L, 2L), listOf(3L)), requestedChunks)
        }
    }
}
