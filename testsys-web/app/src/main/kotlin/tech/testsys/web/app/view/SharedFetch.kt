package tech.testsys.web.app.view

import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

/**
 * Data that several blocks of one page build read through [fetch]: the first loads of the blocks share one fetch, and
 * a later load of a block, e.g. a table refresh or a retry after a failure, fetches again.
 *
 * @param T the type of the fetched data.
 */
internal class SharedFetch<T>(private val fetch: () -> T) {
    private val first = lazy(fetch)
    private val started = AtomicLong()

    // The data of the latest started fetch among the finished ones, with its number; loads may run in several
    // background threads and finish out of order.
    @Volatile
    private var latest: Pair<Long, Lazy<T>> = 0L to first

    /** The data of the latest started fetch among the finished ones; the first read fetches it if no block has yet. */
    val value: T
        get() = latest.second.value

    /** Returns the fetch of one load: its first call takes the shared data, every later call fetches again. */
    fun forLoad(): () -> T {
        val isFirst = AtomicBoolean(true)
        return {
            if (isFirst.getAndSet(false)) {
                first.value
            } else {
                // The number is taken when the fetch starts in the background, not when the load starts in the UI:
                // refreshes come from separate clicks, so both orders match in practice; exact order needs it from load.
                val number = started.incrementAndGet()
                fetch().also { data -> synchronized(this) { if (number > latest.first) latest = number to lazyOf(data) } }
            }
        }
    }

    /** Returns the fetch of one load as [forLoad] does, giving the part of the data that [select] picks. */
    fun <R> forLoad(select: (T) -> R): () -> R {
        val load = forLoad()
        return { select(load()) }
    }
}
