@file:OptIn(InternalComponentsApi::class)

package tech.testsys.web.components.forms

import com.vaadin.flow.component.html.Div
import com.vaadin.flow.component.html.NativeButton
import com.vaadin.flow.component.html.Span
import tech.testsys.web.components.UiTexts
import tech.testsys.web.components.actions.action
import tech.testsys.web.components.actions.mainAction
import tech.testsys.web.components.core.ElementType
import tech.testsys.web.components.core.ICON_SIZE_TINY
import tech.testsys.web.components.core.IconName
import tech.testsys.web.components.core.InternalComponentsApi
import tech.testsys.web.components.core.setType
import tech.testsys.web.components.core.svgIcon
import tech.testsys.web.components.data.Page
import tech.testsys.web.components.data.PageRequest
import tech.testsys.web.components.data.TableScope
import tech.testsys.web.components.data.TableSpec
import tech.testsys.web.components.layout.BlockRowScope
import tech.testsys.web.components.layout.ContentScope
import tech.testsys.web.components.layout.Placement
import tech.testsys.web.components.overlay.DIALOG_COLUMNS

/** Number of values shown as chips; the rest is counted in one more chip. */
private const val MAX_CHIPS = 3

/**
 * Adds a field of several entities checked in a dialog that searches with [fetch] and lists rows in [columns], [pageSize]
 * rows a page; [display] gives the text of a chosen value. The label takes [labelSize] and the control [size] columns of the row.
 *
 * @param T the type of the entities; they are matched with the fetched rows by `equals` and `hashCode`, so [T] must identify
 * an entity by them, as domain entities do by id.
 * @throws IllegalArgumentException if [pageSize] is below one, or [columns] declares no columns, sets its own empty
 * state or row click, which the lookup owns, or adds a menu column.
 * @since %CURRENT_VERSION%
 */
fun <T : Any> BlockRowScope.lookupMany(
    label: String,
    labelSize: Int,
    size: Int,
    fetch: (query: String, request: PageRequest) -> Page<T>,
    display: (T) -> String,
    columns: TableScope<T>.() -> Unit,
    hint: String? = null,
    pageSize: Int = LOOKUP_PAGE_SIZE,
    configure: ValueInput<Set<T>>.() -> Unit = {},
): ValueInput<Set<T>> {
    val tableColumns = lookupColumns(label, pageSize, columns, isSelectable = true)
    val control = LookupManyField(texts, label, display, fetch, pageSize, tableColumns, gridColumns = size)
    return addInput(label, labelSize, size, control, hint, configure)
}

/**
 * Field of several entities checked in the lookup dialog titled [title] with a table of [columns]: shows the first values
 * as chips with [display] and a remove button each, then the count of the rest, and clears the value. A click on a row
 * of the dialog toggles its checkbox; the checks survive search and paging and become the value on apply only. Tab stops
 * follow the DOM: the value button, which the field label focuses, then the chip remove buttons, shown before it by CSS
 * `order`, then the clear button.
 */
internal class LookupManyField<T : Any>(
    texts: UiTexts,
    private val title: String,
    private val display: (T) -> String,
    private val fetch: (String, PageRequest) -> Page<T>,
    private val pageSize: Int,
    private val columns: TableSpec<T>,
    gridColumns: Int,
) : LookupFrame<Set<T>>(texts, title, emptyValue = emptySet(), gridColumns = gridColumns) {
    private val chips = Div().apply { addClassName("ts-lookup__chips") }

    init {
        box.addClassName("ts-lookup--many")
        // After the value button, so that it stays the first input of the field; CSS shows the chips before it.
        box.addComponentAtIndex(1, chips)
        updateView(value)
    }

    override fun valueName(current: Set<T>): String = current.joinToString(", ", transform = display)

    override fun valueEquals(value1: Set<T>?, value2: Set<T>?): Boolean {
        if (value1 === value2) return true
        if (value1 == null || value2 == null || value1.size != value2.size) return false
        // Entities compare by id, but replacing their instances must update the field and notify Binder.
        val instances = value1.associateBy { item -> item }
        return value2.all { item -> instances[item] === item }
    }

    // The chips show the values.
    override fun valueText(current: Set<T>): String = ""

    override fun showValue(current: Set<T>, isChoosable: Boolean) {
        chips.removeAll()
        current.take(MAX_CHIPS).forEach { item -> chips.add(chipOf(item, isChoosable)) }
        if (current.size > MAX_CHIPS) {
            chips.add(Span("+${current.size - MAX_CHIPS}").apply { addClassNames("ts-chip", "ts-chip--more", "ts-obscured-value") })
        }
        chips.isVisible = current.isNotEmpty()
    }

    override fun buildDialog(): LookupDialog<T> {
        // Rows by their table keys, which are the rows themselves: the checked keys turn back into values on apply.
        // An equal row fetched later replaces the stored instance, so the value gets the latest fetched one.
        val knownRows = LinkedHashMap<Any, T>()
        value.forEach { item -> knownRows[item] = item }
        val fetchKnown = { query: String, request: PageRequest ->
            fetch(query, request).also { page -> page.rows.forEach { row -> knownRows[row] = row } }
        }
        val dialog = LookupDialog(
            texts,
            title,
            fetchKnown,
            pageSize,
            columns,
            isSelectable = true,
            selected = value,
            highlighted = { false },
        ) { row -> table.toggle(row) }
        val count = Span(texts.lookup.selectedCount(value.size)).apply { addClassNames("ts-muted", "ts-lookup-count") }
        dialog.table.onSelectionChange = { keys -> count.text = texts.lookup.selectedCount(keys.size) }
        dialog.table.pager.root.addClassName("ts-lookup-pager")
        dialog.shell.content.add(dialog.table.pager.root)
        dialog.shell.foot.add(count)
        val foot = ContentScope(dialog.shell.foot, texts, Placement.Body, DIALOG_COLUMNS)
        foot.action(texts.lookup.reset) { onClick { dialog.table.clearSelection() } }
        foot.mainAction(texts.lookup.apply) {
            onClick {
                choose(dialog.table.selected.mapNotNullTo(LinkedHashSet()) { key -> knownRows[key] })
                dialog.shell.close()
            }
        }
        return dialog
    }

    /** Chip of [item]; the remove button only if [isRemovable], named after the value it removes. */
    private fun chipOf(item: T, isRemovable: Boolean): Span {
        val name = display(item)
        val chip = Span(Span(name).apply { addClassName("ts-obscured-value") }).apply { addClassName("ts-chip") }
        if (isRemovable) {
            chip.add(
                NativeButton().apply {
                    addClassName("ts-chip__x")
                    element.setType(ElementType.Button)
                    element.setAttribute("aria-label", texts.lookup.remove(name))
                    add(svgIcon(IconName.X, ICON_SIZE_TINY))
                    addClickListener { if (isChoosable()) chooseInBox(value - item) }
                },
            )
        }
        return chip
    }
}
