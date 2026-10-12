package tech.testsys.web.service.security

import java.io.Serializable

/**
 * Technical service identity without a domain user id, access code or startup secret.
 * [AUTHORITY] is checked by navigation and HTTP security.
 *
 * @property AUTHORITY the authority belonging exclusively to the service interface.
 * @since %CURRENT_VERSION%
 */
data object ServicePrincipal : Serializable {
    const val AUTHORITY: String = "ROLE_SERVICE"

    private const val serialVersionUID: Long = 1
}
