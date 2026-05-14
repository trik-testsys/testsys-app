package tech.testsys.infra.database.internal.jpa.entity.task

import jakarta.persistence.Entity
import jakarta.persistence.MappedSuperclass
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.SequenceJpaEntity
import java.util.UUID

/**
 * Programming languages supported by the TRIK Studio runtime for solutions and exercises.
 *
 * @see tech.testsys.domain.model.task.TrikSupportedLanguage
 * @since %CURRENT_VERSION%
 */
@InternalDatabaseApi
enum class TrikSupportedLanguageEnum {

    PYTHON,
    JAVA_SCRIPT,
    VISUAL_LANGUAGE,
}

/**
 * JPA entity representing a stored file domain entity.
 *
 * Holds the original (uploaded) name and the storage-side name; binary content
 * itself lives outside the database.
 *
 * Rows representing versions of the same logical file share a common
 * [versionBucket] UUID: files of a versioned resource carry the resource's own
 * version bucket, unversioned files get a fresh bucket on every store.
 *
 * @property versionBucket logical identity shared by all versions of this file.
 * @property contentHash SHA-256 hex digest of the binary content, used to detect
 * whether an incoming file differs from the stored one without loading the blob.
 * @see tech.testsys.domain.model.task.FileData
 * @since %CURRENT_VERSION%
 */
@Entity
@InternalDatabaseApi
class FileDataJpaEntity(
    val uploadedFileName: String,
    val storedFileName: String,
    val versionBucket: UUID,
    val contentHash: String,
    id: Long? = null,
) : SequenceJpaEntity(id)

/**
 * JPA entity representing a TRIK Studio version domain entity.
 *
 * @see tech.testsys.domain.model.task.TrikStudioVersion
 * @since %CURRENT_VERSION%
 */
@Entity
@InternalDatabaseApi
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
@InternalDatabaseApi
abstract class ResourceJpaEntity(
    val name: String,
    @field:JdbcTypeCode(SqlTypes.LONG32VARCHAR)
    val description: String,
    val fileDataId: Long,
    val versionBucket: UUID,
    id: Long? = null,
) : SequenceJpaEntity(id)
