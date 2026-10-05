@file:OptIn(InternalComponentsApi::class)

package tech.testsys.web.components.display

import com.vaadin.flow.component.html.Div
import com.vaadin.flow.component.html.NativeButton
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.dom.Element
import com.vaadin.flow.dom.SignalBinding
import com.vaadin.flow.signals.Signal
import tech.testsys.web.components.Bindable
import tech.testsys.web.components.ElementHandle
import tech.testsys.web.components.core.CssClass
import tech.testsys.web.components.core.DomEvent
import tech.testsys.web.components.core.DomEventFilter
import tech.testsys.web.components.core.ElementRole
import tech.testsys.web.components.core.ElementType
import tech.testsys.web.components.core.HtmlAttribute
import tech.testsys.web.components.core.InternalComponentsApi
import tech.testsys.web.components.core.addClassName
import tech.testsys.web.components.core.addClassNames
import tech.testsys.web.components.core.addEventListener
import tech.testsys.web.components.core.removeAttribute
import tech.testsys.web.components.core.setAttribute
import tech.testsys.web.components.core.setFilter
import tech.testsys.web.components.core.setRole
import tech.testsys.web.components.core.setType
import tech.testsys.web.components.layout.BlockRowScope
import tech.testsys.web.components.layout.ContentScope

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
    placeContent(size, Div()).contestCard(data, configure)

internal class ContestCardDisplay(initial: ContestCardData) : Div() {
    var selected: (() -> Unit)? = null
    var action: () -> Unit = {}

    init {
        addClassName(CssClass.Ccard)
        element.addEventListener(DomEvent.Click) { selected?.invoke() }.setFilter(DomEventFilter.CardClick)
        element.preventCardSelectionDefaults()
        element.addEventListener(DomEvent.KeyDown) { selected?.invoke() }
            .setFilter(DomEventFilter.CardKey)
        present(initial)
    }

    fun updateInteraction() {
        if (selected == null) {
            element.removeAttribute(HtmlAttribute.TabIndex)
            element.setRole(null)
        } else {
            element.setAttribute(HtmlAttribute.TabIndex, "0")
            element.setRole(ElementRole.Link)
        }
    }

    fun present(data: ContestCardData) {
        removeAll()
        val cover = when (data.tone) {
            Tone.Info -> CssClass.CcardCoverInfo
            Tone.Success -> CssClass.CcardCoverLive
            Tone.Neutral, Tone.Warning, Tone.Danger -> CssClass.CcardCoverNeutral
        }
        add(
            Div(
                Div(Span(data.format).apply { addClassName(CssClass.CcardFmt) }, buildBadge(data.status, data.tone)).apply {
                    addClassName(CssClass.CcardHead)
                },
                Span(data.whenText).apply { addClassName(CssClass.CcardWhen) },
            ).apply { addClassNames(CssClass.CcardCover, cover) },
        )
        val tags = Div().apply { addClassName(CssClass.CcardTags) }
        data.tags.forEach { caption -> tags.add(Span(caption).apply { addClassNames(CssClass.Tag, CssClass.TagPlain) }) }
        add(
            Div(
                Span(data.title).apply { addClassName(CssClass.CcardTitle) },
                tags,
                Div(
                    Span(data.people).apply { addClassName(CssClass.CcardPeople) },
                    NativeButton(data.actionLabel.orEmpty()).apply {
                        isVisible = data.actionLabel != null
                        element.setType(ElementType.Button)
                        addClassNames(
                            CssClass.Btn,
                            CssClass.BtnSm,
                            if (cover == CssClass.CcardCoverInfo) CssClass.BtnPrimary else CssClass.BtnSecondary,
                        )
                        addClickListener { action() }
                    },
                ).apply { addClassName(CssClass.CcardFoot) },
            ).apply { addClassName(CssClass.CcardBody) },
        )
        element.setAttribute(HtmlAttribute.AriaLabel, data.title)
    }
}

/**
 * Adds a contest card on [size] columns of the page slot, or the whole slot.
 *
 * @since %CURRENT_VERSION%
 */
fun tech.testsys.web.components.layout.SlotRowScope.contestCard(
    data: ContestCardData,
    size: Int? = null,
    configure: ContestCardHandle.() -> Unit = {},
): ContestCardHandle {
    val card = ContestCardDisplay(data)
    placeElement(size, card)
    return ContestCardHandle(card, data).apply(configure)
}

private fun Element.preventCardSelectionDefaults() = executeJs(
    "this.addEventListener('keydown', e => { if (e.target === this && (e.key === ' ' || e.key === 'Enter')) e.preventDefault(); })",
)
