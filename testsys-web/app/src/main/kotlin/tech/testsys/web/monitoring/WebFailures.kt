package tech.testsys.web.monitoring

import com.vaadin.flow.server.DefaultErrorHandler
import tech.testsys.infra.monitoring.api.TechnicalFailures
import tech.testsys.operation.error.OperationException

/**
 * Counts unexpected exceptions of one web interface in [failures] with the interface name [source] at the stage [EVENT]
 * or [NAVIGATION] before they are shown. A failed operation and a connection closed by the client are not counted.
 *
 * @property source the name of the interface, `main` or `service`.
 * @since 1.0.0-beta.1
 */
class WebFailures(val source: String, private val failures: TechnicalFailures) {
    /**
     * Counts [failure] at [stage], [EVENT] or [NAVIGATION], unless it is an expected refusal or a client disconnect.
     *
     * @since 1.0.0-beta.1
     */
    fun record(stage: String, failure: Throwable) {
        if (causes(failure).any { throwable -> throwable is OperationException }) return
        if (DefaultErrorHandler.findRelevantThrowable(failure).javaClass.name in DefaultErrorHandler.SOCKET_EXCEPTIONS) return

        failures.record(source = source, stage = stage, failure = failure)
    }

    /**
     * Holds the stages [EVENT], for event handlers and `UI.access` tasks, and [NAVIGATION], for opening a page.
     *
     * @since 1.0.0-beta.1
     */
    companion object {
        const val EVENT: String = "event"

        const val NAVIGATION: String = "navigation"

        private fun causes(failure: Throwable): Sequence<Throwable> {
            val seen = mutableSetOf<Throwable>()
            return generateSequence(failure) { throwable -> throwable.cause }.takeWhile { throwable -> seen.add(throwable) }
        }
    }
}
