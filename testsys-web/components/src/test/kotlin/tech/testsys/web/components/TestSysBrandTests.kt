package tech.testsys.web.components

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import tech.testsys.web.components.error.NotFoundPage

class TestSysBrandTests : MockVaadinTests() {
    @Test
    fun `should package brand graphics and favicon as public resources`() {
        val assets =
            listOf(TestSysBrand.EMBLEM, TestSysBrand.WORDMARK, TestSysBrand.HEADER, TestSysBrand.FOOTER, TestSysBrand.FAVICON)

        val resources = assets.map { asset -> javaClass.getResource("/META-INF/resources/$asset") }

        assets.forEach { asset -> org.junit.jupiter.api.Assertions.assertTrue(asset.startsWith("testsys-ui/brand/")) }
        resources.forEach { resource -> assertNotNull(resource) }
    }

    @Test
    fun `should give the missing route brand a localized accessible name`() {
        val page = NotFoundPage(testTexts)

        val brand = page.find("ts-brand")

        assertEquals(testTexts.brand, brand.element.getAttribute("aria-label"))
        assertEquals("img", brand.element.getAttribute("role"))
        assertEquals(1L, brand.children.count())
        assertEquals(TestSysBrand.HEADER, brand.find("ts-brand__logo").element.getAttribute("src"))
        assertEquals("", brand.find("ts-brand__logo").element.getAttribute("alt"))
    }
}
