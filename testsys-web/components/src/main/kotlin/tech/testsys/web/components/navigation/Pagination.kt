@file:OptIn(InternalComponentsApi::class)

package tech.testsys.web.components.navigation

import com.vaadin.flow.component.Component
import com.vaadin.flow.component.html.Div
import com.vaadin.flow.component.html.NativeButton
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.dom.SignalBinding
import com.vaadin.flow.signals.BindingActiveException
import com.vaadin.flow.signals.Signal
import tech.testsys.web.components.ElementHandle
import tech.testsys.web.components.core.AriaCurrent
import tech.testsys.web.components.core.Bindable
import tech.testsys.web.components.core.CssClass
import tech.testsys.web.components.core.ElementType
import tech.testsys.web.components.core.HtmlAttribute
import tech.testsys.web.components.core.IconName
import tech.testsys.web.components.core.InternalComponentsApi
import tech.testsys.web.components.core.addClassName
import tech.testsys.web.components.core.hasClassName
import tech.testsys.web.components.core.pagerArrow
import tech.testsys.web.components.core.setAriaCurrent
import tech.testsys.web.components.core.setAttribute
import tech.testsys.web.components.core.setType
import tech.testsys.web.components.layout.ContentScope
import tech.testsys.web.components.texts.PaginationTexts

/** The most pages shown without gaps. */
private const val MAX_PAGES_WITHOUT_GAPS = 7

/** The number of pages shown in a row at the start or the end of a long range. */
private const val EDGE_RUN = 5

/** How close to the start or the end a page must be for the range to show [EDGE_RUN] pages there. */
private const val EDGE_REACH = 4

/**
 * Handle of a pagination: the current page, the page count and the listener of page choices.
 *
 * @property page the current page from 1; setting it does not run the listener, a page out of `1..pageCount` throws
 * [IllegalArgumentException].
 * @property pageCount the number of pages; a smaller count shows the last page in place of a later current one, a
 * count below 1 throws [IllegalArgumentException].
 * @since %CURRENT_VERSION%
 */
class PaginationHandle internal constructor(
    private val root: Div,
    private val texts: PaginationTexts,
    pageCount: Int,
    page: Int,
) : ElementHandle(root) {
    private val previous = pagerArrow(IconName.ChevronLeft, texts.previous)
    private val next = pagerArrow(IconName.ChevronRight, texts.next)
    private var count = pageCount
    private var items: List<Component> = emptyList()
    private var listener: (Int) -> Unit = {}

    // Kept by the Bindable even beyond the page count while bound, so that a larger count shows it again.
    private val current: Bindable<Int>

    var page: Int
        get() = current.value.coerceAtMost(count)
        set(value) {
            current.value = value
        }

    var pageCount: Int
        get() = count
        set(value) {
            require(value >= 1) { "Page count must be at least 1, got $value" }
            if (value == count) return
            count = value
            if (!current.isBound && current.value > value) current.value = value else render()
        }

    init {
        require(pageCount >= 1) { "Page count must be at least 1, got $pageCount" }
        require(page in 1..pageCount) { "Page must be within 1..$pageCount, got $page" }
        current = Bindable(root.element, initial = page) { value ->
            require(value >= 1) { "Page must be at least 1, got $value" }
            require(current.isBound || value <= count) { "Page must be within 1..$count, got $value" }
            render()
        }
        root.addClassName(CssClass.Pager)
        root.add(previous, next)
        previous.addClickListener { choose(target = this.page - 1, arrow = previous) }
        next.addClickListener { choose(target = this.page + 1, arrow = next) }
        render()
    }

    /**
     * Runs [listener] with the page a user chooses by a page button or an arrow; a click on the current page does
     * not run it. Replaces a listener set by an earlier call.
     *
     * @since %CURRENT_VERSION%
     */
    fun onChange(listener: (Int) -> Unit) {
        this.listener = listener
    }

    /**
     * Binds [page] to [signal]: every page it produces is shown at once, a page beyond [pageCount] as the last one.
     * A user choice then only runs the listener of [onChange], and the pagination shows it once [signal] takes it.
     * A manual [page] while bound, and a second binding, throw [BindingActiveException].
     *
     * @since %CURRENT_VERSION%
     */
    fun bindPage(signal: Signal<Int>): SignalBinding<Int> = current.bind(signal)

    // After the listener, which may change the page count, the focus goes to the new current page unless it stays
    // on the clicked arrow that remains enabled.
    private fun choose(target: Int, arrow: NativeButton?) {
        if (target == page) return
        if (!current.isBound) current.value = target
        listener(target)
        if (page == target && arrow?.isEnabled != true) activeButton().focus()
    }

    private fun render() {
        val shown = page
        items.forEach { item -> root.remove(item) }
        items = pageRange(page = shown, total = count).map { target ->
            if (target == null) Span("…").apply { addClassName(CssClass.PagerGap) } else pageButton(target, shown)
        }
        items.forEachIndexed { index, item -> root.addComponentAtIndex(index + 1, item) }
        previous.isEnabled = shown > 1
        next.isEnabled = shown < count
    }

    private fun activeButton(): NativeButton =
        items.filterIsInstance<NativeButton>().single { button -> button.hasClassName(CssClass.PagerBtnActive) }

    private fun pageButton(target: Int, shown: Int): NativeButton = NativeButton(target.toString()).apply {
        addClassName(CssClass.PagerBtn)
        element.setType(ElementType.Button)
        element.setAttribute(HtmlAttribute.AriaLabel, texts.page(target))
        if (target == shown) {
            addClassName(CssClass.PagerBtnActive)
            element.setAriaCurrent(AriaCurrent.Page)
        }
        addClickListener { choose(target = target, arrow = null) }
    }
}

/**
 * Adds a pagination of [pageCount] pages on [page]: the arrows and the page buttons, long ranges with gaps. The page
 * reacts to a choice through [PaginationHandle.onChange].
 *
 * @throws IllegalArgumentException if [pageCount] is below 1 or [page] is not within `1..pageCount`.
 * @since %CURRENT_VERSION%
 */
fun ContentScope.pagination(pageCount: Int, page: Int = 1, configure: PaginationHandle.() -> Unit = {}): PaginationHandle {
    val handle = PaginationHandle(Div(), texts.pagination, pageCount, page)
    add(handle.component)
    return handle.apply(configure)
}

/**
 * The page buttons of [page] among [total] pages, `null` standing for a gap: every page up to seven, otherwise the
 * first and the last page with five pages at the near end or the neighbours of [page] between gaps.
 */
internal fun pageRange(page: Int, total: Int): List<Int?> = when {
    total <= MAX_PAGES_WITHOUT_GAPS -> (1..total).toList()
    page <= EDGE_REACH -> (1..EDGE_RUN).toList() + listOf(null, total)
    page > total - EDGE_REACH -> listOf(1, null) + (total - EDGE_RUN + 1..total).toList()
    else -> listOf(1, null, page - 1, page, page + 1, null, total)
}
