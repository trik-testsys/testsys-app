package tech.testsys.domain.model.task

import tech.testsys.domain.model.DomainEntity
import tech.testsys.domain.model.DomainId
import tech.testsys.domain.model.LazyEntity
import tech.testsys.domain.model.LazyEntityList
import tech.testsys.domain.model.group.Community
import tech.testsys.domain.model.group.CommunityId
import tech.testsys.domain.model.user.MultipleRoleUser
import tech.testsys.domain.model.user.MultipleRoleUserId
import java.time.Instant

@JvmInline
value class TaskId(
    override val value: Long,
) : DomainId

sealed interface TaskData {

    data class New(
        val wip: TaskContent
    ) : TaskData

    data class Uncommited(
        val wip: TaskContent,
        val lastCommited: TaskContent,
    ) : TaskData

    data class Committed(
        val lastCommited: TaskContent
    ) : TaskData
}

data class TaskContent(
    val owner: LazyEntity<MultipleRoleUserId, MultipleRoleUser>,
    val name: String,
    val description: String,
    val tests: LazyEntityList<TestId, Test>,
    val exercise: LazyEntity<ExerciseId, Exercise>,
    val statement: LazyEntity<StatementId, Statement>,
    val developerSolutions: LazyEntityList<DeveloperSolutionId, DeveloperSolution>,
    val supportedTrikStudioVersions: List<TrikStudioVersion>,
    val sharedTo: LazyEntityList<CommunityId, Community>
)

class Task(
    id: TaskId,
    createdAt: Instant,
    val data: TaskData,
) : DomainEntity<TaskId>(id, createdAt)
