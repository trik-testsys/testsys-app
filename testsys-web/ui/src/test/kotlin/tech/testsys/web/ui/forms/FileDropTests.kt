package tech.testsys.web.ui.forms

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.io.IOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class FileDropTests {
    private val limits = UploadLimits(maxFiles = 1, maxFileBytes = 4, maxMemoryBytes = 4)

    @Test
    fun `should receive exact unknown length boundary and expire temporary content`() {
        lateinit var received: UploadedFile
        val engine = BoundedUploads(
            limits,
            { file ->
                received = file
                assertArrayEquals(byteArrayOf(1, 2, 3, 4), file.openStream().readAllBytes())
            },
            { _, _ -> },
        )

        engine.receive("file.txt", "text/plain", -1, ByteArrayInputStream(byteArrayOf(1, 2, 3, 4)))

        assertEquals(0L, engine.reservedBytes())
        assertThrows(IOException::class.java) { received.openStream() }
    }

    @Test
    fun `should reject actual bytes above unknown length limit and release quota for retry`() {
        var accepted = 0
        val engine = BoundedUploads(limits, { accepted++ }, { _, _ -> })
        assertThrows(IOException::class.java) { engine.receive(
                "large",
                "text/plain",
                -1,
                ByteArrayInputStream(
                    ByteArray(
                        5,
                    ),
                ),
            ) }

        engine.receive("retry", "text/plain", -1, ByteArrayInputStream(ByteArray(4)))

        assertEquals(1, accepted)
        assertEquals(0L, engine.reservedBytes())
    }

    @Test
    fun `should require both matching MIME and extension`() {
        val engine = BoundedUploads(
            limits.copy(mimeTypes = setOf("text/*"), extensions = setOf(".txt")),
            {},
            { _, _ -> },
        )

        assertThrows(IOException::class.java) { engine.receive(
                "f.txt",
                "image/png",
                1,
                ByteArrayInputStream(
                    ByteArray(
                        1,
                    ),
                ),
            ) }
        assertThrows(IOException::class.java) { engine.receive(
                "f.png",
                "text/plain",
                1,
                ByteArrayInputStream(
                    ByteArray(
                        1,
                    ),
                ),
            ) }
        engine.receive("f.TXT", "text/plain", 1, ByteArrayInputStream(ByteArray(1)))
    }

    @Test
    fun `should hold memory through processing and permit retry after cancellation`() {
        val entered = CountDownLatch(1)
        val finish = CountDownLatch(1)
        val pool = Executors.newSingleThreadExecutor()
        var first = true
        val engine = BoundedUploads(
            limits,
            { file ->

                if (first) {
                    first = false
                    entered.countDown()
                    assertTrue(finish.await(5, TimeUnit.SECONDS))
                    file.ensureActive()
                }
            },
            { _, _ -> },
        )
        try {
            val task = pool.submit { assertThrows(IOException::class.java) { engine.receive(
                        "first",
                        "text/plain",
                        1,
                        ByteArrayInputStream(
                            ByteArray(
                                1,
                            ),
                        ),
                    ) } }
            assertTrue(entered.await(5, TimeUnit.SECONDS))
            assertEquals(4L, engine.reservedBytes())

            engine.cancel()
            finish.countDown()
            task.get(5, TimeUnit.SECONDS)
            engine.receive("retry", "text/plain", 1, ByteArrayInputStream(ByteArray(1)))

            assertEquals(0L, engine.reservedBytes())
        } finally {
            finish.countDown()
            pool.shutdownNow()
        }
    }

    @Test
    fun `should keep new cleared quota while previous cancelled handler finishes`() {
        val entered = CountDownLatch(1)
        val finish = CountDownLatch(1)
        val pool = Executors.newSingleThreadExecutor()
        var first = true
        val engine = BoundedUploads(
            limits.copy(maxMemoryBytes = 8),
            { file ->

                if (first) {
                    first = false
                    entered.countDown()
                    assertTrue(finish.await(5, TimeUnit.SECONDS))
                    file.ensureActive()
                }
            },
            { _, _ -> },
        )
        try {
            val task = pool.submit { assertThrows(IOException::class.java) { engine.receive(
                        "old",
                        "text/plain",
                        1,
                        ByteArrayInputStream(
                            ByteArray(
                                1,
                            ),
                        ),
                    ) } }
            assertTrue(entered.await(5, TimeUnit.SECONDS))

            engine.clear()
            engine.receive("new", "text/plain", 1, ByteArrayInputStream(ByteArray(1)))
            finish.countDown()
            task.get(5, TimeUnit.SECONDS)

            assertThrows(IOException::class.java) { engine.receive(
                    "extra",
                    "text/plain",
                    1,
                    ByteArrayInputStream(
                        ByteArray(
                            1,
                        ),
                    ),
                ) }
        } finally {
            finish.countDown()
            pool.shutdownNow()
        }
    }

    @Test
    fun `should release failed application processing before retry`() {
        var failed = false
        val engine = BoundedUploads(
            limits,
            {
                if (!failed) {
                    failed = true
                    throw IllegalStateException("application failure")
                }
            },
            { _, _ -> },
        )
        assertThrows(IllegalStateException::class.java) { engine.receive(
                "first",
                "text/plain",
                1,
                ByteArrayInputStream(
                    ByteArray(
                        1,
                    ),
                ),
            ) }

        engine.receive("retry", "text/plain", 1, ByteArrayInputStream(ByteArray(1)))

        assertEquals(0L, engine.reservedBytes())
    }
    @Test
    fun `should reject bulk reads of a cached temporary stream after expiry`() {
        lateinit var cached: java.io.InputStream
        val engine = BoundedUploads(limits, { file -> cached = file.openStream() }, { _, _ -> })
        engine.receive("file", "text/plain", 1, ByteArrayInputStream(byteArrayOf(42)))

        assertThrows(IOException::class.java) { cached.readAllBytes() }
        assertThrows(IOException::class.java) { cached.readNBytes(1) }
        assertThrows(IOException::class.java) { cached.transferTo(java.io.ByteArrayOutputStream()) }
    }

    @Test
    fun `should ignore a removed old handler failure after a newer success`() {
        val entered = CountDownLatch(1)
        val finish = CountDownLatch(1)
        val pool = Executors.newSingleThreadExecutor()
        val events = java.util.concurrent.CopyOnWriteArrayList<FileUploadState>()
        val queued = java.util.concurrent.CopyOnWriteArrayList<() -> Unit>()
        val engine = BoundedUploads(limits.copy(maxMemoryBytes = 8), { file ->
            if (file.filename == "old") {
                entered.countDown()
                assertTrue(finish.await(5, TimeUnit.SECONDS))
                throw IllegalStateException("Delayed old failure")
            }
        }, { event, isCurrent -> queued.add { if (isCurrent()) events.add(event) } })
        try {
            val task = pool.submit {
                assertThrows(IllegalStateException::class.java) {
                    engine.receive("old", "text/plain", 1, ByteArrayInputStream(ByteArray(1)))
                }
            }
            assertTrue(entered.await(5, TimeUnit.SECONDS))
            engine.remove("old")
            engine.receive("new", "text/plain", 1, ByteArrayInputStream(ByteArray(1)))
            finish.countDown()
            task.get(5, TimeUnit.SECONDS)

            queued.forEach { deliver -> deliver() }

            assertEquals(FileUploadState.Done("new", 1), events.last())
            assertFalse(events.any { event -> event is FileUploadState.Error })
        } finally {
            finish.countDown()
            pool.shutdownNow()
        }
    }
    @Test
    fun `should reject an old request cancelled before its reservation and permit a fresh retry`() {
        val entered = CountDownLatch(1)
        val resume = CountDownLatch(1)
        val pool = Executors.newSingleThreadExecutor()
        var processed = 0
        val engine = BoundedUploads(limits, { processed++ }, { _, _ -> })
        engine.reservationGateOverride = {
            entered.countDown()
            assertTrue(resume.await(5, TimeUnit.SECONDS))
        }
        try {
            val old = pool.submit {
                assertThrows(IOException::class.java) {
                    engine.receive("old", "text/plain", 1, ByteArrayInputStream(ByteArray(1)))
                }
            }
            assertTrue(entered.await(5, TimeUnit.SECONDS))

            engine.cancel()
            resume.countDown()
            old.get(5, TimeUnit.SECONDS)

            assertEquals(0, processed)
            assertEquals(0L, engine.reservedBytes())
            engine.reservationGateOverride = null
            engine.receive("fresh", "text/plain", 1, ByteArrayInputStream(ByteArray(1)))
            assertEquals(1, processed)
        } finally {
            resume.countDown()
            pool.shutdownNow()
        }
    }

}
