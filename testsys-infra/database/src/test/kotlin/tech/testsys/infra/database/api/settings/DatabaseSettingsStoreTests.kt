package tech.testsys.infra.database.api.settings

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.env.MapPropertySource
import org.springframework.core.env.StandardEnvironment
import org.springframework.jdbc.core.JdbcTemplate
import tech.testsys.infra.database.DatabaseIntegrationTests
import tech.testsys.infra.settings.api.SettingsConfiguration
import tech.testsys.infra.settings.api.catalog.SettingKey
import tech.testsys.infra.settings.api.store.StoredSettings
import java.util.UUID
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class DatabaseSettingsStoreTests : DatabaseIntegrationTests() {
    @Autowired
    private lateinit var store: DatabaseSettingsStore

    @Autowired
    private lateinit var jdbc: JdbcTemplate

    @Test
    fun `should abort initialization on a durable null despite a valid environment fallback`() {
        insertPayload("""{"testsys.operation.competition.max-participants":null}""")
        val environment = StandardEnvironment().apply {
            propertySources.addFirst(
                MapPropertySource(
                    "settings-test",
                    mapOf(
                        SettingKey.MaxParticipants.property to "100",
                        SettingKey.ConfirmationCodeLifetime.property to "PT5M",
                        SettingKey.MaxConfirmationAttempts.property to "5",
                        SettingKey.CommunityInviteTtl.property to "PT24H",
                        SettingKey.ClassInviteTtl.property to "PT24H",
                        "testsys.operation.community.public-community-id" to "42",
                    ),
                ),
            )
        }

        assertThrows(IllegalStateException::class.java) { SettingsConfiguration().runtimeSettings(store, environment) }
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "null",
            "[]",
            "\"text\"",
            "1",
            "true",
            "",
            "{\"key\":null}",
            "{\"key\":1}",
            "{\"key\":true}",
            "{\"key\":[]}",
            "{\"key\":{}}",
            "{",
        ],
    )
    fun `should reject malformed payload structures and non string values`(payload: String) {
        insertPayload(payload)

        assertThrows(IllegalStateException::class.java) { store.read() }
    }

    // Review of runtime settings storage: the decoder accepted content after the root object (sysdev-cabinet branch).
    @ParameterizedTest
    @Tag("regression")
    @ValueSource(
        strings = [
            "{\"testsys.operation.competition.max-participants\":\"7\"} garbage",
            "{\"testsys.operation.competition.max-participants\":\"7\"} {\"testsys.operation.competition.max-participants\":\"8\"}",
            "{\"testsys.operation.competition.max-participants\":\"7\"} {",
        ],
    )
    fun `should reject payload with content after the root object`(payload: String) {
        insertPayload(payload)

        assertThrows(IllegalStateException::class.java) { store.read() }
    }

    @Test
    fun `should allow only one concurrent first insertion`() {
        val barrier = CyclicBarrier(2)
        val workers = Executors.newFixedThreadPool(2)
        try {
            val first = workers.submit<StoredSettings?> {
                barrier.await(5, TimeUnit.SECONDS)
                store.compareAndSet(0, mapOf(SettingKey.MaxParticipants.property to "3"), UUID(0, 1))
            }
            val second = workers.submit<StoredSettings?> {
                barrier.await(5, TimeUnit.SECONDS)
                store.compareAndSet(0, mapOf(SettingKey.MaxParticipants.property to "4"), UUID(0, 2))
            }

            val results = listOf(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS))

            assertEquals(1, results.count { result -> result != null })
            assertEquals(1, store.read().revision)
        } finally {
            workers.shutdownNow()
        }
    }

    @Test
    fun `should round trip all overrides in one durable row`() {
        val overrides = SettingKey.entries.associate { key -> key.property to (key.defaultValue ?: "1") }
        val id = UUID(0, 1)

        val saved = store.compareAndSet(0, overrides, id)

        assertNotNull(saved)
        assertEquals(1, store.read().revision)
        assertEquals(overrides, store.read().overrides)
        assertEquals(id, store.read().changeId)
    }

    @Test
    fun `should refuse stale update without partial changes`() {
        store.compareAndSet(0, mapOf(SettingKey.MaxParticipants.property to "3"), UUID(0, 1))

        assertNull(store.compareAndSet(0, mapOf(SettingKey.MaxParticipants.property to "4"), UUID(0, 2)))
        assertEquals("3", store.read().overrides[SettingKey.MaxParticipants.property])
    }

    @Test
    fun `should identify repeated write and compensate only the matching change`() {
        val id = UUID(0, 1)
        val saved = store.compareAndSet(0, mapOf(SettingKey.MaxParticipants.property to "3"), id)
        assertEquals(saved, store.compareAndSet(0, emptyMap(), id))
        assertNull(store.compareAndSet(1, emptyMap(), UUID(0, 2), UUID(0, 9)))

        val restored = store.compareAndSet(1, emptyMap(), UUID(0, 2), id)

        assertEquals(2, restored?.revision)
        assertEquals(emptyMap<String, String>(), store.read().overrides)
    }

    private fun insertPayload(payload: String) {
        jdbc.update(
            "insert into ts_runtime_settings (id, set_key, revision, change_id, payload) values (?, ?, ?, ?, ?)",
            1L,
            "runtime",
            1L,
            UUID(0, 1),
            payload,
        )
    }
}
