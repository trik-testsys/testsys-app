package tech.testsys.infra.database.api.registry

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import tech.testsys.domain.contract.GradingNodeAddress
import tech.testsys.infra.database.DatabaseIntegrationTests
import tech.testsys.infra.settings.api.registry.StoredGradingNode
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class DatabaseGradingNodeStoreTests : DatabaseIntegrationTests() {
    @Autowired
    private lateinit var store: DatabaseGradingNodeStore

    private val first = GradingNodeAddress("grader-1:50051")
    private val second = GradingNodeAddress("grader-2:50051")

    @Test
    fun `should round trip records ordered by address`() {
        store.add(StoredGradingNode(second, isActive = false))
        store.add(StoredGradingNode(first, isActive = true))

        val nodes = store.read()

        assertEquals(listOf(StoredGradingNode(first, isActive = true), StoredGradingNode(second, isActive = false)), nodes)
    }

    @Test
    fun `should read an empty registry after all records are removed`() {
        store.add(StoredGradingNode(first, isActive = true))
        store.add(StoredGradingNode(second, isActive = false))
        store.remove(first)
        store.remove(second)

        val nodes = store.read()

        assertEquals(emptyList<StoredGradingNode>(), nodes)
    }

    @Nested
    inner class AddTests {
        @Test
        fun `should confirm a repeated add of the same record without a second row`() {
            store.add(StoredGradingNode(first, isActive = true))

            val isStored = store.add(StoredGradingNode(first, isActive = true))

            assertTrue(isStored)
            assertEquals(listOf(StoredGradingNode(first, isActive = true)), store.read())
        }

        @Test
        fun `should refuse an add when the address has a different record`() {
            store.add(StoredGradingNode(first, isActive = true))

            val isStored = store.add(StoredGradingNode(first, isActive = false))

            assertFalse(isStored)
            assertEquals(listOf(StoredGradingNode(first, isActive = true)), store.read())
        }

        @Test
        fun `should keep one row when the same address is added concurrently`() {
            val barrier = CyclicBarrier(2)
            val workers = Executors.newFixedThreadPool(2)
            try {
                val attempts = List(2) {
                    workers.submit<Boolean> {
                        barrier.await(5, TimeUnit.SECONDS)
                        store.add(StoredGradingNode(first, isActive = true))
                    }
                }

                val results = attempts.map { attempt -> attempt.get(10, TimeUnit.SECONDS) }

                assertEquals(listOf(true, true), results)
                assertEquals(listOf(StoredGradingNode(first, isActive = true)), store.read())
            } finally {
                workers.shutdownNow()
            }
        }
    }

    @Nested
    inner class SetActiveTests {
        @Test
        fun `should change participation of a stored node`() {
            store.add(StoredGradingNode(first, isActive = true))

            val isChanged = store.setActive(first, isActive = false)

            assertTrue(isChanged)
            assertEquals(listOf(StoredGradingNode(first, isActive = false)), store.read())
        }

        @Test
        fun `should report an absent address without creating a record`() {
            val isChanged = store.setActive(first, isActive = true)

            assertFalse(isChanged)
            assertEquals(emptyList<StoredGradingNode>(), store.read())
        }
    }

    @Nested
    inner class RemoveTests {
        @Test
        fun `should remove only the requested record`() {
            store.add(StoredGradingNode(first, isActive = true))
            store.add(StoredGradingNode(second, isActive = true))

            store.remove(first)

            assertEquals(listOf(StoredGradingNode(second, isActive = true)), store.read())
        }

        @Test
        fun `should ignore removal of an absent address`() {
            store.add(StoredGradingNode(second, isActive = true))

            store.remove(first)

            assertEquals(listOf(StoredGradingNode(second, isActive = true)), store.read())
        }
    }
}
