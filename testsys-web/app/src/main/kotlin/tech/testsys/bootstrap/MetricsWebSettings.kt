package tech.testsys.bootstrap

import org.springframework.boot.context.properties.bind.Binder
import org.springframework.core.env.Environment

/**
 * Validated listener settings of the metrics server; access to it is restricted by the deployment, not by TestSys.
 *
 * @property address the listener address.
 * @property port the listener port; zero requests a random port in tests.
 * @since %CURRENT_VERSION%
 */
data class MetricsWebSettings(val address: String, val port: Int) {
    /**
     * Binds the metrics server settings before any infrastructure starts.
     *
     * @since %CURRENT_VERSION%
     */
    companion object {
        private const val MAXIMUM_PORT = 65_535

        /**
         * Binds and validates `testsys.metrics.address` and `testsys.metrics.port` from [environment].
         *
         * @throws org.springframework.boot.context.properties.bind.BindException if a value is missing or cannot be converted.
         * @since %CURRENT_VERSION%
         */
        fun from(environment: Environment): MetricsWebSettings {
            val settings = Binder.get(environment).bindOrCreate("testsys.metrics", MetricsWebSettings::class.java)
            require(settings.address.isNotBlank()) { "testsys.metrics.address must not be blank" }
            require(settings.port in 0..MAXIMUM_PORT) { "testsys.metrics.port must be between 0 and $MAXIMUM_PORT" }
            return settings
        }
    }
}
