package tech.testsys.infra.database.jpa.entity.task

import jakarta.persistence.Entity
import jakarta.persistence.MappedSuperclass
import tech.testsys.infra.database.jpa.entity.DescribableJpaEntity
import tech.testsys.infra.database.jpa.entity.SequenceJpaEntity

enum class TrikSupportedLanguageEnum {
    PYTHON,
    JAVA_SCRIPT,
    VISUAL_LANGUAGE
}

@Entity
class FileDataJpaEntity(
    val uploadedFileName: String,
    val storedFileName: String,
) : SequenceJpaEntity()

@Entity
class TrikStudioVersion(
    val tag: String,
) : SequenceJpaEntity()

interface Versioned {

    val rootId: Long?
    val index: Long
}

@MappedSuperclass
abstract class FileJpaEntity(
    name: String,
    description: String,
    override val rootId: Long?,
    override val index: Long,
    val fileDataId: Long,
) : DescribableJpaEntity(name, description),
    Versioned