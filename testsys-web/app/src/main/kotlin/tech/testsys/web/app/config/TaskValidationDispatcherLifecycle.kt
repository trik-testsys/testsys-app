package tech.testsys.web.app.config

import org.springframework.context.SmartLifecycle
import tech.testsys.operation.TaskValidationDispatcher
import java.time.Duration
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

/**
 * Starts [dispatcher] with the context and, before the grader and the database close, drops the queued tasks
 * of [executor], which [TaskValidationDispatcher.start] resumes after a restart, and waits up to [stopTimeout]
 * for the running task without interrupting it, since an interrupt would end its request as a technical failure.
 *
 * @since %CURRENT_VERSION%
 */
class TaskValidationDispatcherLifecycle(
    private val dispatcher: TaskValidationDispatcher,
    private val executor: ThreadPoolExecutor,
    private val stopTimeout: Duration,
) : SmartLifecycle {
    @Volatile
    private var isStarted = false

    override fun start() {
        dispatcher.start()
        isStarted = true
    }

    override fun stop() {
        executor.shutdown()
        executor.queue.clear()
        executor.awaitTermination(stopTimeout.toMillis(), TimeUnit.MILLISECONDS)
        isStarted = false
    }

    override fun isRunning(): Boolean = isStarted
}
