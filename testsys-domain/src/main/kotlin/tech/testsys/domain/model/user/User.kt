package tech.testsys.domain.model.user

import tech.testsys.domain.model.DomainEntity
import tech.testsys.domain.model.DomainId
import java.time.Instant

interface UserId : DomainId

sealed class User<Id : UserId>(
    id: Id,
    createdAt: Instant,
) : DomainEntity<Id>(id, createdAt) {

    abstract val accessToken: String
}
