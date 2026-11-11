package tech.testsys.web.app.config

import tech.testsys.infra.settings.api.SettingsProperties
import java.time.Duration

/**
 * Competition settings, bound from `testsys.operation.competition`.
 *
 * @property maxParticipants the maximum total number of Participants in a Competition.
 * @since 1.0.0-beta.1
 */
@SettingsProperties("testsys.operation.competition")
data class CompetitionProperties(val maxParticipants: Int)

/**
 * Email confirmation settings, bound from `testsys.operation.email-confirmation`.
 *
 * @property confirmationCodeLifetime the validity of a confirmation code.
 * @property maxConfirmationAttempts the number of attempts to enter a confirmation code.
 * @since 1.0.0-beta.1
 */
@SettingsProperties("testsys.operation.email-confirmation")
data class EmailConfirmationProperties(val confirmationCodeLifetime: Duration, val maxConfirmationAttempts: Int)

/**
 * Community invite settings, bound from `testsys.operation.community-invite`.
 *
 * @property ttl the validity of a Community invite code.
 * @since 1.0.0-beta.1
 */
@SettingsProperties("testsys.operation.community-invite")
data class CommunityInviteProperties(val ttl: Duration)

/**
 * Class invite settings, bound from `testsys.operation.class-invite`.
 *
 * @property ttl the validity of a Class invite code.
 * @since 1.0.0-beta.1
 */
@SettingsProperties("testsys.operation.class-invite")
data class ClassInviteProperties(val ttl: Duration)
