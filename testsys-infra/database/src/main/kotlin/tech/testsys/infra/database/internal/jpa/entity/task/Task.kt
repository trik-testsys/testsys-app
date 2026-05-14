package tech.testsys.infra.database.internal.jpa.entity.task

import jakarta.persistence.Embeddable
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import tech.testsys.infra.codegen.jpa.CompositeKeyConstructor
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.CompositeId
import tech.testsys.infra.database.internal.jpa.entity.CompositeJpaEntity
import tech.testsys.infra.database.internal.jpa.entity.SequenceJpaEntity

/**
 * Lifecycle state of a [TaskJpaEntity].
 *
 *  - [NEW]: only the WIP content exists, no committed revision yet.
 *  - [UNCOMMITED]: WIP content has unsaved changes on top of the last committed revision.
 *  - [COMMITED]: WIP content equals the last committed revision.
 *
 * @see tech.testsys.domain.model.task.TaskContent
 * @since %CURRENT_VERSION%
 */
@InternalDatabaseApi
enum class TaskStatusJpaEnum {
    NEW,
    UNCOMMITED,
    COMMITED,
}

/**
 * Composite primary key for [TestToTaskContentJpaEntity].
 *
 * @since %CURRENT_VERSION%
 */
@Embeddable
@InternalDatabaseApi
data class TestToTaskContentId(
    val testId: Long,
    val taskContentId: Long,
) : CompositeId

/**
 * JPA entity associating a polygon ([TestJpaEntity]) with a [TaskContentJpaEntity] revision.
 *
 * @since %CURRENT_VERSION%
 */
@Entity
@CompositeKeyConstructor
@InternalDatabaseApi
class TestToTaskContentJpaEntity(id: TestToTaskContentId) : CompositeJpaEntity<TestToTaskContentId>(id)

/**
 * Composite primary key for [DeveloperSolutionToTaskContentJpaEntity].
 *
 * @since %CURRENT_VERSION%
 */
@Embeddable
@InternalDatabaseApi
data class DeveloperSolutionToTaskContentId(
    val developerSolutionId: Long,
    val taskContentId: Long,
) : CompositeId

/**
 * JPA entity associating a [DeveloperSolutionJpaEntity] with a [TaskContentJpaEntity] revision.
 *
 * @since %CURRENT_VERSION%
 */
@Entity
@CompositeKeyConstructor
@InternalDatabaseApi
class DeveloperSolutionToTaskContentJpaEntity(id: DeveloperSolutionToTaskContentId) :
    CompositeJpaEntity<DeveloperSolutionToTaskContentId>(id)

/**
 * Composite primary key for [TrikStudioVersionToTaskContentJpaEntity].
 *
 * @since %CURRENT_VERSION%
 */
@Embeddable
@InternalDatabaseApi
data class TrikStudioVersionToTaskContentId(
    val trikStudioVersionId: Long,
    val taskContentId: Long,
) : CompositeId

/**
 * JPA entity associating a [TrikStudioVersionJpaEntity] supported by a [TaskContentJpaEntity] revision.
 *
 * @since %CURRENT_VERSION%
 */
@Entity
@CompositeKeyConstructor
@InternalDatabaseApi
class TrikStudioVersionToTaskContentJpaEntity(id: TrikStudioVersionToTaskContentId) :
    CompositeJpaEntity<TrikStudioVersionToTaskContentId>(id)

/**
 * Composite primary key for [CommunityToTaskJpaEntity].
 *
 * @since %CURRENT_VERSION%
 */
@Embeddable
@InternalDatabaseApi
data class CommunityToTaskId(
    val communityId: Long,
    val taskId: Long,
) : CompositeId

/**
 * JPA entity representing the share-to-community relation of a task
 * (mirrors `TaskData.sharedTo` from the domain model).
 *
 * @since %CURRENT_VERSION%
 */
@Entity
@CompositeKeyConstructor
@InternalDatabaseApi
class CommunityToTaskJpaEntity(id: CommunityToTaskId) : CompositeJpaEntity<CommunityToTaskId>(id)

/**
 * JPA entity representing a single revision of a task's contents
 * (exercise, statement, tests, developer solutions, supported TRIK Studio versions).
 *
 * Revisions are referenced from [TaskJpaEntity] as either the WIP or the last
 * committed snapshot; resource collections attached to a revision are modelled
 * by the `*ToTaskContentJpaEntity` join entities in this file.
 *
 * @see tech.testsys.domain.model.task.TaskContent
 * @see tech.testsys.domain.model.task.WipTaskContent
 * @see tech.testsys.domain.model.task.CommitedTaskContent
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
 * JPA entity representing a task domain entity.
 *
 * A task's identity (name, description, owner) lives on this entity, while the
 * versioned payload — exercise, statement, tests, developer solutions — is held
 * by [TaskContentJpaEntity] revisions referenced via [wipContentId] and [commitedContentId].
 *
 * [commitedContentId] is `null` while [status] is [TaskStatusJpaEnum.NEW] (no
 * revision has been committed yet).
 *
 * @see tech.testsys.domain.model.task.Task
 * @see tech.testsys.domain.model.task.TaskData
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
    val commitedContentId: Long?,
    id: Long? = null,
) : SequenceJpaEntity(id)
