package tech.testsys.web.app

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

/**
 * Spring Boot application of the TestSys web interface.
 *
 * @since %CURRENT_VERSION%
 */
@SpringBootApplication
class TestSysApplication

/**
 * Starts the TestSys web application.
 *
 * @since %CURRENT_VERSION%
 */
fun main(args: Array<String>) {
    runApplication<TestSysApplication>(*args)
}
