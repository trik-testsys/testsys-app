package tech.testsys.web.ui.actions

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException

class DownloadButtonTests {
    private class Output : ByteArrayOutputStream() {
        var closed = false
        override fun close() {
            closed = true
            super.close()
        }
    }

    @Test
    fun `should stream known length bytes and close both streams`() {
        var inputClosed = false
        val output = Output()
        val input = object : ByteArrayInputStream(byteArrayOf(1, 2, 3)) {
            override fun close() {
                inputClosed = true
                super.close()
            }
        }
        val content = DownloadContent("example.bin", "application/octet-stream", 3) { input }
        val progress = mutableListOf<Long>()

        val count = streamDownload(content, DownloadContext(), output, progress = progress::add)

        assertEquals(3L, count)
        assertArrayEquals(byteArrayOf(1, 2, 3), output.toByteArray())
        assertEquals(listOf(0L, 3L), progress)
        assertTrue(inputClosed)
        assertTrue(output.closed)
    }

    @Test
    fun `should close output when opening source fails`() {
        val output = Output()
        val content = DownloadContent("file", "text/plain") { throw IOException("source failure") }

        assertThrows(IOException::class.java) { streamDownload(content, DownloadContext(), output) {} }

        assertTrue(output.closed)
    }

    @Test
    fun `should stop actual transfer after cooperative cancellation`() {
        val context = DownloadContext()
        val output = Output()
        val content = DownloadContent("file", "application/octet-stream") { ByteArrayInputStream(ByteArray(100000)) }

        assertThrows(IOException::class.java) { streamDownload(content, context, output) { count -> if (count > 0) context.cancel() } }

        assertTrue(output.closed)
        assertTrue(output.size() < 100000)
    }

    @Test
    fun `should detect early EOF and close streams`() {
        val output = Output()
        val content = DownloadContent("short", "text/plain", 10) { ByteArrayInputStream(ByteArray(1)) }

        assertThrows(IOException::class.java) { streamDownload(content, DownloadContext(), output) {} }

        assertTrue(output.closed)
    }

    @Test
    fun `should create a fresh stream on each unknown length attempt`() {
        var opens = 0
        val content = DownloadContent("file", "text/plain") {
            opens++
            ByteArrayInputStream(byteArrayOf(42))
        }

        val first = streamDownload(content, DownloadContext(), Output()) {}
        val second = streamDownload(content, DownloadContext(), Output()) {}

        assertEquals(1L, first)
        assertEquals(1L, second)
        assertEquals(2, opens)
    }
    @Test
    fun `should report HTTP failure before closing output when source opening fails`() {
        var status = 200
        var closedAtStatus = 0
        val output = object : ByteArrayOutputStream() {
            override fun close() { closedAtStatus = status; super.close() }
        }
        val content = DownloadContent("file", "text/plain") { throw IOException("Source failure") }

        assertThrows(IOException::class.java) {
            streamDownload(content, DownloadContext(), output, onFailure = { status = 500 }) {}
        }

        assertEquals(500, closedAtStatus)
    }
}
