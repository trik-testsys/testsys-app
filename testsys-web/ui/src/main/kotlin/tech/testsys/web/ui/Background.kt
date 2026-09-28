package tech.testsys.web.ui

import com.vaadin.flow.component.UI
import com.vaadin.flow.function.SerializableRunnable
import com.vaadin.flow.server.VaadinService
import java.util.concurrent.Executor

/**
 * Runs work for a UI off the request thread and returns to the UI thread; tests may replace the executor.
 *
 * The module compiles without the servlet API, so members of `VaadinSession` are out of reach: the UI thread is told
 * by `UI.getCurrent()`, which Vaadin sets only while the session is locked for that UI.
 *
 * @property executorOverride executor that replaces the one of the Vaadin service when set; a test seam only, so
 * that tests run background work where they can wait for it. Production code never sets it.
 */
internal object Background {
    @Volatile
    var executorOverride: Executor? = null

    /**
     * The executor of background work: [executorOverride] if set, otherwise the one of the current Vaadin service,
     * so it is called in a UI thread.
     *
     * @throws IllegalStateException if called outside a UI thread and [executorOverride] is not set.
     */
    fun executor(): Executor =
        executorOverride ?: checkNotNull(VaadinService.getCurrent()) { "Background work must start in a UI thread" }.executor

    /**
     * Runs [action] in the UI thread of [ui]: at once if the current thread already works for [ui], otherwise queued
     * with `UI.access`. A UI detached from its session, now or before the queued action runs, skips [action]:
     * its page is gone.
     */
    fun inUi(ui: UI, action: () -> Unit) {
        when {
            ui.session == null -> Unit
            UI.getCurrent() === ui -> action()
            else -> ui.accessLater(SerializableRunnable { action() }, SerializableRunnable {}).run()
        }
    }
}
