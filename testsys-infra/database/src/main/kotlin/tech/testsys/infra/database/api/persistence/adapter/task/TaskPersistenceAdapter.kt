package tech.testsys.infra.database.api.persistence.adapter.task

import org.springframework.data.domain.PageRequest
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import tech.testsys.domain.contract.persistence.Page
import tech.testsys.domain.contract.persistence.Pagination
import tech.testsys.domain.contract.persistence.TaskFilter
import tech.testsys.domain.contract.persistence.repository.TaskRepository
import tech.testsys.domain.model.group.CommunityId
import tech.testsys.domain.model.task.CommittedTaskContent
import tech.testsys.domain.model.task.DeveloperSolutionId
import tech.testsys.domain.model.task.ExerciseId
import tech.testsys.domain.model.task.Task
import tech.testsys.domain.model.task.TaskData
import tech.testsys.domain.model.task.TaskId
import tech.testsys.domain.model.task.TestId
import tech.testsys.domain.model.task.TrikStudioVersion
import tech.testsys.domain.model.task.VersionBucket
import tech.testsys.domain.model.task.WipTaskContent
import tech.testsys.domain.model.user.MultipleRoleUserId
import tech.testsys.infra.database.api.persistence.adapter.AbstractPersistenceAdapter
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.task.CommunityToTaskJpaEntity
import tech.testsys.infra.database.internal.jpa.entity.task.TaskJpaEntity
import tech.testsys.infra.database.internal.jpa.entity.task.TaskStatusJpaEnum
import tech.testsys.infra.database.internal.jpa.repository.task.CommunityToTaskJpaEntityRepository
import tech.testsys.infra.database.internal.jpa.repository.task.DeveloperSolutionToTaskContentJpaEntityRepository
import tech.testsys.infra.database.internal.jpa.repository.task.ExerciseToTaskContentJpaEntityRepository
import tech.testsys.infra.database.internal.jpa.repository.task.TaskContentJpaEntityRepository
import tech.testsys.infra.database.internal.jpa.repository.task.TaskJpaEntityRepository
import tech.testsys.infra.database.internal.jpa.repository.task.TestToTaskContentJpaEntityRepository
import tech.testsys.infra.database.internal.jpa.repository.task.TrikStudioVersionJpaEntityRepository
import tech.testsys.infra.database.internal.jpa.repository.task.TrikStudioVersionToTaskContentJpaEntityRepository
import tech.testsys.infra.database.internal.jpa.repository.task.VersionBucketToTaskJpaEntityRepository
import tech.testsys.infra.database.internal.mapping.task.TaskContentMapping
import tech.testsys.infra.database.internal.mapping.task.TaskContentRevision
import tech.testsys.infra.database.internal.mapping.task.TaskMapping
import tech.testsys.infra.database.internal.utils.findByIdOrError
import tech.testsys.infra.database.internal.utils.findIdByTagOrError
import tech.testsys.infra.database.internal.utils.requireId
import tech.testsys.infra.database.internal.utils.syncJoinTable
import org.springframework.data.domain.Sort as JpaSort

/**
 * Persistence adapter of [Task] entities backed by [TaskJpaEntity].
 * Content revisions are replaced wholesale as task content rows on save and update and dropped together with the
 * shared-community and uploaded-resource join rows on remove.
 *
 * @since %CURRENT_VERSION%
 */
@Component
@OptIn(InternalDatabaseApi::class)
class TaskPersistenceAdapter(
    jpaEntityRepository: TaskJpaEntityRepository,
    private val taskContentJpaEntityRepository: TaskContentJpaEntityRepository,
    private val communityToTaskJpaEntityRepository: CommunityToTaskJpaEntityRepository,
    private val versionBucketToTaskJpaEntityRepository: VersionBucketToTaskJpaEntityRepository,
    private val exerciseToTaskContentJpaEntityRepository: ExerciseToTaskContentJpaEntityRepository,
    private val testToTaskContentJpaEntityRepository: TestToTaskContentJpaEntityRepository,
    private val developerSolutionToTaskContentJpaEntityRepository: DeveloperSolutionToTaskContentJpaEntityRepository,
    private val trikStudioVersionToTaskContentJpaEntityRepository: TrikStudioVersionToTaskContentJpaEntityRepository,
    private val trikStudioVersionJpaEntityRepository: TrikStudioVersionJpaEntityRepository,
) : AbstractPersistenceAdapter<TaskData, TaskId, Task, TaskJpaEntity>(jpaEntityRepository),
    TaskRepository {

    private val taskJpaEntityRepository: TaskJpaEntityRepository = jpaEntityRepository

    @Transactional(readOnly = true)
    override fun findAvailableToDeveloper(
        ownerId: MultipleRoleUserId,
        communityIds: Set<CommunityId>,
        pagination: Pagination,
        filter: TaskFilter,
    ): Page<Task> {
        val specification = Specification<TaskJpaEntity> { entity, query, builder ->
            val owned = builder.equal(entity.get<Long>("ownerId"), ownerId.value)
            val access = if (communityIds.isEmpty()) {
                owned
            } else {
                val shared = query.subquery(Long::class.java)
                val association = shared.from(CommunityToTaskJpaEntity::class.java)
                shared.select(association.get<Any>("id").get<Long>("taskId")).where(
                    builder.equal(association.get<Any>("id").get<Long>("taskId"), entity.get<Long>("id")),
                    association.get<Any>("id").get<Long>("communityId").`in`(communityIds.map { community -> community.value }),
                )
                builder.or(owned, builder.exists(shared))
            }
            val predicates = mutableListOf(access)
            filter.name?.let { name ->
                predicates.add(builder.gt(builder.locate(builder.lower(entity.get("name")), name.lowercase()), 0))
            }
            filter.ownerId?.let { owner ->
                predicates.add(builder.equal(entity.get<Long>("ownerId"), owner.value))
            }
            filter.state?.let { state ->
                predicates.add(builder.equal(entity.get<TaskStatusJpaEnum>("status"), TaskStatusJpaEnum.valueOf(state.name)))
            }
            filter.communityId?.let { community ->
                val shared = query.subquery(Long::class.java)
                val association = shared.from(CommunityToTaskJpaEntity::class.java)
                shared.select(association.get<Any>("id").get<Long>("taskId")).where(
                    builder.equal(association.get<Any>("id").get<Long>("taskId"), entity.get<Long>("id")),
                    builder.equal(association.get<Any>("id").get<Long>("communityId"), community.value),
                )
                predicates.add(builder.exists(shared))
            }
            builder.and(*predicates.toTypedArray())
        }
        val orders = pagination.sort.orders.map { order ->
            JpaSort.Order(JpaSort.Direction.valueOf(order.direction.name), order.field)
        }
        val stableOrders = if (orders.any { order -> order.property == "id" }) orders else orders + JpaSort.Order.asc("id")
        val pageable = PageRequest.of(pagination.page, pagination.size, JpaSort.by(stableOrders))
        val page = taskJpaEntityRepository.findAll(specification, pageable)
        return Page(
            content = page.content.map { entity -> assemble(entity) },
            pagination = pagination,
            totalElements = page.totalElements,
        )
    }

    @Transactional
    override fun save(data: TaskData): Task {
        val wipContent = TaskMapping.extractWip(data.content)
        val committedContent = TaskMapping.extractCommitted(data.content)

        val (wipContentId, committedContentId) = persistContents(
            wipContent,
            committedContent,
        )

        val savedJpaEntity = jpaEntityRepository.save(TaskMapping.toJpaEntity(data, wipContentId, committedContentId))
        val taskId = savedJpaEntity.requireId()
        communityToTaskJpaEntityRepository.saveAll(TaskMapping.toSharedToAssociations(taskId, data.sharedTo.ids))
        versionBucketToTaskJpaEntityRepository.saveAll(TaskMapping.toUploadedResourceAssociations(taskId, data.uploadedResources))

        val domainEntity = TaskMapping.toDomain(savedJpaEntity, data)
        return domainEntity
    }

    @Transactional
    override fun update(entity: Task): Task {
        val currentJpaEntity = requireNotNull(taskJpaEntityRepository.findLockedById(entity.id.value)) {
            "Task id=${entity.id.value} does not exist"
        }
        val currentWipId = currentJpaEntity.wipContentId
        val currentCommittedId = currentJpaEntity.committedContentId

        val wipContent = TaskMapping.extractWip(entity.data.content)
        val committedContent = TaskMapping.extractCommitted(entity.data.content)

        val (wipContentId, committedContentId) = persistContents(
            wipContent = wipContent,
            committedContent = committedContent,
        )

        val updatedJpaEntity = TaskMapping.toJpaEntity(entity, currentJpaEntity, wipContentId, committedContentId)
        val savedJpaEntity = jpaEntityRepository.saveAndFlush(updatedJpaEntity)
        deleteContentCascade(currentWipId)
        currentCommittedId?.takeIf { it != currentWipId }?.let { deleteContentCascade(it) }
        val taskId = savedJpaEntity.requireId()
        syncSharedTo(taskId, entity.data.sharedTo.ids)
        syncUploadedResources(taskId, entity.data.uploadedResources)

        val domainEntity = TaskMapping.toDomain(savedJpaEntity, entity.data)
        return domainEntity
    }

    @Transactional
    override fun removeById(id: TaskId) {
        val jpaEntity = taskJpaEntityRepository.findLockedById(id.value) ?: return
        val taskId = jpaEntity.requireId()
        communityToTaskJpaEntityRepository.deleteAll(communityToTaskJpaEntityRepository.findAllByTaskId(taskId))
        versionBucketToTaskJpaEntityRepository.deleteAll(versionBucketToTaskJpaEntityRepository.findAllByTaskId(taskId))
        jpaEntityRepository.delete(jpaEntity)
        // Content rows are referenced by the task row, so they go after it.
        deleteContentCascade(jpaEntity.wipContentId)
        jpaEntity.committedContentId?.takeIf { it != jpaEntity.wipContentId }?.let { deleteContentCascade(it) }
    }

    @Transactional
    override fun removeByIds(ids: List<TaskId>) = ids.forEach(::removeById)

    override fun assemble(jpaEntity: TaskJpaEntity): Task {
        val taskId = jpaEntity.requireId()
        val sharedToIds = communityToTaskJpaEntityRepository.findAllByTaskId(taskId).map { CommunityId(it.id.communityId) }
        val domainEntity = TaskMapping.toDomain(
            jpaEntity = jpaEntity,
            wip = loadWipRevision(jpaEntity),
            committed = loadCommittedRevision(jpaEntity),
            sharedToIds = sharedToIds,
            uploadedResourceBuckets = versionBucketToTaskJpaEntityRepository.findAllByTaskId(taskId)
                .map { VersionBucket(it.id.versionBucket) }.toSet(),
        )
        return domainEntity
    }

    private fun loadWipRevision(jpaEntity: TaskJpaEntity): TaskContentRevision? = when (jpaEntity.status) {
        TaskStatusJpaEnum.NEW, TaskStatusJpaEnum.UNCOMMITTED -> loadRevision(jpaEntity.wipContentId)
        // A committed task has no wip: its wipContentId points at the committed row.
        TaskStatusJpaEnum.COMMITTED -> null
    }

    private fun loadCommittedRevision(jpaEntity: TaskJpaEntity): TaskContentRevision? = when (jpaEntity.status) {
        TaskStatusJpaEnum.NEW -> null
        TaskStatusJpaEnum.UNCOMMITTED, TaskStatusJpaEnum.COMMITTED -> {
            val committedId = requireNotNull(jpaEntity.committedContentId) {
                "Task ${jpaEntity.requireId()} has status=${jpaEntity.status} but committedContentId is null"
            }
            loadRevision(committedId)
        }
    }

    private fun loadRevision(taskContentId: Long): TaskContentRevision {
        val row = taskContentJpaEntityRepository.findByIdOrError(taskContentId)
        val rowId = row.requireId()
        return TaskContentRevision(
            jpaEntity = row,
            exerciseIds = loadExerciseIds(rowId),
            testIds = loadTestIds(rowId),
            developerSolutionIds = loadDeveloperSolutionIds(rowId),
            supportedVersions = loadSupportedVersions(rowId),
        )
    }

    private fun loadExerciseIds(taskContentId: Long): List<ExerciseId> =
        exerciseToTaskContentJpaEntityRepository.findAllByTaskContentId(taskContentId).map { ExerciseId(it.id.exerciseId) }

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
    private fun persistContents(wipContent: WipTaskContent?, committedContent: CommittedTaskContent?): Pair<Long, Long?> {
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
            exerciseIds = content.exercises.ids,
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
            exerciseIds = content.exercises.ids,
            testIds = content.tests.ids,
            developerSolutionIds = content.developerSolutions.ids,
            versions = content.supportedTrikStudioVersions,
        )
        return contentId
    }

    private fun persistContentAssociations(
        contentId: Long,
        exerciseIds: List<ExerciseId>,
        testIds: List<TestId>,
        developerSolutionIds: List<DeveloperSolutionId>,
        versions: List<TrikStudioVersion>,
    ) {
        exerciseToTaskContentJpaEntityRepository.saveAll(TaskContentMapping.toExerciseAssociations(contentId, exerciseIds))
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
        val exercises = exerciseToTaskContentJpaEntityRepository.findAllByTaskContentId(contentId)
        if (exercises.isNotEmpty()) exerciseToTaskContentJpaEntityRepository.deleteAll(exercises)

        val tests = testToTaskContentJpaEntityRepository.findAllByTaskContentId(contentId)
        if (tests.isNotEmpty()) testToTaskContentJpaEntityRepository.deleteAll(tests)

        val devSols = developerSolutionToTaskContentJpaEntityRepository.findAllByTaskContentId(contentId)
        if (devSols.isNotEmpty()) developerSolutionToTaskContentJpaEntityRepository.deleteAll(devSols)

        val versions = trikStudioVersionToTaskContentJpaEntityRepository.findAllByTaskContentId(contentId)
        if (versions.isNotEmpty()) trikStudioVersionToTaskContentJpaEntityRepository.deleteAll(versions)

        taskContentJpaEntityRepository.deleteById(contentId)
    }

    private fun syncUploadedResources(taskId: Long, target: Set<VersionBucket>) = syncJoinTable(
        existing = versionBucketToTaskJpaEntityRepository.findAllByTaskId(taskId),
        targetKeys = target.toList(),
        keyOf = { VersionBucket(it.id.versionBucket) },
        buildAssociation = { TaskMapping.toUploadedResourceAssociations(taskId, setOf(it)).single() },
        deleteAll = { versionBucketToTaskJpaEntityRepository.deleteAll(it) },
        saveAll = { versionBucketToTaskJpaEntityRepository.saveAll(it) },
    )

    private fun syncSharedTo(taskId: Long, target: List<CommunityId>) = syncJoinTable(
        existing = communityToTaskJpaEntityRepository.findAllByTaskId(taskId),
        targetKeys = target,
        keyOf = { CommunityId(it.id.communityId) },
        buildAssociation = { TaskMapping.toSharedToAssociations(taskId, listOf(it)).single() },
        deleteAll = { communityToTaskJpaEntityRepository.deleteAll(it) },
        saveAll = { communityToTaskJpaEntityRepository.saveAll(it) },
    )
}
