package tech.testsys.domain.builder.group

import tech.testsys.domain.builder.Builder
import tech.testsys.domain.builder.util.requireField
import tech.testsys.domain.model.group.InviteCodeHash
import tech.testsys.domain.model.user.HashAlgorithm

/**
 * Base class of invite data builders. Required: [code] or [storedCode].
 *
 * @param Data the type of the built invite data.
 * @since 1.0.0-beta.1
 */
abstract class InviteCodeDataBuilder<Data> : Builder<Data> {

    private var codeHash: InviteCodeHash? = null

    /**
     * Hashes [rawInviteCode] with [algorithm] and sets its stored value and algorithm together.
     *
     * @since 1.0.0-beta.1
     */
    fun code(rawInviteCode: String, algorithm: HashAlgorithm) {
        codeHash = InviteCodeHash.hashInviteCode(rawInviteCode = rawInviteCode, algorithm = algorithm)
    }

    /**
     * Restores [hash] from stored data without hashing.
     *
     * @since 1.0.0-beta.1
     */
    fun storedCode(hash: InviteCodeHash) {
        codeHash = hash
    }

    protected fun requireCodeHash(): InviteCodeHash = requireField(codeHash) { ::codeHash }
}
