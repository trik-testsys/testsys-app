package tech.testsys.domain.builder.user

import tech.testsys.domain.builder.Builder
import tech.testsys.domain.builder.DomainEntityWithDataBuilder
import tech.testsys.domain.model.user.User
import tech.testsys.domain.model.user.UserId

/**
 * Abstract base builder for [User] domain entities.
 *
 *
 * @param U the concrete [User] type being built.
 * @param UI the concrete [UserId] of U.
 * @param Data the type of associated data object.
 * @param DataBuilder the builder type used to construct [Data].
 * @since %CURRENT_VERSION%
 */
abstract class UserBuilder<UI : UserId, U: User<UI>, Data, DataBuilder: Builder<Data>>
    : DomainEntityWithDataBuilder<U, Data, DataBuilder>()
