package tech.testsys.infra.database.internal.jpa.entity.session

import jakarta.persistence.Entity
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.SnowflakeJpaEntity
import java.time.Instant

/**
 * Technical record of a sign-in to the main or service interface that survives a restart, independent of domain
 * aggregates.
 *
 * @property tokenHash the unique SHA-256 hex digest of the sign-in token kept in the browser cookie.
 * @property userId the raw id of the signed-in user, or `null` for the service interface.
 * @property userKind the name of the kind of the signed-in user, `SERVICE` for the service interface.
 * @property impersonatedBy the raw id of the supervisor signed in as the user, or `null`.
 * @property lastSeenAt the moment of the last request of the sign-in, recorded at most once a minute.
 * @since %CURRENT_VERSION%
 */
@Entity
@Table(uniqueConstraints = [UniqueConstraint(name = "uk_ts_cabinet_sign_in_token_hash", columnNames = ["token_hash"])])
@InternalDatabaseApi
class CabinetSignInJpaEntity(
    val tokenHash: String,
    val userId: Long?,
    val userKind: String,
    val impersonatedBy: Long?,
    var lastSeenAt: Instant,
    id: Long? = null,
) : SnowflakeJpaEntity(id)
