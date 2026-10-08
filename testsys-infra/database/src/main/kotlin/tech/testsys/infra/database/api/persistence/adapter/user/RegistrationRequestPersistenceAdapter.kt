package tech.testsys.infra.database.api.persistence.adapter.user

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import tech.testsys.domain.contract.persistence.repository.RegistrationRequestRepository
import tech.testsys.domain.model.user.RegistrationRequest
import tech.testsys.domain.model.user.RegistrationRequestData
import tech.testsys.domain.model.user.RegistrationRequestId
import tech.testsys.infra.database.api.persistence.adapter.AbstractPersistenceAdapter
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.user.RegistrationRequestJpaEntity
import tech.testsys.infra.database.internal.jpa.repository.user.RegistrationRequestJpaEntityRepository
import tech.testsys.infra.database.internal.mapping.user.RegistrationRequestMapping
import tech.testsys.infra.database.internal.utils.findByIdOrError

/**
 * Persistence adapter of [RegistrationRequest] entities backed by [RegistrationRequestJpaEntity].
 *
 * @since %CURRENT_VERSION%
 */
@Component
@OptIn(InternalDatabaseApi::class)
class RegistrationRequestPersistenceAdapter(
    jpaEntityRepository: RegistrationRequestJpaEntityRepository,
) : AbstractPersistenceAdapter<RegistrationRequestData, RegistrationRequestId, RegistrationRequest, RegistrationRequestJpaEntity>(
    jpaEntityRepository,
),
    RegistrationRequestRepository {

    private val requests: RegistrationRequestJpaEntityRepository = jpaEntityRepository

    @Transactional
    override fun save(data: RegistrationRequestData): RegistrationRequest {
        val jpaEntity = RegistrationRequestMapping.toJpaEntity(data)
        val savedJpaEntity = jpaEntityRepository.save(jpaEntity)

        return assemble(savedJpaEntity)
    }

    @Transactional
    override fun update(entity: RegistrationRequest): RegistrationRequest {
        val currentJpaEntity = jpaEntityRepository.findByIdOrError(entity.id.value)
        val updatedJpaEntity = RegistrationRequestMapping.toJpaEntity(entity, currentJpaEntity)
        val savedJpaEntity = jpaEntityRepository.saveAndFlush(updatedJpaEntity)

        return assemble(savedJpaEntity)
    }

    @Transactional(readOnly = true)
    override fun findByEmail(email: String): RegistrationRequest? = requests.findByEmail(email)?.let { assemble(it) }

    override fun assembleAll(rows: List<RegistrationRequestJpaEntity>) = rows.map { row -> RegistrationRequestMapping.toDomain(row) }
}
