package tech.testsys.operation.config

import java.time.Duration

/**
 * Configuration of invite codes into classes.
 *
 * @property ttl the validity period of a class invite counted from its creation, replacement or extension.
 * @property refreshPeriod the delay between checks that replace expired class invites, fixed until restart.
 * @since %CURRENT_VERSION%
 */
interface ClassInviteConfig {

    val ttl: Duration

    val refreshPeriod: Duration
}
