package tech.testsys.infra.database.api.persistence.adapter.task

import org.springframework.data.domain.PageRequest
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import tech.testsys.domain.contract.persistence.Page
import tech.testsys.domain.contract.persistence.Pagination
import tech.testsys.domain.contract.persistence.VerdictFilter
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
import tech.testsys.infra.database.internal.utils.requireId
import org.springframework.data.domain.Sort as JpaSort

/**
 * Persistence adapter of [Verdict] entities backed by [VerdictJpaEntity].
 * The outcome of every test run is stored in its own row and dropped on remove;
 * a verdict is fixed on creation, so [update] always fails.
 *
 * @since %CURRENT_VERSION%
 */
@Component
@OptIn(InternalDatabaseApi::class)
class VerdictPersistenceAdapter(
    jpaEntityRepository: VerdictJpaEntityRepository,
    private val testVerdictJpaEntityRepository: TestVerdictJpaEntityRepository,
) : AbstractPersistenceAdapter<VerdictData, VerdictId, Verdict, VerdictJpaEntity>(jpaEntityRepository),
    VerdictRepository {

    private val verdictJpaEntityRepository: VerdictJpaEntityRepository = jpaEntityRepository

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

    @Transactional
    override fun removeById(id: VerdictId) {
        val jpaEntity = jpaEntityRepository.findByIdOrNull(id.value) ?: return
        val verdictId = jpaEntity.requireId()
        testVerdictJpaEntityRepository.deleteAll(testVerdictJpaEntityRepository.findAllByVerdictIdOrderByTestIdAsc(verdictId))
        jpaEntityRepository.delete(jpaEntity)
    }

    @Transactional
    override fun removeByIds(ids: List<VerdictId>) = ids.forEach(::removeById)

    @Transactional(readOnly = true)
    override fun findAvailableToJudge(pagination: Pagination, filter: VerdictFilter): Page<Verdict> {
        val jpaSort = JpaSort.by(
            pagination.sort.orders.map { order ->
                JpaSort.Order(JpaSort.Direction.valueOf(order.direction.name), order.field)
            },
        )
        val pageable = PageRequest.of(pagination.page, pagination.size, jpaSort)
        val page = verdictJpaEntityRepository.findAvailableToJudge(
            authorId = filter.authorId?.value,
            submissionId = filter.submissionId?.value,
            classId = filter.classId?.value,
            competitionId = filter.competitionId?.value,
            pageable = pageable,
        )
        val verdictIds = page.content.map { jpaEntity -> jpaEntity.requireId() }
        val outcomesByVerdict = if (verdictIds.isEmpty()) {
            emptyMap()
        } else {
            testVerdictJpaEntityRepository.findAllByVerdictIdInOrderByVerdictIdAscTestIdAsc(verdictIds)
                .groupBy { outcome -> outcome.verdictId }
        }
        return Page(
            content = page.content.map { jpaEntity ->
                VerdictMapping.toDomain(jpaEntity, outcomesByVerdict[jpaEntity.requireId()].orEmpty())
            },
            pagination = pagination,
            totalElements = page.totalElements,
        )
    }

    override fun assemble(jpaEntity: VerdictJpaEntity): Verdict {
        val verdictId = jpaEntity.requireId()
        val testVerdictJpaEntities = testVerdictJpaEntityRepository.findAllByVerdictIdOrderByTestIdAsc(verdictId)

        val domainEntity = VerdictMapping.toDomain(jpaEntity, testVerdictJpaEntities)
        return domainEntity
    }
}
