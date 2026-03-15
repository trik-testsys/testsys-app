package tech.testsys.infra.database.jpa.entity.task

import jakarta.persistence.Embeddable
import jakarta.persistence.Entity
import tech.testsys.infra.database.jpa.entity.JpaCompositeEntity
import tech.testsys.infra.database.jpa.entity.JpaCompositeId
import tech.testsys.infra.database.jpa.entity.JpaEntity
import java.time.Instant

@Embeddable
data class TaskToContestId(
    val taskId: Long,
    val contestId: Long,
) : JpaCompositeId()

@Entity
class TaskToContestJpaEntity(
    id: TaskToContestId,
) : JpaCompositeEntity<TaskToContestId>(id)

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
) : JpaEntity()