package tech.testsys.web.components.forms

import com.github.mvysny.kaributesting.v10._find
import com.vaadin.flow.component.html.NativeButton
import com.vaadin.flow.component.upload.Upload
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tech.testsys.web.components.MockVaadinTests
import tech.testsys.web.components.buildTestPage
import tech.testsys.web.components.buildTestRow
import tech.testsys.web.components.find
import tech.testsys.web.components.testTexts

class FileDropDisplayTests : MockVaadinTests() {
    private val limits = UploadLimits(maxFiles = 2, maxFileBytes = 4, maxMemoryBytes = 8)

    @Test
    fun `should retain native upload naming and server limits in the compact display`() {
        val row = buildTestRow { fileDrop("Files", limits = limits, consume = {}) }
        val upload = _find<Upload>().single()

        assertEquals("Files", upload.element.getAttribute("aria-label"))
        assertEquals(limits.maxFiles, upload.maxFiles)
        assertEquals(limits.maxFileBytes, upload.maxFileSize)
        assertEquals("Files", row.find("ts-filedrop").element.getAttribute("aria-label"))
        assertTrue(_find<NativeButton>().any { button -> button.text == testTexts.components.upload })
        assertEquals(testTexts.components.uploadLimits(2, 4), row.find("ts-filedrop__meta").find("ts-hint").element.text)
        assertEquals("", row.find("ts-filedrop__status").element.text)
    }

    @Test
    fun `should keep reception disabled when its block is not editable`() {
        lateinit var input: FileDropHandle
        val root = buildTestPage {
            block {
                editing(onSave = { true }, onCancel = {})
                row { input = fileDrop("Files", limits = limits, consume = {}) }
            }
        }

        input.isObscured = true

        assertFalse(_find<Upload>().single().isEnabled)
        assertTrue(root.find("ts-filedrop").element.hasAttribute("data-ts-obscured"))
    }

    @Test
    fun `should retain the idle state after clearing an obscured disabled receiver`() {
        lateinit var input: FileDropHandle
        buildTestRow { input = fileDrop("Files", limits = limits, consume = {}) { isEnabled = false; isObscured = true } }

        input.clear()

        assertEquals(FileUploadState.Idle, input.state.peek())
        assertTrue(input.isObscured)
        assertFalse(input.isEnabled)
        assertFalse(_find<Upload>().single().isEnabled)
    }
}
