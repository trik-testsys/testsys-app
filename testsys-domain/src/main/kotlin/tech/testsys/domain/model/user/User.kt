package tech.testsys.domain.model.user

import tech.testsys.domain.model.DomainEntity
import tech.testsys.domain.model.DomainId
import java.time.Instant

@JvmInline
value class UserId(
    override val value: Long
) : DomainId

sealed class User(
    id: UserId,
    createdAt: Instant,
    val accessToken: String,
) : DomainEntity<UserId>(id, createdAt)
