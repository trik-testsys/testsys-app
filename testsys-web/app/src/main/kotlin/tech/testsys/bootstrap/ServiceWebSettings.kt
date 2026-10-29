package tech.testsys.bootstrap

import org.springframework.core.env.Environment
import tech.testsys.infra.settings.api.strictBinder
import java.net.URI
import java.net.URISyntaxException
import java.security.MessageDigest
import java.util.HexFormat

/**
 * Validated service listener settings and a digest of its startup secret; string rendering never includes the secret.
 *
 * @property address the listener address configured for deployment.
 * @property port the listener port; zero requests a random port in tests.
 * @property grafanaUrl the absolute http or https address of Grafana opened from the service cabinet without signing in.
 * @since 1.0.0-beta.1
 */
class ServiceWebSettings private constructor(
    val address: String,
    val port: Int,
    val grafanaUrl: String,
    private val secretDigest: ByteArray,
) {
    /**
     * Compares the digest of [candidate] without returning on a matching prefix.
     *
     * @since 1.0.0-beta.1
     */
    fun matches(candidate: String): Boolean = MessageDigest.isEqual(secretDigest, digest(candidate))

    /**
     * Returns the SHA-256 hex digest of the sign-in [token] bound to the startup secret, so that a changed secret
     * invalidates the sign-ins made with the previous one.
     *
     * @since 1.0.0-beta.1
     */
    fun signInTokenHash(token: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(secretDigest)
        return HexFormat.of().formatHex(digest.digest(token.toByteArray(Charsets.UTF_8)))
    }

    companion object {
        private const val MINIMUM_SECRET_LENGTH = 64
        private const val MAXIMUM_PORT = 65_535
        private val WEB_SCHEMES = setOf("http", "https")

        /**
         * Binds and validates `testsys.service` before any infrastructure starts, without including the secret in errors.
         *
         * @throws org.springframework.boot.context.properties.bind.BindException if the address or the port cannot be bound
         * or a placeholder cannot be resolved.
         * @throws IllegalArgumentException if the secret or the Grafana address is missing or invalid, the address is blank
         * or the port is out of range.
         *
         * @since 1.0.0-beta.1
         */
        fun from(environment: Environment): ServiceWebSettings {
            val binder = strictBinder(environment)
            // Bound apart from the other properties, so a failure report of the binder never shows its value.
            val secret = binder.bind("testsys.service.secret", String::class.java).orElse(null)
            require(secret != null && secret.isNotBlank() && secret.length >= MINIMUM_SECRET_LENGTH) {
                "testsys.service.secret must contain at least $MINIMUM_SECRET_LENGTH characters"
            }

            val properties = binder.bindOrCreate("testsys.service", ServiceWebProperties::class.java)
            require(properties.address.isNotBlank()) { "testsys.service.address must not be blank" }
            require(properties.port in 0..MAXIMUM_PORT) { "testsys.service.port must be between 0 and $MAXIMUM_PORT" }

            val grafanaUrl = properties.grafanaUrl
            require(grafanaUrl != null && isWebAddress(grafanaUrl)) {
                "testsys.service.grafana-url must be an absolute http or https URL without user information"
            }

            return ServiceWebSettings(
                address = properties.address,
                port = properties.port,
                grafanaUrl = grafanaUrl,
                secretDigest = digest(secret),
            )
        }

        private fun digest(secret: String): ByteArray = MessageDigest.getInstance("SHA-256").digest(secret.toByteArray(Charsets.UTF_8))

        private fun isWebAddress(value: String): Boolean {
            val uri = try {
                URI(value)
            } catch (_: URISyntaxException) {
                return false
            }
            return uri.isAbsolute && uri.scheme.lowercase() in WEB_SCHEMES && !uri.host.isNullOrEmpty() && uri.rawUserInfo == null
        }
    }
}

/** Non-secret service listener parameters bound from `testsys.service`; [ServiceWebSettings.from] validates them. */
private class ServiceWebProperties(val address: String, val port: Int, val grafanaUrl: String?)
