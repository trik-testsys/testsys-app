package tech.testsys.web.devapp.showcase

import com.vaadin.flow.component.UI
import com.vaadin.flow.router.BeforeEnterEvent
import com.vaadin.flow.router.BeforeEnterObserver
import com.vaadin.flow.router.NotFoundException
import com.vaadin.flow.router.PageTitle
import com.vaadin.flow.router.Route
import com.vaadin.flow.signals.local.ValueSignal
import org.springframework.core.env.Environment
import tech.testsys.web.components.TestSysView
import tech.testsys.web.components.actions.action
import tech.testsys.web.components.texts.UiTexts
import java.time.Duration
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

private val CLOCK_PERIOD: Duration = Duration.ofSeconds(1)

private val CLOCK_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")

/**
 * Second showcase page: page head, block tabs and pills, a pagination, empty states, the load failure of a table,
 * action menus and live updates (a clock bound to a signal, background block loads and table refreshes); it opens only in the `dev`
 * profile.
 *
 * @since %CURRENT_VERSION%
 */
@Route("dev/showcase/states")
@PageTitle("Витрина: навигация и состояния")
class ShowcaseStatesView(texts: UiTexts, private val environment: Environment) : TestSysView(texts), BeforeEnterObserver {
    private val clock = ValueSignal(currentClockText())
    private var clockExecutor: ScheduledExecutorService? = null

    init {
        page(showcaseHeader()) {
            showcaseHead("Навигация и состояния")
            block(title = "Доступность") {
                actions {
                    action("Открыть примеры доступности") { onClick { UI.getCurrent().navigate(ShowcaseNestedView::class.java) } }
                }
            }
            tabsSection()
            wideMatrixSection()
            highlightedTableSection()
            pillsSection()
            paginationSection()
            emptySection()
            failureSection()
            menuSection()
            liveSection(clock)
        }
        addAttachListener { startClock() }
        addDetachListener { stopClock() }
    }

    override fun beforeEnter(event: BeforeEnterEvent) {
        if (!environment.matchesProfiles(DEV_PROFILE)) event.rerouteToError(NotFoundException::class.java)
    }

    /** Starts the clock executor, unless it is already running: an attach without a detach in between is a no-op. */
    private fun startClock() {
        if (clockExecutor != null) return
        val executor = Executors.newSingleThreadScheduledExecutor { runnable ->
            Thread(runnable, "showcase-clock").apply { isDaemon = true }
        }
        val periodMillis = CLOCK_PERIOD.toMillis()
        executor.scheduleAtFixedRate({ clock.set(currentClockText()) }, periodMillis, periodMillis, TimeUnit.MILLISECONDS)
        clockExecutor = executor
    }

    /** Stops the clock executor, unless it is not running: a detach without a matching attach is a no-op. */
    private fun stopClock() {
        clockExecutor?.shutdownNow()
        clockExecutor = null
    }
}

/** The current time as `HH:mm:ss`. */
private fun currentClockText(): String = LocalTime.now().format(CLOCK_FORMAT)
