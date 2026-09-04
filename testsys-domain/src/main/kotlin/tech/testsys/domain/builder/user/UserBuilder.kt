package tech.testsys.domain.builder.user

import tech.testsys.domain.builder.Builder
import tech.testsys.domain.builder.DomainEntityWithDataBuilder
import tech.testsys.domain.model.user.User
import tech.testsys.domain.model.user.UserId

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
