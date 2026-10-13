package tech.testsys.web.app.security

import org.apache.catalina.filters.RateLimitFilter
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.mock.env.MockEnvironment
import org.springframework.mock.web.MockFilterChain
import org.springframework.mock.web.MockFilterConfig
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import java.time.format.DateTimeParseException

class RateLimitConfigurationTests {
    private var filter: RateLimitFilter? = null

    @AfterEach
    fun destroyFilter() {
        filter?.destroy()
    }

    @ParameterizedTest
    @ValueSource(strings = ["0", "-1"])
    fun `should reject requests that are not positive`(requests: String) {
        assertThrows(IllegalArgumentException::class.java) { rateLimitFilter(environment(requests = requests)) }
    }

    @ParameterizedTest
    @ValueSource(strings = ["PT0S", "PT-1S", "PT0.5S", "PT1.5S", "PT2147483648S", "PT4294967297S"])
    fun `should reject duration that is not a positive whole number of seconds within Int range`(duration: String) {
        assertThrows(IllegalArgumentException::class.java) { rateLimitFilter(environment(duration = duration)) }
    }

    @Test
    fun `should reject duration not in ISO-8601 format`() {
        assertThrows(DateTimeParseException::class.java) { rateLimitFilter(environment(duration = "60s")) }
    }

    @Test
    fun `should pass the request over the limit when enforce is false`() {
        val filter = rateLimitFilter(environment(requests = "1", enforce = "false")).also { this.filter = it }
        filter.init(MockFilterConfig())
        filter.doFilter(MockHttpServletRequest(), MockHttpServletResponse(), MockFilterChain())
        val request = MockHttpServletRequest()
        val response = MockHttpServletResponse()
        val chain = MockFilterChain()

        filter.doFilter(request, response, chain)

        assertEquals(200, response.status)
        assertSame(request, chain.request)
    }

    private fun rateLimitFilter(environment: MockEnvironment): RateLimitFilter =
        checkNotNull(RateLimitConfiguration().rateLimitFilter(environment).filter)

    private fun environment(requests: String = "3", duration: String = "PT1M", enforce: String = "true") = MockEnvironment()
        .withProperty("testsys.web.rate-limit.requests", requests)
        .withProperty("testsys.web.rate-limit.duration", duration)
        .withProperty("testsys.web.rate-limit.enforce", enforce)
}
