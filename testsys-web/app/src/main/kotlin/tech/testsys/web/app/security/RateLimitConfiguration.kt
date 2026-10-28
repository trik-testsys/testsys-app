package tech.testsys.web.app.security

import jakarta.servlet.FilterChain
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import jakarta.servlet.http.HttpServletRequest
import org.apache.catalina.filters.RateLimitFilter
import org.apache.catalina.util.ExactRateLimiter
import org.springframework.boot.context.properties.bind.Binder
import org.springframework.boot.context.properties.bind.Name
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered
import org.springframework.core.env.Environment
import java.time.Duration

/**
 * Limits the number of HTTP requests of the main interface per client address with the Tomcat [RateLimitFilter].
 * Requests under `/VAADIN/` are not counted.
 *
 * @since %CURRENT_VERSION%
 */
@Configuration(proxyBeanMethods = false)
class RateLimitConfiguration {
    /**
     * Filter answering 429 to a client address that exceeds `testsys.web.rate-limit.requests` within one window
     * of `testsys.web.rate-limit.duration`, or only logging it when `testsys.web.rate-limit.enforce` is `false`.
     *
     * @since %CURRENT_VERSION%
     */
    @Bean
    fun rateLimitFilter(environment: Environment): FilterRegistrationBean<RateLimitFilter> {
        val (requests, duration, isEnforced) = Binder.get(environment).bindOrCreate(PREFIX, RateLimitProperties::class.java)
        require(requests > 0) { "$PREFIX.requests must be positive, got $requests" }
        require(duration.isPositive && duration.nano == 0 && duration.seconds <= Int.MAX_VALUE) {
            "$PREFIX.duration must be a positive whole number of seconds up to ${Int.MAX_VALUE}, got $duration"
        }

        val filter: RateLimitFilter = object : RateLimitFilter() {
            // Bundles, fonts and the push channel live under /VAADIN/; browsers do not retry them after a 429.
            override fun doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain) {
                val http = request as HttpServletRequest
                if (http.requestURI.removePrefix(http.contextPath).startsWith(VAADIN_PATH)) {
                    chain.doFilter(request, response)
                } else {
                    super.doFilter(request, response, chain)
                }
            }
        }.apply {
            setRateLimitClassName(ExactRateLimiter::class.java.name)
            setBucketRequests(requests)
            setBucketDuration(duration.seconds.toInt())
            setEnforce(isEnforced)
        }
        return FilterRegistrationBean(filter).apply {
            // Inside the request observation, so rejected requests are counted with status 429.
            order = Ordered.HIGHEST_PRECEDENCE + 2
        }
    }

    private companion object {
        const val PREFIX = "testsys.web.rate-limit"
        const val VAADIN_PATH = "/VAADIN/"
    }
}

/** Rate limit parameters bound from `testsys.web.rate-limit`; [RateLimitConfiguration] validates them. */
private data class RateLimitProperties(val requests: Int, val duration: Duration, @Name("enforce") val isEnforced: Boolean)
