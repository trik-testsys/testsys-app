package tech.testsys.infra.database.jpa.entity.task

import jakarta.persistence.Embeddable
import jakarta.persistence.Entity
import tech.testsys.infra.database.jpa.entity.JpaCompositeEntity
import tech.testsys.infra.database.jpa.entity.JpaCompositeId
import tech.testsys.infra.database.jpa.entity.JpaSequenceEntity
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
) : JpaCompositeId

/**
 * JPA entity representing a task to contest association domain entity.
 *
 * @since %CURRENT_VERSION%
 */
@Entity
class TaskToContestJpaEntity(
    id: TaskToContestId,
) : JpaCompositeEntity<TaskToContestId>(id)

/**
 * JPA entity representing a contest domain entity.
 *
 * @see tech.testsys.domain.model.task.Contest
 * @see tech.testsys.domain.model.task.ContestData
 * @since %CURRENT_VERSION%
 */
@Entity
class ContestJpaEntity(
    val ownerId: Long,
    val name: String,
    val description: String,
    val startsAt: Instant?,
    val contestDuration: Long,
    val attemptDuration: Long,
    val trikStudioVersionImage: String,
    val trikStudioVersionTag: String,
) : JpaSequenceEntity()
