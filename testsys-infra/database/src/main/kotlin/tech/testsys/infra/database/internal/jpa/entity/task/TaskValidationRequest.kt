package tech.testsys.infra.database.internal.jpa.entity.task

import jakarta.persistence.Embeddable
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import tech.testsys.infra.database.codegen.api.jpa.CompositeKeyConstructor
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.CompositeId
import tech.testsys.infra.database.internal.jpa.entity.CompositeJpaEntity
import tech.testsys.infra.database.internal.jpa.entity.SnowflakeJpaEntity
import java.time.Instant

/**
 * Stored execution stage of a task validation request.
 *
 * @since %CURRENT_VERSION%
 */
@InternalDatabaseApi
enum class TaskValidationExecutionJpaEnum {
    /**
     * The pending diagnostics stage.
     *
     * @since %CURRENT_VERSION%
     */
    PENDING_DIAGNOSTICS,

    /**
     * The diagnostics in progress stage.
     *
     * @since %CURRENT_VERSION%
     */
    DIAGNOSTICS_IN_PROGRESS,

    /**
     * The awaiting submissions stage.
     *
     * @since %CURRENT_VERSION%
     */
    AWAITING_SUBMISSIONS,

    /**
     * The stopped by diagnostics stage.
     *
     * @since %CURRENT_VERSION%
     */
    STOPPED_BY_DIAGNOSTICS,

    /**
     * The created submissions stage.
     *
     * @since %CURRENT_VERSION%
     */
    SUBMISSIONS_CREATED,

    /**
     * The technical failure stage.
     *
     * @since %CURRENT_VERSION%
     */
    TECHNICAL_FAILURE,
}

/**
 * JPA entity of a task validation request.
 *
 * @property taskId the stored taskId.
 * @property requestedById the stored requestedById.
 * @property execution the stored execution.
 * @property areDiagnosticsComplete the stored areDiagnosticsComplete.
 * @property areSubmissionsCreated the stored areSubmissionsCreated.
 * @property failureDescription the stored failureDescription.
 * @property failureOccurredAt the stored failureOccurredAt.
 * @property completedAt the stored completedAt.
 * @since %CURRENT_VERSION%
 */
@Entity
@InternalDatabaseApi
class TaskValidationRequestJpaEntity(
    val taskId: Long,
    val requestedById: Long,
    @field:Enumerated(EnumType.STRING)
    val execution: TaskValidationExecutionJpaEnum,
    val areDiagnosticsComplete: Boolean,
    val areSubmissionsCreated: Boolean,
    @field:JdbcTypeCode(SqlTypes.LONG32VARCHAR)
    val failureDescription: String?,
    val failureOccurredAt: Instant?,
    val completedAt: Instant?,
    id: Long? = null,
) : SnowflakeJpaEntity(id)

/**
 * Composite key of [TestToTaskValidationRequestJpaEntity].
 *
 * @property testId the input identifier.
 * @property requestId the request identifier.
 * @since %CURRENT_VERSION%
 */
@Embeddable
@InternalDatabaseApi
data class TestToTaskValidationRequestId(val testId: Long, val requestId: Long) : CompositeId

/**
 * Pinned test input or reference of a task validation request.
 *
 * @since %CURRENT_VERSION%
 */
@Entity
@CompositeKeyConstructor
@InternalDatabaseApi
class TestToTaskValidationRequestJpaEntity(
    id: TestToTaskValidationRequestId,
) : CompositeJpaEntity<TestToTaskValidationRequestId>(id)

/**
 * Composite key of [DeveloperSolutionToTaskValidationRequestJpaEntity].
 *
 * @property developerSolutionId the input identifier.
 * @property requestId the request identifier.
 * @since %CURRENT_VERSION%
 */
@Embeddable
@InternalDatabaseApi
data class DeveloperSolutionToTaskValidationRequestId(val developerSolutionId: Long, val requestId: Long) : CompositeId

/**
 * Pinned developersolution input or reference of a task validation request.
 *
 * @property solutionId the stored solutionId.
 * @property expectedScore the stored expectedScore.
 * @since %CURRENT_VERSION%
 */
@Entity
@InternalDatabaseApi
class DeveloperSolutionToTaskValidationRequestJpaEntity(
    id: DeveloperSolutionToTaskValidationRequestId,
    val solutionId: Long,
    val expectedScore: Int,
) : CompositeJpaEntity<DeveloperSolutionToTaskValidationRequestId>(id)

/**
 * Composite key of [TrikStudioVersionToTaskValidationRequestJpaEntity].
 *
 * @property trikStudioVersionId the input identifier.
 * @property requestId the request identifier.
 * @since %CURRENT_VERSION%
 */
@Embeddable
@InternalDatabaseApi
data class TrikStudioVersionToTaskValidationRequestId(val trikStudioVersionId: Long, val requestId: Long) : CompositeId

/**
 * Pinned trikstudioversion input or reference of a task validation request.
 *
 * @since %CURRENT_VERSION%
 */
@Entity
@CompositeKeyConstructor
@InternalDatabaseApi
class TrikStudioVersionToTaskValidationRequestJpaEntity(
    id: TrikStudioVersionToTaskValidationRequestId,
) : CompositeJpaEntity<TrikStudioVersionToTaskValidationRequestId>(id)

/**
 * Composite key of [SubmissionToTaskValidationRequestJpaEntity].
 *
 * @property submissionId the input identifier.
 * @property requestId the request identifier.
 * @since %CURRENT_VERSION%
 */
@Embeddable
@InternalDatabaseApi
data class SubmissionToTaskValidationRequestId(val submissionId: Long, val requestId: Long) : CompositeId

/**
 * Pinned submission input or reference of a task validation request.
 *
 * @since %CURRENT_VERSION%
 */
@Entity
@CompositeKeyConstructor
@InternalDatabaseApi
class SubmissionToTaskValidationRequestJpaEntity(
    id: SubmissionToTaskValidationRequestId,
) : CompositeJpaEntity<SubmissionToTaskValidationRequestId>(id)
