package tech.testsys.infra.database.api.persistence.adapter.task

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import tech.testsys.domain.contract.persistence.repository.RegradeRepository
import tech.testsys.domain.model.task.Regrade
import tech.testsys.domain.model.task.RegradeData
import tech.testsys.domain.model.task.RegradeId
import tech.testsys.domain.model.task.SubmissionId
import tech.testsys.domain.model.task.TaskId
import tech.testsys.domain.model.task.TestId
import tech.testsys.infra.database.api.persistence.adapter.AbstractPersistenceAdapter
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.task.RegradeJpaEntity
import tech.testsys.infra.database.internal.jpa.repository.LinkedIdRow
import tech.testsys.infra.database.internal.jpa.repository.task.RegradeJpaEntityRepository
import tech.testsys.infra.database.internal.jpa.repository.task.SubmissionToRegradeJpaEntityRepository
import tech.testsys.infra.database.internal.jpa.repository.task.TestToRegradeJpaEntityRepository
import tech.testsys.infra.database.internal.mapping.task.RegradeMapping
import tech.testsys.infra.database.internal.utils.findAllInChunks
import tech.testsys.infra.database.internal.utils.findLinkedIds
import tech.testsys.infra.database.internal.utils.requireId

/**
 * Persistence adapter of [Regrade] entities backed by [RegradeJpaEntity].
 * Tests and submissions are stored in join tables on save and dropped on remove after the regrade version is
 * incremented; a regrade is fixed on creation, so [update] always fails.
 *
 * @since %CURRENT_VERSION%
 */
@Component
@OptIn(InternalDatabaseApi::class)
class RegradePersistenceAdapter(
    jpaEntityRepository: RegradeJpaEntityRepository,
    private val testToRegradeJpaEntityRepository: TestToRegradeJpaEntityRepository,
    private val submissionToRegradeJpaEntityRepository: SubmissionToRegradeJpaEntityRepository,
) : AbstractPersistenceAdapter<RegradeData, RegradeId, Regrade, RegradeJpaEntity>(jpaEntityRepository),
    RegradeRepository {

    private val regradeJpaEntityRepository: RegradeJpaEntityRepository = jpaEntityRepository

    @Transactional(readOnly = true)
    override fun findByTaskId(taskId: TaskId): List<Regrade> =
        assembleAll(regradeJpaEntityRepository.findAllByTaskIdOrderByCreatedAtAscIdAsc(taskId.value))

    @Transactional(readOnly = true)
    override fun countGraded(regradeIds: Set<RegradeId>): Map<RegradeId, Int> {
        if (regradeIds.isEmpty()) return emptyMap()
        return findAllInChunks(ids = regradeIds.map { regrade -> regrade.value }, find = regradeJpaEntityRepository::countGraded)
            .associate { row -> RegradeId(row.regradeId) to Math.toIntExact(row.graded) }
    }

    @Transactional
    override fun save(data: RegradeData): Regrade {
        val savedJpaEntity = jpaEntityRepository.save(RegradeMapping.toJpaEntity(data))
        val regradeId = savedJpaEntity.requireId()

        testToRegradeJpaEntityRepository.saveAll(RegradeMapping.toTestAssociations(regradeId, data.tests.ids))
        submissionToRegradeJpaEntityRepository.saveAll(RegradeMapping.toSubmissionAssociations(regradeId, data.submissions.ids))

        return RegradeMapping.toDomain(savedJpaEntity, data.tests.ids, data.submissions.ids)
    }

    override fun update(entity: Regrade): Regrade = throw UnsupportedOperationException(
        "regrade ${entity.id.value} cannot be updated: every field of a regrade is fixed on creation",
    )

    override fun removeRoot(id: RegradeId, expectedVersion: Long?) {
        jpaEntityRepository.findByIdOrNull(id.value) ?: return
        val jpaEntity = touchRoot(jpaEntityRepository, id.value, expectedVersion, changesRootData = true)
        val regradeId = jpaEntity.requireId()
        testToRegradeJpaEntityRepository.deleteAll(testToRegradeJpaEntityRepository.findAllByRegradeId(regradeId))
        submissionToRegradeJpaEntityRepository.deleteAll(submissionToRegradeJpaEntityRepository.findAllByRegradeId(regradeId))
        jpaEntityRepository.delete(jpaEntity)
    }

    override fun assembleAll(rows: List<RegradeJpaEntity>): List<Regrade> {
        val regradeIds = rows.map { row -> row.requireId() }
        val testIds = findLinkedIds(
            ownerIds = regradeIds,
            find = testToRegradeJpaEntityRepository::findLinkedIdsByRegradeIdIn,
            ownerIdOf = LinkedIdRow::ownerId,
            linkedIdOf = { link -> TestId(link.linkedId) },
        )
        val submissionIds = findLinkedIds(
            ownerIds = regradeIds,
            find = submissionToRegradeJpaEntityRepository::findLinkedIdsByRegradeIdIn,
            ownerIdOf = LinkedIdRow::ownerId,
            linkedIdOf = { link -> SubmissionId(link.linkedId) },
        )

        return rows.map { row ->
            val regradeId = row.requireId()
            RegradeMapping.toDomain(
                jpaEntity = row,
                testIds = testIds.getValue(regradeId).sortedBy { test -> test.value },
                submissionIds = submissionIds.getValue(regradeId).sortedBy { submission -> submission.value },
            )
        }
    }
}
