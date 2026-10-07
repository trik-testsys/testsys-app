@file:OptIn(InternalComponentsApi::class)

package tech.testsys.web.components.layout

import com.vaadin.flow.component.Component
import com.vaadin.flow.component.html.Div
import com.vaadin.flow.component.html.H3
import com.vaadin.flow.component.html.Header
import com.vaadin.flow.component.html.Section
import com.vaadin.flow.component.html.Span
import tech.testsys.web.components.core.CssClass
import tech.testsys.web.components.core.InternalComponentsApi
import tech.testsys.web.components.core.addClassName
import tech.testsys.web.components.core.addClassNames
import tech.testsys.web.components.core.setGridColumnSpan
import tech.testsys.web.components.texts.UiTexts

/** Title and subtitle of a block head; the head is omitted if there are neither, nor actions, nor tabs. */
internal class BlockHeading(val title: String?, val subtitle: String?)

/**
 * Builds the `section.ts-block` markup whose rows lie on [columns] grid columns;
 * [span] sets the grid span of the block in its page row.
 */
internal fun buildBlock(
    texts: UiTexts,
    heading: BlockHeading,
    highlight: Boolean,
    span: Int,
    columns: Int,
    content: BlockScope.() -> Unit,
): BlockHandle {
    val body = Div().apply { addClassNames(CssClass.BlockBody, CssClass.BlockBodyGrid) }
    val block = Section()
    val editState = BlockEditState(block.element)
    val scope = BlockScope(body, columns, texts, editState, heading.title).apply(content).apply { finish() }
    with(block) {
        addClassNames(CssClass.Block, CssClass.BlockGrid)
        if (scope.filtersBar != null) addClassName(CssClass.BlockFilters)
        if (highlight) addClassName(CssClass.BlockDark)
        style.setGridColumnSpan(span)
        blockHead(heading = heading, actions = scope.actionsBar, tabs = scope.tabsBar)?.let { head -> add(head) }
        scope.filtersBar?.let { filters -> add(filters) }
        if (body.children.findAny().isPresent) {
            if (scope.isFlushBody) body.addClassName(CssClass.BlockBodyFlush)
            add(body)
        }
        scope.footerBar?.let { footer -> add(footer) }
    }
    scope.start()
    return BlockHandle(block, editState, hasEditingSwitch = scope.hasEditingSwitch)
}

private fun blockHead(heading: BlockHeading, actions: Div?, tabs: Component?): Header? {
    val hasTitles = heading.title != null || heading.subtitle != null
    if (!hasTitles && actions == null && tabs == null) return null
    val titles = Div().apply { addClassName(CssClass.BlockTitles) }
    heading.title?.let { title -> titles.add(H3(title).apply { addClassName(CssClass.BlockTitle) }) }
    heading.subtitle?.let { subtitle -> titles.add(Span(subtitle).apply { addClassName(CssClass.BlockSub) }) }
    if (!hasTitles && tabs != null) titles.add(tabs)
    return Header(titles).apply {
        addClassName(CssClass.BlockHead)
        actions?.let { bar -> add(bar) }
        if (hasTitles && tabs != null) {
            addClassName(CssClass.BlockHeadTabs)
            add(tabs)
        }
    }
}
