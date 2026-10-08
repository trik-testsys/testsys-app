package tech.testsys.domain.model.group

import tech.testsys.domain.model.user.HashAlgorithm

/**
 * Stored invite-code representation and the algorithm that produced it.
 *
 * @property value the stored representation.
 * @property algorithm the algorithm used to produce [value].
 * @since %CURRENT_VERSION%
 */
data class InviteCodeHash(
    val value: String,
    val algorithm: HashAlgorithm,
) {
    companion object {
        /**
         * Hashes [rawInviteCode] with [algorithm] without normalizing or validating the input.
         *
         * @since %CURRENT_VERSION%
         */
        fun hashInviteCode(rawInviteCode: String, algorithm: HashAlgorithm): InviteCodeHash {
            val value = when (algorithm) {
                HashAlgorithm.Identity -> rawInviteCode
            }
            return InviteCodeHash(value = value, algorithm = algorithm)
        }
    }
}
