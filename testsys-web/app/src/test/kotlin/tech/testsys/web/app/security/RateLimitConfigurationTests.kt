package tech.testsys.web.app.security

import org.apache.catalina.filters.RateLimitFilter
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.boot.context.properties.bind.BindException
import org.springframework.mock.env.MockEnvironment
import org.springframework.mock.web.MockFilterChain
import org.springframework.mock.web.MockFilterConfig
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse

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
    fun `should apply duration in Spring units as the window`() {
        val filter = rateLimitFilter(environment(duration = "60s")).also { this.filter = it }
        filter.setExposeHeaders(true)
        filter.setPolicyName("window")
        filter.init(MockFilterConfig())
        val response = MockHttpServletResponse()

        filter.doFilter(MockHttpServletRequest(), response, MockFilterChain())

        assertEquals("\"window\";q=3;w=60", response.getHeader(RateLimitFilter.HEADER_RATE_LIMIT_POLICY))
    }

    @Test
    fun `should reject duration that is not a duration`() {
        assertThrows(BindException::class.java) { rateLimitFilter(environment(duration = "one minute")) }
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

    @Test
    fun `should not count requests under VAADIN within the context path`() {
        val filter = rateLimitFilter(environment(requests = "1")).also { this.filter = it }
        filter.init(MockFilterConfig())
        filter.doFilter(request("/testsys/VAADIN/build/app.js"), MockHttpServletResponse(), MockFilterChain())
        val response = MockHttpServletResponse()

        filter.doFilter(request("/testsys/VAADIN/build/app.js"), response, MockFilterChain())

        assertEquals(200, response.status)
    }

    @Test
    fun `should count requests outside VAADIN within the context path`() {
        val filter = rateLimitFilter(environment(requests = "1")).also { this.filter = it }
        filter.init(MockFilterConfig())
        filter.doFilter(request("/testsys/VAADIN/build/app.js"), MockHttpServletResponse(), MockFilterChain())
        filter.doFilter(request("/testsys/login"), MockHttpServletResponse(), MockFilterChain())
        val response = MockHttpServletResponse()

        filter.doFilter(request("/testsys/login"), response, MockFilterChain())

        assertEquals(429, response.status)
    }

    private fun request(uri: String) = MockHttpServletRequest("GET", uri).apply { contextPath = "/testsys" }

    private fun rateLimitFilter(environment: MockEnvironment): RateLimitFilter =
        checkNotNull(RateLimitConfiguration().rateLimitFilter(environment).filter)

    private fun environment(requests: String = "3", duration: String = "PT1M", enforce: String = "true") = MockEnvironment()
        .withProperty("testsys.web.rate-limit.requests", requests)
        .withProperty("testsys.web.rate-limit.duration", duration)
        .withProperty("testsys.web.rate-limit.enforce", enforce)
}
