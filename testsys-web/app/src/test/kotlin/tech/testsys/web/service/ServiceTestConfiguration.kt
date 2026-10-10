package tech.testsys.web.service

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.core.env.Environment
import org.springframework.mock.env.MockEnvironment
import tech.testsys.bootstrap.ServiceWebSettings
import java.time.Clock

const val SERVICE_TEST_SECRET: String = "test-service-code-that-is-only-used-by-automated-tests-0123456789-ABCD"

@TestConfiguration(proxyBeanMethods = false)
class ServiceTestConfiguration {
    @Bean
    fun serviceSettings(environment: Environment): ServiceWebSettings = ServiceWebSettings.from(
        MockEnvironment().withProperty("testsys.service.secret", SERVICE_TEST_SECRET)
            .withProperty("testsys.service.trusted-proxies", environment.getProperty("testsys.service.trusted-proxies", ""))
            .withProperty("testsys.service.address", "127.0.0.1").withProperty("testsys.service.port", "0"),
    )

    @Bean
    fun clock(): Clock = Clock.systemUTC()
}
