package tech.testsys.infra.database.jpa.entity.task

import jakarta.persistence.Embeddable
import jakarta.persistence.Entity
import tech.testsys.infra.database.jpa.entity.CompositeJpaEntity
import tech.testsys.infra.database.jpa.entity.CompositeId
import tech.testsys.infra.database.jpa.entity.SequenceJpaEntity
import java.time.Instant

/**
 * Composite primary key for [TaskToContestJpaEntity].
 *
 * @since %CURRENT_VERSION%
 */
@Embeddable
data class TaskToContestId(
    val taskId: Long,
    val contestId: Long,
) : CompositeId {

    companion object {

        private const val serialVersionUID: Long = 1L
    }
}

/**
 * JPA entity representing a task to contest association domain entity.
 *
 * @since %CURRENT_VERSION%
 */
@Entity
class TaskToContestJpaEntity(
    id: TaskToContestId,
) : CompositeJpaEntity<TaskToContestId>(id) {

    constructor(taskId: Long, contestId: Long): this(TaskToContestId(taskId, contestId))
}

@Embeddable
data class CommunityToContestId(
    val communityId: Long,
    val contestId: Long,
) : CompositeId {

    companion object {

        private const val serialVersionUID: Long = 1L
    }
}

@Entity
class CommunityToContestJpaEntity(
    id: CommunityToContestId,
) : CompositeJpaEntity<CommunityToContestId>(id) {

    constructor(communityId: Long, contestId: Long): this(CommunityToContestId(communityId, contestId))
}

/**
 * JPA entity representing a contest domain entity.
 *
 * @see tech.testsys.domain.model.task.Contest
 * @see tech.testsys.domain.model.task.ContestData
 * @since %CURRENT_VERSION%
 */
@Entity
class ContestJpaEntity(
    val name: String,
    val description: String,
    val ownerId: Long,
    val startsAt: Instant?,
    val contestDuration: Long,
    val attemptDuration: Long,
    val trikStudioVersionId: Long,
) : SequenceJpaEntity()
