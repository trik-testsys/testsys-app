package tech.testsys.bootstrap

import org.springframework.core.env.Environment
import java.security.MessageDigest
import java.util.regex.Pattern

/**
 * Validated service listener settings and a digest of its startup secret; string rendering never includes the secret.
 *
 * @property address the listener address configured for deployment.
 * @property port the listener port; zero requests a random port in tests.
 * @property trustedProxies the explicit regular expression of trusted proxy addresses, or an expression matching none.
 * @since %CURRENT_VERSION%
 */
class ServiceWebSettings private constructor(
    val address: String,
    val port: Int,
    val trustedProxies: String,
    private val secretDigest: ByteArray,
) {
    /**
     * Compares the digest of [candidate] without returning on a matching prefix.
     *
     * @since %CURRENT_VERSION%
     */
    fun matches(candidate: String): Boolean = MessageDigest.isEqual(secretDigest, digest(candidate))

    companion object {
        private const val MINIMUM_SECRET_LENGTH = 64
        private const val MAXIMUM_PORT = 65_535

        /**
         * Reads and validates service settings before any infrastructure starts, without including the secret in errors.
         *
         * @since %CURRENT_VERSION%
         */
        fun from(environment: Environment): ServiceWebSettings {
            val secret = environment.getProperty("testsys.service.secret")
            require(secret != null && secret.isNotBlank() && secret.length >= MINIMUM_SECRET_LENGTH) {
                "testsys.service.secret must contain at least $MINIMUM_SECRET_LENGTH characters"
            }

            val address = environment.getRequiredProperty("testsys.service.address")
            require(address.isNotBlank()) { "testsys.service.address must not be blank" }

            val port = environment.getRequiredProperty("testsys.service.port", Int::class.java)
            require(port in 0..MAXIMUM_PORT) { "testsys.service.port must be between 0 and $MAXIMUM_PORT" }

            val proxies = environment.getProperty("testsys.service.trusted-proxies")?.takeIf { it.isNotBlank() } ?: "(?!)"
            Pattern.compile(proxies)
            return ServiceWebSettings(address = address, port = port, trustedProxies = proxies, secretDigest = digest(secret))
        }

        private fun digest(secret: String): ByteArray = MessageDigest.getInstance("SHA-256").digest(secret.toByteArray(Charsets.UTF_8))
    }
}
