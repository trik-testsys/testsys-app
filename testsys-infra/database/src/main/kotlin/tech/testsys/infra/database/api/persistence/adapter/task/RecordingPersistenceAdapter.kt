package tech.testsys.infra.database.api.persistence.adapter.task

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import tech.testsys.domain.contract.persistence.repository.RecordingRepository
import tech.testsys.domain.model.task.Recording
import tech.testsys.domain.model.task.RecordingData
import tech.testsys.domain.model.task.RecordingId
import tech.testsys.infra.database.api.persistence.FileDataStorage
import tech.testsys.infra.database.api.persistence.adapter.AbstractPersistenceAdapter
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.task.RecordingJpaEntity
import tech.testsys.infra.database.internal.jpa.repository.task.RecordingJpaEntityRepository
import tech.testsys.infra.database.internal.mapping.task.RecordingMapping
import tech.testsys.infra.database.internal.utils.findByIdOrError

@Component
@OptIn(InternalDatabaseApi::class)
class RecordingPersistenceAdapter(
    jpaEntityRepository: RecordingJpaEntityRepository,
    private val fileDataStorage: FileDataStorage,
) : AbstractPersistenceAdapter<RecordingData, RecordingId, Recording, RecordingJpaEntity>(jpaEntityRepository),
    RecordingRepository {

    @Transactional
    override fun save(data: RecordingData): Recording {
        val fileDataId = fileDataStorage.store(data.file)
        val savedJpaEntity = jpaEntityRepository.save(RecordingMapping.toJpaEntity(data, fileDataId))

        val domainEntity = RecordingMapping.toDomain(savedJpaEntity, data.file.uploadedFilename, data.file.content)
        return domainEntity
    }

    @Transactional
    override fun update(entity: Recording): Recording {
        val currentJpaEntity = jpaEntityRepository.findByIdOrError(entity.id.value)
        val newFileDataId = fileDataStorage.storeIfChanged(currentJpaEntity.fileDataId, entity.data.file)
        val updatedJpaEntity = jpaEntityRepository.save(
            RecordingMapping.toJpaEntity(entity, currentJpaEntity, newFileDataId),
        )

        val domainEntity = RecordingMapping.toDomain(
            updatedJpaEntity,
            entity.data.file.uploadedFilename,
            entity.data.file.content,
        )
        return domainEntity
    }

    override fun assemble(jpaEntity: RecordingJpaEntity): Recording {
        val file = fileDataStorage.load(jpaEntity.fileDataId)
        val domainEntity = RecordingMapping.toDomain(jpaEntity, file.uploadedFilename, file.content)
        return domainEntity
    }
}
