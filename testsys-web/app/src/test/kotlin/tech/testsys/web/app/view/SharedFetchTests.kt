package tech.testsys.web.app.view

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SharedFetchTests {
    private var fetches = 0
    private val shared = SharedFetch { ++fetches }

    @Test
    fun `should share one fetch between the first loads of several blocks`() {
        val details = shared.forLoad()
        val table = shared.forLoad { value -> value * 10 }

        val loaded = details() to table()

        assertEquals(1 to 10, loaded)
        assertEquals(1, fetches)
    }

    @Test
    fun `should fetch again on a later load of a block`() {
        val table = shared.forLoad()
        table()

        val reloaded = table()

        assertEquals(2, reloaded)
        assertEquals(2, fetches)
    }

    @Test
    fun `should return the latest fetched data as value`() {
        val table = shared.forLoad()
        table()
        table()

        val value = shared.value

        assertEquals(2, value)
        assertEquals(2, fetches)
    }

    @Test
    fun `should keep the data of the later started fetch as value if an earlier one finishes after it`() {
        var count = 0
        lateinit var other: () -> Int
        // The second fetch reloads another block, which starts and finishes inside it.
        val nested = SharedFetch { (++count).also { fetched -> if (fetched == 2) other() } }
        val table = nested.forLoad()
        other = nested.forLoad()
        table()
        other()

        table()

        assertEquals(3, nested.value)
    }
}
