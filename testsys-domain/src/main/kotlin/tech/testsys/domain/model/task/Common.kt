package tech.testsys.domain.model.task

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
 * An uploaded file. Not a data class: equality is reference-based.
 *
 * @property uploadedFilename the original name of the file as it entered the system.
 * @property content the raw binary content of the file.
 * @since %CURRENT_VERSION%
 */
class FileData(
    val uploadedFilename: String,
    val content: ByteArray,
)
