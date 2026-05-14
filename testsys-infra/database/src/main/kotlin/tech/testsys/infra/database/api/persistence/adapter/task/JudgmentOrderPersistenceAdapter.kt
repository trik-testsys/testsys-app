package tech.testsys.infra.database.api.persistence.adapter.task

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import tech.testsys.domain.contract.persistence.repository.JudgmentOrderRepository
import tech.testsys.domain.model.task.JudgmentOrder
import tech.testsys.domain.model.task.JudgmentOrderData
import tech.testsys.domain.model.task.JudgmentOrderId
import tech.testsys.infra.database.api.persistence.adapter.AbstractPersistenceAdapter
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.task.JudgmentOrderJpaEntity
import tech.testsys.infra.database.internal.jpa.repository.task.JudgmentOrderJpaEntityRepository
import tech.testsys.infra.database.internal.mapping.task.JudgmentOrderMapping
import tech.testsys.infra.database.internal.utils.findByIdOrError

@Component
@OptIn(InternalDatabaseApi::class)
class JudgmentOrderPersistenceAdapter(
    jpaEntityRepository: JudgmentOrderJpaEntityRepository,
) : AbstractPersistenceAdapter<JudgmentOrderData, JudgmentOrderId, JudgmentOrder, JudgmentOrderJpaEntity>(jpaEntityRepository),
    JudgmentOrderRepository {

    @Transactional
    override fun save(data: JudgmentOrderData) =
        JudgmentOrderMapping.toDomain(jpaEntityRepository.save(JudgmentOrderMapping.toJpaEntity(data)))

    @Transactional
    override fun update(entity: JudgmentOrder): JudgmentOrder {
        val currentJpaEntity = jpaEntityRepository.findByIdOrError(entity.id.value)
        val updatedJpaEntity = JudgmentOrderMapping.toJpaEntity(entity, currentJpaEntity)
        val savedJpaEntity = jpaEntityRepository.save(updatedJpaEntity)

        val domainEntity = JudgmentOrderMapping.toDomain(savedJpaEntity)
        return domainEntity
    }

    override fun assemble(jpaEntity: JudgmentOrderJpaEntity) = JudgmentOrderMapping.toDomain(jpaEntity)
}
