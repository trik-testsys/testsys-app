package tech.testsys.infra.settings.api

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.boot.context.properties.bind.BindException
import org.springframework.core.env.MapPropertySource
import org.springframework.core.env.StandardEnvironment
import tech.testsys.infra.settings.api.catalog.SettingKey
import tech.testsys.infra.settings.api.catalog.SettingsValues
import tech.testsys.infra.settings.internal.withProperty

class SettingsSnapshotTests {
    private val environment = probeEnvironment()

    @Test
    fun `should prefer saved snapshot values over Environment`() {
        val actual = snapshot("10").get<ProbeProperties>(environment)

        assertEquals(ProbeProperties(10, "snapshot:", "environment-only"), actual)
    }

    @Test
    fun `should return the cached instance for the same snapshot and Environment`() {
        val snapshot = snapshot("10")
        val cached = snapshot.get<ProbeProperties>(environment)

        val actual = snapshot.get<ProbeProperties>(environment)

        assertSame(cached, actual)
    }

    @Test
    fun `should bind anew for another Environment of the same snapshot`() {
        val snapshot = snapshot("10")
        val cached = snapshot.get<ProbeProperties>(environment)

        val actual = snapshot.get<ProbeProperties>(probeEnvironment())

        assertNotSame(cached, actual)
    }

    @Test
    fun `should bind anew for another snapshot with the same Environment`() {
        val cached = snapshot("10").get<ProbeProperties>(environment)

        val actual = snapshot("10").get<ProbeProperties>(environment)

        assertNotSame(cached, actual)
    }

    @Test
    fun `should bind again after a failed binding when Environment is fixed`() {
        val properties = mutableMapOf<String, Any>("testsys.probe.count" to "x")
        val environment = StandardEnvironment().apply { propertySources.addFirst(MapPropertySource("test", properties)) }
        val snapshot = snapshot("10")
        assertThrows(BindException::class.java) { snapshot.get<CountProperties>(environment) }
        properties["testsys.probe.count"] = "1"

        val actual = snapshot.get<CountProperties>(environment)

        assertEquals(CountProperties(1), actual)
    }

    @Test
    fun `should resolve placeholders in Environment values but keep saved values literal`() {
        val environment = StandardEnvironment()
            .withProperty("testsys.source", "resolved")
            .withProperty("testsys.diagnostics.extra", "\${testsys.source}")
        val snapshot = SettingsSnapshot(
            revision = 0,
            values = SettingsValues(mapOf(SettingKey.MaxTimeLimitMillis to "10", SettingKey.ScorePrefix to "\${testsys.source}")),
            activeMaxMessageBytes = 100,
        )

        val actual = snapshot.get<ProbeProperties>(environment)

        assertEquals(ProbeProperties(10, "\${testsys.source}", "resolved"), actual)
    }

    @Test
    fun `should bind the active message limit instead of a saved value pending restart`() {
        val snapshot = SettingsSnapshot(
            revision = 0,
            values = SettingsValues(mapOf(SettingKey.MaxMessageBytes to "1024")),
            activeMaxMessageBytes = 100,
        )

        val actual = snapshot.get<GradingProbe>(StandardEnvironment())

        assertEquals(GradingProbe(100), actual)
    }

    @Test
    fun `should reject unconvertible values with BindException`() {
        val snapshot = snapshot("x")

        assertThrows(BindException::class.java) { snapshot.get<ProbeProperties>(environment) }
    }

    @Test
    fun `should reject a class without SettingsProperties`() {
        assertThrows(IllegalArgumentException::class.java) { snapshot("10").get<UnannotatedProperties>(environment) }
    }

    private fun snapshot(maxTimeLimitMillis: String) = SettingsSnapshot(
        revision = 0,
        values = SettingsValues(mapOf(SettingKey.MaxTimeLimitMillis to maxTimeLimitMillis, SettingKey.ScorePrefix to "snapshot:")),
        activeMaxMessageBytes = 100,
    )
}

internal fun probeEnvironment(): StandardEnvironment = StandardEnvironment()
    .withProperty("testsys.diagnostics.max-time-limit-millis", "1")
    .withProperty("testsys.diagnostics.score-prefix", "environment:")
    .withProperty("testsys.diagnostics.extra", "environment-only")

@SettingsProperties("testsys.diagnostics")
data class ProbeProperties(val maxTimeLimitMillis: Long, val scorePrefix: String, val extra: String)

@SettingsProperties("testsys.probe")
data class CountProperties(val count: Int)

@SettingsProperties("testsys.grading")
data class GradingProbe(val maxMessageBytes: Int)

data class UnannotatedProperties(val extra: String)
