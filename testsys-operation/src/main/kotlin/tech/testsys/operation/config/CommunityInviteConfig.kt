package tech.testsys.operation.config

import java.time.Duration

/**
 * Configuration of invite codes into communities.
 *
 * @property ttl the validity period of a community invite counted from its creation, replacement or extension.
 * @property refreshPeriod the delay between checks that replace expired community invites, fixed until restart.
 * @since %CURRENT_VERSION%
 */
interface CommunityInviteConfig {

    val ttl: Duration

    val refreshPeriod: Duration
}
