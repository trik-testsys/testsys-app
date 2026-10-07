@file:OptIn(InternalComponentsApi::class)

package tech.testsys.web.components.overlay

import com.vaadin.flow.component.html.Div
import com.vaadin.flow.dom.SignalBinding
import com.vaadin.flow.signals.BindingActiveException
import com.vaadin.flow.signals.Signal
import tech.testsys.web.components.TestSysDsl
import tech.testsys.web.components.core.CssClass
import tech.testsys.web.components.core.InternalComponentsApi
import tech.testsys.web.components.core.addClassName
import tech.testsys.web.components.layout.BlockEditState
import tech.testsys.web.components.layout.BlockRowScope
import tech.testsys.web.components.layout.ContentScope
import tech.testsys.web.components.layout.Placement
import tech.testsys.web.components.texts.UiTexts
import tech.testsys.web.components.texts.currentTexts

/** Columns of the grid inside a form dialog. */
internal const val DIALOG_COLUMNS: Int = 12

/**
 * Handle of a dialog built once and opened any number of times; field values stay between openings.
 *
 * @property isOpen whether the dialog is shown.
 * @property isEditable whether the fields of the dialog can be edited; manual changes while bound throw [BindingActiveException].
 * @since %CURRENT_VERSION%
 */
@TestSysDsl
class DialogHandle internal constructor(private val shell: DialogShell, private val editState: BlockEditState) {
    val isOpen: Boolean
        get() = shell.dialog.isOpened

    var isEditable: Boolean
        get() = editState.isEditable
        set(value) {
            editState.isEditable = value
        }

    /**
     * Binds [isEditable] to [signal], including before opening; fields keep their own read-only state.
     *
     * @throws BindingActiveException if the mode is already bound.
     * @since %CURRENT_VERSION%
     */
    fun bindEditable(signal: Signal<Boolean>): SignalBinding<Boolean> = editState.bind(signal)

    /**
     * Shows the dialog.
     *
     * @since %CURRENT_VERSION%
     */
    fun open() {
        shell.open()
    }

    /**
     * Hides the dialog.
     *
     * @since %CURRENT_VERSION%
     */
    fun close() {
        shell.close()
    }

    /**
     * Runs [listener] whenever the dialog closes, by an action, the close button, Esc or a click outside.
     *
     * @since %CURRENT_VERSION%
     */
    fun onClose(listener: () -> Unit) {
        shell.dialog.addOpenedChangeListener { event -> if (!event.isOpened) listener() }
    }
}

/**
 * Scope of a form dialog: rows on twelve columns and the footer, which gets the dialog handle to close it.
 *
 * @since %CURRENT_VERSION%
 */
@TestSysDsl
class DialogScope internal constructor(
    private val shell: DialogShell,
    private val texts: UiTexts,
    private val editState: BlockEditState,
    private val handle: DialogHandle,
) {
    /**
     * Adds a row of the dialog; its elements take at most twelve columns in total.
     *
     * @since %CURRENT_VERSION%
     */
    fun row(content: BlockRowScope.() -> Unit) {
        val row = Div().apply { addClassName(CssClass.BlockRow) }
        BlockRowScope(row, DIALOG_COLUMNS, texts, editState).content()
        if (row.children.findAny().isPresent) shell.content.add(row)
    }

    /**
     * Fills the footer of the dialog; [content] gets the dialog handle, for example to close it.
     *
     * @since %CURRENT_VERSION%
     */
    fun footer(content: ContentScope.(DialogHandle) -> Unit) {
        ContentScope(shell.foot, texts, Placement.Body, DIALOG_COLUMNS).content(handle)
    }
}

/**
 * Builds a form dialog of [title] and an optional [subtitle] with rows and a footer; open it with the returned handle.
 *
 * @since %CURRENT_VERSION%
 */
fun dialog(title: String, subtitle: String? = null, content: DialogScope.() -> Unit): DialogHandle {
    val texts = currentTexts()
    val shell = DialogShell(texts, title = title, subtitle = subtitle, isWide = true, isAlert = false)
    shell.content.addClassName(CssClass.DialogGrid)
    val editState = BlockEditState(shell.dialog.element)
    val handle = DialogHandle(shell, editState)
    DialogScope(shell, texts, editState, handle).content()
    return handle
}
