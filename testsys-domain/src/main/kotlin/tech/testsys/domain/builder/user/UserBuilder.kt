package tech.testsys.domain.builder.user

import tech.testsys.domain.builder.DomainEntityBuilder
import tech.testsys.domain.model.user.User

abstract class UserBuilder<U: User> : DomainEntityBuilder<U> {

    var accessToken: String? = null

}
