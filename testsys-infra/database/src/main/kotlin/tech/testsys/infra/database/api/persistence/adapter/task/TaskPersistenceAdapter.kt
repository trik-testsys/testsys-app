package tech.testsys.infra.database.api.persistence.adapter.task

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import tech.testsys.domain.contract.persistence.repository.TaskRepository
import tech.testsys.domain.model.group.CommunityId
import tech.testsys.domain.model.task.CommittedTaskContent
import tech.testsys.domain.model.task.DeveloperSolutionId
import tech.testsys.domain.model.task.Task
import tech.testsys.domain.model.task.TaskContent
import tech.testsys.domain.model.task.TaskData
import tech.testsys.domain.model.task.TaskId
import tech.testsys.domain.model.task.TestId
import tech.testsys.domain.model.task.TrikStudioVersion
import tech.testsys.domain.model.task.WipTaskContent
import tech.testsys.infra.database.api.persistence.adapter.AbstractPersistenceAdapter
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.task.TaskJpaEntity
import tech.testsys.infra.database.internal.jpa.entity.task.TaskStatusJpaEnum
import tech.testsys.infra.database.internal.jpa.repository.task.CommunityToTaskJpaEntityRepository
import tech.testsys.infra.database.internal.jpa.repository.task.DeveloperSolutionToTaskContentJpaEntityRepository
import tech.testsys.infra.database.internal.jpa.repository.task.TaskContentJpaEntityRepository
import tech.testsys.infra.database.internal.jpa.repository.task.TaskJpaEntityRepository
import tech.testsys.infra.database.internal.jpa.repository.task.TestToTaskContentJpaEntityRepository
import tech.testsys.infra.database.internal.jpa.repository.task.TrikStudioVersionJpaEntityRepository
import tech.testsys.infra.database.internal.jpa.repository.task.TrikStudioVersionToTaskContentJpaEntityRepository
import tech.testsys.infra.database.internal.mapping.task.TaskContentMapping
import tech.testsys.infra.database.internal.mapping.task.TaskMapping
import tech.testsys.infra.database.internal.utils.findByIdOrError
import tech.testsys.infra.database.internal.utils.findIdByTagOrError
import tech.testsys.infra.database.internal.utils.requireId
import tech.testsys.infra.database.internal.utils.syncJoinTable

/**
 * Persistence adapter of [Task] entities backed by [TaskJpaEntity].
 * Content revisions are replaced wholesale as task content rows on save and update;
 * shared-community membership is synced through its join table.
 *
 * @since %CURRENT_VERSION%
 */
@Component
@OptIn(InternalDatabaseApi::class)
class TaskPersistenceAdapter(
    jpaEntityRepository: TaskJpaEntityRepository,
    private val taskContentJpaEntityRepository: TaskContentJpaEntityRepository,
    private val communityToTaskJpaEntityRepository: CommunityToTaskJpaEntityRepository,
    private val testToTaskContentJpaEntityRepository: TestToTaskContentJpaEntityRepository,
    private val developerSolutionToTaskContentJpaEntityRepository: DeveloperSolutionToTaskContentJpaEntityRepository,
    private val trikStudioVersionToTaskContentJpaEntityRepository: TrikStudioVersionToTaskContentJpaEntityRepository,
    private val trikStudioVersionJpaEntityRepository: TrikStudioVersionJpaEntityRepository,
) : AbstractPersistenceAdapter<TaskData, TaskId, Task, TaskJpaEntity>(jpaEntityRepository),
    TaskRepository {

    @Transactional
    override fun save(data: TaskData): Task {
        val wipContent = TaskMapping.extractWip(data.content)
        val committedContent = TaskMapping.extractCommitted(data.content)

        val (wipContentId, committedContentId) = persistContents(
            wipContent,
            committedContent,
            currentWipId = null,
            currentCommittedId = null,
        )

        val savedJpaEntity = jpaEntityRepository.save(TaskMapping.toJpaEntity(data, wipContentId, committedContentId))
        val taskId = savedJpaEntity.requireId()
        communityToTaskJpaEntityRepository.saveAll(TaskMapping.toSharedToAssociations(taskId, data.sharedTo.ids))

        val domainEntity = TaskMapping.toDomain(savedJpaEntity, data.content, data.sharedTo.ids)
        return domainEntity
    }

    @Transactional
    override fun update(entity: Task): Task {
        val currentJpaEntity = jpaEntityRepository.findByIdOrError(entity.id.value)

        val wipContent = TaskMapping.extractWip(entity.data.content)
        val committedContent = TaskMapping.extractCommitted(entity.data.content)

        val (wipContentId, committedContentId) = persistContents(
            wipContent = wipContent,
            committedContent = committedContent,
            currentWipId = currentJpaEntity.wipContentId,
            currentCommittedId = currentJpaEntity.committedContentId,
        )

        val updatedJpaEntity = TaskMapping.toJpaEntity(entity, currentJpaEntity, wipContentId, committedContentId)
        val savedJpaEntity = jpaEntityRepository.saveAndFlush(updatedJpaEntity)
        val taskId = savedJpaEntity.requireId()
        syncSharedTo(taskId, entity.data.sharedTo.ids)

        val domainEntity = TaskMapping.toDomain(savedJpaEntity, entity.data.content, entity.data.sharedTo.ids)
        return domainEntity
    }

    override fun assemble(jpaEntity: TaskJpaEntity): Task {
        val taskId = jpaEntity.requireId()
        val sharedToIds = communityToTaskJpaEntityRepository.findAllByTaskId(taskId).map { CommunityId(it.id.communityId) }
        val content = loadTaskContent(jpaEntity)
        val domainEntity = TaskMapping.toDomain(jpaEntity, content, sharedToIds)
        return domainEntity
    }

    private fun loadTaskContent(jpaEntity: TaskJpaEntity): TaskContent {
        return when (jpaEntity.status) {
            TaskStatusJpaEnum.NEW -> {
                val wipRow = taskContentJpaEntityRepository.findByIdOrError(jpaEntity.wipContentId)
                val wip = TaskContentMapping.toWipDomain(
                    jpaEntity = wipRow,
                    testIds = loadTestIds(wipRow.requireId()),
                    developerSolutionIds = loadDeveloperSolutionIds(wipRow.requireId()),
                    supportedVersions = loadSupportedVersions(wipRow.requireId()),
                )
                TaskContent.New(wip)
            }

            TaskStatusJpaEnum.UNCOMMITTED -> {
                val wipRow = taskContentJpaEntityRepository.findByIdOrError(jpaEntity.wipContentId)
                val committedId = requireNotNull(jpaEntity.committedContentId) {
                    "Task ${jpaEntity.requireId()} has status=UNCOMMITTED but committedContentId is null"
                }
                val committedRow = taskContentJpaEntityRepository.findByIdOrError(committedId)

                val wip = TaskContentMapping.toWipDomain(
                    jpaEntity = wipRow,
                    testIds = loadTestIds(wipRow.requireId()),
                    developerSolutionIds = loadDeveloperSolutionIds(wipRow.requireId()),
                    supportedVersions = loadSupportedVersions(wipRow.requireId()),
                )
                val lastCommitted = TaskContentMapping.toCommittedDomain(
                    jpaEntity = committedRow,
                    testIds = loadTestIds(committedRow.requireId()),
                    developerSolutionIds = loadDeveloperSolutionIds(committedRow.requireId()),
                    supportedVersions = loadSupportedVersions(committedRow.requireId()),
                )
                TaskContent.Uncommitted(wip = wip, lastCommitted = lastCommitted)
            }

            TaskStatusJpaEnum.COMMITTED -> {
                val committedId = requireNotNull(jpaEntity.committedContentId) {
                    "Task ${jpaEntity.requireId()} has status=COMMITTED but committedContentId is null"
                }
                val committedRow = taskContentJpaEntityRepository.findByIdOrError(committedId)
                val lastCommitted = TaskContentMapping.toCommittedDomain(
                    jpaEntity = committedRow,
                    testIds = loadTestIds(committedRow.requireId()),
                    developerSolutionIds = loadDeveloperSolutionIds(committedRow.requireId()),
                    supportedVersions = loadSupportedVersions(committedRow.requireId()),
                )
                TaskContent.Committed(lastCommitted)
            }
        }
    }

    private fun loadTestIds(taskContentId: Long): List<TestId> =
        testToTaskContentJpaEntityRepository.findAllByTaskContentId(taskContentId).map { TestId(it.id.testId) }

    private fun loadDeveloperSolutionIds(taskContentId: Long): List<DeveloperSolutionId> =
        developerSolutionToTaskContentJpaEntityRepository.findAllByTaskContentId(taskContentId)
            .map { DeveloperSolutionId(it.id.developerSolutionId) }

    private fun loadSupportedVersions(taskContentId: Long): List<TrikStudioVersion> {
        val versionIds = trikStudioVersionToTaskContentJpaEntityRepository
            .findAllByTaskContentId(taskContentId)
            .map { it.id.trikStudioVersionId }
        if (versionIds.isEmpty()) return emptyList()
        return versionIds.map {
            val row = trikStudioVersionJpaEntityRepository.findByIdOrError(it)
            TrikStudioVersion(row.tag)
        }
    }

    /**
     * Replaces the wip/committed content rows of a task and returns the `(wipContentId, committedContentId)` pair;
     * for a `COMMITTED` task (no wip) both point at the same row.
     */
    private fun persistContents(
        wipContent: WipTaskContent?,
        committedContent: CommittedTaskContent?,
        currentWipId: Long?,
        currentCommittedId: Long?,
    ): Pair<Long, Long?> {
        // Delete previous content rows + their associations to keep the schema tidy.
        currentWipId?.let { deleteContentCascade(it) }
        if (currentCommittedId != null && currentCommittedId != currentWipId) {
            deleteContentCascade(currentCommittedId)
        }

        return when {
            wipContent != null && committedContent != null -> {
                val wipId = insertContent(wipContent)
                val committedId = insertContent(committedContent)
                wipId to committedId
            }

            wipContent != null && committedContent == null -> {
                val wipId = insertContent(wipContent)
                wipId to null
            }

            wipContent == null && committedContent != null -> {
                // COMMITTED status: wip and committed reference the same row.
                val committedId = insertContent(committedContent)
                committedId to committedId
            }

            else -> error("TaskContent must declare either a wip, a committed payload, or both")
        }
    }

    private fun insertContent(content: WipTaskContent): Long {
        val saved = taskContentJpaEntityRepository.save(TaskContentMapping.toJpaEntity(content))
        val contentId = saved.requireId()
        persistContentAssociations(
            contentId = contentId,
            testIds = content.tests.ids,
            developerSolutionIds = content.developerSolutions.ids,
            versions = content.supportedTrikStudioVersions,
        )
        return contentId
    }

    private fun insertContent(content: CommittedTaskContent): Long {
        val saved = taskContentJpaEntityRepository.save(TaskContentMapping.toJpaEntity(content))
        val contentId = saved.requireId()
        persistContentAssociations(
            contentId = contentId,
            testIds = content.tests.ids,
            developerSolutionIds = content.developerSolutions.ids,
            versions = content.supportedTrikStudioVersions,
        )
        return contentId
    }

    private fun persistContentAssociations(
        contentId: Long,
        testIds: List<TestId>,
        developerSolutionIds: List<DeveloperSolutionId>,
        versions: List<TrikStudioVersion>,
    ) {
        testToTaskContentJpaEntityRepository.saveAll(TaskContentMapping.toTestAssociations(contentId, testIds))
        developerSolutionToTaskContentJpaEntityRepository.saveAll(
            TaskContentMapping.toDeveloperSolutionAssociations(contentId, developerSolutionIds),
        )
        val versionIds = versions.map { trikStudioVersionJpaEntityRepository.findIdByTagOrError(it.version) }
        trikStudioVersionToTaskContentJpaEntityRepository.saveAll(
            TaskContentMapping.toTrikStudioVersionAssociations(contentId, versionIds),
        )
    }

    private fun deleteContentCascade(contentId: Long) {
        val tests = testToTaskContentJpaEntityRepository.findAllByTaskContentId(contentId)
        if (tests.isNotEmpty()) testToTaskContentJpaEntityRepository.deleteAll(tests)

        val devSols = developerSolutionToTaskContentJpaEntityRepository.findAllByTaskContentId(contentId)
        if (devSols.isNotEmpty()) developerSolutionToTaskContentJpaEntityRepository.deleteAll(devSols)

        val versions = trikStudioVersionToTaskContentJpaEntityRepository.findAllByTaskContentId(contentId)
        if (versions.isNotEmpty()) trikStudioVersionToTaskContentJpaEntityRepository.deleteAll(versions)

        taskContentJpaEntityRepository.deleteById(contentId)
    }

    private fun syncSharedTo(taskId: Long, target: List<CommunityId>) = syncJoinTable(
        existing = communityToTaskJpaEntityRepository.findAllByTaskId(taskId),
        targetKeys = target,
        keyOf = { CommunityId(it.id.communityId) },
        buildAssociation = { TaskMapping.toSharedToAssociations(taskId, listOf(it)).single() },
        deleteAll = { communityToTaskJpaEntityRepository.deleteAll(it) },
        saveAll = { communityToTaskJpaEntityRepository.saveAll(it) },
    )
}
