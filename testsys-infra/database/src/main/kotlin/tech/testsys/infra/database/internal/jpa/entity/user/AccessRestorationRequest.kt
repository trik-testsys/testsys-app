package tech.testsys.infra.database.internal.jpa.entity.user

import jakarta.persistence.Entity
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.SnowflakeJpaEntity
import java.time.Instant

/**
 * JPA entity of [tech.testsys.domain.model.user.AccessRestorationRequest].
 *
 * @property userId the user restoring access; unique among requests.
 * @property confirmationCode the confirmation code sent to the e-mail address of the user.
 * @property expiresAt the moment the confirmation code expires.
 * @property attemptsLeft the number of remaining attempts to enter the code.
 * @since 1.0.0-beta.1
 */
@Entity
@InternalDatabaseApi
class AccessRestorationRequestJpaEntity(
    val userId: Long,
    val confirmationCode: String,
    val expiresAt: Instant,
    val attemptsLeft: Int,
    id: Long? = null,
) : SnowflakeJpaEntity(id)
