package tech.testsys.web.components.actions

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
        var isClosed = false
        override fun close() {
            isClosed = true
            super.close()
        }
    }

    @Test
    fun `should stream known length bytes and close both streams`() {
        var isInputClosed = false
        val output = Output()
        val input = object : ByteArrayInputStream(byteArrayOf(1, 2, 3)) {
            override fun close() {
                isInputClosed = true
                super.close()
            }
        }
        val content = DownloadContent(filename = "example.bin", contentType = "application/octet-stream", length = 3) {
            input
        }
        val progress = mutableListOf<Long>()

        val count = streamDownload(content, DownloadContext(), output, progress = progress::add)

        assertEquals(3L, count)
        assertArrayEquals(byteArrayOf(1, 2, 3), output.toByteArray())
        assertEquals(listOf(0L, 3L), progress)
        assertTrue(isInputClosed)
        assertTrue(output.isClosed)
    }

    @Test
    fun `should close output when opening source fails`() {
        val output = Output()
        val content = DownloadContent(filename = "file", contentType = "text/plain") {
            throw IOException("source failure")
        }

        assertThrows(IOException::class.java) { streamDownload(content, DownloadContext(), output) {} }

        assertTrue(output.isClosed)
    }

    @Test
    fun `should stop actual transfer after cooperative cancellation`() {
        val context = DownloadContext()
        val output = Output()
        val content = DownloadContent(filename = "file", contentType = "application/octet-stream") {
            ByteArrayInputStream(ByteArray(100_000))
        }

        assertThrows(IOException::class.java) { streamDownload(content, context, output) { count -> if (count > 0) context.cancel() } }

        assertTrue(output.isClosed)
        assertTrue(output.size() < 100_000)
    }

    @Test
    fun `should detect early EOF and close streams`() {
        val output = Output()
        val content = DownloadContent(filename = "short", contentType = "text/plain", length = 10) {
            ByteArrayInputStream(ByteArray(1))
        }

        assertThrows(IOException::class.java) { streamDownload(content, DownloadContext(), output) {} }

        assertTrue(output.isClosed)
    }

    @Test
    fun `should create a fresh stream on each unknown length attempt`() {
        var opens = 0
        val content = DownloadContent(filename = "file", contentType = "text/plain") {
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
            override fun close() {
                closedAtStatus = status
                super.close()
            }
        }
        val content = DownloadContent(filename = "file", contentType = "text/plain") {
            throw IOException("Source failure")
        }

        assertThrows(IOException::class.java) {
            streamDownload(content, DownloadContext(), output, onFailure = { status = 500 }) {}
        }

        assertEquals(500, closedAtStatus)
    }

    @Test
    fun `should reject a blank download filename`() {
        assertThrows(IllegalArgumentException::class.java) { DownloadContent(filename = " ", contentType = "text/plain") { empty() } }
    }

    @Test
    fun `should reject a download filename with a line break`() {
        assertThrows(IllegalArgumentException::class.java) {
            DownloadContent(filename = "a\nb.txt", contentType = "text/plain") { empty() }
        }
    }

    @Test
    fun `should reject a blank download content type`() {
        assertThrows(IllegalArgumentException::class.java) { DownloadContent(filename = "a.txt", contentType = "") { empty() } }
    }

    @Test
    fun `should reject a negative download length`() {
        assertThrows(IllegalArgumentException::class.java) {
            DownloadContent(filename = "a.txt", contentType = "text/plain", length = -1) { empty() }
        }
    }

    private fun empty() = ByteArrayInputStream(ByteArray(0))
}
