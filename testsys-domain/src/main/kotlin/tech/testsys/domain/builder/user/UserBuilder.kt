package tech.testsys.domain.builder.user

import tech.testsys.domain.builder.DomainEntityBuilder
import tech.testsys.domain.model.user.User
import tech.testsys.domain.model.user.UserId

/**
 * Abstract base builder for [User] domain entities.
 *
 * @param U the concrete user type being built.
 * @since %CURRENT_VERSION%
 */
abstract class UserBuilder<UI : UserId, U: User<UI>> : DomainEntityBuilder<U>
