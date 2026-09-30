package tech.testsys.web.ui.actions

import com.vaadin.flow.component.UI
import com.vaadin.flow.component.html.NativeButton
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.server.VaadinRequest
import com.vaadin.flow.server.VaadinResponse
import com.vaadin.flow.server.streams.DownloadEvent
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import tech.testsys.web.ui.Background
import tech.testsys.web.ui.MockVaadinTests
import tech.testsys.web.ui.buildTestContent
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.concurrent.Executor

class DownloadLifecycleTests : MockVaadinTests() {
    private val tasks = mutableListOf<Runnable>()

    @AfterEach
    fun restoreExecutor() { Background.executorOverride = null }

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun `should retain spinner while preparation becomes unknown length progress`(isIconOnly: Boolean) {
        Background.executorOverride = Executor { task -> tasks.add(task) }
        lateinit var handle: DownloadHandle
        buildTestContent {
            val producer: (DownloadContext) -> DownloadContent = {
                DownloadContent("report.txt", "text/plain") { ByteArrayInputStream(byteArrayOf(42)) }
            }
            handle = if (isIconOnly) iconDownloadAction("Report", producer) else downloadAction("Report", producer)
        }
        handle.start()
        val display = requireNotNull(handle.component as? DownloadDisplay)
        val button = display.children.filter { child -> child is NativeButton }.findFirst().orElseThrow()
        val spinner = button.children.filter { child -> "ts-spinner" in child.element.classList }.findFirst().orElseThrow()
        val label = button.children.filter { child -> child is Span && child !== spinner }.findFirst().orElse(null)

        display.state.set(DownloadState.Downloading(bytes = 1, total = null))
        display.state.set(DownloadState.Downloading(bytes = 2, total = null))

        assertSame(spinner, button.children.filter { child -> "ts-spinner" in child.element.classList }.findFirst().orElseThrow())
        if (label != null) assertSame(label, button.children.filter { child -> child is Span && child !== spinner }.findFirst().orElseThrow())
        assertEquals("true", button.element.getAttribute("aria-busy"))
        handle.cancel()
        assertEquals("false", button.element.getAttribute("aria-busy"))
    }

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun `should retain progress children while known length bytes change`(isIconOnly: Boolean) {
        lateinit var handle: DownloadHandle
        buildTestContent {
            val producer: (DownloadContext) -> DownloadContent = {
                DownloadContent("report.txt", "text/plain", 4) { ByteArrayInputStream(byteArrayOf(1, 2, 3, 4)) }
            }
            handle = if (isIconOnly) iconDownloadAction("Report", producer) else downloadAction("Report", producer)
        }
        val display = requireNotNull(handle.component as? DownloadDisplay)
        display.state.set(DownloadState.Downloading(bytes = 1, total = 4))
        val button = display.children.filter { child -> child is NativeButton }.findFirst().orElseThrow()
        val children = button.children.toList()
        val progressProperty = if (isIconOnly) "--ts-download-offset" else "width"
        val previousProgress = children.first().element.style.get(progressProperty)

        display.state.set(DownloadState.Downloading(bytes = 2, total = 4))

        children.forEachIndexed { index, child -> assertSame(child, button.children.toList()[index]) }
        assertNotEquals(previousProgress, children.first().element.style.get(progressProperty))
    }

    @Test
    fun `should attach idle then prepare fail and retry without reading signal reactively`() {
        Background.executorOverride = Executor { task -> tasks.add(task) }
        var attempts = 0
        lateinit var handle: DownloadHandle
        buildTestContent {
            handle = downloadAction("Report", produce = { _ ->
                attempts++
                if (attempts == 1) throw IOException("Source failure")
                DownloadContent("report.txt", "text/plain") { ByteArrayInputStream(byteArrayOf(42)) }
            })
        }
        assertEquals(DownloadState.Idle, handle.state.peek())

        handle.start()
        assertEquals(DownloadState.Preparing, handle.state.peek())
        tasks.removeAt(0).run()
        assertTrue(handle.state.peek() is DownloadState.Error)
        handle.start()
        tasks.removeAt(0).run()

        assertEquals(2, attempts)
        assertEquals(DownloadState.Preparing, handle.state.peek())
        handle.cancel()
        assertEquals(DownloadState.Idle, handle.state.peek())
    }

    @Test
    fun `should ignore queued preparation after detach`() {
        Background.executorOverride = Executor { task -> tasks.add(task) }
        var preparations = 0
        lateinit var handle: DownloadHandle
        buildTestContent {
            handle = downloadAction("Report", produce = { _ ->
                preparations++
                DownloadContent("report.txt", "text/plain") { ByteArrayInputStream(byteArrayOf(42)) }
            })
        }
        handle.start()

        handle.component.element.removeFromParent()
        tasks.toList().forEach(Runnable::run)

        assertEquals(0, preparations)
        assertEquals(DownloadState.Idle, handle.state.peek())
    }

    @Test
    fun `should send metadata and actual bytes once per resource attempt`() {
        Background.executorOverride = Executor(Runnable::run)
        val content = DownloadContent("report.txt", "text/plain", 3) { ByteArrayInputStream(byteArrayOf(1, 2, 3)) }
        lateinit var handle: DownloadHandle
        buildTestContent { handle = downloadAction("Report", produce = { _ -> content }) }
        handle.start()
        val display = requireNotNull(handle.component as? DownloadDisplay)
        val attempt = requireNotNull(display.activeAttempt())
        val output = ByteArrayOutputStream()
        val response = mockk<VaadinResponse>(relaxed = true)
        every { response.outputStream } returns output
        val event = DownloadEvent(mockk<VaadinRequest>(), response, UI.getCurrent().session, display.element)

        display.transfer(event, attempt, content, UI.getCurrent())

        assertArrayEquals(byteArrayOf(1, 2, 3), output.toByteArray())
        assertEquals(DownloadState.Done(3), handle.state.peek())
        verify { response.setContentType("text/plain") }
        verify { response.setContentLengthLong(3) }
        verify { response.setHeader("Content-Disposition", "attachment; filename=\"report.txt\"") }
        verify { response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate") }
        display.transfer(event, attempt, content, UI.getCurrent())
        verify { response.setStatus(410) }
    }
}
