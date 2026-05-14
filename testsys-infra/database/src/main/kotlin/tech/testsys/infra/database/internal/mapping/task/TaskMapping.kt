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

@InternalDatabaseApi
object TaskMapping {

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

    fun toJpaEntity(data: TaskData, wipContentId: Long, commitedContentId: Long?) = TaskJpaEntity(
        name = data.name,
        description = data.description,
        ownerId = data.owner.id.value,
        status = encodeStatus(data.content),
        wipContentId = wipContentId,
        commitedContentId = commitedContentId,
    )

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
        it.version = current.version
    }

    fun toSharedToAssociations(taskId: Long, communityIds: List<CommunityId>) = communityIds.map {
        CommunityToTaskJpaEntity(communityId = it.value, taskId = taskId)
    }

    fun encodeStatus(content: TaskContent): TaskStatusJpaEnum = when (content) {
        is TaskContent.New -> TaskStatusJpaEnum.NEW
        is TaskContent.Uncommited -> TaskStatusJpaEnum.UNCOMMITED
        is TaskContent.Committed -> TaskStatusJpaEnum.COMMITED
    }

    fun extractWip(content: TaskContent): WipTaskContent? = when (content) {
        is TaskContent.New -> content.wip
        is TaskContent.Uncommited -> content.wip
        is TaskContent.Committed -> null
    }

    fun extractCommited(content: TaskContent): CommitedTaskContent? = when (content) {
        is TaskContent.New -> null
        is TaskContent.Uncommited -> content.lastCommited
        is TaskContent.Committed -> content.lastCommited
    }
}
