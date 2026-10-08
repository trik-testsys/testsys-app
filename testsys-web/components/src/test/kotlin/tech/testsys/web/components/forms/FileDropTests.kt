package tech.testsys.web.components.forms

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit

class FileDropTests {
    private val limits = UploadLimits(maxFiles = 1, maxFileBytes = 4, maxMemoryBytes = 4)
    private val twoFiles = limits.copy(maxFiles = 2, maxMemoryBytes = 8)
    private val textOnly = limits.copy(mimeTypes = setOf("text/*"), extensions = setOf(".txt"))
    private val entered = CountDownLatch(1)
    private val finish = CountDownLatch(1)
    private val pool = Executors.newFixedThreadPool(2)

    @AfterEach
    fun releaseHandlers() {
        finish.countDown()
        pool.shutdownNow()
    }

    @Test
    fun `should receive exact unknown length boundary`() {
        var content = ByteArray(0)
        val engine = BoundedUploads(limits, { file -> content = file.openStream().readAllBytes() }, { _, _ -> })

        engine.receiveBytes(name = "file.txt", bytes = byteArrayOf(1, 2, 3, 4), declared = -1)

        assertArrayEquals(byteArrayOf(1, 2, 3, 4), content)
        assertEquals(0L, engine.reservedBytes())
    }

    @Test
    fun `should expire temporary content after the handler returns`() {
        lateinit var received: UploadedFile
        val engine = BoundedUploads(limits, { file -> received = file }, { _, _ -> })

        engine.receiveBytes(name = "file.txt", bytes = byteArrayOf(1, 2, 3, 4), declared = -1)

        assertThrows(IOException::class.java) { received.openStream() }
    }

    @Test
    fun `should reject actual bytes above unknown length limit`() {
        val engine = BoundedUploads(limits, {}, { _, _ -> })

        assertThrows(IOException::class.java) { engine.receiveBytes(name = "large", bytes = ByteArray(5), declared = -1) }

        assertEquals(0L, engine.reservedBytes())
    }

    @Test
    fun `should permit a retry after a file above the limit`() {
        var accepted = 0
        val engine = BoundedUploads(limits, { accepted++ }, { _, _ -> })
        runCatching { engine.receiveBytes(name = "large", bytes = ByteArray(5), declared = -1) }

        engine.receiveBytes(name = "retry", bytes = ByteArray(4), declared = -1)

        assertEquals(1, accepted)
    }

    @ParameterizedTest
    @CsvSource("f.txt,image/png", "f.png,text/plain")
    fun `should reject a file that does not match both MIME and extension`(filename: String, mime: String) {
        val engine = BoundedUploads(textOnly, {}, { _, _ -> })

        assertThrows(IOException::class.java) { engine.receiveBytes(name = filename, mime = mime) }
    }

    @Test
    fun `should accept a file that matches both MIME family and extension in any case`() {
        val events = mutableListOf<FileUploadState>()
        val engine = BoundedUploads(textOnly, {}) { event, _ ->
            events += event
        }

        engine.receiveBytes(name = "f.TXT", mime = "text/plain")

        assertEquals(FileUploadState.Done("f.TXT", 1), events.last())
        assertEquals(1, engine.fileCount())
    }

    @Test
    fun `should hold memory while the handler processes the file`() {
        val engine = BoundedUploads(limits, blockingHandler(), { _, _ -> })
        startInBackground(engine, name = "first")

        awaitHandler()

        assertEquals(4L, engine.reservedBytes())
    }

    @Test
    fun `should permit a retry after cancelling a processing handler`() {
        val engine = BoundedUploads(limits, blockingHandler(), { _, _ -> })
        val task = startInBackground(engine, name = "first")
        awaitHandler()
        engine.cancel()
        finish.countDown()
        runCatching { task.get(5, TimeUnit.SECONDS) }

        engine.receiveBytes(name = "retry")

        assertEquals(0L, engine.reservedBytes())
    }

    @Test
    fun `should keep the new cleared quota while a previous cancelled handler finishes`() {
        val engine = BoundedUploads(limits.copy(maxMemoryBytes = 8), blockingHandler(blocked = "old"), { _, _ -> })
        val old = startInBackground(engine, name = "old")
        awaitHandler()
        engine.clear()
        engine.receiveBytes(name = "new")
        finish.countDown()
        assertThrows(Exception::class.java) { old.get(5, TimeUnit.SECONDS) }

        assertThrows(IOException::class.java) { engine.receiveBytes(name = "extra") }
    }

    @Test
    fun `should release failed application processing before retry`() {
        val engine = BoundedUploads(limits, failingOnce(), { _, _ -> })
        runCatching { engine.receiveBytes(name = "first") }

        engine.receiveBytes(name = "retry")

        assertEquals(0L, engine.reservedBytes())
    }

    @Test
    fun `should reject reading all bytes of a cached temporary stream after expiry`() {
        val cached = expiredStream()

        assertThrows(IOException::class.java) { cached.readAllBytes() }
    }

    @Test
    fun `should reject reading some bytes of a cached temporary stream after expiry`() {
        val cached = expiredStream()

        assertThrows(IOException::class.java) { cached.readNBytes(1) }
    }

    @Test
    fun `should reject transferring a cached temporary stream after expiry`() {
        val cached = expiredStream()

        assertThrows(IOException::class.java) { cached.transferTo(ByteArrayOutputStream()) }
    }

    @Test
    fun `should ignore a removed old handler failure after a newer success`() {
        val events = CopyOnWriteArrayList<FileUploadState>()
        val queued = CopyOnWriteArrayList<() -> Unit>()
        val engine = BoundedUploads(twoFiles, blockingFailure(blocked = "old")) { event, isCurrent ->
            queued.add { if (isCurrent()) events.add(event) }
        }
        val old = startInBackground(engine, name = "old")
        awaitHandler()
        engine.remove(identity(engine, "old"))
        engine.receiveBytes(name = "new")
        finish.countDown()
        runCatching { old.get(5, TimeUnit.SECONDS) }

        deliverQueued(queued)

        assertEquals(FileUploadState.Done("new", 1), events.last())
        assertFalse(events.any { event -> event is FileUploadState.Error })
    }

    @Test
    fun `should reject an old request cancelled before its reservation`() {
        var processed = 0
        val engine = BoundedUploads(limits, { processed++ }, { _, _ -> })
        engine.reservationGateOverride = { blockOnce(null) }
        val old = startInBackground(engine, name = "old")
        awaitHandler()

        engine.cancel()
        finish.countDown()

        assertThrows(Exception::class.java) { old.get(5, TimeUnit.SECONDS) }
        assertEquals(0, processed)
        assertEquals(0L, engine.reservedBytes())
    }

    @Test
    fun `should permit a fresh request after one cancelled before its reservation`() {
        var processed = 0
        val engine = BoundedUploads(limits, { processed++ }, { _, _ -> })
        engine.reservationGateOverride = { blockOnce(null) }
        val old = startInBackground(engine, name = "old")
        awaitHandler()
        engine.cancel()
        finish.countDown()
        assertThrows(Exception::class.java) { old.get(5, TimeUnit.SECONDS) }
        engine.reservationGateOverride = null

        engine.receiveBytes(name = "fresh")

        assertEquals(1, processed)
    }

    @Test
    fun `should remove exactly one completed same named transfer`() {
        val engine = BoundedUploads(twoFiles, {}, { _, _ -> })
        engine.receiveBytes(name = "first", filename = "same.txt")
        engine.receiveBytes(name = "second", filename = "same.txt")

        engine.remove(identity(engine, "first"))

        assertEquals(1, engine.fileCount())
    }

    @Test
    fun `should ignore a stale removal from a previous generation`() {
        val engine = BoundedUploads(twoFiles, {}, { _, _ -> })
        val stale = identity(engine, "second")
        engine.receiveBytes(name = "second", filename = "same.txt")
        engine.clear()
        engine.receiveBytes(name = "second", filename = "same.txt")

        engine.remove(stale)

        assertEquals(1, engine.fileCount())
        assertEquals(0L, engine.reservedBytes())
    }

    @Test
    fun `should report the removal of a running transfer`() {
        val engine = BoundedUploads(twoFiles, blockingHandler(), { _, _ -> })
        startInBackground(engine, name = "running")
        awaitHandler()

        val wasActive = engine.remove(identity(engine, "running"))

        assertTrue(wasActive)
    }

    @Test
    fun `should report the removal of a completed transfer as not running`() {
        val engine = BoundedUploads(twoFiles, {}, { _, _ -> })
        engine.receiveBytes(name = "done")

        val wasActive = engine.remove(identity(engine, "done"))

        assertFalse(wasActive)
    }

    @Test
    fun `should reject a duplicate active identity without losing the first slot`() {
        val engine = BoundedUploads(twoFiles, blockingHandler(), { _, _ -> })
        startInBackground(engine, name = "first", filename = "same.txt")
        awaitHandler()

        assertThrows(IOException::class.java) { engine.receiveBytes(name = "first", filename = "same.txt") }

        assertEquals(1, engine.fileCount())
        assertEquals(4L, engine.reservedBytes())
    }

    @Test
    fun `should reject an invalid identity without losing the active slot`() {
        val engine = BoundedUploads(twoFiles, blockingHandler(), { _, _ -> })
        startInBackground(engine, name = "first", filename = "same.txt")
        awaitHandler()

        assertThrows(IOException::class.java) {
            engine.receive(transferId = "invalid", filename = "same.txt", mime = "text/plain", declared = 1, stream = stream(1))
        }

        assertEquals(1, engine.fileCount())
    }

    @Test
    fun `should reuse an identity after its active transfer is removed and finishes`() {
        val engine = BoundedUploads(twoFiles, blockingHandler(), { _, _ -> })
        val first = startInBackground(engine, name = "first", filename = "same.txt")
        awaitHandler()
        engine.remove(identity(engine, "first"))
        finish.countDown()
        runCatching { first.get(5, TimeUnit.SECONDS) }

        engine.receiveBytes(name = "first", filename = "same.txt")

        assertEquals(1, engine.fileCount())
        assertEquals(0L, engine.reservedBytes())
    }

    @Test
    fun `should cancel only one active same named transfer and preserve the other quota`() {
        val bothEntered = CountDownLatch(2)
        val engine = BoundedUploads(twoFiles, { file ->
            bothEntered.countDown()
            assertTrue(finish.await(5, TimeUnit.SECONDS))
            file.ensureActive()
        }, { _, _ -> })
        startInBackground(engine, name = "one", filename = "same.txt")
        startInBackground(engine, name = "two", filename = "same.txt")
        assertTrue(bothEntered.await(5, TimeUnit.SECONDS))

        engine.remove(identity(engine, "one"))

        assertEquals(1, engine.fileCount())
        assertEquals(8L, engine.reservedBytes())
    }

    /** A handler that signals [entered] and waits for [finish] for the file named [blocked], or for every file. */
    private fun blockingHandler(blocked: String? = null): (UploadedFile) -> Unit = { file ->
        if (blocked == null || file.filename == blocked) blockOnce(file)
    }

    /** A handler that waits for [finish] for the file named [blocked] and then fails as application code. */
    private fun blockingFailure(blocked: String): (UploadedFile) -> Unit = { file ->
        if (file.filename == blocked) {
            blockOnce(null)
            throw IllegalStateException("Delayed $blocked failure")
        }
    }

    /** A handler that fails as application code on the first file only. */
    private fun failingOnce(): (UploadedFile) -> Unit {
        var hasFailed = false
        return {
            if (!hasFailed) {
                hasFailed = true
                throw IllegalStateException("application failure")
            }
        }
    }

    private fun blockOnce(file: UploadedFile?) {
        entered.countDown()
        assertTrue(finish.await(5, TimeUnit.SECONDS))
        file?.ensureActive()
    }

    private fun expiredStream(): InputStream {
        lateinit var cached: InputStream
        BoundedUploads(limits, { file -> cached = file.openStream() }, { _, _ -> }).receiveBytes(name = "file", bytes = byteArrayOf(42))
        return cached
    }

    /** Runs the state deliveries in the order the engine published them, as the UI access queue does. */
    private fun deliverQueued(queued: List<() -> Unit>) = queued.forEach { deliver -> deliver() }

    private fun awaitHandler() {
        assertTrue(entered.await(5, TimeUnit.SECONDS))
    }

    private fun startInBackground(engine: BoundedUploads, name: String, filename: String = name): Future<*> =
        pool.submit { engine.receiveBytes(name = name, filename = filename) }

    private fun BoundedUploads.receiveBytes(
        name: String,
        filename: String = name,
        mime: String = "text/plain",
        bytes: ByteArray = ByteArray(1),
        declared: Long = bytes.size.toLong(),
    ) = receive(
        transferId = identity(this, name),
        filename = filename,
        mime = mime,
        declared = declared,
        stream = ByteArrayInputStream(bytes),
    )

    private fun stream(size: Int) = ByteArrayInputStream(ByteArray(size))

    private fun identity(engine: BoundedUploads, name: String): String =
        "${engine.generation()}:${UUID.nameUUIDFromBytes(name.toByteArray())}"
}
