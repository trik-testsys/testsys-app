package tech.testsys.web.ui.display

import com.vaadin.flow.component.html.Div
import com.vaadin.flow.component.html.NativeButton
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.dom.SignalBinding
import com.vaadin.flow.signals.Signal
import tech.testsys.web.ui.Bindable
import tech.testsys.web.ui.ElementHandle
import tech.testsys.web.ui.layout.BlockRowScope
import tech.testsys.web.ui.layout.ContentScope
import tech.testsys.web.ui.layout.Placement

/**
 * Application data of a contest card; the UI calculates no contest state.
 *
 * @property title the card heading.
 * @property format the format caption.
 * @property status the status caption.
 * @property tone the meaning of the status.
 * @property whenText the date or schedule caption.
 * @property tags additional application labels.
 * @property people the participant caption.
 * @property actionLabel the call to action caption.
 * @since %CURRENT_VERSION%
 */
data class ContestCardData(
    val title: String,
    val format: String,
    val status: String,
    val tone: Tone,
    val whenText: String,
    val tags: List<String> = emptyList(),
    val people: String = "",
    val actionLabel: String? = null,
)

/**
 * Handle of a contest card with separate card and action callbacks.
 *
 * @property data the application-provided card data.
 * @since %CURRENT_VERSION%
 */
class ContestCardHandle internal constructor(private val card: ContestCardDisplay, initial: ContestCardData) : ElementHandle(card) {
    private val state = Bindable(card.element, initial, card::present)
    var data: ContestCardData
        get() = state.value
        set(value) {
            state.value = value
        }

    /**
     * Binds card data to [signal].
     *
     * @since %CURRENT_VERSION%
     */
    fun bindData(signal: Signal<ContestCardData>): SignalBinding<ContestCardData> = state.bind(signal)

    /**
     * Replaces the card selection [listener].
     *
     * @since %CURRENT_VERSION%
     */
    fun onClick(listener: () -> Unit) {
        card.selected = listener
        card.updateInteraction()
    }

    /**
     * Replaces the call to action [listener].
     *
     * @since %CURRENT_VERSION%
     */
    fun onAction(listener: () -> Unit) {
        card.action = listener
    }
}

/**
 * Adds a contest card of [data], with independent interactions configured by [configure].
 *
 * @since %CURRENT_VERSION%
 */
fun ContentScope.contestCard(data: ContestCardData, configure: ContestCardHandle.() -> Unit = {}): ContestCardHandle {
    val card = ContestCardDisplay(data)
    add(card)
    return ContestCardHandle(card, data).apply(configure)
}

/**
 * Adds a contest card on [size] columns, or the remaining columns.
 *
 * @since %CURRENT_VERSION%
 */
fun BlockRowScope.contestCard(data: ContestCardData, size: Int? = null, configure: ContestCardHandle.() -> Unit = {}): ContestCardHandle =
    ContentScope(place(size, Div()), texts, Placement.Body).contestCard(data, configure)

internal class ContestCardDisplay(initial: ContestCardData) : Div() {
    var selected: (() -> Unit)? = null
    var action: () -> Unit = {}

    init {
        addClassName("ts-ccard")
        element.addEventListener("click") { selected?.invoke() }.setFilter("!event.target.closest('button')")
        element.executeJs(
            "this.addEventListener('keydown', e => { if (e.target === this && (e.key === ' ' || e.key === 'Enter')) e.preventDefault(); })",
        )
        element.addEventListener("keydown") { selected?.invoke() }
            .setFilter("event.target === element && (event.key === 'Enter' || event.key === ' ')")
        present(initial)
    }

    fun updateInteraction() {
        if (selected == null) {
            element.removeAttribute("tabindex")
            element.removeAttribute("role")
        } else {
            element.setAttribute("tabindex", "0")
            element.setAttribute("role", "link")
        }
    }

    fun present(data: ContestCardData) {
        removeAll()
        val cover = when (data.tone) {
            Tone.Info ->
                "info"
            Tone.Success -> "live"
            Tone.Neutral, Tone.Warning, Tone.Danger -> "neutral"
        }
        add(
            Div(
                Div(Span(data.format).apply { addClassName("ts-ccard__fmt") }, buildBadge(data.status, data.tone)).apply {
                    addClassName(
                        "ts-ccard__head",
                    )
                },
                Span(data.whenText).apply { addClassName("ts-ccard__when") },
            ).apply { addClassNames("ts-ccard__cover", "ts-ccard__cover--$cover") },
        )
        val tags = Div().apply { addClassName("ts-ccard__tags") }
        data.tags.forEach { caption -> tags.add(Span(caption).apply { addClassNames("ts-tag", "ts-tag--plain") }) }
        add(
            Div(
                Span(data.title).apply { addClassName("ts-ccard__title") },
                tags,
                Div(
                    Span(data.people).apply { addClassName("ts-ccard__people") },
                    NativeButton(data.actionLabel.orEmpty()).apply {
                        isVisible = data.actionLabel != null
                        element.setAttribute("type", "button")
                        addClassNames(
                            "ts-btn",
                            "ts-btn--sm",
                            "ts-btn--${if (cover == "info") "primary" else "secondary"}",
                        )
                        addClickListener { action() }
                    },
                ).apply { addClassName("ts-ccard__foot") },
            ).apply { addClassName("ts-ccard__body") },
        )
        element.setAttribute("aria-label", data.title)
    }
}

/**
 * Adds a contest card on [size] columns of the page slot, or the whole slot.
 *
 * @since %CURRENT_VERSION%
 */
fun tech.testsys.web.ui.layout.SlotRowScope.contestCard(
    data: ContestCardData,
    size: Int? = null,
    configure: ContestCardHandle.() -> Unit = {},
): ContestCardHandle {
    val card = ContestCardDisplay(data)
    placeElement(size, card)
    return ContestCardHandle(card, data).apply(configure)
}
