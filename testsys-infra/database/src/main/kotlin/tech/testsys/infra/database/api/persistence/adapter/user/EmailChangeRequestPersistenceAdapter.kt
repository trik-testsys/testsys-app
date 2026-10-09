package tech.testsys.infra.database.api.persistence.adapter.user

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import tech.testsys.domain.contract.persistence.repository.EmailChangeRequestRepository
import tech.testsys.domain.model.user.EmailChangeRequest
import tech.testsys.domain.model.user.EmailChangeRequestData
import tech.testsys.domain.model.user.EmailChangeRequestId
import tech.testsys.domain.model.user.MultipleRoleUserId
import tech.testsys.infra.database.api.persistence.adapter.AbstractPersistenceAdapter
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.user.EmailChangeRequestJpaEntity
import tech.testsys.infra.database.internal.jpa.repository.user.EmailChangeRequestJpaEntityRepository
import tech.testsys.infra.database.internal.mapping.user.EmailChangeRequestMapping
import tech.testsys.infra.database.internal.utils.requireVersion

/**
 * Persistence adapter of [EmailChangeRequest] entities backed by [EmailChangeRequestJpaEntity].
 *
 * @since %CURRENT_VERSION%
 */
@Component
@OptIn(InternalDatabaseApi::class)
class EmailChangeRequestPersistenceAdapter(
    jpaEntityRepository: EmailChangeRequestJpaEntityRepository,
) : AbstractPersistenceAdapter<EmailChangeRequestData, EmailChangeRequestId, EmailChangeRequest, EmailChangeRequestJpaEntity>(
    jpaEntityRepository,
),
    EmailChangeRequestRepository {

    private val requests: EmailChangeRequestJpaEntityRepository = jpaEntityRepository

    @Transactional
    override fun save(data: EmailChangeRequestData): EmailChangeRequest {
        val jpaEntity = EmailChangeRequestMapping.toJpaEntity(data)
        val savedJpaEntity = jpaEntityRepository.save(jpaEntity)

        return assemble(savedJpaEntity)
    }

    @Transactional
    override fun update(entity: EmailChangeRequest): EmailChangeRequest {
        val savedJpaEntity = updateRoot(entity.id.value, entity.requireVersion()) { current ->
            EmailChangeRequestMapping.toJpaEntity(entity, current)
        }

        return assemble(savedJpaEntity)
    }

    @Transactional(readOnly = true)
    override fun findByUser(user: MultipleRoleUserId): EmailChangeRequest? = requests.findByUserId(user.value)?.let { assemble(it) }

    override fun assembleAll(rows: List<EmailChangeRequestJpaEntity>) = rows.map { row -> EmailChangeRequestMapping.toDomain(row) }
}
