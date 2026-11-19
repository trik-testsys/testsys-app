package tech.testsys.infra.database.api.persistence.adapter.task

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import tech.testsys.domain.contract.persistence.repository.VerdictRepository
import tech.testsys.domain.model.task.Verdict
import tech.testsys.domain.model.task.VerdictData
import tech.testsys.domain.model.task.VerdictId
import tech.testsys.infra.database.api.persistence.adapter.AbstractPersistenceAdapter
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.task.VerdictJpaEntity
import tech.testsys.infra.database.internal.jpa.repository.task.TestVerdictJpaEntityRepository
import tech.testsys.infra.database.internal.jpa.repository.task.VerdictJpaEntityRepository
import tech.testsys.infra.database.internal.mapping.task.VerdictMapping
import tech.testsys.infra.database.internal.utils.findAllInChunks
import tech.testsys.infra.database.internal.utils.requireId

/**
 * Persistence adapter of [Verdict] entities backed by [VerdictJpaEntity].
 * The outcome of every test run is stored in its own row and dropped on remove after the verdict version is
 * incremented; a verdict is fixed on creation, so [update] always fails.
 *
 * @since 1.0.0-beta.1
 */
@Component
@OptIn(InternalDatabaseApi::class)
class VerdictPersistenceAdapter(
    jpaEntityRepository: VerdictJpaEntityRepository,
    private val testVerdictJpaEntityRepository: TestVerdictJpaEntityRepository,
) : AbstractPersistenceAdapter<VerdictData, VerdictId, Verdict, VerdictJpaEntity>(jpaEntityRepository),
    VerdictRepository {

    @Transactional
    override fun save(data: VerdictData): Verdict {
        val savedJpaEntity = jpaEntityRepository.save(VerdictMapping.toJpaEntity(data))
        val verdictId = savedJpaEntity.requireId()

        val testVerdictAssociations = VerdictMapping.toTestVerdictAssociations(verdictId, data.testVerdicts)
        val savedTestVerdicts = testVerdictJpaEntityRepository.saveAll(testVerdictAssociations)

        val domainEntity = VerdictMapping.toDomain(savedJpaEntity, savedTestVerdicts.sortedBy { it.testId })
        return domainEntity
    }

    override fun update(entity: Verdict): Verdict = throw UnsupportedOperationException(
        "verdict ${entity.id.value} cannot be updated: every field of a verdict is fixed on creation",
    )

    override fun removeRoot(id: VerdictId, expectedVersion: Long?) {
        jpaEntityRepository.findByIdOrNull(id.value) ?: return
        val jpaEntity = touchRoot(jpaEntityRepository, id.value, expectedVersion, changesRootData = true)
        val verdictId = jpaEntity.requireId()
        testVerdictJpaEntityRepository.deleteAll(testVerdictJpaEntityRepository.findAllByVerdictIdOrderByTestIdAsc(verdictId))
        jpaEntityRepository.delete(jpaEntity)
    }

    override fun assembleAll(rows: List<VerdictJpaEntity>): List<Verdict> {
        val outcomesByVerdict = findAllInChunks(
            ids = rows.map { row -> row.requireId() },
            find = testVerdictJpaEntityRepository::findAllByVerdictIdInOrderByVerdictIdAscTestIdAsc,
        ).groupBy { outcome -> outcome.verdictId }

        return rows.map { row -> VerdictMapping.toDomain(row, outcomesByVerdict[row.requireId()].orEmpty()) }
    }
}
