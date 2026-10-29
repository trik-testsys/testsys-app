package tech.testsys.web.app.config

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tech.testsys.operation.TaskValidationDispatcher
import java.time.Duration
import java.util.concurrent.CountDownLatch
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class TaskValidationDispatcherLifecycleTests {
    private val dispatcher = mockk<TaskValidationDispatcher>()
    private val started = CountDownLatch(1)
    private val stopping = CountDownLatch(1)

    // Releases the running task only when stop() starts waiting for it, after the queue has been dropped.
    private val executor = object : ThreadPoolExecutor(1, 1, 0, TimeUnit.MILLISECONDS, LinkedBlockingQueue()) {
        override fun awaitTermination(timeout: Long, unit: TimeUnit): Boolean {
            stopping.countDown()
            return super.awaitTermination(timeout, unit)
        }
    }
    private val lifecycle = TaskValidationDispatcherLifecycle(dispatcher, executor, Duration.ofSeconds(10))

    @Test
    fun `should start the dispatcher when started`() {
        every { dispatcher.start() } just runs

        lifecycle.start()

        verify(exactly = 1) { dispatcher.start() }
        assertTrue(lifecycle.isRunning)
    }

    @Test
    fun `should wait for the running task when stopped`() {
        val finished = AtomicBoolean(false)
        executor.execute(runningTask { finished.set(true) })
        started.await()

        lifecycle.stop()

        assertTrue(finished.get())
        assertTrue(executor.isTerminated)
    }

    @Test
    fun `should drop queued tasks when stopped`() {
        val queuedRan = AtomicBoolean(false)
        executor.execute(runningTask {})
        executor.execute { queuedRan.set(true) }
        started.await()

        lifecycle.stop()

        assertFalse(queuedRan.get())
    }

    private fun runningTask(onRelease: () -> Unit) = Runnable {
        started.countDown()
        stopping.await(10, TimeUnit.SECONDS)
        onRelease()
    }
}
