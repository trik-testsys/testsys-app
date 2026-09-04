package tech.testsys.infra.database.internal.mapping.task

import tech.testsys.domain.builder.api.contest
import tech.testsys.domain.builder.data
import tech.testsys.domain.model.group.CommunityId
import tech.testsys.domain.model.task.Contest
import tech.testsys.domain.model.task.ContestData
import tech.testsys.domain.model.task.TaskId
import tech.testsys.domain.model.task.TrikStudioVersion
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.task.CommunityToContestJpaEntity
import tech.testsys.infra.database.internal.jpa.entity.task.ContestJpaEntity
import tech.testsys.infra.database.internal.jpa.entity.task.TaskToContestJpaEntity
import tech.testsys.infra.database.internal.mapping.EntityMapping
import tech.testsys.infra.database.internal.utils.populateFields
import java.time.Duration

/**
 * Mapping between [Contest] and [ContestJpaEntity].
 *
 * @since %CURRENT_VERSION%
 */
@InternalDatabaseApi
object ContestMapping : EntityMapping<Contest, ContestJpaEntity> {

    /**
     * Assembles a [Contest] from [jpaEntity], its resolved [trikStudioVersion] and the ids of its tasks and shared-to communities.
     *
     * @since %CURRENT_VERSION%
     */
    fun toDomain(
        jpaEntity: ContestJpaEntity,
        trikStudioVersion: TrikStudioVersion,
        taskIds: List<TaskId>,
        sharedToIds: List<CommunityId>,
    ) = contest {
        populateFields(jpaEntity)
        data {
            owner(jpaEntity.ownerId)

            name = jpaEntity.name
            description = jpaEntity.description
            tasks = taskIds.toMutableList()
            startsAt = jpaEntity.startsAt
            contestDuration = Duration.ofMillis(jpaEntity.contestDurationMillis)
            attemptDuration = Duration.ofMillis(jpaEntity.attemptDurationMillis)
            this.trikStudioVersion = trikStudioVersion
            sharedTo = sharedToIds.toMutableList()
        }
    }

    /**
     * Creates a new [ContestJpaEntity] row from [data] referencing the version row [trikStudioVersionId].
     *
     * @since %CURRENT_VERSION%
     */
    fun toJpaEntity(data: ContestData, trikStudioVersionId: Long) = ContestJpaEntity(
        name = data.name,
        description = data.description,
        ownerId = data.owner.id.value,
        startsAt = data.startsAt,
        contestDurationMillis = data.contestDuration.toMillis(),
        attemptDurationMillis = data.attemptDuration.toMillis(),
        trikStudioVersionId = trikStudioVersionId,
    )

    /**
     * Creates the [ContestJpaEntity] row replacing [current] from [entity] and [trikStudioVersionId], keeping `createdAt` and `version`.
     *
     * @since %CURRENT_VERSION%
     */
    fun toJpaEntity(entity: Contest, current: ContestJpaEntity, trikStudioVersionId: Long) = ContestJpaEntity(
        name = entity.data.name,
        description = entity.data.description,
        ownerId = entity.data.owner.id.value,
        startsAt = entity.data.startsAt,
        contestDurationMillis = entity.data.contestDuration.toMillis(),
        attemptDurationMillis = entity.data.attemptDuration.toMillis(),
        trikStudioVersionId = trikStudioVersionId,
        id = entity.id.value,
    ).also {
        it.createdAt = current.createdAt
        it.version = current.version
    }

    /**
     * Creates the [TaskToContestJpaEntity] rows linking the contest [contestId] with [taskIds].
     *
     * @since %CURRENT_VERSION%
     */
    fun toTaskAssociations(contestId: Long, taskIds: List<TaskId>) = taskIds.map {
        TaskToContestJpaEntity(
            taskId = it.value,
            contestId = contestId,
        )
    }

    /**
     * Creates the [CommunityToContestJpaEntity] rows linking the contest [contestId] with [communityIds].
     *
     * @since %CURRENT_VERSION%
     */
    fun toCommunityAssociations(contestId: Long, communityIds: List<CommunityId>) = communityIds.map {
        CommunityToContestJpaEntity(
            communityId = it.value,
            contestId = contestId,
        )
    }
}
