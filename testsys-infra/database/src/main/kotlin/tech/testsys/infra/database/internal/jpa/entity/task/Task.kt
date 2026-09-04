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
import tech.testsys.infra.database.internal.jpa.entity.SequenceJpaEntity

/**
 * Lifecycle state of a [TaskJpaEntity]: [NEW] has no committed revision, [UNCOMMITTED] has WIP changes over the last
 * one, [COMMITTED] equals it.
 *
 * @since %CURRENT_VERSION%
 */
@InternalDatabaseApi
enum class TaskStatusJpaEnum {
    NEW,
    UNCOMMITTED,
    COMMITTED,
}

/**
 * Composite key of [TestToTaskContentJpaEntity].
 *
 * @property testId id of the test.
 * @property taskContentId id of the task content revision.
 * @since %CURRENT_VERSION%
 */
@Embeddable
@InternalDatabaseApi
data class TestToTaskContentId(
    val testId: Long,
    val taskContentId: Long,
) : CompositeId

/**
 * Join row: a [TestJpaEntity] belongs to a [TaskContentJpaEntity] revision.
 *
 * @since %CURRENT_VERSION%
 */
@Entity
@CompositeKeyConstructor
@InternalDatabaseApi
class TestToTaskContentJpaEntity(id: TestToTaskContentId) : CompositeJpaEntity<TestToTaskContentId>(id)

/**
 * Composite key of [DeveloperSolutionToTaskContentJpaEntity].
 *
 * @property developerSolutionId id of the developer solution.
 * @property taskContentId id of the task content revision.
 * @since %CURRENT_VERSION%
 */
@Embeddable
@InternalDatabaseApi
data class DeveloperSolutionToTaskContentId(
    val developerSolutionId: Long,
    val taskContentId: Long,
) : CompositeId

/**
 * Join row: a [DeveloperSolutionJpaEntity] belongs to a [TaskContentJpaEntity] revision.
 *
 * @since %CURRENT_VERSION%
 */
@Entity
@CompositeKeyConstructor
@InternalDatabaseApi
class DeveloperSolutionToTaskContentJpaEntity(id: DeveloperSolutionToTaskContentId) :
    CompositeJpaEntity<DeveloperSolutionToTaskContentId>(id)

/**
 * Composite key of [TrikStudioVersionToTaskContentJpaEntity].
 *
 * @property trikStudioVersionId id of the TRIK Studio version.
 * @property taskContentId id of the task content revision.
 * @since %CURRENT_VERSION%
 */
@Embeddable
@InternalDatabaseApi
data class TrikStudioVersionToTaskContentId(
    val trikStudioVersionId: Long,
    val taskContentId: Long,
) : CompositeId

/**
 * Join row: a [TrikStudioVersionJpaEntity] is supported by a [TaskContentJpaEntity] revision.
 *
 * @since %CURRENT_VERSION%
 */
@Entity
@CompositeKeyConstructor
@InternalDatabaseApi
class TrikStudioVersionToTaskContentJpaEntity(id: TrikStudioVersionToTaskContentId) :
    CompositeJpaEntity<TrikStudioVersionToTaskContentId>(id)

/**
 * Composite key of [CommunityToTaskJpaEntity].
 *
 * @property communityId id of the community.
 * @property taskId id of the task.
 * @since %CURRENT_VERSION%
 */
@Embeddable
@InternalDatabaseApi
data class CommunityToTaskId(
    val communityId: Long,
    val taskId: Long,
) : CompositeId

/**
 * Join row: a task is shared to a community (`TaskData.sharedTo`).
 *
 * @since %CURRENT_VERSION%
 */
@Entity
@CompositeKeyConstructor
@InternalDatabaseApi
class CommunityToTaskJpaEntity(id: CommunityToTaskId) : CompositeJpaEntity<CommunityToTaskId>(id)

/**
 * JPA entity of [tech.testsys.domain.model.task.TaskContent], one revision of a task's payload; tests, developer
 * solutions and TRIK Studio versions are attached through the `*ToTaskContentJpaEntity` join rows.
 *
 * @property exerciseId id of the [ExerciseJpaEntity], or `null` if not attached yet.
 * @property statementId id of the [StatementJpaEntity], or `null` if not attached yet.
 * @since %CURRENT_VERSION%
 */
@Entity
@InternalDatabaseApi
class TaskContentJpaEntity(
    val exerciseId: Long?,
    val statementId: Long?,
    id: Long? = null,
) : SequenceJpaEntity(id)

/**
 * JPA entity of [tech.testsys.domain.model.task.Task]; the versioned payload lives in [TaskContentJpaEntity] revisions.
 *
 * @property name the name of the task.
 * @property description the description of the task.
 * @property ownerId id of the developer owning the task.
 * @property status lifecycle state of the task.
 * @property wipContentId id of the WIP revision.
 * @property committedContentId id of the last committed revision, or `null` while [status] is [TaskStatusJpaEnum.NEW].
 * @since %CURRENT_VERSION%
 */
@Entity
@InternalDatabaseApi
class TaskJpaEntity(
    val name: String,
    @field:JdbcTypeCode(SqlTypes.LONG32VARCHAR)
    val description: String,
    val ownerId: Long,
    @Enumerated(EnumType.STRING)
    val status: TaskStatusJpaEnum,
    val wipContentId: Long,
    val committedContentId: Long?,
    id: Long? = null,
) : SequenceJpaEntity(id)
