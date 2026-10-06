package tech.testsys.domain.builder.user

import tech.testsys.domain.builder.Builder
import tech.testsys.domain.builder.DomainEntityWithDataBuilder
import tech.testsys.domain.builder.util.requireField
import tech.testsys.domain.model.user.AccessTokenHash
import tech.testsys.domain.model.user.HashAlgorithm
import tech.testsys.domain.model.user.User
import tech.testsys.domain.model.user.UserData
import tech.testsys.domain.model.user.UserId
import tech.testsys.domain.model.user.hashAccessToken

/**
 * Base class of [User] builders.
 *
 * @param UI the identifier type of the built user.
 * @param U the type of the built user.
 * @param Data the type of the user data.
 * @param DataBuilder the builder type of [Data].
 * @since %CURRENT_VERSION%
 */
abstract class UserBuilder<UI : UserId, U : User<UI>, Data, DataBuilder : Builder<Data>> :
    DomainEntityWithDataBuilder<U, Data, DataBuilder>()

/**
 * Base class of user data builders. Required: [accessToken] or [storedAccessToken].
 *
 * @param Data the type of the built user data.
 * @since %CURRENT_VERSION%
 */
abstract class UserDataBuilder<Data : UserData> : Builder<Data> {

    private var tokenHash: AccessTokenHash? = null

    /**
     * Hashes [rawAccessToken] with [algorithm] and sets its stored value and algorithm together.
     *
     * @since %CURRENT_VERSION%
     */
    fun accessToken(rawAccessToken: String, algorithm: HashAlgorithm) {
        tokenHash = hashAccessToken(rawAccessToken = rawAccessToken, algorithm = algorithm)
    }

    /**
     * Restores [value] and [algorithm] from stored data without hashing.
     *
     * @since %CURRENT_VERSION%
     */
    fun storedAccessToken(value: String, algorithm: HashAlgorithm) {
        tokenHash = AccessTokenHash(value = value, algorithm = algorithm)
    }

    protected fun requireAccessTokenHash(): AccessTokenHash = requireField(tokenHash) { ::tokenHash }
}
