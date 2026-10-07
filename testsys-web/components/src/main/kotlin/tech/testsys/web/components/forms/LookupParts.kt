@file:OptIn(InternalComponentsApi::class)

package tech.testsys.web.components.forms

import com.vaadin.flow.component.customfield.CustomField
import com.vaadin.flow.component.html.Div
import com.vaadin.flow.component.html.NativeButton
import com.vaadin.flow.component.textfield.TextField
import com.vaadin.flow.data.binder.HasValidator
import com.vaadin.flow.data.value.ValueChangeMode
import com.vaadin.flow.dom.Element
import tech.testsys.web.components.actions.ActionHandle
import tech.testsys.web.components.actions.iconAction
import tech.testsys.web.components.core.AriaPopup
import tech.testsys.web.components.core.CssClass
import tech.testsys.web.components.core.CssUnit
import tech.testsys.web.components.core.DomEvent
import tech.testsys.web.components.core.DomEventFilter
import tech.testsys.web.components.core.ElementType
import tech.testsys.web.components.core.HtmlAttribute
import tech.testsys.web.components.core.IconName
import tech.testsys.web.components.core.InternalComponentsApi
import tech.testsys.web.components.core.LOOKUP_CLICK_EXPRESSION
import tech.testsys.web.components.core.LOOKUP_CONTROL_SELECTOR
import tech.testsys.web.components.core.addClassName
import tech.testsys.web.components.core.addClassNames
import tech.testsys.web.components.core.addEventListener
import tech.testsys.web.components.core.removeAttribute
import tech.testsys.web.components.core.setAriaDisabled
import tech.testsys.web.components.core.setAriaHasPopup
import tech.testsys.web.components.core.setAttribute
import tech.testsys.web.components.core.setFilter
import tech.testsys.web.components.core.setReadOnly
import tech.testsys.web.components.core.setType
import tech.testsys.web.components.core.setWidth
import tech.testsys.web.components.data.DataTable
import tech.testsys.web.components.data.Page
import tech.testsys.web.components.data.PageRequest
import tech.testsys.web.components.data.TableScope
import tech.testsys.web.components.data.TableSpec
import tech.testsys.web.components.feedback.EmptyContent
import tech.testsys.web.components.layout.BlockRowScope
import tech.testsys.web.components.layout.ContentScope
import tech.testsys.web.components.layout.Placement
import tech.testsys.web.components.overlay.DIALOG_COLUMNS
import tech.testsys.web.components.overlay.DialogShell
import tech.testsys.web.components.texts.UiTexts

/** Default number of rows on a page of the lookup dialog. */
internal const val LOOKUP_PAGE_SIZE: Int = 10

/** Delay after the last typed character before the lookup searches. */
private const val SEARCH_DELAY_MS: Int = 300

/** Selectors of the controls inside the lookup box whose clicks are their own: its buttons and the remove buttons of its chips. */
internal const val LOOKUP_OWN_CLICKS: String = LOOKUP_CONTROL_SELECTOR

/** Client-side filter of clicks on the lookup box: clicks on [LOOKUP_OWN_CLICKS] do not open the dialog. */
internal const val LOOKUP_CLICK_FILTER: String = LOOKUP_CLICK_EXPRESSION

/**
 * Checks the arguments of a lookup field [label] and returns the columns of its dialog table.
 *
 * @throws IllegalArgumentException if [pageSize] is below one, or [columns] declares no columns, sets its own empty
 * state or row click, which the lookup owns, or adds a menu column.
 */
internal fun <T : Any> BlockRowScope.lookupColumns(
    label: String,
    pageSize: Int,
    columns: TableScope<T>.() -> Unit,
    isSelectable: Boolean = false,
): TableSpec<T> {
    require(pageSize >= 1) { "Lookup '$label' page size must be at least 1, got $pageSize" }
    val scope = TableScope<T>(texts, gridColumns = DIALOG_COLUMNS, selectionSize = if (isSelectable) 1 else 0).apply(columns)
    val spec = scope.spec()
    require(spec.columns.isNotEmpty()) { "Lookup '$label' must declare at least one column" }
    require(!scope.hasOwnEmpty && spec.rowClick == null && !scope.hasMenuColumn) {
        "Lookup '$label' columns must not set the empty state, the row click or a menu column: the lookup sets the first two itself"
    }
    return spec
}

/**
 * Box of a lookup field like an input: a value button that opens the dialog titled [title] from [openDialog], and the
 * clear and open actions. The value button is the first input of the field, so the field label focuses it and Enter and
 * Space open the dialog natively; the open action repeats it for the mouse and stays out of the tab order. The value button
 * shows [valueText] of the value and is named [valueName] of it where the text differs. Subclasses end their construction
 * with [updateView].
 */
internal abstract class LookupFrame<V>(
    protected val texts: UiTexts,
    private val title: String,
    emptyValue: V,
    gridColumns: Int,
) : CustomField<V>(emptyValue, true), HasValidator<V> {
    protected val valueButton: NativeButton = NativeButton().apply {
        addClassNames(CssClass.LookupText, CssClass.ObscuredValue)
        element.setType(ElementType.Button)
    }
    protected val box: Div = Div(valueButton).apply { addClassName(CssClass.Lookup) }
    private val clearAction: ActionHandle
    private val openAction: ActionHandle
    private var currentDialog: LookupDialog<*>? = null

    init {
        val actions = ContentScope(box, texts, Placement.Head, gridColumns)
        clearAction = actions.iconAction(IconName.X, texts.lookup.clear) { onClick { clearByUser() } }
        openAction = actions.iconAction(IconName.Search, texts.lookup.open) { onClick { openDialog() } }
        openAction.button.tabIndex = -1
        box.element.addEventListener(DomEvent.Click) { openDialog() }.setFilter(DomEventFilter.LookupClick)
        add(box)
    }

    override fun generateModelValue(): V = value

    override fun setPresentationValue(newPresentationValue: V) {
        updateView(newPresentationValue)
    }

    /** Also sets the attribute, since `vaadin-custom-field` has no read-only state of its own to style. */
    override fun setReadOnly(readOnly: Boolean) {
        super.setReadOnly(readOnly)
        element.setReadOnly(readOnly)
        if (!isChoosable()) currentDialog?.shell?.close()
        updateView(value)
    }

    override fun onEnabledStateChanged(enabled: Boolean) {
        super.onEnabledStateChanged(enabled)
        if (!enabled) currentDialog?.shell?.close()
        updateView(value)
    }

    /** Accessible name of [current] for the value button; empty for the empty value. */
    protected abstract fun valueName(current: V): String

    /** Text of [current] on the value button; by default its name. */
    protected open fun valueText(current: V): String = valueName(current)

    /** Shows [current] in the box besides the value button; [isChoosable] tells whether the user may change it. */
    protected open fun showValue(current: V, isChoosable: Boolean) {
        // By default the value button alone shows the value.
    }

    /** Builds the dialog that chooses the value; the frame opens it. */
    protected abstract fun buildDialog(): LookupDialog<*>

    protected fun isChoosable(): Boolean = isEnabled && !isReadOnly

    protected fun updateView(current: V) {
        val isChoosable = isChoosable()
        val name = valueName(current)
        val text = valueText(current)
        valueButton.text = text
        // An empty button would have no accessible name: it is named after what it does, or after the field if it does nothing.
        val label = when {
            name.isEmpty() -> if (isChoosable) texts.lookup.open else title
            text == name -> null
            else -> name
        }
        valueButton.element.setOrRemove(name = HtmlAttribute.AriaLabel, value = label)
        valueButton.element.setAriaHasPopup(AriaPopup.Dialog.takeIf { isChoosable })
        valueButton.element.setAriaDisabled(true.takeUnless { isChoosable })
        openAction.isVisible = isChoosable
        clearAction.isVisible = isChoosable && current != emptyValue
        showValue(current, isChoosable)
    }

    /** Sets [newValue] as a value chosen by the user. */
    protected fun choose(newValue: V) {
        if (!isChoosable()) return
        setModelValue(newValue, true)
        updateView(value)
    }

    /** Sets [newValue] chosen in the box and gives the focus to the value button, since the pressed button may have just disappeared. */
    protected fun chooseInBox(newValue: V) {
        choose(newValue)
        valueButton.focus()
    }

    private fun clearByUser() {
        chooseInBox(emptyValue)
    }

    private fun openDialog() {
        if (!isChoosable() || currentDialog != null) return
        val dialog = buildDialog()
        currentDialog = dialog
        dialog.onClose { if (currentDialog === dialog) currentDialog = null }
        dialog.open()
    }
}

/** Sets attribute [name] to [value], or removes it if [value] is `null`. */
private fun Element.setOrRemove(name: HtmlAttribute, value: String?) {
    if (value == null) removeAttribute(name) else setAttribute(name, value)
}

/**
 * Lookup dialog titled [title]: a search line and a table of [columns] whose rows [fetch] finds by the typed query,
 * [pageSize] rows a page. A click on a row runs [onRowClick]; [isSelectable], [selected] and [highlighted] are those of
 * the table. The caller places the pager and fills the footer of [shell].
 */
internal class LookupDialog<T : Any>(
    texts: UiTexts,
    title: String,
    fetch: (String, PageRequest) -> Page<T>,
    pageSize: Int,
    columns: TableSpec<T>,
    isSelectable: Boolean,
    selected: Set<T>,
    highlighted: (T) -> Boolean,
    onRowClick: LookupDialog<T>.(T) -> Unit,
) {
    val shell: DialogShell = DialogShell(texts, title, subtitle = null, isWide = true, isAlert = false)
    val table: DataTable<T>
    private val search: TextField

    init {
        var query = ""
        val spec =
            TableSpec(columns.columns, EmptyContent(texts.lookup.empty), columns.layout) { row -> onRowClick(this, row) }
        table = DataTable(
            texts,
            key = { row -> row },
            pageSize,
            isSelectable,
            fetch = { request -> fetch(query, request) },
            spec,
            highlighted,
            initialSelection = selected,
        )
        search = TextField().apply {
            addClassName(CssClass.LookupSearch)
            placeholder = texts.lookup.search
            setAriaLabel(texts.lookup.search)
            isAutofocus = true
            valueChangeMode = ValueChangeMode.LAZY
            valueChangeTimeout = SEARCH_DELAY_MS
            style.setWidth(value = 100, unit = CssUnit.Percent)
            addValueChangeListener { event ->
                query = event.value.trim()
                table.reload(toFirstPage = true)
            }
        }
        shell.content.add(search, table.root)
    }

    fun open() {
        shell.open()
        search.focus()
    }

    /** Runs [listener] whenever the dialog closes, however it is closed. */
    fun onClose(listener: () -> Unit) {
        shell.dialog.addOpenedChangeListener { event -> if (!event.isOpened) listener() }
    }
}
