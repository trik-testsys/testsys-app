package tech.testsys.infra.database.jpa.entity.task

import jakarta.persistence.Entity
import jakarta.persistence.MappedSuperclass
import tech.testsys.infra.database.jpa.entity.SequenceJpaEntity
import java.util.UUID

/**
 * Programming languages supported by the TRIK Studio runtime for solutions and exercises.
 *
 * @see tech.testsys.domain.model.task.TrikSupportedLanguage
 * @since %CURRENT_VERSION%
 */
enum class TrikSupportedLanguageEnum {

    PYTHON,
    JAVA_SCRIPT,
    VISUAL_LANGUAGE
}

/**
 * JPA entity representing a stored file domain entity.
 *
 * Holds the original (uploaded) name and the storage-side name; binary content
 * itself lives outside the database.
 *
 * @see tech.testsys.domain.model.task.FileData
 * @since %CURRENT_VERSION%
 */
@Entity
class FileDataJpaEntity(
    val uploadedFileName: String,
    val storedFileName: String,
) : SequenceJpaEntity()

/**
 * JPA entity representing a TRIK Studio version domain entity.
 *
 * @see tech.testsys.domain.model.task.TrikStudioVersion
 * @since %CURRENT_VERSION%
 */
@Entity
class TrikStudioVersionJpaEntity(
    val tag: String,
) : SequenceJpaEntity()

/**
 * Base class for task resources backed by a stored file: tests, statements,
 * exercises, solutions and developer solutions.
 *
 * Resources sharing the same logical identity across versions carry a common
 * [versionBucket] UUID, which lets the persistence layer group historical
 * revisions of the same logical resource together.
 *
 * @property name human-readable resource name.
 * @property description human-readable resource description.
 * @property fileDataId reference to the [FileDataJpaEntity] holding the binary content.
 * @property versionBucket logical identity shared by all versions of this resource.
 * @since %CURRENT_VERSION%
 */
@MappedSuperclass
abstract class ResourceJpaEntity(
    val name: String,
    val description: String,
    val fileDataId: Long,
    val versionBucket: UUID,
) : SequenceJpaEntity()
