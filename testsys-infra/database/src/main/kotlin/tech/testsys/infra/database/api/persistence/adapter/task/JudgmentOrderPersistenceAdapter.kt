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
import tech.testsys.infra.database.internal.utils.requireVersion

/**
 * Persistence adapter of [JudgmentOrder] entities backed by [JudgmentOrderJpaEntity].
 *
 * @since %CURRENT_VERSION%
 */
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
        val savedJpaEntity = updateRoot(entity.id.value, entity.requireVersion()) { current ->
            JudgmentOrderMapping.toJpaEntity(entity, current)
        }

        val domainEntity = JudgmentOrderMapping.toDomain(savedJpaEntity)
        return domainEntity
    }

    override fun assembleAll(rows: List<JudgmentOrderJpaEntity>) = rows.map { row -> JudgmentOrderMapping.toDomain(row) }
}
