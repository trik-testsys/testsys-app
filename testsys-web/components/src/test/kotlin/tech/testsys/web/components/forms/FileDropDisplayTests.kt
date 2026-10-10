package tech.testsys.web.components.forms

import com.github.mvysny.kaributesting.v10._find
import com.github.mvysny.kaributesting.v10._fireDomEvent
import com.vaadin.flow.component.html.NativeButton
import com.vaadin.flow.component.upload.Upload
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tech.testsys.web.components.MockVaadinTests
import tech.testsys.web.components.buildTestPage
import tech.testsys.web.components.buildTestRow
import tech.testsys.web.components.button
import tech.testsys.web.components.find
import tech.testsys.web.components.testTexts
import tools.jackson.databind.ObjectMapper
import java.io.ByteArrayInputStream
import java.io.IOException
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class FileDropDisplayTests : MockVaadinTests() {
    private val limits = UploadLimits(maxFiles = 2, maxFileBytes = 4, maxMemoryBytes = 8)
    private val entered = CountDownLatch(1)
    private val finish = CountDownLatch(1)
    private val pool = Executors.newSingleThreadExecutor()

    @AfterEach
    fun releaseHandlers() {
        finish.countDown()
        pool.shutdownNow()
    }

    @Test
    fun `should place the receiver as a labeled field and keep its accessible name`() {
        lateinit var handle: FileDropHandle
        val row = buildTestRow {
            handle = fileDrop("New file", limits = limits, labelSize = 4, size = 8, consume = {})
        }
        val engine = display().engine
        engine.receiveFile("done.txt")

        assertEquals(listOf(identity(engine, "done.txt")), handle.fileIds)
        assertEquals("New file", row.find("ts-field__text").element.text)
        assertEquals("span 12", row.find("ts-field").element.style.get("grid-column"))
        assertEquals("New file", _find<Upload>().single().element.getAttribute("aria-label"))
        assertFalse(display().children.anyMatch { child -> child.element.tag == "span" && child.element.text == "New file" })
    }

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
    fun `should name file removal and a stalled transfer with their own texts`() {
        buildTestRow { fileDrop("Files", limits = limits, consume = {}) }

        val i18n = _find<Upload>().single().i18n

        assertEquals(testTexts.components.removeFile, i18n.file.remove)
        assertEquals(testTexts.components.stalled, i18n.uploading.status.stalled)
    }

    @Test
    fun `should keep reception disabled when its block is not editable`() {
        lateinit var input: FileDropHandle
        val root = buildTestPage {
            row {
                block {
                    editing(onSave = { true }, onCancel = {})
                    row { input = fileDrop("Files", limits = limits, consume = {}) }
                }
            }
        }

        input.isObscured = true

        assertFalse(_find<Upload>().single().isEnabled)
        assertTrue(root.find("ts-filedrop").element.hasAttribute("data-ts-obscured"))
    }

    @Test
    fun `should retain the idle state after clearing an obscured disabled receiver`() {
        lateinit var input: FileDropHandle
        buildTestRow {
            input = fileDrop("Files", limits = limits, consume = {}) {
                isEnabled = false
                isObscured = true
            }
        }

        input.clear()

        assertEquals(FileUploadState.Idle, input.state.peek())
        assertTrue(input.isObscured)
        assertFalse(input.isEnabled)
        assertFalse(_find<Upload>().single().isEnabled)
    }

    @Test
    fun `should show the cancelled status after cancelling a running transfer`() {
        lateinit var input: FileDropHandle
        val row = buildTestRow { input = fileDrop("Files", limits = limits, consume = {}) }
        display().state.set(FileUploadState.Uploading("a.txt", bytes = 1, total = 4))

        input.cancel()

        assertEquals(FileUploadState.Cancelled, input.state.peek())
        assertEquals(testTexts.components.cancelled, row.find("ts-filedrop__status").element.text)
    }

    @Test
    fun `should cancel the state when the running transfer is removed while another file remains`() {
        lateinit var input: FileDropHandle
        buildTestRow { input = fileDrop("Files", limits = limits, consume = { file -> blockFor(file, "running.txt") }) }
        val engine = display().engine
        engine.receiveFile("done.txt")
        pool.submit { engine.receiveFile("running.txt") }
        assertTrue(entered.await(5, TimeUnit.SECONDS))
        display().state.set(FileUploadState.Processing("running.txt"))

        _find<Upload>().single()._fireDomEvent("testsys-transfer-remove", removal(identity(engine, "running.txt")))

        assertEquals(FileUploadState.Cancelled, input.state.peek())
        assertFalse(button(testTexts.components.cancel).isVisible)
    }

    @Test
    fun `should cancel the state when a running transfer is removed while another transfer shows its result`() {
        lateinit var input: FileDropHandle
        buildTestRow { input = fileDrop("Files", limits = limits, consume = { file -> blockFor(file, "running.txt") }) }
        val engine = display().engine
        engine.receiveFile("done.txt")
        pool.submit { engine.receiveFile("running.txt") }
        entered.await(5, TimeUnit.SECONDS)
        display().state.set(FileUploadState.Done("done.txt", 1))

        _find<Upload>().single()._fireDomEvent("testsys-transfer-remove", removal(identity(engine, "running.txt")))

        assertEquals(FileUploadState.Cancelled, input.state.peek())
    }

    @Test
    fun `should cancel the state when detached while another transfer shows its error`() {
        lateinit var input: FileDropHandle
        val row = buildTestRow {
            input = fileDrop("Files", limits = limits, consume = { file -> blockFor(file, "running.txt") })
        }
        val engine = display().engine
        pool.submit { engine.receiveFile("running.txt") }
        entered.await(5, TimeUnit.SECONDS)
        display().state.set(FileUploadState.Error("other.txt", IOException("Other transfer failure")))

        row.element.removeFromParent()

        assertEquals(FileUploadState.Cancelled, input.state.peek())
    }

    @Test
    fun `should return to idle when the last file is removed`() {
        lateinit var input: FileDropHandle
        buildTestRow { input = fileDrop("Files", limits = limits, consume = {}) }
        val engine = display().engine
        engine.receiveFile("done.txt")
        display().state.set(FileUploadState.Done("done.txt", 1))

        _find<Upload>().single()._fireDomEvent("testsys-transfer-remove", removal(identity(engine, "done.txt")))

        assertEquals(FileUploadState.Idle, input.state.peek())
    }

    @Test
    fun `should cancel the state when detached during a transfer`() {
        lateinit var input: FileDropHandle
        val row = buildTestRow { input = fileDrop("Files", limits = limits, consume = {}) }
        display().state.set(FileUploadState.Uploading("a.txt", bytes = 1, total = 4))

        row.element.removeFromParent()

        assertEquals(FileUploadState.Cancelled, input.state.peek())
    }

    private fun display(): FileDropDisplay = _find<FileDropDisplay>().single()

    private fun blockFor(file: UploadedFile, blocked: String) {
        if (file.filename == blocked) {
            entered.countDown()
            finish.await(5, TimeUnit.SECONDS)
        }
    }

    private fun BoundedUploads.receiveFile(filename: String) = receive(
        transferId = identity(this, filename),
        filename = filename,
        mime = "text/plain",
        declared = 1,
        stream = ByteArrayInputStream(ByteArray(1)),
    )

    private fun removal(identity: String) = ObjectMapper().createObjectNode().put("event.detail.identity", identity)

    private fun identity(engine: BoundedUploads, name: String): String =
        "${engine.generation()}:${UUID.nameUUIDFromBytes(name.toByteArray())}"
}
