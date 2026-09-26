package tech.testsys.web.ui.layout

import com.vaadin.flow.component.html.Div
import com.vaadin.flow.component.html.H3
import com.vaadin.flow.component.html.Header
import com.vaadin.flow.component.html.Section
import com.vaadin.flow.component.html.Span
import tech.testsys.web.ui.UiTexts

/** Title and subtitle of a block head; the head is omitted if both are `null` and the block has no actions. */
internal class BlockHeading(val title: String?, val subtitle: String?)

/**
 * Builds the `section.ts-block` markup whose rows lie on [columns] grid columns;
 * [span] sets the grid span inside a slot row, `null` for a page block.
 */
internal fun buildBlock(
    texts: UiTexts,
    heading: BlockHeading,
    highlight: Boolean,
    span: Int?,
    columns: Int,
    content: BlockScope.() -> Unit,
): BlockHandle {
    val body = Div().apply { addClassNames("ts-block__body", "ts-block__body--grid") }
    val editState = BlockEditState()
    val scope = BlockScope(body, columns, texts, editState).apply(content).apply { finish() }
    val block = Section().apply {
        addClassNames("ts-block", "ts-block--grid")
        if (highlight) addClassName("ts-block--dark")
        if (span != null) style.set("grid-column", "span $span")
        blockHead(heading, scope.actionsBar)?.let { head -> add(head) }
        if (body.children.findAny().isPresent) {
            if (scope.isFlushBody) body.addClassName("ts-block__body--flush")
            add(body)
        }
        scope.footerBar?.let { footer -> add(footer) }
    }
    return BlockHandle(block, editState)
}

private fun blockHead(heading: BlockHeading, actions: Div?): Header? {
    if (heading.title == null && heading.subtitle == null && actions == null) return null
    val titles = Div().apply { addClassName("ts-block__titles") }
    heading.title?.let { title -> titles.add(H3(title).apply { addClassName("ts-block__title") }) }
    heading.subtitle?.let { subtitle -> titles.add(Span(subtitle).apply { addClassName("ts-block__sub") }) }
    return Header(titles).apply {
        addClassName("ts-block__head")
        actions?.let { bar -> add(bar) }
    }
}
