package tech.testsys.web.components.display

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import tech.testsys.web.components.DataHandle
import tech.testsys.web.components.MockVaadinTests
import tech.testsys.web.components.buildTestContent
import tech.testsys.web.components.buildTestPage
import tech.testsys.web.components.find

internal class ProgressBarTests : MockVaadinTests() {
    @ParameterizedTest
    @ValueSource(doubles = [Double.NaN, -1.0, 100.5])
    fun `should reject a percent outside the range`(percent: Double) {
        assertThrows(IllegalArgumentException::class.java) { ProgressValue.Determinate(percent) }
    }

    @Test
    fun `should render indeterminate progress without a numeric value`() {
        val root = buildTestContent { progressBar("Progress", ProgressValue.Indeterminate) }

        val progress = root.find("ts-progress")
        assertFalse(progress.element.hasAttribute("aria-valuenow"))
        assertTrue("ts-progress--indeterminate" in progress.element.classList)
        assertNull(progress.find("ts-progress__bar").element.style.get("width"))
    }

    @Test
    fun `should expose a determinate value and the bar width`() {
        val root = buildTestContent { progressBar("Progress", ProgressValue.Determinate(42.5)) }

        val progress = root.find("ts-progress")
        assertEquals("42.5", progress.element.getAttribute("aria-valuenow"))
        assertEquals("42.5%", progress.find("ts-progress__bar").element.style.get("width"))
    }

    @Test
    fun `should switch from indeterminate to a determinate value through the handle`() {
        lateinit var handle: DataHandle<ProgressValue>
        val root = buildTestContent { handle = progressBar("Progress", ProgressValue.Indeterminate) }

        handle.data = ProgressValue.Determinate(10.0)

        val progress = root.find("ts-progress")
        assertFalse("ts-progress--indeterminate" in progress.element.classList)
        assertEquals("10.0", progress.element.getAttribute("aria-valuenow"))
    }

    @ParameterizedTest
    @CsvSource(
        "Neutral,ts-progress--neutral",
        "Info,ts-progress--accent",
        "Success,ts-progress--success",
        "Warning,ts-progress--warning",
        "Danger,ts-progress--danger",
    )
    fun `should color the progress by its tone`(tone: Tone, toneClass: String) {
        val root = buildTestContent { progressBar("Progress", ProgressValue.Determinate(1.0), tone = tone) }

        assertTrue(toneClass in root.find("ts-progress").element.classList)
    }

    @Test
    fun `should use a thin bar in a compact place`() {
        val root = buildTestPage {
            row { block(title = "Блок") { actions { progressBar("Progress", ProgressValue.Determinate(1.0)) } } }
        }

        assertTrue("ts-progress--thin" in root.find("ts-progress").element.classList)
    }

    @Test
    fun `should use a regular bar in the block body`() {
        val root = buildTestContent { progressBar("Progress", ProgressValue.Determinate(1.0)) }

        assertFalse("ts-progress--thin" in root.find("ts-progress").element.classList)
    }
}
