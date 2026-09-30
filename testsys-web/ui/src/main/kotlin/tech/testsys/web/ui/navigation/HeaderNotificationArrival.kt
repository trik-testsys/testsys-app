package tech.testsys.web.ui.navigation

import com.vaadin.flow.component.html.Div
import com.vaadin.flow.component.html.NativeButton
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.component.notification.Notification
import tech.testsys.web.ui.HeaderTexts
import tech.testsys.web.ui.core.ICON_SIZE_SMALL
import tech.testsys.web.ui.core.IconName
import tech.testsys.web.ui.core.svgIcon
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
    val title = Span().apply { addClassName("ts-toast__title") }
    private val description = Span().apply { addClassName("ts-toast__desc") }
    private val open = NativeButton(texts.arrivalOpen).apply {
        addClassNames("ts-btn", "ts-btn--link", "ts-btn--sm")
        addClickListener { openDestination() }
    }
    private val dismiss = NativeButton().apply {
        addClassName("ts-toast__close")
        element.setAttribute("aria-label", texts.arrivalClose)
        element.setAttribute("type", "button")
        add(svgIcon(IconName.X, ICON_SIZE_SMALL))
        addClickListener { close() }
    }
    private val content = Div(title, description, open).apply { addClassName("ts-toast__text") }
    private val card = Div(content, dismiss).apply { addClassNames("ts-toast", "ts-toast--info", "ts-header-arrival") }
    private val notification = Notification(card).apply {
        duration = 0
        position = Notification.Position.TOP_END
        isAssertive = false
        element.themeList.addAll(listOf("ts-toast", "ts-header-arrival"))
    }
    val isOpen: Boolean
        get() = notification.isOpened

    init {
        card.element.addEventListener("header-arrival-expired") { event ->
            if (event.eventData.get("event.detail.revision").asLong() == revision.get()) close()
        }.addEventData("event.detail.revision")
        notification.addOpenedChangeListener { event -> if (!event.isOpened) selection = Selection.Hidden }
    }

    fun show(values: List<HeaderNotification>) {
        if (values.isEmpty() || !widget.isAttached) return
        selection = if (values.size == 1) Selection.Single(values.single().key) else Selection.Batch(values.map { item -> item.key })
        render(values)
        notification.open()
        widget.element.executeJs(
            "window.testsysHeaderArrivals.show(this, $0, $1, $2, $3)",
            card.element,
            notification.element,
            ARRIVAL_DURATION_MS,
            revision.incrementAndGet(),
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
        if (widget.isAttached) widget.element.executeJs("window.testsysHeaderArrivals.hide(this)")
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
