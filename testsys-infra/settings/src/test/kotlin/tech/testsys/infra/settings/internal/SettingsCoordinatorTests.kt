@file:OptIn(InternalSettingsApi::class)

package tech.testsys.infra.settings.internal

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.EnumSource
import tech.testsys.infra.settings.api.SettingKey
import tech.testsys.infra.settings.api.SettingsChangeResult
import tech.testsys.infra.settings.api.SettingsPersistence
import java.time.Duration
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class SettingsCoordinatorTests {
    private val environment = settingsEnvironment()
    private val store = MemorySettingsStore()
    private val runtime = SettingsInitializer.initialize(store.read(), environment)
    private val service = SettingsCoordinator(store, runtime, SettingsResolver(environment), store.read())

    @ParameterizedTest
    @EnumSource(SettingKey::class, names = ["ConfirmationCodeLifetime", "CommunityInviteTtl", "ClassInviteTtl", "CleanupMinimumAge"])
    fun `should persist publish and restore long durations used with instants`(key: SettingKey) {
        val result = service.change(0, mapOf(key to "P110000D"))

        assertTrue(result is SettingsChangeResult.Applied)
        assertEquals(Duration.ofDays(110_000), runtime.snapshot().values.duration(key))
        assertEquals(mapOf(key.property to "PT2640000H"), store.stored.overrides)
        val restored = SettingsInitializer.initialize(store.read(), environment)
        assertEquals(1, restored.snapshot().revision)
        assertEquals(Duration.ofDays(110_000), restored.snapshot().values.duration(key))
    }

    @ParameterizedTest
    @CsvSource(
        "TotalTimeout,P110000D",
        "RpcTimeout,P110000D",
        "StatusTimeout,P110000D",
        "PollInterval,P110000D",
        "ConfirmationCodeLifetime,P800000000000D",
        "CommunityInviteTtl,P800000000000D",
        "ClassInviteTtl,P800000000000D",
        "CleanupMinimumAge,P800000000000D",
    )
    fun `should reject overflow of the required operation without writing or publishing`(key: SettingKey, value: String) {
        val original = runtime.snapshot()

        val result = service.change(0, mapOf(key to value))

        assertTrue(result is SettingsChangeResult.Invalid)
        assertEquals(0, store.stored.revision)
        assertEquals(emptyMap<String, String>(), store.stored.overrides)
        assertSame(original, runtime.snapshot())
    }

    @Test
    fun `should publish a complete candidate while retaining the startup transport limit`() {
        val result = service.change(0, mapOf(SettingKey.MaxParticipants to "0", SettingKey.MaxMessageBytes to "1024"))

        assertTrue(result is SettingsChangeResult.Applied)
        assertEquals(0, runtime.snapshot().values.int(SettingKey.MaxParticipants))
        assertEquals(1024, runtime.snapshot().values.int(SettingKey.MaxMessageBytes))
        assertEquals(400_000_000, runtime.snapshot().activeMaxMessageBytes)
        assertEquals(2, store.stored.overrides.size)
    }

    @Test
    fun `should refuse stale form without overwriting another change`() {
        service.change(0, mapOf(SettingKey.MaxParticipants to "3"))

        assertEquals(SettingsChangeResult.Stale, service.change(0, mapOf(SettingKey.MaxParticipants to "4")))
        assertEquals("3", store.stored.overrides[SettingKey.MaxParticipants.property])
    }

    @Test
    fun `should reject invalid duration at its field without writing`() {
        val result = service.change(0, mapOf(SettingKey.TotalTimeout to "wrong"))

        assertTrue(result is SettingsChangeResult.Invalid)
        assertEquals(0, store.stored.revision)
    }

    @Test
    fun `should leave storage and active resources untouched on preparation failure`() {
        val original = runtime.snapshot()
        val closed = AtomicInteger()
        runtime.register { AutoCloseable { closed.incrementAndGet() } }
        runtime.register { candidate ->
            check(candidate === original) { "Preparation failed" }
            AutoCloseable {}
        }

        assertEquals(SettingsChangeResult.Failed, service.change(0, mapOf(SettingKey.PollInterval to "PT1S")))
        assertSame(original, runtime.snapshot())
        assertEquals(0, store.stored.revision)
        assertEquals(1, closed.get())
    }

    @Test
    fun `should recognize confirmed rollback and retain previous snapshot`() {
        val original = runtime.snapshot()
        store.beforeWrite = { error("Write rolled back") }

        assertEquals(SettingsChangeResult.Failed, service.change(0, mapOf(SettingKey.MaxParticipants to "3")))
        assertSame(original, runtime.snapshot())
        assertEquals(emptyMap<String, String>(), store.stored.overrides)
    }

    @Test
    fun `should compensate committed write including absent overrides and retain old polling resource`() {
        val original = runtime.snapshot()
        val closes = AtomicInteger()
        runtime.register { AutoCloseable { closes.incrementAndGet() } }
        store.afterWrite = { if (store.stored.revision == 1L) error("Commit response lost") }

        assertEquals(SettingsChangeResult.Failed, service.change(0, mapOf(SettingKey.MaxParticipants to "3")))
        assertEquals(emptyMap<String, String>(), store.stored.overrides)
        assertEquals(2, service.overview().revision)
        assertSame(original, runtime.snapshot())
        assertEquals(1, closes.get())
    }

    @Test
    fun `should read back successful compensation when its commit response is lost`() {
        store.afterWrite = { error("Commit response lost") }

        assertEquals(SettingsChangeResult.Failed, service.change(0, mapOf(SettingKey.MaxParticipants to "3")))
        assertEquals(SettingsPersistence.Known, service.overview().persistence)
        assertEquals(2, store.stored.revision)
    }

    @Test
    fun `should lock edits and keep known active snapshot on unresolved commit`() {
        val original = runtime.snapshot()
        store.afterWrite = { error("Commit response lost") }
        store.beforeRead = { error("Database unavailable") }

        assertEquals(SettingsChangeResult.Uncertain, service.change(0, mapOf(SettingKey.MaxParticipants to "3")))
        assertEquals(SettingsPersistence.Uncertain, service.overview().persistence)
        assertSame(original, runtime.snapshot())
        assertEquals(SettingsChangeResult.Uncertain, service.change(0, mapOf(SettingKey.MaxParticipants to "4")))
    }

    @Test
    fun `should never compensate an unknown concurrent write`() {
        store.afterWrite = {
            store.stored = store.stored.copy(changeId = java.util.UUID(9, 9))
            error("Foreign write")
        }

        assertEquals(SettingsChangeResult.Uncertain, service.change(0, mapOf(SettingKey.MaxParticipants to "3")))
        assertEquals(1, store.stored.revision)
    }

    @Test
    fun `should serialize concurrent forms and publish only after durable commit`() {
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        val original = runtime.snapshot()
        store.beforeWrite = {
            entered.countDown()
            check(release.await(5, TimeUnit.SECONDS))
        }
        val workers = Executors.newFixedThreadPool(2)
        try {
            val first = workers.submit<SettingsChangeResult> { service.change(0, mapOf(SettingKey.MaxParticipants to "3")) }
            assertTrue(entered.await(5, TimeUnit.SECONDS))
            assertSame(original, runtime.snapshot())
            val second = workers.submit<SettingsChangeResult> { service.change(0, mapOf(SettingKey.MaxParticipants to "4")) }
            release.countDown()

            assertTrue(first.get(5, TimeUnit.SECONDS) is SettingsChangeResult.Applied)
            assertEquals(SettingsChangeResult.Stale, second.get(5, TimeUnit.SECONDS))
            assertEquals(3, runtime.snapshot().values.int(SettingKey.MaxParticipants))
        } finally {
            release.countDown()
            workers.shutdownNow()
        }
    }

    @Test
    fun `should finish publication before shutdown requested during a durable write`() {
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        val closeRequested = CountDownLatch(1)
        val closedResources = AtomicInteger()
        runtime.register { AutoCloseable { closedResources.incrementAndGet() } }
        store.beforeWrite = {
            entered.countDown()
            check(release.await(5, TimeUnit.SECONDS))
        }
        val workers = Executors.newFixedThreadPool(2)
        try {
            val changing = workers.submit<SettingsChangeResult> { service.change(0, mapOf(SettingKey.MaxParticipants to "3")) }
            assertTrue(entered.await(5, TimeUnit.SECONDS))
            val closing = workers.submit {
                closeRequested.countDown()
                runtime.close()
            }
            assertTrue(closeRequested.await(5, TimeUnit.SECONDS))
            assertFalse(closing.isDone)
            release.countDown()

            assertTrue(changing.get(5, TimeUnit.SECONDS) is SettingsChangeResult.Applied)
            closing.get(5, TimeUnit.SECONDS)

            assertEquals(3, runtime.snapshot().values.int(SettingKey.MaxParticipants))
            assertEquals(2, closedResources.get())
            assertEquals(SettingsChangeResult.Failed, service.change(1, mapOf(SettingKey.MaxParticipants to "4")))
            assertEquals(1, store.stored.revision)
        } finally {
            release.countDown()
            workers.shutdownNow()
        }
    }

    @Test
    fun `should reject changes after holder shutdown before writing`() {
        runtime.close()

        assertEquals(SettingsChangeResult.Failed, service.change(0, mapOf(SettingKey.MaxParticipants to "3")))
        assertEquals(0, store.stored.revision)
    }
}
