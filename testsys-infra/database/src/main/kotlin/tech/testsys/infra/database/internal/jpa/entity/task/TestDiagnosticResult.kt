package tech.testsys.infra.database.internal.jpa.entity.task

import jakarta.persistence.Embeddable
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.CompositeId
import tech.testsys.infra.database.internal.jpa.entity.CompositeJpaEntity
import tech.testsys.infra.database.internal.jpa.entity.SnowflakeJpaEntity

/**
 * Composite key of [TestDiagnosticResultJpaEntity].
 *
 * @property requestId the request identifier.
 * @property testId the polygon identifier.
 * @since %CURRENT_VERSION%
 */
@Embeddable
@InternalDatabaseApi
data class TestDiagnosticResultId(val requestId: Long, val testId: Long) : CompositeId

/**
 * A completed polygon result, including a result without messages.
 *
 * @since %CURRENT_VERSION%
 */
@Entity
@InternalDatabaseApi
class TestDiagnosticResultJpaEntity(id: TestDiagnosticResultId) : CompositeJpaEntity<TestDiagnosticResultId>(id)

/**
 * Stored severity of a polygon diagnostic.
 *
 * @since %CURRENT_VERSION%
 */
@InternalDatabaseApi
enum class DiagnosticSeverityJpaEnum {
    /**
     * A info message.
     *
     * @since %CURRENT_VERSION%
     */
    INFO,

    /**
     * A warning message.
     *
     * @since %CURRENT_VERSION%
     */
    WARNING,

    /**
     * A error message.
     *
     * @since %CURRENT_VERSION%
     */
    ERROR,
}

/**
 * A message of a completed polygon result.
 *
 * @property requestId the request identifier.
 * @property testId the polygon identifier.
 * @property position the message order.
 * @property severity the level.
 * @property reason the typed reason discriminator.
 * @property parameters the encoded reason parameters.
 * @property location the encoded XML location, or `null` for the whole file.
 * @since %CURRENT_VERSION%
 */
@Entity
@InternalDatabaseApi
class DiagnosticReportJpaEntity(
    val requestId: Long,
    val testId: Long,
    val position: Int,
    @field:Enumerated(EnumType.STRING)
    val severity: DiagnosticSeverityJpaEnum,
    val reason: String,
    @field:JdbcTypeCode(SqlTypes.LONG32VARCHAR)
    val parameters: String,
    @field:JdbcTypeCode(SqlTypes.LONG32VARCHAR)
    val location: String?,
    id: Long? = null,
) : SnowflakeJpaEntity(id)
