@file:OptIn(InternalComponentsApi::class)

package tech.testsys.web.components.overlay

import com.vaadin.flow.component.html.Div
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.dom.SignalBinding
import com.vaadin.flow.signals.Signal
import tech.testsys.web.components.TestSysDsl
import tech.testsys.web.components.core.CssClass
import tech.testsys.web.components.core.CssTheme
import tech.testsys.web.components.core.InternalComponentsApi
import tech.testsys.web.components.core.addClassName
import tech.testsys.web.components.core.addThemeName
import tech.testsys.web.components.data.DataTable
import tech.testsys.web.components.data.Page
import tech.testsys.web.components.data.TableHandle
import tech.testsys.web.components.data.TableScope
import tech.testsys.web.components.layout.BlockEditState
import tech.testsys.web.components.layout.BlockRowScope
import tech.testsys.web.components.layout.ContentScope
import tech.testsys.web.components.texts.UiTexts
import tech.testsys.web.components.texts.currentTexts

/**
 * Handle of a reusable side drawer whose form values survive closing.
 *
 * @property isOpen whether the drawer is open.
 * @property isEditable whether the drawer fields can be changed.
 * @since %CURRENT_VERSION%
 */
@TestSysDsl
class DrawerHandle internal constructor(private val delegate: DialogHandle) {
    private var closeListener: () -> Unit = {}

    init {
        delegate.onClose { closeListener() }
    }

    val isOpen: Boolean
        get() = delegate.isOpen
    var isEditable: Boolean
        get() = delegate.isEditable
        set(value) {
            delegate.isEditable = value
        }

    /**
     * Opens the drawer and focuses its content.
     *
     * @since %CURRENT_VERSION%
     */
    fun open() {
        delegate.open()
    }

    /**
     * Closes the drawer and restores the previous focus.
     *
     * @since %CURRENT_VERSION%
     */
    fun close() {
        delegate.close()
    }

    /**
     * Binds the drawer edit mode to [signal].
     *
     * @since %CURRENT_VERSION%
     */
    fun bindEditable(signal: Signal<Boolean>): SignalBinding<Boolean> = delegate.bindEditable(signal)

    /**
     * Runs [listener] for every closing action; a later listener replaces the earlier one.
     *
     * @since %CURRENT_VERSION%
     */
    fun onClose(listener: () -> Unit) {
        closeListener = listener
    }
}

/**
 * Scope of a drawer with rows on 24 columns, static tables and a footer.
 *
 * @since %CURRENT_VERSION%
 */
@TestSysDsl
class DrawerScope internal constructor(
    private val delegate: DialogScope,
    private val handle: DrawerHandle,
    internal val container: Div,
    internal val texts: UiTexts,
) {
    /**
     * Adds one row of [content] to the drawer.
     *
     * @since %CURRENT_VERSION%
     */
    fun row(content: BlockRowScope.() -> Unit) {
        delegate.row(content)
    }

    /**
     * Adds footer [content], receiving the drawer handle for its actions.
     *
     * @since %CURRENT_VERSION%
     */
    fun footer(content: ContentScope.(DrawerHandle) -> Unit) {
        delegate.footer {
            content(this@DrawerScope.handle)
        }
    }
}

/**
 * Builds a 480 pixel side drawer of [title], optional [subtitle], rows, tables and footer [content].
 *
 * @param configure the returned drawer configuration.
 * @param content the rows, tables and footer composition.
 * @since %CURRENT_VERSION%
 */
fun drawer(
    title: String,
    subtitle: String? = null,
    configure: DrawerHandle.() -> Unit = {},
    content: DrawerScope.() -> Unit,
): DrawerHandle {
    val texts = currentTexts()
    val shell = DialogShell(texts, title = title, subtitle = subtitle, size = DialogSize.M, isAlert = false)
    shell.dialog.addThemeName(CssTheme.Drawer)
    shell.content.addClassName(CssClass.DialogGrid)

    val editing = BlockEditState(shell.dialog.element)
    val delegate = DialogHandle(shell, editing)
    val handle = DrawerHandle(delegate)
    DrawerScope(DialogScope(shell, texts, editing, delegate), handle, shell.content, texts).content()
    return handle.apply(configure)
}

/**
 * Adds a full-width static table of [rows] under [title], without pagination; [key] identifies each row.
 *
 * @param T the type of the rows.
 * @param key the stable identity of each row.
 * @param content the columns and row interactions of the table.
 * @since %CURRENT_VERSION%
 */
fun <T> DrawerScope.table(title: String, key: (T) -> Any, rows: List<T>, content: TableScope<T>.() -> Unit): TableHandle<T> {
    val spec = TableScope<T>(texts).apply(content).spec()
    require(spec.columns.isNotEmpty()) { "Drawer table '$title' must declare at least one column" }
    val table = DataTable(
        texts,
        key,
        pageSize = maxOf(1, rows.size),
        isSelectable = false,
        fetch = { Page(rows, rows.size) },
        spec = spec,
    )

    val heading = Span(title).apply { addClassName(CssClass.BlockTitle) }
    container.add(Div(heading, table.root).apply { addClassName(CssClass.DrawerTable) })
    return TableHandle(table)
}
