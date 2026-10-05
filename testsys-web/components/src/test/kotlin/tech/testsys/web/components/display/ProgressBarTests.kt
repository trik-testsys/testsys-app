package tech.testsys.web.components.display

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import tech.testsys.web.components.MockVaadinTests
import tech.testsys.web.components.buildTestContent
import tech.testsys.web.components.find

internal class ProgressBarTests : MockVaadinTests() {
    @Test
    fun `should reject invalid progress and render indeterminate without numeric value`() {
        assertThrows(IllegalArgumentException::class.java) { ProgressValue.Determinate(Double.NaN) }
        val root = buildTestContent { progressBar("Progress", ProgressValue.Indeterminate) }

        assertFalse(root.find("ts-progress").element.hasAttribute("aria-valuenow"))
    }
}
