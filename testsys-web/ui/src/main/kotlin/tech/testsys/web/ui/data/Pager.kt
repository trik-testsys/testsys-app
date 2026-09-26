package tech.testsys.web.ui.data

import com.vaadin.flow.component.html.Div
import com.vaadin.flow.component.html.NativeButton
import com.vaadin.flow.component.html.Span
import tech.testsys.web.ui.UiTexts
import tech.testsys.web.ui.core.ICON_SIZE_SMALL
import tech.testsys.web.ui.core.IconName
import tech.testsys.web.ui.core.svgIcon

/** Footer part of a table: the range of shown rows and the compact pager «‹ 3 / 71 ›»; [onPage] gets the page to go to. */
internal class Pager(private val texts: UiTexts, private val onPage: (Int) -> Unit) {
    private val range = Span().apply { addClassName("ts-muted") }
    private val counter = Span().apply { addClassName("ts-pager__label") }
    private val previous = pagerButton(IconName.ChevronLeft, texts.table.previous)
    private val next = pagerButton(IconName.ChevronRight, texts.table.next)
    private var page = 0

    val root: Div = Div(range, Div(previous, counter, next).apply { addClassName("ts-pager") }).apply { addClassName("ts-table-pager") }

    init {
        previous.addClickListener { onPage(page - 1) }
        next.addClickListener { onPage(page + 1) }
    }

    /** Shows page [page] (from 0) of [pageCount] with rows [from]–[to] of [total]. */
    fun show(page: Int, pageCount: Int, from: Int, to: Int, total: Int) {
        this.page = page
        range.text = texts.table.range(from, to, total)
        counter.text = "${page + 1} / $pageCount"
        previous.isEnabled = page > 0
        next.isEnabled = page < pageCount - 1
    }

    private fun pagerButton(icon: IconName, label: String): NativeButton = NativeButton().apply {
        addClassName("ts-pager__btn")
        element.setAttribute("aria-label", label)
        element.setAttribute("type", "button")
        add(svgIcon(icon, ICON_SIZE_SMALL))
    }
}
