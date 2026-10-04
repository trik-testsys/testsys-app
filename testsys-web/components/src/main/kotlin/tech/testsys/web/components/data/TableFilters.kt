package tech.testsys.web.components.data

import com.vaadin.flow.component.html.Div
import com.vaadin.flow.component.html.NativeButton
import tech.testsys.web.components.ElementHandle
import tech.testsys.web.components.TestSysDsl
import tech.testsys.web.components.UiTexts
import tech.testsys.web.components.actions.action
import tech.testsys.web.components.actions.mainAction
import tech.testsys.web.components.core.IconName
import tech.testsys.web.components.core.svgIcon
import tech.testsys.web.components.layout.BlockEditState
import tech.testsys.web.components.layout.BlockRowScope
import tech.testsys.web.components.layout.BlockScope
import tech.testsys.web.components.layout.ContentScope
import tech.testsys.web.components.layout.Placement
import java.util.UUID

/**
 * Scope of table filter fields, built from the ordinary block row DSL.
 *
 * @since %CURRENT_VERSION%
 */
@TestSysDsl
class TableFiltersScope internal constructor(
    private val fields: Div,
    private val columns: Int,
    private val texts: UiTexts,
    private val editState: BlockEditState,
) {
    /**
     * Adds a row of ordinary fields on the block columns.
     *
     * @since %CURRENT_VERSION%
     */
    fun row(content: BlockRowScope.() -> Unit) {
        val row = Div().apply { addClassName("ts-block__row") }
        BlockRowScope(row, columns, texts, editState).content()
        if (row.children.findAny().isPresent) fields.add(row)
    }
}

/**
 * Handle of the table filter panel, whose fields retain their draft when collapsed.
 *
 * @property isExpanded whether the filter fields and actions are shown.
 * @since %CURRENT_VERSION%
 */
class TableFiltersHandle internal constructor(
    component: Div,
    private val toggle: NativeButton,
    private val content: Div,
) : ElementHandle(component) {
    var isExpanded: Boolean
        get() = toggle.element.getAttribute("aria-expanded") == "true"
        set(value) {
            toggle.element.setAttribute("aria-expanded", value.toString())
            if (value) content.element.removeAttribute("hidden") else content.element.setAttribute("hidden", true)
            if (!value) {
                content.element.executeJs("if (this.contains(document.activeElement)) this.previousElementSibling.focus();")
            }
        }
}

/**
 * Adds a table filter panel separately from the block body.
 *
 * @param onApply validates the draft and publishes applied values, returning whether it succeeded.
 * @param onReset restores the draft and applied values to the page defaults.
 * @param onRefresh refreshes the first table page after successful application or resetting.
 * @param content declares ordinary fields in rows.
 * @throws IllegalStateException if the block already has filters, or this is the content of a load.
 * @since %CURRENT_VERSION%
 */
fun BlockScope.filters(
    onApply: () -> Boolean,
    onReset: () -> Unit,
    onRefresh: () -> Unit,
    content: TableFiltersScope.() -> Unit,
): TableFiltersHandle {
    val root = Div().apply { addClassName("ts-table-filters") }
    val panelId = "ts-table-filters-${UUID.randomUUID()}"
    val toggle = NativeButton().apply {
        addClassName("ts-table-filters__toggle")
        setId("$panelId-toggle")
        element.setAttribute("aria-controls", panelId)
        element.setAttribute("type", "button")
        add(svgIcon(IconName.ChevronDown))
        element.appendChild(com.vaadin.flow.component.Text(texts.tableFilters.title).element)
    }
    val panel = Div().apply {
        addClassName("ts-table-filters__content")
        setId(panelId)
        element.setAttribute("role", "region")
        element.setAttribute("aria-labelledby", "$panelId-toggle")
    }
    val fields = Div().apply {
        addClassName("ts-table-filters__fields")
        style.set("grid-template-columns", "repeat($columns, minmax(0, 1fr))")
    }
    val actions = Div().apply { addClassName("ts-table-filters__actions") }
    val bar = ContentScope(actions, texts, Placement.Body, columns)
    bar.action(texts.tableFilters.reset) {
        onClick {
            onReset()
            onRefresh()
        }
    }
        .component.addClassName("ts-table-filters__reset")
    bar.mainAction(texts.tableFilters.apply) { onClick { if (onApply()) onRefresh() } }
        .component.addClassName("ts-table-filters__apply")
    TableFiltersScope(fields, columns, texts, BlockEditState(fields.element)).apply(content)
    panel.add(fields, actions)
    root.add(toggle, panel)
    placeFilters(root)
    return TableFiltersHandle(root, toggle, panel).apply {
        isExpanded = false
        toggle.addClickListener { isExpanded = !isExpanded }
    }
}
