package tech.testsys.infra.database.api.persistence.adapter.task

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import tech.testsys.domain.contract.persistence.repository.TaskRepository
import tech.testsys.domain.model.group.CommunityId
import tech.testsys.domain.model.task.CommitedTaskContent
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
import tech.testsys.infra.database.internal.jpa.entity.task.TrikStudioVersionJpaEntity
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
        val commitedContent = TaskMapping.extractCommited(data.content)

        val (wipContentId, commitedContentId) = persistContents(wipContent, commitedContent, currentWipId = null, currentCommitedId = null)

        val savedJpaEntity = jpaEntityRepository.save(TaskMapping.toJpaEntity(data, wipContentId, commitedContentId))
        val taskId = savedJpaEntity.requireId()
        communityToTaskJpaEntityRepository.saveAll(TaskMapping.toSharedToAssociations(taskId, data.sharedTo.ids))

        val domainEntity = TaskMapping.toDomain(savedJpaEntity, data.content, data.sharedTo.ids)
        return domainEntity
    }

    @Transactional
    override fun update(entity: Task): Task {
        val currentJpaEntity = jpaEntityRepository.findByIdOrError(entity.id.value)

        val wipContent = TaskMapping.extractWip(entity.data.content)
        val commitedContent = TaskMapping.extractCommited(entity.data.content)

        val (wipContentId, commitedContentId) = persistContents(
            wipContent = wipContent,
            commitedContent = commitedContent,
            currentWipId = currentJpaEntity.wipContentId,
            currentCommitedId = currentJpaEntity.commitedContentId,
        )

        val updatedJpaEntity = TaskMapping.toJpaEntity(entity, currentJpaEntity, wipContentId, commitedContentId)
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

            TaskStatusJpaEnum.UNCOMMITED -> {
                val wipRow = taskContentJpaEntityRepository.findByIdOrError(jpaEntity.wipContentId)
                val commitedId = requireNotNull(jpaEntity.commitedContentId) {
                    "Task ${jpaEntity.requireId()} has status=UNCOMMITED but commitedContentId is null"
                }
                val commitedRow = taskContentJpaEntityRepository.findByIdOrError(commitedId)

                val wip = TaskContentMapping.toWipDomain(
                    jpaEntity = wipRow,
                    testIds = loadTestIds(wipRow.requireId()),
                    developerSolutionIds = loadDeveloperSolutionIds(wipRow.requireId()),
                    supportedVersions = loadSupportedVersions(wipRow.requireId()),
                )
                val lastCommited = TaskContentMapping.toCommitedDomain(
                    jpaEntity = commitedRow,
                    testIds = loadTestIds(commitedRow.requireId()),
                    developerSolutionIds = loadDeveloperSolutionIds(commitedRow.requireId()),
                    supportedVersions = loadSupportedVersions(commitedRow.requireId()),
                )
                TaskContent.Uncommited(wip = wip, lastCommited = lastCommited)
            }

            TaskStatusJpaEnum.COMMITED -> {
                val commitedId = requireNotNull(jpaEntity.commitedContentId) {
                    "Task ${jpaEntity.requireId()} has status=COMMITED but commitedContentId is null"
                }
                val commitedRow = taskContentJpaEntityRepository.findByIdOrError(commitedId)
                val lastCommited = TaskContentMapping.toCommitedDomain(
                    jpaEntity = commitedRow,
                    testIds = loadTestIds(commitedRow.requireId()),
                    developerSolutionIds = loadDeveloperSolutionIds(commitedRow.requireId()),
                    supportedVersions = loadSupportedVersions(commitedRow.requireId()),
                )
                TaskContent.Committed(lastCommited)
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
     * Replaces the wip/commited content rows of a task and returns the `(wipContentId, commitedContentId)` pair;
     * for a `COMMITED` task (no wip) both point at the same row.
     */
    private fun persistContents(
        wipContent: WipTaskContent?,
        commitedContent: CommitedTaskContent?,
        currentWipId: Long?,
        currentCommitedId: Long?,
    ): Pair<Long, Long?> {
        // Delete previous content rows + their associations to keep the schema tidy.
        currentWipId?.let { deleteContentCascade(it) }
        if (currentCommitedId != null && currentCommitedId != currentWipId) {
            deleteContentCascade(currentCommitedId)
        }

        return when {
            wipContent != null && commitedContent != null -> {
                val wipId = insertContent(wipContent)
                val commitedId = insertContent(commitedContent)
                wipId to commitedId
            }

            wipContent != null && commitedContent == null -> {
                val wipId = insertContent(wipContent)
                wipId to null
            }

            wipContent == null && commitedContent != null -> {
                // COMMITED status: wip and committed reference the same row.
                val commitedId = insertContent(commitedContent)
                commitedId to commitedId
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

    private fun insertContent(content: CommitedTaskContent): Long {
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
        val versionIds = versions.map(::resolveTrikStudioVersionId)
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

    private fun resolveTrikStudioVersionId(version: TrikStudioVersion): Long {
        val existingId = trikStudioVersionJpaEntityRepository.findByTag(version.version)?.id
        if (existingId != null) return existingId
        val saved = trikStudioVersionJpaEntityRepository.save(TrikStudioVersionJpaEntity(tag = version.version))
        return saved.requireId()
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
