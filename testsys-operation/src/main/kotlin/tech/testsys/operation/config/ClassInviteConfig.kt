package tech.testsys.operation.config

import tech.testsys.operation.annotation.ConfigProperty
import tech.testsys.operation.annotation.OperationConfig
import java.time.Duration

/**
 * Configuration of invite codes into classes.
 *
 * @property ttl the validity period of a class invite counted from its creation, replacement or extension.
 * @property refreshPeriod the delay between checks that replace expired class invites, fixed until restart.
 * @since %CURRENT_VERSION%
 */
@OperationConfig("class-invite")
interface ClassInviteConfig {

    @ConfigProperty(isDynamic = true)
    val ttl: Duration

    val refreshPeriod: Duration
}
