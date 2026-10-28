package tech.testsys.web.components

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import tech.testsys.web.components.error.NotFoundPage

class TestSysBrandTests : MockVaadinTests() {
    @ParameterizedTest
    @ValueSource(strings = [TestSysBrand.EMBLEM, TestSysBrand.WORDMARK, TestSysBrand.ICON, TestSysBrand.FOOTER, TestSysBrand.FAVICON])
    fun `should package a brand graphic as a public resource`(asset: String) {
        val resource = javaClass.getResource("/META-INF/resources/$asset")

        assertTrue(asset.startsWith("testsys-ui/brand/"))
        assertNotNull(resource)
    }

    @Test
    fun `should give the missing route brand a localized accessible name`() {
        val page = NotFoundPage()

        val brand = page.find("ts-brand")

        assertEquals(testTexts.messages.uiPage.brandName(), brand.element.getAttribute("aria-label"))
        assertEquals("img", brand.element.getAttribute("role"))
        assertEquals(2L, brand.children.count())
        assertEquals(TestSysBrand.ICON, brand.find("ts-brand__emblem").element.getAttribute("src"))
        assertEquals(TestSysBrand.WORDMARK, brand.find("ts-brand__wordmark").element.getAttribute("src"))
        assertEquals("", brand.find("ts-brand__emblem").element.getAttribute("alt"))
        assertEquals("", brand.find("ts-brand__wordmark").element.getAttribute("alt"))
    }
}
