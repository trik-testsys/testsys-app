package tech.testsys.domain.model.task

import tech.testsys.domain.model.DomainEntity
import tech.testsys.domain.model.DomainId
import tech.testsys.domain.model.LazyEntityList
import tech.testsys.domain.model.group.Community
import tech.testsys.domain.model.group.CommunityId
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
    val createdAt: Instant,
    val trikStudioVersion: TrikStudioVersion,
    val sharedTo: LazyEntityList<CommunityId, Community>
) {
    val endsAt = startsAt?.let { it + contestDuration }
}

class Contest(
    id: ContestId,
    createdAt: Instant,
    val data: ContestData,
) : DomainEntity<ContestId>(id, createdAt)
