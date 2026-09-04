package tech.testsys.infra.database.internal.jpa.id

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import tech.testsys.infra.database.internal.InternalDatabaseApi
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.Collections
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

@OptIn(InternalDatabaseApi::class)
class SnowflakeIdGeneratorTest {

    private val clock = MutableClock(SnowflakeIdGenerator.EPOCH.plusSeconds(START_SECOND))

    private val generator = SnowflakeIdGenerator(nodeId = NODE_ID, clock = clock)

    @Test
    fun `encodes second, node id and counter into the id layout`() {
        val first = generator.next()
        val second = generator.next()

        assertEquals(SnowflakeIdGenerator.EPOCH.plusSeconds(START_SECOND), SnowflakeIdGenerator.instantOf(first))
        assertEquals(NODE_ID, SnowflakeIdGenerator.nodeIdOf(first))
        assertEquals(0, SnowflakeIdGenerator.counterOf(first))
        assertEquals(1, SnowflakeIdGenerator.counterOf(second))
        assertEquals(0L, first ushr USED_BITS, "the sign bit and the 5 reserved bits must be zero")
    }

    @Test
    fun `ids strictly increase within one second`() {
        val ids = List(SAMPLE_SIZE) { generator.next() }

        assertEquals(ids, ids.sorted())
        assertEquals(SAMPLE_SIZE, ids.toSet().size)
    }

    @Test
    fun `new second resets the counter`() {
        generator.next()
        generator.next()

        clock.advance(Duration.ofSeconds(1))
        val id = generator.next()

        assertEquals(SnowflakeIdGenerator.EPOCH.plusSeconds(START_SECOND + 1), SnowflakeIdGenerator.instantOf(id))
        assertEquals(0, SnowflakeIdGenerator.counterOf(id))
    }

    @Test
    fun `clock going backwards keeps the logical second`() {
        val before = generator.next()

        clock.now = SnowflakeIdGenerator.EPOCH.plusSeconds(START_SECOND - BACKWARDS_JUMP_SECONDS)
        val after = generator.next()

        assertTrue(after > before)
        assertEquals(SnowflakeIdGenerator.instantOf(before), SnowflakeIdGenerator.instantOf(after))
        assertEquals(1, SnowflakeIdGenerator.counterOf(after))
    }

    @Test
    @Timeout(value = TEST_TIMEOUT_SECONDS, unit = TimeUnit.SECONDS)
    fun `waits for the next second when the counter is exhausted`() {
        var last = 0L
        repeat(SnowflakeIdGenerator.MAX_COUNTER + 1) { last = generator.next() }

        val ticker = Thread {
            Thread.sleep(TICK_DELAY_MILLIS)
            clock.advance(Duration.ofSeconds(1))
        }
        ticker.start()
        val id = generator.next()
        ticker.join()

        assertEquals(SnowflakeIdGenerator.EPOCH.plusSeconds(START_SECOND + 1), SnowflakeIdGenerator.instantOf(id))
        assertEquals(0, SnowflakeIdGenerator.counterOf(id))
        assertTrue(id > last)
    }

    @Test
    fun `rejects node id out of range`() {
        assertFailsWith<IllegalArgumentException> { SnowflakeIdGenerator(nodeId = -1, clock = clock) }
        assertFailsWith<IllegalArgumentException> { SnowflakeIdGenerator(nodeId = SnowflakeIdGenerator.MAX_NODE_ID + 1, clock = clock) }
    }

    @Test
    fun `fails when the clock is before the epoch`() {
        clock.now = SnowflakeIdGenerator.EPOCH.minusSeconds(1)

        assertFailsWith<IllegalStateException> { generator.next() }
    }

    @Test
    fun `accepts boundary node ids and round-trips the maximal one`() {
        val minNodeId = SnowflakeIdGenerator(nodeId = 0, clock = clock).next()
        val maxNodeId = SnowflakeIdGenerator(nodeId = SnowflakeIdGenerator.MAX_NODE_ID, clock = clock).next()

        assertEquals(0, SnowflakeIdGenerator.nodeIdOf(minNodeId))
        assertEquals(SnowflakeIdGenerator.MAX_NODE_ID, SnowflakeIdGenerator.nodeIdOf(maxNodeId))
        assertEquals(0L, maxNodeId ushr USED_BITS, "the maximal node id must not spill into the reserved bits")
    }

    @Test
    @Timeout(value = TEST_TIMEOUT_SECONDS, unit = TimeUnit.SECONDS)
    fun `concurrent generation yields unique ids`() {
        val ids = Collections.synchronizedList(ArrayList<Long>(THREADS * IDS_PER_THREAD))
        val pool = Executors.newFixedThreadPool(THREADS)

        repeat(THREADS) {
            pool.execute { repeat(IDS_PER_THREAD) { ids.add(generator.next()) } }
        }
        pool.shutdown()
        assertTrue(pool.awaitTermination(POOL_TIMEOUT_SECONDS, TimeUnit.SECONDS))

        assertEquals(THREADS * IDS_PER_THREAD, ids.toSet().size)
    }

    /** Clock whose instant the test moves by hand; `@Volatile` so another thread can advance it. */
    private class MutableClock(@Volatile var now: Instant) : Clock() {
        override fun getZone(): ZoneId = ZoneOffset.UTC
        override fun withZone(zone: ZoneId): Clock = this
        override fun instant(): Instant = now
        fun advance(duration: Duration) {
            now = now.plus(duration)
        }
    }

    private companion object {
        const val NODE_ID = 7
        const val START_SECOND = 100L
        const val SAMPLE_SIZE = 1000
        const val USED_BITS = 58
        const val BACKWARDS_JUMP_SECONDS = 30L
        const val TICK_DELAY_MILLIS = 50L
        const val THREADS = 8
        const val IDS_PER_THREAD = 1000
        const val POOL_TIMEOUT_SECONDS = 30L
        const val TEST_TIMEOUT_SECONDS = 10L
    }
}
