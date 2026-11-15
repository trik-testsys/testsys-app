package tech.testsys.web.components.layout

import com.vaadin.flow.component.UI
import com.vaadin.flow.router.RouteParameters
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tech.testsys.web.components.ItemTestView
import tech.testsys.web.components.MockVaadinTests
import tech.testsys.web.components.find
import tech.testsys.web.components.findAll
import tech.testsys.web.components.testTexts

internal class PageFooterScopeTests : MockVaadinTests() {
    @Test
    fun `should render the brand name and the version of the UI in this order`() {
        setFooterVersion(UI.getCurrent(), "1.2.3")

        val footer = buildPageFooter(testTexts)

        assertEquals("footer", footer.element.tag)
        assertEquals(
            listOf(testTexts.messages.uiPage.brandName(), "1.2.3"),
            footer.find("ts-footer__brand").children.map { child -> child.element.text }.toList(),
        )
        assertEquals("1.2.3", footer.find("ts-footer__version").element.text)
    }

    @Test
    fun `should omit the version if the UI has none`() {
        val footer = buildPageFooter(testTexts)

        assertTrue(footer.findAll("ts-footer__version").isEmpty())
        assertEquals(testTexts.messages.uiPage.brandName(), footer.find("ts-footer__name").element.text)
    }

    @Test
    fun `should place the links before the brand`() {
        val links = PageFooterScope().apply { link(label = "Source", href = "/source") }

        val footer = buildPageFooter(testTexts, links)

        assertEquals(
            listOf("ts-footer__links", "ts-footer__brand"),
            footer.find("ts-footer__inner").children.map { child -> child.element.getAttribute("class") }.toList(),
        )
    }

    @Test
    fun `should omit navigation if the footer has no links`() {
        val footer = buildPageFooter(testTexts)

        assertTrue(footer.findAll("ts-footer__links").isEmpty())
    }

    @Test
    fun `should keep configured route parameters and URL links in their display order`() {
        val links = PageFooterScope().apply {
            link("Item", ItemTestView::class.java, RouteParameters("id", "7"))
            link(label = "Source", href = "https://github.com/trik-testsys/testsys-app")
        }

        val footer = buildPageFooter(testTexts, links)

        val rendered = footer.findAll("ts-footer__link")
        assertEquals(testTexts.messages.uiPage.footerLinks(), footer.find("ts-footer__links").element.getAttribute("aria-label"))
        assertEquals(listOf("Item", "Source"), rendered.map { link -> link.element.textRecursively })
        assertEquals(
            listOf("test/item/7", "https://github.com/trik-testsys/testsys-app"),
            rendered.map { link -> link.element.getAttribute("href") },
        )
        assertEquals(listOf(null, null), rendered.map { link -> link.element.getAttribute("target") })
    }

    @Test
    fun `should add the common links of the UI after the links of the page at each build`() {
        var label = "Первая"
        setCommonFooterLinks(UI.getCurrent()) { link(label, href = "/common") }
        buildPageFooter(testTexts)
        label = "Вторая"
        val links = PageFooterScope().apply { link("Item", ItemTestView::class.java, RouteParameters("id", "7")) }

        val footer = buildPageFooter(testTexts, links)

        assertEquals(listOf("Item", "Вторая"), footer.findAll("ts-footer__link").map { link -> link.element.textRecursively })
    }
}
