package tech.testsys.domain.model.user

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
