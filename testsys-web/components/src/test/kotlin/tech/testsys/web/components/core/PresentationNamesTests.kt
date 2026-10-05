@file:OptIn(InternalComponentsApi::class)

package tech.testsys.web.components.core

import com.vaadin.flow.component.html.Div
import com.vaadin.flow.dom.Element
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class PresentationNamesTests {
    @Test
    fun `should preserve native class order toggling and replacement`() {
        val component = Div()

        component.addClassNames(CssClass.FieldText, CssClass.Icon, CssClass.FieldText)
        assertEquals(listOf("ts-field__text", "ts-icon"), component.classNames.toList())
        component.setClassName(CssClass.FieldText, false)
        component.setClassName(CssClass.FieldText, true)
        assertEquals(listOf("ts-icon", "ts-field__text"), component.classNames.toList())
        component.setClassName(CssClass.TableScroll)

        assertEquals(listOf("ts-table-scroll"), component.classNames.toList())
        assertTrue(component.hasClassName(CssClass.TableScroll))
        assertTrue(component.removeClassName(CssClass.TableScroll))
        assertFalse(component.removeClassName(CssClass.TableScroll))
    }

    @Test
    fun `should retain native class list change results`() {
        val classes = Element("div").classList

        assertTrue(classes.add(CssClass.Icon))
        assertFalse(classes.set(CssClass.Icon, true))
        assertTrue(classes.contains(CssClass.Icon))
        assertTrue(classes.set(CssClass.Icon, false))
        assertFalse(classes.remove(CssClass.Icon))
    }

    @Test
    fun `should serialize grid geometry units tokens and width removal`() {
        val style = Element("div").style

        style.setGridColumnSpan(4)
        style.setGridTemplateColumns(8)
        style.setWidth(100.0, CssUnit.Percent)
        style.setHeightPixels(28)
        style.setPaddingPixels(0)
        style.setTableUsed(8)
        style.setTableWidth(100.0)
        style.setMenuColumns(4)
        style.setDownloadOffset(81.7)
        style.setCodeMinLines(10)
        style.setToken(CssProperty.Background, "--accent")

        assertEquals("span 4", style.get("grid-column"))
        assertEquals("repeat(8, minmax(0, 1fr))", style.get("grid-template-columns"))
        assertEquals("100.0%", style.get("width"))
        assertEquals("28px", style.get("height"))
        assertEquals("0", style.get("padding"))
        assertEquals("8", style.get("--ts-table-used"))
        assertEquals("100.0%", style.get("--ts-table-width"))
        assertEquals("4", style.get("--ts-menu-columns"))
        assertEquals("81.7", style.get("--ts-download-offset"))
        assertEquals("10", style.get("--ts-code-min-lines"))
        assertEquals("var(--accent)", style.get("background"))
        assertSame(style, style.removeWidth())
        assertFalse(style.has("width"))
    }
}
