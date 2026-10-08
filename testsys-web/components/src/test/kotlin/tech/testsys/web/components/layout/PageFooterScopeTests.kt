package tech.testsys.web.components.layout

import com.vaadin.flow.router.RouteParameters
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import tech.testsys.web.components.ItemTestView
import tech.testsys.web.components.MockVaadinTests
import tech.testsys.web.components.TestSysBrand
import tech.testsys.web.components.find
import tech.testsys.web.components.findAll
import tech.testsys.web.components.testTexts
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

internal class PageFooterScopeTests : MockVaadinTests() {
    private val fixedClock = Clock.fixed(Instant.parse("2037-04-05T00:00:00Z"), ZoneOffset.UTC)

    @Test
    fun `should render the accessible graphic brand without repeating its name as text`() {
        val footer = buildPageFooter(testTexts, clock = fixedClock)

        assertEquals("footer", footer.element.tag)
        assertEquals(TestSysBrand.FOOTER, footer.find("ts-footer__logo").element.getAttribute("src"))
        assertEquals(testTexts.brand, footer.find("ts-footer__logo").element.getAttribute("alt"))
        assertEquals("2037", footer.element.textRecursively)
    }

    @Test
    fun `should omit navigation if the footer has no links`() {
        val footer = buildPageFooter(testTexts, clock = fixedClock)

        assertTrue(footer.findAll("ts-footer__links").isEmpty())
    }

    @Test
    fun `should keep configured route parameters and URL links in their display order`() {
        val links = PageFooterScope().apply {
            link("Item", ItemTestView::class.java, RouteParameters("id", "7"))
            link(label = "Source", href = "https://github.com/trik-testsys/testsys-app")
        }

        val footer = buildPageFooter(testTexts, links, clock = fixedClock)

        val rendered = footer.findAll("ts-footer__link")
        assertEquals(testTexts.footer.links, footer.find("ts-footer__links").element.getAttribute("aria-label"))
        assertEquals(listOf("Item", "Source"), rendered.map { link -> link.element.textRecursively })
        assertEquals(
            listOf("test/item/7", "https://github.com/trik-testsys/testsys-app"),
            rendered.map { link -> link.element.getAttribute("href") },
        )
        assertEquals(listOf(null, null), rendered.map { link -> link.element.getAttribute("target") })
    }

    @ParameterizedTest
    @CsvSource("2030-12-31T23:59:59Z,2030", "2031-01-01T00:00:00Z,2031")
    fun `should compute the current year at rendering across the year boundary`(instant: String, expectedYear: String) {
        val clock = Clock.fixed(Instant.parse(instant), ZoneOffset.UTC)

        val footer = buildPageFooter(testTexts, clock = clock)

        assertEquals(expectedYear, footer.find("ts-footer__year").element.text)
    }
}
