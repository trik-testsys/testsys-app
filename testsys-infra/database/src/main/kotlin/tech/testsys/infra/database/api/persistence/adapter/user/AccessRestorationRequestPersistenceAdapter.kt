package tech.testsys.infra.database.api.persistence.adapter.user

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import tech.testsys.domain.contract.persistence.repository.AccessRestorationRequestRepository
import tech.testsys.domain.model.user.AccessRestorationRequest
import tech.testsys.domain.model.user.AccessRestorationRequestData
import tech.testsys.domain.model.user.AccessRestorationRequestId
import tech.testsys.domain.model.user.MultipleRoleUserId
import tech.testsys.infra.database.api.persistence.adapter.AbstractPersistenceAdapter
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.user.AccessRestorationRequestJpaEntity
import tech.testsys.infra.database.internal.jpa.repository.user.AccessRestorationRequestJpaEntityRepository
import tech.testsys.infra.database.internal.mapping.user.AccessRestorationRequestMapping
import tech.testsys.infra.database.internal.utils.requireVersion

/**
 * Persistence adapter of [AccessRestorationRequest] entities backed by [AccessRestorationRequestJpaEntity].
 *
 * @since 1.0.0-beta.1
 */
@Component
@OptIn(InternalDatabaseApi::class)
class AccessRestorationRequestPersistenceAdapter(
    jpaEntityRepository: AccessRestorationRequestJpaEntityRepository,
) : AbstractPersistenceAdapter<
    AccessRestorationRequestData,
    AccessRestorationRequestId,
    AccessRestorationRequest,
    AccessRestorationRequestJpaEntity,
    >(jpaEntityRepository),
    AccessRestorationRequestRepository {

    private val requests: AccessRestorationRequestJpaEntityRepository = jpaEntityRepository

    @Transactional
    override fun save(data: AccessRestorationRequestData): AccessRestorationRequest {
        val jpaEntity = AccessRestorationRequestMapping.toJpaEntity(data)
        val savedJpaEntity = jpaEntityRepository.save(jpaEntity)

        return assemble(savedJpaEntity)
    }

    @Transactional
    override fun update(entity: AccessRestorationRequest): AccessRestorationRequest {
        val savedJpaEntity = updateRoot(entity.id.value, entity.requireVersion()) { current ->
            AccessRestorationRequestMapping.toJpaEntity(entity, current)
        }

        return assemble(savedJpaEntity)
    }

    @Transactional(readOnly = true)
    override fun findByUser(user: MultipleRoleUserId): AccessRestorationRequest? = requests.findByUserId(user.value)?.let { assemble(it) }

    override fun assembleAll(rows: List<AccessRestorationRequestJpaEntity>) =
        rows.map { row -> AccessRestorationRequestMapping.toDomain(row) }
}
