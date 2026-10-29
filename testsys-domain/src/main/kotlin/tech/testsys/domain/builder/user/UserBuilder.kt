package tech.testsys.domain.builder.user

import tech.testsys.domain.builder.Builder
import tech.testsys.domain.builder.DomainEntityWithDataBuilder
import tech.testsys.domain.builder.util.requireField
import tech.testsys.domain.model.user.AccessTokenHash
import tech.testsys.domain.model.user.AccessTokenHasher
import tech.testsys.domain.model.user.SingleRoleUserData
import tech.testsys.domain.model.user.User
import tech.testsys.domain.model.user.UserId

/**
 * Base class of [User] builders.
 *
 * @param UI the identifier type of the built user.
 * @param U the type of the built user.
 * @param Data the type of the user data.
 * @param DataBuilder the builder type of [Data].
 * @since 1.0.0-beta.1
 */
abstract class UserBuilder<UI : UserId, U : User<UI>, Data, DataBuilder : Builder<Data>> :
    DomainEntityWithDataBuilder<U, Data, DataBuilder>()

/**
 * Base class of fixed-role user data builders. Required: [accessToken] or [storedAccessToken].
 *
 * @param Data the type of the built user data.
 * @since 1.0.0-beta.1
 */
abstract class UserDataBuilder<Data : SingleRoleUserData> : Builder<Data> {

    private var tokenHash: AccessTokenHash? = null

    /**
     * Hashes [rawAccessToken] with [hasher] and sets its stored value and algorithm together.
     *
     * @since 1.0.0-beta.1
     */
    fun accessToken(rawAccessToken: String, hasher: AccessTokenHasher) {
        tokenHash = hasher.hash(rawAccessToken)
    }

    /**
     * Restores [hash] from stored data without hashing.
     *
     * @since 1.0.0-beta.1
     */
    fun storedAccessToken(hash: AccessTokenHash) {
        tokenHash = hash
    }

    protected fun requireAccessTokenHash(): AccessTokenHash = requireField(tokenHash) { ::tokenHash }
}
