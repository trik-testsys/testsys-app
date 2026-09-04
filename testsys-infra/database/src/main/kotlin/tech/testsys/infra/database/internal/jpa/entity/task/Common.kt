package tech.testsys.infra.database.internal.jpa.entity.task

import jakarta.persistence.Entity
import jakarta.persistence.MappedSuperclass
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.SnowflakeJpaEntity
import java.util.UUID

/**
 * Column form of [tech.testsys.domain.model.task.TrikSupportedLanguage].
 *
 * @since %CURRENT_VERSION%
 */
@InternalDatabaseApi
enum class TrikSupportedLanguageEnum {

    PYTHON,
    JAVA_SCRIPT,
    VISUAL_LANGUAGE,
}

/**
 * JPA entity of [tech.testsys.domain.model.task.FileData]; the binary content lives in external storage.
 *
 * @property uploadedFileName the name the file was uploaded with.
 * @property storedFileName the key of the blob in the external storage.
 * @property versionBucket identity shared by all versions of the file (that of the owning resource, if any).
 * @property contentHash SHA-256 hex digest of the content, used to detect changes without loading the blob.
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
) : SnowflakeJpaEntity(id)

/**
 * JPA entity of [tech.testsys.domain.model.task.TrikStudioVersion].
 *
 * @property tag the version tag.
 * @since %CURRENT_VERSION%
 */
@Entity
@InternalDatabaseApi
class TrikStudioVersionJpaEntity(
    val tag: String,
) : SnowflakeJpaEntity()

/**
 * Base of task resources backed by a [FileDataJpaEntity].
 *
 * @property name the name of the resource.
 * @property description the description of the resource.
 * @property fileDataId id of the [FileDataJpaEntity] holding the content.
 * @property versionBucket identity shared by all versions of the resource.
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
) : SnowflakeJpaEntity(id)
