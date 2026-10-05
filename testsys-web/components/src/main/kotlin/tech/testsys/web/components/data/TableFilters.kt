@file:OptIn(InternalComponentsApi::class)

package tech.testsys.web.components.data

import com.vaadin.flow.component.html.Div
import com.vaadin.flow.component.html.NativeButton
import com.vaadin.flow.component.page.PendingJavaScriptResult
import com.vaadin.flow.dom.Element
import tech.testsys.web.components.ElementHandle
import tech.testsys.web.components.TestSysDsl
import tech.testsys.web.components.UiTexts
import tech.testsys.web.components.actions.action
import tech.testsys.web.components.actions.mainAction
import tech.testsys.web.components.core.CssClass
import tech.testsys.web.components.core.ElementRole
import tech.testsys.web.components.core.ElementType
import tech.testsys.web.components.core.HtmlAttribute
import tech.testsys.web.components.core.IconName
import tech.testsys.web.components.core.InternalComponentsApi
import tech.testsys.web.components.core.addClassName
import tech.testsys.web.components.core.getAriaExpanded
import tech.testsys.web.components.core.setAriaExpanded
import tech.testsys.web.components.core.setAttribute
import tech.testsys.web.components.core.setGridTemplateColumns
import tech.testsys.web.components.core.setHidden
import tech.testsys.web.components.core.setRole
import tech.testsys.web.components.core.setType
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
        val row = Div().apply { addClassName(CssClass.BlockRow) }
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
        get() = toggle.element.getAriaExpanded() == true
        set(value) {
            toggle.element.setAriaExpanded(value)
            if (value) content.element.setHidden(false) else content.element.setHidden(true)
            if (!value) {
                content.element.restoreFilterToggleFocus()
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
    val root = Div().apply { addClassName(CssClass.TableFilters) }
    val panelId = "ts-table-filters-${UUID.randomUUID()}"
    val toggle = NativeButton().apply {
        addClassName(CssClass.TableFiltersToggle)
        setId("$panelId-toggle")
        element.setAttribute(HtmlAttribute.AriaControls, panelId)
        element.setType(ElementType.Button)
        add(svgIcon(IconName.ChevronDown))
        element.appendChild(com.vaadin.flow.component.Text(texts.tableFilters.title).element)
    }
    val panel = Div().apply {
        addClassName(CssClass.TableFiltersContent)
        setId(panelId)
        element.setRole(ElementRole.Region)
        element.setAttribute(HtmlAttribute.AriaLabelledBy, "$panelId-toggle")
    }
    val fields = Div().apply {
        addClassName(CssClass.TableFiltersFields)
        style.setGridTemplateColumns(columns)
    }
    val actions = Div().apply { addClassName(CssClass.TableFiltersActions) }
    val bar = ContentScope(actions, texts, Placement.Body, columns)
    bar.action(texts.tableFilters.reset) {
        onClick {
            onReset()
            onRefresh()
        }
    }
        .component.addClassName(CssClass.TableFiltersReset)
    bar.mainAction(texts.tableFilters.apply) { onClick { if (onApply()) onRefresh() } }
        .component.addClassName(CssClass.TableFiltersApply)
    TableFiltersScope(fields, columns, texts, BlockEditState(fields.element)).apply(content)
    panel.add(fields, actions)
    root.add(toggle, panel)
    placeFilters(root)
    return TableFiltersHandle(root, toggle, panel).apply {
        isExpanded = false
        toggle.addClickListener { isExpanded = !isExpanded }
    }
}

private fun Element.restoreFilterToggleFocus(): PendingJavaScriptResult =
    executeJs("if (this.contains(document.activeElement)) this.previousElementSibling.focus();")
