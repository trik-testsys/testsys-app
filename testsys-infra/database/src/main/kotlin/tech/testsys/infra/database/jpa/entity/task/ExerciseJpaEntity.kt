package tech.testsys.infra.database.jpa.entity.task

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated

@Entity
class ExerciseJpaEntity(
    name: String,
    description: String,
    rootId: Long?,
    index: Long,
    fileDataId: Long,
    @Enumerated(EnumType.STRING)
    val language: TrikSupportedLanguageEnum,
) : FileJpaEntity(name, description, rootId, index, fileDataId)
