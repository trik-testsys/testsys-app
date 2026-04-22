package tech.testsys.infra.database.jpa.entity.task

import jakarta.persistence.Entity

@Entity
class StatementJpaEntity(
    name: String,
    description: String,
    rootId: Long?,
    index: Long,
    fileDataId: Long,
) : FileJpaEntity(name, description, rootId, index, fileDataId)