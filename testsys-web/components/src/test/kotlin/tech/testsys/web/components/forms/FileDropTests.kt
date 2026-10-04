package tech.testsys.web.components.forms

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

        engine.receive(identity(engine, "file.txt"), "file.txt", "text/plain", -1, ByteArrayInputStream(byteArrayOf(1, 2, 3, 4)))

        assertEquals(0L, engine.reservedBytes())
        assertThrows(IOException::class.java) { received.openStream() }
    }

    @Test
    fun `should reject actual bytes above unknown length limit and release quota for retry`() {
        var accepted = 0
        val engine = BoundedUploads(limits, { accepted++ }, { _, _ -> })
        assertThrows(IOException::class.java) { engine.receive(
                identity(engine, "large"), "large",
                "text/plain",
                -1,
                ByteArrayInputStream(
                    ByteArray(
                        5,
                    ),
                ),
            ) }

        engine.receive(identity(engine, "retry"), "retry", "text/plain", -1, ByteArrayInputStream(ByteArray(4)))

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
                identity(engine, "f.txt"), "f.txt",
                "image/png",
                1,
                ByteArrayInputStream(
                    ByteArray(
                        1,
                    ),
                ),
            ) }
        assertThrows(IOException::class.java) { engine.receive(
                identity(engine, "f.png"), "f.png",
                "text/plain",
                1,
                ByteArrayInputStream(
                    ByteArray(
                        1,
                    ),
                ),
            ) }
        engine.receive(identity(engine, "f.TXT"), "f.TXT", "text/plain", 1, ByteArrayInputStream(ByteArray(1)))
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
                        identity(engine, "first"), "first",
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
            engine.receive(identity(engine, "retry"), "retry", "text/plain", 1, ByteArrayInputStream(ByteArray(1)))

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
                        identity(engine, "old"), "old",
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
            engine.receive(identity(engine, "new"), "new", "text/plain", 1, ByteArrayInputStream(ByteArray(1)))
            finish.countDown()
            task.get(5, TimeUnit.SECONDS)

            assertThrows(IOException::class.java) { engine.receive(
                    identity(engine, "extra"), "extra",
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
                identity(engine, "first"), "first",
                "text/plain",
                1,
                ByteArrayInputStream(
                    ByteArray(
                        1,
                    ),
                ),
            ) }

        engine.receive(identity(engine, "retry"), "retry", "text/plain", 1, ByteArrayInputStream(ByteArray(1)))

        assertEquals(0L, engine.reservedBytes())
    }
    @Test
    fun `should reject bulk reads of a cached temporary stream after expiry`() {
        lateinit var cached: java.io.InputStream
        val engine = BoundedUploads(limits, { file -> cached = file.openStream() }, { _, _ -> })
        engine.receive(identity(engine, "file"), "file", "text/plain", 1, ByteArrayInputStream(byteArrayOf(42)))

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
                    engine.receive(identity(engine, "old"), "old", "text/plain", 1, ByteArrayInputStream(ByteArray(1)))
                }
            }
            assertTrue(entered.await(5, TimeUnit.SECONDS))
            engine.remove(identity(engine, "old"))
            engine.receive(identity(engine, "new"), "new", "text/plain", 1, ByteArrayInputStream(ByteArray(1)))
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
                    engine.receive(identity(engine, "old"), "old", "text/plain", 1, ByteArrayInputStream(ByteArray(1)))
                }
            }
            assertTrue(entered.await(5, TimeUnit.SECONDS))

            engine.cancel()
            resume.countDown()
            old.get(5, TimeUnit.SECONDS)

            assertEquals(0, processed)
            assertEquals(0L, engine.reservedBytes())
            engine.reservationGateOverride = null
            engine.receive(identity(engine, "fresh"), "fresh", "text/plain", 1, ByteArrayInputStream(ByteArray(1)))
            assertEquals(1, processed)
        } finally {
            resume.countDown()
            pool.shutdownNow()
        }
    }

    // Review report, Issue 5: preserve the selected transfer or disabled field contract.
    @Test
    @org.junit.jupiter.api.Tag("regression")
    fun `should remove exactly one completed same named transfer and ignore stale removal`() {
        val engine = BoundedUploads(limits.copy(maxFiles = 2, maxMemoryBytes = 8), {}, { _, _ -> })
        val first = identity(engine, "first")
        val second = identity(engine, "second")
        engine.receive(first, "same.txt", "text/plain", 1, ByteArrayInputStream(ByteArray(1)))
        engine.receive(second, "same.txt", "text/plain", 1, ByteArrayInputStream(ByteArray(1)))
        assertEquals(2, engine.fileCount())
        engine.remove(first)
        assertEquals(1, engine.fileCount())
        engine.clear()
        val fresh = identity(engine, "second")
        engine.receive(fresh, "same.txt", "text/plain", 1, ByteArrayInputStream(ByteArray(1)))
        engine.remove(second)
        assertEquals(1, engine.fileCount())
        assertEquals(0L, engine.reservedBytes())
    }

    // Review report, Issue 5: preserve the selected transfer or disabled field contract.
    @Test
    @org.junit.jupiter.api.Tag("regression")
    fun `should reject invalid and duplicate active identities without losing the first slot`() {
        val entered = CountDownLatch(1)
        val finish = CountDownLatch(1)
        val pool = Executors.newSingleThreadExecutor()
        val engine = BoundedUploads(limits.copy(maxFiles = 2, maxMemoryBytes = 8), { file ->
            entered.countDown()
            assertTrue(finish.await(5, TimeUnit.SECONDS))
            file.ensureActive()
        }, { _, _ -> })
        val first = identity(engine, "first")
        try {
            val task = pool.submit { assertThrows(IOException::class.java) {
                engine.receive(first, "same.txt", "text/plain", 1, ByteArrayInputStream(ByteArray(1)))
            } }
            assertTrue(entered.await(5, TimeUnit.SECONDS))
            assertThrows(IOException::class.java) {
                engine.receive(first, "same.txt", "text/plain", 1, ByteArrayInputStream(ByteArray(1)))
            }
            assertThrows(IOException::class.java) {
                engine.receive("invalid", "same.txt", "text/plain", 1, ByteArrayInputStream(ByteArray(1)))
            }
            assertEquals(1, engine.fileCount())
            assertEquals(4L, engine.reservedBytes())
            engine.remove(first)
            assertEquals(0, engine.fileCount())
            finish.countDown()
            task.get(5, TimeUnit.SECONDS)
            assertEquals(0L, engine.reservedBytes())
            engine.receive(first, "same.txt", "text/plain", 1, ByteArrayInputStream(ByteArray(1)))
            assertEquals(1, engine.fileCount())
        } finally {
            finish.countDown()
            pool.shutdownNow()
        }
    }

    // Review report, Issue 5: preserve the selected transfer or disabled field contract.
    @Test
    @org.junit.jupiter.api.Tag("regression")
    fun `should cancel only one active same named transfer and preserve the other quota`() {
        val entered = CountDownLatch(2)
        val finish = CountDownLatch(1)
        val pool = Executors.newFixedThreadPool(2)
        val engine = BoundedUploads(limits.copy(maxFiles = 2, maxMemoryBytes = 8), { file ->
            entered.countDown()
            assertTrue(finish.await(5, TimeUnit.SECONDS))
            file.ensureActive()
        }, { _, _ -> })
        val first = identity(engine, "one")
        val second = identity(engine, "two")
        try {
            val cancelled = pool.submit { assertThrows(IOException::class.java) {
                engine.receive(first, "same.txt", "text/plain", 1, ByteArrayInputStream(ByteArray(1)))
            } }
            val retained = pool.submit { engine.receive(second, "same.txt", "text/plain", 1, ByteArrayInputStream(ByteArray(1))) }
            assertTrue(entered.await(5, TimeUnit.SECONDS))
            engine.remove(first)
            assertEquals(1, engine.fileCount())
            assertEquals(8L, engine.reservedBytes())
            finish.countDown()
            cancelled.get(5, TimeUnit.SECONDS)
            retained.get(5, TimeUnit.SECONDS)
            assertEquals(1, engine.fileCount())
            assertEquals(0L, engine.reservedBytes())
        } finally {
            finish.countDown()
            pool.shutdownNow()
        }
    }

    private fun identity(engine: BoundedUploads, name: String): String =
        "${engine.generation()}:${java.util.UUID.nameUUIDFromBytes(name.toByteArray())}"

}
