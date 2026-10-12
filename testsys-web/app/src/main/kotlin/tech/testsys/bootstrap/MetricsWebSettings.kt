package tech.testsys.bootstrap

import org.springframework.core.env.Environment

/**
 * Validated listener settings of the metrics server; access to it is restricted by the deployment, not by TestSys.
 *
 * @property address the listener address, all interfaces unless configured.
 * @property port the listener port; zero requests a random port in tests.
 * @since %CURRENT_VERSION%
 */
data class MetricsWebSettings(val address: String, val port: Int) {
    /**
     * Reads the metrics server settings before any infrastructure starts.
     *
     * @since %CURRENT_VERSION%
     */
    companion object {
        private const val ALL_INTERFACES = "0.0.0.0"
        private const val MAXIMUM_PORT = 65_535

        /**
         * Reads the optional `testsys.metrics.address` and the required `testsys.metrics.port` from [environment].
         *
         * @since %CURRENT_VERSION%
         */
        fun from(environment: Environment): MetricsWebSettings {
            val address = environment.getProperty("testsys.metrics.address") ?: ALL_INTERFACES
            require(address.isNotBlank()) { "testsys.metrics.address must not be blank" }

            val port = environment.getRequiredProperty("testsys.metrics.port", Int::class.java)
            require(port in 0..MAXIMUM_PORT) { "testsys.metrics.port must be between 0 and $MAXIMUM_PORT" }
            return MetricsWebSettings(address = address, port = port)
        }
    }
}
