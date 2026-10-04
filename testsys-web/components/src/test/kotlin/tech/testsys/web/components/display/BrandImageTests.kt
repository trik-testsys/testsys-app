package tech.testsys.web.components.display

import com.github.mvysny.kaributesting.v10._get
import com.vaadin.flow.component.html.Image
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import tech.testsys.web.components.ElementHandle
import tech.testsys.web.components.MockVaadinTests
import tech.testsys.web.components.find
import tech.testsys.web.components.buildTestContent

class BrandImageTests : MockVaadinTests() {
    @ParameterizedTest
    @EnumSource(BrandAsset::class)
    fun `should preserve canonical brand asset and accessible name`(asset: BrandAsset) {
        buildTestContent { brandImage(asset, label = "TestSys brand") }

        val image = _get<Image> { classes = "ts-brand-image" }
        assertEquals(asset.path, image.src)
        assertEquals("TestSys brand", image.alt.orElseThrow())
        assertTrue("ts-brand-image" in image.element.classList)
    }
    @Test
    fun `should configure visibility of the brand representation through its handle`() {
        lateinit var handle: ElementHandle
        val root = buildTestContent { handle = brandImage(BrandAsset.Header, label = "TestSys") { isVisible = false } }

        assertFalse(root.find("ts-brand-image").isVisible)
        handle.isVisible = true
        assertTrue(root.find("ts-brand-image").isVisible)
    }
}
