package tech.testsys.infra.database.internal.mapping.task

import tech.testsys.domain.builder.api.task
import tech.testsys.domain.builder.util.lazify
import tech.testsys.domain.model.group.CommunityId
import tech.testsys.domain.model.task.CommitedTaskContent
import tech.testsys.domain.model.task.Task
import tech.testsys.domain.model.task.TaskContent
import tech.testsys.domain.model.task.TaskData
import tech.testsys.domain.model.task.WipTaskContent
import tech.testsys.domain.model.user.MultipleRoleUserId
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.task.CommunityToTaskJpaEntity
import tech.testsys.infra.database.internal.jpa.entity.task.TaskJpaEntity
import tech.testsys.infra.database.internal.jpa.entity.task.TaskStatusJpaEnum
import tech.testsys.infra.database.internal.utils.populateFields

/**
 * Mapping between [Task] and [TaskJpaEntity]; content revisions are mapped by [TaskContentMapping].
 *
 * @since %CURRENT_VERSION%
 */
@InternalDatabaseApi
object TaskMapping {

    /**
     * Assembles a [Task] from [jpaEntity], its mapped [content] and [sharedToIds].
     *
     * @since %CURRENT_VERSION%
     */
    fun toDomain(jpaEntity: TaskJpaEntity, content: TaskContent, sharedToIds: List<CommunityId>): Task {
        val builtData = TaskData(
            owner = MultipleRoleUserId(jpaEntity.ownerId).lazify(),
            name = jpaEntity.name,
            description = jpaEntity.description,
            sharedTo = sharedToIds.lazify(),
            content = content,
        )
        return task {
            populateFields(jpaEntity)
            data = builtData
        }
    }

    /**
     * Creates a new [TaskJpaEntity] row from [data] and its content revisions [wipContentId] and [commitedContentId].
     *
     * @since %CURRENT_VERSION%
     */
    fun toJpaEntity(data: TaskData, wipContentId: Long, commitedContentId: Long?) = TaskJpaEntity(
        name = data.name,
        description = data.description,
        ownerId = data.owner.id.value,
        status = encodeStatus(data.content),
        wipContentId = wipContentId,
        commitedContentId = commitedContentId,
    )

    /**
     * Creates the [TaskJpaEntity] row replacing [current] from [entity], [wipContentId] and [commitedContentId];
     * keeps `createdAt` and `version`.
     *
     * @since %CURRENT_VERSION%
     */
    fun toJpaEntity(entity: Task, current: TaskJpaEntity, wipContentId: Long, commitedContentId: Long?) = TaskJpaEntity(
        name = entity.data.name,
        description = entity.data.description,
        ownerId = entity.data.owner.id.value,
        status = encodeStatus(entity.data.content),
        wipContentId = wipContentId,
        commitedContentId = commitedContentId,
        id = entity.id.value,
    ).also {
        it.createdAt = current.createdAt
        it.version = entity.version.value
    }

    /**
     * Creates the [CommunityToTaskJpaEntity] rows linking the task [taskId] with [communityIds].
     *
     * @since %CURRENT_VERSION%
     */
    fun toSharedToAssociations(taskId: Long, communityIds: List<CommunityId>) = communityIds.map {
        CommunityToTaskJpaEntity(communityId = it.value, taskId = taskId)
    }

    /**
     * Maps the sealed variant of [content] to its [TaskStatusJpaEnum] discriminator.
     *
     * @since %CURRENT_VERSION%
     */
    fun encodeStatus(content: TaskContent): TaskStatusJpaEnum = when (content) {
        is TaskContent.New -> TaskStatusJpaEnum.NEW
        is TaskContent.Uncommited -> TaskStatusJpaEnum.UNCOMMITED
        is TaskContent.Committed -> TaskStatusJpaEnum.COMMITED
    }

    /**
     * Returns the WIP content of [content], or `null` for a committed task.
     *
     * @since %CURRENT_VERSION%
     */
    fun extractWip(content: TaskContent): WipTaskContent? = when (content) {
        is TaskContent.New -> content.wip
        is TaskContent.Uncommited -> content.wip
        is TaskContent.Committed -> null
    }

    /**
     * Returns the last committed content of [content], or `null` for a new task.
     *
     * @since %CURRENT_VERSION%
     */
    fun extractCommited(content: TaskContent): CommitedTaskContent? = when (content) {
        is TaskContent.New -> null
        is TaskContent.Uncommited -> content.lastCommited
        is TaskContent.Committed -> content.lastCommited
    }
}
