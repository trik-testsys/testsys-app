package tech.testsys.infra.database.api.persistence.adapter.task

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import tech.testsys.domain.contract.persistence.repository.SubmissionRepository
import tech.testsys.domain.model.task.JudgmentOrderId
import tech.testsys.domain.model.task.Submission
import tech.testsys.domain.model.task.SubmissionData
import tech.testsys.domain.model.task.SubmissionId
import tech.testsys.infra.database.api.persistence.adapter.AbstractPersistenceAdapter
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.task.SubmissionJpaEntity
import tech.testsys.infra.database.internal.jpa.repository.task.JudgmentOrderJpaEntityRepository
import tech.testsys.infra.database.internal.jpa.repository.task.SubmissionJpaEntityRepository
import tech.testsys.infra.database.internal.mapping.task.SubmissionMapping
import tech.testsys.infra.database.internal.utils.findByIdOrError
import tech.testsys.infra.database.internal.utils.requireId

/**
 * Persistence adapter of [Submission] entities backed by [SubmissionJpaEntity].
 * Judgment order ids are projected from the judgment order table on read.
 *
 * @since %CURRENT_VERSION%
 */
@Component
@OptIn(InternalDatabaseApi::class)
class SubmissionPersistenceAdapter(
    jpaEntityRepository: SubmissionJpaEntityRepository,
    private val judgmentOrderJpaEntityRepository: JudgmentOrderJpaEntityRepository,
) : AbstractPersistenceAdapter<SubmissionData, SubmissionId, Submission, SubmissionJpaEntity>(jpaEntityRepository),
    SubmissionRepository {

    @Transactional
    override fun save(data: SubmissionData): Submission {
        val jpaEntity = SubmissionMapping.toJpaEntity(data)
        val savedJpaEntity = jpaEntityRepository.save(jpaEntity)

        val domainEntity = SubmissionMapping.toDomain(
            jpaEntity = savedJpaEntity,
            judgmentOrderIds = emptyList(),
        )
        return domainEntity
    }

    @Transactional
    override fun update(entity: Submission): Submission {
        val currentJpaEntity = jpaEntityRepository.findByIdOrError(entity.id.value)
        val updatedJpaEntity = SubmissionMapping.toJpaEntity(entity, currentJpaEntity)
        val savedJpaEntity = jpaEntityRepository.saveAndFlush(updatedJpaEntity)

        val submissionId = savedJpaEntity.requireId()
        val judgmentOrderIds = judgmentOrderJpaEntityRepository.findAllBySubmissionId(submissionId)
            .map { JudgmentOrderId(it.requireId()) }

        val domainEntity = SubmissionMapping.toDomain(savedJpaEntity, judgmentOrderIds)
        return domainEntity
    }

    override fun assemble(jpaEntity: SubmissionJpaEntity): Submission {
        val submissionId = jpaEntity.requireId()
        val judgmentOrderIds = judgmentOrderJpaEntityRepository.findAllBySubmissionId(submissionId)
            .map { JudgmentOrderId(it.requireId()) }

        val domainEntity = SubmissionMapping.toDomain(jpaEntity, judgmentOrderIds)
        return domainEntity
    }
}
