package tech.testsys.operation.util

import tech.testsys.operation.annotation.InternalOperationsApi
import java.util.random.RandomGenerator

private const val CONFIRMATION_CODE_LENGTH = 8
private const val CONFIRMATION_CODE_ALPHABET = "0123456789"

private const val ACCESS_TOKEN_GROUP_COUNT = 4
private const val ACCESS_TOKEN_GROUP_LENGTH = 4
private const val ACCESS_TOKEN_SEPARATOR = "-"
private const val ACCESS_TOKEN_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"

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

private fun RandomGenerator.nextString(alphabet: String, length: Int): String = buildString(length) {
    repeat(length) { append(alphabet[nextInt(alphabet.length)]) }
}
