package tech.testsys.web.components.actions

import com.github.mvysny.kaributesting.v10._click
import com.vaadin.flow.component.Component
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.html.NativeButton
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.server.VaadinRequest
import com.vaadin.flow.server.VaadinResponse
import com.vaadin.flow.server.streams.DownloadEvent
import com.vaadin.flow.signals.BindingActiveException
import com.vaadin.flow.signals.local.ValueSignal
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import tech.testsys.web.components.MockVaadinTests
import tech.testsys.web.components.buildTestContent
import tech.testsys.web.components.buildTestRow
import tech.testsys.web.components.child
import tech.testsys.web.components.core.Background
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.concurrent.Executor

class DownloadLifecycleTests : MockVaadinTests() {
    private val tasks = mutableListOf<Runnable>()
    private val unknownLength: (DownloadContext) -> DownloadContent = {
        DownloadContent(filename = "report.txt", contentType = "text/plain") { ByteArrayInputStream(byteArrayOf(42)) }
    }

    @BeforeEach
    fun queueBackgroundTasks() {
        Background.executorOverride = Executor { task -> tasks.add(task) }
    }

    @AfterEach
    fun restoreExecutor() {
        Background.executorOverride = null
    }

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun `should keep the spinner while preparation becomes unknown length progress`(isIconOnly: Boolean) {
        val handle = buildDownload(isIconOnly, unknownLength)
        handle.start()
        val spinner = spinner(handle)

        display(handle).state.set(DownloadState.Downloading(bytes = 1, total = null))
        display(handle).state.set(DownloadState.Downloading(bytes = 2, total = null))

        assertSame(spinner, spinner(handle))
        assertEquals("true", trigger(handle).element.getAttribute("aria-busy"))
    }

    @Test
    fun `should keep the label while preparation becomes unknown length progress`() {
        val handle = buildDownload(isIconOnly = false, produce = unknownLength)
        handle.start()
        val label = label(handle)

        display(handle).state.set(DownloadState.Downloading(bytes = 1, total = null))
        display(handle).state.set(DownloadState.Downloading(bytes = 2, total = null))

        assertSame(label, label(handle))
    }

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun `should clear the busy state after cancelling`(isIconOnly: Boolean) {
        val handle = buildDownload(isIconOnly, unknownLength)
        handle.start()

        handle.cancel()

        assertEquals("false", trigger(handle).element.getAttribute("aria-busy"))
    }

    @ParameterizedTest
    @CsvSource("false,width", "true,--ts-download-offset")
    fun `should retain progress children while known length bytes change`(isIconOnly: Boolean, progressProperty: String) {
        val handle = buildDownload(isIconOnly) {
            DownloadContent(filename = "report.txt", contentType = "text/plain", length = 4) {
                ByteArrayInputStream(byteArrayOf(1, 2, 3, 4))
            }
        }
        display(handle).state.set(DownloadState.Downloading(bytes = 1, total = 4))
        val children = trigger(handle).children.toList()
        val previousProgress = children.first().element.style.get(progressProperty)

        display(handle).state.set(DownloadState.Downloading(bytes = 2, total = 4))

        assertEquals(children, trigger(handle).children.toList())
        assertNotEquals(previousProgress, children.first().element.style.get(progressProperty))
    }

    @Test
    fun `should be idle after attach`() {
        val handle = buildDownload(isIconOnly = false, produce = unknownLength)

        assertEquals(DownloadState.Idle, handle.state.peek())
    }

    @Test
    fun `should prepare after start`() {
        val handle = buildDownload(isIconOnly = false, produce = unknownLength)

        handle.start()

        assertEquals(DownloadState.Preparing, handle.state.peek())
    }

    @Test
    fun `should show an error when preparation fails`() {
        val handle = buildDownload(isIconOnly = false) { throw IOException("Source failure") }
        handle.start()

        tasks.removeAt(0).run()

        assertInstanceOf(DownloadState.Error::class.java, handle.state.peek())
    }

    @Test
    fun `should prepare again on a retry after a failure`() {
        var attempts = 0
        val handle = buildDownload(isIconOnly = false) { context ->
            attempts++
            if (attempts == 1) throw IOException("Source failure")
            unknownLength(context)
        }
        handle.start()
        tasks.removeAt(0).run()
        handle.start()

        tasks.removeAt(0).run()

        assertEquals(2, attempts)
        assertEquals(DownloadState.Preparing, handle.state.peek())
    }

    @Test
    fun `should return to idle on cancel`() {
        val handle = buildDownload(isIconOnly = false, produce = unknownLength)
        handle.start()

        handle.cancel()

        assertEquals(DownloadState.Idle, handle.state.peek())
    }

    @Test
    fun `should cancel a running download on a click on the busy action`() {
        val handle = buildDownload(isIconOnly = false, produce = unknownLength)
        handle.start()

        trigger(handle)._click()

        assertEquals(DownloadState.Idle, handle.state.peek())
    }

    @Test
    fun `should ignore queued preparation after detach`() {
        var preparations = 0
        val handle = buildDownload(isIconOnly = false) { context ->
            preparations++
            unknownLength(context)
        }
        handle.start()

        handle.component.element.removeFromParent()
        tasks.toList().forEach(Runnable::run)

        assertEquals(0, preparations)
        assertEquals(DownloadState.Idle, handle.state.peek())
    }

    @Test
    fun `should disable the action through the handle`() {
        val handle = buildDownload(isIconOnly = false, produce = unknownLength)

        handle.isEnabled = false

        assertFalse(trigger(handle).isEnabled)
    }

    @Test
    fun `should follow a bound enabled signal`() {
        val handle = buildDownload(isIconOnly = false, produce = unknownLength)
        val enabled = ValueSignal(true)
        handle.bindEnabled(enabled)

        enabled.set(false)

        assertFalse(trigger(handle).isEnabled)
    }

    @Test
    fun `should reject a manual enabled state while bound`() {
        val handle = buildDownload(isIconOnly = false, produce = unknownLength)
        handle.bindEnabled(ValueSignal(true))

        assertThrows(BindingActiveException::class.java) { handle.isEnabled = false }
    }

    @Test
    fun `should place a row download action on the requested columns`() {
        val row = buildTestRow { downloadAction("Report", size = 6, produce = unknownLength) }

        assertEquals("span 6", row.child(0).element.style.get("grid-column"))
    }

    @Test
    fun `should send metadata and actual bytes of a resource attempt`() {
        val content = knownContent()
        val (display, attempt) = startedAttempt(content)
        val output = ByteArrayOutputStream()
        val response = response(output)

        display.transfer(downloadEvent(response, display), attempt, content, UI.getCurrent())

        assertArrayEquals(byteArrayOf(1, 2, 3), output.toByteArray())
        assertEquals(DownloadState.Done(3), display.state.peek())
        verify { response.setContentType("text/plain") }
        verify { response.setContentLengthLong(3) }
        verify { response.setHeader("Content-Disposition", "attachment; filename=\"report.txt\"") }
        verify { response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate") }
    }

    @Test
    fun `should reject a second transfer of the same attempt as gone`() {
        val content = knownContent()
        val (display, attempt) = startedAttempt(content)
        val response = response(ByteArrayOutputStream())
        val event = downloadEvent(response, display)
        display.transfer(event, attempt, content, UI.getCurrent())

        display.transfer(event, attempt, content, UI.getCurrent())

        verify { response.setStatus(410) }
    }

    private fun buildDownload(isIconOnly: Boolean, produce: (DownloadContext) -> DownloadContent): DownloadHandle {
        lateinit var handle: DownloadHandle
        buildTestContent { handle = if (isIconOnly) iconDownloadAction("Report", produce) else downloadAction("Report", produce) }
        return handle
    }

    private fun display(handle: DownloadHandle): DownloadDisplay = requireNotNull(handle.component as? DownloadDisplay)

    private fun trigger(handle: DownloadHandle): NativeButton = display(handle).children.toList().filterIsInstance<NativeButton>().first()

    private fun spinner(handle: DownloadHandle): Component =
        trigger(handle).children.toList().single { child -> "ts-spinner" in child.element.classList }

    private fun label(handle: DownloadHandle): Component =
        trigger(handle).children.toList().first { child -> child is Span && "ts-spinner" !in child.element.classList }

    private fun knownContent() =
        DownloadContent(filename = "report.txt", contentType = "text/plain", length = 3) { ByteArrayInputStream(byteArrayOf(1, 2, 3)) }

    private fun startedAttempt(content: DownloadContent): Pair<DownloadDisplay, DownloadAttempt> {
        Background.executorOverride = Executor(Runnable::run)
        lateinit var handle: DownloadHandle
        buildTestContent { handle = downloadAction("Report", produce = { _ -> content }) }
        handle.start()
        return display(handle) to requireNotNull(display(handle).activeAttempt())
    }

    private fun response(output: ByteArrayOutputStream): VaadinResponse = mockk<VaadinResponse>(relaxed = true).apply {
        every { outputStream } returns output
    }

    private fun downloadEvent(response: VaadinResponse, display: DownloadDisplay) =
        DownloadEvent(mockk<VaadinRequest>(), response, UI.getCurrent().session, display.element)
}
