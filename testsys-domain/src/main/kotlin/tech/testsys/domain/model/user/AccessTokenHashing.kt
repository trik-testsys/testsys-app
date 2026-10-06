package tech.testsys.domain.model.user

/**
 * Stored access-code representation and the algorithm that produced it.
 *
 * @property value the stored representation.
 * @property algorithm the algorithm used to produce [value].
 * @since %CURRENT_VERSION%
 */
data class AccessTokenHash(
    val value: String,
    val algorithm: HashAlgorithm,
)

/**
 * Hashes [rawAccessToken] with [algorithm] without normalizing or validating the input.
 *
 * @since %CURRENT_VERSION%
 */
fun hashAccessToken(rawAccessToken: String, algorithm: HashAlgorithm): AccessTokenHash {
    val value = when (algorithm) {
        HashAlgorithm.Identity -> rawAccessToken
    }
    return AccessTokenHash(value = value, algorithm = algorithm)
}
