package tech.testsys.infra.database.api.persistence.adapter.task

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import tech.testsys.domain.contract.persistence.repository.ContestRepository
import tech.testsys.domain.model.group.CommunityId
import tech.testsys.domain.model.task.Contest
import tech.testsys.domain.model.task.ContestData
import tech.testsys.domain.model.task.ContestId
import tech.testsys.domain.model.task.TaskId
import tech.testsys.domain.model.task.TrikStudioVersion
import tech.testsys.infra.database.api.persistence.adapter.AbstractPersistenceAdapter
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.task.ContestJpaEntity
import tech.testsys.infra.database.internal.jpa.repository.task.CommunityToContestJpaEntityRepository
import tech.testsys.infra.database.internal.jpa.repository.task.ContestJpaEntityRepository
import tech.testsys.infra.database.internal.jpa.repository.task.TaskToContestJpaEntityRepository
import tech.testsys.infra.database.internal.jpa.repository.task.TrikStudioVersionJpaEntityRepository
import tech.testsys.infra.database.internal.mapping.task.ContestMapping
import tech.testsys.infra.database.internal.utils.findByIdOrError
import tech.testsys.infra.database.internal.utils.findIdByTagOrError
import tech.testsys.infra.database.internal.utils.requireId
import tech.testsys.infra.database.internal.utils.syncJoinTable

/**
 * Persistence adapter of [Contest] entities backed by [ContestJpaEntity].
 * Task and shared-community membership is synced through the join tables; the TRIK Studio version must already be
 * registered by tag.
 *
 * @since %CURRENT_VERSION%
 */
@Component
@OptIn(InternalDatabaseApi::class)
class ContestPersistenceAdapter(
    jpaEntityRepository: ContestJpaEntityRepository,
    private val taskToContestJpaEntityRepository: TaskToContestJpaEntityRepository,
    private val communityToContestJpaEntityRepository: CommunityToContestJpaEntityRepository,
    private val trikStudioVersionJpaEntityRepository: TrikStudioVersionJpaEntityRepository,
) : AbstractPersistenceAdapter<ContestData, ContestId, Contest, ContestJpaEntity>(jpaEntityRepository),
    ContestRepository {

    @Transactional
    override fun save(data: ContestData): Contest {
        val trikStudioVersionId = trikStudioVersionJpaEntityRepository.findIdByTagOrError(data.trikStudioVersion.version)
        val jpaEntity = ContestMapping.toJpaEntity(data, trikStudioVersionId)
        val savedJpaEntity = jpaEntityRepository.save(jpaEntity)
        val contestId = savedJpaEntity.requireId()

        val taskAssociations = ContestMapping.toTaskAssociations(contestId, data.tasks.ids)
        val communityAssociations = ContestMapping.toCommunityAssociations(contestId, data.sharedTo.ids)

        taskToContestJpaEntityRepository.saveAll(taskAssociations)
        communityToContestJpaEntityRepository.saveAll(communityAssociations)

        val domainEntity = ContestMapping.toDomain(savedJpaEntity, data.trikStudioVersion, data.tasks.ids, data.sharedTo.ids)
        return domainEntity
    }

    @Transactional
    override fun update(entity: Contest): Contest {
        val currentJpaEntity = jpaEntityRepository.findByIdOrError(entity.id.value)
        val trikStudioVersionId = trikStudioVersionJpaEntityRepository.findIdByTagOrError(entity.data.trikStudioVersion.version)
        val updatedJpaEntity = ContestMapping.toJpaEntity(entity, currentJpaEntity, trikStudioVersionId)
        val savedJpaEntity = jpaEntityRepository.saveAndFlush(updatedJpaEntity)

        val contestId = savedJpaEntity.requireId()

        syncTasks(contestId, entity.data.tasks.ids)
        syncSharedTo(contestId, entity.data.sharedTo.ids)

        val domainEntity = ContestMapping.toDomain(
            savedJpaEntity,
            entity.data.trikStudioVersion,
            entity.data.tasks.ids,
            entity.data.sharedTo.ids,
        )
        return domainEntity
    }

    override fun assemble(jpaEntity: ContestJpaEntity): Contest {
        val contestId = jpaEntity.requireId()
        val tag = trikStudioVersionJpaEntityRepository.findByIdOrError(jpaEntity.trikStudioVersionId).tag

        val taskIds = taskToContestJpaEntityRepository.findAllByContestId(contestId).map { TaskId(it.id.taskId) }
        val sharedToIds = communityToContestJpaEntityRepository.findAllByContestId(contestId).map { CommunityId(it.id.communityId) }

        val domainEntity = ContestMapping.toDomain(jpaEntity, TrikStudioVersion(tag), taskIds, sharedToIds)
        return domainEntity
    }

    private fun syncTasks(contestId: Long, target: List<TaskId>) = syncJoinTable(
        existing = taskToContestJpaEntityRepository.findAllByContestId(contestId),
        targetKeys = target,
        keyOf = { TaskId(it.id.taskId) },
        buildAssociation = { ContestMapping.toTaskAssociations(contestId, listOf(it)).single() },
        deleteAll = { taskToContestJpaEntityRepository.deleteAll(it) },
        saveAll = { taskToContestJpaEntityRepository.saveAll(it) },
    )

    private fun syncSharedTo(contestId: Long, target: List<CommunityId>) = syncJoinTable(
        existing = communityToContestJpaEntityRepository.findAllByContestId(contestId),
        targetKeys = target,
        keyOf = { CommunityId(it.id.communityId) },
        buildAssociation = { ContestMapping.toCommunityAssociations(contestId, listOf(it)).single() },
        deleteAll = { communityToContestJpaEntityRepository.deleteAll(it) },
        saveAll = { communityToContestJpaEntityRepository.saveAll(it) },
    )
}
