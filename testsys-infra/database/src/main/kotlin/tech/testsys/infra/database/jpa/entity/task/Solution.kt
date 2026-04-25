package tech.testsys.infra.database.jpa.entity.task

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import java.util.UUID

@Entity
class SolutionJpaEntity(
    name: String,
    description: String,
    fileDataId: Long,
    versionBucket: UUID,
    @Enumerated(EnumType.STRING)
    val language: TrikSupportedLanguageEnum,
) : ResourceJpaEntity(name, description, fileDataId, versionBucket)
