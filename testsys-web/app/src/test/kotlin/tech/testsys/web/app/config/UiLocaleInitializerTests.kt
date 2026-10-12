package tech.testsys.web.app.config

import com.vaadin.flow.server.VaadinRequest
import io.mockk.every
import io.mockk.mockk
import jakarta.servlet.http.Cookie
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.mock.env.MockEnvironment
import tech.testsys.infra.localization.bundle.SupportedRegion
import java.time.ZoneId

// Only RU is supported yet, so these tests check that every source of the region is read without failing.
class UiLocaleInitializerTests {
    @Test
    fun `should take the region from the region cookie`() {
        val request = request(cookies = arrayOf(Cookie(REGION_COOKIE, "RU")), acceptLanguage = "en-US")

        assertEquals(SupportedRegion.RU, resolveRegion(request, SupportedRegion.RU))
    }

    @ParameterizedTest
    @ValueSource(strings = ["ru", "ru-RU,ru;q=0.9,en;q=0.8", "en-US, ru;q=0.5", "*"])
    fun `should take the best supported browser language`(acceptLanguage: String) {
        val request = request(cookies = arrayOf(Cookie(REGION_COOKIE, "XX")), acceptLanguage = acceptLanguage)

        assertEquals(SupportedRegion.RU, resolveRegion(request, SupportedRegion.RU))
    }

    @ParameterizedTest
    @ValueSource(strings = ["en-US", "ru;q=abc", ""])
    fun `should fall back to the default region for unsupported or malformed browser languages`(acceptLanguage: String) {
        val request = request(cookies = null, acceptLanguage = acceptLanguage)

        assertEquals(SupportedRegion.RU, resolveRegion(request, SupportedRegion.RU))
    }

    @Test
    fun `should fall back to the default region without a request`() {
        assertEquals(SupportedRegion.RU, resolveRegion(null, SupportedRegion.RU))
    }

    @Test
    fun `should read the default region and time zone from the environment`() {
        val environment = MockEnvironment()
            .withProperty("testsys.web.localization.default-region", "RU")
            .withProperty("testsys.web.localization.default-time-zone", "Asia/Novosibirsk")

        assertEquals(SupportedRegion.RU, environment.defaultRegion())
        assertEquals(ZoneId.of("Asia/Novosibirsk"), environment.defaultTimeZone())
    }

    private fun request(cookies: Array<Cookie>?, acceptLanguage: String?): VaadinRequest = mockk {
        every { this@mockk.cookies } returns cookies
        every { getHeader("Accept-Language") } returns acceptLanguage
    }
}
