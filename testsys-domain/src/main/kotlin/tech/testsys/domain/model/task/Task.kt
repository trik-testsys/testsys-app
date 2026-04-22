package tech.testsys.domain.model.task

import tech.testsys.domain.model.Describable
import tech.testsys.domain.model.DomainEntity
import tech.testsys.domain.model.DomainId
import tech.testsys.domain.model.LazyEntity
import tech.testsys.domain.model.LazyEntityList
import tech.testsys.domain.model.Sharable
import tech.testsys.domain.model.group.Community
import tech.testsys.domain.model.group.CommunityId
import tech.testsys.domain.model.user.MultipleRoleUser
import tech.testsys.domain.model.user.MultipleRoleUserId
import java.time.Instant

@JvmInline
value class TaskId(
    override val value: Long,
) : DomainId

sealed interface TaskData : Describable, Sharable {

    data class New(
        val wip: WipTaskContent
    ) : TaskData {

        override val name = wip.name
        override val description = wip.description
        override val sharedTo = wip.sharedTo
    }

    data class Uncommited(
        val wip: WipTaskContent,
        val lastCommited: CommitedTaskContent,
    ) : TaskData {

        override val name = wip.name
        override val description = wip.description
        override val sharedTo = wip.sharedTo
    }

    data class Committed(
        val lastCommited: CommitedTaskContent
    ) : TaskData {

        override val name = lastCommited.name
        override val description = lastCommited.description
        override val sharedTo = lastCommited.sharedTo
    }

    companion object
}

data class CommitedTaskContent(
    val owner: LazyEntity<MultipleRoleUserId, MultipleRoleUser>,
    val tests: LazyEntityList<TestId, Test>,
    val exercise: LazyEntity<ExerciseId, Exercise>,
    val statement: LazyEntity<StatementId, Statement>,
    val developerSolutions: LazyEntityList<DeveloperSolutionId, DeveloperSolution>,
    val supportedTrikStudioVersions: List<TrikStudioVersion>,
    val sharedTo: LazyEntityList<CommunityId, Community>,
    val name: String,
    val description: String,
)

data class WipTaskContent(
    val owner: LazyEntity<MultipleRoleUserId, MultipleRoleUser>, // TODO может стоит вынести в другую дату поля, которые не меняются между стейтами таски
    val tests: LazyEntityList<TestId, Test>,
    val exercise: LazyEntity<ExerciseId, Exercise>?,
    val statement: LazyEntity<StatementId, Statement>?,
    val developerSolutions: LazyEntityList<DeveloperSolutionId, DeveloperSolution>,
    val supportedTrikStudioVersions: List<TrikStudioVersion>,
    val sharedTo: LazyEntityList<CommunityId, Community>, // TODO может стоит вынести в общую дату поля, которые не меняются между стейтами таски
    val name: String, // TODO мб добавить опциональное поле previousName, чтобы после переименовывания было понятней (или прошлое название могут указать в скобках?)
    val description: String?, // TODO либо убираем здесь nullable либо в других аналогичных местах его добавляем
)

class Task(
    id: TaskId,
    createdAt: Instant,
    val data: TaskData,
) : DomainEntity<TaskId>(id, createdAt),
    Describable, Sharable {

    override val name = data.name
    override val description = data.description
    override val sharedTo = data.sharedTo
}
