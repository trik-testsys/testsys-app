package tech.testsys.infra.database.jpa.entity.task

import jakarta.persistence.Entity

@Entity
class DeveloperSolutionJpaEntity(
    name: String,
    description: String,
    rootId: Long?,
    index: Long,
    fileDataId: Long,
    val expectedScore: Int,
) : FileJpaEntity(name, description, rootId, index, fileDataId)