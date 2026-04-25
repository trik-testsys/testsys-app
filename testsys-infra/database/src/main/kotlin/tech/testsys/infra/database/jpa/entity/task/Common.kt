package tech.testsys.infra.database.jpa.entity.task

import jakarta.persistence.Entity
import jakarta.persistence.MappedSuperclass
import tech.testsys.infra.database.jpa.entity.SequenceJpaEntity
import java.util.UUID

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

@MappedSuperclass
abstract class ResourceJpaEntity(
    val name: String,
    val description: String,
    val fileDataId: Long,
    val versionBucket: UUID,
) : SequenceJpaEntity()