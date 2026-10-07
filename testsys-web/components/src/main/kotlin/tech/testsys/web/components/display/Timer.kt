@file:OptIn(InternalComponentsApi::class)

package tech.testsys.web.components.display

import com.vaadin.flow.component.Html
import com.vaadin.flow.component.html.Div
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.dom.SignalBinding
import com.vaadin.flow.signals.Signal
import com.vaadin.flow.signals.local.ValueSignal
import tech.testsys.web.components.ElementHandle
import tech.testsys.web.components.core.Background
import tech.testsys.web.components.core.Bindable
import tech.testsys.web.components.core.CssClass
import tech.testsys.web.components.core.ElementRole
import tech.testsys.web.components.core.HtmlAttribute
import tech.testsys.web.components.core.InternalComponentsApi
import tech.testsys.web.components.core.addClassName
import tech.testsys.web.components.core.remove
import tech.testsys.web.components.core.setAttribute
import tech.testsys.web.components.core.setClassName
import tech.testsys.web.components.core.setRole
import tech.testsys.web.components.layout.BlockRowScope
import tech.testsys.web.components.layout.ContentScope
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

private const val DEFAULT_DANGER_MINUTES = 10L
private const val SECONDS_PER_MINUTE = 60L
private const val SECONDS_PER_HOUR = 3600L
private const val SECONDS_PER_DAY = 86_400L
private const val HOURS_PER_DAY = 24L

/**
 * Timer data: a static duration or a live deadline.
 *
 * @since %CURRENT_VERSION%
 */
sealed interface TimerValue {
    /**
     * An unchanging duration, limited to zero when negative.
     *
     * @property duration the shown remaining duration.
     * @since %CURRENT_VERSION%
     */
    data class Static(val duration: Duration) : TimerValue

    /**
     * A live deadline, updated only while attached.
     *
     * @property instant the moment at which the timer reaches zero.
     * @since %CURRENT_VERSION%
     */
    data class Until(val instant: Instant) : TimerValue
}

/**
 * Presentation of timer numbers.
 *
 * @since %CURRENT_VERSION%
 */
enum class TimerVariant { Chip, Hero, Tiles, Text }

/** Test seams keep countdown tests independent of wall time and sleeping. */
internal object TimerRuntime {
    var clock: Clock = Clock.systemUTC()
    private val scheduler = Executors.newSingleThreadScheduledExecutor { task ->
        Thread(
            task,
            "testsys-ui-timer",
        ).apply {
            isDaemon = true
        }
    }
    var schedule: (() -> Unit) -> AutoCloseable = { tick ->
        val future = scheduler.scheduleAtFixedRate(tick, 1, 1, TimeUnit.SECONDS)
        AutoCloseable { future.cancel(false) }
    }
}

/**
 * Adds a [value] countdown labelled by [label], in the chosen [variant].
 *
 * @since %CURRENT_VERSION%
 */
fun ContentScope.timer(
    label: String,
    value: TimerValue,
    variant: TimerVariant = TimerVariant.Chip,
    dangerBelow: Duration = Duration.ofMinutes(
        DEFAULT_DANGER_MINUTES,
    ),
    configure: TimerHandle.() -> Unit = {},
): TimerHandle {
    require(!dangerBelow.isNegative) { "Timer '$label' danger threshold must be nonnegative, got $dangerBelow" }
    val control = TimerDisplay(label, value, variant, texts, dangerBelow)
    add(control)
    return TimerHandle(control, value).apply(configure)
}

/**
 * Adds a timer on [size] columns, or the remaining columns.
 *
 * @since %CURRENT_VERSION%
 */
fun BlockRowScope.timer(
    label: String,
    value: TimerValue,
    size: Int? = null,
    variant: TimerVariant = TimerVariant.Chip,
    dangerBelow: Duration = Duration.ofMinutes(
        DEFAULT_DANGER_MINUTES,
    ),
    configure: TimerHandle.() -> Unit = {},
): TimerHandle = placeContent(size, Div()).timer(label, value, variant, dangerBelow, configure)

/**
 * Handle of a countdown and its read-only remaining-seconds signal.
 *
 * @property data the static duration or live deadline.
 * @property remainingSeconds the nonnegative rounded-down remaining seconds.
 * @since %CURRENT_VERSION%
 */
class TimerHandle internal constructor(private val display: TimerDisplay, initial: TimerValue) : ElementHandle(display) {
    private val state = Bindable(display.element, initial, display::present)
    var data: TimerValue
        get() = state.value
        set(value) {
            state.value = value
        }
    val remainingSeconds: Signal<Long> = display.remainingState.asReadonly()

    /**
     * Binds the deadline or duration to [signal].
     *
     * @since %CURRENT_VERSION%
     */
    fun bindData(signal: Signal<TimerValue>): SignalBinding<TimerValue> = state.bind(signal)
}

internal class TimerDisplay(
    label: String,
    private var current: TimerValue,
    private val variant: TimerVariant,
    private val texts: tech.testsys.web.components.texts.UiTexts,
    private val dangerBelow: Duration,
) : Div() {
    val remainingState = ValueSignal(0L)
    private var task: AutoCloseable? = null
    private var generation = 0L

    init {
        addClassName(CssClass.Timer)
        element.setAttribute(HtmlAttribute.AriaLabel, label)
        element.setRole(ElementRole.Timer)
        addAttachListener { start() }
        addDetachListener { stop() }
        show()
    }

    fun present(value: TimerValue) {
        stop()
        current = value
        show()
        if (isAttached) start()
    }

    internal fun remaining(): Long = when (val value = current) {
        is TimerValue.Static -> value.duration.seconds.coerceAtLeast(0)
        is TimerValue.Until -> Duration.between(TimerRuntime.clock.instant(), value.instant).seconds.coerceAtLeast(0)
    }

    private fun start() {
        stop()
        show()
        if (current !is TimerValue.Until || remaining() == 0L) return
        val ui = ui.orElseThrow()
        val token = generation
        task = TimerRuntime.schedule {
            Background.inUi(ui) {
                if (isAttached && generation == token) {
                    show()
                    if (remaining() == 0L) stop()
                }
            }
        }
    }

    private fun stop() {
        generation++
        task?.close()
        task = null
    }

    private fun show() {
        val seconds = remaining()
        remainingState.set(seconds)
        val hours = seconds / SECONDS_PER_HOUR
        val minutes = seconds / SECONDS_PER_MINUTE % SECONDS_PER_MINUTE
        val rest = seconds % SECONDS_PER_MINUTE
        fun pad(value: Long): String = value.toString().padStart(2, '0')
        removeAll()
        element.classList.remove(CssClass.TimerChip)
        element.classList.remove(CssClass.TimerParts)
        element.classList.remove(CssClass.TimerTiles)
        when (variant) {
            TimerVariant.Chip, TimerVariant.Text -> {
                if (variant == TimerVariant.Chip) addClassName(CssClass.TimerChip)
                if (variant == TimerVariant.Chip && seconds > dangerBelow.seconds) {
                    add(
                        tech.testsys.web.components.core.svgIcon(
                            tech.testsys.web.components.core.IconName.Clock,
                            tech.testsys.web.components.core.ICON_SIZE_SMALL,
                        ),
                    )
                }
                val caption = if (variant == TimerVariant.Chip && hours == 0L) {
                    "${pad(minutes)}:${pad(rest)}"
                } else {
                    "${pad(hours)}:${pad(minutes)}:${pad(rest)}"
                }
                add(Span(caption))
            }
            TimerVariant.Hero, TimerVariant.Tiles -> {
                val isTiles = variant == TimerVariant.Tiles
                addClassName(if (isTiles) CssClass.TimerTiles else CssClass.TimerParts)
                val values = if (isTiles) {
                    listOf(seconds / SECONDS_PER_DAY, hours % HOURS_PER_DAY, minutes, rest)
                } else {
                    listOf(hours, minutes, rest)
                }
                val units = if (isTiles) texts.components.timerUnits else texts.components.timerUnits.drop(1)
                values.forEachIndexed { index, number ->
                    val numeric = if (isTiles) {
                        number.timerTileNumber()
                    } else {
                        Span(pad(number)).apply { addClassName(CssClass.TimerNum) }
                    }
                    val unit = Span(units[index]).apply { addClassName(CssClass.TimerUnit) }
                    val tile = Div(numeric, unit).apply {
                        addClassName(if (isTiles) CssClass.TimerTile else CssClass.TimerPart)
                    }
                    add(tile)
                }
            }
        }
        setClassName(CssClass.TimerDanger, variant == TimerVariant.Chip && seconds <= dangerBelow.seconds)
    }
}

private fun Long.timerTileNumber(): Html = Html("<b>${toString().padStart(2, '0')}</b>")
