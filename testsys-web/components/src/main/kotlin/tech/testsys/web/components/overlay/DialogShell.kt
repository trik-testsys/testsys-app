@file:OptIn(InternalComponentsApi::class)

package tech.testsys.web.components.overlay

import com.vaadin.flow.component.ModalityMode
import com.vaadin.flow.component.dialog.Dialog
import com.vaadin.flow.component.html.Div
import com.vaadin.flow.component.html.H2
import com.vaadin.flow.component.html.Span
import tech.testsys.web.components.actions.iconAction
import tech.testsys.web.components.core.CssClass
import tech.testsys.web.components.core.CssTheme
import tech.testsys.web.components.core.DomProperty
import tech.testsys.web.components.core.ElementRole
import tech.testsys.web.components.core.IconName
import tech.testsys.web.components.core.InternalComponentsApi
import tech.testsys.web.components.core.addClassName
import tech.testsys.web.components.core.addThemeName
import tech.testsys.web.components.core.setAriaRole
import tech.testsys.web.components.core.setProperty
import tech.testsys.web.components.core.svgIcon
import tech.testsys.web.components.layout.ContentScope
import tech.testsys.web.components.layout.Placement
import tech.testsys.web.components.texts.UiTexts

private const val GLYPH_SIZE = 20

/**
 * Width of a dialog card; on a narrow screen every card fits the screen.
 *
 * @since %CURRENT_VERSION%
 */
enum class DialogSize(internal val cssClass: CssClass?) {
    /** 440 px: a confirmation or a short message. */
    S(null),

    /** 520 px: a form of a few fields; the default of a form dialog. */
    M(CssClass.DialogMd),

    /** 720 px: a form with long values or a table. */
    L(CssClass.DialogLg),

    /** 960 px: a wide table or a form of two columns. */
    XL(CssClass.DialogXl),
}

/**
 * Modal `vaadin-dialog` drawing the `.ts-dialog` markup: a head with the title and a close button, or an alert body
 * with a warning glyph; [content] and [foot] take the content. [size] gives the width of the card.
 */
internal class DialogShell(texts: UiTexts, title: String, subtitle: String?, size: DialogSize, isAlert: Boolean) {
    val dialog = Dialog().apply {
        addThemeName(CssTheme.Dialog)
        // Vaadin defaults, set explicitly to state how every dialog closes.
        modality = ModalityMode.STRICT
        isCloseOnEsc = true
        isCloseOnOutsideClick = true
        setAriaRole(if (isAlert) ElementRole.AlertDialog else ElementRole.Dialog)
        // Dialog.setAriaLabel is protected; it only sets this property.
        element.setProperty(DomProperty.AriaLabel, title)
    }
    val body = Div().apply { addClassName(CssClass.DialogBody) }
    val content = Div()
    val foot = Div().apply { addClassName(CssClass.DialogFoot) }

    init {
        val card = Div().apply {
            addClassName(CssClass.Dialog)
            size.cssClass?.let { sizeClass -> addClassName(sizeClass) }
            if (isAlert) addClassName(CssClass.DialogAlert)
        }

        val heading = H2(title).apply { addClassName(CssClass.DialogTitle) }
        if (isAlert) {
            val glyph = Span(svgIcon(IconName.TriangleAlert, GLYPH_SIZE)).apply { addClassName(CssClass.DialogGlyph) }
            body.add(glyph, Div(heading, content).apply { addClassName(CssClass.DialogStack) })
            card.add(body, foot)
        } else {
            val titles = Div(heading).apply { addClassName(CssClass.BlockTitles) }
            subtitle?.let { text -> titles.add(Span(text).apply { addClassName(CssClass.BlockSub) }) }
            val head = Div(titles).apply { addClassName(CssClass.DialogHead) }
            ContentScope(head, texts, Placement.Head).iconAction(IconName.X, texts.dialog.close) { onClick { close() } }
            body.add(content)
            card.add(head, body, foot)
        }

        dialog.add(card)
    }

    fun open() = dialog.open()

    fun close() = dialog.close()
}
