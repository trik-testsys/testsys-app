package tech.testsys.operation.util

import tech.testsys.domain.model.group.InviteCodeHash
import tech.testsys.operation.annotation.InternalOperationsApi
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Locale
import java.util.Random

private const val INVITE_CODE_ALPHABET = "abcdefghjkmnpqrstuvwxyz23456789"
private const val INVITE_CODE_LENGTH = 12

/**
 * Generates a raw invite code of 12 characters drawn by [random] from the lowercase invite-code alphabet.
 * The code is regenerated while it hashes, with the algorithm of [previous], to the replaced [previous] code.
 *
 * @since %CURRENT_VERSION%
 */
@InternalOperationsApi
fun generateInviteCode(random: Random, previous: InviteCodeHash?): String = generateSequence { randomInviteCode(random) }
    .first { code -> previous == null || InviteCodeHash.hashInviteCode(code, previous.algorithm) != previous }

/**
 * Normalizes a user-entered invite code by lowercasing [input] with the root locale; other characters are kept.
 *
 * @since %CURRENT_VERSION%
 */
@InternalOperationsApi
fun normalizeInviteCode(input: String): String = input.lowercase(Locale.ROOT)

/**
 * Returns the expiration moment of an invite created, replaced or extended at [now] with the validity period [ttl],
 * truncated to microseconds.
 *
 * @throws IllegalStateException if [ttl] is zero or negative.
 * @since %CURRENT_VERSION%
 */
@InternalOperationsApi
fun inviteExpiresAt(now: Instant, ttl: Duration): Instant {
    check(ttl > Duration.ZERO) { "Invite ttl must be positive, got $ttl" }
    return now.plus(ttl).truncatedTo(ChronoUnit.MICROS)
}

private fun randomInviteCode(random: Random): String = buildString(INVITE_CODE_LENGTH) {
    repeat(INVITE_CODE_LENGTH) { append(INVITE_CODE_ALPHABET[random.nextInt(INVITE_CODE_ALPHABET.length)]) }
}
