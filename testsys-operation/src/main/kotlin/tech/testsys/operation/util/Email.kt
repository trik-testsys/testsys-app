package tech.testsys.operation.util

import tech.testsys.operation.annotation.InternalOperationsApi
import tech.testsys.operation.config.EmailConfirmationConfig
import java.time.Instant
import java.time.temporal.ChronoUnit

private const val MAX_EMAIL_LENGTH = 255

/**
 * Returns [email] without surrounding whitespace in lower case, the form in which e-mail addresses are stored and compared.
 *
 * @since %CURRENT_VERSION%
 */
@InternalOperationsApi
fun normalizeEmail(email: String): String = email.trim().lowercase()

/**
 * Checks that [email] has at most 255 Unicode code points and exactly one `@` with non-empty parts around it.
 *
 * @since %CURRENT_VERSION%
 */
@InternalOperationsApi
fun isValidEmail(email: String): Boolean {
    val parts = email.split('@')
    return email.codePointCount(0, email.length) <= MAX_EMAIL_LENGTH && parts.size == 2 && parts.all { it.isNotEmpty() }
}

/**
 * Returns the moment a confirmation code issued at [now] expires, truncated to microseconds.
 *
 * @since %CURRENT_VERSION%
 */
@InternalOperationsApi
fun EmailConfirmationConfig.confirmationCodeExpiresAt(now: Instant): Instant =
    now.plus(confirmationCodeLifetime).truncatedTo(ChronoUnit.MICROS)

/**
 * Checks whether a confirmation code expiring at [expiresAt] with [attemptsLeft] remaining attempts is still accepted at [now].
 *
 * @since %CURRENT_VERSION%
 */
@InternalOperationsApi
fun isConfirmationActive(expiresAt: Instant, attemptsLeft: Int, now: Instant): Boolean = now.isBefore(expiresAt) && attemptsLeft > 0
