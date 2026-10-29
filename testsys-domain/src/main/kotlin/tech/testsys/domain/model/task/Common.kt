package tech.testsys.domain.model.task

import tech.testsys.domain.contract.StoredBlobRef
import java.util.UUID

/**
 * Globally unique identifier shared by all versions of one logical resource, across all resource types.
 *
 * @property value the raw identifier of the version chain.
 * @since 1.0.0-beta.1
 */
@JvmInline
value class VersionBucket(
    val value: UUID,
)

/**
 * Programming language of a TRIK Studio program.
 *
 * @since 1.0.0-beta.1
 */
sealed interface TrikSupportedLanguage {
    /**
     * Python.
     *
     * @since 1.0.0-beta.1
     */
    object Python : TrikSupportedLanguage

    /**
     * JavaScript.
     *
     * @since 1.0.0-beta.1
     */
    object JavaScript : TrikSupportedLanguage

    /**
     * The visual (diagram-based) language of TRIK Studio.
     *
     * @since 1.0.0-beta.1
     */
    object VisualLanguage : TrikSupportedLanguage
}

/**
 * Score awarded to a solution.
 *
 * @property value the raw score.
 * @since 1.0.0-beta.1
 */
@JvmInline
value class Score(
    val value: Int,
)

/**
 * Registered TRIK Studio version: a Docker image tag that grading nodes run and a version number shown to people.
 *
 * @property tag the Docker image tag, e.g. `"trikset/trik-studio:2026.2"`.
 * @property version the version number, e.g. `"2026.2"`.
 * @since 1.0.0-beta.1
 */
data class TrikStudioVersion(
    val tag: String,
    val version: String,
)

/**
 * An uploaded file, never changed once stored: a changed file is stored as a new one. Not a data class: equality is
 * reference-based.
 *
 * @property uploadedFilename the original name of the file as it entered the system.
 * @property content the inline bytes or stored reference of the file.
 * @since 1.0.0-beta.1
 */
class FileData(
    val uploadedFilename: String,
    val content: FileContent,
) {
    /**
     * Creates a file with inline [content].
     *
     * @since 1.0.0-beta.1
     */
    constructor(uploadedFilename: String, content: ByteArray) : this(uploadedFilename, FileContent.Inline(content))
}

/**
 * Binary content held in memory or referenced in file storage.
 *
 * @since 1.0.0-beta.1
 */
sealed interface FileContent {
    /**
     * Content supplied in memory.
     *
     * @property bytes the raw bytes.
     * @since 1.0.0-beta.1
     */
    class Inline(val bytes: ByteArray) : FileContent

    /**
     * Content kept in file storage, without a transaction or entity dependency.
     *
     * @property ref the opaque blob reference.
     * @property kind the storage directory kind.
     * @since 1.0.0-beta.1
     */
    data class Stored(val ref: StoredBlobRef, val kind: FileStorageKind) : FileContent
}

/**
 * Storage directory kind of a file.
 *
 * @since 1.0.0-beta.1
 */
enum class FileStorageKind {
    /**
     * Statement files.
     *
     * @since 1.0.0-beta.1
     */
    Statement,

    /**
     * Exercise files.
     *
     * @since 1.0.0-beta.1
     */
    Exercise,

    /**
     * Test files.
     *
     * @since 1.0.0-beta.1
     */
    Test,

    /**
     * Solution files.
     *
     * @since 1.0.0-beta.1
     */
    Solution,

    /**
     * Recording files.
     *
     * @since 1.0.0-beta.1
     */
    Recording,

    /**
     * Logs files.
     *
     * @since 1.0.0-beta.1
     */
    Logs,
}
