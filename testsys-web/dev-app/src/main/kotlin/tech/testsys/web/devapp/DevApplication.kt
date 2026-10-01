package tech.testsys.web.devapp

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

/**
 * Standalone application of the TestSys development showcase.
 *
 * @since %CURRENT_VERSION%
 */
@SpringBootApplication
class DevApplication

/**
 * Starts the standalone development application.
 *
 * @since %CURRENT_VERSION%
 */
fun main(args: Array<String>) {
    runApplication<DevApplication>(*args)
}
