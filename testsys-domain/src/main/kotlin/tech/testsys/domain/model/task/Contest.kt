package tech.testsys.domain.model.task

import tech.testsys.domain.model.DomainEntity
import tech.testsys.domain.model.DomainId
import tech.testsys.domain.model.LazyEntityList
import tech.testsys.domain.model.user.MultipleRoleUser
import java.time.Duration
import java.time.Instant

@JvmInline
value class ContestId(
    override val value: Long
) : DomainId

data class ContestData(
    val owner: MultipleRoleUser,
    val name: String,
    val description: String,
    val tasks: LazyEntityList<TaskId, Task>,
    val tasksOrder: List<TaskId>,
    val startsAt: Instant?,
    val contestDuration: Duration,
    val attemptDuration: Duration,
    val trikStudioVersion: TrikStudioVersion
    // TODO: val sharedTo: List<>
) {
    val endsAt = startsAt?.let { it + contestDuration }
}

class Contest(
    id: ContestId,
    val data: ContestData,
    createdAt: Instant = Instant.now(),
) : DomainEntity<ContestId>(id, createdAt)
