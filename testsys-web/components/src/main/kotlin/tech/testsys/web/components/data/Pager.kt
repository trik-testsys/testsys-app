@file:OptIn(InternalComponentsApi::class)

package tech.testsys.web.components.data

import com.vaadin.flow.component.html.Div
import com.vaadin.flow.component.html.Span
import tech.testsys.web.components.core.CssClass
import tech.testsys.web.components.core.IconName
import tech.testsys.web.components.core.InternalComponentsApi
import tech.testsys.web.components.core.addClassName
import tech.testsys.web.components.core.pagerArrow
import tech.testsys.web.components.texts.UiLocalization

/** Footer part of a table: the range of shown rows and the compact pager «‹ 3 / 71 ›»; [onPage] gets the page to go to. */
internal class Pager(private val texts: UiLocalization, private val onPage: (Int) -> Unit) {
    private val range = Span().apply { addClassName(CssClass.Muted) }
    private val counter = Span().apply { addClassName(CssClass.PagerLabel) }
    private val previous = pagerArrow(IconName.ChevronLeft, texts.messages.uiData.paginationPrevious())
    private val next = pagerArrow(IconName.ChevronRight, texts.messages.uiData.paginationNext())
    private var page = 0

    val root: Div = Div(
        range,
        Div(previous, counter, next).apply { addClassName(CssClass.Pager) },
    ).apply { addClassName(CssClass.TablePager) }

    init {
        previous.addClickListener { onPage(page - 1) }
        next.addClickListener { onPage(page + 1) }
    }

    /** Shows page [page] (from 0) of [pageCount] with rows [from]–[to] of [total]. */
    fun show(page: Int, pageCount: Int, from: Int, to: Int, total: Int) {
        this.page = page
        range.text = texts.messages.uiData.tableRange(from = from, to = to, total = total)
        counter.text = texts.messages.uiData.paginationCounter(page = page + 1, count = pageCount)
        previous.isEnabled = page > 0
        next.isEnabled = page < pageCount - 1
    }
}
