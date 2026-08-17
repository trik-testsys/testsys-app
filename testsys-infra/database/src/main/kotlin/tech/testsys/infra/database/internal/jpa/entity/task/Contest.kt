package tech.testsys.infra.database.internal.jpa.entity.task

import jakarta.persistence.Embeddable
import jakarta.persistence.Entity
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import tech.testsys.infra.database.codegen.api.jpa.CompositeKeyConstructor
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.CompositeId
import tech.testsys.infra.database.internal.jpa.entity.CompositeJpaEntity
import tech.testsys.infra.database.internal.jpa.entity.SequenceJpaEntity
import java.time.Instant

/**
 * Composite primary key for [TaskToContestJpaEntity].
 *
 * @since %CURRENT_VERSION%
 */
@Embeddable
@InternalDatabaseApi
data class TaskToContestId(
    val taskId: Long,
    val contestId: Long,
) : CompositeId

/**
 * JPA entity representing a task to contest association domain entity.
 *
 * @since %CURRENT_VERSION%
 */
@Entity
@CompositeKeyConstructor
@InternalDatabaseApi
class TaskToContestJpaEntity(id: TaskToContestId) : CompositeJpaEntity<TaskToContestId>(id)

/**
 * Composite primary key for [CommunityToContestJpaEntity].
 *
 * @since %CURRENT_VERSION%
 */
@Embeddable
@InternalDatabaseApi
data class CommunityToContestId(
    val communityId: Long,
    val contestId: Long,
) : CompositeId

/**
 * JPA entity representing the share-to-community relation of a contest
 * (mirrors `ContestData.sharedTo` from the domain model).
 *
 * @since %CURRENT_VERSION%
 */
@Entity
@CompositeKeyConstructor
@InternalDatabaseApi
class CommunityToContestJpaEntity(id: CommunityToContestId) : CompositeJpaEntity<CommunityToContestId>(id)

/**
 * JPA entity representing a contest domain entity.
 *
 * @property contestDurationMillis total contest duration in milliseconds.
 * @property attemptDurationMillis per-attempt duration in milliseconds.
 *
 * @see tech.testsys.domain.model.task.Contest
 * @see tech.testsys.domain.model.task.ContestData
 * @since %CURRENT_VERSION%
 */
@Entity
@InternalDatabaseApi
class ContestJpaEntity(
    val name: String,
    @field:JdbcTypeCode(SqlTypes.LONG32VARCHAR)
    val description: String,
    val ownerId: Long,
    val startsAt: Instant?,
    val contestDurationMillis: Long,
    val attemptDurationMillis: Long,
    val trikStudioVersionId: Long,
    id: Long? = null,
) : SequenceJpaEntity(id)
