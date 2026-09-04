package tech.testsys.infra.database.internal.jpa.id

import tech.testsys.infra.database.internal.InternalDatabaseApi
import java.time.Clock
import java.time.Instant

/**
 * Snowflake-style id source: a zero sign bit, 5 reserved zero bits, 32 bits of seconds since [EPOCH] (2026-01-01), 10 bits
 * of [nodeId] and 16 bits of a per-second counter. Ids grow monotonically per instance: a clock moving backwards keeps the last
 * logical second and an exhausted counter waits for the real clock to pass it, which after a large rollback can take long.
 *
 * @param nodeId id of the running instance, 0 to [MAX_NODE_ID].
 * @param clock the time source, UTC system clock by default.
 * @since %CURRENT_VERSION%
 */
@InternalDatabaseApi
class SnowflakeIdGenerator(
    private val nodeId: Int,
    private val clock: Clock = Clock.systemUTC(),
) {

    private var lastSecond: Long = 0

    private var counter: Int = 0

    init {
        require(nodeId in 0..MAX_NODE_ID) { "Snowflake node id must be in 0..$MAX_NODE_ID, got $nodeId" }
    }

    /**
     * Issues the next id.
     *
     * @since %CURRENT_VERSION%
     */
    @Synchronized
    fun next(): Long {
        var second = maxOf(elapsedSeconds(), lastSecond)
        if (second != lastSecond) {
            counter = 0
        } else if (counter < MAX_COUNTER) {
            counter++
        } else {
            second = awaitSecondAfter(lastSecond)
            counter = 0
        }
        lastSecond = second
        return (second shl TIMESTAMP_SHIFT) or (nodeId.toLong() shl NODE_ID_SHIFT) or counter.toLong()
    }

    private fun elapsedSeconds(): Long {
        val now = clock.instant()
        check(!now.isBefore(EPOCH)) { "Clock $now is before the Snowflake epoch $EPOCH" }
        val elapsed = now.epochSecond - EPOCH.epochSecond
        check(elapsed <= MAX_TIMESTAMP) { "Clock $now is beyond the Snowflake timestamp range" }
        return elapsed
    }

    private fun awaitSecondAfter(second: Long): Long {
        var current = elapsedSeconds()
        while (current <= second) {
            Thread.sleep(WAIT_STEP_MILLIS)
            current = elapsedSeconds()
        }
        return current
    }

    companion object {

        val EPOCH: Instant = Instant.parse("2026-01-01T00:00:00Z")

        const val TIMESTAMP_BITS = 32
        const val NODE_ID_BITS = 10
        const val COUNTER_BITS = 16

        const val NODE_ID_SHIFT = COUNTER_BITS
        const val TIMESTAMP_SHIFT = NODE_ID_BITS + COUNTER_BITS

        const val MAX_TIMESTAMP = (1L shl TIMESTAMP_BITS) - 1
        const val MAX_NODE_ID = (1 shl NODE_ID_BITS) - 1
        const val MAX_COUNTER = (1 shl COUNTER_BITS) - 1

        private const val WAIT_STEP_MILLIS = 10L

        /**
         * Moment encoded in [id], at second resolution.
         *
         * @since %CURRENT_VERSION%
         */
        fun instantOf(id: Long): Instant = EPOCH.plusSeconds(id ushr TIMESTAMP_SHIFT)

        /**
         * Node id encoded in [id].
         *
         * @since %CURRENT_VERSION%
         */
        fun nodeIdOf(id: Long): Int = ((id ushr NODE_ID_SHIFT) and MAX_NODE_ID.toLong()).toInt()

        /**
         * Per-second counter encoded in [id].
         *
         * @since %CURRENT_VERSION%
         */
        fun counterOf(id: Long): Int = (id and MAX_COUNTER.toLong()).toInt()
    }
}
