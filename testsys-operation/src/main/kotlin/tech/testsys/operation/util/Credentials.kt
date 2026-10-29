package tech.testsys.operation.util

import tech.testsys.operation.annotation.InternalOperationsApi
import java.util.random.RandomGenerator

private const val CONFIRMATION_CODE_LENGTH = 8
private const val CONFIRMATION_CODE_ALPHABET = "0123456789"

private const val ACCESS_TOKEN_GROUP_COUNT = 4
private const val ACCESS_TOKEN_GROUP_LENGTH = 4
private const val ACCESS_TOKEN_SEPARATOR = "-"
private const val ACCESS_TOKEN_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"

private const val MIN_PASSWORD_LENGTH = 8
private const val MAX_PASSWORD_LENGTH = 64
private const val PASSWORD_SYMBOLS = "!@#$%^&*()-_=+.,?"

/**
 * Generates a registration confirmation code of 8 decimal digits.
 *
 * @since %CURRENT_VERSION%
 */
@InternalOperationsApi
fun RandomGenerator.nextConfirmationCode(): String = nextString(CONFIRMATION_CODE_ALPHABET, CONFIRMATION_CODE_LENGTH)

/**
 * Generates an access code of 16 Latin letters and digits in groups of four separated by `-`, e.g. `aB3d-x9Yz-0kLm-P7qR`.
 *
 * @since %CURRENT_VERSION%
 */
@InternalOperationsApi
fun RandomGenerator.nextAccessToken(): String = List(ACCESS_TOKEN_GROUP_COUNT) {
    nextString(ACCESS_TOKEN_ALPHABET, ACCESS_TOKEN_GROUP_LENGTH)
}.joinToString(ACCESS_TOKEN_SEPARATOR)

/**
 * Checks that [value] has the format of codes from [nextAccessToken]: 16 Latin letters and digits in groups of four
 * separated by `-`, without surrounding whitespace.
 *
 * @since %CURRENT_VERSION%
 */
@InternalOperationsApi
fun isAccessToken(value: String): Boolean {
    val groups = value.split(ACCESS_TOKEN_SEPARATOR)
    return groups.size == ACCESS_TOKEN_GROUP_COUNT &&
        groups.all { group -> group.length == ACCESS_TOKEN_GROUP_LENGTH && group.all { char -> char in ACCESS_TOKEN_ALPHABET } }
}

/**
 * Checks that [password] has 8 to 64 characters, each a Latin letter, a digit or one of `!@#$%^&*()-_=+.,?`, and has
 * at least one lowercase letter, one uppercase letter, one digit and one of those symbols; the password is taken exactly
 * as entered, without trimming, so a space makes it invalid. Every allowed character takes one byte, so the password
 * always fits the 72 bytes bcrypt reads.
 *
 * @since %CURRENT_VERSION%
 */
@InternalOperationsApi
fun isValidPassword(password: String): Boolean = password.length in MIN_PASSWORD_LENGTH..MAX_PASSWORD_LENGTH &&
    password.all { char -> char in 'a'..'z' || char in 'A'..'Z' || char in '0'..'9' || char in PASSWORD_SYMBOLS } &&
    password.any { char -> char in 'a'..'z' } &&
    password.any { char -> char in 'A'..'Z' } &&
    password.any { char -> char in '0'..'9' } &&
    password.any { char -> char in PASSWORD_SYMBOLS }

private fun RandomGenerator.nextString(alphabet: String, length: Int): String = buildString(length) {
    repeat(length) { append(alphabet[nextInt(alphabet.length)]) }
}
