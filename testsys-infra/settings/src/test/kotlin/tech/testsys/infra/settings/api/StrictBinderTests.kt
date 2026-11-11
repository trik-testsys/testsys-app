@file:OptIn(InternalSettingsApi::class)

package tech.testsys.infra.settings.api

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.boot.context.properties.bind.BindException
import org.springframework.core.env.StandardEnvironment
import tech.testsys.infra.settings.internal.InternalSettingsApi
import tech.testsys.infra.settings.internal.withProperty

class StrictBinderTests {
    @Test
    fun `should reject an unresolvable placeholder`() {
        val environment = StandardEnvironment().withProperty("testsys.probe.secret", "\${TESTSYS_MISSING_PROBE_SECRET}")

        assertThrows(BindException::class.java) { strictBinder(environment).bind("testsys.probe.secret", String::class.java) }
    }

    @Test
    fun `should bind a resolved placeholder`() {
        val environment = StandardEnvironment()
            .withProperty("testsys.probe.secret", "\${testsys.probe.source}")
            .withProperty("testsys.probe.source", "value")

        val actual = strictBinder(environment).bind("testsys.probe.secret", String::class.java).get()

        assertEquals("value", actual)
    }
}
