package tech.testsys.web.components.overlay

import com.vaadin.flow.component.ModalityMode
import com.vaadin.flow.component.dialog.Dialog
import com.vaadin.flow.component.html.Div
import com.vaadin.flow.component.html.H2
import com.vaadin.flow.component.html.Span
import tech.testsys.web.components.UiTexts
import tech.testsys.web.components.actions.iconAction
import tech.testsys.web.components.core.IconName
import tech.testsys.web.components.core.svgIcon
import tech.testsys.web.components.layout.ContentScope
import tech.testsys.web.components.layout.Placement

private const val GLYPH_SIZE = 20

/**
 * Modal `vaadin-dialog` drawing the `.ts-dialog` markup: a head with the title and a close button, or an alert body
 * with a warning glyph; [content] and [foot] take the content. [isWide] gives the form width.
 */
internal class DialogShell(texts: UiTexts, title: String, subtitle: String?, isWide: Boolean, isAlert: Boolean) {
    val dialog = Dialog().apply {
        addThemeName("ts-dialog")
        // Vaadin defaults, set explicitly to state how every dialog closes.
        modality = ModalityMode.STRICT
        isCloseOnEsc = true
        isCloseOnOutsideClick = true
        setAriaRole(if (isAlert) "alertdialog" else "dialog")
        // Dialog.setAriaLabel is protected; it only sets this property.
        element.setProperty("ariaLabel", title)
    }
    val body = Div().apply { addClassName("ts-dialog__body") }
    val content = Div()
    val foot = Div().apply { addClassName("ts-dialog__foot") }

    init {
        val card = Div().apply {
            addClassName("ts-dialog")
            if (isWide) addClassName("ts-dialog--md")
            if (isAlert) addClassName("ts-dialog--alert")
        }
        val heading = H2(title).apply { addClassName("ts-dialog__title") }
        if (isAlert) {
            val glyph = Span(svgIcon(IconName.TriangleAlert, GLYPH_SIZE)).apply { addClassName("ts-dialog__glyph") }
            body.add(glyph, Div(heading, content).apply { addClassName("ts-dialog__stack") })
            card.add(body, foot)
        } else {
            val titles = Div(heading).apply { addClassName("ts-block__titles") }
            subtitle?.let { text -> titles.add(Span(text).apply { addClassName("ts-block__sub") }) }
            val head = Div(titles).apply { addClassName("ts-dialog__head") }
            ContentScope(head, texts, Placement.Head).iconAction(IconName.X, texts.dialog.close) { onClick { close() } }
            body.add(content)
            card.add(head, body, foot)
        }
        dialog.add(card)
    }

    fun open() = dialog.open()

    fun close() = dialog.close()
}
