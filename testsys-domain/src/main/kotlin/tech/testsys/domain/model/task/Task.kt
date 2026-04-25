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

sealed interface TaskContent {

    data class New(
        val wip: WipTaskContent
    ) : TaskContent

    data class Uncommited(
        val wip: WipTaskContent,
        val lastCommited: CommitedTaskContent,
    ) : TaskContent

    data class Committed(
        val lastCommited: CommitedTaskContent
    ) : TaskContent
}

data class CommitedTaskContent(
    val tests: LazyEntityList<TestId, Test>,
    val exercise: LazyEntity<ExerciseId, Exercise>,
    val statement: LazyEntity<StatementId, Statement>,
    val developerSolutions: LazyEntityList<DeveloperSolutionId, DeveloperSolution>,
    val supportedTrikStudioVersions: List<TrikStudioVersion>,
)

data class WipTaskContent(
    val tests: LazyEntityList<TestId, Test>,
    val exercise: LazyEntity<ExerciseId, Exercise>?,
    val statement: LazyEntity<StatementId, Statement>?,
    val developerSolutions: LazyEntityList<DeveloperSolutionId, DeveloperSolution>,
    val supportedTrikStudioVersions: List<TrikStudioVersion>,
)

data class TaskData(
    val owner: LazyEntity<MultipleRoleUserId, MultipleRoleUser>,
    val name: String,
    val description: String,
    val sharedTo: LazyEntityList<CommunityId, Community>,
    val content: TaskContent,
)

class Task(
    id: TaskId,
    createdAt: Instant,
    val data: TaskData,
) : DomainEntity<TaskId>(id, createdAt)