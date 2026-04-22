package tech.testsys.domain.model.user

import tech.testsys.domain.model.DomainEntity
import tech.testsys.domain.model.DomainId
import java.time.Instant

interface UserId : DomainId

sealed class User<Id : UserId>(
    id: Id,
    createdAt: Instant,
    data: UserData,
) : DomainEntity<Id>(id, createdAt), UserData by data

interface UserData {

    val accessToken: String
    val name: String
    val description: String
}

interface WithEmail {

    val email: String
}