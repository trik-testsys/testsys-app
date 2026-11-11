package tech.testsys.infra.database.internal.jpa.entity.task

import jakarta.persistence.Embeddable
import jakarta.persistence.Entity
import tech.testsys.infra.database.codegen.api.jpa.CompositeKeyConstructor
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.CompositeId
import tech.testsys.infra.database.internal.jpa.entity.CompositeJpaEntity
import tech.testsys.infra.database.internal.jpa.entity.SnowflakeJpaEntity

/**
 * Composite key of [TestToRegradeJpaEntity].
 *
 * @property regradeId id of the regrade.
 * @property testId id of the test.
 * @since 1.0.0-beta.1
 */
@Embeddable
@InternalDatabaseApi
data class TestToRegradeId(
    val regradeId: Long,
    val testId: Long,
) : CompositeId

/**
 * Join row: a test belongs to the committed revision of a regrade (`RegradeData.tests`).
 *
 * @since 1.0.0-beta.1
 */
@Entity
@CompositeKeyConstructor
@InternalDatabaseApi
class TestToRegradeJpaEntity(id: TestToRegradeId) : CompositeJpaEntity<TestToRegradeId>(id)

/**
 * Composite key of [SubmissionToRegradeJpaEntity].
 *
 * @property regradeId id of the regrade.
 * @property submissionId id of the submission.
 * @since 1.0.0-beta.1
 */
@Embeddable
@InternalDatabaseApi
data class SubmissionToRegradeId(
    val regradeId: Long,
    val submissionId: Long,
) : CompositeId

/**
 * Join row: a submission was sent to grading again by a regrade (`RegradeData.submissions`).
 *
 * @since 1.0.0-beta.1
 */
@Entity
@CompositeKeyConstructor
@InternalDatabaseApi
class SubmissionToRegradeJpaEntity(id: SubmissionToRegradeId) : CompositeJpaEntity<SubmissionToRegradeId>(id)

/**
 * JPA entity of [tech.testsys.domain.model.task.Regrade].
 *
 * @property taskId id of the regraded task.
 * @since 1.0.0-beta.1
 */
@Entity
@InternalDatabaseApi
class RegradeJpaEntity(
    val taskId: Long,
    id: Long? = null,
) : SnowflakeJpaEntity(id)
