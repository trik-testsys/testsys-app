@file:OptIn(InternalSettingsApi::class)

package tech.testsys.infra.settings.api

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.boot.context.properties.bind.BindException
import org.springframework.core.env.StandardEnvironment
import tech.testsys.domain.model.group.CommunityId
import tech.testsys.infra.settings.api.catalog.SettingKey
import tech.testsys.infra.settings.api.catalog.SettingsValues
import tech.testsys.infra.settings.internal.InternalSettingsApi
import tech.testsys.infra.settings.internal.withProperty

class SettingsTests {
    @Nested
    inner class WithoutHolderTests {
        @Test
        fun `should bind from Environment resolving placeholders`() {
            val environment = StandardEnvironment().withProperty("testsys.probe.count", "\${missing.limit:7}")

            val actual = settingsOf(CountProperties::class.java, environment, null).current()

            assertEquals(CountProperties(7), actual)
        }

        @Test
        fun `should reject an invalid Environment value when created`() {
            val environment = StandardEnvironment().withProperty("testsys.probe.count", "x")

            assertThrows(BindException::class.java) { settingsOf(CountProperties::class.java, environment, null) }
        }
    }

    @Nested
    inner class WithHolderTests {
        private val runtime = RuntimeSettings(snapshot(revision = 0, maxTimeLimitMillis = "10"), CommunityId(42))

        @Test
        fun `should read catalog values from the snapshot and other values from the given Environment`() {
            val actual = settingsOf(ProbeProperties::class.java, probeEnvironment(), runtime).current()

            assertEquals(ProbeProperties(10, "snapshot:", "environment-only"), actual)
        }

        @Test
        fun `should return values of a newly published snapshot`() {
            val settings = settingsOf(ProbeProperties::class.java, probeEnvironment(), runtime)
            runtime.publish(snapshot(revision = 1, maxTimeLimitMillis = "20"), emptyList())

            val actual = settings.current()

            assertEquals(ProbeProperties(20, "snapshot:", "environment-only"), actual)
        }

        @Test
        fun `should reject an invalid value outside the catalog when created`() {
            val environment = StandardEnvironment().withProperty("testsys.probe.count", "x")

            assertThrows(BindException::class.java) { settingsOf(CountProperties::class.java, environment, runtime) }
        }

        @Test
        fun `should reject a candidate that the properties class rejects when prepared`() {
            settingsOf(LimitedProperties::class.java, StandardEnvironment(), runtime)

            assertThrows(BindException::class.java) { runtime.prepare(snapshot(revision = 1, maxTimeLimitMillis = "20")) }
        }

        private fun snapshot(revision: Long, maxTimeLimitMillis: String) = SettingsSnapshot(
            revision = revision,
            values = SettingsValues(mapOf(SettingKey.MaxTimeLimitMillis to maxTimeLimitMillis, SettingKey.ScorePrefix to "snapshot:")),
            activeMaxMessageBytes = 100,
        )
    }
}

@SettingsProperties("testsys.diagnostics")
data class LimitedProperties(val maxTimeLimitMillis: Long) {
    init {
        require(maxTimeLimitMillis <= 15)
    }
}
