package tech.testsys.infra.database.internal.jpa.entity.user

import jakarta.persistence.Entity
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.SnowflakeJpaEntity
import java.time.Instant

/**
 * JPA entity of [tech.testsys.domain.model.user.RegistrationRequest].
 *
 * @property email the e-mail address being confirmed; unique among requests.
 * @property confirmationCode the confirmation code sent to [email].
 * @property expiresAt the moment the confirmation code expires.
 * @property attemptsLeft the number of remaining attempts to enter the code.
 * @since %CURRENT_VERSION%
 */
@Entity
@InternalDatabaseApi
class RegistrationRequestJpaEntity(
    val email: String,
    val confirmationCode: String,
    val expiresAt: Instant,
    val attemptsLeft: Int,
    id: Long? = null,
) : SnowflakeJpaEntity(id)
