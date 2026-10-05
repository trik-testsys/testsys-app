@file:OptIn(InternalComponentsApi::class)

package tech.testsys.web.components.navigation

import com.vaadin.flow.component.html.Div
import com.vaadin.flow.component.html.NativeButton
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.component.notification.Notification
import com.vaadin.flow.dom.Element
import tech.testsys.web.components.HeaderTexts
import tech.testsys.web.components.core.CssClass
import tech.testsys.web.components.core.CssTheme
import tech.testsys.web.components.core.DomEvent
import tech.testsys.web.components.core.DomEventData
import tech.testsys.web.components.core.ElementType
import tech.testsys.web.components.core.HtmlAttribute
import tech.testsys.web.components.core.ICON_SIZE_SMALL
import tech.testsys.web.components.core.IconName
import tech.testsys.web.components.core.InternalComponentsApi
import tech.testsys.web.components.core.add
import tech.testsys.web.components.core.addClassName
import tech.testsys.web.components.core.addClassNames
import tech.testsys.web.components.core.addEventData
import tech.testsys.web.components.core.addEventListener
import tech.testsys.web.components.core.get
import tech.testsys.web.components.core.setAttribute
import tech.testsys.web.components.core.setType
import tech.testsys.web.components.core.svgIcon
import java.util.concurrent.atomic.AtomicLong

private const val ARRIVAL_DURATION_MS = 6000

/** Owns one reusable arrival card; it never owns notification read state. */
internal class HeaderNotificationArrival(
    private val widget: Div,
    private val texts: HeaderTexts,
    private val onOpen: (String) -> Unit,
    private val onOpenList: () -> Unit,
) {
    private val revision = AtomicLong()
    private var selection: Selection = Selection.Hidden
    val title = Span().apply { addClassName(CssClass.ToastTitle) }
    private val description = Span().apply { addClassName(CssClass.ToastDesc) }
    private val open = NativeButton(texts.arrivalOpen).apply {
        addClassNames(CssClass.Btn, CssClass.BtnLink, CssClass.BtnSm)
        addClickListener { openDestination() }
    }
    private val dismiss = NativeButton().apply {
        addClassName(CssClass.ToastClose)
        element.setAttribute(HtmlAttribute.AriaLabel, texts.arrivalClose)
        element.setType(ElementType.Button)
        add(svgIcon(IconName.X, ICON_SIZE_SMALL))
        addClickListener { close() }
    }
    private val content = Div(title, description, open).apply { addClassName(CssClass.ToastText) }
    private val card = Div(content, dismiss).apply { addClassNames(CssClass.Toast, CssClass.ToastInfo, CssClass.HeaderArrival) }
    private val notification = Notification(card).apply {
        duration = 0
        position = Notification.Position.TOP_END
        isAssertive = false
        element.themeList.add(CssTheme.Toast)
        element.themeList.add(CssTheme.HeaderArrival)
    }
    val isOpen: Boolean
        get() = notification.isOpened

    init {
        card.element.addEventListener(DomEvent.HeaderArrivalExpired) { event ->
            if (event.eventData.get(DomEventData.DetailRevision).asLong() == revision.get()) close()
        }.addEventData(DomEventData.DetailRevision)
        notification.addOpenedChangeListener { event -> if (!event.isOpened) selection = Selection.Hidden }
    }

    fun show(values: List<HeaderNotification>) {
        if (values.isEmpty() || !widget.isAttached) return
        selection = if (values.size == 1) Selection.Single(values.single().key) else Selection.Batch(values.map { item -> item.key })
        render(values)
        notification.open()
        widget.element.showNotificationArrival(
            card = card.element,
            notification = notification.element,
            durationMillis = ARRIVAL_DURATION_MS,
            revision = revision.incrementAndGet(),
        )
    }

    fun refresh(values: List<HeaderNotification>) {
        when (val current = selection) {
            Selection.Hidden -> Unit
            is Selection.Single -> {
                val item = values.find { value -> value.key == current.key }
                if (item == null) close() else render(listOf(item))
            }
            is Selection.Batch -> if (values.none { value -> value.key in current.keys }) close()
        }
    }

    fun close() {
        selection = Selection.Hidden
        revision.incrementAndGet()
        notification.close()
        if (widget.isAttached) widget.element.hideNotificationArrival()
    }

    fun openDestination() {
        val chosen = selection
        close()
        when (chosen) {
            Selection.Hidden -> Unit
            is Selection.Single -> onOpen(chosen.key)
            is Selection.Batch -> onOpenList()
        }
    }

    private fun render(values: List<HeaderNotification>) {
        val single = values.singleOrNull()
        title.text = single?.label ?: texts.arrivalCount(values.size)
        description.text = single?.description ?: if (single == null) texts.arrivalBatchHint else ""
        description.isVisible = description.text.isNotEmpty()
    }

    private sealed interface Selection {
        data object Hidden : Selection
        class Single(val key: String) : Selection
        class Batch(val keys: List<String>) : Selection
    }
}

private fun Element.showNotificationArrival(card: Element, notification: Element, durationMillis: Int, revision: Long) = executeJs(
    "window.testsysHeaderArrivals.show(this, $0, $1, $2, $3)",
    card,
    notification,
    durationMillis,
    revision,
)

private fun Element.hideNotificationArrival() = executeJs("window.testsysHeaderArrivals.hide(this)")
