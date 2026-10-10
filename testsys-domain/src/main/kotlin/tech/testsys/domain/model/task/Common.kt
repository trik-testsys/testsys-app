package tech.testsys.domain.model.task

import tech.testsys.domain.contract.StoredBlobRef
import java.util.UUID

/**
 * Globally unique identifier shared by all versions of one logical resource, across all resource types.
 *
 * @property value the raw identifier of the version chain.
 * @since %CURRENT_VERSION%
 */
@JvmInline
value class VersionBucket(
    val value: UUID,
)

/**
 * Programming language of a TRIK Studio program.
 *
 * @since %CURRENT_VERSION%
 */
sealed interface TrikSupportedLanguage {
    /**
     * Python.
     *
     * @since %CURRENT_VERSION%
     */
    object Python : TrikSupportedLanguage

    /**
     * JavaScript.
     *
     * @since %CURRENT_VERSION%
     */
    object JavaScript : TrikSupportedLanguage

    /**
     * The visual (diagram-based) language of TRIK Studio.
     *
     * @since %CURRENT_VERSION%
     */
    object VisualLanguage : TrikSupportedLanguage
}

/**
 * Score awarded to a solution.
 *
 * @property value the raw score.
 * @since %CURRENT_VERSION%
 */
@JvmInline
value class Score(
    val value: Int,
)

/**
 * Version tag of TRIK Studio, e.g. `"3.0.0"`.
 *
 * @property version the raw version string.
 * @since %CURRENT_VERSION%
 */
@JvmInline
value class TrikStudioVersion(
    val version: String,
)

/**
 * An uploaded file, never changed once stored: a changed file is stored as a new one. Not a data class: equality is
 * reference-based.
 *
 * @property uploadedFilename the original name of the file as it entered the system.
 * @property content the inline bytes or stored reference of the file.
 * @since %CURRENT_VERSION%
 */
class FileData(
    val uploadedFilename: String,
    val content: FileContent,
) {
    /**
     * Creates a file with inline [content].
     *
     * @since %CURRENT_VERSION%
     */
    constructor(uploadedFilename: String, content: ByteArray) : this(uploadedFilename, FileContent.Inline(content))
}

/**
 * Binary content held in memory or referenced in file storage.
 *
 * @since %CURRENT_VERSION%
 */
sealed interface FileContent {
    /**
     * Content supplied in memory.
     *
     * @property bytes the raw bytes.
     * @since %CURRENT_VERSION%
     */
    class Inline(val bytes: ByteArray) : FileContent

    /**
     * Content kept in file storage, without a transaction or entity dependency.
     *
     * @property ref the opaque blob reference.
     * @property kind the storage directory kind.
     * @since %CURRENT_VERSION%
     */
    data class Stored(val ref: StoredBlobRef, val kind: FileStorageKind) : FileContent
}

/**
 * Storage directory kind of a file.
 *
 * @since %CURRENT_VERSION%
 */
enum class FileStorageKind {
    /**
     * Statement files.
     *
     * @since %CURRENT_VERSION%
     */
    Statement,

    /**
     * Exercise files.
     *
     * @since %CURRENT_VERSION%
     */
    Exercise,

    /**
     * Test files.
     *
     * @since %CURRENT_VERSION%
     */
    Test,

    /**
     * Solution files.
     *
     * @since %CURRENT_VERSION%
     */
    Solution,

    /**
     * Recording files.
     *
     * @since %CURRENT_VERSION%
     */
    Recording,

    /**
     * Logs files.
     *
     * @since %CURRENT_VERSION%
     */
    Logs,
}
