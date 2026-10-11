package tech.testsys.infra.database.internal.jpa.entity.settings

import jakarta.persistence.Entity
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.SnowflakeJpaEntity
import java.util.UUID

/**
 * Single technical settings set, independent of domain aggregates.
 *
 * @property setKey the constant key identifying the sole set.
 * @property payload serialized explicit overrides.
 * @property revision monotonically increasing service revision.
 * @property changeId identity of the last completed write.
 * @since %CURRENT_VERSION%
 */
@Entity
@Table(uniqueConstraints = [UniqueConstraint(name = "uk_ts_runtime_settings_set_key", columnNames = ["set_key"])])
@InternalDatabaseApi
class RuntimeSettingsJpaEntity(
    val setKey: String,
    @field:JdbcTypeCode(SqlTypes.LONG32VARCHAR)
    var payload: String,
    var revision: Long,
    var changeId: UUID,
    id: Long? = null,
) : SnowflakeJpaEntity(id)
