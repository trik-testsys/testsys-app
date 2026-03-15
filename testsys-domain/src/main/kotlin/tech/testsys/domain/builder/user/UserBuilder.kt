package tech.testsys.domain.builder.user

import tech.testsys.domain.builder.DomainEntityBuilder
import tech.testsys.domain.model.user.User

/**
 * Abstract base builder for [User] domain entities.
 *
 * @param U the concrete user type being built.
 * @since %CURRENT_VERSION%
 */
abstract class UserBuilder<U: User> : DomainEntityBuilder<U> {

    /**
     * The access token for authenticating this user.
     *
     * @since %CURRENT_VERSION%
     */
    var accessToken: String? = null

}
