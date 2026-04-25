package tech.testsys.infra.database.jpa.entity.task

import jakarta.persistence.Entity
import java.util.UUID

@Entity
class DeveloperSolutionJpaEntity(
    name: String,
    description: String,
    fileDataId: Long,
    versionBucket: UUID,
    val expectedScore: Int,
) : ResourceJpaEntity(name, description, fileDataId, versionBucket)
