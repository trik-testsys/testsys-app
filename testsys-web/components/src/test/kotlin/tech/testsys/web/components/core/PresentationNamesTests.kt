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
    fun `should add class names once in native order`() {
        val component = Div()

        component.addClassNames(CssClass.FieldText, CssClass.Icon, CssClass.FieldText)

        assertEquals(listOf("ts-field__text", "ts-icon"), component.classNames.toList())
    }

    @Test
    fun `should move a toggled class name to the end`() {
        val component = Div().apply { addClassNames(CssClass.FieldText, CssClass.Icon) }
        component.setClassName(CssClass.FieldText, false)

        component.setClassName(CssClass.FieldText, true)

        assertEquals(listOf("ts-icon", "ts-field__text"), component.classNames.toList())
    }

    @Test
    fun `should replace all class names`() {
        val component = Div().apply { addClassNames(CssClass.FieldText, CssClass.Icon) }

        component.setClassName(CssClass.TableScroll)

        assertEquals(listOf("ts-table-scroll"), component.classNames.toList())
        assertTrue(component.hasClassName(CssClass.TableScroll))
    }

    @Test
    fun `should report the removal of a present class name`() {
        val component = Div().apply { setClassName(CssClass.TableScroll) }

        val isRemoved = component.removeClassName(CssClass.TableScroll)

        assertTrue(isRemoved)
    }

    @Test
    fun `should report no removal of an absent class name`() {
        val component = Div()

        val isRemoved = component.removeClassName(CssClass.TableScroll)

        assertFalse(isRemoved)
    }

    @Test
    fun `should report a change of the class list when adding`() {
        val classes = Element("div").classList

        assertTrue(classes.add(CssClass.Icon))
        assertTrue(classes.contains(CssClass.Icon))
    }

    @Test
    fun `should report no change when setting a present class`() {
        val classes = Element("div").classList.apply { add(CssClass.Icon) }

        assertFalse(classes.set(CssClass.Icon, true))
    }

    @Test
    fun `should report a change when unsetting a present class`() {
        val classes = Element("div").classList.apply { add(CssClass.Icon) }

        val isChanged = classes.set(CssClass.Icon, false)

        assertTrue(isChanged)
        assertFalse(classes.contains(CssClass.Icon))
    }

    @Test
    fun `should serialize grid geometry`() {
        val style = Element("div").style

        style.setGridColumnSpan(4)
        style.setGridTemplateColumns(8)

        assertEquals("span 4", style.get("grid-column"))
        assertEquals("repeat(8, minmax(0, 1fr))", style.get("grid-template-columns"))
    }

    @Test
    fun `should serialize sizes with their units`() {
        val style = Element("div").style

        style.setWidth(100.0, CssUnit.Percent)
        style.setHeightPixels(28)
        style.setPaddingPixels(0)

        assertEquals("100.0%", style.get("width"))
        assertEquals("28px", style.get("height"))
        assertEquals("0", style.get("padding"))
    }

    @Test
    fun `should serialize the custom properties of components`() {
        val style = Element("div").style

        style.setTableUsed(8)
        style.setTableWidth(100.0)
        style.setMenuColumns(4)
        style.setDownloadOffset(81.7)
        style.setCodeMinLines(10)

        assertEquals("8", style.get("--ts-table-used"))
        assertEquals("100.0%", style.get("--ts-table-width"))
        assertEquals("4", style.get("--ts-menu-columns"))
        assertEquals("81.7", style.get("--ts-download-offset"))
        assertEquals("10", style.get("--ts-code-min-lines"))
    }

    @Test
    fun `should remove the width and return the style`() {
        val style = Element("div").style.apply { setWidth(100.0, CssUnit.Percent) }

        val result = style.removeWidth()

        assertSame(style, result)
        assertFalse(style.has("width"))
    }
}
