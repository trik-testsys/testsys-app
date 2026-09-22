package tech.testsys.infra.database.internal.mapping.task

import tech.testsys.domain.builder.api.task
import tech.testsys.domain.builder.api.withData
import tech.testsys.domain.builder.data
import tech.testsys.domain.builder.util.chooser.TaskContentChooser
import tech.testsys.domain.model.group.CommunityId
import tech.testsys.domain.model.task.CommittedTaskContent
import tech.testsys.domain.model.task.Task
import tech.testsys.domain.model.task.TaskContent
import tech.testsys.domain.model.task.TaskData
import tech.testsys.domain.model.task.WipTaskContent
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.task.CommunityToTaskJpaEntity
import tech.testsys.infra.database.internal.jpa.entity.task.TaskJpaEntity
import tech.testsys.infra.database.internal.jpa.entity.task.TaskStatusJpaEnum
import tech.testsys.infra.database.internal.mapping.EntityMapping
import tech.testsys.infra.database.internal.utils.populateFields
import tech.testsys.infra.database.internal.utils.requireVersion

/**
 * Mapping between [Task] and [TaskJpaEntity]; content revisions are mapped by [TaskContentMapping].
 *
 * @since %CURRENT_VERSION%
 */
@InternalDatabaseApi
object TaskMapping : EntityMapping<Task, TaskJpaEntity> {

    /**
     * Assembles a [Task] from [jpaEntity], its stored content revisions [wip] and [committed] and [sharedToIds];
     * fails when a revision required by the status of [jpaEntity] is missing.
     *
     * @since %CURRENT_VERSION%
     */
    fun toDomain(
        jpaEntity: TaskJpaEntity,
        wip: TaskContentRevision?,
        committed: TaskContentRevision?,
        sharedToIds: List<CommunityId>,
    ): Task = task {
        populateFields(jpaEntity)
        data {
            owner(jpaEntity.ownerId)
            name = jpaEntity.name
            description = jpaEntity.description
            sharedTo = sharedToIds.toMutableList()

            content.decodeContent(jpaEntity, wip, committed)
        }
    }

    /**
     * Assembles a [Task] from the just stored [jpaEntity] and the [data] it was stored from; the owner comes from the row.
     *
     * @since %CURRENT_VERSION%
     */
    fun toDomain(jpaEntity: TaskJpaEntity, data: TaskData): Task = task {
        populateFields(jpaEntity)
        this.data = data
    }.withData { owner(jpaEntity.ownerId) }

    /**
     * Creates a new [TaskJpaEntity] row from [data] and its content revisions [wipContentId] and [committedContentId].
     *
     * @since %CURRENT_VERSION%
     */
    fun toJpaEntity(data: TaskData, wipContentId: Long, committedContentId: Long?) = TaskJpaEntity(
        name = data.name,
        description = data.description,
        ownerId = data.owner.id.value,
        status = encodeStatus(data.content),
        wipContentId = wipContentId,
        committedContentId = committedContentId,
    )

    /**
     * Creates the [TaskJpaEntity] row replacing [current] from [entity], [wipContentId] and [committedContentId];
     * keeps `ownerId`, `createdAt` and `version`.
     *
     * @since %CURRENT_VERSION%
     */
    fun toJpaEntity(entity: Task, current: TaskJpaEntity, wipContentId: Long, committedContentId: Long?) = TaskJpaEntity(
        name = entity.data.name,
        description = entity.data.description,
        ownerId = current.ownerId,
        status = encodeStatus(entity.data.content),
        wipContentId = wipContentId,
        committedContentId = committedContentId,
        id = entity.id.value,
    ).also {
        it.createdAt = current.createdAt
        it.version = entity.requireVersion()
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
        is TaskContent.Uncommitted -> TaskStatusJpaEnum.UNCOMMITTED
        is TaskContent.Committed -> TaskStatusJpaEnum.COMMITTED
    }

    /**
     * Returns the WIP content of [content], or `null` for a committed task.
     *
     * @since %CURRENT_VERSION%
     */
    fun extractWip(content: TaskContent): WipTaskContent? = when (content) {
        is TaskContent.New -> content.wip
        is TaskContent.Uncommitted -> content.wip
        is TaskContent.Committed -> null
    }

    /**
     * Returns the last committed content of [content], or `null` for a new task.
     *
     * @since %CURRENT_VERSION%
     */
    fun extractCommitted(content: TaskContent): CommittedTaskContent? = when (content) {
        is TaskContent.New -> null
        is TaskContent.Uncommitted -> content.lastCommitted
        is TaskContent.Committed -> content.lastCommitted
    }

    private fun TaskContentChooser.decodeContent(jpaEntity: TaskJpaEntity, wip: TaskContentRevision?, committed: TaskContentRevision?) {
        when (jpaEntity.status) {
            TaskStatusJpaEnum.NEW -> {
                val wipRevision = requireRevision(wip, jpaEntity, revisionName = "wip")
                new { TaskContentMapping.populateWip(builder = this, revision = wipRevision) }
            }

            TaskStatusJpaEnum.UNCOMMITTED -> {
                val wipRevision = requireRevision(wip, jpaEntity, revisionName = "wip")
                val committedRevision = requireRevision(committed, jpaEntity, revisionName = "committed")
                uncommitted(
                    wipBuilder = { TaskContentMapping.populateWip(builder = this, revision = wipRevision) },
                    lastCommittedBuilder = {
                        TaskContentMapping.populateCommitted(builder = this, revision = committedRevision)
                    },
                )
            }

            TaskStatusJpaEnum.COMMITTED -> {
                val committedRevision = requireRevision(committed, jpaEntity, revisionName = "committed")
                committed { TaskContentMapping.populateCommitted(builder = this, revision = committedRevision) }
            }
        }
    }

    private fun requireRevision(revision: TaskContentRevision?, jpaEntity: TaskJpaEntity, revisionName: String): TaskContentRevision {
        return requireNotNull(revision) {
            "Task ${jpaEntity.id} has status=${jpaEntity.status} but its $revisionName content revision is missing"
        }
    }
}
