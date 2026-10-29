package tech.testsys.web.components.texts

import com.github.mvysny.kaributesting.v10.MockVaadin
import com.vaadin.flow.component.ComponentUtil
import com.vaadin.flow.component.UI
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import tech.testsys.web.components.MockVaadinTests
import tech.testsys.web.components.testTexts
import java.time.ZoneOffset

class TextsHolderTests : MockVaadinTests() {
    @Test
    fun `should give the localization bound to the current UI`() {
        assertSame(testTexts, currentUiLocalization())
        assertSame(testTexts.messages, currentLocalization())
        assertEquals(ZoneOffset.UTC, currentTimeZone())
    }

    @Test
    fun `should reject reading the localization of a UI it is not bound to`() {
        ComponentUtil.setData(UI.getCurrent(), UiLocalization::class.java, null)

        assertThrows<IllegalStateException> { currentLocalization() }
    }

    @Test
    fun `should reject reading the localization without a current UI`() {
        MockVaadin.tearDown()

        assertThrows<IllegalStateException> { currentTimeZone() }
    }
}
