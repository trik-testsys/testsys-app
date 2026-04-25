package tech.testsys.infra.database.jpa.entity.task

import jakarta.persistence.Entity
import java.util.UUID

@Entity
class TestJpaEntity(
    name: String,
    description: String,
    fileDataId: Long,
    versionBucket: UUID,
) : ResourceJpaEntity(name, description, fileDataId, versionBucket)