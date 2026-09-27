package tech.testsys.web.ui.forms

import com.vaadin.flow.component.HasValue
import com.vaadin.flow.component.customfield.CustomField
import com.vaadin.flow.component.html.Div
import com.vaadin.flow.component.html.NativeButton
import com.vaadin.flow.component.textfield.TextField
import com.vaadin.flow.data.binder.HasValidator
import com.vaadin.flow.data.value.ValueChangeMode
import tech.testsys.web.ui.UiTexts
import tech.testsys.web.ui.actions.ActionHandle
import tech.testsys.web.ui.actions.iconAction
import tech.testsys.web.ui.core.IconName
import tech.testsys.web.ui.data.DataTable
import tech.testsys.web.ui.data.Page
import tech.testsys.web.ui.data.PageRequest
import tech.testsys.web.ui.data.TableColumn
import tech.testsys.web.ui.data.TableScope
import tech.testsys.web.ui.data.TableSpec
import tech.testsys.web.ui.feedback.EmptyContent
import tech.testsys.web.ui.layout.BlockRowScope
import tech.testsys.web.ui.layout.ContentScope
import tech.testsys.web.ui.layout.Placement
import tech.testsys.web.ui.overlay.DialogShell

/** Default number of rows on a page of the lookup dialog. */
internal const val LOOKUP_PAGE_SIZE: Int = 10

/** Delay after the last typed character before the lookup searches. */
private const val SEARCH_DELAY_MS: Int = 300

/** Client-side filter of clicks on the lookup box: clicks on its buttons are their own. */
internal const val LOOKUP_CLICK_FILTER: String = "!event.target.closest('vaadin-button')"

/**
 * Adds a field of one entity chosen in a dialog that searches with [fetch] and lists rows in [columns], [pageSize]
 * rows a page; [display] gives the text of the chosen value. The label takes [labelSize] and the control [size] columns of the row.
 *
 * @param T the type of the entities.
 * @throws IllegalArgumentException if [pageSize] is below one, or [columns] declares no columns, sets its own empty
 * state or row click, which the lookup owns, or adds a menu column.
 * @since %CURRENT_VERSION%
 */
fun <T : Any> BlockRowScope.lookup(
    label: String,
    labelSize: Int,
    size: Int,
    fetch: (query: String, request: PageRequest) -> Page<T>,
    display: (T) -> String,
    columns: TableScope<T>.() -> Unit,
    hint: String? = null,
    pageSize: Int = LOOKUP_PAGE_SIZE,
    configure: ValueInput<T?>.() -> Unit = {},
): ValueInput<T?> {
    require(pageSize >= 1) { "Lookup '$label' page size must be at least 1, got $pageSize" }
    val scope = TableScope<T>(texts).apply(columns)
    val spec = scope.spec()
    require(spec.columns.isNotEmpty()) { "Lookup '$label' must declare at least one column" }
    require(!scope.hasOwnEmpty && spec.rowClick == null && !scope.hasMenuColumn) {
        "Lookup '$label' columns must not set the empty state, the row click or a menu column: the lookup sets the first two itself"
    }
    val control = LookupField(texts, label, display, fetch, pageSize, spec.columns)
    val subscribe = { listener: HasValue.ValueChangeListener<in HasValue.ValueChangeEvent<T?>> ->
        control.addValueChangeListener { event -> listener.valueChanged(event) }
    }
    return placeInput(label, labelSize, size, control, hint, subscribe, configure)
}

/**
 * Field of one entity chosen in a dialog titled [title] with a search line and a table of [columns]: shows [display]
 * of the value on a button that opens the dialog, and clears the value. The value button is the only input of the field,
 * so the field label focuses it and Enter and Space open the dialog natively; the open action repeats it for the mouse
 * and stays out of the tab order.
 */
internal class LookupField<T : Any>(
    private val texts: UiTexts,
    private val title: String,
    private val display: (T) -> String,
    private val fetch: (String, PageRequest) -> Page<T>,
    private val pageSize: Int,
    private val columns: List<TableColumn<T>>,
) : CustomField<T?>(null, true), HasValidator<T?> {
    private val text = NativeButton().apply {
        addClassName("ts-lookup__text")
        element.setAttribute("type", "button")
    }
    private val clearAction: ActionHandle
    private val openAction: ActionHandle
    private var openShell: DialogShell? = null

    init {
        val box = Div(text).apply { addClassName("ts-lookup") }
        val actions = ContentScope(box, texts, Placement.Head)
        clearAction = actions.iconAction(IconName.X, texts.lookup.clear) { onClick { clearByUser() } }
        openAction = actions.iconAction(IconName.Search, texts.lookup.open) { onClick { openDialog() } }
        openAction.button.tabIndex = -1
        box.element.addEventListener("click") { openDialog() }.setFilter(LOOKUP_CLICK_FILTER)
        add(box)
        updateView(value)
    }

    override fun generateModelValue(): T? = value

    override fun setPresentationValue(newPresentationValue: T?) {
        updateView(newPresentationValue)
    }

    /** Also sets the attribute, since `vaadin-custom-field` has no read-only state of its own to style. */
    override fun setReadOnly(readOnly: Boolean) {
        super.setReadOnly(readOnly)
        element.setAttribute("readonly", readOnly)
        updateView(value)
    }

    override fun onEnabledStateChanged(enabled: Boolean) {
        super.onEnabledStateChanged(enabled)
        updateView(value)
    }

    private fun isChoosable(): Boolean = isEnabled && !isReadOnly

    private fun updateView(current: T?) {
        val isChoosable = isChoosable()
        val shown = current?.let(display).orEmpty()
        text.text = shown
        // An empty button would have no accessible name: it is named after what it does, or after the field if it does nothing.
        val emptyName = if (isChoosable) texts.lookup.open else title
        if (shown.isEmpty()) text.element.setAttribute("aria-label", emptyName) else text.element.removeAttribute("aria-label")
        if (isChoosable) text.element.setAttribute("aria-haspopup", "dialog") else text.element.removeAttribute("aria-haspopup")
        if (isChoosable) text.element.removeAttribute("aria-disabled") else text.element.setAttribute("aria-disabled", "true")
        openAction.isVisible = isChoosable
        clearAction.isVisible = isChoosable && current != null
    }

    /** Sets [row] as a value chosen by the user. */
    private fun choose(row: T?) {
        setModelValue(row, true)
        updateView(row)
    }

    /** Clears the value and gives the focus to the value button, since the clear button has just disappeared. */
    private fun clearByUser() {
        choose(null)
        text.focus()
    }

    private fun openDialog() {
        if (!isChoosable() || openShell != null) return
        val shell = DialogShell(texts, title, subtitle = null, isWide = true, isAlert = false)
        openShell = shell
        shell.dialog.addOpenedChangeListener { event -> if (!event.isOpened) openShell = null }
        var query = ""
        val spec = TableSpec(columns, EmptyContent(texts.lookup.empty)) { row ->
            choose(row)
            shell.close()
        }
        val table = DataTable(
            texts,
            key = { row -> row },
            pageSize,
            isSelectable = false,
            fetch = { request -> fetch(query, request) },
            spec,
            highlighted = { row -> row == value },
        )
        val search = TextField().apply {
            addClassName("ts-lookup-search")
            placeholder = texts.lookup.search
            setAriaLabel(texts.lookup.search)
            isAutofocus = true
            valueChangeMode = ValueChangeMode.LAZY
            valueChangeTimeout = SEARCH_DELAY_MS
            width = "100%"
            addValueChangeListener { event ->
                query = event.value.trim()
                table.reload(toFirstPage = true)
            }
        }
        shell.content.add(search, table.table)
        shell.foot.add(table.pager.root)
        table.pagerHost = shell.foot
        shell.open()
        search.focus()
    }
}
