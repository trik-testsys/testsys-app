package tech.testsys.domain.model.user

/** Access-code hasher with a fixed test secret. */
val testAccessTokenHasher = AccessTokenHasher("0123456789abcdef".repeat(4))
