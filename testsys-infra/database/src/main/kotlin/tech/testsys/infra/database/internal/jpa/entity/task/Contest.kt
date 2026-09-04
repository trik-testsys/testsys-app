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
 * Composite key of [TaskToContestJpaEntity].
 *
 * @property taskId id of the task.
 * @property contestId id of the contest.
 * @since %CURRENT_VERSION%
 */
@Embeddable
@InternalDatabaseApi
data class TaskToContestId(
    val taskId: Long,
    val contestId: Long,
) : CompositeId

/**
 * Join row: a task belongs to a contest.
 *
 * @since %CURRENT_VERSION%
 */
@Entity
@CompositeKeyConstructor
@InternalDatabaseApi
class TaskToContestJpaEntity(id: TaskToContestId) : CompositeJpaEntity<TaskToContestId>(id)

/**
 * Composite key of [CommunityToContestJpaEntity].
 *
 * @property communityId id of the community.
 * @property contestId id of the contest.
 * @since %CURRENT_VERSION%
 */
@Embeddable
@InternalDatabaseApi
data class CommunityToContestId(
    val communityId: Long,
    val contestId: Long,
) : CompositeId

/**
 * Join row: a contest is shared to a community (`ContestData.sharedTo`).
 *
 * @since %CURRENT_VERSION%
 */
@Entity
@CompositeKeyConstructor
@InternalDatabaseApi
class CommunityToContestJpaEntity(id: CommunityToContestId) : CompositeJpaEntity<CommunityToContestId>(id)

/**
 * JPA entity of [tech.testsys.domain.model.task.Contest].
 *
 * @property name the name of the contest.
 * @property description the description of the contest.
 * @property ownerId id of the developer owning the contest.
 * @property startsAt moment the contest starts, or `null` if not scheduled yet.
 * @property contestDurationMillis total duration in milliseconds.
 * @property attemptDurationMillis per-attempt duration in milliseconds.
 * @property trikStudioVersionId id of the [TrikStudioVersionJpaEntity] the contest runs on.
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
